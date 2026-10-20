import type { AuthTokens, Role, SessionSnapshot } from "../types/auth";

export const REFRESH_TOKEN_KEY = "campus.refreshToken";

// Claims inform presentation only. Signature/issuer/authorization are verified by the API.
function decodeClaims(token: string): {
  role: Role;
  userId: string;
  expiresAt: number;
} {
  try {
    const parts = token.split(".");
    if (parts.length !== 3 || parts.some((part) => !part)) throw new Error();
    const payload = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const decoded = atob(
      payload.padEnd(Math.ceil(payload.length / 4) * 4, "="),
    );
    const bytes = Uint8Array.from(decoded, (character) =>
      character.charCodeAt(0),
    );
    const claims: unknown = JSON.parse(new TextDecoder().decode(bytes));
    if (!claims || typeof claims !== "object") throw new Error();
    const { role, sub, exp } = claims as Record<string, unknown>;
    if (
      (role !== "ADMIN" && role !== "STUDENT") ||
      typeof sub !== "string" ||
      !sub ||
      typeof exp !== "number" ||
      !Number.isFinite(exp) ||
      exp * 1000 <= Date.now()
    )
      throw new Error();
    return { role, userId: sub, expiresAt: exp * 1000 };
  } catch {
    throw new Error(
      "The server returned an invalid or expired session. Please sign in again.",
    );
  }
}

let accessToken: string | null = null;
let snapshot: SessionSnapshot = {
  status: "restoring",
  role: null,
  userId: null,
  expiresAt: null,
  generation: 0,
};
const listeners = new Set<() => void>();

function publish(next: SessionSnapshot) {
  snapshot = next;
  listeners.forEach((listener) => listener());
}

export const session = {
  getSnapshot: () => snapshot,
  subscribe(listener: () => void) {
    listeners.add(listener);
    return () => {
      listeners.delete(listener);
    };
  },
  getAccessToken: () => accessToken,
  getRefreshToken(): string | null {
    try {
      return sessionStorage.getItem(REFRESH_TOKEN_KEY) || null;
    } catch {
      return null;
    }
  },
  isCurrent: (generation: number) => snapshot.generation === generation,
  clear(expectedGeneration?: number) {
    if (
      expectedGeneration !== undefined &&
      !session.isCurrent(expectedGeneration)
    )
      return;
    accessToken = null;
    try {
      sessionStorage.removeItem(REFRESH_TOKEN_KEY);
    } catch {
      /* Local memory is still cleared when browser storage is unavailable. */
    }
    publish({
      status: "anonymous",
      role: null,
      userId: null,
      expiresAt: null,
      generation: snapshot.generation + 1,
    });
  },
  markRestoring(generation: number) {
    if (
      session.isCurrent(generation) &&
      (!snapshot.expiresAt || snapshot.expiresAt <= Date.now())
    ) {
      publish({ ...snapshot, status: "restoring" });
    }
  },
  accept(tokens: AuthTokens, generation: number): boolean {
    if (!session.isCurrent(generation)) return false;
    if (
      typeof tokens?.accessToken !== "string" ||
      typeof tokens.refreshToken !== "string" ||
      !tokens.refreshToken ||
      tokens.tokenType !== "Bearer" ||
      !Number.isFinite(tokens.expiresIn) ||
      tokens.expiresIn <= 0
    ) {
      throw new Error(
        "The server returned an invalid authentication response.",
      );
    }
    const claims = decodeClaims(tokens.accessToken);
    try {
      sessionStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
    } catch {
      throw new Error(
        "Browser session storage is unavailable. Allow session storage to sign in.",
      );
    }
    // Storage rotation, memory replacement, and publication run synchronously in this order.
    accessToken = tokens.accessToken;
    publish({
      status: "authenticated",
      ...claims,
      expiresAt: Math.min(
        claims.expiresAt,
        Date.now() + tokens.expiresIn * 1000,
      ),
      generation,
    });
    return true;
  },
};

export function sessionChanged(): DOMException {
  return new DOMException(
    "The session changed before this request completed.",
    "AbortError",
  );
}

export function landingPage(role: Role | null): string {
  return role === "ADMIN" ? "/" : "/courses";
}
