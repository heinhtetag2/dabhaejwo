import { PlannedView } from "@/features/shared/ui/planned-view";

export default function Page() {
  return (
    <PlannedView
      title="팀원"
      description="이 챗봇을 함께 관리할 사람"
      planRef="docs/prototype/chatbot-tenant-dashboard.html — 팀원"
      contents={[
        "이름, 이메일, 권한(소유자 / 편집 / 보기만), 마지막 접속",
        "팀원 초대 — 수락 대기 상태를 함께 표시한다",
        "대리 접속 중에는 팀원 초대·삭제가 차단된다 (tenant-plan.md §6.2)",
      ]}
    />
  );
}
