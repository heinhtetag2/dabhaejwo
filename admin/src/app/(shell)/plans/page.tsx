import { PlannedView } from "@/features/shared/ui/planned-view";

export default function Page() {
  return (
    <PlannedView
      title="요금제"
      description="플랜을 정의하고 한도 초과 시 동작을 정한다"
      planRef="docs/plan/admin-console-plan.md §4.6"
      contents={[
        "요금제 표 — 이름, 월 요금, 대화 한도, 문서 한도, 사용 업체 수, 판매 여부",
        "요금제는 삭제하지 않고 판매 중단만 한다 — 기존 계약 업체가 남아 있다",
        "판매 중단된 구 요금제는 흐리게 유지하고 사용 업체 수를 표시한다",
        "한도 초과 시 동작 — 챗봇 중단(초기 권장) / 초과분 과금 / 알림만",
      ]}
    />
  );
}
