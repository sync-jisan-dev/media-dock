import { Module } from "@nestjs/common";
import { AppConfigModule } from "../../config/config.module";
import { PrismaModule } from "../../prisma/prisma.module";
import { AuthModule } from "../../auth/auth.module";
import { AppApiModule } from "../app-api/app-api.module";
import { ImageExtractorService } from "./image-extractor.service";
import { ImageDownloadService } from "./image-download.service";
import { ImagesController } from "./images.controller";

@Module({
  imports: [AppConfigModule, PrismaModule, AuthModule, AppApiModule],
  controllers: [ImagesController],
  providers: [ImageExtractorService, ImageDownloadService],
  exports: [ImageExtractorService, ImageDownloadService],
})
export class ImagesModule {}
