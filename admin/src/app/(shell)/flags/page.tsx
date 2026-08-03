import { PlannedView } from "@/features/shared/ui/planned-view";

export default function Page() {
  return (
    <PlannedView
      title="기능 공개"
      description="새 기능을 일부 업체에만 먼저 열고 반응을 본다"
      planRef="docs/plan/admin-console-plan.md §4.8"
      contents={[
        "공개 대상 — 내부 테스트만 / 지정 업체 / 특정 요금제 / 전체 공개",
        "기능 플래그는 코드 배포와 기능 공개를 분리해준다",
        "문제가 생기면 배포를 되돌리지 않고 플래그만 끈다",
      ]}
    />
  );
}
