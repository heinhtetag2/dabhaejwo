import type { Metadata } from "next";
import { Noto_Sans_KR, Noto_Sans_Mono } from "next/font/google";

import { Providers } from "./providers";
import "./globals.css";

// TODO(stub): kickoff-prompt.md §1.2 의 로컬 폰트가 아직 제공되지 않았다.
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
  title: "답해줘",
  description: "홈페이지에 붙이는 챗봇 콘솔",
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
