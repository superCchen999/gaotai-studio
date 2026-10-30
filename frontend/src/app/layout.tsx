import type { Metadata } from "next";
import { Header } from "@/components/Header";
import { Providers } from "@/components/Providers";
import "./globals.css";

export const metadata: Metadata = {
  title: "稿台",
  description: "浏览模板、上传素材，并在后台异步制作图片。",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="zh-CN">
      <body>
        <a className="skip" href="#content">
          跳到主要内容
        </a>
        <Providers>
          <Header />
          <main id="content">{children}</main>
        </Providers>
      </body>
    </html>
  );
}
