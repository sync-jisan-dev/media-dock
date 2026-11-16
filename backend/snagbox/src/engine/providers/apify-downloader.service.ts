import { Injectable } from "@nestjs/common";
import type { MediaInfo } from "@snagbox/shared";
import { detectPlatform } from "@snagbox/shared";
import { basename, dirname, join } from "path";
import { safeFetch } from "../../common/http/safe-fetch";
import { AppConfigService } from "../../config/app-config.service";
import { DirectDownloadService } from "../direct-download.service";
import { LruTtlCache } from "../media-cache";
import { CancelledError, RunDownloadInput } from "../ytdlp.service";

interface ActorRun {
  id: string;
  status: string;
  defaultDatasetId: string;
}

interface ActorResult {
  status?: string;
  ok?: boolean;
  meta?: { title?: string; duration_sec?: number };
  artifacts?: Array<{ name?: string; url?: string; bytes?: number }>;
}

/** Uses the account's free-credit allowance. The account must retain its Free plan hard limit. */
@Injectable()
export class ApifyDownloaderService {
  private readonly completed = new LruTtlCache<ActorResult>(100);
  private readonly inflight = new Map<string, Promise<ActorResult>>();

  constructor(
    private readonly config: AppConfigService,
    private readonly direct: DirectDownloadService,
  ) {}

  supports(url: string): boolean {
    return this.config.apifyEnabled && Boolean(this.config.apifyApiToken) && detectPlatform(url) === "youtube";
  }

  async getMediaInfo(url: string): Promise<MediaInfo> {
    const result = await this.resolve("resolve", url);
    const id = new URL(url).searchParams.get("v") || new URL(url).pathname.split("/").filter(Boolean).at(-1) || "youtube";
    return {
      id, platform: "youtube", url,
      title: result.meta?.title || "YouTube video",
      description: "Resolved through Apify", thumbnail: `https://i.ytimg.com/vi/${encodeURIComponent(id)}/hqdefault.jpg`,
      durationSeconds: Math.round(result.meta?.duration_sec || 0) || null,
      uploader: null, uploaderUrl: null, viewCount: null, likeCount: null,
      commentCount: null, uploadedAt: null, isLive: false,
      qualities: { video: ["360p"], audio: [] },
      formats: { video: ["mp4"], audio: [] }, subtitles: [],
    };
  }

  canDownload(input: RunDownloadInput): boolean {
    const r = input.request;
    return this.supports(input.url) && r.kind === "video" && (!r.format || r.format === "mp4")
      && (!r.quality || ["360p", "highest", "lowest"].includes(r.quality))
      && r.clipStart == null && r.clipEnd == null && !r.embedSubtitles && !r.writeSubtitles
      && !r.embedMetadata && !r.embedChapters && !r.embedThumbnail && !r.writeThumbnail && !r.videoCodec;
  }

  async runDownload(input: RunDownloadInput): Promise<void> {
    const result = await this.resolve("full", input.url, input.isCancelled);
    if (input.isCancelled?.()) throw new CancelledError();
    const artifact = result.artifacts?.find((item) => item.url && (item.bytes ?? 0) > 0);
    if (!artifact?.url) throw new Error("Apify returned no downloadable file");
    const source = new URL(artifact.url);
    if (source.protocol !== "https:" || source.hostname !== "api.apify.com" || !source.pathname.startsWith("/v2/key-value-stores/")) {
      throw new Error("Apify returned an unexpected artifact address");
    }
    source.searchParams.delete("token");
    const name = basename(input.outputPath).replace(/\.%\(ext\)s$/, "");
    await this.direct.download({
      url: source.toString(), outputPath: join(dirname(input.outputPath), `${name}.mp4`),
      headers: { Authorization: `Bearer ${this.config.apifyApiToken}` },
      onProgress: input.onProgress, isCancelled: input.isCancelled,
    });
  }

  private async resolve(op: "resolve" | "full", url: string, isCancelled?: () => boolean): Promise<ActorResult> {
    if (isCancelled?.()) throw new CancelledError();
    const key = `${op}:${url}`;
    const cached = this.completed.get(key);
    if (cached) return cached;
    // Full downloads have independent cancellation. Metadata callers can share a run.
    if (op === "resolve") {
      const existing = this.inflight.get(key);
      if (existing) return existing;
    }
    const pending = this.startRun(op, url, isCancelled).then((result) => {
      this.completed.set(key, result, 300_000);
      return result;
    });
    if (op === "resolve") this.inflight.set(key, pending);
    try {
      return await pending;
    } finally {
      if (op === "resolve") this.inflight.delete(key);
    }
  }

  private async startRun(op: string, url: string, isCancelled?: () => boolean): Promise<ActorResult> {
    // No POST retries. A lost response must not start another billable Actor run.
    const started = await this.request<{ data: ActorRun }>(
      "/acts/andrew_babo~video-downloader/runs?maxTotalChargeUsd=0.50&timeout=180&memory=1024&waitForFinish=1",
      { op, source: url, options: { min_height: 360, max_height: 360, lanes: 8,
        proxy_ladder: ["direct", "DATACENTER", "RESIDENTIAL"] }, cleanup: "on_success" },
    );
    let run = started.data;
    if (!run?.id || !run.defaultDatasetId) throw new Error("Apify returned an invalid run");
    const deadline = Date.now() + 190_000;
    while (["READY", "RUNNING"].includes(run.status)) {
      if (isCancelled?.()) {
        await this.request(`/actor-runs/${encodeURIComponent(run.id)}/abort`, {}).catch(() => undefined);
        throw new CancelledError();
      }
      if (Date.now() >= deadline) throw new Error("Apify processing timed out");
      await new Promise((resolve) => setTimeout(resolve, 1_500));
      run = (await this.request<{ data: ActorRun }>(`/actor-runs/${encodeURIComponent(run.id)}`)).data;
    }
    if (run.status !== "SUCCEEDED") throw new Error(`Apify processing ended with ${run.status}`);
    const items = await this.request<ActorResult[]>(`/datasets/${encodeURIComponent(run.defaultDatasetId)}/items?clean=true&limit=1`);
    const result = items[0];
    if (!result || result.status !== "success" || result.ok === false) throw new Error("Apify could not resolve this media");
    return result;
  }

  private async request<T>(path: string, body?: object): Promise<T> {
    const response = await safeFetch(`https://api.apify.com/v2${path}`, {
      method: body ? "POST" : "GET", maxRedirects: 0, timeoutMs: 20_000, maxBytes: 4 * 1024 * 1024,
      headers: { Authorization: `Bearer ${this.config.apifyApiToken}`, "Content-Type": "application/json" },
      body: body ? JSON.stringify(body) : undefined,
    });
    const text = await response.text();
    if (response.status < 200 || response.status >= 300) throw new Error(`Apify returned HTTP ${response.status}`);
    return JSON.parse(text) as T;
  }
}
