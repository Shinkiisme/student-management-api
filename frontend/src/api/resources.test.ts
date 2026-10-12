import { describe, expect, it, vi } from "vitest";
import { collectAll, queryString } from "./resources";

describe("directory pagination", () => {
  it("collects selections from every page with the backend maximum page size", async () => {
    const list = vi
      .fn()
      .mockResolvedValueOnce({
        content: [{ id: 1 }],
        page: 0,
        size: 100,
        totalElements: 3,
        totalPages: 3,
      })
      .mockResolvedValueOnce({
        content: [{ id: 101 }],
        page: 1,
        size: 100,
        totalElements: 3,
        totalPages: 3,
      })
      .mockResolvedValueOnce({
        content: [{ id: 201 }],
        page: 2,
        size: 100,
        totalElements: 3,
        totalPages: 3,
      });
    const controller = new AbortController();
    expect(await collectAll(list, controller.signal)).toEqual([
      { id: 1 },
      { id: 101 },
      { id: 201 },
    ]);
    expect(list).toHaveBeenCalledTimes(3);
    expect(list).toHaveBeenNthCalledWith(
      3,
      { page: 2, size: 100, sortBy: "id", direction: "asc" },
      controller.signal,
    );
  });

  it("does not silently offer a partial directory when a later page fails", async () => {
    const list = vi
      .fn()
      .mockResolvedValueOnce({ content: [{ id: 1 }], totalPages: 2 })
      .mockRejectedValueOnce(new Error("Second page unavailable"));
    await expect(collectAll(list)).rejects.toThrow("Second page unavailable");
  });

  it("handles an empty directory with a single request", async () => {
    const list = vi.fn().mockResolvedValue({ content: [], totalPages: 0 });
    expect(await collectAll(list)).toEqual([]);
    expect(list).toHaveBeenCalledTimes(1);
  });

  it("stops between pages when its consumer unmounts", async () => {
    const controller = new AbortController();
    const list = vi.fn().mockImplementation(async () => {
      controller.abort();
      return { content: [{ id: 1 }], totalPages: 2 };
    });
    await expect(collectAll(list, controller.signal)).rejects.toMatchObject({
      name: "AbortError",
    });
    expect(list).toHaveBeenCalledTimes(1);
  });

  it("preserves age zero and omits unspecified filters", () => {
    const params = new URLSearchParams(
      queryString({
        page: 0,
        size: 10,
        sortBy: "studentCode",
        direction: "desc",
        age: 0,
        credits: undefined,
      }),
    );
    expect(params.get("age")).toBe("0");
    expect(params.get("sortBy")).toBe("studentCode");
    expect(params.has("credits")).toBe(false);
  });
});
