import { useRef, useState } from "react";
import { AlertTriangle, LoaderCircle } from "lucide-react";
import { Modal } from "./Modal";
import { ErrorState } from "./ui";

export function ConfirmDialog({
  title,
  description,
  confirmLabel = "Delete record",
  onConfirm,
  onClose,
}: {
  title: string;
  description: string;
  confirmLabel?: string;
  onConfirm: () => Promise<void>;
  onClose: () => void;
}) {
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const lock = useRef(false);
  async function confirm() {
    if (lock.current) return;
    lock.current = true;
    setPending(true);
    setError(null);
    try {
      await onConfirm();
      onClose();
    } catch (error) {
      setError(error);
    } finally {
      lock.current = false;
      setPending(false);
    }
  }
  return (
    <Modal title={title} onClose={onClose} busy={pending}>
      <div className="space-y-4 p-6">
        <div className="flex gap-3">
          <div className="flex size-10 shrink-0 items-center justify-center rounded-xl bg-red-50 text-red-600">
            <AlertTriangle className="size-5" />
          </div>
          <p className="min-w-0 text-sm leading-6 break-words text-slate-600">
            {description}
          </p>
        </div>
        {error !== null && <ErrorState error={error} />}
      </div>
      <div className="dialog-footer">
        <button
          className="button-secondary"
          disabled={pending}
          onClick={onClose}
        >
          Keep record
        </button>
        <button
          className="button-danger"
          disabled={pending}
          onClick={() => void confirm()}
        >
          {pending && <LoaderCircle className="size-4 animate-spin" />}
          {pending ? "Deleting…" : confirmLabel}
        </button>
      </div>
    </Modal>
  );
}
