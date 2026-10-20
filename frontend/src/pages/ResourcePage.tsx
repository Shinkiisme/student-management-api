import { useCallback, useEffect, useState } from "react";
import type { FormEvent } from "react";
import { Link } from "react-router";
import {
  BookOpen,
  Eye,
  Filter,
  Pencil,
  Plus,
  RefreshCw,
  Trash2,
  Users,
  X,
} from "lucide-react";
import { useAsync } from "../hooks/useAsync";
import { useNotify } from "../hooks/notificationContext";
import { useAuth } from "../hooks/authContext";
import type { ListQuery } from "../types/api";
import type { ResourceConfig } from "../utils/resourceConfig";
import { initials, integerValue, MAX_INTEGER } from "../utils/validation";
import {
  EmptyState,
  ErrorState,
  Loading,
  PageHeading,
  SortHeading,
} from "../components/ui";
import { Pagination } from "../components/Pagination";
import { ResourceForm } from "../components/ResourceForm";
import { ConfirmDialog } from "../components/ConfirmDialog";

export function ResourcePage<T extends { id: number; name: string }, P>({
  config,
}: {
  config: ResourceConfig<T, P>;
}) {
  const canManage = useAuth().role === "ADMIN";
  const [query, setQuery] = useState<ListQuery>({
    page: 0,
    size: 10,
    sortBy: "id",
    direction: "asc",
  });
  const [filter, setFilter] = useState("");
  const [filterError, setFilterError] = useState("");
  const [form, setForm] = useState<{ item?: T } | null>(null);
  const [deleting, setDeleting] = useState<T | null>(null);
  const load = useCallback(
    (signal: AbortSignal) => config.api.list(query, signal),
    [config, query],
  );
  const { data, error, pending, reload } = useAsync(load);
  const notify = useNotify();
  const activeFilter = config.kind === "students" ? query.age : query.credits;
  useEffect(() => {
    if (data && data.page > 0 && data.page >= data.totalPages) {
      setQuery((previous) => ({
        ...previous,
        page: Math.max(0, data.totalPages - 1),
      }));
    }
  }, [data]);
  function applyFilter(event: FormEvent) {
    event.preventDefault();
    const numeric =
      filter.trim() === ""
        ? undefined
        : integerValue(filter, config.numericMin);
    if (numeric === null) {
      setFilterError(
        `Enter a whole number from ${config.numericMin} to ${MAX_INTEGER}.`,
      );
      return;
    }
    setFilterError("");
    setQuery((previous) => ({
      ...previous,
      page: 0,
      [config.numericKey]: numeric,
    }));
  }
  function clearFilter() {
    setFilter("");
    setFilterError("");
    setQuery((previous) => ({
      ...previous,
      page: 0,
      [config.numericKey]: undefined,
    }));
  }
  function sort(field: string) {
    setQuery((previous) => ({
      ...previous,
      page: 0,
      sortBy: field,
      direction:
        previous.sortBy === field && previous.direction === "asc"
          ? "desc"
          : "asc",
    }));
  }
  const Icon = config.kind === "students" ? Users : BookOpen;
  return (
    <>
      <PageHeading
        title={config.title}
        description={config.description}
        action={
          canManage && (
            <button className="button-primary" onClick={() => setForm({})}>
              <Plus className="size-4" />
              Add {config.singular}
            </button>
          )
        }
      />
      <section className="panel" aria-label={`${config.title} directory`}>
        <div className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-100 px-5 py-5">
          <div className="flex items-center gap-3">
            <div className="rounded-lg bg-blue-50 p-2 text-blue-600">
              <Icon className="size-4" />
            </div>
            <h2 className="text-sm font-semibold text-slate-800">
              {activeFilter === undefined
                ? `All ${config.kind}`
                : `Filtered ${config.kind}`}
            </h2>
            {data && (
              <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-600">
                {data.totalElements}
              </span>
            )}
          </div>
          <button className="button-quiet" disabled={pending} onClick={reload}>
            <RefreshCw
              className={`size-3.5 ${pending ? "animate-spin" : ""}`}
            />
            Refresh
          </button>
        </div>
        <div className="border-b border-slate-100 px-5 py-4">
          <form
            onSubmit={applyFilter}
            className="flex flex-wrap items-center gap-2"
          >
            <Filter className="mr-1 size-4 text-slate-400" />
            <label
              htmlFor={`${config.kind}-filter`}
              className="mr-1 text-xs font-medium text-slate-600"
            >
              {config.numericLabel}
            </label>
            <input
              id={`${config.kind}-filter`}
              className="input max-w-36 py-2 text-xs"
              type="number"
              min={config.numericMin}
              max={MAX_INTEGER}
              step="1"
              value={filter}
              placeholder={`Any ${config.numericLabel.toLowerCase()}`}
              aria-invalid={Boolean(filterError)}
              aria-describedby={filterError ? "filter-error" : undefined}
              onChange={(event) => {
                setFilter(event.target.value);
                setFilterError("");
              }}
            />
            <button className="button-secondary py-2 text-xs" type="submit">
              Apply filter
            </button>
            {(activeFilter !== undefined || filter) && (
              <button
                className="button-quiet"
                type="button"
                onClick={clearFilter}
              >
                <X className="size-3.5" />
                Clear
              </button>
            )}
            <span className="ml-auto text-xs text-slate-400">
              Select a column to sort
            </span>
          </form>
          {filterError && (
            <p
              id="filter-error"
              role="alert"
              className="mt-2 text-xs text-red-600"
            >
              {filterError}
            </p>
          )}
        </div>
        {pending ? (
          <Loading />
        ) : error ? (
          <div className="p-5">
            <ErrorState error={error} retry={reload} />
          </div>
        ) : (
          data && (
            <>
              {data.content.length === 0 ? (
                <EmptyState
                  title={
                    activeFilter === undefined
                      ? `No ${config.kind} yet`
                      : "No matching records"
                  }
                  description={
                    activeFilter === undefined && canManage
                      ? `Add your first ${config.singular} to get started.`
                      : activeFilter === undefined
                        ? `No ${config.kind} are available in the directory.`
                        : `No ${config.kind} have ${config.numericLabel.toLowerCase()} ${activeFilter}. Try another value or clear the filter.`
                  }
                  action={
                    activeFilter === undefined ? (
                      canManage && (
                        <button
                          className="button-primary"
                          onClick={() => setForm({})}
                        >
                          <Plus className="size-4" />
                          Add {config.singular}
                        </button>
                      )
                    ) : (
                      <button
                        className="button-secondary"
                        onClick={clearFilter}
                      >
                        Clear filter
                      </button>
                    )
                  }
                />
              ) : (
                <div className="table-scroll">
                  <table className="data-table">
                    <caption className="sr-only">
                      {config.title} directory, page {data.page + 1}
                    </caption>
                    <thead>
                      <tr>
                        <SortHeading
                          label="ID"
                          field="id"
                          sortBy={query.sortBy}
                          direction={query.direction}
                          onSort={sort}
                        />
                        <SortHeading
                          label={config.codeLabel}
                          field={config.codeKey}
                          sortBy={query.sortBy}
                          direction={query.direction}
                          onSort={sort}
                        />
                        <SortHeading
                          label={
                            config.kind === "students"
                              ? "Student name"
                              : "Course name"
                          }
                          field="name"
                          sortBy={query.sortBy}
                          direction={query.direction}
                          onSort={sort}
                        />
                        <SortHeading
                          label={config.numericLabel}
                          field={config.numericKey}
                          sortBy={query.sortBy}
                          direction={query.direction}
                          onSort={sort}
                        />
                        <th scope="col" className="text-right">
                          Actions
                        </th>
                      </tr>
                    </thead>
                    <tbody>
                      {data.content.map((item) => (
                        <tr key={item.id}>
                          <td className="text-slate-400">#{item.id}</td>
                          <td>
                            <span className="code-badge">
                              {config.getCode(item)}
                            </span>
                          </td>
                          <td>
                            <Link
                              className="inline-flex items-center gap-3 font-medium text-slate-800 hover:text-blue-600"
                              to={`/${config.kind}/${item.id}`}
                            >
                              <span
                                className={`flex size-8 shrink-0 items-center justify-center rounded-lg text-[10px] font-semibold ${config.kind === "students" ? "bg-blue-50 text-blue-600" : "bg-violet-50 text-violet-600"}`}
                              >
                                {config.kind === "students" ? (
                                  initials(item.name)
                                ) : (
                                  <BookOpen className="size-4" />
                                )}
                              </span>
                              <span className="max-w-80 break-words whitespace-normal">
                                {item.name}
                              </span>
                            </Link>
                          </td>
                          <td>
                            <span
                              className={
                                config.kind === "courses"
                                  ? "rounded-md bg-emerald-50 px-2 py-1 text-xs font-medium text-emerald-700"
                                  : "text-slate-600"
                              }
                            >
                              {config.getNumber(item)}
                              {config.kind === "courses" ? " credits" : ""}
                            </span>
                          </td>
                          <td>
                            <div className="flex justify-end gap-1">
                              <Link
                                className="icon-button"
                                aria-label={`View ${item.name}`}
                                to={`/${config.kind}/${item.id}`}
                              >
                                <Eye className="size-4" />
                              </Link>
                              {canManage && (
                                <>
                                  <button
                                    className="icon-button"
                                    aria-label={`Edit ${item.name}`}
                                    onClick={() => setForm({ item })}
                                  >
                                    <Pencil className="size-4" />
                                  </button>
                                  <button
                                    className="icon-button hover:bg-red-50 hover:text-red-600"
                                    aria-label={`Delete ${item.name}`}
                                    onClick={() => setDeleting(item)}
                                  >
                                    <Trash2 className="size-4" />
                                  </button>
                                </>
                              )}
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
              <Pagination
                {...data}
                onPage={(page) =>
                  setQuery((previous) => ({ ...previous, page }))
                }
                onSize={(size) =>
                  setQuery((previous) => ({ ...previous, page: 0, size }))
                }
              />
            </>
          )
        )}
      </section>
      <p className="mt-4 flex items-center gap-2 text-xs text-slate-400">
        <span className="size-1.5 rounded-full bg-slate-300" />
        {config.kind === "students"
          ? "Student codes uniquely identify each student."
          : "Course codes uniquely identify each course."}
      </p>
      {canManage && form && (
        <ResourceForm
          config={config}
          item={form.item}
          onClose={() => setForm(null)}
          onSaved={() => {
            notify(
              `${config.singular === "student" ? "Student" : "Course"} ${form.item ? "updated" : "added"} successfully.`,
            );
            reload();
          }}
        />
      )}
      {canManage && deleting && (
        <ConfirmDialog
          title={`Delete ${config.singular}?`}
          description={`“${deleting.name}” will be permanently removed. ${config.title} with active enrollments cannot be deleted; cancel their enrollments first.`}
          onClose={() => setDeleting(null)}
          onConfirm={async () => {
            await config.api.remove(deleting.id);
            notify(
              `${config.singular === "student" ? "Student" : "Course"} deleted successfully.`,
            );
            reload();
          }}
        />
      )}
    </>
  );
}
