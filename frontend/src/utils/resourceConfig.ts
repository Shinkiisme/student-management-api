import { coursesApi, studentsApi } from "../api/resources";
import type { ResourceApi } from "../api/resources";
import type { Course, CourseInput, Student, StudentInput } from "../types/api";
import type { FieldDefinition, FormValues } from "./validation";

export interface ResourceConfig<T extends { id: number; name: string }, P> {
  kind: "students" | "courses";
  singular: string;
  title: string;
  description: string;
  codeKey: string;
  codeLabel: string;
  numericKey: string;
  numericLabel: string;
  numericMin: number;
  fields: FieldDefinition[];
  api: ResourceApi<T, P>;
  getCode: (item: T) => string;
  getNumber: (item: T) => number;
  values: (item?: T) => FormValues;
  input: (values: FormValues) => P;
}

export const studentConfig: ResourceConfig<Student, StudentInput> = {
  kind: "students",
  singular: "student",
  title: "Students",
  description:
    "A home for every student. Keep your student directory up to date.",
  codeKey: "studentCode",
  codeLabel: "Student code",
  numericKey: "age",
  numericLabel: "Age",
  numericMin: 0,
  fields: [
    {
      key: "studentCode",
      label: "Student code",
      type: "text",
      maxLength: 20,
      placeholder: "e.g. STU2026001",
    },
    {
      key: "name",
      label: "Full name",
      type: "text",
      maxLength: 100,
      placeholder: "Enter the student’s full name",
    },
    {
      key: "age",
      label: "Age",
      type: "number",
      min: 0,
      placeholder: "e.g. 20",
    },
  ],
  api: studentsApi,
  getCode: (student) => student.studentCode,
  getNumber: (student) => student.age,
  values: (student) => ({
    studentCode: student?.studentCode ?? "",
    name: student?.name ?? "",
    age: student ? String(student.age) : "",
  }),
  input: (values) => ({
    studentCode: values.studentCode.trim(),
    name: values.name.trim(),
    age: Number(values.age),
  }),
};

export const courseConfig: ResourceConfig<Course, CourseInput> = {
  kind: "courses",
  singular: "course",
  title: "Courses",
  description:
    "Shape the curriculum. Organize courses and their credit requirements.",
  codeKey: "code",
  codeLabel: "Course code",
  numericKey: "credits",
  numericLabel: "Credits",
  numericMin: 1,
  fields: [
    {
      key: "code",
      label: "Course code",
      type: "text",
      maxLength: 20,
      placeholder: "e.g. CS101",
    },
    {
      key: "name",
      label: "Course name",
      type: "text",
      maxLength: 100,
      placeholder: "Enter the course name",
    },
    {
      key: "credits",
      label: "Credits",
      type: "number",
      min: 1,
      placeholder: "e.g. 4",
    },
  ],
  api: coursesApi,
  getCode: (course) => course.code,
  getNumber: (course) => course.credits,
  values: (course) => ({
    code: course?.code ?? "",
    name: course?.name ?? "",
    credits: course ? String(course.credits) : "",
  }),
  input: (values) => ({
    code: values.code.trim(),
    name: values.name.trim(),
    credits: Number(values.credits),
  }),
};
