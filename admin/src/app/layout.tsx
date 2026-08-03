import type { Metadata } from "next";
import { Noto_Sans_KR, Noto_Sans_Mono } from "next/font/google";

import { Providers } from "./providers";
import "./globals.css";

// TODO(stub): kickoff-prompt.md §1.2 는 public/fonts 의 로컬 폰트를 쓰기로 했으나
// 폰트 파일이 아직 제공되지 않았다. 제공되면 next/font/local 로 교체한다.
const notoSansKr = Noto_Sans_KR({
  subsets: ["latin"],
  weight: ["300", "400", "500", "600", "700"],
  variable: "--font-noto-sans-kr",
  display: "swap",
});

const notoSansMono = Noto_Sans_Mono({
  subsets: ["latin"],
  weight: ["400", "500", "600"],
  variable: "--font-noto-sans-mono",
  display: "swap",
});

export const metadata: Metadata = {
  title: "답해줘 운영 콘솔",
  description: "내부 운영 도구",
  // 별도 도메인 + 검색엔진 색인 차단 (admin-console-plan.md §8)
  robots: { index: false, follow: false },
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko" className={`${notoSansKr.variable} ${notoSansMono.variable}`}>
      <body className="font-sans antialiased">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
