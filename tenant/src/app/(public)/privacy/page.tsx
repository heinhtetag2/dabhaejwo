import type { Metadata } from "next";
import type { ReactNode } from "react";

import { LegalPlaceholder } from "@/features/legal";
import { getLanguage } from "@/shared/lib/get-language";
import type { Language } from "@/shared/lib/language";

export const metadata: Metadata = { title: "개인정보처리방침 — 답해줘" };

const INTRO_TEXT: Record<Language, string> = {
  en: "The official Privacy Policy will cover the following.",
  ko: "정식 방침에는 다음이 담깁니다.",
};

const TITLE_TEXT: Record<Language, string> = {
  en: "Privacy Policy",
  ko: "개인정보처리방침",
};

const SUMMARY_ITEMS: Record<Language, ReactNode[]> = {
  en: [
    "What we collect and why — staff account information, and contact details visitors leave",
    "Retention period and deletion procedure. After cancellation, we keep a 30-day grace period before deleting training data",
    <>
      <b className="font-semibold text-ink">Staff impersonation access</b> — our team may access a
      company&rsquo;s dashboard to handle inquiries and reproduce issues. Access requires a reason, and
      the time and reason can be checked from the dashboard&rsquo;s team page
    </>,
    "Which third parties (e.g. AI providers) process data on our behalf",
  ],
  ko: [
    "수집 항목과 목적 — 담당자 계정 정보, 방문자가 남긴 연락처",
    "보관 기간과 파기 절차. 해지 후 30일 유예를 두고 학습 데이터를 삭제합니다",
    <>
      <b className="font-semibold text-ink">운영팀의 대리 접속</b> — 문의 처리와 문제 재현을 위해
      운영팀이 업체 대시보드에 접속할 수 있습니다. 접속에는 사유가 필요하며, 시각과 사유는 대시보드의
      팀원 화면에서 확인할 수 있습니다
    </>,
    "AI 공급사 등 처리 위탁 현황",
  ],
};

export default async function Page() {
  const language = await getLanguage();

  return (
    <LegalPlaceholder
      title={TITLE_TEXT[language]}
      language={language}
      summary={
        <>
          <p>{INTRO_TEXT[language]}</p>
          <ul className="mt-3 list-disc space-y-1.5 pl-5">
            {SUMMARY_ITEMS[language].map((item, index) => (
              <li key={index}>{item}</li>
            ))}
          </ul>
        </>
      }
    />
  );
}
