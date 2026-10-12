import { afterEach, describe, expect, it, vi } from "vitest";
import { ApiError, request } from "./http";

afterEach(() => {
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
});

describe("HTTP client", () => {
  it("supports a configured API base and removes trailing slashes", async () => {
    vi.stubEnv("VITE_API_BASE_URL", "https://api.example.test/backend/");
    vi.resetModules();
    const { request: configuredRequest } = await import("./http");
    const fetch = vi.fn().mockResolvedValue(Response.json([]));
    vi.stubGlobal("fetch", fetch);
    await configuredRequest("/enrollments");
    expect(fetch).toHaveBeenCalledWith(
      "https://api.example.test/backend/enrollments",
      expect.any(Object),
    );
  });

  it("sends the exact JSON payload through /api and accepts real DTO shapes", async () => {
    const student = {
      id: 1,
      studentCode: "STU1",
      name: "Test student",
      age: 0,
    };
    const fetch = vi
      .fn()
      .mockResolvedValue(Response.json(student, { status: 201 }));
    vi.stubGlobal("fetch", fetch);
    expect(
      await request("/students", {
        method: "POST",
        body: JSON.stringify({
          studentCode: "STU1",
          name: "Test student",
          age: 0,
        }),
      }),
    ).toEqual(student);
    expect(fetch).toHaveBeenCalledWith(
      "/api/students",
      expect.objectContaining({
        method: "POST",
        headers: expect.objectContaining({
          "Content-Type": "application/json",
        }),
      }),
    );
  });

  it("accepts empty 204 delete responses without parsing JSON", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(new Response(null, { status: 204 })),
    );
    expect(await request("/students/1", { method: "DELETE" })).toBeUndefined();
  });

  it("preserves ProblemDetail messages and field validation errors", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        Response.json(
          {
            detail: "Request validation failed",
            errors: { age: "Age cannot be negative" },
          },
          { status: 400 },
        ),
      ),
    );
    await expect(request("/students")).rejects.toMatchObject({
      status: 400,
      message: "Request validation failed",
      errors: { age: "Age cannot be negative" },
    });
  });

  it.each([400, 404, 409, 500, 502])(
    "handles non-JSON %s responses with a readable fallback",
    async (status) => {
      vi.stubGlobal(
        "fetch",
        vi
          .fn()
          .mockResolvedValue(
            new Response("<html>proxy error</html>", { status }),
          ),
      );
      await expect(request("/courses")).rejects.toBeInstanceOf(ApiError);
      await expect(request("/courses")).rejects.toMatchObject({ status });
    },
  );

  it("ignores malformed field-error values and retains duplicate conflict detail", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue(
        Response.json(
          {
            detail: "Course code CS101 already exists",
            errors: { code: "Duplicate code", name: 4 },
          },
          { status: 409 },
        ),
      ),
    );
    await expect(request("/courses")).rejects.toMatchObject({
      message: "Course code CS101 already exists",
      errors: { code: "Duplicate code" },
    });
  });

  it("reports network failures clearly", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockRejectedValue(new TypeError("Failed to fetch")),
    );
    await expect(request("/students")).rejects.toThrow(
      "Cannot reach the server",
    );
  });

  it("preserves aborted requests so obsolete reads do not become network errors", async () => {
    const controller = new AbortController();
    controller.abort();
    const error = new DOMException("Aborted", "AbortError");
    vi.stubGlobal("fetch", vi.fn().mockRejectedValue(error));
    await expect(
      request("/students", { signal: controller.signal }),
    ).rejects.toBe(error);
  });
});
