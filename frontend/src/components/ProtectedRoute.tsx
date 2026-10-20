import { Navigate, Outlet } from "react-router";
import { useAuth } from "../hooks/authContext";
import { Loading } from "./ui";
import { AccessDeniedPage } from "../pages/AccessDeniedPage";
import { AppLayout } from "../layouts/AppLayout";

export function ProtectedRoute() {
  const auth = useAuth();
  if (
    auth.status === "restoring" ||
    (auth.status === "authenticated" &&
      (auth.expiresAt === null || auth.expiresAt <= Date.now()))
  ) {
    return (
      <main className="min-h-screen bg-slate-50 p-8">
        <Loading label="Restoring your session…" />
      </main>
    );
  }
  return auth.status === "authenticated" ? (
    <Outlet />
  ) : (
    <Navigate to="/login" replace />
  );
}

export function AdminRoute() {
  return useAuth().role === "ADMIN" ? <Outlet /> : <AccessDeniedPage />;
}

export function AuthenticatedLayout() {
  const auth = useAuth();
  return <AppLayout key={`${auth.generation}:${auth.role}`} />;
}
