import type { AuthTokens, Role } from "../types/auth";

// Deliberately unsigned, synthetic tokens used only by isolated frontend tests.
export function authTokens(
  role: Role = "ADMIN",
  marker = "first",
  lifetime = 900,
): AuthTokens {
  const encode = (value: unknown) =>
    btoa(JSON.stringify(value))
      .replace(/=/g, "")
      .replace(/\+/g, "-")
      .replace(/\//g, "_");
  return {
    accessToken: `${encode({ alg: "HS256", typ: "JWT" })}.${encode({ sub: "71", role, exp: Math.floor(Date.now() / 1000) + lifetime, marker })}.test-signature`,
    refreshToken: `test-only-refresh-${marker}`,
    tokenType: "Bearer",
    expiresIn: lifetime,
  };
}

export function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason: unknown) => void;
  const promise = new Promise<T>((onResolve, onReject) => {
    resolve = onResolve;
    reject = onReject;
  });
  return { promise, resolve, reject };
}
