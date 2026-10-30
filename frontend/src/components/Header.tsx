"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useStudio } from "./Providers";

export function Header() {
  const pathname = usePathname();
  const { users, userId } = useStudio();
  const templateCurrent = pathname === "/" || pathname.startsWith("/templates");
  const accountCurrent = pathname.startsWith("/account") || pathname.startsWith("/history");
  const current = users.find((user) => user.id === userId);

  return (
    <header className="topbar">
      <Link className="brand" href="/">
        <span className="mark" aria-hidden="true" />
        稿台
      </Link>
      <nav className="nav" aria-label="主要">
        <Link href="/" aria-current={templateCurrent ? "page" : undefined}>
          模板
        </Link>
        <Link href="/account" aria-current={accountCurrent ? "page" : undefined}>
          {current?.displayName ?? "我的"}
        </Link>
      </nav>
    </header>
  );
}
