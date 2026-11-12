import { Injectable, Logger } from "@nestjs/common";
import { createHmac, randomUUID, timingSafeEqual } from "crypto";
import { AppConfigService } from "../../config/app-config.service";

export interface GuestSessionPayload {
  sessionId: string;
  createdAt: number; // Unix timestamp in seconds
  expiresAt: number; // Unix timestamp in seconds
  quota: number;     // Total operations allowed per session
}

export interface GuestSessionInfo extends GuestSessionPayload {
  token: string;
  quotaRemaining: number;
}

@Injectable()
export class GuestSessionService {
  private readonly logger = new Logger(GuestSessionService.name);
  private readonly sessionQuotas = new Map<string, number>();
  private readonly revokedSessions = new Set<string>();

  constructor(private readonly config: AppConfigService) {}

  private get secret(): string {
    return this.config.fileLinkSecret;
  }

  /**
   * Generates a new cryptographically signed guest session token.
   * Default session validity: 24 hours (86,400 seconds).
   * Default operation quota: 100 operations.
   */
  createSession(quota = 100, ttlSeconds = 86400): GuestSessionInfo {
    const nowSec = Math.floor(Date.now() / 1000);
    const sessionId = randomUUID();
    const expiresAt = nowSec + ttlSeconds;

    const payload: GuestSessionPayload = {
      sessionId,
      createdAt: nowSec,
      expiresAt,
      quota,
    };

    const payloadB64 = Buffer.from(JSON.stringify(payload)).toString("base64url");
    const signature = createHmac("sha256", this.secret).update(payloadB64).digest("base64url");
    const token = `${payloadB64}.${signature}`;

    this.sessionQuotas.set(sessionId, quota);

    return {
      ...payload,
      token,
      quotaRemaining: quota,
    };
  }

  /**
   * Verifies the authenticity and expiration of a guest token.
   */
  validateToken(token: string): GuestSessionPayload | null {
    if (!token || typeof token !== "string") return null;
    const parts = token.split(".");
    if (parts.length !== 2) return null;

    const [payloadB64, signature] = parts;
    const expectedSig = createHmac("sha256", this.secret).update(payloadB64).digest("base64url");

    const left = Buffer.from(signature);
    const right = Buffer.from(expectedSig);
    if (left.length !== right.length || !timingSafeEqual(left, right)) {
      return null;
    }

    try {
      const payloadJson = Buffer.from(payloadB64, "base64url").toString("utf-8");
      const payload = JSON.parse(payloadJson) as GuestSessionPayload;

      const nowSec = Math.floor(Date.now() / 1000);
      if (payload.expiresAt <= nowSec) {
        return null;
      }

      if (this.revokedSessions.has(payload.sessionId)) {
        return null;
      }

      return payload;
    } catch {
      return null;
    }
  }

  /**
   * Consumes 1 credit from the session's quota.
   * Returns remaining quota, or -1 if exhausted.
   */
  consumeQuota(sessionId: string, initialQuota = 100): number {
    const current = this.sessionQuotas.has(sessionId)
      ? this.sessionQuotas.get(sessionId)!
      : initialQuota;

    if (current <= 0) return -1;
    const next = current - 1;
    this.sessionQuotas.set(sessionId, next);
    return next;
  }

  /**
   * Revokes a session immediately.
   */
  revokeSession(sessionId: string): void {
    this.revokedSessions.add(sessionId);
    this.sessionQuotas.delete(sessionId);
  }
}
