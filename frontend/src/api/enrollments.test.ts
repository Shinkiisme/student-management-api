// @vitest-environment jsdom
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { enrollmentsApi } from "./resources";
import { session } from "../auth/session";
import { authTokens } from "../test/authFixtures";
import type { EnrollmentListQuery } from "../types/api";

const query: EnrollmentListQuery = {
  page: 2,
  size: 25,
  sortBy: "studentId",
  direction: "desc",
  studentId: 8,
};

beforeEach(() => {
  session.clear();
  session.accept(authTokens(), session.getSnapshot().generation);
});
afterEach(() => {
  session.clear();
  vi.unstubAllGlobals();
});

describe("paginated enrollment API", () => {
  it("sends page, size, sorting and student filter and returns metadata unchanged", async () => {
    const response = {
      content: [{ id: 14, studentId: 8, courseId: 2 }],
      page: 2,
      size: 25,
      totalElements: 64,
      totalPages: 3,
    };
    const fetch = vi.fn().mockResolvedValue(Response.json(response));
    vi.stubGlobal("fetch", fetch);
    expect(await enrollmentsApi.list(query)).toEqual(response);
    const url = new URL(fetch.mock.calls[0][0], "http://localhost");
    expect(url.pathname).toBe("/api/enrollments");
    expect(Object.fromEntries(url.searchParams)).toEqual({
      page: "2",
      size: "25",
      sortBy: "studentId",
      direction: "desc",
      studentId: "8",
    });
    expect(fetch.mock.calls[0][1].headers.get("Authorization")).toBe(
      `Bearer ${session.getAccessToken()}`,
    );
  });

  it("does not send an unspecified student filter", async () => {
    const fetch = vi
      .fn()
      .mockResolvedValue(
        Response.json({
          content: [],
          page: 0,
          size: 10,
          totalElements: 0,
          totalPages: 0,
        }),
      );
    vi.stubGlobal("fetch", fetch);
    await enrollmentsApi.list({
      page: 0,
      size: 10,
      sortBy: "courseId",
      direction: "asc",
    });
    expect(
      new URL(fetch.mock.calls[0][0], "http://localhost").searchParams.has(
        "studentId",
      ),
    ).toBe(false);
  });

  it("puts the student identity in the path only and propagates the AbortSignal", async () => {
    const controller = new AbortController();
    const fetch = vi
      .fn()
      .mockResolvedValue(
        Response.json({
          content: [],
          page: 2,
          size: 25,
          totalElements: 0,
          totalPages: 0,
        }),
      );
    vi.stubGlobal("fetch", fetch);
    await enrollmentsApi.byStudent(8, query, controller.signal);
    const url = new URL(fetch.mock.calls[0][0], "http://localhost");
    expect(url.pathname).toBe("/api/students/8/enrollments");
    expect(Object.fromEntries(url.searchParams)).toEqual({
      page: "2",
      size: "25",
      sortBy: "studentId",
      direction: "desc",
    });
    expect(fetch.mock.calls[0][1].signal).toBe(controller.signal);
  });

  it("preserves the list signal and the single-record/create/delete contracts", async () => {
    const enrollment = { id: 1, studentId: 2, courseId: 3 };
    const controller = new AbortController();
    const fetch = vi
      .fn()
      .mockResolvedValueOnce(
        Response.json({
          content: [],
          page: 0,
          size: 10,
          totalElements: 0,
          totalPages: 0,
        }),
      )
      .mockResolvedValueOnce(Response.json(enrollment))
      .mockResolvedValueOnce(Response.json(enrollment, { status: 201 }))
      .mockResolvedValueOnce(new Response(null, { status: 204 }));
    vi.stubGlobal("fetch", fetch);
    await enrollmentsApi.list(
      { page: 0, size: 10, sortBy: "id", direction: "asc" },
      controller.signal,
    );
    expect(fetch.mock.calls[0][1].signal).toBe(controller.signal);
    expect(await enrollmentsApi.get(1, controller.signal)).toEqual(enrollment);
    expect(await enrollmentsApi.create({ studentId: 2, courseId: 3 })).toEqual(
      enrollment,
    );
    expect(JSON.parse(fetch.mock.calls[2][1].body)).toEqual({
      studentId: 2,
      courseId: 3,
    });
    expect(fetch.mock.calls[2][1].method).toBe("POST");
    expect(await enrollmentsApi.remove(1)).toBeUndefined();
    expect(fetch.mock.calls[3][0]).toBe("/api/enrollments/1");
    expect(fetch.mock.calls[3][1].method).toBe("DELETE");
  });
});
