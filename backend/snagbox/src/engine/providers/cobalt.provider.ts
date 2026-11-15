import { Injectable } from "@nestjs/common";
import type { Platform } from "@snagbox/shared";
import { AppConfigService } from "../../config/app-config.service";
import { safeFetch } from "../../common/http/safe-fetch";
import type { MediaProvider, ProviderMedia } from "./types";

interface CobaltPickerItem {
  type?: "photo" | "video" | "gif";
  url?: string;
  thumb?: string;
}

interface CobaltResponse {
  status?: "tunnel" | "redirect" | "picker" | "local-processing" | "error";
  url?: string;
  filename?: string;
  picker?: CobaltPickerItem[];
  error?: { code?: string };
}

/**
 * Optional fallback backed by an operator-owned cobalt instance.
 *
 * api.cobalt.tools is deliberately not a default: cobalt's documentation says
 * hosted instances are not intended for third-party applications. Operators
 * enable this provider explicitly with COBALT_API_URL.
 */
@Injectable()
export class CobaltProvider implements MediaProvider {
  readonly name = "cobalt";

  constructor(private readonly config: AppConfigService) {}

  supports(): boolean {
    return Boolean(this.config.cobaltApiUrl) && !/^https?:\/\/api\.cobalt\.tools\/?$/i.test(this.config.cobaltApiUrl);
  }

  async extract(url: string): Promise<ProviderMedia> {
    const headers: Record<string, string> = {
      Accept: "application/json",
      "Content-Type": "application/json",
      "User-Agent": "MediaDock/2.0",
    };
    if (this.config.cobaltApiKey) {
      headers.Authorization = `Api-Key ${this.config.cobaltApiKey}`;
    }

    const response = await safeFetch(`${this.config.cobaltApiUrl}/`, {
      method: "POST",
      headers,
      body: JSON.stringify({
        url,
        downloadMode: "auto",
        filenameStyle: "pretty",
        videoQuality: "max",
        localProcessing: "disabled",
      }),
      timeoutMs: 45_000,
      maxBytes: 2 * 1024 * 1024,
    });
    const body = await response.text();
    if (response.status < 200 || response.status >= 300) {
      throw new Error(`cobalt returned HTTP ${response.status}`);
    }

    let result: CobaltResponse;
    try {
      result = JSON.parse(body) as CobaltResponse;
    } catch {
      throw new Error("cobalt returned invalid JSON");
    }
    if (result.status === "error") {
      throw new Error(`cobalt could not resolve this media (${result.error?.code ?? "unknown"})`);
    }

    const picker = result.status === "picker" ? result.picker ?? [] : [];
    const videoUrls = result.url
      ? [result.url]
      : picker.map((item) => item.url).filter((item): item is string => Boolean(item));
    if (!videoUrls.length) throw new Error(`cobalt returned unsupported status ${result.status ?? "unknown"}`);

    const parsed = new URL(url);
    const filename = result.filename?.replace(/\.[a-z0-9]{2,5}$/i, "").trim();
    return {
      id: `cobalt-${Buffer.from(url).toString("base64url").slice(0, 16)}`,
      title: filename || `Media from ${parsed.hostname.replace(/^www\./, "")}`,
      description: "Resolved through the MediaDock cobalt fallback engine.",
      thumbnail: picker.find((item) => item.thumb)?.thumb ?? null,
      durationSeconds: null,
      uploader: null,
      uploaderUrl: null,
      viewCount: null,
      likeCount: null,
      commentCount: null,
      uploadedAt: null,
      isLive: false,
      videoUrls,
      headers: { "User-Agent": "MediaDock/2.0" },
    };
  }
}
