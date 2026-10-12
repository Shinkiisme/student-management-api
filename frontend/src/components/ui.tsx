import {
  AlertCircle,
  ArrowDown,
  ArrowUp,
  ArrowUpDown,
  Inbox,
  LoaderCircle,
  RefreshCw,
} from "lucide-react";
import type { ReactNode } from "react";
import { errorMessage } from "../api/http";
import type { Direction } from "../types/api";

export function Loading({ label = "Loading records…" }: { label?: string }) {
  return (
    <div
      role="status"
      className="flex min-h-48 items-center justify-center gap-3 text-sm text-slate-500"
    >
      <LoaderCircle className="size-5 animate-spin text-blue-600" />
      {label}
    </div>
  );
}

export function ErrorState({
  error,
  retry,
}: {
  error: unknown;
  retry?: () => void;
}) {
  return (
    <div
      role="alert"
      className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800"
    >
      <div className="flex items-start gap-3">
        <AlertCircle className="mt-0.5 size-4 shrink-0" />
        <div className="min-w-0 flex-1">
          <p className="font-semibold">Unable to complete this request</p>
          <p className="mt-1 break-words">{errorMessage(error)}</p>
          {retry && (
            <button
              className="mt-3 inline-flex items-center gap-2 font-semibold underline underline-offset-4"
              onClick={retry}
            >
              <RefreshCw className="size-3.5" />
              Try again
            </button>
          )}
        </div>
      </div>
    </div>
  );
}

export function EmptyState({
  title,
  description,
  action,
}: {
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <div className="flex min-h-64 flex-col items-center justify-center px-6 py-10 text-center">
      <div className="mb-4 rounded-2xl bg-slate-100 p-3 text-slate-400">
        <Inbox className="size-6" />
      </div>
      <h3 className="font-semibold text-slate-800">{title}</h3>
      <p className="mt-2 max-w-sm text-sm leading-6 text-slate-500">
        {description}
      </p>
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}

export function PageHeading({
  eyebrow = "WORKSPACE",
  title,
  description,
  action,
}: {
  eyebrow?: string;
  title: string;
  description: string;
  action?: ReactNode;
}) {
  return (
    <div className="mb-7 flex flex-wrap items-end justify-between gap-4">
      <div className="min-w-0 max-w-full">
        <p className="mb-2 text-[11px] font-semibold tracking-[0.16em] text-slate-400">
          {eyebrow}
        </p>
        <h1 className="text-2xl font-bold tracking-tight break-words text-slate-900 sm:text-3xl">
          {title}
        </h1>
        <p className="mt-2 text-sm leading-6 text-slate-500">{description}</p>
      </div>
      {action}
    </div>
  );
}

export function SortHeading({
  label,
  field,
  sortBy,
  direction,
  onSort,
}: {
  label: string;
  field: string;
  sortBy: string;
  direction: Direction;
  onSort: (field: string) => void;
}) {
  const active = sortBy === field;
  const Icon = active
    ? direction === "asc"
      ? ArrowUp
      : ArrowDown
    : ArrowUpDown;
  return (
    <th
      scope="col"
      aria-sort={
        active ? (direction === "asc" ? "ascending" : "descending") : "none"
      }
    >
      <button
        onClick={() => onSort(field)}
        className="flex items-center gap-2 hover:text-blue-700"
      >
        {label}
        <Icon
          className={`size-3.5 ${active ? "text-blue-600" : "text-slate-300"}`}
        />
      </button>
    </th>
  );
}
