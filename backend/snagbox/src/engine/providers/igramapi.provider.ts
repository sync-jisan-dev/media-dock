import { Injectable } from "@nestjs/common";
import type { Platform } from "@snagbox/shared";
import { detectPlatform } from "@snagbox/shared";
import { safeFetch } from "../../common/http/safe-fetch";
import { AppConfigService } from "../../config/app-config.service";
import type { MediaProvider, ProviderMedia } from "./types";

const IGRAM_API = "https://api.igramapi.com";
const MAX_RESPONSE_BYTES = 1024 * 1024;

interface FacebookVideoResponse {
  id?: string;
  caption?: string | null;
  created_at?: number | null;
  video?: {
    hd_url?: string | null;
    sd_url?: string | null;
    duration?: number | null;
    thumbnail_url?: string | null;
  };
  author?: { name?: string | null; url?: string | null };
}

interface InstagramMediaResponse {
  shortcode?: string;
  media_type?: string;
  video_url?: string | null;
  thumbnail?: string | null;
  formats?: Array<{ url?: string; quality?: string; height?: number; audio_url?: string | null }>;
  slides?: Array<{ type?: string; url?: string; thumbnail?: string }>;
  caption?: string | null;
  duration?: number | null;
  taken_at?: number | null;
  author?: { username?: string | null; full_name?: string | null; profile_picture_url?: string | null };
}

/**
 * Optional public-media fallback for Facebook and Instagram.
 * yt-dlp remains first; this API is only called when the built-in extractor
 * fails. Its credential is sent only to api.igramapi.com and never logged.
 */
@Injectable()
export class IGramApiProvider implements MediaProvider {
  readonly name = "igramapi";

  constructor(private readonly config: AppConfigService) {}

  supports(url: string, platform: Platform | null): boolean {
    return Boolean(this.config.igramApiKey) && (platform === "facebook" || platform === "instagram") &&
      detectPlatform(url) === platform;
  }

  async extract(url: string): Promise<ProviderMedia> {
    const platform = detectPlatform(url);
    if (!this.supports(url, platform)) throw new Error("IGramAPI is not configured for this platform");

    const endpoint = platform === "facebook" ? "/v1/facebook/videos" : "/v1/media";
    const result = await this.request<FacebookVideoResponse | InstagramMediaResponse>(endpoint, url);
    return platform === "facebook"
      ? this.facebookMedia(result as FacebookVideoResponse)
      : this.instagramMedia(result as InstagramMediaResponse);
  }

  private async request<T>(endpoint: string, sourceUrl: string): Promise<T> {
    const target = new URL(endpoint, IGRAM_API);
    target.searchParams.set("url", sourceUrl);
    const response = await safeFetch(target.toString(), {
      headers: { Authorization: `Bearer ${this.config.igramApiKey}`, Accept: "application/json" },
      timeoutMs: 15_000,
      maxBytes: MAX_RESPONSE_BYTES,
    });
    if (new URL(response.url).origin !== IGRAM_API) {
      response.body.resume();
      throw new Error("IGramAPI redirected outside its API host");
    }
    const body = await response.text();
    if (response.status < 200 || response.status >= 300) {
      throw new Error(`IGramAPI returned HTTP ${response.status}`);
    }
    try {
      return JSON.parse(body) as T;
    } catch {
      throw new Error("IGramAPI returned invalid JSON");
    }
  }

  private facebookMedia(data: FacebookVideoResponse): ProviderMedia {
    const video = data.video;
    if (!video) throw new Error("IGramAPI found no public Facebook video");
    const videoUrls = uniqueUrls([video.hd_url, video.sd_url], "facebook");
    if (!videoUrls.length) throw new Error("IGramAPI returned no valid Facebook CDN video URLs");

    return {
      id: data.id || "facebook-video",
      title: cleanText(data.caption) || "Facebook video",
      description: cleanText(data.caption),
      thumbnail: firstCdnUrl([video.thumbnail_url], "facebook"),
      durationSeconds: finiteNumber(video.duration),
      uploader: cleanText(data.author?.name) || null,
      uploaderUrl: safeProfileUrl(data.author?.url, "facebook"),
      viewCount: null,
      likeCount: null,
      commentCount: null,
      uploadedAt: unixDate(data.created_at),
      isLive: false,
      videoUrls,
      headers: { "User-Agent": "MediaDock/2.0", Referer: "https://www.facebook.com/" },
    };
  }

  private instagramMedia(data: InstagramMediaResponse): ProviderMedia {
    if (data.media_type && data.media_type !== "video") {
      throw new Error(`IGramAPI returned ${data.media_type}, not a video`);
    }

    // Prefer progressive MP4 formats that already contain audio. Adaptive
    // video-only formats are excluded to avoid saving a silent video.
    const formats = Array.isArray(data.formats) ? data.formats : [];
    const progressive = formats
      .filter((format) => !format.audio_url)
      .sort((a, b) => (b.height ?? qualityHeight(b.quality)) - (a.height ?? qualityHeight(a.quality)))
      .map((format) => format.url);
    // `video_url` is the API's best rendition when no per-quality list is
    // supplied. When formats are listed, use only progressive entries: a
    // format with audio_url needs a separate merge request, so downloading
    // just its video URL would produce a silent file.
    const videoUrls = uniqueUrls([
      ...progressive,
      ...(formats.length === 0 ? [data.video_url] : []),
    ], "instagram");
    if (!videoUrls.length) throw new Error("IGramAPI returned no valid Instagram video URL with audio");

    const username = cleanText(data.author?.username);
    return {
      id: data.shortcode || "instagram-video",
      title: cleanText(data.caption) || (username ? `Instagram video by ${username}` : "Instagram video"),
      description: cleanText(data.caption),
      thumbnail: firstCdnUrl([data.thumbnail, ...(data.slides ?? []).map((slide) => slide.thumbnail), data.author?.profile_picture_url], "instagram"),
      durationSeconds: finiteNumber(data.duration),
      uploader: username || cleanText(data.author?.full_name) || null,
      uploaderUrl: username ? `https://www.instagram.com/${encodeURIComponent(username)}/` : null,
      viewCount: null,
      likeCount: null,
      commentCount: null,
      uploadedAt: unixDate(data.taken_at),
      isLive: false,
      videoUrls,
      headers: { "User-Agent": "MediaDock/2.0", Referer: "https://www.instagram.com/" },
    };
  }
}

function uniqueUrls(values: Array<string | null | undefined>, platform: "facebook" | "instagram"): string[] {
  return [...new Set(values.map((value) => cdnUrl(value, platform)).filter((value): value is string => Boolean(value)))];
}

function firstCdnUrl(values: Array<string | null | undefined>, platform: "facebook" | "instagram"): string | null {
  return values.map((value) => cdnUrl(value, platform)).find((value): value is string => Boolean(value)) ?? null;
}

function cdnUrl(value: string | null | undefined, platform: "facebook" | "instagram"): string | null {
  if (!value) return null;
  try {
    const url = new URL(value);
    const roots = platform === "facebook" ? ["fbcdn.net"] : ["cdninstagram.com", "fbcdn.net"];
    if (url.protocol !== "https:" || url.username || url.password || !roots.some((root) => hostMatches(url.hostname, root))) {
      return null;
    }
    return url.toString();
  } catch {
    return null;
  }
}

function hostMatches(host: string, root: string): boolean {
  const normalized = host.toLowerCase().replace(/\.$/, "");
  return normalized === root || normalized.endsWith(`.${root}`);
}

function safeProfileUrl(value: string | null | undefined, platform: "facebook" | "instagram"): string | null {
  if (!value) return null;
  try {
    const url = new URL(value);
    const roots = platform === "facebook" ? ["facebook.com"] : ["instagram.com"];
    return url.protocol === "https:" && !url.username && !url.password && roots.some((root) => hostMatches(url.hostname, root))
      ? url.toString()
      : null;
  } catch {
    return null;
  }
}

function cleanText(value: string | null | undefined): string {
  return typeof value === "string" ? value.trim().slice(0, 2_000) : "";
}

function finiteNumber(value: number | null | undefined): number | null {
  return typeof value === "number" && Number.isFinite(value) && value >= 0 ? value : null;
}

function unixDate(value: number | null | undefined): string | null {
  if (typeof value !== "number" || !Number.isFinite(value) || value <= 0) return null;
  const date = new Date(value * 1_000);
  return Number.isNaN(date.getTime()) ? null : date.toISOString();
}

function qualityHeight(quality: string | undefined): number {
  const match = /(?:^|\D)(\d{3,4})p?(?:\D|$)/i.exec(quality ?? "");
  return match ? Number(match[1]) : 0;
}
