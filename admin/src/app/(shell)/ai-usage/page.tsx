import { PlannedView } from "@/features/shared/ui/planned-view";

export default function Page() {
  return (
    <PlannedView
      title="AI 사용량"
      description="원가가 어디서 얼마나 발생하는지 분해해서 본다"
      planRef="docs/plan/admin-console-plan.md §4.4"
      contents={[
        "지표 4종 — 오늘 처리 토큰, 오늘 원가, 대화당 평균 원가, 이번 달 누적·예상",
        "14일 추이 — 용도별 누적 막대 (답변 생성 / 문서 학습 / 질문 벡터화)",
        "모델별 표 — 모델 × 용도별 호출 수, 입출력 토큰, 원가, 비중",
        "용도를 나누지 않으면 같은 모델이 답변용인지 요약용인지 구분되지 않아 절감 지점을 찾을 수 없다",
      ]}
    />
  );
}
