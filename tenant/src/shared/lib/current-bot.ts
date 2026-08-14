"use client";

import { useParams } from "next/navigation";
import { useEffect } from "react";

/**
 * 지금 보고 있는 서비스.
 *
 * <p><b>URL 이 유일한 진실이다.</b> 전역 상태로 두면 서비스 둘을 두 탭에 띄울 수 없다 —
 * 한쪽에서 바꾸면 다른 쪽이 다음 조회에서 조용히 따라 바뀐다. 그런데 여러 서비스를 나란히
 * 비교하는 것이 이 기능을 쓰는 사람이 하는 일 그 자체다.
 *
 * 화면 용어는 "서비스", 코드·API 는 `bot` 이다 — `docs/plan/service-plan.md` §3.
 */
export function useCurrentBotId(): string | null {
  const params = useParams<{ botId?: string }>();
  const value = params?.botId;
  return typeof value === "string" && value.length > 0 ? value : null;
}

/**
 * 서비스 범위 API 경로.
 *
 * 서버가 범위를 **경로**로 받는다 — 헤더면 빼먹을 수 있고, 빼먹으면 기본 서비스로
 * 조용히 떨어진다.
 */
export function botApi(botId: string | null, resource: string): string {
  if (!botId) {
    // 서비스 없이 서비스 데이터를 부르려 한 것이다. 배선 실수이므로 조용히 넘기지 않는다 —
    // 기본 서비스로 떨어뜨리면 화면이 엉뚱한 데이터를 자기 것으로 보여준다.
    throw new Error("서비스가 지정되지 않았습니다");
  }
  return `/api/app/bots/${botId}${resource}`;
}

/**
 * 서비스 범위 쿼리 키.
 *
 * <p><b>서비스를 키에 넣지 않으면 서비스를 바꿔도 옛 캐시가 그대로 보인다.</b>
 * 오류도 나지 않고, 사용자는 남의 서비스 대화 로그를 자기 것으로 읽는다.
 */
export function botKey(botId: string | null, ...rest: readonly unknown[]): readonly unknown[] {
  return ["bot", botId, ...rest];
}

/**
 * 마지막으로 보던 서비스.
 *
 * <p>모듈 변수다 — <b>탭마다 독립</b>이 자연히 보장된다. `localStorage` 는 탭이 공유하므로,
 * 탭 둘에 서로 다른 서비스를 띄워놓고 한쪽에서 계정 화면에 들르면 다른 쪽 링크까지 갈아탄다.
 * 새로고침하면 잊고 기본 서비스로 돌아간다 — 그 정도면 충분하다.
 */
let lastVisitedBotId: string | null = null;

/**
 * 사이드바 링크가 가리킬 서비스.
 *
 * <p>계정 화면(서비스·요금제·팀원)은 URL 에 서비스가 없다. 그때도 "홈"·"공통 질문" 같은
 * 링크는 <b>어딘가를 가리켜야 한다</b> — 안 그러면 죽은 링크가 되고 사용자는 메뉴가
 * 고장 난 줄 안다.
 *
 * <p>순서: URL → 마지막으로 보던 서비스 → 기본 서비스.
 *
 * <p><b>데이터 조회의 근거로는 절대 쓰지 않는다.</b> 그건 URL 만이 정한다 — 여기서 고른 값은
 * "링크가 어디를 가리키나"에만 쓰인다.
 */
export function useNavBotId(
  bots: ReadonlyArray<{ id: string; defaultBot: boolean }> | undefined,
): string | null {
  const botId = useCurrentBotId();

  // 기억은 렌더 밖에서 한다 — 렌더는 순수해야 한다.
  useEffect(() => {
    if (botId) {
      lastVisitedBotId = botId;
    }
  }, [botId]);

  if (botId) {
    return botId;
  }
  if (!bots || bots.length === 0) {
    return null;
  }
  // 기억한 서비스가 이미 지워졌을 수 있다. 목록에 있는 것만 믿는다.
  if (lastVisitedBotId && bots.some((bot) => bot.id === lastVisitedBotId)) {
    return lastVisitedBotId;
  }
  return (bots.find((bot) => bot.defaultBot) ?? bots[0]).id;
}
