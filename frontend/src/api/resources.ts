import { request } from "./http";
import type {
  Course,
  CourseInput,
  Enrollment,
  EnrollmentInput,
  EnrollmentListQuery,
  EnrollmentPageQuery,
  ListQuery,
  PageResponse,
  Student,
  StudentInput,
} from "../types/api";

export function queryString(query: ListQuery | EnrollmentListQuery): string {
  const params = new URLSearchParams();
  for (const [key, value] of Object.entries(query)) {
    if (value !== undefined) params.set(key, String(value));
  }
  return params.toString();
}

export interface ResourceApi<T, P> {
  list: (query: ListQuery, signal?: AbortSignal) => Promise<PageResponse<T>>;
  get: (id: number, signal?: AbortSignal) => Promise<T>;
  create: (input: P) => Promise<T>;
  update: (id: number, input: P) => Promise<T>;
  remove: (id: number) => Promise<void>;
}

function resourceApi<T, P>(path: string): ResourceApi<T, P> {
  return {
    list: (query, signal) =>
      request(`${path}?${queryString(query)}`, { signal }),
    get: (id, signal) => request(`${path}/${id}`, { signal }),
    create: (input) =>
      request(path, { method: "POST", body: JSON.stringify(input) }),
    update: (id, input) =>
      request(`${path}/${id}`, { method: "PUT", body: JSON.stringify(input) }),
    remove: (id) => request(`${path}/${id}`, { method: "DELETE" }),
  };
}

export const studentsApi = resourceApi<Student, StudentInput>("/students");
export const coursesApi = resourceApi<Course, CourseInput>("/courses");
export const enrollmentsApi = {
  list: (query: EnrollmentListQuery, signal?: AbortSignal) =>
    request<PageResponse<Enrollment>>(`/enrollments?${queryString(query)}`, {
      signal,
    }),
  get: (id: number, signal?: AbortSignal) =>
    request<Enrollment>(`/enrollments/${id}`, { signal }),
  byStudent: (id: number, query: EnrollmentPageQuery, signal?: AbortSignal) => {
    const { page, size, sortBy, direction } = query;
    return request<PageResponse<Enrollment>>(
      `/students/${id}/enrollments?${queryString({ page, size, sortBy, direction })}`,
      { signal },
    );
  },
  create: (input: EnrollmentInput) =>
    request<Enrollment>("/enrollments", {
      method: "POST",
      body: JSON.stringify(input),
    }),
  remove: (id: number) =>
    request<void>(`/enrollments/${id}`, { method: "DELETE" }),
};

// The API caps page size at 100. Stable id ordering reaches records beyond page one.
export async function collectAll<T>(
  list: ResourceApi<T, unknown>["list"],
  signal?: AbortSignal,
): Promise<T[]> {
  const items: T[] = [];
  let page = 0;
  let totalPages: number;
  do {
    signal?.throwIfAborted();
    const response = await list(
      { page, size: 100, sortBy: "id", direction: "asc" },
      signal,
    );
    items.push(...response.content);
    totalPages = response.totalPages;
    page += 1;
  } while (page < totalPages);
  return items;
}
