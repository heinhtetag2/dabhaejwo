import { Suspense } from "react";

import { TenantsView } from "@/features/tenants";
import { LoadingState } from "@/shared/common/states";

/**
 * 선택된 업체를 `?tenantId=` 로 받는다 — 알림·감사 기록이 이 링크로 특정 업체를 가리킨다.
 * 검색 파라미터를 읽는 화면이라 Suspense 경계가 필요하다.
 */
export default function Page() {
  return (
    <Suspense fallback={<LoadingState label="업체를 불러오는 중" />}>
      <TenantsView />
    </Suspense>
  );
}
