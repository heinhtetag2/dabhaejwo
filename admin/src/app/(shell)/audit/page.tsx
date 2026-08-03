import { PlannedView } from "@/features/shared/ui/planned-view";

export default function Page() {
  return (
    <PlannedView
      title="감사 기록"
      description="운영자가 고객 데이터에 접근한 이력 · 운영 관리자 전용"
      planRef="docs/plan/admin-console-plan.md §4.10"
      contents={[
        "시각, 운영자, 행위, 대상 업체, 사유",
        "기록은 수정·삭제할 수 없다 — DB 트리거로도 막혀 있다 (V1__init.sql)",
        "최소 3년 보존",
        "업체 대시보드에서도 자기 계정에 대한 접근 이력을 확인할 수 있게 한다",
      ]}
    />
  );
}
