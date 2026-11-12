import { Injectable, Logger } from "@nestjs/common";
import { randomUUID } from "crypto";
import * as fs from "fs/promises";
import { join } from "path";
import { assertPublicHttpUrl, safeFetch } from "../../common/http/safe-fetch";
import { ApiException, ErrorCode } from "../../common/errors";
import { AppConfigService } from "../../config/app-config.service";
import { PrismaService } from "../../prisma/prisma.service";
import { fileLinkPath } from "../../files/download-link";
import { toDownloadJob } from "../../downloads/present";
import { DownloadImageDto } from "./dto/download-image.dto";

const MAX_IMAGE_BYTES = 25 * 1024 * 1024; // 25 MB max image size

@Injectable()
export class ImageDownloadService {
  private readonly logger = new Logger(ImageDownloadService.name);

  constructor(
    private readonly config: AppConfigService,
    private readonly prisma: PrismaService,
  ) {}

  private verifyImageMagicBytes(buffer: Buffer): string | null {
    if (buffer.length < 12) return null;

    // JPEG: FF D8 FF
    if (buffer[0] === 0xff && buffer[1] === 0xd8 && buffer[2] === 0xff) {
      return "jpg";
    }

    // PNG: 89 50 4E 47 0D 0A 1A 0A
    if (
      buffer[0] === 0x89 &&
      buffer[1] === 0x50 &&
      buffer[2] === 0x4e &&
      buffer[3] === 0x47 &&
      buffer[4] === 0x0d &&
      buffer[5] === 0x0a &&
      buffer[6] === 0x1a &&
      buffer[7] === 0x0a
    ) {
      return "png";
    }

    // GIF: GIF87a or GIF89a
    const header6 = buffer.subarray(0, 6).toString("ascii");
    if (header6 === "GIF87a" || header6 === "GIF89a") {
      return "gif";
    }

    // WEBP: RIFF....WEBP
    const riff = buffer.subarray(0, 4).toString("ascii");
    const webp = buffer.subarray(8, 12).toString("ascii");
    if (riff === "RIFF" && webp === "WEBP") {
      return "webp";
    }

    return null;
  }

  async download(dto: DownloadImageDto) {
    assertPublicHttpUrl(dto.url);

    let response;
    try {
      response = await safeFetch(dto.url, {
        headers: {
          "User-Agent":
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 MediaDock/1.0",
          Accept: "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8",
        },
        timeoutMs: 20000,
        maxBytes: MAX_IMAGE_BYTES,
      });
    } catch (err: any) {
      throw new ApiException(
        ErrorCode.EXTRACTION_FAILED,
        `Failed to download image from source: ${err?.message ?? "Network error"}`,
      );
    }

    // Read full response stream into a Buffer
    const chunks: Buffer[] = [];
    for await (const chunk of response.body) {
      chunks.push(typeof chunk === "string" ? Buffer.from(chunk) : chunk);
    }
    const buffer = Buffer.concat(chunks);

    if (buffer.length === 0) {
      throw new ApiException(ErrorCode.EXTRACTION_FAILED, "Remote server returned an empty image body.");
    }

    // Verify format via magic bytes
    let detectedExt = this.verifyImageMagicBytes(buffer);
    if (!detectedExt) {
      // Fallback to Content-Type if magic byte check is strict
      const contentType = (response.headers["content-type"] ?? "").toLowerCase();
      if (contentType.includes("jpeg") || contentType.includes("jpg")) detectedExt = "jpg";
      else if (contentType.includes("png")) detectedExt = "png";
      else if (contentType.includes("webp")) detectedExt = "webp";
      else if (contentType.includes("gif")) detectedExt = "gif";
      else {
        throw new ApiException(
          ErrorCode.INVALID_URL,
          "The provided URL did not return a supported image format (JPEG, PNG, WEBP, GIF).",
        );
      }
    }

    const ext = dto.format?.toLowerCase() || detectedExt;
    const sanitizedTitle = (dto.title || "image")
      .replace(/[^a-zA-Z0-9_\-\s]/g, "")
      .trim()
      .slice(0, 50) || "image";

    const filename = `mediadock_${sanitizedTitle}_${Date.now()}_${randomUUID().slice(0, 6)}.${ext}`;
    const downloadDir = this.config.downloadsDir;
    await fs.mkdir(downloadDir, { recursive: true });
    const localFilePath = join(downloadDir, filename);

    await fs.writeFile(localFilePath, buffer);

    const expiresAt = new Date(Date.now() + this.config.fileTtlMinutes * 60 * 1000);

    const record = await this.prisma.download.create({
      data: {
        url: dto.url,
        platform: "generic",
        kind: "image",
        status: "COMPLETED",
        title: dto.title || sanitizedTitle,
        filename,
        fileSizeBytes: BigInt(buffer.length),
        quality: "original",
        format: ext,
        progress: 100,
        completedAt: new Date(),
        expiresAt,
      },
    });

    const job = toDownloadJob(record, this.config.fileLinkSecret, this.config.signedUrlTtlSeconds);
    return job;
  }
}
