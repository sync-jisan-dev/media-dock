import { GuestSessionService } from "./guest-session.service";
import { AppConfigService } from "../../config/app-config.service";

describe("GuestSessionService", () => {
  let service: GuestSessionService;
  let mockConfig: Partial<AppConfigService>;

  beforeEach(() => {
    mockConfig = {
      fileLinkSecret: "super-secret-test-key-32-chars-long!",
    };
    service = new GuestSessionService(mockConfig as AppConfigService);
  });

  it("should create a valid cryptographically signed guest session", () => {
    const session = service.createSession(50, 3600);
    expect(session).toBeDefined();
    expect(session.sessionId).toMatch(/^[0-9a-f-]{36}$/);
    expect(session.token).toContain(".");
    expect(session.quotaRemaining).toBe(50);
  });

  it("should validate a valid guest session token", () => {
    const session = service.createSession(20, 3600);
    const validated = service.validateToken(session.token);

    expect(validated).not.toBeNull();
    expect(validated?.sessionId).toBe(session.sessionId);
    expect(validated?.quota).toBe(20);
  });

  it("should reject tampered or corrupted token", () => {
    const session = service.createSession(20, 3600);
    const tampered = session.token.slice(0, -4) + "XXXX";
    const validated = service.validateToken(tampered);

    expect(validated).toBeNull();
  });

  it("should reject an expired token", () => {
    // Session that expired 10 seconds ago
    const session = service.createSession(20, -10);
    const validated = service.validateToken(session.token);

    expect(validated).toBeNull();
  });

  it("should track quota consumption and exhaust correctly", () => {
    const session = service.createSession(2, 3600);
    expect(service.consumeQuota(session.sessionId)).toBe(1);
    expect(service.consumeQuota(session.sessionId)).toBe(0);
    expect(service.consumeQuota(session.sessionId)).toBe(-1);
  });

  it("should invalidate a revoked session", () => {
    const session = service.createSession(10, 3600);
    service.revokeSession(session.sessionId);

    const validated = service.validateToken(session.token);
    expect(validated).toBeNull();
  });
});
