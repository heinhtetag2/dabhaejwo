import { LinkButton } from "@/shared/common/button";
import { ROUTES } from "@/shared/config/routes";
import type { Language } from "@/shared/lib/language";
import { PublicCard, PublicSection } from "@/shared/ui/public-section";
import { SignupCtaLink } from "@/shared/ui/signup-cta-link";

import { LANDING_TEXT } from "./landing-content";

/**
 * 서비스 안내.
 *
 * <p>목적은 설명이 아니라 가입이다 (tenant-public-plan.md §1.3).
 * 기능을 나열하지 않고, 이 제품의 가장 큰 강점인 "붙이기 쉽다"를 코드로 보여준다.
 *
 * <p>Server Component 다. 클라이언트 상태가 없다 — `language` 는 페이지에서
 * `getLanguage()` 로 읽어 props 로 내려받는다(header·footer·pricing 과 같은 방식).
 */
export function LandingView({ language }: { language: Language }) {
  const t = LANDING_TEXT[language];

  return (
    <>
      <Hero language={language} />

      <PublicSection tone="fill" eyebrow={t.stepsEyebrow} title={t.stepsTitle} description={t.stepsDescription}>
        <ol className="grid gap-4 sm:grid-cols-3">
          {t.steps.map((step, index) => (
            <PublicCard as="li" key={step.title} className="bg-card">
              <span
                aria-hidden
                className="grid size-8 place-items-center rounded-full bg-ink text-[13px] font-bold text-mark-bright"
              >
                {index + 1}
              </span>
              <h3 className="mt-5 text-[17px] font-bold tracking-[-0.02em]">{step.title}</h3>
              <p className="mt-2.5 text-[14.5px] leading-[1.7] text-slate">{step.body}</p>
            </PublicCard>
          ))}
        </ol>
      </PublicSection>

      <PublicSection eyebrow={t.benefitsEyebrow} title={t.benefitsTitle} description={t.benefitsDescription}>
        <div className="grid gap-4 sm:grid-cols-3">
          {t.benefits.map((benefit) => (
            <PublicCard key={benefit.title} className="bg-fill">
              <h3 className="text-[17px] font-bold tracking-[-0.02em] text-balance">
                {benefit.title}
              </h3>
              <p className="mt-2.5 text-[14.5px] leading-[1.7] text-slate">{benefit.body}</p>
            </PublicCard>
          ))}
        </div>
      </PublicSection>

      <PublicSection tone="fill" eyebrow={t.faqEyebrow} title={t.faqTitle}>
        <dl className="grid gap-3">
          {t.faqs.map((faq) => (
            <div key={faq.q} className="rounded-block bg-card px-6 py-6 sm:px-7">
              <dt className="text-[15.5px] font-bold tracking-[-0.02em]">{faq.q}</dt>
              <dd className="mt-2.5 text-[14.5px] leading-[1.75] text-slate">{faq.a}</dd>
            </div>
          ))}
        </dl>
      </PublicSection>

      <section className="mx-auto max-w-[1080px] px-5 pt-4 pb-24 sm:pb-28">
        <div className="rounded-panel bg-ink px-6 py-16 text-center sm:px-10 sm:py-20">
          <h2 className="text-[26px] leading-[1.35] font-bold tracking-[-0.035em] text-balance text-white sm:text-[34px]">
            {t.closingTitle}
          </h2>
          <p className="mt-4 text-[15.5px] leading-[1.7] text-slate-2">{t.closingSubtitle}</p>
          <SignupCtaLink size="lg" className="mt-8 border-mark bg-mark text-white hover:brightness-105">
            {t.closingCta}
          </SignupCtaLink>
        </div>
      </section>
    </>
  );
}

function Hero({ language }: { language: Language }) {
  const t = LANDING_TEXT[language];

  return (
    <section className="mx-auto max-w-[1080px] px-5 pt-16 pb-20 sm:pt-24 sm:pb-24 lg:pt-28">
      <div className="grid items-center gap-14 lg:grid-cols-[1.05fr_1fr] lg:gap-16">
        <div>
          <p className="inline-flex items-center gap-1.5 rounded-full bg-mark-soft px-3 py-1.5 text-[13px] font-semibold text-ink-2">
            {t.heroBadge}
          </p>

          <h1 className="mt-5 text-[36px] leading-[1.28] font-bold tracking-[-0.045em] text-balance sm:text-[46px] lg:text-[52px]">
            {t.heroTitle[0]}
            <br />
            {t.heroTitle[1]}
            <br />
            {t.heroTitle[2]}
          </h1>

          <p className="mt-6 max-w-[500px] text-[16px] leading-[1.75] text-slate sm:text-[17px]">
            {t.heroSubtitle}
          </p>

          <div className="mt-9 flex flex-wrap items-center gap-2.5">
            <SignupCtaLink variant="primary" size="lg">
              {t.heroCtaPrimary}
            </SignupCtaLink>
            <LinkButton href={ROUTES.pricing} size="lg" className="border-edge bg-fill hover:bg-line-2">
              {t.heroCtaSecondary}
            </LinkButton>
          </div>
          <p className="mt-4 text-[13.5px] text-slate-2">{t.heroCtaNote}</p>
        </div>

        <div className="rounded-panel bg-fill p-6 sm:p-7">
          <p className="text-[13px] font-semibold text-slate-2">{t.heroCodeLabel}</p>
          <pre className="mt-4 overflow-x-auto rounded-block bg-ink px-5 py-5 font-mono text-[12.5px] leading-[1.8] text-code">
{`<script>
  window.dabhaejwo = { key: "pk_live_..." };
</script>
<script src="https://cdn.dabhaejwo.com/w.js" async></script>`}
          </pre>
          <p className="mt-4 text-[13.5px] leading-relaxed text-slate">{t.heroCodeNote}</p>
        </div>
      </div>
    </section>
  );
}
