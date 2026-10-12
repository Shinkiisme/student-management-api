import { useCallback, useState } from "react";
import { useSearchParams } from "react-router";
import { AlertCircle, Link2, Plus, RefreshCw } from "lucide-react";
import { ApiError } from "../api/http";
import { enrollmentsApi } from "../api/resources";
import { useAsync } from "../hooks/useAsync";
import { useDirectory } from "../hooks/useDirectory";
import { useNotify } from "../hooks/notificationContext";
import type { Enrollment } from "../types/api";
import { integerValue } from "../utils/validation";
import { DirectorySelect } from "../components/DirectorySelect";
import { EnrollmentTable } from "../components/EnrollmentTable";
import { EnrollmentForm } from "../components/EnrollmentForm";
import { EnrollmentDetails } from "../components/EnrollmentDetails";
import { ConfirmDialog } from "../components/ConfirmDialog";
import { EmptyState, ErrorState, Loading, PageHeading } from "../components/ui";

export function EnrollmentsPage() {
  const [params, setParams] = useSearchParams();
  const studentFilter = params.get("studentId") ?? "";
  const [adding, setAdding] = useState(false);
  const [deleting, setDeleting] = useState<Enrollment | null>(null);
  const [viewing, setViewing] = useState<Enrollment | null>(null);
  const directory = useDirectory();
  const notify = useNotify();
  const load = useCallback(
    (signal: AbortSignal) => {
      if (!studentFilter) return enrollmentsApi.list(signal);
      const id = integerValue(studentFilter, 1);
      if (id === null)
        return Promise.reject(
          new ApiError(400, {
            detail: "The student filter must be a positive integer.",
          }),
        );
      return enrollmentsApi.byStudent(id, signal);
    },
    [studentFilter],
  );
  const { data, error, pending, reload } = useAsync(load);
  const directoryPending =
    directory.students.pending || directory.courses.pending;
  const directoryReady =
    directory.students.data !== null && directory.courses.data !== null;
  const studentOptions =
    directory.students.data?.map((student) => ({
      id: student.id,
      label: `${student.name} · ${student.studentCode}`,
    })) ?? [];
  // Preserve links to an unknown/deleted student so the backend's 404 remains visible.
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
                {data.length}
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
              onChange={(value) => setParams(value ? { studentId: value } : {})}
              options={studentOptions}
              placeholder="All students"
              disabled={directory.students.pending}
            />
          </div>
          {studentFilter && (
            <button
              className="mt-3 text-xs font-medium text-blue-600 hover:underline"
              onClick={() => setParams({})}
            >
              Clear student filter
            </button>
          )}
        </div>
        {pending ? (
          <Loading label="Loading enrollments…" />
        ) : error ? (
          <div className="p-5">
            <ErrorState error={error} retry={reload} />
          </div>
        ) : (
          data &&
          (data.length ? (
            <>
              <EnrollmentTable
                enrollments={data}
                students={directory.studentMap}
                courses={directory.courseMap}
                onCancel={setDeleting}
                onView={setViewing}
              />
              <div className="border-t border-slate-100 px-5 py-4 text-xs text-slate-500">
                {data.length.toLocaleString()} enrollment
                {data.length === 1 ? "" : "s"} · Showing all returned records
              </div>
            </>
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
          ))
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
