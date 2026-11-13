import { Injectable, Logger } from "@nestjs/common";

export interface NeonAuthUser {
  id: string;
  email?: string;
  name?: string;
  [key: string]: unknown;
}

@Injectable()
export class NeonAuthService {
  private readonly logger = new Logger(NeonAuthService.name);
  private jwks: any = null;
  private issuer: string | undefined;

  getAuthConfig() {
    return {
      enabled: Boolean(process.env.NEON_AUTH_JWKS_URL),
      baseUrl: process.env.NEON_AUTH_BASE_URL || null,
      jwksUrl: process.env.NEON_AUTH_JWKS_URL || null,
    };
  }

  async verifyToken(token: string): Promise<NeonAuthUser | null> {
    const jwksUrl = process.env.NEON_AUTH_JWKS_URL;
    if (!jwksUrl) return null;

    try {
      const { createRemoteJWKSet, jwtVerify } = await import("jose");
      if (!this.jwks) {
        this.jwks = createRemoteJWKSet(new URL(jwksUrl));
      }
      if (process.env.NEON_AUTH_BASE_URL && !this.issuer) {
        this.issuer = new URL(process.env.NEON_AUTH_BASE_URL).origin;
      }
      const options = this.issuer ? { issuer: this.issuer } : undefined;
      const { payload } = await jwtVerify(token, this.jwks, options);

      if (!payload.sub) {
        return null;
      }

      return {
        id: payload.sub,
        email: typeof payload.email === "string" ? payload.email : undefined,
        name: typeof payload.name === "string" ? payload.name : undefined,
        ...payload,
      };
    } catch {
      return null;
    }
  }
}
