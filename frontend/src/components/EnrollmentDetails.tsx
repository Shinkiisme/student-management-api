import { useCallback } from "react";
import { Link } from "react-router";
import { enrollmentsApi } from "../api/resources";
import { useAsync } from "../hooks/useAsync";
import type { Course, Student } from "../types/api";
import { Modal } from "./Modal";
import { ErrorState, Loading } from "./ui";

export function EnrollmentDetails({
  id,
  students,
  courses,
  onClose,
}: {
  id: number;
  students: Map<number, Student>;
  courses: Map<number, Course>;
  onClose: () => void;
}) {
  const load = useCallback(
    (signal: AbortSignal) => enrollmentsApi.get(id, signal),
    [id],
  );
  const { data, error, pending, reload } = useAsync(load);
  return (
    <Modal title={`Enrollment #${id}`} onClose={onClose}>
      <div className="p-6">
        {pending ? (
          <Loading />
        ) : error ? (
          <ErrorState error={error} retry={reload} />
        ) : (
          data && (
            <dl className="space-y-5">
              <div>
                <dt className="detail-label">Student</dt>
                <dd className="mt-2">
                  <Link
                    className="font-medium break-words text-blue-600 hover:underline"
                    to={`/students/${data.studentId}`}
                  >
                    {students.get(data.studentId)?.name ??
                      `Student #${data.studentId}`}
                  </Link>
                  <p className="mt-1 text-xs text-slate-500">
                    {students.get(data.studentId)?.studentCode ??
                      `ID ${data.studentId}`}
                  </p>
                </dd>
              </div>
              <div>
                <dt className="detail-label">Course</dt>
                <dd className="mt-2">
                  <Link
                    className="font-medium break-words text-blue-600 hover:underline"
                    to={`/courses/${data.courseId}`}
                  >
                    {courses.get(data.courseId)?.name ??
                      `Course #${data.courseId}`}
                  </Link>
                  <p className="mt-1 text-xs text-slate-500">
                    {courses.get(data.courseId)?.code ?? `ID ${data.courseId}`}
                  </p>
                </dd>
              </div>
            </dl>
          )
        )}
      </div>
      <div className="dialog-footer">
        <button className="button-secondary" onClick={onClose}>
          Close
        </button>
      </div>
    </Modal>
  );
}
