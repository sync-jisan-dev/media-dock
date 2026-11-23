import { Global, Module } from "@nestjs/common";
import { BinariesService } from "./binaries.service";
import { DirectDownloadService } from "./direct-download.service";
import { ExtractionService } from "./extraction.service";
import { MediaCache } from "./media-cache";
import { TikTokEmbedProvider } from "./providers/tiktok-embed.provider";
import { TikLiveApiProvider } from "./providers/tikliveapi.provider";
import { CobaltProvider } from "./providers/cobalt.provider";
import { YtdlpService } from "./ytdlp.service";
import { ApifyDownloaderService } from "./providers/apify-downloader.service";

@Global()
@Module({
  providers: [
    BinariesService,
    YtdlpService,
    DirectDownloadService,
    ApifyDownloaderService,
    TikTokEmbedProvider,
    TikLiveApiProvider,
    CobaltProvider,
    MediaCache,
    ExtractionService,
  ],
  exports: [BinariesService, YtdlpService, ExtractionService, MediaCache],
})
export class EngineModule {}
