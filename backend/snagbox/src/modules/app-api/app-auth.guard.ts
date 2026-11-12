import { CanActivate, ExecutionContext, HttpStatus, Injectable } from "@nestjs/common";
import { Reflector } from "@nestjs/core";
import type { Request } from "express";
import { ApiException, ErrorCode } from "../../common/errors";
import { IS_PUBLIC_KEY } from "../../common/decorators/public.decorator";
import { ApiKeyService } from "../../auth/api-key.service";
import { GuestSessionPayload, GuestSessionService } from "./guest-session.service";

export interface MobileAppRequest extends Request {
  guestSession?: GuestSessionPayload;
  apiKey?: any;
}

@Injectable()
export class AppAuthGuard implements CanActivate {
  constructor(
    private readonly reflector: Reflector,
    private readonly guestSessions: GuestSessionService,
    private readonly apiKeys: ApiKeyService,
  ) {}

  async canActivate(context: ExecutionContext): Promise<boolean> {
    const isPublic = this.reflector.getAllAndOverride<boolean>(IS_PUBLIC_KEY, [
      context.getHandler(),
      context.getClass(),
    ]);
    if (isPublic) return true;

    const request = context.switchToHttp().getRequest<MobileAppRequest>();

    // 1. Check Bearer or X-Guest-Token
    const authHeader = request.header("authorization") ?? "";
    const guestTokenHeader = request.header("x-guest-token") ?? "";
    const token = guestTokenHeader || authHeader.replace(/^Bearer\s+/i, "").trim();

    if (token) {
      const session = this.guestSessions.validateToken(token);
      if (session) {
        const remaining = this.guestSessions.consumeQuota(session.sessionId, session.quota);
        if (remaining < 0) {
          throw new ApiException(
            ErrorCode.RATE_LIMITED,
            "Guest session quota exceeded. Please start a new session.",
            HttpStatus.TOO_MANY_REQUESTS,
          );
        }
        request.guestSession = session;
        return true;
      }

      // Check if the token happens to be an API key instead
      const key = await this.apiKeys.validate(token);
      if (key) {
        request.apiKey = key;
        return true;
      }
    }

    // 2. Check X-API-Key header
    const rawApiKey = request.header("x-api-key");
    if (rawApiKey) {
      const key = await this.apiKeys.validate(rawApiKey);
      if (key) {
        request.apiKey = key;
        return true;
      }
    }

    throw new ApiException(
      ErrorCode.INVALID_API_KEY,
      "Unauthorized: Valid guest session token or API key is required.",
      HttpStatus.UNAUTHORIZED,
    );
  }
}
