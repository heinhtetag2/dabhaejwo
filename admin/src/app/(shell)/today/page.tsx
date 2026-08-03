import { PlannedView } from "@/features/shared/ui/planned-view";

export default function Page() {
  return (
    <PlannedView
      title="오늘"
      description="로그인 직후 오늘 해야 할 일을 파악한다"
      planRef="docs/plan/admin-console-plan.md §4.1"
      contents={[
        "헤드라인 — 원가 초과 업체 수를 문장 하나로 크게 제시. 지표를 나열하면 무엇이 중요한지 사라진다",
        "지표 4종 — 유료 업체 수, 월 반복 매출, 오늘 총 대화, 오늘 모델 원가",
        "조치 목록 — 원가 초과·작업 실패·결제 실패·체험 종료·미응답 문의. 누르면 바로 그 대상으로 이동",
        "시스템 상태 — 응답 시간, 큐 대기, 워커 가동, 오류 건수",
      ]}
    />
  );
}
