import type { Task, TaskStatus } from "./types";

export const POLL_INTERVAL_MS = 1500;

export interface TrackerState {
  userId: string | null;
  sessionEpoch: number;
  tasks: Task[];
  trackingIds: string[];
  listError: string | null;
  hydrating: boolean;
}

export type TrackerAction =
  | { type: "begin-session"; userId: string; sessionEpoch: number }
  | { type: "hydrate"; userId: string; sessionEpoch: number; tasks: Task[] }
  | { type: "list-error"; userId: string; sessionEpoch: number; message: string }
  | { type: "reload-start" }
  | { type: "add-task"; userId: string; sessionEpoch: number; task: Task }
  | {
      type: "poll-success";
      userId: string;
      sessionEpoch: number;
      seq: number;
      latestSeq: number;
      task: Task;
    }
  | {
      type: "poll-error";
      userId: string;
      sessionEpoch: number;
      seq: number;
      latestSeq: number;
      taskId: string;
      message: string;
    }
  | { type: "resume"; taskId: string };

export const initialTracker: TrackerState = {
  userId: null,
  sessionEpoch: 0,
  tasks: [],
  trackingIds: [],
  listError: null,
  hydrating: false,
};

export function isTerminal(status: TaskStatus): boolean {
  return status === "SUCCEEDED" || status === "FAILED";
}

export function reduceTracker(state: TrackerState, action: TrackerAction): TrackerState {
  switch (action.type) {
    case "begin-session":
      return {
        userId: action.userId,
        sessionEpoch: action.sessionEpoch,
        tasks: [],
        trackingIds: [],
        listError: null,
        hydrating: true,
      };
    case "hydrate": {
      if (!sameSession(state, action.userId, action.sessionEpoch)) return state;
      const tasks = sortTasks(mergeTasks(state.tasks, action.tasks));
      return {
        ...state,
        tasks,
        trackingIds: trackingIdsFor(tasks),
        listError: null,
        hydrating: false,
      };
    }
    case "list-error":
      if (!sameSession(state, action.userId, action.sessionEpoch)) return state;
      return { ...state, listError: action.message, hydrating: false };
    case "reload-start":
      return { ...state, hydrating: true, listError: null };
    case "add-task": {
      if (!sameSession(state, action.userId, action.sessionEpoch)) return state;
      const next = normalize(action.task);
      const without = state.tasks.filter((task) => task.id !== next.id);
      const tasks = sortTasks([next, ...without]);
      return { ...state, tasks, trackingIds: trackingIdsFor(tasks) };
    }
    case "poll-success": {
      if (!sameSession(state, action.userId, action.sessionEpoch)) return state;
      if (action.seq !== action.latestSeq) return state;
      const incoming = normalize(action.task);
      const previous = state.tasks.find((task) => task.id === incoming.id);
      if (previous && isTerminal(previous.status) && !isTerminal(incoming.status)) return state;
      const tasks = sortTasks(
        previous
          ? state.tasks.map((task) => (task.id === incoming.id ? incoming : task))
          : [incoming, ...state.tasks],
      );
      return { ...state, tasks, trackingIds: trackingIdsFor(tasks) };
    }
    case "poll-error": {
      if (!sameSession(state, action.userId, action.sessionEpoch)) return state;
      if (action.seq !== action.latestSeq) return state;
      if (!state.tasks.some((task) => task.id === action.taskId)) return state;
      const tasks = state.tasks.map((task) =>
        task.id === action.taskId ? { ...task, queryError: action.message } : task,
      );
      return { ...state, tasks, trackingIds: trackingIdsFor(tasks) };
    }
    case "resume": {
      const current = state.tasks.find((task) => task.id === action.taskId);
      if (!current || isTerminal(current.status)) return state;
      const tasks = state.tasks.map((task) =>
        task.id === action.taskId ? { ...task, queryError: null } : task,
      );
      return { ...state, tasks, trackingIds: trackingIdsFor(tasks) };
    }
    default:
      return state;
  }
}

function sameSession(state: TrackerState, userId: string, sessionEpoch: number): boolean {
  return state.userId === userId && state.sessionEpoch === sessionEpoch;
}

function normalize(task: Task): Task {
  return { ...task, queryError: task.queryError ?? null };
}

function trackingIdsFor(tasks: Task[]): string[] {
  return tasks.filter((task) => !isTerminal(task.status) && !task.queryError).map((task) => task.id);
}

function mergeTasks(existing: Task[], incoming: Task[]): Task[] {
  const previous = new Map(existing.map((task) => [task.id, task]));
  const seen = new Set<string>();
  const merged: Task[] = [];
  for (const task of incoming) {
    seen.add(task.id);
    const prev = previous.get(task.id);
    const next = normalize(task);
    if (!prev) {
      merged.push(next);
      continue;
    }
    if (isTerminal(prev.status) && !isTerminal(next.status)) {
      merged.push(prev);
      continue;
    }
    if (time(prev.updatedAt) > time(next.updatedAt)) {
      merged.push(prev);
      continue;
    }
    merged.push(next);
  }
  for (const task of existing) {
    if (!seen.has(task.id)) merged.push(task);
  }
  return merged;
}

function sortTasks(tasks: Task[]): Task[] {
  return [...tasks].sort((left, right) => time(right.createdAt) - time(left.createdAt));
}

function time(value: string): number {
  const parsed = Date.parse(value);
  return Number.isFinite(parsed) ? parsed : 0;
}
