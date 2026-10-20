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

async function authenticated() {
  const http = await import("./http");
  const { session, REFRESH_TOKEN_KEY } = await import("../auth/session");
  const tokens = authTokens();
  session.accept(tokens, session.getSnapshot().generation);
  return { ...http, session, tokens, REFRESH_TOKEN_KEY };
}

const unauthorized = () =>
  Response.json({ detail: "Authentication is required" }, { status: 401 });

describe("authenticated HTTP requests", () => {
  it("revokes a rotated refresh token if its storage write fails", async () => {
    const { refreshSession, session } = await authenticated();
    const fetch = vi
      .fn()
      .mockImplementation((url: string) =>
        Promise.resolve(
          url.endsWith("/auth/refresh")
            ? Response.json(authTokens("ADMIN", "failed-storage"))
            : new Response(null, { status: 204 }),
        ),
      );
    vi.stubGlobal("fetch", fetch);
    vi.spyOn(Storage.prototype, "setItem").mockImplementation(() => {
      throw new DOMException("Unavailable", "SecurityError");
    });
    await expect(refreshSession()).rejects.toThrow(
      "session storage is unavailable",
    );
    expect(session.getSnapshot().status).toBe("anonymous");
    expect(
      fetch.mock.calls.filter((call) => call[0].endsWith("/auth/logout")),
    ).toHaveLength(1);
  });

  it("injects the current Bearer token and supports native Headers inputs", async () => {
    const { request, tokens } = await authenticated();
    const fetch = vi.fn().mockResolvedValue(Response.json([]));
    vi.stubGlobal("fetch", fetch);
    await request("/courses", {
      headers: new Headers({ "X-Request-ID": "test-request" }),
    });
    const headers = fetch.mock.calls[0][1].headers as Headers;
    expect(headers.get("Authorization")).toBe(`Bearer ${tokens.accessToken}`);
    expect(headers.get("X-Request-ID")).toBe("test-request");
  });

  it("rotates on 401 then retries once with the new token and the same JSON body", async () => {
    const { request, session, REFRESH_TOKEN_KEY } = await authenticated();
    const rotated = authTokens("ADMIN", "rotated");
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(unauthorized())
      .mockResolvedValueOnce(Response.json(rotated))
      .mockResolvedValueOnce(Response.json({ id: 3 }));
    vi.stubGlobal("fetch", fetch);
    await expect(
      request("/courses", {
        method: "POST",
        body: JSON.stringify({ code: "C1", name: "Course", credits: 4 }),
      }),
    ).resolves.toEqual({ id: 3 });
    expect(fetch.mock.calls.map((call) => call[0])).toEqual([
      "/api/courses",
      "/api/auth/refresh",
      "/api/courses",
    ]);
    expect(fetch.mock.calls[1][1].headers.get("Authorization")).toBeNull();
    expect(fetch.mock.calls[2][1].headers.get("Authorization")).toBe(
      `Bearer ${rotated.accessToken}`,
    );
    expect(fetch.mock.calls[2][1].body).toBe(fetch.mock.calls[0][1].body);
    expect(sessionStorage.getItem(REFRESH_TOKEN_KEY)).toBe(
      rotated.refreshToken,
    );
    expect(session.getAccessToken()).toBe(rotated.accessToken);
  });

  it("clears the session if refresh fails without retrying the protected request", async () => {
    const { request, session, REFRESH_TOKEN_KEY } = await authenticated();
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(unauthorized())
      .mockResolvedValueOnce(unauthorized());
    vi.stubGlobal("fetch", fetch);
    await expect(request("/students")).rejects.toMatchObject({ status: 401 });
    expect(fetch).toHaveBeenCalledTimes(2);
    expect(session.getSnapshot().status).toBe("anonymous");
    expect(sessionStorage.getItem(REFRESH_TOKEN_KEY)).toBeNull();
  });

  it("shares one in-flight refresh between simultaneous 401 responses", async () => {
    const { request, tokens } = await authenticated();
    const rotated = authTokens("ADMIN", "shared");
    const reply = deferred<Response>();
    const fetch = vi
      .fn()
      .mockImplementation((url: string, options: RequestInit) => {
        if (url.endsWith("/auth/refresh")) return reply.promise;
        return Promise.resolve(
          new Headers(options.headers).get("Authorization") ===
            `Bearer ${tokens.accessToken}`
            ? unauthorized()
            : Response.json([]),
        );
      });
    vi.stubGlobal("fetch", fetch);
    const first = request("/students");
    const second = request("/courses");
    await vi.waitFor(() =>
      expect(
        fetch.mock.calls.filter((call) => call[0].endsWith("/auth/refresh")),
      ).toHaveLength(1),
    );
    reply.resolve(Response.json(rotated));
    await Promise.all([first, second]);
    expect(
      fetch.mock.calls.filter((call) => call[0].endsWith("/auth/refresh")),
    ).toHaveLength(1);
  });

  it("reuses the new token for a delayed old-token 401 instead of rotating twice", async () => {
    const { request, tokens } = await authenticated();
    const delayed = deferred<Response>();
    const fetch = vi
      .fn()
      .mockImplementation((url: string, options: RequestInit) => {
        if (url.endsWith("/auth/refresh"))
          return Promise.resolve(Response.json(authTokens("ADMIN", "new")));
        if (
          new Headers(options.headers).get("Authorization") ===
          `Bearer ${tokens.accessToken}`
        )
          return url.endsWith("/students")
            ? delayed.promise
            : Promise.resolve(unauthorized());
        return Promise.resolve(Response.json([]));
      });
    vi.stubGlobal("fetch", fetch);
    const slow = request("/students");
    await request("/courses");
    delayed.resolve(unauthorized());
    await slow;
    expect(
      fetch.mock.calls.filter((call) => call[0].endsWith("/auth/refresh")),
    ).toHaveLength(1);
  });

  it("stops after one retry when the new token also receives 401", async () => {
    const { request, session } = await authenticated();
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(unauthorized())
      .mockResolvedValueOnce(Response.json(authTokens("ADMIN", "new")))
      .mockResolvedValueOnce(unauthorized());
    vi.stubGlobal("fetch", fetch);
    await expect(request("/courses")).rejects.toMatchObject({ status: 401 });
    expect(fetch).toHaveBeenCalledTimes(3);
    expect(session.getSnapshot().status).toBe("anonymous");
  });

  it.each(["/auth/login", "/auth/refresh", "/auth/logout"])(
    "never sends a Bearer header or refreshes public endpoint %s",
    async (path) => {
      const { request, session } = await authenticated();
      const fetch = vi.fn().mockResolvedValue(unauthorized());
      vi.stubGlobal("fetch", fetch);
      await expect(request(path, { method: "POST" })).rejects.toMatchObject({
        status: 401,
      });
      expect(fetch).toHaveBeenCalledTimes(1);
      expect(fetch.mock.calls[0][1].headers.has("Authorization")).toBe(false);
      expect(session.getSnapshot().status).toBe("authenticated");
    },
  );

  it("preserves a forbidden session without refreshing on 403", async () => {
    const { request, session } = await authenticated();
    const fetch = vi
      .fn()
      .mockResolvedValue(
        Response.json(
          { detail: "You do not have permission to access this resource" },
          { status: 403 },
        ),
      );
    vi.stubGlobal("fetch", fetch);
    await expect(request("/students")).rejects.toMatchObject({
      status: 403,
      message: "You do not have permission to access this resource",
    });
    expect(fetch).toHaveBeenCalledTimes(1);
    expect(session.getSnapshot().status).toBe("authenticated");
  });

  it("does not retry an aborted waiter or cancel the shared refresh for another caller", async () => {
    const { request, tokens } = await authenticated();
    const reply = deferred<Response>();
    const controller = new AbortController();
    const fetch = vi
      .fn()
      .mockImplementation((url: string, options: RequestInit) => {
        if (url.endsWith("/auth/refresh")) return reply.promise;
        return Promise.resolve(
          new Headers(options.headers).get("Authorization") ===
            `Bearer ${tokens.accessToken}`
            ? unauthorized()
            : Response.json([]),
        );
      });
    vi.stubGlobal("fetch", fetch);
    const aborted = request("/students", { signal: controller.signal });
    const rejected = expect(aborted).rejects.toMatchObject({
      name: "AbortError",
    });
    const waiting = request("/courses");
    await vi.waitFor(() =>
      expect(
        fetch.mock.calls.some((call) => call[0].endsWith("/auth/refresh")),
      ).toBe(true),
    );
    controller.abort();
    await rejected;
    reply.resolve(Response.json(authTokens("ADMIN", "rotated")));
    await waiting;
    expect(
      fetch.mock.calls.filter((call) => call[0] === "/api/students"),
    ).toHaveLength(1);
    expect(
      fetch.mock.calls.filter((call) => call[0] === "/api/courses"),
    ).toHaveLength(2);
  });

  it("discards and revokes a rotated token arriving after logout", async () => {
    const { refreshSession, session, REFRESH_TOKEN_KEY } =
      await authenticated();
    const { logout } = await import("./auth");
    const reply = deferred<Response>();
    const fetch = vi
      .fn()
      .mockImplementation((url: string) =>
        url.endsWith("/auth/refresh")
          ? reply.promise
          : Promise.resolve(new Response(null, { status: 204 })),
      );
    vi.stubGlobal("fetch", fetch);
    const pending = refreshSession();
    const rejected = expect(pending).rejects.toMatchObject({
      name: "AbortError",
    });
    await logout();
    reply.resolve(Response.json(authTokens("ADMIN", "late")));
    await rejected;
    expect(session.getSnapshot().status).toBe("anonymous");
    expect(sessionStorage.getItem(REFRESH_TOKEN_KEY)).toBeNull();
    expect(
      fetch.mock.calls.filter((call) => call[0].endsWith("/auth/logout")),
    ).toHaveLength(2);
  });

  it("does not expose a previous account's successful response after session replacement", async () => {
    const { request, session } = await authenticated();
    const reply = deferred<Response>();
    vi.stubGlobal("fetch", vi.fn().mockReturnValue(reply.promise));
    const pending = request("/students");
    const rejected = expect(pending).rejects.toMatchObject({
      name: "AbortError",
    });
    session.clear();
    session.accept(
      authTokens("STUDENT", "other-account"),
      session.getSnapshot().generation,
    );
    reply.resolve(Response.json([{ id: 9 }]));
    await rejected;
    expect(session.getSnapshot().role).toBe("STUDENT");
  });

  it("preserves cancellation while reading a response body", async () => {
    const { request } = await authenticated();
    const controller = new AbortController();
    const body = deferred<unknown>();
    const json = vi.fn(() => body.promise);
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({ ok: true, status: 200, json }),
    );
    const pending = request("/courses", { signal: controller.signal });
    const rejected = expect(pending).rejects.toMatchObject({
      name: "AbortError",
    });
    await vi.waitFor(() => expect(json).toHaveBeenCalledOnce());
    controller.abort();
    body.reject(controller.signal.reason);
    await rejected;
  });
});
