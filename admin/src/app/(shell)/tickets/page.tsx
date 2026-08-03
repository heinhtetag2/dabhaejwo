import { PlannedView } from "@/features/shared/ui/planned-view";

export default function Page() {
  return (
    <PlannedView
      title="문의"
      description="업체 문의를 놓치지 않고 응대한다"
      planRef="docs/plan/admin-console-plan.md §4.9"
      contents={[
        "업체, 문의 내용, 경과 시간, 상태, 열기",
        "경과 시간 기준 정렬 — 오래된 것이 위로 온다",
        "티켓에서 업체 상세로 바로 이동 — 문의 응대의 첫 단계는 항상 그 업체 상황 파악이다",
        "반대로 업체 상세에서도 그 업체의 과거 문의를 볼 수 있어야 한다",
      ]}
    />
  );
}
