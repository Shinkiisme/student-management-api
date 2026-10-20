// @vitest-environment jsdom
import { beforeEach, describe, expect, it, vi } from "vitest";
import { authTokens } from "../test/authFixtures";

beforeEach(() => {
  sessionStorage.clear();
  vi.restoreAllMocks();
  vi.resetModules();
});

describe("session policy", () => {
  it("stores only the refresh token, with the access token and User ID in memory", async () => {
    const { session, REFRESH_TOKEN_KEY } = await import("./session");
    const tokens = authTokens("STUDENT");
    session.accept(tokens, session.getSnapshot().generation);
    expect(session.getAccessToken()).toBe(tokens.accessToken);
    expect(sessionStorage.length).toBe(1);
    expect(sessionStorage.getItem(REFRESH_TOKEN_KEY)).toBe(tokens.refreshToken);
    expect(session.getSnapshot()).toMatchObject({
      status: "authenticated",
      role: "STUDENT",
      userId: "71",
    });
    expect(session.getSnapshot()).not.toHaveProperty("studentId");
    expect(session.getSnapshot()).not.toHaveProperty("accessToken");
  });

  it("rejects malformed or expired claims without exposing protected state", async () => {
    const { session } = await import("./session");
    const generation = session.getSnapshot().generation;
    expect(() =>
      session.accept(
        { ...authTokens(), accessToken: "not-a-token" },
        generation,
      ),
    ).toThrow("invalid or expired session");
    expect(() =>
      session.accept(authTokens("ADMIN", "expired", -1), generation),
    ).toThrow();
    expect(session.getAccessToken()).toBeNull();
    expect(sessionStorage.length).toBe(0);
  });

  it("invalidates late session commits after logout", async () => {
    const { session } = await import("./session");
    const generation = session.getSnapshot().generation;
    session.clear();
    expect(session.accept(authTokens(), generation)).toBe(false);
    expect(session.getSnapshot().status).toBe("anonymous");
  });

  it("fails closed when refresh token persistence is blocked", async () => {
    const { session } = await import("./session");
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new DOMException("Unavailable", "SecurityError");
    });
    expect(() =>
      session.accept(authTokens(), session.getSnapshot().generation),
    ).toThrow("session storage is unavailable");
    expect(session.getAccessToken()).toBeNull();
    expect(session.getSnapshot().status).not.toBe("authenticated");
  });
});
