import { createContext, useContext } from "react";

export interface Notice {
  id: number;
  message: string;
  kind: "success" | "error";
}
export const NotificationContext = createContext<
  ((message: string, kind?: Notice["kind"]) => void) | null
>(null);
export function useNotify() {
  const notify = useContext(NotificationContext);
  if (!notify) throw new Error("Notifications require NotificationProvider");
  return notify;
}
