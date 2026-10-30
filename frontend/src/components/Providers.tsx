"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useReducer, useRef } from "react";
import { getTask, getUsers, isAbort, listTasks, toQueryErrorMessage } from "@/lib/api";
import { initialTracker, POLL_INTERVAL_MS, reduceTracker } from "@/lib/taskTracker";
import { toTask, type ServerTask, type StudioUser, type Task } from "@/lib/types";

const USER_KEY = "gaotai-user-id";

interface StudioContextValue {
  users: StudioUser[];
  userId: string | null;
  userError: string | null;
  tasks: Task[];
  hydrating: boolean;
  listError: string | null;
  selectUser: (userId: string) => void;
  reloadTasks: () => void;
  addTask: (task: ServerTask) => void;
  resumeTask: (taskId: string) => void;
}

const StudioContext = createContext<StudioContextValue | null>(null);

export function useStudio(): StudioContextValue {
  const value = useContext(StudioContext);
  if (!value) throw new Error("页面状态尚未准备好");
  return value;
}

function delay(ms: number, signal: AbortSignal): Promise<void> {
  return new Promise((resolve) => {
    if (signal.aborted) {
      resolve();
      return;
    }
    const timer = setTimeout(() => {
      signal.removeEventListener("abort", onAbort);
      resolve();
    }, ms);
    const onAbort = () => {
      clearTimeout(timer);
      resolve();
    };
    signal.addEventListener("abort", onAbort);
  });
}

export function Providers({ children }: { children: React.ReactNode }) {
  const [users, setUsers] = useReducer((_: StudioUser[], next: StudioUser[]) => next, [] as StudioUser[]);
  const [userError, setUserError] = useReducer((_: string | null, next: string | null) => next, null);
  const [tracker, dispatch] = useReducer(reduceTracker, initialTracker);
  const userIdRef = useRef<string | null>(null);
  const epochRef = useRef(0);
  const seqsRef = useRef<Record<string, number>>({});
  const trackingRef = useRef<string[]>([]);
  const listAbortRef = useRef<AbortController | null>(null);
  trackingRef.current = tracker.trackingIds;

  const startListLoad = useCallback((userId: string, sessionEpoch: number) => {
    listAbortRef.current?.abort();
    const controller = new AbortController();
    listAbortRef.current = controller;
    listTasks(userId, controller.signal)
      .then((tasks) => {
        if (controller.signal.aborted) return;
        dispatch({ type: "hydrate", userId, sessionEpoch, tasks: tasks.map(toTask) });
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted || isAbort(error)) return;
        dispatch({
          type: "list-error",
          userId,
          sessionEpoch,
          message: toQueryErrorMessage(error),
        });
      });
  }, []);

  const selectUser = useCallback((nextId: string) => {
    userIdRef.current = nextId;
    epochRef.current += 1;
    seqsRef.current = {};
    window.localStorage.setItem(USER_KEY, nextId);
    const sessionEpoch = epochRef.current;
    dispatch({ type: "begin-session", userId: nextId, sessionEpoch });
    startListLoad(nextId, sessionEpoch);
  }, [startListLoad]);

  const reloadTasks = useCallback(() => {
    const userId = userIdRef.current;
    if (!userId) return;
    dispatch({ type: "reload-start" });
    startListLoad(userId, epochRef.current);
  }, [startListLoad]);

  const addTask = useCallback((task: ServerTask) => {
    const userId = userIdRef.current;
    if (!userId) return;
    dispatch({
      type: "add-task",
      userId,
      sessionEpoch: epochRef.current,
      task: toTask(task),
    });
  }, []);

  const resumeTask = useCallback((taskId: string) => {
    dispatch({ type: "resume", taskId });
  }, []);

  useEffect(() => {
    const controller = new AbortController();
    getUsers(controller.signal)
      .then((loaded) => {
        if (controller.signal.aborted) return;
        setUsers(loaded);
        const saved = window.localStorage.getItem(USER_KEY);
        const picked = loaded.find((user) => user.id === saved) ?? loaded[0];
        if (picked) selectUser(picked.id);
      })
      .catch((error: unknown) => {
        if (controller.signal.aborted || isAbort(error)) return;
        setUserError("暂时无法加载用户，请刷新页面");
      });
    return () => {
      controller.abort();
      listAbortRef.current?.abort();
    };
  }, [selectUser]);

  const trackingKey = tracker.trackingIds.slice().sort().join("|");
  useEffect(() => {
    if (!tracker.userId || trackingKey === "") return;
    const controller = new AbortController();
    let stopped = false;
    const loop = async () => {
      while (!stopped) {
        const ids = trackingRef.current.slice();
        const userId = userIdRef.current;
        const sessionEpoch = epochRef.current;
        if (!userId || ids.length === 0) return;
        const planned = ids.map((taskId) => {
          const seq = (seqsRef.current[taskId] ?? 0) + 1;
          seqsRef.current = { ...seqsRef.current, [taskId]: seq };
          return { taskId, seq };
        });
        await Promise.all(
          planned.map(async ({ taskId, seq }) => {
            try {
              const task = await getTask(userId, taskId, controller.signal);
              if (stopped) return;
              dispatch({
                type: "poll-success",
                userId,
                sessionEpoch,
                seq,
                latestSeq: seqsRef.current[taskId] ?? seq,
                task: toTask(task),
              });
            } catch (error) {
              if (stopped || isAbort(error)) return;
              dispatch({
                type: "poll-error",
                userId,
                sessionEpoch,
                seq,
                latestSeq: seqsRef.current[taskId] ?? seq,
                taskId,
                message: toQueryErrorMessage(error),
              });
            }
          }),
        );
        if (stopped) return;
        await delay(POLL_INTERVAL_MS, controller.signal);
      }
    };
    void loop();
    return () => {
      stopped = true;
      controller.abort();
    };
  }, [tracker.userId, trackingKey]);

  const value = useMemo<StudioContextValue>(
    () => ({
      users,
      userId: tracker.userId,
      userError,
      tasks: tracker.tasks,
      hydrating: tracker.hydrating,
      listError: tracker.listError,
      selectUser,
      reloadTasks,
      addTask,
      resumeTask,
    }),
    [users, userError, tracker, selectUser, reloadTasks, addTask, resumeTask],
  );

  return <StudioContext.Provider value={value}>{children}</StudioContext.Provider>;
}
