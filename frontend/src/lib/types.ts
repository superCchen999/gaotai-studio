export type TaskStatus = "PENDING" | "PROCESSING" | "SUCCEEDED" | "FAILED";

export interface ServerTask {
  id: string;
  templateId: string;
  templateTitle: string;
  status: TaskStatus;
  stage: string | null;
  failureStage: string | null;
  failureReason: string | null;
  resultUrl: string | null;
  createdAt: string;
  updatedAt: string;
  finishedAt: string | null;
}

export interface Task extends ServerTask {
  queryError: string | null;
}

export interface Template {
  id: string;
  title: string;
  category: string;
  coverUrl: string;
  description: string;
  requiredImageCount: number;
  acceptedMimeTypes: string[];
  maxFileSizeBytes: number;
}

export interface TemplateList {
  categories: string[];
  templates: Template[];
}

export interface StudioUser {
  id: string;
  displayName: string;
}

export interface AssetRecord {
  id: string;
  url: string;
  mimeType: string;
  sizeBytes: number;
  originalFilename: string;
}

export function toTask(task: ServerTask): Task {
  return { ...task, queryError: null };
}
