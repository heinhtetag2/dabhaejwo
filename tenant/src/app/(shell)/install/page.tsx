import { PlannedView } from "@/features/shared/ui/planned-view";

export default function Page() {
  return (
    <PlannedView
      title="설치"
      description="홈페이지에 챗봇을 붙인다"
      planRef="docs/prototype/chatbot-tenant-dashboard.html — 설치"
      contents={[
        "붙여넣을 코드 — </body> 바로 위. 공개 키(pk_live_...)가 들어간다",
        "이 키는 공개돼도 괜찮다. 아래에 등록한 주소에서만 작동하기 때문이다",
        "허용할 주소 — 등록된 Origin 에서만 위젯이 응답한다. 그 외에는 403",
        "어디에 붙일지 모르겠다면 — 카페24 / 아임웹 / 워드프레스 / 직접 만든 사이트",
        "최근 호출 로그 — 어느 페이지에서 몇 번 불렸는지",
      ]}
    />
  );
}
