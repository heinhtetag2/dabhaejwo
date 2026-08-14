import { LegacyRedirect } from "@/features/bot/ui/legacy-redirect";

/**
 * 서비스가 없던 시절의 경로. 이미 발행된 알림이 여기를 가리키므로 지우지 않는다.
 */
export default function Page() {
  return <LegacyRedirect screen="improve" />;
}
