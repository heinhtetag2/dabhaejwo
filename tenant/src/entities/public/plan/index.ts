import { z } from "zod";

import { env } from "@/shared/config/env";

/**
 * 공개 요금제. 로그인 없이 읽는다.
 *
 * <p>서버 컴포넌트에서 직접 부르므로 TanStack Query 훅이 아니라 함수다 —
 * 검색 엔진이 빈 페이지를 보지 않으려면 서버에서 그려야 한다.
 */
export interface PublicPlan {
  id: string;
  code: string;
  name: string;
  monthlyFee: number;
  /** true 면 금액 대신 "문의"를 보여준다. */
  negotiable: boolean;
  convLimit: number;
  docLimit: number;
}

const publicPlanSchema = z.object({
  id: z.string(),
  code: z.string(),
  name: z.string(),
  monthlyFee: z.number(),
  negotiable: z.boolean(),
  convLimit: z.number(),
  docLimit: z.number(),
});

/**
 * @returns 조회에 실패하면 빈 배열. 호출부가 "문의" 안내로 대체한다 —
 *          <b>옛 가격을 캐시로 보여주지 않는다.</b> 틀린 가격은 그대로 분쟁이 된다
 *          (tenant-public-plan.md §8)
 */
export async function fetchPublicPlans(): Promise<PublicPlan[]> {
  try {
    const response = await fetch(new URL("/api/public/plans", env.apiBaseUrl), {
      // 요금제는 자주 바뀌지 않지만, 바꿨는데 한참 옛 값이 보이면 그게 더 나쁘다.
      next: { revalidate: 60 },
    });
    if (!response.ok) {
      // 이유 없이 빈 배열만 돌려주면 운영 환경(Vercel 등)에서 원인을 알 방법이 없다 —
      // 실제로 겪었다. 화면은 그대로 "문의" 안내로 대체하되, 로그에는 남긴다.
      console.error(`fetchPublicPlans: ${response.status} ${response.statusText} from ${env.apiBaseUrl}`);
      return [];
    }
    return z.array(publicPlanSchema).parse(await response.json());
  } catch (error) {
    console.error("fetchPublicPlans failed:", env.apiBaseUrl, error);
    return [];
  }
}
