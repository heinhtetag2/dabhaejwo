import Link from "next/link";

import { COMPANY, companyFacts } from "@/shared/config/company";
import { ROUTES } from "@/shared/config/routes";
import { cn } from "@/shared/lib/cn";
import { getLanguage } from "@/shared/lib/get-language";
import type { Language } from "@/shared/lib/language";
import { LoginCtaLink } from "@/shared/ui/login-cta-link";
import { SignupCtaLink } from "@/shared/ui/signup-cta-link";

import { LanguageSwitch } from "./language-switch";

const TEXT: Record<
  Language,
  {
    tagline: [string, string];
    service: string;
    pricing: string;
    startFree: string;
    login: string;
    contact: string;
    terms: string;
    privacy: string;
  }
> = {
  en: {
    tagline: ["Add one line of script to your site,", "and a chatbot trained on it answers visitor questions."],
    service: "Product",
    pricing: "Pricing",
    startFree: "Start free",
    login: "Log in",
    contact: "Contact & legal",
    terms: "Terms of Service",
    privacy: "Privacy Policy",
  },
  ko: {
    tagline: ["홈페이지에 스크립트 한 줄을 붙이면,", "그 사이트를 학습한 챗봇이 방문자 질문에 답합니다."],
    service: "서비스",
    pricing: "요금제",
    startFree: "무료로 시작하기",
    login: "로그인",
    contact: "문의와 약관",
    terms: "이용약관",
    privacy: "개인정보처리방침",
  },
};

/**
 * 공개 영역 푸터 — 브랜드(로고+언어전환) / 서비스 / 문의와 약관 3열, 아래 저작권 바.
 *
 * <p>레이아웃·타이포 스케일은 Figma 참조(node 15:15507, 같은 프레임이 57:1278 로도 확인됨)를
 * 따랐다 — 원본은 실제 채널톡 사이트라 ALF·AI CoS·앱 다운로드·SNS 아이콘처럼 우리에게 없는
 * 제품·채널 링크와 채널코퍼레이션의 실제 사업자 정보(대표자·사업자등록번호)를 담고 있다.
 * 그대로 옮기면 없는 기능이 있는 척하고 남의 회사 정보를 붙이는 셈이라 <b>시각 스타일만</b>
 * 가져왔다 — 칼럼 폭·간격(160px + gap-16)·타이포 크기(16px 헤더/15.5px 링크)·하단 법적
 * 정보 바를 구분선으로 잇는 구조까지는 맞추되, 콘텐츠는 우리가 실제로 가진 것만
 * (docs/prototype: 마크업 복사 금지 규칙과 같은 이유). 컬럼 수를 5개로 채우지 않은 것도
 * 같은 이유 — 없는 카테고리를 만들어 칸을 채우지 않는다.
 *
 * <p>사업자 정보는 전자상거래법상 표기 의무다. 아직 값이 없으므로 <b>빈 항목은 아예 그리지 않는다</b> —
 * "—" 로 채우면 없는 정보가 있는 것처럼 보인다. 값이 채워지면 자동으로 나타난다.
 *
 * <p>언어 쿠키를 읽는다(`getLanguage`) — public-header.tsx 와 같은 범위 결정.
 */
export async function PublicFooter() {
  const facts = companyFacts();
  const language = await getLanguage();
  const text = TEXT[language];
  const year = new Date().getFullYear();

  return (
    <footer className="bg-fill">
      <div className="px-5 sm:px-[68px]">
        <div className="flex flex-col gap-10 pt-16 pb-8 sm:pb-10 md:flex-row md:gap-16">
          <div className="flex shrink-0 flex-col items-start gap-6 md:w-[200px]">
            <div>
              <p className="flex items-center gap-2.5 text-[18px] font-bold tracking-[-0.03em]">
                <span
                  aria-hidden
                  className="grid size-7 place-items-center rounded-[9px] bg-ink text-[14px] font-bold text-mark-bright"
                >
                  답
                </span>
                답해줘
              </p>
              <p className="mt-3.5 text-[14px] leading-relaxed text-slate">
                {text.tagline[0]}
                <br />
                {text.tagline[1]}
              </p>
            </div>
            <LanguageSwitch language={language} />
          </div>

          <div className="grid flex-1 grid-cols-2 gap-8 md:flex md:gap-16">
            <nav aria-label={text.service} className="flex flex-col items-start gap-3 md:w-[160px]">
              <p className="pb-1 text-[16px] font-semibold tracking-[-0.02em] text-slate-2">
                {text.service}
              </p>
              <FooterLink href={ROUTES.pricing}>{text.pricing}</FooterLink>
              <SignupCtaLink className="text-[15.5px] tracking-[-0.02em] text-slate transition-colors hover:text-ink">
                {text.startFree}
              </SignupCtaLink>
              <LoginCtaLink className="text-[15.5px] tracking-[-0.02em] text-slate transition-colors hover:text-ink">
                {text.login}
              </LoginCtaLink>
            </nav>

            <div className="flex flex-col items-start gap-3 md:w-[160px]">
              <p className="pb-1 text-[16px] font-semibold tracking-[-0.02em] text-slate-2">
                {text.contact}
              </p>
              <a
                href={`mailto:${COMPANY.contactEmail}`}
                className="text-[15.5px] tracking-[-0.02em] break-all text-slate transition-colors hover:text-ink"
              >
                {COMPANY.contactEmail}
              </a>
              {COMPANY.contactPhone ? (
                <p className="text-[15.5px] tracking-[-0.02em] text-slate">{COMPANY.contactPhone}</p>
              ) : null}
              <FooterLink href={ROUTES.terms}>{text.terms}</FooterLink>
              <FooterLink href={ROUTES.privacy}>{text.privacy}</FooterLink>
            </div>
          </div>
        </div>

        <div className="border-t border-edge py-6">
          <p className="text-[14px] text-slate-2">
            © {year} {COMPANY.name}
          </p>
          {facts.length > 0 ? (
            <dl className="mt-2 flex flex-wrap items-center text-[13px] text-slate-2">
              {facts.map((fact, index) => (
                <span
                  key={fact.label}
                  className={cn("flex gap-1.5 py-0.5", index > 0 && "ml-4 border-l border-edge pl-4")}
                >
                  <dt>{fact.label}</dt>
                  <dd>{fact.value}</dd>
                </span>
              ))}
            </dl>
          ) : null}
        </div>
      </div>
    </footer>
  );
}

function FooterLink({ href, children }: { href: string; children: React.ReactNode }) {
  return (
    <Link
      href={href}
      className="text-[15.5px] tracking-[-0.02em] text-slate transition-colors hover:text-ink"
    >
      {children}
    </Link>
  );
}
