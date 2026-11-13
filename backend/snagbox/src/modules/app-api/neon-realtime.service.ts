import { Injectable, Logger } from "@nestjs/common";

export interface LiveDownloadsSealedQuery {
  realtimeUrl: string;
  sealedQuery: unknown;
  db: string;
}

@Injectable()
export class NeonRealtimeService {
  private readonly logger = new Logger(NeonRealtimeService.name);
  private realtimeInstance: any = null;
  private readonly secret = process.env.NEON_REALTIME_SECRET;
  private readonly realtimeUrl = process.env.NEON_REALTIME_URL;

  isConfigured(): boolean {
    return Boolean(this.secret && this.realtimeUrl);
  }

  getRealtimeConfig() {
    return {
      enabled: this.isConfigured(),
      realtimeUrl: this.realtimeUrl || null,
    };
  }

  private async getRealtime() {
    if (!this.realtimeInstance && this.secret) {
      const { createRealtime } = await import("@neon/realtime/server");
      this.realtimeInstance = createRealtime({
        secret: this.secret,
        db: "neondb",
      });
    }
    return this.realtimeInstance;
  }

  async sealDownloadsQuery(): Promise<LiveDownloadsSealedQuery> {
    if (!this.secret || !this.realtimeUrl) {
      throw new Error("Neon Realtime is not configured (missing NEON_REALTIME_SECRET or NEON_REALTIME_URL).");
    }

    const { rawSql } = await import("@neon/realtime/server");
    const realtime = await this.getRealtime();

    const query = rawSql(
      `SELECT "id", "status", "progress", "title", "platform" FROM "Download" ORDER BY "createdAt" DESC LIMIT 20`
    );

    const sealed = await realtime.seal({ query });
    return {
      realtimeUrl: this.realtimeUrl,
      db: "neondb",
      sealedQuery: sealed,
    };
  }
}
