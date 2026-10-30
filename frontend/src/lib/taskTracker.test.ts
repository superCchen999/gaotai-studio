import { describe, expect, it } from "vitest";
import { initialTracker, reduceTracker, type TrackerState } from "./taskTracker";
import type { Task } from "./types";

function task(overrides: Partial<Task> & Pick<Task, "id" | "status">): Task {
  return {
    templateId: "demo-postcard",
    templateTitle: overrides.id === "task-b" ? "人像卡片" : "旅行明信片",
    stage: overrides.status === "PENDING" ? null : "ASSET_CHECK",
    failureStage: null,
    failureReason: null,
    resultUrl: null,
    createdAt: "2026-04-01T00:00:00.000Z",
    updatedAt: "2026-04-01T00:00:01.000Z",
    finishedAt: null,
    queryError: null,
    ...overrides,
  };
}

function session(): TrackerState {
  return reduceTracker(initialTracker, {
    type: "begin-session",
    userId: "u-linxia",
    sessionEpoch: 1,
  });
}

describe("reduceTracker", () => {
  it("keeps two tasks independent when one fails", () => {
    let state = session();
    state = reduceTracker(state, {
      type: "hydrate",
      userId: "u-linxia",
      sessionEpoch: 1,
      tasks: [
        task({ id: "task-a", status: "PROCESSING", stage: "ASSET_CHECK" }),
        task({
          id: "task-b",
          status: "PROCESSING",
          stage: "SIMULATE_GENERATE",
          createdAt: "2026-04-01T00:00:02.000Z",
        }),
      ],
    });
    state = reduceTracker(state, {
      type: "poll-success",
      userId: "u-linxia",
      sessionEpoch: 1,
      seq: 1,
      latestSeq: 1,
      task: task({
        id: "task-a",
        status: "FAILED",
        stage: "SIMULATE_GENERATE",
        failureStage: "SIMULATE_GENERATE",
        failureReason: "模拟生成失败",
        updatedAt: "2026-04-01T00:00:05.000Z",
      }),
    });
    const failed = state.tasks.find((item) => item.id === "task-a");
    const other = state.tasks.find((item) => item.id === "task-b");
    expect(failed?.status).toBe("FAILED");
    expect(failed?.failureReason).toBe("模拟生成失败");
    expect(other?.status).toBe("PROCESSING");
    expect(other?.stage).toBe("SIMULATE_GENERATE");
    expect(other?.failureReason).toBeNull();
    expect(state.trackingIds).toEqual(["task-b"]);
  });

  it("does not mark a task failed when the query fails", () => {
    let state = session();
    state = reduceTracker(state, {
      type: "hydrate",
      userId: "u-linxia",
      sessionEpoch: 1,
      tasks: [task({ id: "task-a", status: "PROCESSING", stage: "ASSET_CHECK" })],
    });
    state = reduceTracker(state, {
      type: "poll-error",
      userId: "u-linxia",
      sessionEpoch: 1,
      seq: 2,
      latestSeq: 2,
      taskId: "task-a",
      message: "暂时无法查询任务状态，请稍后重试",
    });
    const current = state.tasks[0];
    expect(current.status).toBe("PROCESSING");
    expect(current.stage).toBe("ASSET_CHECK");
    expect(current.failureReason).toBeNull();
    expect(current.queryError).toBe("暂时无法查询任务状态，请稍后重试");
    expect(state.trackingIds).toEqual([]);
    state = reduceTracker(state, { type: "resume", taskId: "task-a" });
    expect(state.tasks[0].queryError).toBeNull();
    expect(state.tasks[0].status).toBe("PROCESSING");
    expect(state.trackingIds).toEqual(["task-a"]);
  });

  it("ignores stale users, epochs, sequences, and completed regressions", () => {
    let state = reduceTracker(initialTracker, {
      type: "begin-session",
      userId: "u-linxia",
      sessionEpoch: 3,
    });
    state = reduceTracker(state, {
      type: "hydrate",
      userId: "u-linxia",
      sessionEpoch: 3,
      tasks: [
        task({
          id: "task-a",
          status: "SUCCEEDED",
          stage: "RESULT_PACKAGING",
          resultUrl: "/media/results/task-a.png",
          updatedAt: "2026-04-01T00:00:09.000Z",
        }),
      ],
    });
    const snapshot = state;
    state = reduceTracker(state, {
      type: "poll-success",
      userId: "u-zhouyu",
      sessionEpoch: 3,
      seq: 1,
      latestSeq: 1,
      task: task({ id: "task-a", status: "FAILED", failureReason: "其他用户" }),
    });
    state = reduceTracker(state, {
      type: "poll-success",
      userId: "u-linxia",
      sessionEpoch: 2,
      seq: 1,
      latestSeq: 1,
      task: task({ id: "task-a", status: "FAILED", failureReason: "旧会话" }),
    });
    state = reduceTracker(state, {
      type: "poll-success",
      userId: "u-linxia",
      sessionEpoch: 3,
      seq: 1,
      latestSeq: 4,
      task: task({ id: "task-a", status: "FAILED", failureReason: "旧响应" }),
    });
    state = reduceTracker(state, {
      type: "poll-success",
      userId: "u-linxia",
      sessionEpoch: 3,
      seq: 5,
      latestSeq: 5,
      task: task({ id: "task-a", status: "PROCESSING", stage: "ASSET_CHECK" }),
    });
    expect(state).toEqual(snapshot);
    expect(state.tasks[0].status).toBe("SUCCEEDED");
    state = reduceTracker(state, { type: "resume", taskId: "task-a" });
    expect(state.trackingIds).toEqual([]);
  });

  it("drops the previous user and keeps a task created before hydration returns", () => {
    let state = reduceTracker(initialTracker, {
      type: "begin-session",
      userId: "u-linxia",
      sessionEpoch: 1,
    });
    state = reduceTracker(state, {
      type: "add-task",
      userId: "u-linxia",
      sessionEpoch: 1,
      task: task({ id: "task-a", status: "PENDING", stage: null }),
    });
    state = reduceTracker(state, {
      type: "begin-session",
      userId: "u-zhouyu",
      sessionEpoch: 2,
    });
    expect(state.tasks).toEqual([]);
    state = reduceTracker(state, {
      type: "add-task",
      userId: "u-linxia",
      sessionEpoch: 1,
      task: task({ id: "old", status: "PENDING", stage: null }),
    });
    expect(state.tasks).toEqual([]);
    state = reduceTracker(state, {
      type: "add-task",
      userId: "u-zhouyu",
      sessionEpoch: 2,
      task: task({ id: "task-b", status: "PENDING", stage: null, templateTitle: "人像卡片" }),
    });
    state = reduceTracker(state, {
      type: "hydrate",
      userId: "u-zhouyu",
      sessionEpoch: 2,
      tasks: [],
    });
    expect(state.tasks.map((item) => item.id)).toEqual(["task-b"]);
    expect(state.trackingIds).toEqual(["task-b"]);
  });

  it("does not turn tasks failed when the list request fails", () => {
    let state = reduceTracker(initialTracker, {
      type: "begin-session",
      userId: "u-linxia",
      sessionEpoch: 1,
    });
    state = reduceTracker(state, {
      type: "hydrate",
      userId: "u-linxia",
      sessionEpoch: 1,
      tasks: [task({ id: "task-a", status: "PROCESSING" })],
    });
    state = reduceTracker(state, {
      type: "list-error",
      userId: "u-linxia",
      sessionEpoch: 1,
      message: "暂时无法查询任务状态，请稍后重试",
    });
    expect(state.listError).toBe("暂时无法查询任务状态，请稍后重试");
    expect(state.tasks[0].status).toBe("PROCESSING");
    expect(state.hydrating).toBe(false);
  });
});
