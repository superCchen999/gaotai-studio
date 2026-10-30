"use client";

export default function AppError({ reset }: { error: Error; reset: () => void }) {
  return (
    <section className="page">
      <h1>页面出错了</h1>
      <p>当前页面没有正常显示。已有的制作任务仍保留在服务器上。</p>
      <button type="button" onClick={reset}>
        重试
      </button>
    </section>
  );
}
