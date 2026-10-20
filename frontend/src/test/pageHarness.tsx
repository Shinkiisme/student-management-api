import { act } from "react";
import { createRoot } from "react-dom/client";
import { MemoryRouter } from "react-router";
import App from "../App";
import type { PageResponse } from "../types/api";

export const students = [
  { id: 1, studentCode: "S1", name: "Student One", age: 20 },
  { id: 2, studentCode: "S2", name: "Student Two", age: 21 },
];
export const courses = [
  { id: 1, code: "C1", name: "Algorithms", credits: 4 },
  { id: 2, code: "C2", name: "Databases", credits: 3 },
];

export function paginated<T>(
  content: T[],
  page = 0,
  size = 10,
  totalElements = content.length,
): PageResponse<T> {
  return {
    content,
    page,
    size,
    totalElements,
    totalPages: Math.ceil(totalElements / size),
  };
}

export function directoryResponse(url: URL): Response | undefined {
  if (url.pathname === "/api/students")
    return Response.json(
      paginated(students, 0, Number(url.searchParams.get("size") ?? 10)),
    );
  if (url.pathname === "/api/courses")
    return Response.json(
      paginated(courses, 0, Number(url.searchParams.get("size") ?? 10)),
    );
  const student = students.find(
    (item) => url.pathname === `/api/students/${item.id}`,
  );
  if (student) return Response.json(student);
  return undefined;
}

export async function renderApp(path: string) {
  const container = document.createElement("div");
  document.body.append(container);
  const root = createRoot(container);
  await act(async () => {
    root.render(
      <MemoryRouter initialEntries={[path]}>
        <App />
      </MemoryRouter>,
    );
  });
  return {
    container,
    async unmount() {
      await act(async () => root.unmount());
      container.remove();
    },
  };
}

export async function click(container: HTMLElement, selector: string) {
  const element = container.querySelector<HTMLButtonElement>(selector);
  if (!element) throw new Error(`Missing test control: ${selector}`);
  await act(async () => element.click());
}

export async function select(
  container: HTMLElement,
  label: string,
  value: string,
) {
  const fieldLabel = Array.from(container.querySelectorAll("label")).find(
    (item) => item.textContent?.trim().startsWith(label),
  );
  const control = fieldLabel?.htmlFor
    ? document.getElementById(fieldLabel.htmlFor)
    : fieldLabel?.querySelector("select");
  if (!(control instanceof HTMLSelectElement))
    throw new Error(`Missing test select: ${label}`);
  await act(async () => {
    control.value = value;
    control.dispatchEvent(new Event("change", { bubbles: true }));
  });
}
