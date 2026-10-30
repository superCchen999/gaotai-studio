"use client";

import { formatTime, stageText, STATUS_LABEL } from "@/lib/format";
import type { Task, TaskStatus } from "@/lib/types";

const STAGES = ["ASSET_CHECK", "SIMULATE_GENERATE", "RESULT_PACKAGING"] as const;

function sentence(task: Task): string {
  if (task.status === "PENDING") return "已受理，等待处理";
  if (task.status === "PROCESSING") return `正在进行：${stageText(task.stage)}`;
  if (task.status === "SUCCEEDED") return "制作完成";
  return "制作失败";
}

function stepState(task: Task, stage: string): "done" | "current" | "failed" | "waiting" {
  const index = STAGES.indexOf(stage as (typeof STAGES)[number]);
  const current = task.stage ? STAGES.indexOf(task.stage as (typeof STAGES)[number]) : -1;
  if (task.status === "SUCCEEDED") return "done";
  if (task.status === "FAILED" && task.failureStage === stage) return "failed";
  if (task.status === "PENDING" || current < 0) return "waiting";
  if (index < current) return "done";
  if (index === current) return task.status === "FAILED" ? "failed" : "current";
  return "waiting";
}

export function TaskCard({ task, onResume }: { task: Task; onResume?: (taskId: string) => void }) {
  const status = task.status as TaskStatus;
  return (
    <article className="task-card" aria-labelledby={`task-title-${task.id}`}>
      <div className="task-head">
        <h2 id={`task-title-${task.id}`}>{task.templateTitle || "未命名模板"}</h2>
        <span className={`badge status-${status.toLowerCase()}`}>{STATUS_LABEL[status]}</span>
      </div>
      <p className="meta">
        <time dateTime={task.createdAt}>{formatTime(task.createdAt)}</time>
        <span>编号 {task.id.slice(0, 8)}</span>
      </p>
      <ol className="stages" aria-hidden="true">
        {STAGES.map((stage) => (
          <li key={stage} className={`stage stage-${stepState(task, stage)}`}>
            {stageText(stage)}
          </li>
        ))}
      </ol>
      <p className="stage-sentence">{sentence(task)}</p>
      {status === "FAILED" && task.failureReason ? <p className="reason">{task.failureReason}</p> : null}
      {status === "SUCCEEDED" && task.resultUrl ? (
        <img className="result" src={task.resultUrl} alt={`${task.templateTitle || "任务"}的制作结果`} />
      ) : null}
      {task.queryError ? (
        <div className="query-error" role="status">
          <p>{task.queryError}</p>
          {onResume ? (
            <button type="button" onClick={() => onResume(task.id)}>
              重新查询
            </button>
          ) : null}
        </div>
      ) : null}
    </article>
  );
}
