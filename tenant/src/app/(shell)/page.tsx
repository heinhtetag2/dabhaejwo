import { PlannedView } from "@/features/shared/ui/planned-view";

export default function Page() {
  return (
    <PlannedView
      title="홈"
      description="오늘 챗봇이 어떻게 일했는지 한눈에 본다"
      planRef="docs/prototype/chatbot-tenant-dashboard.html — 홈"
      contents={[
        "오늘의 한 줄 — 들어온 질문 중 답하지 못한 건수를 문장으로 제시하고 답변 개선으로 보낸다",
        "지표 4종 — 오늘 대화, 답변 성공률, 남긴 연락처, 평균 응답 시간",
        "지식 상태 — 학습 완료 / 처리 중 / 실패 문서 수와 실패분 재학습",
        "많이 물어본 질문 최근 7일 — 공통 질문으로 등록할 후보다",
      ]}
    />
  );
}
