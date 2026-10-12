import { useCallback, useEffect, useRef, useState } from "react";
import type { ReactNode } from "react";
import { CheckCircle2, AlertCircle, X } from "lucide-react";
import { NotificationContext } from "../hooks/notificationContext";
import type { Notice } from "../hooks/notificationContext";

export function NotificationProvider({ children }: { children: ReactNode }) {
  const [notices, setNotices] = useState<Notice[]>([]);
  const counter = useRef(0);
  const timers = useRef<ReturnType<typeof setTimeout>[]>([]);
  const notify = useCallback(
    (message: string, kind: Notice["kind"] = "success") => {
      const id = ++counter.current;
      setNotices((previous) => [...previous, { id, message, kind }]);
      timers.current.push(
        setTimeout(
          () =>
            setNotices((previous) =>
              previous.filter((notice) => notice.id !== id),
            ),
          7000,
        ),
      );
    },
    [],
  );
  useEffect(() => {
    const handles = timers.current;
    return () => handles.forEach(clearTimeout);
  }, []);
  return (
    <NotificationContext.Provider value={notify}>
      {children}
      <div
        role="region"
        className="fixed right-4 bottom-4 z-50 flex w-[calc(100%-2rem)] max-w-sm flex-col gap-3"
        aria-label="Notifications"
      >
        {notices.map((notice) => (
          <div
            key={notice.id}
            role={notice.kind === "error" ? "alert" : "status"}
            className="flex items-start gap-3 rounded-xl border border-slate-200 bg-white p-4 shadow-lg"
          >
            {notice.kind === "success" ? (
              <CheckCircle2 className="size-5 shrink-0 text-emerald-600" />
            ) : (
              <AlertCircle className="size-5 shrink-0 text-red-600" />
            )}
            <p className="flex-1 text-sm leading-5 text-slate-700">
              {notice.message}
            </p>
            <button
              aria-label="Dismiss notification"
              className="text-slate-400 hover:text-slate-800"
              onClick={() =>
                setNotices((previous) =>
                  previous.filter((item) => item.id !== notice.id),
                )
              }
            >
              <X className="size-4" />
            </button>
          </div>
        ))}
      </div>
    </NotificationContext.Provider>
  );
}
