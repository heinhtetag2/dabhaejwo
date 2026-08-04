import type { Metadata } from "next";

import { LegalPlaceholder } from "@/features/legal";

export const metadata: Metadata = { title: "이용약관 — 답해줘" };

export default function Page() {
  return (
    <LegalPlaceholder
      title="이용약관"
      summary={
        <>
          <p>정식 약관에는 다음이 담깁니다.</p>
          <ul className="mt-3 list-disc space-y-1.5 pl-5">
            <li>서비스 제공 범위와 이용 한도, 한도 초과 시 동작</li>
            <li>요금 결제와 환불, 해지 절차</li>
            <li>업체가 등록한 자료의 이용 범위 — 챗봇 답변 생성 목적에 한합니다</li>
            <li>서비스 중단·변경 시 고지 방법</li>
          </ul>
        </>
      }
    />
  );
}
