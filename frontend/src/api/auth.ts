import { request, refreshSession, revokeDiscardedTokens } from "./http";
import { session, sessionChanged } from "../auth/session";
import type { AuthTokens, LoginCredentials } from "../types/auth";

export async function login(credentials: LoginCredentials): Promise<void> {
  session.clear();
  const generation = session.getSnapshot().generation;
  try {
    const tokens = await request<AuthTokens>("/auth/login", {
      method: "POST",
      body: JSON.stringify(credentials),
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
  }
}

export async function restoreSession(): Promise<void> {
  if (session.getSnapshot().status !== "restoring") return;
  if (!session.getRefreshToken()) {
    session.clear();
    return;
  }
  try {
    await refreshSession();
  } catch {
    /* Failed restoration clears the store; route guards show Login. */
  }
}

export async function logout(): Promise<void> {
  const refreshToken = session.getRefreshToken();
  session.clear();
  if (!refreshToken) return;
  try {
    await request<void>("/auth/logout", {
      method: "POST",
      body: JSON.stringify({ refreshToken }),
    });
  } catch {
    /* Local sign-out is immediate even if server revocation is unavailable. */
  }
}
