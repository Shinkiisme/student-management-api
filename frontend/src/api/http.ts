import type { ProblemDetail } from "../types/api";

const baseUrl = (import.meta.env.VITE_API_BASE_URL?.trim() || "/api").replace(
  /\/+$/,
  "",
);
const statusMessages: Record<number, string> = {
  400: "Please check the request and try again.",
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

export async function request<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  let response: Response;
  try {
    response = await fetch(`${baseUrl}${path}`, {
      ...options,
      headers: {
        Accept: "application/json, application/problem+json",
        ...(options.body ? { "Content-Type": "application/json" } : {}),
        ...options.headers,
      },
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
    } catch {
      /* Proxy and server failures may return non-JSON bodies. */
    }
    throw new ApiError(response.status, problem);
  }
  if (response.status === 204) return undefined as T;
  try {
    return (await response.json()) as T;
  } catch (error) {
    throw new Error(
      "The server returned an unreadable response. Please try again.",
      { cause: error },
    );
  }
}
