import { useEffect, useId, useRef } from "react";
import type { ReactNode } from "react";
import { X } from "lucide-react";

export function Modal({
  title,
  description,
  children,
  onClose,
  busy = false,
}: {
  title: string;
  description?: string;
  children: ReactNode;
  onClose: () => void;
  busy?: boolean;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  const id = useId();
  useEffect(() => {
    const dialog = ref.current;
    const previousFocus = document.activeElement;
    dialog?.showModal();
    return () => {
      dialog?.close();
      if (previousFocus instanceof HTMLElement && previousFocus.isConnected) {
        previousFocus.focus();
      }
    };
  }, []);
  return (
    <dialog
      ref={ref}
      aria-labelledby={`${id}-title`}
      aria-describedby={description ? `${id}-description` : undefined}
      onCancel={(event) => {
        event.preventDefault();
        if (!busy) onClose();
      }}
      className="m-auto w-[calc(100%-2rem)] max-w-lg rounded-2xl border-0 bg-white p-0 text-slate-900 shadow-2xl backdrop:bg-slate-950/35 backdrop:backdrop-blur-[2px]"
    >
      <div className="flex items-start justify-between gap-4 border-b border-slate-100 p-6">
        <div>
          <h2
            id={`${id}-title`}
            className="text-lg font-semibold tracking-tight"
          >
            {title}
          </h2>
          {description && (
            <p
              id={`${id}-description`}
              className="mt-1.5 text-sm leading-6 text-slate-500"
            >
              {description}
            </p>
          )}
        </div>
        <button
          className="icon-button shrink-0"
          aria-label="Close dialog"
          disabled={busy}
          onClick={onClose}
        >
          <X className="size-4" />
        </button>
      </div>
      {children}
    </dialog>
  );
}
