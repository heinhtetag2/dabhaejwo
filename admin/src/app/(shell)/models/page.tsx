import { PlannedView } from "@/features/shared/ui/planned-view";

export default function Page() {
  return (
    <PlannedView
      title="모델과 프롬프트"
      description="원가의 기준값을 관리하고 비용 폭주를 막는다 · 운영 관리자 전용"
      planRef="docs/plan/admin-console-plan.md §4.7"
      contents={[
        "모델 단가 — 공급사별 100만 토큰당 입력·출력 단가. 수정은 새 행 추가이며 과거 원가는 소급되지 않는다",
        "요금제별 배정 — 답변 생성 모델, 조각 수, 예상 대화당 원가",
        "비용 안전장치 — 업체별·전체 일일 원가 상한, IP 분당 질문 수, 일괄 업로드 제한",
        "검색 설정 — 임베딩 모델, 조각 수 기본값, 답변 실패 판단 기준",
        "공통 프롬프트 — 전 업체 공통 규칙, 답변 최대 길이",
      ]}
    />
  );
}
