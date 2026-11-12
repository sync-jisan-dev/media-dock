import { forwardRef, Module } from "@nestjs/common";
import { AppConfigModule } from "../../config/config.module";
import { AuthModule } from "../../auth/auth.module";
import { MediaModule } from "../../media/media.module";
import { DownloadsModule } from "../../downloads/downloads.module";
import { AppApiController } from "./app-api.controller";
import { GuestSessionService } from "./guest-session.service";
import { AppAuthGuard } from "./app-auth.guard";

@Module({
  imports: [
    AppConfigModule,
    forwardRef(() => AuthModule),
    forwardRef(() => MediaModule),
    forwardRef(() => DownloadsModule),
  ],
  controllers: [AppApiController],
  providers: [GuestSessionService, AppAuthGuard],
  exports: [GuestSessionService, AppAuthGuard],
})
export class AppApiModule {}
