import { useMemo } from "react";
import { collectAll, coursesApi, studentsApi } from "../api/resources";
import { useAsync } from "./useAsync";

const loadStudents = (signal: AbortSignal) =>
  collectAll(studentsApi.list, signal);
const loadCourses = (signal: AbortSignal) =>
  collectAll(coursesApi.list, signal);

export function useDirectory() {
  const students = useAsync(loadStudents);
  const courses = useAsync(loadCourses);
  const studentMap = useMemo(
    () => new Map(students.data?.map((student) => [student.id, student])),
    [students.data],
  );
  const courseMap = useMemo(
    () => new Map(courses.data?.map((course) => [course.id, course])),
    [courses.data],
  );
  return { students, courses, studentMap, courseMap };
}
