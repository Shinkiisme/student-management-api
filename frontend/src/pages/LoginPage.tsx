import { useEffect, useRef, useState } from "react";
import type { FormEvent } from "react";
import { Navigate } from "react-router";
import { Eye, EyeOff, GraduationCap, LoaderCircle, LogIn } from "lucide-react";
import { login } from "../api/auth";
import { ApiError } from "../api/http";
import { landingPage } from "../auth/session";
import { useAuth } from "../hooks/authContext";
import { Field } from "../components/Field";
import { ErrorState, Loading } from "../components/ui";

export function LoginPage() {
  const auth = useAuth();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [visible, setVisible] = useState(false);
  const [pending, setPending] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<unknown>(null);
  const lock = useRef(false);
  useEffect(() => {
    document.title = "Sign in · Campus";
  }, []);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (lock.current) return;
    const nextErrors: Record<string, string> = {};
    if (!username.trim()) nextErrors.username = "Username is required.";
    if (!password.trim()) nextErrors.password = "Password is required.";
    setErrors(nextErrors);
    setError(null);
    if (Object.keys(nextErrors).length) return;
    lock.current = true;
    setPending(true);
    try {
      await login({ username, password });
    } catch (error) {
      if (error instanceof Error && error.name === "AbortError") return;
      setError(
        error instanceof ApiError && error.status === 401
          ? new Error("Sign-in failed. Check your username and password.")
          : error,
      );
      if (error instanceof ApiError) setErrors(error.errors);
    } finally {
      lock.current = false;
      setPending(false);
    }
  }
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
  if (auth.status === "authenticated")
    return <Navigate to={landingPage(auth.role)} replace />;
  return (
    <main className="flex min-h-dvh items-center justify-center bg-[#f7f9fc] px-4 py-10">
      <div className="w-full max-w-md">
        <div className="mb-7 flex items-center justify-center gap-3">
          <span className="flex size-11 items-center justify-center rounded-xl bg-blue-600 text-white shadow-sm">
            <GraduationCap className="size-7" />
          </span>
          <p className="text-2xl font-bold tracking-tight text-slate-900">
            Campus<span className="text-blue-600">.</span>
          </p>
        </div>
        <section className="panel p-6 sm:p-8">
          <h1 className="text-2xl font-bold tracking-tight text-slate-900">
            Welcome back
          </h1>
          <p className="mt-2 text-sm leading-6 text-slate-500">
            Sign in to your student management workspace.
          </p>
          <form
            className="mt-7 space-y-5"
            noValidate
            onSubmit={(event) => void submit(event)}
          >
            {error !== null && <ErrorState error={error} />}
            <Field label="Username" error={errors.username}>
              {(props) => (
                <input
                  {...props}
                  className="input"
                  autoComplete="username"
                  name="username"
                  required
                  disabled={pending}
                  value={username}
                  onChange={(event) => {
                    setUsername(event.target.value);
                    setErrors({ ...errors, username: "" });
                  }}
                />
              )}
            </Field>
            <Field label="Password" error={errors.password}>
              {(props) => (
                <div className="relative">
                  <input
                    {...props}
                    className="input pr-12"
                    type={visible ? "text" : "password"}
                    autoComplete="current-password"
                    name="password"
                    required
                    disabled={pending}
                    value={password}
                    onChange={(event) => {
                      setPassword(event.target.value);
                      setErrors({ ...errors, password: "" });
                    }}
                  />
                  <button
                    className="icon-button absolute top-1.5 right-2"
                    type="button"
                    aria-label={visible ? "Hide password" : "Show password"}
                    aria-pressed={visible}
                    disabled={pending}
                    onClick={() => setVisible(!visible)}
                  >
                    {visible ? (
                      <EyeOff className="size-4" />
                    ) : (
                      <Eye className="size-4" />
                    )}
                  </button>
                </div>
              )}
            </Field>
            <button
              className="button-primary w-full"
              type="submit"
              disabled={pending}
            >
              {pending ? (
                <LoaderCircle className="size-4 animate-spin" />
              ) : (
                <LogIn className="size-4" />
              )}
              {pending ? "Signing in…" : "Sign in"}
            </button>
          </form>
        </section>
        <p className="mt-5 text-center text-xs leading-5 text-slate-500">
          Use the account provided by your administrator.
        </p>
      </div>
    </main>
  );
}
