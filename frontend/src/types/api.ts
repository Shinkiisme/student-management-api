export interface Student {
  id: number;
  studentCode: string;
  name: string;
  age: number;
}
export interface Course {
  id: number;
  code: string;
  name: string;
  credits: number;
}
export interface Enrollment {
  id: number;
  studentId: number;
  courseId: number;
}
export type StudentInput = Omit<Student, "id">;
export type CourseInput = Omit<Course, "id">;
export type EnrollmentInput = Omit<Enrollment, "id">;
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
export type Direction = "asc" | "desc";
export interface ListQuery {
  page: number;
  size: number;
  sortBy: string;
  direction: Direction;
  age?: number;
  credits?: number;
}
export interface ProblemDetail {
  title?: string;
  detail?: string;
  status?: number;
  errors?: Record<string, string>;
}
