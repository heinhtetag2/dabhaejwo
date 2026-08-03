import { PlannedView } from "@/features/shared/ui/planned-view";

export default function Page() {
  return (
    <PlannedView
      title="요금제"
      description="지금 쓰는 요금제와 결제 내역"
      planRef="docs/prototype/chatbot-tenant-dashboard.html — 요금제 / tenant-plan.md §6.3"
      contents={[
        "현재 요금제, 다음 결제일, 이번 달 대화·학습 문서 사용량",
        "요금제 변경 · 결제 수단 · 세금계산서",
        "운영팀 접속 이력 — 언제, 왜 접속했는지 시각과 사유를 공개한다",
        "숨기는 편이 편하지만 공개하는 쪽이 신뢰에 유리하고 개인정보 처리방침 고지 의무에도 부합한다",
      ]}
    />
  );
}
