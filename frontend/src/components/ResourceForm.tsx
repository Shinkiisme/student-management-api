import { useRef, useState } from "react";
import type { FormEvent } from "react";
import { LoaderCircle } from "lucide-react";
import { ApiError } from "../api/http";
import type { ResourceConfig } from "../utils/resourceConfig";
import { MAX_INTEGER, validateFields } from "../utils/validation";
import { Modal } from "./Modal";
import { Field } from "./Field";
import { ErrorState } from "./ui";

export function ResourceForm<T extends { id: number; name: string }, P>({
  config,
  item,
  onSaved,
  onClose,
}: {
  config: ResourceConfig<T, P>;
  item?: T;
  onSaved: (item: T) => void;
  onClose: () => void;
}) {
  const [values, setValues] = useState(() => config.values(item));
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<unknown>(null);
  const [pending, setPending] = useState(false);
  const lock = useRef(false);
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (lock.current) return;
    const nextErrors = validateFields(config.fields, values);
    setErrors(nextErrors);
    setError(null);
    if (Object.keys(nextErrors).length) return;
    lock.current = true;
    setPending(true);
    try {
      const payload = config.input(values);
      const saved = item
        ? await config.api.update(item.id, payload)
        : await config.api.create(payload);
      onSaved(saved);
      onClose();
    } catch (error) {
      setError(error);
      if (error instanceof ApiError) setErrors(error.errors);
    } finally {
      lock.current = false;
      setPending(false);
    }
  }
  return (
    <Modal
      title={`${item ? "Edit" : "Add"} ${config.singular}`}
      description={`Enter the ${config.singular} information below. All fields are required.`}
      onClose={onClose}
      busy={pending}
    >
      <form noValidate onSubmit={(event) => void submit(event)}>
        <div className="space-y-5 p-6">
          {error !== null && <ErrorState error={error} />}
          {config.fields.map((field) => (
            <Field
              key={field.key}
              label={field.label}
              error={errors[field.key]}
            >
              {(props) => (
                <input
                  {...props}
                  className="input"
                  type={field.type}
                  maxLength={field.maxLength}
                  min={field.min}
                  max={field.type === "number" ? MAX_INTEGER : undefined}
                  step={field.type === "number" ? 1 : undefined}
                  required
                  disabled={pending}
                  value={values[field.key]}
                  placeholder={field.placeholder}
                  onChange={(event) => {
                    setValues({ ...values, [field.key]: event.target.value });
                    setErrors({ ...errors, [field.key]: "" });
                  }}
                />
              )}
            </Field>
          ))}
        </div>
        <div className="dialog-footer">
          <button
            className="button-secondary"
            type="button"
            disabled={pending}
            onClick={onClose}
          >
            Cancel
          </button>
          <button className="button-primary" type="submit" disabled={pending}>
            {pending && <LoaderCircle className="size-4 animate-spin" />}
            {pending
              ? "Saving…"
              : item
                ? "Save changes"
                : `Add ${config.singular}`}
          </button>
        </div>
      </form>
    </Modal>
  );
}
