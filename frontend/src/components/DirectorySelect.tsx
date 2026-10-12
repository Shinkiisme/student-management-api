import { useState } from "react";
import { Search } from "lucide-react";
import { Field } from "./Field";

export interface SelectOption {
  id: number;
  label: string;
}

export function DirectorySelect({
  label,
  value,
  onChange,
  options,
  placeholder,
  error,
  disabled = false,
  required = false,
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  options: SelectOption[];
  placeholder: string;
  error?: string;
  disabled?: boolean;
  required?: boolean;
}) {
  const [search, setSearch] = useState("");
  const searchTerm = search.trim().toLocaleLowerCase();
  const matched = options.filter((option) =>
    option.label.toLocaleLowerCase().includes(searchTerm),
  );
  const matchedIds = new Set(matched.map((option) => option.id));
  // Keep the current choice visible when narrowing the directory search.
  const displayed = options.filter(
    (option) => String(option.id) === value || matchedIds.has(option.id),
  );
  return (
    <Field
      label={label}
      error={error}
      hint={
        search && matched.length === 0
          ? "No matching options. Try a different search."
          : `${options.length.toLocaleString()} available. Search to narrow the options.`
      }
    >
      {(props) => (
        <div className="space-y-2">
          <div className="relative">
            <Search className="pointer-events-none absolute top-3 left-3 size-4 text-slate-400" />
            <input
              className="input pl-9"
              type="search"
              aria-label={`Search ${label.toLowerCase()}`}
              placeholder={`Search ${label.toLowerCase()}…`}
              value={search}
              disabled={disabled}
              onChange={(event) => setSearch(event.target.value)}
              onKeyDown={(event) => {
                // Searching must never implicitly submit a surrounding registration form.
                if (event.key === "Enter") event.preventDefault();
              }}
            />
          </div>
          <select
            {...props}
            className="input"
            required={required}
            disabled={disabled}
            value={value}
            onChange={(event) => onChange(event.target.value)}
          >
            <option value="">{placeholder}</option>
            {displayed.map((option) => (
              <option key={option.id} value={option.id}>
                {option.label}
              </option>
            ))}
          </select>
        </div>
      )}
    </Field>
  );
}
