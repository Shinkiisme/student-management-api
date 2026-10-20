// @vitest-environment jsdom
import { act } from "react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { session } from "../auth/session";
import { authTokens, deferred } from "../test/authFixtures";
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
let listError: number | null;

beforeEach(() => {
  // jsdom has no native dialog methods. Browser dialogs themselves are unchanged.
  Object.defineProperties(HTMLDialogElement.prototype, {
    showModal: {
      configurable: true,
      value: function (this: HTMLDialogElement) {
        this.open = true;
      },
    },
    close: {
      configurable: true,
      value: function (this: HTMLDialogElement) {
        this.open = false;
      },
    },
  });
  session.clear();
  session.accept(authTokens(), session.getSnapshot().generation);
  vi.stubGlobal("IS_REACT_ACT_ENVIRONMENT", true);
  rows = Array.from({ length: 21 }, (_, index) => ({
    id: index + 1,
    studentId: 1,
    courseId: 1,
  }));
  listError = null;
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((path: string, options: RequestInit) => {
      const url = new URL(path, "http://localhost");
      if (url.pathname === "/api/auth/refresh")
        return Promise.resolve(
          Response.json({ detail: "Invalid refresh token" }, { status: 401 }),
        );
      const directory = directoryResponse(url);
      if (directory) return Promise.resolve(directory);
      if (url.pathname === "/api/enrollments" && options.method === "POST") {
        const input = JSON.parse(String(options.body)) as {
          studentId: number;
          courseId: number;
        };
        const created = {
          id: Math.max(0, ...rows.map((item) => item.id)) + 1,
          ...input,
        };
        rows.push(created);
        return Promise.resolve(Response.json(created, { status: 201 }));
      }
      if (
        url.pathname.startsWith("/api/enrollments/") &&
        options.method === "DELETE"
      ) {
        const id = Number(url.pathname.split("/").at(-1));
        rows = rows.filter((item) => item.id !== id);
        return Promise.resolve(new Response(null, { status: 204 }));
      }
      if (url.pathname === "/api/enrollments") {
        if (listError)
          return Promise.resolve(
            Response.json(
              { detail: "Enrollment request failed" },
              { status: listError },
            ),
          );
        const page = Number(url.searchParams.get("page"));
        const size = Number(url.searchParams.get("size"));
        const filter = url.searchParams.get("studentId");
        const filtered = filter
          ? rows.filter((item) => item.studentId === Number(filter))
          : rows;
        return Promise.resolve(
          Response.json(
            paginated(
              filtered.slice(page * size, (page + 1) * size),
              page,
              size,
              filtered.length,
            ),
          ),
        );
      }
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

async function render(path = "/enrollments") {
  view = await renderApp(path);
  return view.container;
}
function queries() {
  return vi
    .mocked(fetch)
    .mock.calls.filter(([url]) => String(url).startsWith("/api/enrollments?"))
    .map(([url]) => new URL(String(url), "http://localhost").searchParams);
}

describe("enrollment pagination", () => {
  it("recovers to page zero when cancellation leaves no enrollments at all", async () => {
    rows = rows.slice(0, 11);
    const container = await render();
    await click(container, 'button[aria-label="Next page"]');
    // Other rows disappear concurrently; only the visible enrollment remains to cancel.
    rows = [rows[10]];
    await click(container, 'button[aria-label="Cancel enrollment 11"]');
    await click(container, "dialog .button-danger");
    expect(container.textContent).toContain("No enrollments yet");
    expect(container.textContent).toContain("0 of 0");
    expect(
      queries()
        .slice(-2)
        .map((query) => query.get("page")),
    ).toEqual(["1", "0"]);
  });

  it("normalizes a positive URL filter with leading zeros for selection and requests", async () => {
    const container = await render("/enrollments?studentId=01");
    expect(queries().at(-1)?.get("studentId")).toBe("1");
    expect(container.querySelector<HTMLSelectElement>("select")?.value).toBe(
      "1",
    );
    expect(container.querySelectorAll('select option[value="1"]')).toHaveLength(
      1,
    );
  });

  it.each([401, 403])(
    "handles a list %s through the existing authentication behavior",
    async (status) => {
      listError = status;
      const container = await render();
      if (status === 401) {
        expect(container.querySelector("h1")?.textContent).toBe("Welcome back");
        expect(session.getSnapshot().status).toBe("anonymous");
      } else {
        expect(container.querySelector("[role=alert]")?.textContent).toContain(
          "Enrollment request failed",
        );
        expect(session.getSnapshot().status).toBe("authenticated");
      }
    },
  );

  it("shows the first server page and global total with readable labels", async () => {
    const container = await render();
    expect(container.querySelectorAll("tbody tr")).toHaveLength(10);
    expect(container.textContent).toContain("of 21 records");
    expect(container.textContent).toContain("Student One");
    expect(container.textContent).toContain("Algorithms");
    expect(Object.fromEntries(queries()[0])).toEqual({
      page: "0",
      size: "10",
      sortBy: "id",
      direction: "asc",
    });
  });

  it("requests next/previous pages and resets the page when size changes", async () => {
    const container = await render();
    await click(container, 'button[aria-label="Next page"]');
    expect(container.querySelector("tbody td")?.textContent).toBe("#11");
    expect(queries().at(-1)?.get("page")).toBe("1");
    await click(container, 'button[aria-label="Previous page"]');
    expect(container.querySelector("tbody td")?.textContent).toBe("#1");
    await click(container, 'button[aria-label="Next page"]');
    await select(container, "Rows per page", "25");
    expect(queries().at(-1)?.get("page")).toBe("0");
    expect(queries().at(-1)?.get("size")).toBe("25");
    expect(container.querySelectorAll("tbody tr")).toHaveLength(21);
  });

  it("filters on the list endpoint and resets pagination when changing or clearing the student", async () => {
    rows[0].studentId = 2;
    const container = await render();
    await click(container, 'button[aria-label="Next page"]');
    await select(container, "Filter by student", "2");
    expect(queries().at(-1)?.get("studentId")).toBe("2");
    expect(queries().at(-1)?.get("page")).toBe("0");
    expect(container.querySelectorAll("tbody tr")).toHaveLength(1);
    expect(container.textContent).toContain("Student Two");
    await select(container, "Filter by student", "");
    expect(queries().at(-1)?.has("studentId")).toBe(false);
    expect(queries().at(-1)?.get("page")).toBe("0");
    expect(
      vi
        .mocked(fetch)
        .mock.calls.some(
          ([url]) =>
            String(url).includes("/enrollments") &&
            String(url).startsWith("/api/students/"),
        ),
    ).toBe(false);
  });

  it.each(["0", "-1", "bad", "2147483648"])(
    "rejects invalid student filter %s before requesting enrollments",
    async (value) => {
      const container = await render(`/enrollments?studentId=${value}`);
      expect(container.querySelector("[role=alert]")?.textContent).toContain(
        "positive integer",
      );
      expect(queries()).toHaveLength(0);
    },
  );

  it("resets pagination for supported server sort fields and toggles direction", async () => {
    const container = await render();
    await click(container, 'button[aria-label="Next page"]');
    await click(container, "thead th:nth-child(2) button");
    expect(Object.fromEntries(queries().at(-1)!)).toMatchObject({
      page: "0",
      sortBy: "studentId",
      direction: "asc",
    });
    await click(container, "thead th:nth-child(2) button");
    expect(queries().at(-1)?.get("direction")).toBe("desc");
    await click(container, "thead th:nth-child(3) button");
    expect(queries().at(-1)?.get("sortBy")).toBe("courseId");
    await click(container, "thead th:nth-child(1) button");
    expect(queries().at(-1)?.get("sortBy")).toBe("id");
  });

  it("shows empty filtered/unfiltered states without fabricating rows", async () => {
    rows = [];
    const container = await render();
    expect(container.textContent).toContain("No enrollments yet");
    expect(container.textContent).toContain("0 of 0");
    expect(container.querySelectorAll("tbody tr")).toHaveLength(0);
    await select(container, "Filter by student", "1");
    expect(container.textContent).toContain("No enrollments for this student");
  });

  it("recovers to the prior valid page after deleting its last enrollment", async () => {
    rows = rows.slice(0, 11);
    const container = await render();
    await click(container, 'button[aria-label="Next page"]');
    await click(container, 'button[aria-label="Cancel enrollment 11"]');
    await click(container, "dialog .button-danger");
    expect(container.textContent).toContain(
      "Enrollment canceled successfully.",
    );
    expect(container.textContent).toContain("1 of 1");
    expect(container.querySelectorAll("tbody tr")).toHaveLength(10);
    expect(
      queries()
        .slice(-2)
        .map((query) => query.get("page")),
    ).toEqual(["1", "0"]);
  });

  it("refreshes the current page after registration", async () => {
    rows = rows.slice(0, 11);
    const container = await render();
    await click(container, 'button[aria-label="Next page"]');
    await click(container, "main .button-primary");
    await select(container, "Student", "1");
    await select(container, "Course", "2");
    await click(container, 'dialog button[type="submit"]');
    expect(container.textContent).toContain(
      "Enrollment registered successfully.",
    );
    expect(queries().at(-1)?.get("page")).toBe("1");
    expect(container.querySelectorAll("tbody tr")).toHaveLength(2);
    expect(container.textContent).toContain("of 12 records");
  });

  it("shows API errors and retries the same query", async () => {
    listError = 500;
    const container = await render();
    expect(container.querySelector("[role=alert]")?.textContent).toContain(
      "Enrollment request failed",
    );
    listError = null;
    await click(container, '[role="alert"] button');
    expect(container.querySelectorAll("tbody tr")).toHaveLength(10);
    expect(queries().at(-1)?.get("page")).toBe("0");
  });

  it("aborts a superseded page request and keeps newer filter results", async () => {
    const original = vi.mocked(fetch).getMockImplementation()!;
    const slow = deferred<Response>();
    let oldSignal: AbortSignal | null | undefined;
    vi.mocked(fetch).mockImplementation((path, options) => {
      const url = new URL(String(path), "http://localhost");
      if (
        url.pathname === "/api/enrollments" &&
        url.searchParams.get("page") === "1"
      ) {
        oldSignal = options?.signal;
        return slow.promise;
      }
      return original(path, options);
    });
    const container = await render();
    await click(container, 'button[aria-label="Next page"]');
    await select(container, "Filter by student", "2");
    expect(oldSignal?.aborted).toBe(true);
    await act(async () =>
      slow.resolve(
        Response.json(
          paginated([{ id: 999, studentId: 1, courseId: 1 }], 1, 10, 21),
        ),
      ),
    );
    expect(container.textContent).toContain("No enrollments for this student");
    expect(container.textContent).not.toContain("#999");
  });
});
