import type { Metadata } from "next";

import { LegalPlaceholder } from "@/features/legal";
import { getLanguage } from "@/shared/lib/get-language";
import type { Language } from "@/shared/lib/language";

export const metadata: Metadata = { title: "이용약관 — 답해줘" };

const SUMMARY_ITEMS: Record<Language, string[]> = {
  en: [
    "Scope of service and usage limits, and what happens when a limit is exceeded",
    "Billing, refunds, and cancellation procedures",
    "How material a company uploads may be used — limited to generating chatbot answers",
    "How we notify you of service interruptions or changes",
  ],
  ko: [
    "서비스 제공 범위와 이용 한도, 한도 초과 시 동작",
    "요금 결제와 환불, 해지 절차",
    "업체가 등록한 자료의 이용 범위 — 챗봇 답변 생성 목적에 한합니다",
    "서비스 중단·변경 시 고지 방법",
  ],
};

const INTRO_TEXT: Record<Language, string> = {
  en: "The official Terms of Service will cover the following.",
  ko: "정식 약관에는 다음이 담깁니다.",
};

const TITLE_TEXT: Record<Language, string> = {
  en: "Terms of Service",
  ko: "이용약관",
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
            {SUMMARY_ITEMS[language].map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </>
      }
    />
  );
}
