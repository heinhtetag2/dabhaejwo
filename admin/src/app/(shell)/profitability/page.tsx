import { PlannedView } from "@/features/shared/ui/planned-view";

export default function Page() {
  return (
    <PlannedView
      title="수익성"
      description="어느 업체가 돈을 벌어주고 어느 업체가 손해인지 판단한다"
      planRef="docs/plan/admin-console-plan.md §4.3"
      contents={[
        "기준 설명 — 70% 기준선의 의미를 문장으로 명시",
        "지표 4종 — 이번 달 매출, 모델 원가, 저장 답변 비중, 원가 초과 업체 수",
        "업체별 표 — 요금 / 모델 원가 / 원가율 막대 / 저장 답변 비율",
        "저장 답변 비율을 나란히 두는 이유 — 원가율이 높은 업체는 대부분 공통 질문이 0~2개다",
      ]}
    />
  );
}
