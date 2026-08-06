import { Suspense } from "react";

import { TenantDetailView } from "@/features/tenants";
import { LoadingState } from "@/shared/common/states";

/**
 * 업체 상세.
 *
 * `?back=` 에 목록의 검색·필터·정렬이 담겨 온다. 돌아가기가 그 상태를 복원하므로
 * 여러 업체를 연속으로 확인하는 CS 흐름이 끊기지 않는다.
 */
export default async function Page({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return (
    <Suspense fallback={<LoadingState label="업체를 불러오는 중" />}>
      <TenantDetailView tenantId={id} />
    </Suspense>
  );
}
