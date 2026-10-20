// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { session } from "../auth/session";
import { authTokens } from "../test/authFixtures";
import {
  click,
  directoryResponse,
  paginated,
  renderApp,
  select,
} from "../test/pageHarness";
import type { Enrollment } from "../types/api";

let view: Awaited<ReturnType<typeof renderApp>> | undefined;
let rows: Enrollment[];
let studentError: number | null;
let enrollmentError: number | null;

beforeEach(() => {
  session.clear();
  session.accept(authTokens(), session.getSnapshot().generation);
  vi.stubGlobal("IS_REACT_ACT_ENVIRONMENT", true);
  rows = Array.from({ length: 13 }, (_, index) => ({
    id: index + 1,
    studentId: 1,
    courseId: (index % 2) + 1,
  }));
  studentError = null;
  enrollmentError = null;
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((path: string) => {
      const url = new URL(path, "http://localhost");
      if (url.pathname === "/api/auth/refresh")
        return Promise.resolve(
          Response.json({ detail: "Invalid refresh token" }, { status: 401 }),
        );
      if (url.pathname === "/api/students/1" && studentError)
        return Promise.resolve(
          Response.json(
            { detail: "Student not found" },
            { status: studentError },
          ),
        );
      if (url.pathname === "/api/students/1/enrollments") {
        if (enrollmentError)
          return Promise.resolve(
            Response.json(
              {
                detail:
                  enrollmentError === 403
                    ? "Enrollment access forbidden"
                    : "Student not found",
              },
              { status: enrollmentError },
            ),
          );
        const page = Number(url.searchParams.get("page"));
        const size = Number(url.searchParams.get("size"));
        return Promise.resolve(
          Response.json(
            paginated(
              rows.slice(page * size, (page + 1) * size),
              page,
              size,
              rows.length,
            ),
          ),
        );
      }
      const directory = directoryResponse(url);
      if (directory) return Promise.resolve(directory);
      return Promise.reject(new Error("Unexpected test API request"));
    }),
  );
});
afterEach(async () => {
  await view?.unmount();
  view = undefined;
  session.clear();
  vi.unstubAllGlobals();
});
async function render() {
  view = await renderApp("/students/1");
  return view.container;
}
function queries() {
  return vi
    .mocked(fetch)
    .mock.calls.filter(([url]) =>
      String(url).startsWith("/api/students/1/enrollments?"),
    )
    .map(([url]) => new URL(String(url), "http://localhost").searchParams);
}

describe("student enrollment details", () => {
  it("returns to Login when an enrollment 401 cannot be refreshed", async () => {
    enrollmentError = 401;
    const container = await render();
    expect(container.querySelector("h1")?.textContent).toBe("Welcome back");
    expect(session.getSnapshot().status).toBe("anonymous");
  });

  it("recovers to page zero when a retried student page has no remaining records", async () => {
    const container = await render();
    enrollmentError = 500;
    await click(container, 'button[aria-label="Next page"]');
    rows = [];
    enrollmentError = null;
    await click(container, '[role="alert"] button');
    expect(container.textContent).toContain("Course enrollments (0)");
    expect(container.textContent).toContain("0 of 0");
    expect(
      queries()
        .slice(-2)
        .map((query) => query.get("page")),
    ).toEqual(["1", "0"]);
  });

  it("uses the selected Student ID path and paginates resolved course names with the global student total", async () => {
    const container = await render();
    expect(container.querySelector("h1")?.textContent).toBe("Student One");
    expect(container.textContent).toContain("Course enrollments (13)");
    expect(container.querySelectorAll("tbody tr")).toHaveLength(10);
    expect(container.textContent).toContain("Algorithms");
    expect(container.textContent).toContain("Databases");
    await click(container, 'button[aria-label="Next page"]');
    expect(container.querySelectorAll("tbody tr")).toHaveLength(3);
    expect(queries().at(-1)?.get("page")).toBe("1");
    expect(queries().every((params) => !params.has("studentId"))).toBe(true);
    expect(
      vi
        .mocked(fetch)
        .mock.calls.some(([url]) => String(url).includes("/students/71")),
    ).toBe(false);
    await click(container, 'button[aria-label="Previous page"]');
    expect(queries().at(-1)?.get("page")).toBe("0");
  });

  it("resets the page when sorting or changing page size", async () => {
    const container = await render();
    await click(container, 'button[aria-label="Next page"]');
    await click(container, "thead th:nth-child(3) button");
    expect(Object.fromEntries(queries().at(-1)!)).toMatchObject({
      page: "0",
      sortBy: "courseId",
      direction: "asc",
    });
    await click(container, 'button[aria-label="Next page"]');
    await select(container, "Rows per page", "25");
    expect(Object.fromEntries(queries().at(-1)!)).toMatchObject({
      page: "0",
      size: "25",
    });
    expect(container.querySelectorAll("tbody tr")).toHaveLength(13);
  });

  it("shows the empty state and accurate zero count", async () => {
    rows = [];
    const container = await render();
    expect(container.textContent).toContain("Course enrollments (0)");
    expect(container.textContent).toContain("No courses registered");
    expect(container.textContent).toContain("0 of 0");
  });

  it("shows a missing student without requesting their enrollments", async () => {
    studentError = 404;
    const container = await render();
    expect(container.querySelector("[role=alert]")?.textContent).toContain(
      "Student not found",
    );
    expect(queries()).toHaveLength(0);
  });

  it.each([403, 404])(
    "retains student details and handles an enrollment %s",
    async (status) => {
      enrollmentError = status;
      const container = await render();
      expect(container.querySelector("h1")?.textContent).toBe("Student One");
      expect(container.querySelector("[role=alert]")?.textContent).toContain(
        status === 403 ? "forbidden" : "not found",
      );
      expect(session.getSnapshot().status).toBe("authenticated");
      expect(container.querySelectorAll("tbody tr")).toHaveLength(0);
    },
  );
});
