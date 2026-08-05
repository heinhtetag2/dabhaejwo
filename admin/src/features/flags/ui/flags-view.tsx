"use client";

import * as Switch from "@radix-ui/react-switch";

import { useFeatureFlagListQuery, useUpdateFeatureFlag, type FlagScope } from "@/entities/flag";
import { Badge, type BadgeTone } from "@/shared/common/badge";
import { Card, CardBody, CardHeader, Eyebrow } from "@/shared/common/card";
import { EmptyState, ErrorState, LoadingState } from "@/shared/common/states";
import { Table, Td, Th, TitleCell } from "@/shared/common/table";
import { errorMessage } from "@/shared/lib/error-message";
import { PageHeader } from "@/widgets/page-header/page-header";

const SCOPE: Record<FlagScope, { text: string; tone: BadgeTone }> = {
  INTERNAL: { text: "내부 테스트만", tone: "idle" },
  TENANTS: { text: "지정 업체", tone: "info" },
  PLAN: { text: "요금제", tone: "info" },
  ALL: { text: "전체 공개", tone: "ok" },
};

export function FlagsView() {
  const { data, isPending, isError, error, refetch } = useFeatureFlagListQuery();
  const update = useUpdateFeatureFlag();

  return (
    <>
      <PageHeader title="기능 공개" description="새 기능을 일부 업체에만 먼저 연다" />

      <Card>
        <CardHeader title="기능 공개" aside={<Eyebrow>특정 업체에만 먼저 열기</Eyebrow>} />

        {isPending ? <LoadingState /> : null}
        {isError ? (
          <ErrorState message={errorMessage(error)} onRetry={() => void refetch()} />
        ) : null}
        {update.isError ? (
          <CardBody className="border-b border-line-2 py-3">
            <p className="text-[12.5px] text-brick">{errorMessage(update.error)}</p>
          </CardBody>
        ) : null}
        {data && data.length === 0 ? (
          <EmptyState message="등록된 기능 플래그가 없습니다" />
        ) : null}

        {data && data.length > 0 ? (
          <Table>
            <thead>
              <tr>
                <Th>기능</Th>
                <Th>공개 대상</Th>
                <Th className="w-[74px]">상태</Th>
              </tr>
            </thead>
            <tbody>
              {data.map((flag) => (
                <tr key={flag.key}>
                  <Td>
                    <TitleCell title={flag.name} sub={flag.description ?? flag.key} />
                  </Td>
                  <Td>
                    <Badge tone={SCOPE[flag.scope].tone} dot={false}>
                      {targetLabel(flag.scope, flag.targetTenantNames, flag.targetPlanName)}
                    </Badge>
                  </Td>
                  <Td>
                    {/* radix Switch 를 쓰는 이유는 프로토타입의 div+onclick 토글이
                        키보드로 조작되지 않기 때문이다 (IMPROVEMENTS 접근성 부채) */}
                    <Switch.Root
                      checked={flag.enabled}
                      disabled={update.isPending}
                      aria-label={`${flag.name} ${flag.enabled ? "끄기" : "켜기"}`}
                      onCheckedChange={(next) =>
                        update.mutate({
                          key: flag.key,
                          body: {
                            scope: flag.scope,
                            targetTenantIds: flag.targetTenantIds,
                            targetPlanId: flag.targetPlanId,
                            enabled: next,
                          },
                        })
                      }
                      className="relative h-[19px] w-[34px] rounded-full bg-line transition-colors data-[state=checked]:bg-seal"
                    >
                      <Switch.Thumb className="block size-[15px] translate-x-0.5 rounded-full bg-white shadow transition-transform data-[state=checked]:translate-x-[17px]" />
                    </Switch.Root>
                  </Td>
                </tr>
              ))}
            </tbody>
          </Table>
        ) : null}
      </Card>

      <Card className="mt-4">
        <CardBody className="text-[13px] leading-[1.7] text-slate">
          기능 플래그는 코드 배포와 기능 공개를 분리해 줍니다. 문제가 생기면 배포를 되돌리지 않고
          플래그만 끄면 됩니다.
        </CardBody>
      </Card>
    </>
  );
}

function targetLabel(scope: FlagScope, tenantNames: string[], planName: string | null): string {
  if (scope === "TENANTS") {
    if (tenantNames.length === 0) return "지정 업체 없음";
    const [first, ...rest] = tenantNames;
    return rest.length === 0 ? first : `${first} 외 ${rest.length}곳`;
  }
  if (scope === "PLAN") {
    return planName ? `${planName} 요금제` : "요금제 미지정";
  }
  return SCOPE[scope].text;
}
