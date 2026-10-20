export type Role = "ADMIN" | "STUDENT";

export interface LoginCredentials {
  username: string;
  password: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface SessionSnapshot {
  status: "restoring" | "authenticated" | "anonymous";
  role: Role | null;
  userId: string | null;
  expiresAt: number | null;
  generation: number;
}
