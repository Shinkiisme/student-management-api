import { ChevronLeft, ChevronRight } from "lucide-react";

export function Pagination({
  page,
  size,
  totalElements,
  totalPages,
  onPage,
  onSize,
}: {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  onPage: (page: number) => void;
  onSize: (size: number) => void;
}) {
  const from = totalElements === 0 ? 0 : page * size + 1;
  const to = Math.min((page + 1) * size, totalElements);
  return (
    <div className="flex flex-wrap items-center justify-between gap-4 border-t border-slate-100 px-5 py-4 text-xs text-slate-500">
      <p>
        Showing{" "}
        <span className="font-semibold text-slate-700">
          {from}–{to}
        </span>{" "}
        of{" "}
        <span className="font-semibold text-slate-700">
          {totalElements.toLocaleString()}
        </span>{" "}
        records
      </p>
      <div className="flex items-center gap-4">
        <label className="flex items-center gap-2">
          Rows per page
          <select
            className="rounded-lg border border-slate-200 bg-white p-1.5 text-slate-700"
            value={size}
            onChange={(event) => onSize(Number(event.target.value))}
          >
            {[5, 10, 25, 50, 100].map((value) => (
              <option key={value} value={value}>
                {value}
              </option>
            ))}
          </select>
        </label>
        <div className="flex items-center gap-2">
          <button
            className="icon-button"
            aria-label="Previous page"
            disabled={page === 0}
            onClick={() => onPage(page - 1)}
          >
            <ChevronLeft className="size-4" />
          </button>
          <span className="min-w-16 text-center">
            {totalPages === 0 ? "0 of 0" : `${page + 1} of ${totalPages}`}
          </span>
          <button
            className="icon-button"
            aria-label="Next page"
            disabled={page + 1 >= totalPages}
            onClick={() => onPage(page + 1)}
          >
            <ChevronRight className="size-4" />
          </button>
        </div>
      </div>
    </div>
  );
}
