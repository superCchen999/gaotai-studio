export function formatTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return new Intl.DateTimeFormat("zh-CN", {
    month: "numeric",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    second: "2-digit",
    hour12: false,
  }).format(date);
}

export function formatBytes(size: number): string {
  if (size >= 1024 * 1024 && size % (1024 * 1024) === 0) {
    return `${size / (1024 * 1024)} MB`;
  }
  if (size >= 1024 * 1024) {
    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
  }
  return `${Math.max(1, Math.ceil(size / 1024))} KB`;
}

export function mimeLabel(mimeTypes: string[]): string {
  return mimeTypes
    .map((mime) => {
      if (mime === "image/jpeg") return "JPEG";
      if (mime === "image/png") return "PNG";
      return mime;
    })
    .join("、");
}

export const STAGE_LABEL: Record<string, string> = {
  ASSET_CHECK: "素材检查",
  SIMULATE_GENERATE: "模拟生成",
  RESULT_PACKAGING: "结果整理",
};

export const STATUS_LABEL = {
  PENDING: "等待处理",
  PROCESSING: "处理中",
  SUCCEEDED: "成功",
  FAILED: "失败",
} as const;

export function stageText(stage: string | null): string {
  if (!stage) return "尚未开始";
  return STAGE_LABEL[stage] ?? stage;
}
