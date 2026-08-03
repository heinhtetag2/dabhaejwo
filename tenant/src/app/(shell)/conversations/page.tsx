import { PlannedView } from "@/features/shared/ui/planned-view";

export default function Page() {
  return (
    <PlannedView
      title="대화 로그"
      description="방문자와 챗봇이 주고받은 내용을 확인한다"
      planRef="docs/prototype/chatbot-tenant-dashboard.html — 대화 로그"
      contents={[
        "좌우 분할 — 대화 목록과 선택한 대화의 전문",
        "답변에 사용한 문서를 출처로 함께 보여준다",
        "답변 실패한 메시지에서 답변 개선으로 바로 이동한다",
        "운영팀이 대리 접속으로 이 화면을 열면 감사 기록에 VIEW_CONVERSATIONS 가 남는다",
      ]}
    />
  );
}
