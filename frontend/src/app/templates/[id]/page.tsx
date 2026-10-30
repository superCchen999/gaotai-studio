"use client";

import { useParams } from "next/navigation";
import { TemplateEditor } from "@/components/TemplateEditor";

export default function TemplatePage() {
  const params = useParams<{ id: string }>();
  const id = Array.isArray(params.id) ? params.id[0] : params.id;
  if (!id) return <p className="status-line">模板不存在</p>;
  return <TemplateEditor templateId={id} />;
}
