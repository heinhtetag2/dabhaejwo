import { ChevronRight } from "lucide-react";
import Image from "next/image";

import type { Language } from "@/shared/lib/language";
import { SignupCtaLink } from "@/shared/ui/signup-cta-link";

import { LANDING_TEXT_V2 } from "./landing-content-v2";

/**
 * 히어로 — Figma 참조(node 65:11822)의 타이포 스케일(64px 헤딩·24px 서브)과 여백을
 * 그대로 옮겼다. 큰 배너 이미지(`hero-banner.png`)는 우리 자체 대시보드·위젯 목업을
 * 담은 실제 디자인 산출물이라(Figma node 106:17228, 남의 제품 스크린샷이 아니다)
 * 그대로 내려받아 썼다 — 이전에는 이 자산이 없어 위젯 미리보기를 직접 그렸었다.
 */
export function LandingHeroV2({ language }: { language: Language }) {
  const t = LANDING_TEXT_V2[language];

  return (
    <section className="relative flex flex-col items-center overflow-x-clip px-5 pt-[80px] pb-[80px] sm:px-[68px] sm:pt-[120px]">
      <Image
        src="/landing/hero-decoration-left.png"
        alt=""
        aria-hidden
        width={630}
        height={1000}
        className="pointer-events-none absolute top-[220px] -left-32 z-0 hidden w-[300px] select-none sm:block lg:top-[260px] lg:-left-24 lg:w-[380px]"
      />

      <div className="relative z-10 flex flex-col items-center gap-6">
        <h1 className="text-center text-[36px] leading-[1.2] font-semibold tracking-[-1px] text-balance break-keep text-black sm:text-[52px] sm:leading-[1.15] lg:text-[64px] lg:leading-[88px] lg:tracking-[-2px]">
          <span className="block">{t.heroTitle[0]}</span>
          <span className="block">{t.heroTitle[1]}</span>
        </h1>
        <p className="max-w-[700px] text-center text-[17px] leading-[1.5] font-semibold text-black/60 sm:text-[24px] sm:leading-[34px] sm:tracking-[-0.5px]">
          {t.heroSubtitle}
        </p>
      </div>

      <SignupCtaLink
        variant="primary"
        className="relative z-10 mt-6 h-[48px] gap-0.5 rounded-full bg-black py-[10px] pr-[15px] pl-[22px] text-[18px] leading-[28px] font-bold tracking-[-0.18px] hover:bg-black/85 sm:mt-8"
      >
        {t.heroCta}
        <ChevronRight aria-hidden className="size-5" />
      </SignupCtaLink>

      <div className="relative mt-10 w-full sm:mt-[60px]">
        <Image
          src="/landing/hero-banner.png"
          alt={HERO_BANNER_ALT[language]}
          width={2640}
          height={1360}
          sizes="100vw"
          priority
          className="relative z-10 h-[280px] w-full rounded-[20px] object-cover object-top sm:h-auto sm:object-contain"
        />
      </div>
    </section>
  );
}

const HERO_BANNER_ALT: Record<Language, string> = {
  en: "dabhaejwo dashboard and chat widget preview",
  ko: "답해줘 대시보드와 챗봇 위젯 미리보기",
};
