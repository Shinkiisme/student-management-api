export type FormValues = Record<string, string>;
export interface FieldDefinition {
  key: string;
  label: string;
  type: "text" | "number";
  maxLength?: number;
  min?: number;
  placeholder?: string;
}
export const MAX_INTEGER = 2147483647;

export function integerValue(value: string, min: number): number | null {
  if (!/^\d+$/.test(value.trim())) return null;
  const number = Number(value);
  return Number.isInteger(number) && number >= min && number <= MAX_INTEGER
    ? number
    : null;
}

export function validateFields(
  fields: FieldDefinition[],
  values: FormValues,
): Record<string, string> {
  const errors: Record<string, string> = {};
  for (const field of fields) {
    const value = (values[field.key] ?? "").trim();
    if (!value) errors[field.key] = `${field.label} is required.`;
    else if (
      field.type === "number" &&
      integerValue(value, field.min ?? 0) === null
    ) {
      errors[field.key] =
        `${field.label} must be a whole number from ${field.min ?? 0} to ${MAX_INTEGER}.`;
    } else if (field.maxLength && value.length > field.maxLength) {
      errors[field.key] =
        `${field.label} must not exceed ${field.maxLength} characters.`;
    }
  }
  return errors;
}

export function initials(name: string): string {
  return name
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((part) => part[0])
    .join("")
    .toUpperCase();
}
