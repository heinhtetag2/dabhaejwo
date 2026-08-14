import { RefreshCcw } from "lucide-react";

import type { Language } from "@/shared/lib/language";

import { LANDING_TEXT_V2 } from "./landing-content-v2";

/**
 * "쓸수록 저장 답변이 쌓이는 구조" — Figma 참조(node 119:1949)는 채널톡의 ALF·상담사
 * 지표(해결률 상승, 상담원 개입 비율 하락)를 담은 아이소메트릭 다이어그램이다.
 * 헤딩·서브타이틀·루프 캡션은 실제 파이프라인(AnswerService)으로 바꿨다: 방문자
 * 질문·학습한 문서·말투 설정이 AI로 들어가 답변 완료 또는 답변 개선으로 갈리고,
 * 담당자가 채운 답은 저장 답변으로 루프백된다.
 *
 * <p><b>영상(`visualvideo.mp4`)만은 예외다</b> — 사용자가 직접 준비해 두 번 확인 후
 * 그대로 쓰기로 결정했다. 영상 자체에 "Questions·Rules·Knowledge·Answers·Human
 * Support" 라벨이 박혀 있어(채널톡 원본과 같은 다이어그램의 애니메이션 버전),
 * 우리에게 없는 기능(Rules 엔진·Human Support 팀)이 있는 것처럼 보일 수 있다는 점을
 * 두 차례 명시하고 승인받았다 — 다른 stub 없는 기능과 달리 이건 사용자의 명시적
 * 선택이라 그대로 둔다.
 */
export function LandingFlowSection({ language }: { language: Language }) {
  const t = LANDING_TEXT_V2[language];

  return (
    <section className="bg-fill-2 px-5 py-[80px] sm:px-[68px]">
      <div className="flex flex-col gap-3">
        <p className="text-[16px] leading-[28px] tracking-[-0.18px] text-black sm:text-[18px]">{t.flowEyebrow}</p>
        <div className="flex flex-col items-start gap-6 lg:flex-row lg:items-end lg:justify-between">
          <h2 className="max-w-[780px] text-[28px] leading-[1.3] font-semibold tracking-[-0.7px] text-balance break-keep text-black sm:text-[36px] lg:text-[44px] lg:leading-[62px] lg:tracking-[-0.88px]">
            {t.flowTitle}
          </h2>
          <p className="max-w-[420px] text-[16px] leading-[1.6] text-black sm:text-[18px] sm:leading-[28px] sm:tracking-[-0.18px]">
            {t.flowSubtitle}
          </p>
        </div>
      </div>

      <div className="mt-[35px] overflow-hidden rounded-[35px] bg-fill-2">
        <video
          src="/landing/visualvideo.mp4"
          width={1280}
          height={720}
          autoPlay
          muted
          loop
          playsInline
          className="h-auto w-full"
        />
      </div>

      <div className="mt-6 flex items-center justify-center gap-2.5 text-center">
        <RefreshCcw aria-hidden className="size-4 shrink-0 text-ink-2" />
        <p className="max-w-[560px] text-[14px] leading-[1.6] tracking-[-0.02em] text-slate">{t.flowLoop}</p>
      </div>
    </section>
  );
}
