"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { createTask, getTemplate, isAbort, toErrorMessage, uploadAsset } from "@/lib/api";
import { formatBytes, mimeLabel } from "@/lib/format";
import { isTerminal } from "@/lib/taskTracker";
import type { Template } from "@/lib/types";
import { TaskCard } from "./TaskCard";
import { useStudio } from "./Providers";

type SlotPhase = "empty" | "uploading" | "ready" | "error";

interface Slot {
  phase: SlotPhase;
  assetId?: string;
  previewUrl?: string;
  fileName?: string;
  message?: string;
}

function emptySlots(count: number): Slot[] {
  return Array.from({ length: count }, () => ({ phase: "empty" }));
}

function validateFile(file: File, template: Template): string | null {
  if (!template.acceptedMimeTypes.includes(file.type)) {
    return `仅支持 ${mimeLabel(template.acceptedMimeTypes)}`;
  }
  if (file.size <= 0) return "文件是空的";
  if (file.size > template.maxFileSizeBytes) return `文件超过 ${formatBytes(template.maxFileSizeBytes)} 限制`;
  return null;
}

export function TemplateEditor({ templateId }: { templateId: string }) {
  const { userId, tasks, addTask, resumeTask } = useStudio();
  const [template, setTemplate] = useState<Template | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [slots, setSlots] = useState<Slot[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [sessionIds, setSessionIds] = useState<string[]>([]);
  const [reloadKey, setReloadKey] = useState(0);
  const slotsRef = useRef<Slot[]>([]);
  const userIdRef = useRef(userId);
  const uploadsRef = useRef<AbortController[]>([]);
  slotsRef.current = slots;
  userIdRef.current = userId;

  useEffect(() => {
    setSessionIds((current) => (current.length === 0 ? current : []));
    setNotice(null);
    setSubmitError(null);
  }, [userId]);

  useEffect(() => {
    const uploads = uploadsRef.current;
    return () => {
      slotsRef.current.forEach((slot) => {
        if (slot.previewUrl) URL.revokeObjectURL(slot.previewUrl);
      });
      uploads.forEach((controller) => controller.abort());
    };
  }, []);

  useEffect(() => {
    if (!userId) return;
    const controller = new AbortController();
    setLoading(true);
    setLoadError(null);
    getTemplate(userId, templateId, controller.signal)
      .then((next) => {
        if (controller.signal.aborted) return;
        setTemplate(next);
        setSlots((current) => {
          current.forEach((slot) => {
            if (slot.previewUrl) URL.revokeObjectURL(slot.previewUrl);
          });
          return emptySlots(next.requiredImageCount);
        });
        setLoading(false);
        document.title = `${next.title} · 稿台`;
      })
      .catch((reason: unknown) => {
        if (controller.signal.aborted || isAbort(reason)) return;
        setTemplate(null);
        setLoadError(toErrorMessage(reason, "模板加载失败"));
        setLoading(false);
      });
    return () => controller.abort();
  }, [userId, templateId, reloadKey]);

  const ready = Boolean(template) && slots.length > 0 && slots.every((slot) => slot.phase === "ready" && slot.assetId);
  const uploading = slots.some((slot) => slot.phase === "uploading");

  function revoke(slot: Slot | undefined) {
    if (slot?.previewUrl) URL.revokeObjectURL(slot.previewUrl);
  }

  async function onFile(index: number, file: File | undefined) {
    if (!file || !template || !userId) return;
    const problem = validateFile(file, template);
    const previewUrl = problem ? undefined : URL.createObjectURL(file);
    setSlots((current) => {
      const next = current.slice();
      revoke(next[index]);
      next[index] = problem
        ? { phase: "error", message: problem }
        : { phase: "uploading", previewUrl, fileName: file.name };
      return next;
    });
    if (problem || !previewUrl) return;
    uploadsRef.current[index]?.abort();
    const controller = new AbortController();
    uploadsRef.current[index] = controller;
    const owner = userId;
    try {
      const asset = await uploadAsset(owner, file, controller.signal);
      if (controller.signal.aborted || userIdRef.current !== owner) {
        URL.revokeObjectURL(previewUrl);
        return;
      }
      setSlots((current) => {
        const next = current.slice();
        if (next[index]?.previewUrl && next[index].previewUrl !== previewUrl) {
          URL.revokeObjectURL(previewUrl);
          return current;
        }
        next[index] = { phase: "ready", assetId: asset.id, previewUrl, fileName: file.name };
        return next;
      });
    } catch (reason) {
      if (controller.signal.aborted || isAbort(reason)) return;
      URL.revokeObjectURL(previewUrl);
      if (userIdRef.current !== owner) return;
      setSlots((current) => {
        const next = current.slice();
        next[index] = { phase: "error", message: toErrorMessage(reason, "上传失败") };
        return next;
      });
    }
  }

  function removeSlot(index: number) {
    uploadsRef.current[index]?.abort();
    setSlots((current) => {
      const next = current.slice();
      revoke(next[index]);
      next[index] = { phase: "empty" };
      return next;
    });
  }

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault();
    if (!template || !userId || !ready || submitting || uploading) return;
    setSubmitting(true);
    setSubmitError(null);
    setNotice(null);
    const owner = userId;
    try {
      const created = await createTask(
        owner,
        template.id,
        slots.map((slot) => slot.assetId).filter((id): id is string => Boolean(id)),
      );
      if (userIdRef.current !== owner) return;
      addTask(created);
      setSessionIds((current) => [created.id, ...current.filter((id) => id !== created.id)]);
      setSlots((current) => {
        current.forEach(revoke);
        return emptySlots(template.requiredImageCount);
      });
      setNotice("任务已受理，可以继续制作或到记录页查看进度。");
    } catch (reason) {
      if (userIdRef.current !== owner) return;
      setSubmitError(toErrorMessage(reason, "提交失败"));
    } finally {
      setSubmitting(false);
    }
  }

  const visible = tasks.filter((task) => sessionIds.includes(task.id) || !isTerminal(task.status));

  return (
    <section className="page">
      <p className="back">
        <Link href="/">返回模板</Link>
      </p>
      {loading ? <p className="status-line">正在读取模板</p> : null}
      {loadError ? (
        <div className="banner" role="alert">
          <p>{loadError}</p>
          <button type="button" onClick={() => setReloadKey((value) => value + 1)}>
            重试
          </button>
        </div>
      ) : null}
      {template ? (
        <div className="detail">
          <div className="detail-summary">
            <img src={template.coverUrl} alt="" />
            <span className="eyebrow">{template.category}</span>
            <h1>{template.title}</h1>
            <p>{template.description}</p>
            <ul className="requirements">
              <li>需要 {template.requiredImageCount} 张图片</li>
              <li>格式 {mimeLabel(template.acceptedMimeTypes)}</li>
              <li>单张不超过 {formatBytes(template.maxFileSizeBytes)}</li>
            </ul>
          </div>
          <form onSubmit={onSubmit}>
            <div className="slots">
              {slots.map((slot, index) => (
                <div className={`slot slot-${slot.phase}`} key={`${template.id}-${index}`}>
                  {slot.previewUrl ? <img src={slot.previewUrl} alt="" /> : <span className="slot-index">素材 {index + 1}</span>}
                  {slot.phase === "uploading" ? <p>上传中</p> : null}
                  {slot.phase === "ready" ? <p>已上传 {slot.fileName}</p> : null}
                  {slot.phase === "error" ? <p role="alert">{slot.message}</p> : null}
                  <div className="slot-actions">
                    <label className="button file-button">
                      {slot.phase === "ready" ? "替换" : "选择图片"}
                      <input
                        className="file-input"
                        type="file"
                        accept={template.acceptedMimeTypes.join(",")}
                        aria-label={`选择第 ${index + 1} 张素材，${mimeLabel(template.acceptedMimeTypes)}`}
                        onChange={(event) => {
                          const file = event.target.files?.[0];
                          event.target.value = "";
                          void onFile(index, file);
                        }}
                      />
                    </label>
                    {slot.phase === "ready" || slot.phase === "error" ? (
                      <button type="button" onClick={() => removeSlot(index)}>
                        移除第 {index + 1} 张素材
                      </button>
                    ) : null}
                  </div>
                </div>
              ))}
            </div>
            <div className="submit-row">
              <button type="submit" disabled={!ready || submitting || uploading} aria-describedby="submit-hint">
                {submitting ? "提交中" : "开始制作"}
              </button>
              <p id="submit-hint">
                {submitting ? "正在把任务交给后台" : ready ? "素材已准备好" : "请按模板要求上传图片后再提交"}
              </p>
            </div>
            {submitError ? <p className="reason" role="alert">{submitError}</p> : null}
            {notice ? <p className="notice" role="status">{notice}</p> : null}
          </form>
        </div>
      ) : null}
      {visible.length > 0 ? (
        <div className="task-list">
          <h2>任务进度</h2>
          {visible.map((task) => (
            <TaskCard key={task.id} task={task} onResume={resumeTask} />
          ))}
        </div>
      ) : null}
    </section>
  );
}
