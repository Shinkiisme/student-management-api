import { useEffect, useReducer } from "react";

interface State<T> {
  data: T | null;
  error: unknown;
  pending: boolean;
}
type Action<T> =
  | { type: "loading" }
  | { type: "success"; data: T }
  | { type: "error"; error: unknown };

function reducer<T>(_state: State<T>, action: Action<T>): State<T> {
  if (action.type === "success")
    return { data: action.data, error: null, pending: false };
  if (action.type === "error")
    return { data: null, error: action.error, pending: false };
  return { data: null, error: null, pending: true };
}

export function useAsync<T>(load: (signal: AbortSignal) => Promise<T>) {
  const [revision, reload] = useReducer((value: number) => value + 1, 0);
  const [state, dispatch] = useReducer(reducer<T>, {
    data: null,
    error: null,
    pending: true,
  });
  useEffect(() => {
    const controller = new AbortController();
    dispatch({ type: "loading" });
    void load(controller.signal).then(
      (data) => {
        if (!controller.signal.aborted) dispatch({ type: "success", data });
      },
      (error: unknown) => {
        if (!controller.signal.aborted) dispatch({ type: "error", error });
      },
    );
    return () => controller.abort();
  }, [load, revision]);
  return { ...state, reload };
}
