import type { Language } from "@/shared/lib/language";
import { FaqAccordion } from "@/shared/ui/faq-accordion";

import { LANDING_TEXT_V2 } from "./landing-content-v2";
import { LandingDocsSection } from "./landing-docs-section";
import { LandingFlowSection } from "./landing-flow-section";
import { LandingFlywheelSection } from "./landing-flywheel-section";
import { LandingHeroV2 } from "./landing-hero-v2";
import { LandingStrengthsCarousel } from "./landing-strengths-carousel";

/**
 * 랜딩 리디자인 (v2) — Figma 참조(node 65:11820)를 픽셀 단위로 따랐다.
 *
 * <p>원본은 채널톡 홈페이지라 실제 고객 사례(회사명·인물·"AI 상담 해결률" %), 채널웍스·
 * AI CoS·규칙 엔진·마케팅·전화(Voice ALF)·인증 파트너처럼 우리에게 없는 제품, 브랜드명을
 * 입력하면 데모를 만들어 주는 기능을 담고 있다. 그대로 옮기면 없는 성과·제품이 있는
 * 척하는 셈이라(workflow-rules) 섹션 순서·시각 리듬(배지·큰 카드·탭·캐러셀)은
 * 그대로 두고 내용만 답해줘가 실제로 가진 것으로 바꿨다 — 절마다 정확히 무엇을
 * 바꿨는지는 각 섹션 컴포넌트의 코멘트에 있다:
 * `landing-hero-v2` · `landing-docs-section`(유일하게 원본과 컨셉이 그대로 겹친다) ·
 * `landing-strengths-carousel` · `landing-flywheel-section` · `landing-flow-section`
 * (별도 Figma 노드 119:1949, 채널톡 ALF·상담사 지표를 우리 파이프라인으로 바꿨다).
 *
 * <p>FAQ 섹션(플라이휠·흐름 섹션 바로 아래, 페이지 맨 끝 — 사용자 요청)은 Figma 참조에 없다 — 대신
 * `pricing-view-v2` 의 FAQ 아코디언(node 57:2)과 같은 디자인을 쓰기로 해(사용자 요청)
 * `shared/ui/faq-accordion`으로 뽑아 두 화면이 공유한다. 그래서 아래 타이포 표를
 * 따르지 않는다 — 표는 이 Figma 참조 안에서의 일관성이고, FAQ 는 다른 참조에서 온
 * 컴포넌트라 그 참조의 값을 그대로 가져왔다.
 *
 * <p>5개 섹션 중 2개(캐러셀·플라이휠)는 클릭 상호작용이 있어 각자
 * `"use client"` 섬(island)이다 — 나머지와 이 오케스트레이터 자체는 Server Component 로 남는다.
 *
 * <p><b>타이포 체계</b>(사용자 요청으로 섹션 간 일관성을 맞췄다 — Figma 각 노드의
 * 원값을 그대로 베끼면 노드마다 미묘하게 달라 화면 전체가 들쭉날쭉해진다. 아래 세
 * 계층으로 통일했고, 새 섹션을 추가할 때도 이 표를 따른다):
 *
 * <table>
 * <tr><td>계층</td><td>모바일→데스크톱</td><td>쓰는 곳</td></tr>
 * <tr><td>히어로 헤딩(H1)</td><td>36→52→64px</td><td>{@code landing-hero-v2} 만</td></tr>
 * <tr><td>히어로급 섹션 헤딩(H2)</td><td>32→48→64px</td><td>{@code landing-docs-section}
 *     — 원본이 이 섹션만 "두 번째 히어로"급으로 다뤘다</td></tr>
 * <tr><td>히어로급 서브타이틀</td><td>17→24px, semibold, black/60</td>
 *     <td>hero·docs 둘 다</td></tr>
 * <tr><td>일반 섹션 헤딩(H2)</td><td>28→36→44px, tracking -0.7→-0.88</td>
 *     <td>carousel·flywheel 둘 다 동일</td></tr>
 * <tr><td>일반 섹션 서브타이틀</td><td>16→18px, regular, tracking -0.18(sm~)</td>
 *     <td>carousel·flywheel 둘 다 동일(배경색에 맞는 흰/검만 다르다)</td></tr>
 * <tr><td>eyebrow(섹션 위 라벨)</td><td>16→18px, tracking -0.18</td>
 *     <td>flywheel — Figma 노드 원값이 16~22px 로 제각각이라 하나로 맞췄다</td></tr>
 * <tr><td>카드 헤딩(H3)</td><td>24→30px, tracking -0.7→-1</td>
 *     <td>docs 의 큰 카드·미니 카드, carousel 의 어두운 카드 — 셋 다 동일</td></tr>
 * <tr><td>카드 본문</td><td>16→18px, tracking -0.18(sm~)</td>
 *     <td>위와 같은 카드들의 본문 문단</td></tr>
 * </table>
 *
 * <p>모든 헤딩에 `text-black`(또는 어두운 배경이면 `text-white`)을 명시한다 — 비워두면
 * 전역 본문 색(`--color-ink`, 짙은 네이비)을 물려받아 검정이 아니라 남색으로 보인다
 * (사용자가 실제로 이 문제를 지적해서 발견됨).
 */
export function LandingViewV2({ language }: { language: Language }) {
  const t = LANDING_TEXT_V2[language];

  return (
    <>
      <LandingHeroV2 language={language} />
      <LandingDocsSection language={language} />
      <LandingStrengthsCarousel language={language} />
      <LandingFlywheelSection language={language} />
      <LandingFlowSection language={language} />
      <FaqAccordion heading={t.faqHeading} faqs={t.faqs} />
    </>
  );
}
