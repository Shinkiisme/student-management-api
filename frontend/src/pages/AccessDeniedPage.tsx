import { Link } from "react-router";
import { ShieldAlert } from "lucide-react";
import { useAuth } from "../hooks/authContext";
import { landingPage } from "../auth/session";

export function AccessDeniedPage() {
  const auth = useAuth();
  return (
    <section className="panel flex min-h-80 flex-col items-center justify-center px-6 py-10 text-center">
      <ShieldAlert className="mb-4 size-9 text-blue-600" />
      <h1 className="text-2xl font-bold tracking-tight text-slate-900">
        Access denied
      </h1>
      <p className="mt-3 max-w-md text-sm leading-6 text-slate-500">
        Your account does not have permission to view this page.
      </p>
      <Link className="button-primary mt-6" to={landingPage(auth.role)}>
        Go to {auth.role === "ADMIN" ? "dashboard" : "courses"}
      </Link>
    </section>
  );
}
