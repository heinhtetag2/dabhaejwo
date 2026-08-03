import { PlannedView } from "@/features/shared/ui/planned-view";

export default function Page() {
  return (
    <PlannedView
      title="지식 소스"
      description="챗봇이 참고할 내용을 관리한다"
      planRef="docs/prototype/chatbot-tenant-dashboard.html — 지식 소스"
      contents={[
        "웹페이지 — 사이트를 주기적으로 다시 읽는다. 페이지별 학습 상태와 제외 설정",
        "파일 — PDF·Word·엑셀 업로드. 스캔 문서는 글자 인식이 어려울 수 있다",
        "직접 입력 — 사이트에 없는 내용(운영시간, 이벤트 안내 등)",
        "대량 업로드는 임베딩 비용이 한꺼번에 발생하므로 건수 상한이 걸려 있다",
      ]}
    />
  );
}
