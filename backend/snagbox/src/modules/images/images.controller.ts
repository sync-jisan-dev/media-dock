import { Body, Controller, Get, HttpStatus, Post, Query, UseGuards, VERSION_NEUTRAL } from "@nestjs/common";
import { ApiOperation, ApiQuery, ApiResponse, ApiTags } from "@nestjs/swagger";
import { Throttle } from "@nestjs/throttler";
import { Public } from "../../common/decorators/public.decorator";
import { AppAuthGuard } from "../app-api/app-auth.guard";
import { ImageExtractorService } from "./image-extractor.service";
import { ImageDownloadService } from "./image-download.service";
import { DownloadImageDto } from "./dto/download-image.dto";

@ApiTags("Mobile App - Images")
@Controller({ path: "app/v1/images", version: VERSION_NEUTRAL })
@Public() // Bypasses Snagbox ApiKeyGuard so AppAuthGuard handles it
@UseGuards(AppAuthGuard)
export class ImagesController {
  constructor(
    private readonly extractor: ImageExtractorService,
    private readonly downloader: ImageDownloadService,
  ) {}

  @Get("extract")
  @Throttle({ default: { limit: 15, ttl: 60_000 } })
  @ApiOperation({ summary: "Extract images and metadata from a public webpage" })
  @ApiQuery({ name: "url", required: true, description: "Target webpage URL" })
  @ApiResponse({ status: HttpStatus.OK, description: "Extracted image list" })
  @ApiResponse({ status: HttpStatus.BAD_REQUEST, description: "Invalid URL or extraction failed" })
  async extract(@Query("url") url: string) {
    return this.extractor.extract(url);
  }

  @Post("download")
  @Throttle({ default: { limit: 20, ttl: 60_000 } })
  @ApiOperation({ summary: "Download an image safely and get signed file delivery link" })
  @ApiResponse({ status: HttpStatus.OK, description: "Completed download job with signed URL" })
  @ApiResponse({ status: HttpStatus.BAD_REQUEST, description: "Invalid image or format error" })
  async download(@Body() dto: DownloadImageDto) {
    return this.downloader.download(dto);
  }
}
