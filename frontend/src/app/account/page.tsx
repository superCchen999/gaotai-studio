"use client";

import Link from "next/link";
import { useEffect, useId, useRef, useState } from "react";
import { TaskCard } from "@/components/TaskCard";
import { useStudio } from "@/components/Providers";

const DEMO_EMAIL: Record<string, string> = {
  "u-linxia": "linxia@gaotai.local",
  "u-zhouyu": "zhouyu@gaotai.local",
};

const MENU = [
  { id: "billing", label: "管理账单", icon: "card" },
  { id: "terms", label: "服务条款", icon: "doc" },
  { id: "privacy", label: "隐私政策", icon: "user" },
  { id: "subscription", label: "订阅管理", icon: "sub" },
] as const;

export default function AccountPage() {
  const { users, userId, userError, selectUser, tasks, hydrating, listError, reloadTasks, resumeTask } = useStudio();
  const current = users.find((user) => user.id === userId);
  const email = current ? DEMO_EMAIL[current.id] ?? `${current.id}@gaotai.local` : "尚未选择账号";
  const [entry, setEntry] = useState<string | null>(null);
  const dialogRef = useRef<HTMLDialogElement>(null);
  const titleId = useId();

  useEffect(() => {
    document.title = "账号设置 · 稿台";
  }, []);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (!dialog || !entry || dialog.open) return;
    dialog.showModal();
  }, [entry]);

  function openEntry(name: string) {
    setEntry(name);
  }

  function closeEntry() {
    dialogRef.current?.close();
  }

  return (
    <section className="page account-page">
      <div className="account-panel">
        <div className="account-head">
          <h1>账号设置</h1>
          <Link className="account-close" href="/" aria-label="关闭账号设置">
            ×
          </Link>
        </div>

        <div className="account-card">
          <div className="account-profile">
            <span className="avatar" aria-hidden="true">
              <PersonIcon />
            </span>
            <div>
              <p className="account-name">{current?.displayName ?? "正在加载用户"}</p>
              <p className="account-mail">{email}</p>
            </div>
          </div>

          <label className="account-switch">
            切换账号
            <select
              value={userId ?? ""}
              disabled={users.length === 0}
              onChange={(event) => selectUser(event.target.value)}
            >
              {users.length === 0 ? <option value="">正在加载</option> : null}
              {users.map((user) => (
                <option key={user.id} value={user.id}>
                  {user.displayName}
                </option>
              ))}
            </select>
          </label>

          <div className="account-row">
            <span>邮箱</span>
            <strong>{email}</strong>
          </div>
          <div className="account-row">
            <div>
              <span>密码</span>
              <p className="password-mask" aria-hidden="true">
                ••••••••••••
              </p>
              <p className="sr-only">密码已隐藏</p>
            </div>
            <button type="button" onClick={() => openEntry("修改密码")}>
              修改密码
            </button>
          </div>
        </div>

        <div className="account-menu" role="group" aria-label="账号入口">
          {MENU.map((item) => (
            <button key={item.id} type="button" onClick={() => openEntry(item.label)}>
              <span className="menu-icon" aria-hidden="true">
                <MenuIcon name={item.icon} />
              </span>
              <span>{item.label}</span>
              <span className="chevron" aria-hidden="true">
                ›
              </span>
            </button>
          ))}
        </div>

        <section id="records" className="account-records" aria-labelledby="records-title">
          <div className="account-records-head">
            <h2 id="records-title">制作记录</h2>
            <p>只显示当前账号的任务。刷新后会从服务器恢复，未完成的任务会继续更新。</p>
          </div>
          {userError ? (
            <div className="banner" role="alert">
              {userError}
            </div>
          ) : null}
          {!userId && !userError ? <p className="status-line">正在读取记录</p> : null}
          {userId && listError ? (
            <div className="banner" role="status">
              <p>{listError}</p>
              <button type="button" onClick={reloadTasks}>
                重新查询
              </button>
            </div>
          ) : null}
          {userId && hydrating && tasks.length === 0 ? <p className="status-line">正在读取记录</p> : null}
          {userId && !hydrating && !listError && tasks.length === 0 ? (
            <p className="status-line">还没有制作记录</p>
          ) : null}
          <div className="task-list" aria-live="polite">
            {userId
              ? tasks.map((task) => <TaskCard key={task.id} task={task} onResume={resumeTask} />)
              : null}
          </div>
        </section>

        <div className="account-danger">
          <h2>注销账号</h2>
          <button type="button" aria-label="打开注销入口" onClick={() => openEntry("注销")}>
            ···
          </button>
          <p>注销只是入口，不会删除账号，制作记录也会保留。</p>
        </div>
      </div>

      <dialog
        ref={dialogRef}
        className="entry-dialog"
        aria-labelledby={titleId}
        onClose={() => setEntry(null)}
      >
        <h2 id={titleId}>{entry}</h2>
        <p>此功能暂未开放，不会修改账号、密码或任务。</p>
        <button type="button" onClick={closeEntry}>
          知道了
        </button>
      </dialog>
    </section>
  );
}

function PersonIcon() {
  return (
    <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" strokeWidth="1.8">
      <circle cx="12" cy="8" r="3.2" />
      <path d="M5.5 19.2c1.4-3 3.7-4.4 6.5-4.4s5.1 1.4 6.5 4.4" strokeLinecap="round" />
    </svg>
  );
}

function MenuIcon({ name }: { name: (typeof MENU)[number]["icon"] }) {
  if (name === "card") {
    return (
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.7">
        <rect x="3.5" y="6" width="17" height="12" rx="2" />
        <path d="M3.5 10h17" />
      </svg>
    );
  }
  if (name === "doc") {
    return (
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.7">
        <path d="M7 3.5h7l4.5 4.5V20a1.5 1.5 0 0 1-1.5 1.5H7A1.5 1.5 0 0 1 5.5 20V5A1.5 1.5 0 0 1 7 3.5z" />
        <path d="M14 3.8V8h4.2" />
      </svg>
    );
  }
  if (name === "user") {
    return (
      <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.7">
        <circle cx="12" cy="8" r="2.6" />
        <path d="M6.5 18.5c1-2.4 3-3.6 5.5-3.6s4.5 1.2 5.5 3.6" strokeLinecap="round" />
      </svg>
    );
  }
  return (
    <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="1.7">
      <path d="M12 4.5v10" strokeLinecap="round" />
      <path d="M8.2 11.2 12 15l3.8-3.8" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M5.5 19.5h13" strokeLinecap="round" />
    </svg>
  );
}
