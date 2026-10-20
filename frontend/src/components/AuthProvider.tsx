import { useEffect, useSyncExternalStore } from "react";
import type { ReactNode } from "react";
import { session } from "../auth/session";
import { restoreSession } from "../api/auth";
import { refreshSession } from "../api/http";
import { AuthContext } from "../hooks/authContext";

export function AuthProvider({ children }: { children: ReactNode }) {
  const snapshot = useSyncExternalStore(session.subscribe, session.getSnapshot);
  useEffect(() => {
    void restoreSession();
  }, []);
  useEffect(() => {
    if (snapshot.status !== "authenticated" || snapshot.expiresAt === null)
      return;
    const expiresAt = snapshot.expiresAt;
    const renewIfExpired = () => {
      const current = session.getSnapshot();
      if (
        session.isCurrent(snapshot.generation) &&
        current.expiresAt !== null &&
        Date.now() >= current.expiresAt
      ) {
        void refreshSession().catch(() => undefined);
      }
    };
    const timer = setTimeout(
      renewIfExpired,
      Math.min(Math.max(0, expiresAt - Date.now()), 2147483647),
    );
    window.addEventListener("focus", renewIfExpired);
    document.addEventListener("visibilitychange", renewIfExpired);
    return () => {
      clearTimeout(timer);
      window.removeEventListener("focus", renewIfExpired);
      document.removeEventListener("visibilitychange", renewIfExpired);
    };
  }, [snapshot]);
  return (
    <AuthContext.Provider value={snapshot}>{children}</AuthContext.Provider>
  );
}
