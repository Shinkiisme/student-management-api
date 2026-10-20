import { useCallback, useState } from "react";
import { Link, useNavigate, useParams } from "react-router";
import {
  ArrowLeft,
  ArrowRight,
  BookOpen,
  Pencil,
  Trash2,
  Users,
} from "lucide-react";
import { ApiError } from "../api/http";
import { enrollmentsApi } from "../api/resources";
import { useAsync } from "../hooks/useAsync";
import { useDirectory } from "../hooks/useDirectory";
import { useNotify } from "../hooks/notificationContext";
import { useAuth } from "../hooks/authContext";
import type { ResourceConfig } from "../utils/resourceConfig";
import { integerValue } from "../utils/validation";
import { ResourceForm } from "../components/ResourceForm";
import { ConfirmDialog } from "../components/ConfirmDialog";
import { EnrollmentTable } from "../components/EnrollmentTable";
import { EmptyState, ErrorState, Loading, PageHeading } from "../components/ui";

function StudentEnrollments({ studentId }: { studentId: number }) {
  const load = useCallback(
    (signal: AbortSignal) => enrollmentsApi.byStudent(studentId, signal),
    [studentId],
  );
  const { data, error, pending, reload } = useAsync(load);
  const directory = useDirectory();
  return (
    <section className="panel mt-6">
      <div className="flex flex-wrap items-center justify-between gap-3 border-b border-slate-100 p-5">
        <h2 className="text-sm font-semibold">
          Course enrollments{" "}
          {data && <span className="ml-2 text-slate-400">({data.length})</span>}
        </h2>
        <Link
          className="inline-flex items-center gap-2 text-xs font-medium text-blue-600"
          to={`/enrollments?studentId=${studentId}`}
        >
          Manage enrollments
          <ArrowRight className="size-3.5" />
        </Link>
      </div>
      {directory.courses.error !== null && (
        <div className="p-5">
          <ErrorState
            error={directory.courses.error}
            retry={directory.courses.reload}
          />
        </div>
      )}
      {pending ? (
        <Loading />
      ) : error ? (
        <div className="p-5">
          <ErrorState error={error} retry={reload} />
        </div>
      ) : (
        data &&
        (data.length ? (
          <EnrollmentTable
            enrollments={data}
            students={directory.studentMap}
            courses={directory.courseMap}
          />
        ) : (
          <EmptyState
            title="No courses registered"
            description="This student has no enrollments. Register them for a course from the enrollments workspace."
          />
        ))
      )}
    </section>
  );
}

export function ResourceDetailsPage<T extends { id: number; name: string }, P>({
  config,
}: {
  config: ResourceConfig<T, P>;
}) {
  const canManage = useAuth().role === "ADMIN";
  const { id: rawId } = useParams();
  const id = integerValue(rawId ?? "", 1);
  const [editing, setEditing] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const notify = useNotify();
  const navigate = useNavigate();
  const load = useCallback(
    (signal: AbortSignal) =>
      id === null
        ? Promise.reject(
            new ApiError(400, {
              detail: "The record ID must be a positive integer.",
            }),
          )
        : config.api.get(id, signal),
    [config, id],
  );
  const { data, error, pending, reload } = useAsync(load);
  const Icon = config.kind === "students" ? Users : BookOpen;
  return (
    <>
      <Link
        className="mb-6 inline-flex items-center gap-2 text-sm font-medium text-slate-500 hover:text-blue-600"
        to={`/${config.kind}`}
      >
        <ArrowLeft className="size-4" />
        Back to {config.kind}
      </Link>
      <PageHeading
        eyebrow={`${config.singular.toUpperCase()} PROFILE`}
        title={
          data?.name ??
          `${config.singular === "student" ? "Student" : "Course"} details`
        }
        description={
          canManage
            ? `View and manage this ${config.singular}’s information.`
            : `View this ${config.singular}’s information.`
        }
        action={
          data &&
          canManage && (
            <div className="flex gap-2">
              <button
                className="button-secondary"
                onClick={() => setDeleting(true)}
              >
                <Trash2 className="size-4" />
                Delete
              </button>
              <button
                className="button-primary"
                onClick={() => setEditing(true)}
              >
                <Pencil className="size-4" />
                Edit {config.singular}
              </button>
            </div>
          )
        }
      />
      {pending ? (
        <div className="panel">
          <Loading />
        </div>
      ) : error ? (
        <ErrorState error={error} retry={reload} />
      ) : (
        data && (
          <>
            <section className="panel p-6 sm:p-8">
              <div className="mb-7 flex items-center gap-4">
                <div className="rounded-2xl bg-blue-50 p-4 text-blue-600">
                  <Icon className="size-7" />
                </div>
                <div className="min-w-0">
                  <p className="font-semibold break-words text-slate-900">
                    {data.name}
                  </p>
                  <p className="mt-1 text-sm text-slate-500">
                    {config.getCode(data)}
                  </p>
                </div>
              </div>
              <dl className="grid gap-6 border-t border-slate-100 pt-6 sm:grid-cols-2 lg:grid-cols-4">
                {[
                  { label: "Record ID", value: `#${data.id}` },
                  { label: config.codeLabel, value: config.getCode(data) },
                  { label: "Name", value: data.name },
                  { label: config.numericLabel, value: config.getNumber(data) },
                ].map((field) => (
                  <div key={field.label}>
                    <dt className="detail-label">{field.label}</dt>
                    <dd className="mt-2 break-words text-sm font-medium text-slate-800">
                      {field.value}
                    </dd>
                  </div>
                ))}
              </dl>
            </section>
            {config.kind === "students" && (
              <StudentEnrollments studentId={data.id} />
            )}
          </>
        )
      )}
      {canManage && editing && data && (
        <ResourceForm
          key={data.id}
          config={config}
          item={data}
          onClose={() => setEditing(false)}
          onSaved={() => {
            notify(
              `${config.singular === "student" ? "Student" : "Course"} updated successfully.`,
            );
            reload();
          }}
        />
      )}
      {canManage && deleting && data && (
        <ConfirmDialog
          title={`Delete ${config.singular}?`}
          description={`“${data.name}” will be permanently removed. Cancel any enrollments first; the API prevents deleting records in use.`}
          onClose={() => setDeleting(false)}
          onConfirm={async () => {
            await config.api.remove(data.id);
            notify(
              `${config.singular === "student" ? "Student" : "Course"} deleted successfully.`,
            );
            navigate(`/${config.kind}`);
          }}
        />
      )}
    </>
  );
}
