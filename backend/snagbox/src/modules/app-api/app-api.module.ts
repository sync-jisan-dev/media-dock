import { forwardRef, Module } from "@nestjs/common";
import { AppConfigModule } from "../../config/config.module";
import { AuthModule } from "../../auth/auth.module";
import { MediaModule } from "../../media/media.module";
import { DownloadsModule } from "../../downloads/downloads.module";
import { AppApiController } from "./app-api.controller";
import { GuestSessionService } from "./guest-session.service";
import { AppAuthGuard } from "./app-auth.guard";
import { NeonAuthService } from "./neon-auth.service";
import { NeonRealtimeService } from "./neon-realtime.service";
import { NeonAiService } from "./neon-ai.service";
import { NeonFunctionsService } from "./neon-functions.service";

@Module({
  imports: [
    AppConfigModule,
    forwardRef(() => AuthModule),
    forwardRef(() => MediaModule),
    forwardRef(() => DownloadsModule),
  ],
  controllers: [AppApiController],
  providers: [
    GuestSessionService,
    AppAuthGuard,
    NeonAuthService,
    NeonRealtimeService,
    NeonAiService,
    NeonFunctionsService,
  ],
  exports: [
    GuestSessionService,
    AppAuthGuard,
    NeonAuthService,
    NeonRealtimeService,
    NeonAiService,
    NeonFunctionsService,
  ],
})
export class AppApiModule {}
