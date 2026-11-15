import { Global, Module } from "@nestjs/common";
import { BinariesService } from "./binaries.service";
import { DirectDownloadService } from "./direct-download.service";
import { ExtractionService } from "./extraction.service";
import { MediaCache } from "./media-cache";
import { TikTokEmbedProvider } from "./providers/tiktok-embed.provider";
import { CobaltProvider } from "./providers/cobalt.provider";
import { YtdlpService } from "./ytdlp.service";

@Global()
@Module({
  providers: [
    BinariesService,
    YtdlpService,
    DirectDownloadService,
    TikTokEmbedProvider,
    CobaltProvider,
    MediaCache,
    ExtractionService,
  ],
  exports: [BinariesService, YtdlpService, ExtractionService, MediaCache],
})
export class EngineModule {}
