// @vitest-environment jsdom
import { act, StrictMode } from "react";
import { createRoot } from "react-dom/client";
import type { Root } from "react-dom/client";
import { MemoryRouter } from "react-router";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import App from "../App";
import { session, REFRESH_TOKEN_KEY } from "./session";
import { authTokens, deferred } from "../test/authFixtures";

let container: HTMLDivElement;
let root: Root;
const page = <T,>(content: T[]) => ({
  content,
  page: 0,
  size: 10,
  totalElements: content.length,
  totalPages: content.length ? 1 : 0,
});
const course = { id: 1, code: "TEST101", name: "Test course", credits: 4 };

beforeEach(() => {
  session.clear();
  session.markRestoring(session.getSnapshot().generation);
  sessionStorage.clear();
  vi.stubGlobal("IS_REACT_ACT_ENVIRONMENT", true);
  container = document.createElement("div");
  document.body.append(container);
  root = createRoot(container);
  vi.stubGlobal(
    "fetch",
    vi.fn().mockImplementation((url: string) => {
      if (url === "/api/courses/1")
        return Promise.resolve(Response.json(course));
      if (url.startsWith("/api/courses?"))
        return Promise.resolve(Response.json(page([course])));
      if (url.startsWith("/api/students?"))
        return Promise.resolve(Response.json(page([])));
      if (url.startsWith("/api/enrollments?"))
        return Promise.resolve(Response.json(page([])));
      return Promise.reject(new Error("Unexpected test API request"));
    }),
  );
});

afterEach(async () => {
  await act(async () => root.unmount());
  container.remove();
  session.clear();
  sessionStorage.clear();
  vi.unstubAllGlobals();
  vi.useRealTimers();
});

async function render(path: string) {
  await act(async () => {
    root.render(
      <StrictMode>
        <MemoryRouter initialEntries={[path]}>
          <App />
        </MemoryRouter>
      </StrictMode>,
    );
  });
}

describe("authentication routing and presentation", () => {
  it("redirects anonymous protected visits to Login before making protected requests", async () => {
    await render("/students");
    expect(container.querySelector("h1")?.textContent).toBe("Welcome back");
    expect(
      container.querySelector("input[autocomplete=username]"),
    ).not.toBeNull();
    expect(fetch).not.toHaveBeenCalled();
  });

  it("allows ADMIN into the administrative dashboard", async () => {
    session.accept(authTokens("ADMIN"), session.getSnapshot().generation);
    await render("/");
    expect(container.querySelector("h1")?.textContent).toBe(
      "Your campus, at a glance",
    );
    expect(container.querySelector("nav")?.textContent).toContain("Students");
    expect(container.querySelector("nav")?.textContent).toContain(
      "Enrollments",
    );
    expect(container.textContent).toContain("0 Enrollments");
  });

  it.each(["/", "/students", "/students/71", "/enrollments"])(
    "denies STUDENT access to administrative route %s",
    async (path) => {
      session.accept(authTokens("STUDENT"), session.getSnapshot().generation);
      await render(path);
      expect(container.querySelector("h1")?.textContent).toBe("Access denied");
      expect(container.querySelector("nav")?.textContent).not.toContain(
        "Students",
      );
      expect(container.querySelector("nav")?.textContent).not.toContain(
        "Dashboard",
      );
      expect(fetch).not.toHaveBeenCalled();
    },
  );

  it("lands STUDENT users on the permitted read-only course directory", async () => {
    session.accept(authTokens("STUDENT"), session.getSnapshot().generation);
    await render("/login");
    expect(container.querySelector("h1")?.textContent).toBe("Courses");
    const buttons = Array.from(container.querySelectorAll("button")).map(
      (button) => button.getAttribute("aria-label") || button.textContent,
    );
    expect(
      buttons.some(
        (label) =>
          label?.includes("Add course") ||
          label?.startsWith("Edit ") ||
          label?.startsWith("Delete "),
      ),
    ).toBe(false);
    expect(container.querySelector('a[href="/courses/1"]')).not.toBeNull();
    expect(
      vi
        .mocked(fetch)
        .mock.calls.every(([url]) => String(url).startsWith("/api/courses?")),
    ).toBe(true);
  });

  it("shows STUDENT course details without administrative actions or student identity requests", async () => {
    session.accept(authTokens("STUDENT"), session.getSnapshot().generation);
    await render("/courses/1");
    expect(container.querySelector("h1")?.textContent).toBe("Test course");
    expect(container.textContent).not.toContain("Edit course");
    expect(container.textContent).not.toContain("Delete");
    expect(
      vi.mocked(fetch).mock.calls.every(([url]) => url === "/api/courses/1"),
    ).toBe(true);
  });

  it("preserves administrative course actions for ADMIN", async () => {
    session.accept(authTokens("ADMIN"), session.getSnapshot().generation);
    await render("/courses");
    expect(container.textContent).toContain("Add course");
    expect(
      container.querySelector('button[aria-label="Edit Test course"]'),
    ).not.toBeNull();
    expect(
      container.querySelector('button[aria-label="Delete Test course"]'),
    ).not.toBeNull();
  });

  it("does not mount protected content while restoration is pending or after it fails", async () => {
    const reply = deferred<Response>();
    const fetch = vi.fn().mockReturnValue(reply.promise);
    vi.stubGlobal("fetch", fetch);
    sessionStorage.setItem(REFRESH_TOKEN_KEY, "test-only-restore");
    await render("/courses");
    expect(container.textContent).toContain("Restoring your session");
    expect(container.querySelector("table")).toBeNull();
    expect(fetch).toHaveBeenCalledTimes(1);
    await act(async () => {
      reply.resolve(Response.json({}, { status: 401 }));
    });
    expect(container.querySelector("h1")?.textContent).toBe("Welcome back");
    expect(sessionStorage.getItem(REFRESH_TOKEN_KEY)).toBeNull();
  });

  it("hides protected pages at token expiry, then signs out after failed refresh", async () => {
    vi.useFakeTimers();
    session.accept(
      authTokens("STUDENT", "short", 1),
      session.getSnapshot().generation,
    );
    await render("/courses");
    const reply = deferred<Response>();
    vi.stubGlobal("fetch", vi.fn().mockReturnValue(reply.promise));
    await act(async () => {
      await vi.advanceTimersByTimeAsync(1001);
    });
    expect(container.querySelector("table")).toBeNull();
    expect(container.textContent).toContain("Restoring your session");
    await act(async () => {
      reply.resolve(Response.json({}, { status: 401 }));
    });
    expect(container.querySelector("h1")?.textContent).toBe("Welcome back");
  });

  it("handles a course 403 as a permission error without logging out", async () => {
    session.accept(authTokens("STUDENT"), session.getSnapshot().generation);
    vi.stubGlobal(
      "fetch",
      vi
        .fn()
        .mockResolvedValue(
          Response.json(
            { detail: "You do not have permission to access this resource" },
            { status: 403 },
          ),
        ),
    );
    await render("/courses");
    expect(container.querySelector("[role=alert]")?.textContent).toContain(
      "permission",
    );
    expect(session.getSnapshot().status).toBe("authenticated");
    expect(container.querySelector('a[href="/login"]')).toBeNull();
  });

  it("validates empty login fields and toggles password visibility without a request", async () => {
    await render("/login");
    const form = container.querySelector("form")!;
    await act(async () => {
      form.dispatchEvent(
        new Event("submit", { bubbles: true, cancelable: true }),
      );
    });
    expect(container.querySelectorAll('[aria-invalid="true"]')).toHaveLength(2);
    const toggle = container.querySelector(
      'button[aria-label="Show password"]',
    ) as HTMLButtonElement;
    await act(async () => toggle.click());
    expect(
      container.querySelector('input[name="password"]')?.getAttribute("type"),
    ).toBe("text");
    expect(fetch).not.toHaveBeenCalled();
  });
});
