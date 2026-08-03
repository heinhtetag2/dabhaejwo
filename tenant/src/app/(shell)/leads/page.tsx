import { PlannedView } from "@/features/shared/ui/planned-view";

export default function Page() {
  return (
    <PlannedView
      title="남긴 연락처"
      description="챗봇이 답하지 못했을 때 방문자가 남긴 연락처"
      planRef="docs/prototype/chatbot-tenant-dashboard.html — 남긴 연락처"
      contents={[
        "이름, 연락처, 남긴 이유, 시각, 처리 상태",
        "연락처는 화면에서 뒷자리를 가린다. 원문은 CSV 내보내기에서만 나간다",
        "CSV 내보내기 · 알림 설정",
      ]}
    />
  );
}
