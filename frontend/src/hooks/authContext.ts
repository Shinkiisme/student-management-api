import { createContext, useContext } from "react";
import type { SessionSnapshot } from "../types/auth";

export const AuthContext = createContext<SessionSnapshot | null>(null);

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error("Authentication requires AuthProvider");
  return value;
}
