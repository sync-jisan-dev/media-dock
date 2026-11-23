import { Injectable } from "@nestjs/common";
import type { Platform } from "@snagbox/shared";
import { safeFetch } from "../../common/http/safe-fetch";
import { AppConfigService } from "../../config/app-config.service";
import { DESKTOP_UA } from "../resilience";
import type { MediaProvider, ProviderMedia } from "./types";

const DOWNLOAD_ENDPOINT = "https://api.tikliveapi.com/download-video/";
const LINK_TTL_MS = 60_000;
const MAX_CACHED_LINKS = 128;
const CDN_DOMAINS = ["tiktokcdn.com", "tiktokcdn-us.com", "tiktokcdn-eu.com", "tokcdn.com"];

function matchesDomain(hostname: string, domain: string): boolean {
  return hostname === domain || hostname.endsWith(`.${domain}`);
}

function tiktokVideoUrl(value: string): URL | null {
  try {
    const url = new URL(value);
    if (url.protocol !== "https:" || url.username || url.password || url.port) return null;
    if (!matchesDomain(url.hostname, "tiktok.com")) return null;
    const isVideo = /^\/@[^/]+\/video\/\d+\/?$/.test(url.pathname);
    const isShort = /^\/t\/[\w-]+\/?$/.test(url.pathname)
      || (["vm.tiktok.com", "vt.tiktok.com"].includes(url.hostname) && /^\/[\w-]+\/?$/.test(url.pathname));
    if (!isVideo && !isShort) return null;
    url.search = "";
    url.hash = "";
    url.pathname = url.pathname.replace(/\/$/, "");
    return url;
  } catch {
    return null;
  }
}

function mediaUrl(value: unknown): string | null {
  if (typeof value !== "string") return null;
  try {
    const url = new URL(value);
    if (url.protocol !== "https:" || url.username || url.password || url.port) return null;
    return CDN_DOMAINS.some((domain) => matchesDomain(url.hostname, domain)) ? url.toString() : null;
  } catch {
    return null;
  }
}

@Injectable()
export class TikLiveApiProvider implements MediaProvider {
  readonly name = "tikliveapi";
  private readonly links = new Map<string, { expiresAt: number; result: Promise<ProviderMedia> }>();

  constructor(private readonly config: AppConfigService) {}

  supports(url: string, platform: Platform | null): boolean {
    return Boolean(this.config.tikLiveApiKey) && platform === "tiktok" && tiktokVideoUrl(url) !== null;
  }

  async extract(value: string): Promise<ProviderMedia> {
    const url = tiktokVideoUrl(value);
    const apiKey = this.config.tikLiveApiKey;
    if (!url || !apiKey) throw new Error("TikLiveAPI requires a public TikTok video link and a configured API key.");

    // Inspection and the subsequent job can share the same signed CDN links.
    const key = url.toString();
    const now = Date.now();
    const cached = this.links.get(key);
    if (cached && cached.expiresAt > now) return cached.result;

    this.links.delete(key);
    if (this.links.size >= MAX_CACHED_LINKS) {
      const oldest = this.links.keys().next().value;
      if (oldest !== undefined) this.links.delete(oldest);
    }
    const result = this.request(url, apiKey);
    this.links.set(key, { expiresAt: now + LINK_TTL_MS, result });
    try {
      return await result;
    } catch (error) {
      if (this.links.get(key)?.result === result) this.links.delete(key);
      throw error;
    }
  }

  private async request(url: URL, apiKey: string): Promise<ProviderMedia> {
    const endpoint = new URL(DOWNLOAD_ENDPOINT);
    endpoint.searchParams.set("url", url.toString());
    const response = await safeFetch(endpoint.toString(), {
      headers: { "X-Api-Key": apiKey, Accept: "application/json" },
      maxRedirects: 0,
      timeoutMs: 10_000,
      maxBytes: 64 * 1024,
    });
    if (response.status < 200 || response.status >= 300) {
      response.body.resume();
      throw new Error(`TikLiveAPI returned HTTP ${response.status}.`);
    }

    let data: { video_hd?: unknown; video?: unknown; message?: unknown } | null;
    try {
      data = JSON.parse(await response.text()) as typeof data;
    } catch {
      throw new Error("TikLiveAPI returned an unreadable video response.");
    }
    if (typeof data?.message === "string" && data.message) {
      // The provider can report key and quota failures inside a 200 response.
      throw new Error("TikLiveAPI declined the video request.");
    }
    const videoUrls = [...new Set([mediaUrl(data?.video_hd), mediaUrl(data?.video)].filter((link): link is string => link !== null))];
    if (!videoUrls.length) throw new Error("TikLiveAPI returned no supported TikTok video links.");

    const uploader = /^\/@([^/]+)\/video\//.exec(url.pathname)?.[1] ?? null;
    return {
      id: url.pathname.split("/").at(-1) ?? "tiktok-video",
      title: uploader ? `TikTok video by @${uploader}` : "TikTok video",
      description: "",
      thumbnail: null,
      durationSeconds: null,
      uploader,
      uploaderUrl: uploader ? `https://www.tiktok.com/@${uploader}` : null,
      viewCount: null,
      likeCount: null,
      commentCount: null,
      uploadedAt: null,
      isLive: false,
      videoUrls,
      headers: { "User-Agent": DESKTOP_UA, Referer: "https://www.tiktok.com/" },
    };
  }
}
