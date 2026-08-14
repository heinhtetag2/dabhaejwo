"use client";

import { useRouter } from "next/navigation";
import { useEffect } from "react";

import { useAppContextQuery } from "@/entities/auth/session";
import { ErrorState, LoadingState } from "@/shared/common/states";
import { botRoute, type BotScreen } from "@/shared/config/routes";

/**
 * 서비스가 없던 시절의 경로를 현재 서비스로 보낸다.
 *
 * <p><b>이 화면이 없으면 이미 발행된 알림이 전부 깨진다.</b> `notifications.target_path` 에
 * `/app/leads` 같은 절대 경로가 DB 에 저장돼 있고, 그건 이제 고칠 수 없다.
 *
 * <p>`replace` 다 — `push` 면 뒤로가기가 리다이렉트 루프에 걸린다.
 *
 * <p>착지점은 기본 서비스다. 서비스가 하나뿐인 업체(대다수)에게는 아무것도 달라 보이지 않는다.
 */
export function LegacyRedirect({ screen }: { screen: BotScreen }) {
  const router = useRouter();
  const { data, isPending, isError, error, refetch } = useAppContextQuery();
  const bots = data?.bots;

  useEffect(() => {
    if (!bots || bots.length === 0) {
      return;
    }
    const target = bots.find((bot) => bot.defaultBot) ?? bots[0];
    router.replace(botRoute(target.id, screen));
  }, [bots, router, screen]);

  /*
   * **실패를 삼키지 않는다.** 처음에는 어느 경우든 로딩만 그렸는데, 그러면 컨텍스트 조회가
   * 실패했을 때 화면이 영원히 "불러오는 중"으로 남는다 — 실제로 서버가 옛 빌드일 때
   * 그렇게 됐고, 사용자는 무엇이 잘못됐는지 알 방법이 없었다.
   */
  if (isError) {
    return (
      <ErrorState
        message={
          error instanceof Error
            ? `계정 정보를 불러오지 못했습니다: ${error.message}`
            : "계정 정보를 불러오지 못했습니다"
        }
        onRetry={() => void refetch()}
      />
    );
  }

  // 조회는 끝났는데 서비스가 하나도 없다. 가입이 반쪽으로 끝난 상태이므로 조용히 넘기지 않는다.
  if (!isPending && (!bots || bots.length === 0)) {
    return <ErrorState message="이 계정에 서비스가 없습니다. 운영팀에 문의해 주세요" />;
  }

  return <LoadingState />;
}
