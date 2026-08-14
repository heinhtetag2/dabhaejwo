import type { Metadata } from "next";

import { LandingVersionSwitch } from "@/features/landing";
import { getLanguage } from "@/shared/lib/get-language";

export const metadata: Metadata = {
  title: "답해줘 — 홈페이지에 붙이는 챗봇",
  description:
    "스크립트 한 줄만 붙이면 우리 사이트를 학습한 챗봇이 방문자 질문에 답합니다. 14일 무료, 카드 등록 불필요.",
};

export default async function Page() {
  const language = await getLanguage();
  return <LandingVersionSwitch language={language} />;
}
