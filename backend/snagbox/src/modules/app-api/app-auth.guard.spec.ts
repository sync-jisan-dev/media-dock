import { ExecutionContext, HttpStatus } from "@nestjs/common";
import { Reflector } from "@nestjs/core";
import { AppAuthGuard } from "./app-auth.guard";
import { GuestSessionService } from "./guest-session.service";
import { ApiKeyService } from "../../auth/api-key.service";
import { NeonAuthService } from "./neon-auth.service";
import { ApiException } from "../../common/errors";

describe("AppAuthGuard", () => {
  let guard: AppAuthGuard;
  let reflector: Reflector;
  let guestSessions: GuestSessionService;
  let apiKeys: jest.Mocked<ApiKeyService>;
  let neonAuth: jest.Mocked<NeonAuthService>;

  beforeEach(() => {
    reflector = new Reflector();
    guestSessions = {
      validateToken: jest.fn(),
      consumeQuota: jest.fn().mockReturnValue(10),
    } as unknown as GuestSessionService;

    apiKeys = {
      validate: jest.fn().mockResolvedValue(null),
    } as unknown as jest.Mocked<ApiKeyService>;

    neonAuth = {
      verifyToken: jest.fn().mockResolvedValue(null),
      getAuthConfig: jest.fn(),
    } as unknown as jest.Mocked<NeonAuthService>;

    guard = new AppAuthGuard(reflector, guestSessions, apiKeys, neonAuth);
  });

  const createMockContext = (headers: Record<string, string>): ExecutionContext => {
    const request = {
      header: (name: string) => headers[name.toLowerCase()],
    };
    return {
      getHandler: () => ({}),
      getClass: () => ({}),
      switchToHttp: () => ({
        getRequest: () => request,
      }),
    } as unknown as ExecutionContext;
  };

  it("should allow request with valid guest session token in Authorization header", async () => {
    (guestSessions.validateToken as jest.Mock).mockReturnValue({
      sessionId: "session-123",
      quota: 50,
      expiresAt: Math.floor(Date.now() / 1000) + 3600,
    });

    const context = createMockContext({ authorization: "Bearer valid.token.here" });
    const result = await guard.canActivate(context);

    expect(result).toBe(true);
    expect(guestSessions.consumeQuota).toHaveBeenCalledWith("session-123", 50);
  });

  it("should allow request with valid Neon Auth JWT token", async () => {
    (guestSessions.validateToken as jest.Mock).mockReturnValue(null);
    (neonAuth.verifyToken as jest.Mock).mockResolvedValue({
      id: "usr_neon_123",
      email: "neonuser@example.com",
    });

    const context = createMockContext({ authorization: "Bearer neon.jwt.token" });
    const result = await guard.canActivate(context);

    expect(result).toBe(true);
    expect(neonAuth.verifyToken).toHaveBeenCalledWith("neon.jwt.token");
  });

  it("should throw UNAUTHORIZED when no token is supplied", async () => {
    const context = createMockContext({});
    await expect(guard.canActivate(context)).rejects.toThrow(ApiException);
  });

  it("should throw TOO_MANY_REQUESTS when quota is exhausted", async () => {
    (guestSessions.validateToken as jest.Mock).mockReturnValue({
      sessionId: "session-exhausted",
      quota: 50,
      expiresAt: Math.floor(Date.now() / 1000) + 3600,
    });
    (guestSessions.consumeQuota as jest.Mock).mockReturnValue(-1);

    const context = createMockContext({ authorization: "Bearer exhausted.token" });
    await expect(guard.canActivate(context)).rejects.toThrow(ApiException);
  });
});
