import { useRef, useState } from "react";
import type { FormEvent } from "react";
import { Link } from "react-router";
import { LoaderCircle } from "lucide-react";
import { ApiError } from "../api/http";
import { enrollmentsApi } from "../api/resources";
import type { Course, Student } from "../types/api";
import { integerValue } from "../utils/validation";
import { DirectorySelect } from "./DirectorySelect";
import { Modal } from "./Modal";
import { ErrorState } from "./ui";

export function EnrollmentForm({
  students,
  courses,
  initialStudentId = "",
  onSaved,
  onClose,
}: {
  students: Student[];
  courses: Course[];
  initialStudentId?: string;
  onSaved: () => void;
  onClose: () => void;
}) {
  const [studentId, setStudentId] = useState(() =>
    students.some((student) => String(student.id) === initialStudentId)
      ? initialStudentId
      : "",
  );
  const [courseId, setCourseId] = useState("");
  const [pending, setPending] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<unknown>(null);
  const lock = useRef(false);
  const empty = !students.length || !courses.length;
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (lock.current) return;
    const student = integerValue(studentId, 1);
    const course = integerValue(courseId, 1);
    const nextErrors: Record<string, string> = {};
    if (student === null) nextErrors.studentId = "Select a student.";
    if (course === null) nextErrors.courseId = "Select a course.";
    setErrors(nextErrors);
    setError(null);
    if (student === null || course === null) return;
    lock.current = true;
    setPending(true);
    try {
      await enrollmentsApi.create({ studentId: student, courseId: course });
      onSaved();
      onClose();
    } catch (error) {
      setError(error);
      if (error instanceof ApiError) setErrors(error.errors);
    } finally {
      lock.current = false;
      setPending(false);
    }
  }
  return (
    <Modal
      title="Register an enrollment"
      description="Connect a student with a course. Each student can enroll in a course once."
      onClose={onClose}
      busy={pending}
    >
      <form noValidate onSubmit={(event) => void submit(event)}>
        <div className="space-y-5 p-6">
          {error !== null && <ErrorState error={error} />}
          {empty && (
            <p className="rounded-lg bg-amber-50 p-3 text-sm leading-6 text-amber-800">
              Add at least one{" "}
              <Link className="font-semibold underline" to="/students">
                student
              </Link>{" "}
              and one{" "}
              <Link className="font-semibold underline" to="/courses">
                course
              </Link>{" "}
              before registering an enrollment.
            </p>
          )}
          <DirectorySelect
            label="Student"
            value={studentId}
            onChange={(value) => {
              setStudentId(value);
              setErrors({ ...errors, studentId: "" });
            }}
            options={students.map((student) => ({
              id: student.id,
              label: `${student.name} · ${student.studentCode}`,
            }))}
            placeholder="Choose a student"
            error={errors.studentId}
            disabled={pending}
            required
          />
          <DirectorySelect
            label="Course"
            value={courseId}
            onChange={(value) => {
              setCourseId(value);
              setErrors({ ...errors, courseId: "" });
            }}
            options={courses.map((course) => ({
              id: course.id,
              label: `${course.code} · ${course.name} (${course.credits} credits)`,
            }))}
            placeholder="Choose a course"
            error={errors.courseId}
            disabled={pending}
            required
          />
        </div>
        <div className="dialog-footer">
          <button
            className="button-secondary"
            type="button"
            disabled={pending}
            onClick={onClose}
          >
            Cancel
          </button>
          <button
            className="button-primary"
            type="submit"
            disabled={pending || empty}
          >
            {pending && <LoaderCircle className="size-4 animate-spin" />}
            {pending ? "Registering…" : "Register enrollment"}
          </button>
        </div>
      </form>
    </Modal>
  );
}
