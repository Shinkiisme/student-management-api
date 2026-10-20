import { Link } from "react-router";
import { BookOpen, Eye, Unlink } from "lucide-react";
import type {
  Course,
  Direction,
  Enrollment,
  EnrollmentSortField,
  Student,
} from "../types/api";
import { initials } from "../utils/validation";
import { SortHeading } from "./ui";

export function EnrollmentTable({
  enrollments,
  students,
  courses,
  onCancel,
  onView,
  sorting,
}: {
  enrollments: Enrollment[];
  students: Map<number, Student>;
  courses: Map<number, Course>;
  onCancel?: (enrollment: Enrollment) => void;
  onView?: (enrollment: Enrollment) => void;
  sorting?: {
    sortBy: EnrollmentSortField;
    direction: Direction;
    onSort: (field: EnrollmentSortField) => void;
  };
}) {
  return (
    <div className="table-scroll">
      <table className="data-table">
        <caption className="sr-only">Student course enrollments</caption>
        <thead>
          <tr>
            {(
              [
                { field: "id", label: "Enrollment ID" },
                { field: "studentId", label: "Student ID" },
                { field: "courseId", label: "Course ID" },
              ] as const
            ).map(({ field, label }) =>
              sorting ? (
                <SortHeading
                  key={field}
                  label={label}
                  field={field}
                  sortBy={sorting.sortBy}
                  direction={sorting.direction}
                  onSort={() => sorting.onSort(field)}
                />
              ) : (
                <th key={field} scope="col">
                  {label}
                </th>
              ),
            )}
            <th scope="col">Credits</th>
            {(onCancel || onView) && (
              <th scope="col" className="text-right">
                Actions
              </th>
            )}
          </tr>
        </thead>
        <tbody>
          {enrollments.map((enrollment) => {
            const student = students.get(enrollment.studentId);
            const course = courses.get(enrollment.courseId);
            return (
              <tr key={enrollment.id}>
                <td className="text-slate-400">#{enrollment.id}</td>
                <td>
                  <Link
                    to={`/students/${enrollment.studentId}`}
                    className="flex items-center gap-3 hover:text-blue-600"
                  >
                    <span className="flex size-8 shrink-0 items-center justify-center rounded-lg bg-blue-50 text-[10px] font-semibold text-blue-600">
                      {student ? initials(student.name) : "#"}
                    </span>
                    <div className="min-w-0">
                      <p className="max-w-64 break-words whitespace-normal font-medium text-slate-800">
                        {student?.name ?? `Student #${enrollment.studentId}`}
                      </p>
                      <p className="mt-0.5 text-xs text-slate-400">
                        {student?.studentCode ?? "Student details unavailable"}
                      </p>
                    </div>
                  </Link>
                </td>
                <td>
                  <Link
                    to={`/courses/${enrollment.courseId}`}
                    className="flex items-center gap-3 hover:text-blue-600"
                  >
                    <span className="rounded-lg bg-violet-50 p-2 text-violet-600">
                      <BookOpen className="size-4" />
                    </span>
                    <div>
                      <p className="max-w-80 break-words whitespace-normal font-medium text-slate-800">
                        {course?.name ?? `Course #${enrollment.courseId}`}
                      </p>
                      <p className="mt-0.5 text-xs text-slate-400">
                        {course?.code ?? "Course details unavailable"}
                      </p>
                    </div>
                  </Link>
                </td>
                <td className="text-slate-600">{course?.credits ?? "—"}</td>
                {(onCancel || onView) && (
                  <td>
                    <div className="flex justify-end gap-1">
                      {onView && (
                        <button
                          className="icon-button"
                          aria-label={`View enrollment ${enrollment.id}`}
                          onClick={() => onView(enrollment)}
                        >
                          <Eye className="size-4" />
                        </button>
                      )}
                      {onCancel && (
                        <button
                          className="icon-button hover:bg-red-50 hover:text-red-600"
                          aria-label={`Cancel enrollment ${enrollment.id}`}
                          onClick={() => onCancel(enrollment)}
                        >
                          <Unlink className="size-4" />
                        </button>
                      )}
                    </div>
                  </td>
                )}
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}
