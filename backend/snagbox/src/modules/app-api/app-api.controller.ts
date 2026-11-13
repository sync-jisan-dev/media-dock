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
import { NeonAuthService } from "./neon-auth.service";
import { NeonRealtimeService } from "./neon-realtime.service";
import { NeonAiService } from "./neon-ai.service";
import { NeonFunctionsService } from "./neon-functions.service";

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
    private readonly neonAuth: NeonAuthService,
    private readonly neonRealtime: NeonRealtimeService,
    private readonly neonAi: NeonAiService,
    private readonly neonFunctions: NeonFunctionsService,
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

  // --- Neon Integration Endpoints ---

  @Get("neon/services")
  @ApiOperation({ summary: "Check status of all provisioned Neon cloud services" })
  getNeonServices() {
    return {
      status: "operational",
      branch: process.env.NEON_BRANCH || "production",
      branchId: process.env.NEON_BRANCH_ID || null,
      services: {
        postgres: {
          configured: Boolean(process.env.DATABASE_URL),
          host: "ep-summer-scene-be1iseuv.c-9.us-east-2.aws.neon.tech",
          pooled: Boolean(process.env.DATABASE_URL?.includes("-pooler")),
        },
        auth: this.neonAuth.getAuthConfig(),
        realtime: this.neonRealtime.getRealtimeConfig(),
        aiGateway: this.neonAi.getAiConfig(),
        functions: {
          configured: this.neonFunctions.isConfigured(),
          helloUrl: this.neonFunctions.isConfigured()
            ? this.neonFunctions.getFunctionUrl("hello")
            : null,
        },
      },
    };
  }

  @Get("auth/config")
  @ApiOperation({ summary: "Get Neon Auth configuration" })
  getAuthConfig() {
    return this.neonAuth.getAuthConfig();
  }

  @Post("realtime/downloads/live")
  @UseGuards(AppAuthGuard)
  @ApiOperation({ summary: "Get sealed live query for Neon Realtime downloads stream" })
  async getLiveDownloadsQuery() {
    return this.neonRealtime.sealDownloadsQuery();
  }

  @Get("ai/models")
  @UseGuards(AppAuthGuard)
  @ApiOperation({ summary: "List available models on the Neon AI Gateway" })
  async getAiModels() {
    return this.neonAi.listModels();
  }

  @Post("ai/enhance")
  @UseGuards(AppAuthGuard)
  @ApiOperation({ summary: "Enhance media metadata using Neon AI Gateway" })
  async enhanceMetadata(@Body() body: { title: string; platform?: string }) {
    return this.neonAi.enhanceMediaMetadata(body.title, body.platform);
  }

  @Get("functions/hello")
  @ApiOperation({ summary: "Invoke the deployed Neon serverless Hello function" })
  async invokeHelloFunction() {
    return this.neonFunctions.invokeFunction("hello");
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
