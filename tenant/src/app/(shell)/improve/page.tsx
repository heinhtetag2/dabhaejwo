import { PlannedView } from "@/features/shared/ui/planned-view";

export default function Page() {
  return (
    <PlannedView
      title="답변 개선"
      description="챗봇이 답하지 못한 질문에 답을 달아둔다"
      planRef="docs/prototype/chatbot-tenant-dashboard.html — 답변 개선"
      contents={[
        "답변 실패 또는 👎를 받은 질문 목록. 같은 질문이 몇 번 들어왔는지 함께 표시한다",
        "답을 등록하면 고정 답변으로 저장되고 다음 질문부터 바로 쓰인다",
        "이 화면이 원가 절감의 핵심이다 — 자주 오는 질문을 모델로 처리하지 않게 만든다",
      ]}
    />
  );
}
