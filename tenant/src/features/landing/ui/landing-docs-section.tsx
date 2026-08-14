import Image from "next/image";

import type { Language } from "@/shared/lib/language";

import { LANDING_TEXT_V2 } from "./landing-content-v2";

/**
 * 문서 학습(RAG) 하이라이트 — Figma 참조(node 65:11936)의 유일하게 실재하는 축이다.
 * 원본은 "도큐먼트" 제품 하나를 파는 섹션이지만, 우리는 문서 학습이 챗봇 파이프라인
 * 자체라 컨셉이 정확히 겹친다.
 *
 * <p>오른쪽 목업 이미지(`docs-mockup.png`)는 Figma 참조(node 111:7970)의 실제 위젯
 * 화면 디자인을 그대로 내려받아 썼다 — `hero-banner.png`·`flywheel-*.png` 와 같은
 * 근거다(우리 자체 디자인 산출물이라 워터마크 우려가 없다). 왼쪽 카피는 그대로 뒀다 —
 * 참조 노드의 "ALF" 라벨은 참조가 파는 채널톡 자체 기능명이라(workflow-rules: 없는
 * 기능을 있는 것처럼 담지 않는다) 우리 실제 카피(`docsCard`)로 대체했다.
 *
 * <p>미니 카드 두 개 밑의 그래픽은 직접 준비한 목업 이미지를 쓴다
 * (`docs-mini-1.png`·`docs-mini-2.png`, `public/landing/`) — Figma export를 그대로
 * 늘려 쓰면 저해상도라 흐려지므로, 카드 높이에 맞는 고해상도 원본을 직접 받는다.
 */
export function LandingDocsSection({ language }: { language: Language }) {
  const t = LANDING_TEXT_V2[language];

  return (
    <section className="bg-[#ebf4ff] px-5 py-[80px] sm:px-[68px]">
      <div className="mx-auto flex w-full max-w-[1440px] flex-col items-center">
        <div className="flex flex-col items-center gap-6 text-center">
          <span className="rounded-[24px] bg-[#5f98ff] px-4 py-2 text-[16px] leading-[22px] font-semibold tracking-[-0.4px] text-white">
            {t.docsBadge}
          </span>
          <h2 className="text-[32px] leading-[1.25] font-semibold tracking-[-1px] text-balance break-keep text-black sm:text-[48px] sm:leading-[1.15] lg:text-[64px] lg:leading-[88px] lg:tracking-[-2px]">
            <span className="block">{t.docsTitle[0]}</span>
            <span className="block">{t.docsTitle[1]}</span>
          </h2>
          <p className="max-w-[600px] text-[17px] leading-[1.5] font-semibold text-black/60 sm:text-[24px] sm:leading-[34px] sm:tracking-[-0.5px]">
            {t.docsSubtitle}
          </p>
        </div>

        <div className="mt-[60px] flex w-full flex-col overflow-hidden rounded-[20px] bg-gradient-to-br from-[#d5e7ff] to-[#c0d7ff] lg:h-[500px] lg:flex-row lg:items-stretch lg:gap-[34px]">
          <div className="flex w-full flex-col justify-center gap-3 p-[34px] lg:w-[440px] lg:shrink-0">
            <span className="bg-mark w-fit rounded-[20px] px-4 py-2 text-[16px] leading-[22px] font-semibold tracking-[-0.4px] text-white">
              {t.docsCard.badge}
            </span>
            <h3 className="text-[24px] leading-[1.25] font-semibold tracking-[-0.7px] text-balance break-keep sm:text-[30px] sm:leading-[42px] sm:tracking-[-1px]">
              <span className="block">{t.docsCard.title[0]}</span>
              <span className="block">{t.docsCard.title[1]}</span>
            </h3>
            <p className="text-[16px] leading-[1.6] sm:text-[18px] sm:leading-[28px] sm:tracking-[-0.18px]">{t.docsCard.body}</p>
          </div>
          <div className="relative h-[320px] w-full lg:h-auto lg:flex-1">
            <Image
              src="/landing/docs-mockup.png"
              alt={DOCS_MOCKUP_ALT[language]}
              fill
              sizes="(min-width: 1024px) 60vw, 100vw"
              className="object-cover"
            />
          </div>
        </div>

        <div className="mt-6 grid w-full gap-5 sm:grid-cols-2">
          {t.docsMiniCards.map((card, index) => (
            <div key={card.title} className="flex h-full flex-col overflow-hidden rounded-[20px] bg-[#b2d1ff]">
              <div className="p-[34px] pb-6">
                <h3 className="text-[24px] leading-[1.25] font-semibold tracking-[-0.7px] text-balance break-keep sm:text-[30px] sm:leading-[42px] sm:tracking-[-1px]">
                  {card.title}
                </h3>
                <p className="mt-3 min-h-[52px] text-[16px] leading-[1.6] sm:min-h-[56px] sm:text-[18px] sm:leading-[28px] sm:tracking-[-0.18px]">
                  {card.body}
                </p>
              </div>
              <div className="relative min-h-[220px] flex-1 sm:min-h-[346px]">
                <Image
                  src={DOCS_MINI_IMAGES[index]}
                  alt={DOCS_MINI_IMAGE_ALT[language][index]}
                  fill
                  sizes="(min-width: 640px) 45vw, 100vw"
                  className="object-cover"
                />
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

const DOCS_MOCKUP_ALT: Record<Language, string> = {
  en: "dabhaejwo widget answering a question with a cited source",
  ko: "출처를 인용해 답변하는 답해줘 위젯 화면",
};

/**
 * 사용자가 직접 준비해 `public/landing/`에 넣는 목업 이미지 — 파일이 없으면 Next.js가
 * 빌드/요청 시점에 404 를 낸다(조용히 깨지지 않는다).
 */
const DOCS_MINI_IMAGES = ["/landing/docs-mini-1.png", "/landing/docs-mini-2.png"];

const DOCS_MINI_IMAGE_ALT: Record<Language, [string, string]> = {
  en: ["The install script screen, showing the code to paste and a live connection check", "A missed question added to the answer-improvement list"],
  ko: ["붙여넣을 설치 코드와 연결 확인 상태를 보여주는 화면", "놓친 질문이 답변 개선 목록에 쌓이는 화면"],
};
