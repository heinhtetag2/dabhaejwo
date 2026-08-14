import { LegacyRedirect } from "@/features/bot/ui/legacy-redirect";

/**
 * 대시보드 진입점. 현재 서비스의 홈으로 보낸다.
 *
 * 서비스가 하나뿐인 업체에게는 주소창만 길어질 뿐 화면은 그대로다.
 */
export default function Page() {
  return <LegacyRedirect screen="" />;
}
