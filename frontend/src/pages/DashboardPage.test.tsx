// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { session } from "../auth/session";
import { authTokens } from "../test/authFixtures";
import {
  click,
  courses,
  paginated,
  renderApp,
  students,
} from "../test/pageHarness";

let view: Awaited<ReturnType<typeof renderApp>> | undefined;
let empty: boolean;
let failed: "students" | "courses" | "enrollments" | null;

beforeEach(() => {
  session.clear();
  session.accept(authTokens(), session.getSnapshot().generation);
  vi.stubGlobal("IS_REACT_ACT_ENVIRONMENT", true);
  empty = false;
  failed = null;
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((path: string) => {
      const url = new URL(path, "http://localhost");
      const resource = url.pathname.split("/").at(-1);
      if (resource === failed)
        return Promise.resolve(
          Response.json({ detail: `${failed} unavailable` }, { status: 500 }),
        );
      const size = Number(url.searchParams.get("size"));
      if (resource === "students")
        return Promise.resolve(
          Response.json(
            paginated(empty ? [] : students, 0, size, empty ? 0 : 145),
          ),
        );
      if (resource === "courses")
        return Promise.resolve(
          Response.json(
            paginated(empty ? [] : courses, 0, size, empty ? 0 : 37),
          ),
        );
      if (resource === "enrollments")
        return Promise.resolve(
          Response.json(
            paginated(
              empty ? [] : [{ id: 1, studentId: 1, courseId: 1 }],
              0,
              size,
              empty ? 0 : 912,
            ),
          ),
        );
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
  view = await renderApp("/");
  return view.container;
}
function stat(container: HTMLElement, label: string) {
  const card = Array.from(container.querySelectorAll("section")).find(
    (section) => section.querySelector("p")?.textContent === label,
  );
  if (!card) throw new Error(`Missing statistic: ${label}`);
  return card;
}

describe("dashboard metadata and independent panels", () => {
  it("uses global metadata totals even when enrollment content contains one record", async () => {
    const container = await render();
    expect(stat(container, "Students").textContent).toContain("145 Students");
    expect(stat(container, "Courses").textContent).toContain("37 Courses");
    expect(stat(container, "Enrollments").textContent).toContain(
      "912 Enrollments",
    );
    const enrollmentCalls = vi
      .mocked(fetch)
      .mock.calls.filter(([url]) =>
        String(url).startsWith("/api/enrollments?"),
      );
    expect(enrollmentCalls).toHaveLength(1);
    const params = new URL(String(enrollmentCalls[0][0]), "http://localhost")
      .searchParams;
    expect(Object.fromEntries(params)).toEqual({
      page: "0",
      size: "1",
      sortBy: "id",
      direction: "asc",
    });
    expect(
      container.querySelector('[aria-label="Algorithms: 4 credits"]'),
    ).not.toBeNull();
    expect(
      container.querySelector('[aria-label="Databases: 3 credits"]'),
    ).not.toBeNull();
    expect(container.textContent).not.toContain("Enrollment counts for");
  });

  it("shows genuine zero totals and empty previews", async () => {
    empty = true;
    const container = await render();
    expect(stat(container, "Students").textContent).toContain("0 Students");
    expect(stat(container, "Courses").textContent).toContain("0 Courses");
    expect(stat(container, "Enrollments").textContent).toContain(
      "0 Enrollments",
    );
    expect(container.textContent).toContain("No courses available");
    expect(container.textContent).toContain("Your directory starts here");
  });

  it.each(["students", "courses", "enrollments"] as const)(
    "keeps independent totals usable when %s fails and supports retry",
    async (resource) => {
      failed = resource;
      const container = await render();
      const label = resource[0].toUpperCase() + resource.slice(1);
      const card = stat(container, label);
      expect(card.textContent).toContain(`${label} unavailable`);
      expect(card.textContent).not.toContain(`0 ${label}`);
      const unaffected = resource === "students" ? "Courses" : "Students";
      expect(
        stat(container, unaffected).querySelector("[role=alert]"),
      ).toBeNull();
      if (resource === "enrollments")
        expect(
          container.querySelector('[aria-label="Algorithms: 4 credits"]'),
        ).not.toBeNull();
      failed = null;
      await click(card, '[role="alert"] button');
      expect(stat(container, label).textContent).not.toContain(
        `${label} unavailable`,
      );
    },
  );
});
