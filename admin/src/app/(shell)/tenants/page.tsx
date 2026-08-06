import { redirect } from "next/navigation";
import { Suspense } from "react";

import { TenantsView } from "@/features/tenants";
import { LoadingState } from "@/shared/common/states";
import { ROUTES } from "@/shared/config/routes";

/**
 * 업체 목록.
 *
 * 상세가 별도 페이지가 되기 전에는 이 화면이 `?tenantId=` 로 오른쪽 패널을 열었다.
 * 알림·감사 기록에 그 형태의 링크가 남아 있을 수 있어 상세로 넘겨준다 —
 * 링크를 깨뜨리면 "알림을 눌렀는데 목록 첫 화면이 뜬다"가 된다.
 */
export default async function Page({
  searchParams,
}: {
  searchParams: Promise<{ tenantId?: string }>;
}) {
  const { tenantId } = await searchParams;
  if (tenantId) {
    redirect(`${ROUTES.tenants}/${tenantId}`);
  }
  return (
    <Suspense fallback={<LoadingState label="업체를 불러오는 중" />}>
      <TenantsView />
    </Suspense>
  );
}
