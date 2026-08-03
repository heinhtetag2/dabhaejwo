import { PlannedView } from "@/features/shared/ui/planned-view";

export default function Page() {
  return (
    <PlannedView
      title="작업 큐"
      description="크롤링·임베딩 작업의 실패를 발견하고 복구한다"
      planRef="docs/plan/admin-console-plan.md §4.5"
      contents={[
        "지표 4종 — 대기, 진행 중, 오늘 완료·성공률, 실패",
        "실패 목록 — 작업 종류, 대상 업체·파일, 오류 코드, 재시도 횟수, 시각",
        "액션 — 개별 재시도, 전체 재시도, 로그 내려받기, 업체 안내",
        "오류 코드는 원문 그대로 노출하되 옆에 한글 설명을 병기한다",
      ]}
    />
  );
}
