import type { ProblemDetail } from "../types/api";
import type { AuthTokens } from "../types/auth";
import { session, sessionChanged } from "../auth/session";

const baseUrl = (import.meta.env.VITE_API_BASE_URL?.trim() || "/api").replace(
  /\/+$/,
  "",
);
const statusMessages: Record<number, string> = {
  400: "Please check the request and try again.",
  401: "Your session has expired. Please sign in again.",
  403: "You do not have permission to access this resource.",
  404: "This record could not be found. It may have been removed.",
  409: "This record already exists or is currently in use.",
  500: "The server could not complete the request. Please try again.",
};

export class ApiError extends Error {
  readonly status: number;
  readonly errors: Record<string, string>;

  constructor(status: number, problem: ProblemDetail = {}) {
    super(
      problem.detail ||
        problem.title ||
        statusMessages[status] ||
        "The request failed. Please try again.",
    );
    this.name = "ApiError";
    this.status = status;
    this.errors = problem.errors ?? {};
  }
}

export function errorMessage(error: unknown): string {
  return error instanceof Error
    ? error.message
    : "Something went wrong. Please try again.";
}

export interface RequestOptions extends RequestInit {
  authentication?: "none";
}

const publicAuthPaths = new Set([
  "/auth/login",
  "/auth/refresh",
  "/auth/logout",
]);

function checkRequest(
  signal: AbortSignal | null | undefined,
  generation: number,
) {
  signal?.throwIfAborted();
  if (!session.isCurrent(generation)) throw sessionChanged();
}

async function send(path: string, options: RequestInit, token: string | null) {
  options.signal?.throwIfAborted();
  const headers = new Headers(options.headers);
  if (!headers.has("Accept"))
    headers.set("Accept", "application/json, application/problem+json");
  if (options.body && !headers.has("Content-Type"))
    headers.set("Content-Type", "application/json");
  headers.delete("Authorization");
  if (token) headers.set("Authorization", `Bearer ${token}`);
  let response: Response;
  try {
    response = await fetch(`${baseUrl}${path}`, {
      ...options,
      headers,
    });
  } catch (error) {
    if (
      options.signal?.aborted ||
      (error instanceof Error && error.name === "AbortError")
    )
      throw error;
    throw new Error(
      "Cannot reach the server. Check your connection and that the API is running.",
      { cause: error },
    );
  }
  return response;
}

async function readResponse<T>(
  response: Response,
  signal?: AbortSignal | null,
): Promise<T> {
  if (!response.ok) {
    let problem: ProblemDetail = {};
    try {
      const body: unknown = await response.json();
      if (body && typeof body === "object") {
        const raw = body as Record<string, unknown>;
        const errors: Record<string, string> = {};
        if (raw.errors && typeof raw.errors === "object") {
          for (const [key, value] of Object.entries(raw.errors)) {
            if (typeof value === "string") errors[key] = value;
          }
        }
        problem = {
          detail: typeof raw.detail === "string" ? raw.detail : undefined,
          title: typeof raw.title === "string" ? raw.title : undefined,
          errors,
        };
      }
    } catch (error) {
      if (
        signal?.aborted ||
        (error instanceof Error && error.name === "AbortError")
      )
        throw error;
      /* Proxy and server failures may return non-JSON bodies. */
    }
    throw new ApiError(response.status, problem);
  }
  if (response.status === 204) return undefined as T;
  try {
    return (await response.json()) as T;
  } catch (error) {
    if (
      signal?.aborted ||
      (error instanceof Error && error.name === "AbortError")
    )
      throw error;
    throw new Error(
      "The server returned an unreadable response. Please try again.",
      { cause: error },
    );
  }
}

// Cancel one caller's wait without canceling refresh for the other callers.
function waitForRefresh(
  operation: Promise<void>,
  signal?: AbortSignal | null,
): Promise<void> {
  if (!signal) return operation;
  signal.throwIfAborted();
  return new Promise((resolve, reject) => {
    const abort = () => reject(signal.reason);
    signal.addEventListener("abort", abort, { once: true });
    void operation.then(
      () => {
        signal.removeEventListener("abort", abort);
        resolve();
      },
      (error: unknown) => {
        signal.removeEventListener("abort", abort);
        reject(error);
      },
    );
  });
}

let refreshFlight: { generation: number; operation: Promise<void> } | null =
  null;

export function revokeDiscardedTokens(tokens: AuthTokens) {
  if (typeof tokens?.refreshToken !== "string" || !tokens.refreshToken) return;
  void request<void>("/auth/logout", {
    method: "POST",
    body: JSON.stringify({ refreshToken: tokens.refreshToken }),
  }).catch(() => undefined);
}

export function refreshSession(): Promise<void> {
  const generation = session.getSnapshot().generation;
  if (refreshFlight?.generation === generation) return refreshFlight.operation;
  const refreshToken = session.getRefreshToken();
  if (!refreshToken) {
    session.clear(generation);
    return Promise.reject(new ApiError(401));
  }
  const operation = (async () => {
    try {
      const tokens = await request<AuthTokens>("/auth/refresh", {
        method: "POST",
        body: JSON.stringify({ refreshToken }),
      });
      try {
        if (!session.accept(tokens, generation)) throw sessionChanged();
      } catch (error) {
        revokeDiscardedTokens(tokens);
        throw error;
      }
    } catch (error) {
      session.clear(generation);
      throw error;
    } finally {
      if (refreshFlight?.generation === generation) refreshFlight = null;
    }
  })();
  refreshFlight = { generation, operation };
  session.markRestoring(generation);
  return operation;
}

export async function request<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const { authentication, ...fetchOptions } = options;
  const isPublic =
    authentication === "none" || publicAuthPaths.has(path.split("?")[0]);
  if (isPublic)
    return readResponse<T>(
      await send(path, fetchOptions, null),
      options.signal,
    );

  const generation = session.getSnapshot().generation;
  checkRequest(options.signal, generation);
  let token = session.getAccessToken();
  const expiresAt = session.getSnapshot().expiresAt;
  if (
    (!token && session.getRefreshToken()) ||
    (token && expiresAt !== null && expiresAt <= Date.now())
  ) {
    await waitForRefresh(refreshSession(), options.signal);
    checkRequest(options.signal, generation);
    token = session.getAccessToken();
  }

  let response = await send(path, fetchOptions, token);
  checkRequest(options.signal, generation);
  if (response.status === 401 && session.getRefreshToken()) {
    // A delayed 401 may arrive after another request already rotated the token.
    if (!session.getAccessToken() || session.getAccessToken() === token) {
      await waitForRefresh(refreshSession(), options.signal);
    }
    checkRequest(options.signal, generation);
    response = await send(path, fetchOptions, session.getAccessToken());
    checkRequest(options.signal, generation);
  }
  if (response.status === 401) session.clear(generation);
  const result = await readResponse<T>(response, options.signal);
  checkRequest(options.signal, generation);
  return result;
}
