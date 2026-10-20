// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { authTokens, deferred } from "../test/authFixtures";

beforeEach(() => {
  sessionStorage.clear();
  vi.resetModules();
});
afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("authentication lifecycle", () => {
  it("revokes a newly issued refresh token if login cannot persist it", async () => {
    const { login } = await import("./auth");
    const { session } = await import("../auth/session");
    const fetch = vi
      .fn()
      .mockImplementation((url: string) =>
        Promise.resolve(
          url.endsWith("/auth/login")
            ? Response.json(authTokens())
            : new Response(null, { status: 204 }),
        ),
      );
    vi.stubGlobal("fetch", fetch);
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new DOMException("Unavailable", "SecurityError");
    });
    await expect(
      login({ username: "test-user", password: "test-only-password" }),
    ).rejects.toThrow("session storage is unavailable");
    expect(session.getSnapshot().status).toBe("anonymous");
    expect(
      fetch.mock.calls.filter((call) => call[0].endsWith("/auth/logout")),
    ).toHaveLength(1);
  });

  it("logs in through the actual contract without Bearer headers or password persistence", async () => {
    const { login } = await import("./auth");
    const { session, REFRESH_TOKEN_KEY } = await import("../auth/session");
    const tokens = authTokens();
    const fetch = vi.fn().mockResolvedValue(Response.json(tokens));
    vi.stubGlobal("fetch", fetch);
    await login({ username: "test-user", password: "test-only-password" });
    expect(fetch.mock.calls[0][0]).toBe("/api/auth/login");
    expect(fetch.mock.calls[0][1].headers.get("Authorization")).toBeNull();
    expect(JSON.parse(fetch.mock.calls[0][1].body)).toEqual({
      username: "test-user",
      password: "test-only-password",
    });
    expect(sessionStorage.getItem(REFRESH_TOKEN_KEY)).toBe(tokens.refreshToken);
    expect(sessionStorage.length).toBe(1);
    expect(session.getSnapshot().role).toBe("ADMIN");
  });

  it("reports invalid credentials without trying refresh", async () => {
    const { login } = await import("./auth");
    const { session } = await import("../auth/session");
    const fetch = vi
      .fn()
      .mockResolvedValue(
        Response.json({ detail: "Invalid credentials" }, { status: 401 }),
      );
    vi.stubGlobal("fetch", fetch);
    await expect(
      login({ username: "test-user", password: "incorrect" }),
    ).rejects.toMatchObject({ status: 401 });
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(session.getSnapshot().status).toBe("anonymous");
  });

  it("restores a reloaded tab once and saves the rotated refresh token", async () => {
    const { restoreSession } = await import("./auth");
    const { session, REFRESH_TOKEN_KEY } = await import("../auth/session");
    sessionStorage.setItem(REFRESH_TOKEN_KEY, "test-only-prior-refresh");
    const reply = deferred<Response>();
    const fetch = vi.fn().mockReturnValue(reply.promise);
    vi.stubGlobal("fetch", fetch);
    const first = restoreSession();
    const second = restoreSession();
    expect(session.getSnapshot().status).toBe("restoring");
    reply.resolve(Response.json(authTokens("STUDENT", "restored")));
    await Promise.all([first, second]);
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(fetch.mock.calls[0][0]).toBe("/api/auth/refresh");
    expect(sessionStorage.getItem(REFRESH_TOKEN_KEY)).toBe(
      "test-only-refresh-restored",
    );
    expect(session.getSnapshot()).toMatchObject({
      status: "authenticated",
      role: "STUDENT",
    });
  });

  it("clears a failed restoration", async () => {
    const { restoreSession } = await import("./auth");
    const { session, REFRESH_TOKEN_KEY } = await import("../auth/session");
    sessionStorage.setItem(REFRESH_TOKEN_KEY, "test-only-revoked-refresh");
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          Response.json({ detail: "Invalid refresh token" }, { status: 401 }),
        ),
    );
    await restoreSession();
    expect(session.getSnapshot().status).toBe("anonymous");
    expect(sessionStorage.getItem(REFRESH_TOKEN_KEY)).toBeNull();
  });

  it("resolves a reload without a refresh token without any network request", async () => {
    const { restoreSession } = await import("./auth");
    const { session } = await import("../auth/session");
    const fetch = vi.fn();
    vi.stubGlobal("fetch", fetch);
    await restoreSession();
    expect(fetch).not.toHaveBeenCalled();
    expect(session.getSnapshot().status).toBe("anonymous");
  });

  it("clears logout locally before the revocation request completes, even if it fails", async () => {
    const { logout } = await import("./auth");
    const { session, REFRESH_TOKEN_KEY } = await import("../auth/session");
    const tokens = authTokens();
    session.accept(tokens, session.getSnapshot().generation);
    const reply = deferred<Response>();
    const fetch = vi.fn().mockReturnValue(reply.promise);
    vi.stubGlobal("fetch", fetch);
    const result = logout();
    expect(session.getAccessToken()).toBeNull();
    expect(session.getSnapshot().status).toBe("anonymous");
    expect(sessionStorage.getItem(REFRESH_TOKEN_KEY)).toBeNull();
    expect(JSON.parse(fetch.mock.calls[0][1].body)).toEqual({
      refreshToken: tokens.refreshToken,
    });
    reply.reject(new TypeError("Offline"));
    await expect(result).resolves.toBeUndefined();
  });

  it("cannot restore a session from a login response arriving after logout", async () => {
    const { login, logout } = await import("./auth");
    const { session } = await import("../auth/session");
    const reply = deferred<Response>();
    const fetch = vi
      .fn()
      .mockImplementation((url: string) =>
        url.endsWith("/auth/login")
          ? reply.promise
          : Promise.resolve(new Response(null, { status: 204 })),
      );
    vi.stubGlobal("fetch", fetch);
    const pending = login({
      username: "test-user",
      password: "test-only-password",
    });
    const rejected = expect(pending).rejects.toMatchObject({
      name: "AbortError",
    });
    await logout();
    reply.resolve(Response.json(authTokens("ADMIN", "late-login")));
    await rejected;
    expect(session.getSnapshot().status).toBe("anonymous");
    expect(fetch).toHaveBeenCalledWith("/api/auth/logout", expect.any(Object));
  });
});
