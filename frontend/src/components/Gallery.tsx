"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { getTemplates, isAbort, toErrorMessage } from "@/lib/api";
import type { Template } from "@/lib/types";
import { useStudio } from "./Providers";

export function Gallery() {
  const { userId, userError } = useStudio();
  const [category, setCategory] = useState("");
  const [categories, setCategories] = useState<string[]>([]);
  const [templates, setTemplates] = useState<Template[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    if (!userId) return;
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    setTemplates([]);
    getTemplates(userId, category, controller.signal)
      .then((result) => {
        if (controller.signal.aborted) return;
        setCategories(result.categories);
        setTemplates(result.templates);
        setLoading(false);
      })
      .catch((reason: unknown) => {
        if (controller.signal.aborted || isAbort(reason)) return;
        setError(toErrorMessage(reason, "模板列表加载失败"));
        setLoading(false);
      });
    return () => controller.abort();
  }, [userId, category, reloadKey]);

  return (
    <section className="page">
      <div className="page-intro">
        <h1>选择一个模板</h1>
        <p>上传素材后提交制作。任务在后台分步处理，你可以继续浏览，或到「我的」查看制作记录。</p>
      </div>
      {userError ? <div className="banner" role="alert">{userError}</div> : null}
      <div className="category-bar" role="group" aria-label="模板分类">
        <button type="button" aria-pressed={category === ""} onClick={() => setCategory("")}>
          全部
        </button>
        {categories.map((item) => (
          <button
            key={item}
            type="button"
            aria-pressed={category === item}
            onClick={() => setCategory(item)}
          >
            {item}
          </button>
        ))}
      </div>
      {!userId && !userError ? <p className="status-line">正在准备用户</p> : null}
      {loading ? <p className="status-line">正在读取模板</p> : null}
      {error ? (
        <div className="banner" role="alert">
          <p>{error}</p>
          <button type="button" onClick={() => setReloadKey((value) => value + 1)}>
            重试
          </button>
        </div>
      ) : null}
      {userId && !loading && !error && templates.length === 0 ? <p className="status-line">这个分类还没有模板</p> : null}
      <ul className="cards">
        {templates.map((template) => (
          <li key={template.id}>
            <Link className="card" href={`/templates/${template.id}`}>
              <img src={template.coverUrl} alt="" />
              <div className="card-body">
                <span className="eyebrow">{template.category}</span>
                <h2>{template.title}</h2>
                <p>{template.requiredImageCount} 张素材</p>
              </div>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}
