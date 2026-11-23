import { Injectable, Logger } from "@nestjs/common";
import type { MediaInfo, Platform, VideoQuality } from "@snagbox/shared";
import { AUDIO_FORMATS, AUDIO_QUALITIES, VIDEO_FORMATS, detectPlatform } from "@snagbox/shared";
import { basename, dirname, join } from "path";
import { AppConfigService } from "../config/app-config.service";
import { sanitizeFilename } from "../common/utils/filename";
import { DirectDownloadService } from "./direct-download.service";
import { MediaCache } from "./media-cache";
import { TikTokEmbedProvider } from "./providers/tiktok-embed.provider";
import { TikLiveApiProvider } from "./providers/tikliveapi.provider";
import { CobaltProvider } from "./providers/cobalt.provider";
import { ApifyDownloaderService } from "./providers/apify-downloader.service";
import { IGramApiProvider } from "./providers/igramapi.provider";
import type { MediaProvider, ProviderMedia } from "./providers/types";
import { DESKTOP_UA } from "./resilience";
import { CancelledError, RunDownloadInput, YtdlpService } from "./ytdlp.service";

/** Tries enabled cloud extraction, yt-dlp, and alternate providers in order. */
@Injectable()
export class ExtractionService {
  private readonly logger = new Logger(ExtractionService.name);
  private readonly providers: MediaProvider[];
  /** In-flight extractions so concurrent callers share one process. */
  private readonly inflight = new Map<string, Promise<MediaInfo>>();

  constructor(
    private readonly ytdlp: YtdlpService,
    private readonly direct: DirectDownloadService,
    private readonly config: AppConfigService,
    private readonly cache: MediaCache,
    cobalt: CobaltProvider,
    tiktokEmbed: TikTokEmbedProvider,
    tikLiveApi: TikLiveApiProvider,
    private readonly apify: ApifyDownloaderService,
    igram: IGramApiProvider,
  ) {
    this.providers = [cobalt, tiktokEmbed, tikLiveApi, igram];
  }

  async getMediaInfo(url: string): Promise<MediaInfo> {
    const ttl = this.config.mediaCacheTtlMs;
    if (ttl > 0) {
      const cached = await this.cache.get(url);
      if (cached) return cached;
    }
    const existing = this.inflight.get(url);
    if (existing) return existing;

    const promise = this.resolveInfo(url)
      .then(async (info) => {
        if (ttl > 0) await this.cache.set(url, info, ttl);
        return info;
      })
      .finally(() => {
        this.inflight.delete(url);
      });
    this.inflight.set(url, promise);
    return promise;
  }

  private async resolveInfo(url: string): Promise<MediaInfo> {
    const platform = detectPlatform(url);
    try {
      // yt-dlp reports the actual adaptive formats, including YouTube's
      // video-only 1080p/4K streams. The Apify actor is capped at 360p and
      // must stay a fallback so it cannot hide higher available qualities.
      return await this.ytdlp.extractInfo(url);
    } catch (primaryError) {
      if (this.apify.supports(url)) {
        try {
          return await this.apify.getMediaInfo(url);
        } catch {
          this.logger.warn("Apify metadata failed; trying the next extractor");
        }
      }
      const media = await this.tryProviders(url, platform, primaryError);
      if (!media) {
        const fallback = this.fallbackMediaInfo(url, platform ?? "generic");
        if (fallback) {
          this.logger.log(`Recovered metadata via direct file fallback for ${url}`);
          return fallback;
        }
        throw primaryError;
      }
      return this.toMediaInfo(media, url, platform ?? "generic");
    }
  }

  private fallbackMediaInfo(url: string, platform: Platform): MediaInfo | null {
    try {
      const parsed = new URL(url);
      const clean = parsed.pathname.toLowerCase();
      const ext = clean.split(".").pop() || "";
      const isDirect = /\.(mp4|webm|mkv|mov|avi|mp3|m4a|wav|aac|ogg|opus|flac|pdf|doc|docx|jpg|jpeg|png|webp|gif|svg)$/i.test(clean);
      if (!isDirect && platform !== "generic") return null;

      const filename = decodeURIComponent(parsed.pathname.split("/").pop() || "media");
      const isImage = ["jpg", "jpeg", "png", "webp", "gif", "svg"].includes(ext);
      const isDoc = ["pdf", "doc", "docx"].includes(ext);
      const isAudio = ["mp3", "m4a", "wav", "aac", "ogg", "opus", "flac"].includes(ext);

      return {
        id: sanitizeFilename(filename, 40) || "media",
        platform,
        url,
        title: filename || `${parsed.hostname} media`,
        description: isDoc ? "Document Download" : isImage ? "Image Download" : isAudio ? "Audio Download" : "Media Download",
        thumbnail: isImage ? url : null,
        durationSeconds: null,
        uploader: parsed.hostname,
        uploaderUrl: `https://${parsed.hostname}`,
        viewCount: null,
        likeCount: null,
        commentCount: null,
        uploadedAt: new Date().toISOString(),
        isLive: false,
        qualities: {
          video: ["highest", "1080p", "720p", "480p", "lowest"] as VideoQuality[],
          audio: [...AUDIO_QUALITIES],
        },
        formats: {
          video: [...VIDEO_FORMATS],
          audio: [...AUDIO_FORMATS],
        },
        subtitles: [],
      };
    } catch {
      return null;
    }
  }

  /** First provider that can both claim and resolve the URL, or null. */
  private async tryProviders(
    url: string,
    platform: Platform | null,
    primaryError: unknown,
  ): Promise<ProviderMedia | null> {
    for (const provider of this.providers) {
      if (!provider.supports(url, platform)) continue;
      try {
        const media = await provider.extract(url);
        if (media.videoUrls.length === 0) throw new Error("Provider returned no downloadable media");
        this.logger.log(
          `yt-dlp could not reach ${url} (${errorText(primaryError)}); recovered via ${provider.name}`,
        );
        return media;
      } catch (providerError) {
        this.logger.warn(`Provider ${provider.name} also failed: ${errorText(providerError)}`);
      }
    }
    return null;
  }

  /**
   * Tries cloud downloads, yt-dlp, and provider URLs until a file transfer succeeds.
   *
   * The fallback is a genuinely different route to the bytes, so it is worth
   * taking even though it gives up quality selection: one playable file beats a
   * perfectly-specified failure.
   */
  async runDownload(input: RunDownloadInput & { kind: string; format: string }): Promise<void> {
    if (this.apify.canDownload(input)) {
      try {
        await this.apify.runDownload(input);
        return;
      } catch (error) {
        if (error instanceof CancelledError || input.isCancelled?.()) throw error;
        this.logger.warn("Apify download failed; trying the next extractor");
      }
    }
    try {
      await this.ytdlp.runDownload(input);
      return;
    } catch (primaryError) {
      if (primaryError instanceof CancelledError || input.isCancelled?.()) throw primaryError;

      const platform = detectPlatform(input.url);
      for (const provider of this.providers) {
        if (input.isCancelled?.()) throw new CancelledError();
        if (!provider.supports(input.url, platform)) continue;
        try {
          const media = await provider.extract(input.url);
          for (const source of media.videoUrls) {
            if (input.isCancelled?.()) throw new CancelledError();
            try {
              await this.downloadDirect(input, source, media.headers);
              this.logger.log(`Download completed via ${provider.name}`);
              return;
            } catch (error) {
              if (error instanceof CancelledError || input.isCancelled?.()) throw error;
              this.logger.warn(`Media transfer via ${provider.name} failed: ${errorText(error)}`);
            }
          }
        } catch (error) {
          if (error instanceof CancelledError || input.isCancelled?.()) throw error;
          this.logger.warn(`Provider ${provider.name} failed: ${errorText(error)}`);
        }
      }

      // Final fallback for direct streams, documents, images, or generic links
      try {
        this.logger.log(`Attempting direct stream download fallback for: ${input.url}`);
        await this.downloadDirect(input, input.url, {
          "User-Agent": DESKTOP_UA,
          Accept: "*/*",
        });
        this.logger.log(`Direct stream download fallback succeeded: ${input.url}`);
        return;
      } catch (directErr) {
        if (directErr instanceof CancelledError || input.isCancelled?.()) throw directErr;
        this.logger.warn(`Direct download fallback also failed: ${errorText(directErr)}`);
      }

      throw primaryError;
    }
  }

  private async downloadDirect(
    input: RunDownloadInput & { kind: string; format: string },
    source: string,
    headers: Record<string, string>,
  ): Promise<void> {
    const dir = dirname(input.outputPath);
    const base = basename(input.outputPath).replace(/\.%\(ext\)s$/, "");
    const ext = (input.format || "mp4").toLowerCase();
    const target = join(dir, `${base}.${ext}`);

    const isNonVideo =
      input.kind === "image" ||
      input.kind === "document" ||
      ["pdf", "doc", "docx", "jpg", "jpeg", "png", "webp", "gif", "svg"].includes(ext);

    if (isNonVideo || (input.kind === "video" && ext === "mp4")) {
      await this.direct.download({
        url: source,
        outputPath: target,
        headers,
        onProgress: input.onProgress,
        isCancelled: input.isCancelled,
      });
      return;
    }

    const fetched = join(dir, `${base}.src.mp4`);
    await this.direct.download({
      url: source,
      outputPath: fetched,
      headers,
      onProgress: input.onProgress,
      isCancelled: input.isCancelled,
    });

    await this.direct.convert(fetched, target, input.kind === "audio" ? "audio" : "video");
  }

  /** Providers describe one rendition; MediaInfo has to describe the menu. */
  private toMediaInfo(media: ProviderMedia, url: string, platform: Platform): MediaInfo {
    return {
      id: media.id,
      platform,
      url,
      title: media.title,
      description: media.description,
      thumbnail: media.thumbnail,
      durationSeconds: media.durationSeconds,
      uploader: media.uploader,
      uploaderUrl: media.uploaderUrl,
      viewCount: media.viewCount,
      likeCount: media.likeCount,
      commentCount: media.commentCount,
      uploadedAt: media.uploadedAt,
      isLive: media.isLive,
      qualities: {
        // A provider serves the platform's single rendition, so offering a
        // quality ladder here would be a menu we cannot honour.
        video: ["highest"] as VideoQuality[],
        audio: [...AUDIO_QUALITIES],
      },
      formats: { video: [...VIDEO_FORMATS], audio: [...AUDIO_FORMATS] },
      subtitles: [],
    };
  }
}

function errorText(error: unknown): string {
  return (error instanceof Error ? error.message : String(error)).split("\n")[0].slice(0, 200);
}
