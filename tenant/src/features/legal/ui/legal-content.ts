import type { Language } from "@/shared/lib/language";

export const LEGAL_TEXT: Record<Language, { notice: string; contact: string }> = {
  en: {
    notice:
      "We're preparing the official document. It will be posted here before the service's public launch, and anyone already signed up will be notified of any changes before it goes live.",
    contact: "Contact",
  },
  ko: {
    notice:
      "정식 문서를 준비하고 있습니다. 서비스 정식 공개 전에 이 자리에 게시되며, 게시 전에 가입하신 분께는 변경 내용을 알려드립니다.",
    contact: "문의",
  },
};
