import { Link } from "react-router";
import {
  ArrowRight,
  BookOpen,
  ChevronRight,
  GraduationCap,
  Link2,
  RefreshCw,
  Users,
} from "lucide-react";
import type { LucideIcon } from "lucide-react";
import { coursesApi, enrollmentsApi, studentsApi } from "../api/resources";
import { useAsync } from "../hooks/useAsync";
import { EmptyState, ErrorState, Loading, PageHeading } from "../components/ui";
import { initials } from "../utils/validation";

const query = { page: 0, size: 5, sortBy: "id", direction: "asc" as const };
const loadStudents = (signal: AbortSignal) => studentsApi.list(query, signal);
const loadCourses = (signal: AbortSignal) => coursesApi.list(query, signal);
const loadEnrollments = (signal: AbortSignal) => enrollmentsApi.list(signal);

function StatCard({
  label,
  value,
  description,
  icon: Icon,
  tone,
  to,
  pending,
  error,
  retry,
}: {
  label: string;
  value?: number;
  description: string;
  icon: LucideIcon;
  tone: string;
  to: string;
  pending: boolean;
  error: unknown;
  retry: () => void;
}) {
  return (
    <section className="panel p-5">
      <div className="flex items-center justify-between">
        <p className="text-xs font-medium text-slate-500">{label}</p>
        <span className={`rounded-xl p-2.5 ${tone}`}>
          <Icon className="size-5" />
        </span>
      </div>
      <p className="mt-3 text-3xl font-bold tracking-tight text-slate-900">
        <span aria-hidden="true">
          {pending ? (
            <span className="inline-block h-8 w-16 animate-pulse rounded bg-slate-100" />
          ) : (
            (value?.toLocaleString() ?? "—")
          )}
        </span>
        <span className="sr-only">
          {pending
            ? `${label} loading`
            : value === undefined
              ? `${label} unavailable`
              : `${value} ${label}`}
        </span>
      </p>
      {error ? (
        <div className="mt-3">
          <ErrorState error={error} retry={retry} />
        </div>
      ) : (
        <p className="mt-3 text-xs text-slate-400">{description}</p>
      )}
      <Link
        className="mt-4 inline-flex items-center gap-1.5 text-xs font-semibold text-blue-600 hover:text-blue-800"
        to={to}
      >
        View {label.toLowerCase()}
        <ArrowRight className="size-3.5" />
      </Link>
    </section>
  );
}

export function DashboardPage() {
  const students = useAsync(loadStudents);
  const courses = useAsync(loadCourses);
  const enrollments = useAsync(loadEnrollments);
  const pending = students.pending || courses.pending || enrollments.pending;
  const courseRows =
    courses.data?.content.map((course) => ({
      ...course,
      enrollmentCount:
        enrollments.data?.filter(
          (enrollment) => enrollment.courseId === course.id,
        ).length ?? null,
    })) ?? [];
  const maximum = Math.max(
    1,
    ...courseRows.map((course) => course.enrollmentCount ?? 0),
  );
  return (
    <>
      <PageHeading
        eyebrow="CAMPUS OVERVIEW"
        title="Your campus, at a glance"
        description="A clear view of your students, curriculum, and course registrations."
        action={
          <button
            className="button-secondary"
            disabled={pending}
            onClick={() => {
              students.reload();
              courses.reload();
              enrollments.reload();
            }}
          >
            <RefreshCw className={`size-4 ${pending ? "animate-spin" : ""}`} />
            Refresh overview
          </button>
        }
      />
      <div className="mb-7 flex flex-wrap items-center justify-between gap-5 rounded-2xl border border-blue-100 bg-gradient-to-r from-blue-50 via-blue-50/60 to-white p-6 sm:p-7">
        <div>
          <div className="mb-3 inline-flex items-center gap-2 text-xs font-medium text-blue-600">
            <GraduationCap className="size-4" />
            Student management workspace
          </div>
          <h2 className="text-xl font-semibold tracking-tight text-slate-900">
            Good administration starts with clarity.
          </h2>
          <p className="mt-2 max-w-xl text-sm leading-6 text-slate-500">
            Keep your student records organized, build your course catalog, and
            make every enrollment count.
          </p>
        </div>
        <Link to="/students" className="button-primary">
          Explore students
          <ArrowRight className="size-4" />
        </Link>
      </div>
      <div className="grid gap-5 md:grid-cols-3">
        <StatCard
          label="Students"
          value={students.data?.totalElements}
          description="Students in your directory"
          icon={Users}
          tone="bg-blue-50 text-blue-600"
          to="/students"
          pending={students.pending}
          error={students.error}
          retry={students.reload}
        />
        <StatCard
          label="Courses"
          value={courses.data?.totalElements}
          description="Courses in your catalog"
          icon={BookOpen}
          tone="bg-violet-50 text-violet-600"
          to="/courses"
          pending={courses.pending}
          error={courses.error}
          retry={courses.reload}
        />
        <StatCard
          label="Enrollments"
          value={enrollments.data?.length}
          description="Student–course registrations"
          icon={Link2}
          tone="bg-emerald-50 text-emerald-600"
          to="/enrollments"
          pending={enrollments.pending}
          error={enrollments.error}
          retry={enrollments.reload}
        />
      </div>
      <div className="mt-7 grid gap-6 xl:grid-cols-[1.15fr_1fr]">
        <section className="panel">
          <div className="flex items-center justify-between border-b border-slate-100 p-5">
            <div>
              <h2 className="text-sm font-semibold text-slate-900">
                Student directory
              </h2>
              <p className="mt-1 text-xs text-slate-400">
                First five students, ordered by ID
              </p>
            </div>
            <Link
              className="text-xs font-medium text-blue-600 hover:underline"
              to="/students"
            >
              View all
            </Link>
          </div>
          {students.pending ? (
            <Loading />
          ) : students.error ? (
            <div className="p-5">
              <ErrorState error={students.error} retry={students.reload} />
            </div>
          ) : students.data?.content.length ? (
            <div className="divide-y divide-slate-100">
              {students.data.content.map((student) => (
                <Link
                  key={student.id}
                  to={`/students/${student.id}`}
                  className="flex items-center gap-3 px-5 py-4 hover:bg-slate-50"
                >
                  <span className="flex size-9 shrink-0 items-center justify-center rounded-xl bg-blue-50 text-xs font-semibold text-blue-600">
                    {initials(student.name)}
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-medium text-slate-800">
                      {student.name}
                    </p>
                    <p className="mt-1 text-xs text-slate-400">
                      {student.studentCode}
                    </p>
                  </div>
                  <span className="text-xs text-slate-500">
                    Age {student.age}
                  </span>
                  <ChevronRight className="ml-2 size-4 text-slate-300" />
                </Link>
              ))}
            </div>
          ) : (
            <EmptyState
              title="Your directory starts here"
              description="Add a student to see your campus grow."
              action={
                <Link to="/students" className="button-primary">
                  Add a student
                </Link>
              }
            />
          )}
        </section>
        <section className="panel">
          <div className="flex items-center justify-between border-b border-slate-100 p-5">
            <div>
              <h2 className="text-sm font-semibold text-slate-900">
                Course snapshot
              </h2>
              <p className="mt-1 text-xs text-slate-400">
                Enrollment counts for the first five courses
              </p>
            </div>
            <Link
              className="text-xs font-medium text-blue-600 hover:underline"
              to="/courses"
            >
              View all
            </Link>
          </div>
          {courses.pending || enrollments.pending ? (
            <Loading />
          ) : courses.error || enrollments.error ? (
            <div className="p-5">
              <ErrorState
                error={courses.error || enrollments.error}
                retry={() => {
                  courses.reload();
                  enrollments.reload();
                }}
              />
            </div>
          ) : courseRows.length ? (
            <div className="space-y-6 p-5">
              {courseRows.map((course) => (
                <div key={course.id}>
                  <div className="mb-2 flex items-start justify-between gap-3">
                    <Link
                      className="min-w-0 text-xs font-medium break-words text-slate-700 hover:text-blue-600"
                      to={`/courses/${course.id}`}
                    >
                      <span className="mr-2 text-slate-400">{course.code}</span>
                      {course.name}
                    </Link>
                    <span className="shrink-0 text-xs font-semibold text-slate-600">
                      {course.enrollmentCount}
                    </span>
                  </div>
                  <div
                    role="img"
                    className="h-1.5 overflow-hidden rounded-full bg-slate-100"
                    aria-label={`${course.name}: ${course.enrollmentCount} enrollments`}
                  >
                    <div
                      className="h-full rounded-full bg-blue-500"
                      style={{
                        width: `${((course.enrollmentCount ?? 0) / maximum) * 100}%`,
                      }}
                    />
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <EmptyState
              title="No courses available"
              description="Build your course catalog to see enrollment counts here."
            />
          )}
        </section>
      </div>
      <div className="mt-7 grid gap-4 md:grid-cols-3">
        {[
          {
            to: "/students",
            icon: Users,
            title: "Manage students",
            description: "Add, update, and organize student records.",
          },
          {
            to: "/courses",
            icon: BookOpen,
            title: "Build your curriculum",
            description: "Manage course details and credit values.",
          },
          {
            to: "/enrollments",
            icon: Link2,
            title: "Connect the dots",
            description: "Register students and manage enrollments.",
          },
        ].map(({ to, icon: Icon, title, description }) => (
          <Link
            key={to}
            to={to}
            className="group flex items-start gap-3 rounded-xl border border-slate-200/80 bg-white p-4 hover:border-blue-200 hover:shadow-sm"
          >
            <Icon className="mt-0.5 size-4 shrink-0 text-slate-400 group-hover:text-blue-600" />
            <div className="flex-1">
              <h3 className="text-xs font-semibold text-slate-800">{title}</h3>
              <p className="mt-1.5 text-xs leading-5 text-slate-400">
                {description}
              </p>
            </div>
            <ArrowRight className="mt-0.5 size-4 shrink-0 text-slate-300 group-hover:text-blue-600" />
          </Link>
        ))}
      </div>
    </>
  );
}
