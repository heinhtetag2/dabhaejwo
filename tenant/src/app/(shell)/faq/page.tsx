import { PlannedView } from "@/features/shared/ui/planned-view";

export default function Page() {
  return (
    <PlannedView
      title="공통 질문"
      description="자주 오는 질문에 답을 저장해 둔다"
      planRef="docs/prototype/chatbot-tenant-dashboard.html — 공통 질문"
      contents={[
        "여기 등록한 답변은 AI가 새로 만들지 않고 저장된 그대로 나간다",
        "기다림 없이 바로 뜨고 대화 사용량에도 잡히지 않는다 — 모델 원가가 0이다",
        "노출을 꺼도 방문자가 비슷한 내용을 직접 입력하면 이 답변이 쓰인다",
        "미리보기 — 방문자가 채팅창을 열었을 때 실제로 보이는 모습",
      ]}
    />
  );
}
