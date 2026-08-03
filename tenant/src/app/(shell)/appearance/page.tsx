import { PlannedView } from "@/features/shared/ui/planned-view";

export default function Page() {
  return (
    <PlannedView
      title="말투와 모양"
      description="챗봇의 성격과 겉모습을 정한다"
      planRef="docs/prototype/chatbot-tenant-dashboard.html — 말투와 모양"
      contents={[
        "모양 — 챗봇 이름, 버블 색상, 첫 인사말",
        "말투 — 어떤 태도로 답할지, 모를 때 할 말, 말하지 말 것",
        "답을 못 찾았을 때 — 연락처 남기기 제안 / 고객센터 안내 / 상담원 연결",
        "미리보기 — 바꾸는 즉시 오른쪽에서 확인할 수 있다",
      ]}
    />
  );
}
