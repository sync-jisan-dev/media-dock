import {
  Body,
  Controller,
  Get,
  HttpCode,
  HttpStatus,
  Param,
  ParseUUIDPipe,
  Post,
  Req,
  Sse,
  UseGuards,
  VERSION_NEUTRAL,
} from "@nestjs/common";
import { ApiOperation, ApiParam, ApiResponse, ApiTags } from "@nestjs/swagger";
import { Throttle } from "@nestjs/throttler";
import { Observable, endWith, ignoreElements, interval, map, merge, takeUntil } from "rxjs";
import type { JobEvent } from "@snagbox/shared";
import { Public } from "../../common/decorators/public.decorator";
import { MediaService } from "../../media/media.service";
import { DownloadsService } from "../../downloads/downloads.service";
import { AppConfigService } from "../../config/app-config.service";
import { AppAuthGuard, MobileAppRequest } from "./app-auth.guard";
import { GuestSessionService } from "./guest-session.service";
import { InspectUrlDto } from "./dto/inspect-url.dto";
import { CreateAppDownloadDto } from "./dto/create-app-download.dto";
import { CreateAppBatchDto } from "./dto/create-app-batch.dto";

interface SseMessage {
  data: string;
}

@ApiTags("Mobile App - Gateway")
@Controller({ path: "app/v1", version: VERSION_NEUTRAL })
@Public() // Bypasses Snagbox ApiKeyGuard; AppAuthGuard handles mobile authorization
export class AppApiController {
  constructor(
    private readonly guestSessions: GuestSessionService,
    private readonly media: MediaService,
    private readonly downloads: DownloadsService,
    private readonly config: AppConfigService,
  ) {}

  @Post("sessions/guest")
  @Throttle({ default: { limit: 10, ttl: 60_000 } })
  @ApiOperation({ summary: "Create a guest session for the mobile application" })
  @ApiResponse({ status: HttpStatus.OK, description: "Guest session token with quota" })
  createGuestSession() {
    return this.guestSessions.createSession();
  }

  @Post("media/inspect")
  @UseGuards(AppAuthGuard)
  @Throttle({ default: { limit: 20, ttl: 60_000 } })
  @ApiOperation({ summary: "Inspect a media URL and return available formats and metadata" })
  @ApiResponse({ status: HttpStatus.OK, description: "Normalized media information" })
  @ApiResponse({ status: HttpStatus.BAD_REQUEST, description: "Invalid or unsupported URL" })
  async inspect(@Body() dto: InspectUrlDto) {
    return this.media.getInfo(dto.url);
  }

  @Post("downloads")
  @UseGuards(AppAuthGuard)
  @HttpCode(HttpStatus.ACCEPTED)
  @Throttle({ default: { limit: 15, ttl: 60_000 } })
  @ApiOperation({ summary: "Queue a media download job (video, audio, gif)" })
  @ApiResponse({ status: HttpStatus.ACCEPTED, description: "Download job queued" })
  async createDownload(@Body() dto: CreateAppDownloadDto, @Req() req: MobileAppRequest) {
    return this.downloads.create(dto, { apiKey: req.apiKey });
  }

  @Post("downloads/batch")
  @UseGuards(AppAuthGuard)
  @HttpCode(HttpStatus.ACCEPTED)
  @Throttle({ default: { limit: 5, ttl: 60_000 } })
  @ApiOperation({ summary: "Queue multiple download jobs in batch" })
  @ApiResponse({ status: HttpStatus.ACCEPTED, description: "Array of queued download jobs" })
  async createBatch(@Body() dto: CreateAppBatchDto, @Req() req: MobileAppRequest) {
    return this.downloads.createBatch(dto, { apiKey: req.apiKey });
  }

  @Get("downloads/:id")
  @UseGuards(AppAuthGuard)
  @ApiOperation({ summary: "Get current status and metadata of a download job" })
  @ApiParam({ name: "id", format: "uuid" })
  @ApiResponse({ status: HttpStatus.OK, description: "Download job details" })
  @ApiResponse({ status: HttpStatus.NOT_FOUND, description: "Job not found" })
  async getDownload(@Param("id", ParseUUIDPipe) id: string) {
    return this.downloads.get(id);
  }

  @Sse("downloads/:id/events")
  @ApiOperation({ summary: "Live Server-Sent Events stream for download progress" })
  @ApiParam({ name: "id", format: "uuid" })
  events(@Param("id", ParseUUIDPipe) id: string): Observable<SseMessage> {
    const events$ = this.downloads
      .events$(id)
      .pipe(map((event: JobEvent): SseMessage => ({ data: JSON.stringify(event) })));
    const done$ = events$.pipe(ignoreElements(), endWith(null));
    const heartbeat$ = interval(15_000).pipe(
      map((): SseMessage => ({ data: '{"type":"ping"}' })),
      takeUntil(done$),
    );
    return merge(events$, heartbeat$);
  }

  @Post("downloads/:id/cancel")
  @UseGuards(AppAuthGuard)
  @ApiOperation({ summary: "Cancel a queued or active download job" })
  @ApiParam({ name: "id", format: "uuid" })
  @ApiResponse({ status: HttpStatus.OK, description: "Job cancelled successfully" })
  async cancelDownload(@Param("id", ParseUUIDPipe) id: string) {
    return this.downloads.cancel(id);
  }

  @Get("health")
  @ApiOperation({ summary: "Mobile gateway health probe" })
  @ApiResponse({ status: HttpStatus.OK, description: "Mobile gateway is operational" })
  health() {
    return {
      status: "ok",
      service: "MediaDock App Gateway",
      timestamp: new Date().toISOString(),
      version: "1.0.0",
    };
  }
}
