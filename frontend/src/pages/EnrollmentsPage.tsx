import { useCallback, useEffect, useState } from "react";
import { useSearchParams } from "react-router";
import { AlertCircle, Link2, Plus, RefreshCw } from "lucide-react";
import { ApiError } from "../api/http";
import { enrollmentsApi } from "../api/resources";
import { useAsync } from "../hooks/useAsync";
import { useDirectory } from "../hooks/useDirectory";
import { useNotify } from "../hooks/notificationContext";
import type {
  Enrollment,
  EnrollmentPageQuery,
  EnrollmentSortField,
} from "../types/api";
import { integerValue } from "../utils/validation";
import { DirectorySelect } from "../components/DirectorySelect";
import { EnrollmentTable } from "../components/EnrollmentTable";
import { EnrollmentForm } from "../components/EnrollmentForm";
import { EnrollmentDetails } from "../components/EnrollmentDetails";
import { ConfirmDialog } from "../components/ConfirmDialog";
import { Pagination } from "../components/Pagination";
import { EmptyState, ErrorState, Loading, PageHeading } from "../components/ui";

export function EnrollmentsPage() {
  const [params, setParams] = useSearchParams();
  const requestedStudent = params.get("studentId") ?? "";
  const studentId = requestedStudent
    ? integerValue(requestedStudent, 1)
    : undefined;
  const studentFilter =
    studentId === undefined
      ? ""
      : studentId === null
        ? requestedStudent
        : String(studentId);
  const [query, setQuery] = useState<EnrollmentPageQuery>({
    page: 0,
    size: 10,
    sortBy: "id",
    direction: "asc",
  });
  const [adding, setAdding] = useState(false);
  const [deleting, setDeleting] = useState<Enrollment | null>(null);
  const [viewing, setViewing] = useState<Enrollment | null>(null);
  const directory = useDirectory();
  const notify = useNotify();
  const load = useCallback(
    (signal: AbortSignal) => {
      if (studentId === null)
        return Promise.reject(
          new ApiError(400, {
            detail: "The student filter must be a positive integer.",
          }),
        );
      return enrollmentsApi.list({ ...query, studentId }, signal);
    },
    [studentId, query],
  );
  const { data, error, pending, reload } = useAsync(load);
  const recoveringPage =
    data !== null && data.page > 0 && data.page >= data.totalPages;
  useEffect(() => {
    setQuery((previous) =>
      previous.page === 0 ? previous : { ...previous, page: 0 },
    );
  }, [studentFilter]);
  useEffect(() => {
    if (data && data.page > 0 && data.page >= data.totalPages) {
      setQuery((previous) => ({
        ...previous,
        page: Math.max(0, data.totalPages - 1),
      }));
    }
  }, [data]);
  function changeFilter(value: string) {
    setQuery((previous) => ({ ...previous, page: 0 }));
    setParams((previous) => {
      const next = new URLSearchParams(previous);
      if (value) next.set("studentId", value);
      else next.delete("studentId");
      return next;
    });
  }
  function sort(field: EnrollmentSortField) {
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
  const directoryPending =
    directory.students.pending || directory.courses.pending;
  const directoryReady =
    directory.students.data !== null && directory.courses.data !== null;
  const studentOptions =
    directory.students.data?.map((student) => ({
      id: student.id,
      label: `${student.name} · ${student.studentCode}`,
    })) ?? [];
  // Preserve a positive ID absent from the directory so its server-side filter stays visible.
  if (
    studentFilter &&
    !studentOptions.some((option) => String(option.id) === studentFilter)
  ) {
    const id = integerValue(studentFilter, 1);
    if (id !== null) studentOptions.push({ id, label: `Student #${id}` });
  }
  return (
    <>
      <PageHeading
        title="Enrollments"
        description="Bring students and courses together. Manage every registration in one place."
        action={
          <button
            className="button-primary"
            disabled={!directoryReady}
            onClick={() => setAdding(true)}
          >
            <Plus className="size-4" />
            {directoryPending ? "Loading directory…" : "Register enrollment"}
          </button>
        }
      />
      {(directory.students.error !== null ||
        directory.courses.error !== null) && (
        <div className="mb-5 space-y-3">
          <div className="flex items-center gap-2 text-sm text-amber-800">
            <AlertCircle className="size-4" />
            Some names are unavailable. Registration needs the complete
            directory.
          </div>
          {directory.students.error !== null && (
            <ErrorState
              error={directory.students.error}
              retry={directory.students.reload}
            />
          )}
          {directory.courses.error !== null && (
            <ErrorState
              error={directory.courses.error}
              retry={directory.courses.reload}
            />
          )}
        </div>
      )}
      <section className="panel">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 px-5 py-5">
          <div className="flex items-center gap-3">
            <span className="rounded-lg bg-blue-50 p-2 text-blue-600">
              <Link2 className="size-4" />
            </span>
            <h2 className="text-sm font-semibold">
              {studentFilter ? "Student enrollments" : "All enrollments"}
            </h2>
            {data && (
              <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs text-slate-600">
                {data.totalElements.toLocaleString()}
              </span>
            )}
          </div>
          <button
            className="button-quiet"
            disabled={pending || directoryPending}
            onClick={() => {
              reload();
              directory.students.reload();
              directory.courses.reload();
            }}
          >
            <RefreshCw className="size-3.5" />
            Refresh
          </button>
        </div>
        <div className="border-b border-slate-100 p-5">
          <div className="max-w-lg">
            <DirectorySelect
              label="Filter by student"
              value={studentFilter}
              onChange={changeFilter}
              options={studentOptions}
              placeholder="All students"
              disabled={directory.students.pending}
            />
          </div>
          {studentFilter && (
            <button
              className="mt-3 text-xs font-medium text-blue-600 hover:underline"
              onClick={() => changeFilter("")}
            >
              Clear student filter
            </button>
          )}
        </div>
        {pending || recoveringPage ? (
          <Loading label="Loading enrollments…" />
        ) : error ? (
          <div className="p-5">
            <ErrorState error={error} retry={reload} />
          </div>
        ) : (
          data && (
            <>
              {data.content.length ? (
                <EnrollmentTable
                  enrollments={data.content}
                  students={directory.studentMap}
                  courses={directory.courseMap}
                  onCancel={setDeleting}
                  onView={setViewing}
                  sorting={{ ...query, onSort: sort }}
                />
              ) : (
                <EmptyState
                  title={
                    studentFilter
                      ? "No enrollments for this student"
                      : "No enrollments yet"
                  }
                  description="Register a student for a course to start building their learning journey."
                  action={
                    <button
                      className="button-primary"
                      disabled={!directoryReady}
                      onClick={() => setAdding(true)}
                    >
                      <Plus className="size-4" />
                      Register enrollment
                    </button>
                  }
                />
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
      {adding && directory.students.data && directory.courses.data && (
        <EnrollmentForm
          students={directory.students.data}
          courses={directory.courses.data}
          initialStudentId={studentFilter}
          onClose={() => setAdding(false)}
          onSaved={() => {
            notify("Enrollment registered successfully.");
            reload();
          }}
        />
      )}
      {viewing && (
        <EnrollmentDetails
          id={viewing.id}
          students={directory.studentMap}
          courses={directory.courseMap}
          onClose={() => setViewing(null)}
        />
      )}
      {deleting && (
        <ConfirmDialog
          title="Cancel enrollment?"
          description={`Remove ${directory.studentMap.get(deleting.studentId)?.name ?? `student #${deleting.studentId}`} from ${directory.courseMap.get(deleting.courseId)?.name ?? `course #${deleting.courseId}`}? The student and course will remain in your directory.`}
          confirmLabel="Cancel enrollment"
          onClose={() => setDeleting(null)}
          onConfirm={async () => {
            await enrollmentsApi.remove(deleting.id);
            notify("Enrollment canceled successfully.");
            reload();
          }}
        />
      )}
    </>
  );
}
