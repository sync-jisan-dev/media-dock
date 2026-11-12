import { Injectable, Logger } from "@nestjs/common";
import * as cheerio from "cheerio";
import { assertPublicHttpUrl, safeFetch } from "../../common/http/safe-fetch";
import { ApiException, ErrorCode } from "../../common/errors";

export interface ExtractedImageItem {
  id: string;
  url: string;
  title?: string;
  alt?: string;
  width?: number;
  height?: number;
  format?: string;
  source: "og" | "twitter" | "jsonld" | "img" | "direct";
}

export interface ImageExtractionResult {
  url: string;
  siteTitle: string;
  totalFound: number;
  images: ExtractedImageItem[];
}

const COMMON_IMAGE_EXTENSIONS = new Set(["jpg", "jpeg", "png", "webp", "gif", "bmp", "avif"]);
const TRACKING_KEYWORDS = ["tracking", "pixel", "beacon", "analytics", "spacer", "1x1", "blank.gif"];

@Injectable()
export class ImageExtractorService {
  private readonly logger = new Logger(ImageExtractorService.name);

  private sanitizeUrl(raw: string, baseUrl?: string): string | null {
    try {
      const parsed = baseUrl ? new URL(raw, baseUrl) : new URL(raw);
      if (parsed.protocol !== "http:" && parsed.protocol !== "https:") return null;
      // Drop tracking queries
      parsed.searchParams.delete("utm_source");
      parsed.searchParams.delete("utm_medium");
      parsed.searchParams.delete("utm_campaign");
      parsed.searchParams.delete("utm_term");
      parsed.searchParams.delete("utm_content");
      parsed.hash = "";
      return parsed.href;
    } catch {
      return null;
    }
  }

  private isTrackingImage(url: string, width?: number, height?: number): boolean {
    if (width === 1 && height === 1) return true;
    const lower = url.toLowerCase();
    return TRACKING_KEYWORDS.some((kw) => lower.includes(kw));
  }

  private detectFormatFromUrl(url: string): string | undefined {
    try {
      const pathname = new URL(url).pathname;
      const match = pathname.match(/\.([a-zA-Z0-9]+)$/);
      if (match) {
        const ext = match[1].toLowerCase();
        if (COMMON_IMAGE_EXTENSIONS.has(ext)) return ext === "jpeg" ? "jpg" : ext;
      }
    } catch {
      // ignore
    }
    return undefined;
  }

  private parseSrcSetBest(srcset: string, baseUrl: string): string | null {
    if (!srcset) return null;
    const candidates = srcset.split(",").map((item) => item.trim()).filter(Boolean);
    let bestUrl: string | null = null;
    let maxDensityOrWidth = -1;

    for (const cand of candidates) {
      const parts = cand.split(/\s+/);
      const url = parts[0];
      const descriptor = parts[1] ?? "";

      let score = 1;
      if (descriptor.endsWith("w")) {
        score = parseInt(descriptor.slice(0, -1), 10) || 1;
      } else if (descriptor.endsWith("x")) {
        score = (parseFloat(descriptor.slice(0, -1)) || 1) * 1000;
      }

      if (score > maxDensityOrWidth) {
        maxDensityOrWidth = score;
        bestUrl = url;
      }
    }

    return bestUrl ? this.sanitizeUrl(bestUrl, baseUrl) : null;
  }

  async extract(rawUrl: string): Promise<ImageExtractionResult> {
    assertPublicHttpUrl(rawUrl);

    // 1. Direct Image Check by extension
    const directFormat = this.detectFormatFromUrl(rawUrl);
    if (directFormat) {
      return {
        url: rawUrl,
        siteTitle: "Direct Image",
        totalFound: 1,
        images: [
          {
            id: "img_0",
            url: rawUrl,
            title: "Direct Image",
            format: directFormat,
            source: "direct",
          },
        ],
      };
    }

    // 2. Fetch page HTML with safeFetch
    let response;
    try {
      response = await safeFetch(rawUrl, {
        headers: {
          "User-Agent":
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 MediaDock/1.0",
          Accept: "text/html,application/xhtml+xml,application/xml;q=0.9,image/*,*/*;q=0.8",
        },
        timeoutMs: 12000,
        maxBytes: 3 * 1024 * 1024, // 3MB HTML ceiling
      });
    } catch (err: any) {
      throw new ApiException(
        ErrorCode.EXTRACTION_FAILED,
        `Failed to reach target webpage: ${err?.message ?? "Network error"}`,
      );
    }

    const contentType = (response.headers["content-type"] ?? "").toLowerCase();

    // Check if the server answered with an image content-type directly
    if (contentType.startsWith("image/")) {
      const ext = contentType.split("/")[1]?.split(";")[0]?.replace("jpeg", "jpg") || "jpg";
      return {
        url: rawUrl,
        siteTitle: "Direct Media",
        totalFound: 1,
        images: [
          {
            id: "img_0",
            url: rawUrl,
            title: "Direct Media",
            format: ext,
            source: "direct",
          },
        ],
      };
    }

    const html = await response.text();
    const $ = cheerio.load(html);
    const siteTitle = $("title").first().text().trim() || $("meta[property='og:title']").attr("content") || "Webpage Images";

    const seenUrls = new Set<string>();
    const images: ExtractedImageItem[] = [];

    const addImage = (candidateUrl: string | undefined | null, source: ExtractedImageItem["source"], extra: Partial<ExtractedImageItem> = {}) => {
      if (!candidateUrl) return;
      const cleanUrl = this.sanitizeUrl(candidateUrl, rawUrl);
      if (!cleanUrl) return;
      if (seenUrls.has(cleanUrl)) return;
      if (this.isTrackingImage(cleanUrl, extra.width, extra.height)) return;

      seenUrls.add(cleanUrl);
      const format = extra.format ?? this.detectFormatFromUrl(cleanUrl) ?? "jpg";
      images.push({
        id: `img_${images.length + 1}`,
        url: cleanUrl,
        title: extra.title || extra.alt || siteTitle,
        alt: extra.alt,
        width: extra.width,
        height: extra.height,
        format,
        source,
      });
    };

    // 1. Open Graph
    $('meta[property="og:image"], meta[property="og:image:url"], meta[property="og:image:secure_url"]').each((_, el) => {
      addImage($(el).attr("content"), "og", { title: siteTitle });
    });

    // 2. Twitter Cards
    $('meta[name="twitter:image"], meta[name="twitter:image:src"]').each((_, el) => {
      addImage($(el).attr("content"), "twitter", { title: siteTitle });
    });

    // 3. JSON-LD Structured Data
    $('script[type="application/ld+json"]').each((_, el) => {
      try {
        const text = $(el).contents().text();
        if (!text) return;
        const data = JSON.parse(text);
        const parseJsonLdItem = (item: any) => {
          if (!item) return;
          if (typeof item === "string" && (item.startsWith("http://") || item.startsWith("https://"))) {
            addImage(item, "jsonld");
          } else if (Array.isArray(item)) {
            item.forEach(parseJsonLdItem);
          } else if (typeof item === "object") {
            if (item.image) parseJsonLdItem(item.image);
            if (item.thumbnailUrl) parseJsonLdItem(item.thumbnailUrl);
            if (item.url && item["@type"] === "ImageObject") addImage(item.url, "jsonld", { width: item.width, height: item.height });
            if (item["@graph"] && Array.isArray(item["@graph"])) item["@graph"].forEach(parseJsonLdItem);
          }
        };
        parseJsonLdItem(data);
      } catch {
        // ignore malformed JSON-LD
      }
    });

    // 4. Picture source tags
    $("picture source[srcset]").each((_, el) => {
      const srcset = $(el).attr("srcset") ?? "";
      const best = this.parseSrcSetBest(srcset, rawUrl);
      if (best) addImage(best, "img");
    });

    // 5. Standard img tags
    $("img").each((_, el) => {
      const srcset = $(el).attr("srcset");
      if (srcset) {
        const best = this.parseSrcSetBest(srcset, rawUrl);
        if (best) {
          const width = parseInt($(el).attr("width") ?? "", 10) || undefined;
          const height = parseInt($(el).attr("height") ?? "", 10) || undefined;
          addImage(best, "img", {
            alt: $(el).attr("alt")?.trim(),
            width,
            height,
          });
        }
      }

      const src = $(el).attr("src") || $(el).attr("data-src") || $(el).attr("data-original") || $(el).attr("data-lazy-src");
      const width = parseInt($(el).attr("width") ?? "", 10) || undefined;
      const height = parseInt($(el).attr("height") ?? "", 10) || undefined;
      addImage(src, "img", {
        alt: $(el).attr("alt")?.trim(),
        width,
        height,
      });
    });

    return {
      url: rawUrl,
      siteTitle,
      totalFound: images.length,
      images: images.slice(0, 100), // Cap at 100 images per page
    };
  }
}
