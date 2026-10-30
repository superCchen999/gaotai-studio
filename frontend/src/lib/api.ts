import type { AssetRecord, ServerTask, StudioUser, Template, TemplateList } from "./types";

export class ApiError extends Error {
  status: number;
  code: string;

  constructor(status: number, code: string, message: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
  }
}

export function isAbort(error: unknown): boolean {
  return error instanceof Error && error.name === "AbortError";
}

export function toQueryErrorMessage(error: unknown): string {
  if (isAbort(error)) return "查询已取消";
  return "暂时无法查询任务状态，请稍后重试";
}

export function toErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof ApiError && error.message) return error.message;
  return fallback;
}

async function request<T>(userId: string | null, path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  if (userId) headers.set("X-User-Id", userId);
  if (init.body && !(init.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const response = await fetch(path, { ...init, headers, cache: "no-store" });
  if (!response.ok) {
    let code = "REQUEST_FAILED";
    let message = response.status >= 500 ? "服务器暂时不可用" : "请求失败";
    try {
      const body = (await response.json()) as { error?: unknown; message?: unknown };
      if (typeof body.message === "string" && body.message) message = body.message;
      if (typeof body.error === "string" && body.error) code = body.error;
    } catch {
      // 保留上面的默认说明。
    }
    throw new ApiError(response.status, code, message);
  }
  return (await response.json()) as T;
}

export function getUsers(signal?: AbortSignal): Promise<StudioUser[]> {
  return request<StudioUser[]>(null, "/api/users", { signal });
}

export function getTemplates(userId: string, category: string, signal?: AbortSignal): Promise<TemplateList> {
  const query = category ? `?category=${encodeURIComponent(category)}` : "";
  return request<TemplateList>(userId, `/api/templates${query}`, { signal });
}

export function getTemplate(userId: string, id: string, signal?: AbortSignal): Promise<Template> {
  return request<Template>(userId, `/api/templates/${encodeURIComponent(id)}`, { signal });
}

export function uploadAsset(userId: string, file: File, signal?: AbortSignal): Promise<AssetRecord> {
  const body = new FormData();
  body.set("file", file);
  return request<AssetRecord>(userId, "/api/assets", { method: "POST", body, signal });
}

export function createTask(userId: string, templateId: string, assetIds: string[], signal?: AbortSignal): Promise<ServerTask> {
  return request<ServerTask>(userId, "/api/tasks", {
    method: "POST",
    body: JSON.stringify({ templateId, assetIds }),
    signal,
  });
}

export function listTasks(userId: string, signal?: AbortSignal): Promise<ServerTask[]> {
  return request<ServerTask[]>(userId, "/api/tasks", { signal });
}

export function getTask(userId: string, taskId: string, signal?: AbortSignal): Promise<ServerTask> {
  return request<ServerTask>(userId, `/api/tasks/${encodeURIComponent(taskId)}`, { signal });
}
