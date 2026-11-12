import { ExecutionContext, HttpStatus } from "@nestjs/common";
import { Reflector } from "@nestjs/core";
import { AppAuthGuard } from "./app-auth.guard";
import { GuestSessionService } from "./guest-session.service";
import { ApiKeyService } from "../../auth/api-key.service";
import { ApiException } from "../../common/errors";

describe("AppAuthGuard", () => {
  let guard: AppAuthGuard;
  let reflector: Reflector;
  let guestSessions: GuestSessionService;
  let apiKeys: jest.Mocked<ApiKeyService>;

  beforeEach(() => {
    reflector = new Reflector();
    guestSessions = {
      validateToken: jest.fn(),
      consumeQuota: jest.fn().mockReturnValue(10),
    } as unknown as GuestSessionService;

    apiKeys = {
      validate: jest.fn().mockResolvedValue(null),
    } as unknown as jest.Mocked<ApiKeyService>;

    guard = new AppAuthGuard(reflector, guestSessions, apiKeys);
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
