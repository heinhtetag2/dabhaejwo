import type { Metadata } from "next";

import { LegalPlaceholder } from "@/features/legal";

export const metadata: Metadata = { title: "개인정보처리방침 — 답해줘" };

export default function Page() {
  return (
    <LegalPlaceholder
      title="개인정보처리방침"
      summary={
        <>
          <p>정식 방침에는 다음이 담깁니다.</p>
          <ul className="mt-3 list-disc space-y-1.5 pl-5">
            <li>수집 항목과 목적 — 담당자 계정 정보, 방문자가 남긴 연락처</li>
            <li>보관 기간과 파기 절차. 해지 후 30일 유예를 두고 학습 데이터를 삭제합니다</li>
            <li>
              <b className="font-semibold text-ink">운영팀의 대리 접속</b> — 문의 처리와 문제
              재현을 위해 운영팀이 업체 대시보드에 접속할 수 있습니다. 접속에는 사유가
              필요하며, 시각과 사유는 대시보드의 팀원 화면에서 확인할 수 있습니다
            </li>
            <li>AI 공급사 등 처리 위탁 현황</li>
          </ul>
        </>
      }
    />
  );
}
