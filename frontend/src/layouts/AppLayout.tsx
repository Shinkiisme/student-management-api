import { useEffect, useRef, useState } from "react";
import { Link, NavLink, Outlet, useLocation } from "react-router";
import {
  BookOpen,
  ChevronRight,
  GraduationCap,
  LayoutDashboard,
  Link2,
  LogOut,
  Menu,
  PanelLeftClose,
  Users,
} from "lucide-react";
import { useAuth } from "../hooks/authContext";
import { landingPage } from "../auth/session";
import { logout } from "../api/auth";

const navigation = [
  { to: "/", label: "Dashboard", icon: LayoutDashboard },
  { to: "/students", label: "Students", icon: Users },
  { to: "/courses", label: "Courses", icon: BookOpen },
  { to: "/enrollments", label: "Enrollments", icon: Link2 },
];

function SidebarContents({ onNavigate }: { onNavigate?: () => void }) {
  const auth = useAuth();
  return (
    <>
      <Link
        to={landingPage(auth.role)}
        onClick={onNavigate}
        className="flex items-center gap-3 px-6 py-7"
      >
        <span className="flex size-10 items-center justify-center rounded-xl bg-blue-600 text-white shadow-sm shadow-blue-200">
          <GraduationCap className="size-6" />
        </span>
        <div>
          <p className="text-lg font-bold tracking-tight text-slate-900">
            Campus<span className="text-blue-600">.</span>
          </p>
          <p className="mt-0.5 text-[10px] font-medium tracking-wide text-slate-400">
            STUDENT MANAGEMENT
          </p>
        </div>
      </Link>
      <p className="mt-5 mb-3 px-6 text-[10px] font-semibold tracking-[0.16em] text-slate-400">
        WORKSPACE
      </p>
      <nav aria-label="Main navigation" className="space-y-1.5 px-3">
        {navigation
          .filter((item) => auth.role === "ADMIN" || item.to === "/courses")
          .map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
              end={to === "/"}
              onClick={onNavigate}
              className={({ isActive }) =>
                `flex items-center gap-3 rounded-lg px-3 py-3 text-sm ${isActive ? "bg-blue-50 font-semibold text-blue-600" : "font-medium text-slate-500 hover:bg-slate-50 hover:text-slate-800"}`
              }
            >
              <Icon className="size-[18px]" />
              {label}
            </NavLink>
          ))}
      </nav>
      <div className="mt-auto p-4">
        <div className="rounded-xl border border-slate-100 bg-slate-50 p-4">
          <GraduationCap className="mb-3 size-5 text-slate-400" />
          <p className="text-xs font-semibold text-slate-700">
            Built around your campus
          </p>
          <p className="mt-2 text-xs leading-5 text-slate-400">
            Your students, courses, and enrollments. One connected workspace.
          </p>
        </div>
        <p className="mt-5 px-2 text-[10px] text-slate-400">
          Campus · Administration workspace
        </p>
      </div>
    </>
  );
}

export function AppLayout() {
  const auth = useAuth();
  const [mobileOpen, setMobileOpen] = useState(false);
  const location = useLocation();
  const section =
    navigation.find(
      (item) => item.to !== "/" && location.pathname.startsWith(item.to),
    )?.label ?? "Dashboard";
  useEffect(() => {
    document.title = `${section} · Campus`;
  }, [section]);
  return (
    <div className="min-h-screen bg-[#f7f9fc]">
      <a
        className="sr-only fixed top-2 left-2 z-[100] rounded-lg bg-blue-600 p-3 text-white focus:not-sr-only"
        href="#main-content"
      >
        Skip to content
      </a>
      <aside className="fixed inset-y-0 left-0 z-20 hidden w-60 flex-col border-r border-slate-200/70 bg-white lg:flex">
        <SidebarContents />
      </aside>
      <div className="lg:pl-60">
        <header className="flex h-18 items-center justify-between gap-3 border-b border-slate-200/70 bg-white px-4 sm:px-8">
          <div className="flex items-center gap-3">
            <button
              className="icon-button lg:hidden"
              aria-label="Open navigation"
              aria-expanded={mobileOpen}
              onClick={() => setMobileOpen(true)}
            >
              <Menu className="size-5" />
            </button>
            <span className="hidden text-xs text-slate-400 sm:block">
              Workspace
            </span>
            <ChevronRight className="hidden size-3.5 text-slate-300 sm:block" />
            <span className="text-xs font-medium text-slate-700">
              {section}
            </span>
          </div>
          <div className="flex items-center gap-2 sm:gap-4">
            <span className="flex items-center gap-2 text-xs text-slate-500">
              <GraduationCap className="size-4 text-blue-500" />
              <span className="hidden sm:inline">
                {auth.role === "ADMIN" ? "Administrator" : "Student"}
              </span>
              <span className="sm:hidden">Campus</span>
            </span>
            <button className="button-quiet" onClick={() => void logout()}>
              <LogOut className="size-4" />
              Log out
            </button>
          </div>
        </header>
        <main
          id="main-content"
          tabIndex={-1}
          className="mx-auto max-w-7xl px-4 py-7 outline-none sm:px-8 sm:py-8"
        >
          <Outlet />
        </main>
        <footer className="mx-auto flex max-w-7xl flex-wrap items-center justify-between gap-2 px-4 pb-6 text-[10px] text-slate-400 sm:px-8">
          <span>Campus · Student Management</span>
          <span>A simpler way to manage your university</span>
        </footer>
      </div>
      {mobileOpen && <MobileNavigation onClose={() => setMobileOpen(false)} />}
    </div>
  );
}

function MobileNavigation({ onClose }: { onClose: () => void }) {
  // Native modal supplies keyboard focus containment and Escape dismissal.
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const dialog = ref.current;
    const previousFocus = document.activeElement;
    dialog?.showModal();
    return () => {
      dialog?.close();
      if (previousFocus instanceof HTMLElement && previousFocus.isConnected) {
        previousFocus.focus();
      }
    };
  }, []);
  return (
    <dialog
      ref={ref}
      aria-label="Navigation menu"
      className="fixed inset-y-0 left-0 m-0 h-dvh max-h-none w-72 max-w-[85vw] border-0 bg-white p-0 backdrop:bg-slate-950/35"
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
    >
      <div className="flex h-full flex-col">
        <button
          className="icon-button absolute top-4 right-3"
          aria-label="Close navigation"
          onClick={onClose}
        >
          <PanelLeftClose className="size-4" />
        </button>
        <SidebarContents onNavigate={onClose} />
      </div>
    </dialog>
  );
}
