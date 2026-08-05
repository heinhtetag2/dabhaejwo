import Link from "next/link";

import { COMPANY, companyFacts } from "@/shared/config/company";
import { ROUTES } from "@/shared/config/routes";

/**
 * 공개 영역 푸터.
 *
 * <p>사업자 정보는 전자상거래법상 표기 의무다. 아직 값이 없으므로 <b>빈 항목은 아예 그리지 않는다</b> —
 * "—" 로 채우면 없는 정보가 있는 것처럼 보인다. 값이 채워지면 자동으로 나타난다.
 */
export function PublicFooter() {
  const facts = companyFacts();

  return (
    <footer className="bg-fill">
      <div className="mx-auto max-w-[1080px] px-5 py-16">
        <div className="grid gap-10 sm:grid-cols-[1.4fr_1fr_1fr]">
          <div>
            <p className="flex items-center gap-2.5 text-[16px] font-bold tracking-[-0.03em]">
              <span
                aria-hidden
                className="grid size-6.5 place-items-center rounded-[9px] bg-ink text-[13px] font-bold text-mark"
              >
                답
              </span>
              답해줘
            </p>
            <p className="mt-3.5 text-[14px] leading-relaxed text-slate">
              홈페이지에 스크립트 한 줄을 붙이면,
              <br />
              그 사이트를 학습한 챗봇이 방문자 질문에 답합니다.
            </p>
          </div>

          <nav aria-label="서비스" className="flex flex-col items-start gap-3">
            <p className="text-[13px] font-semibold text-slate-2">서비스</p>
            <FooterLink href={ROUTES.pricing}>요금제</FooterLink>
            <FooterLink href={ROUTES.signup}>무료로 시작하기</FooterLink>
            <FooterLink href={ROUTES.login}>로그인</FooterLink>
          </nav>

          <div className="flex flex-col items-start gap-3">
            <p className="text-[13px] font-semibold text-slate-2">문의와 약관</p>
            <a
              href={`mailto:${COMPANY.contactEmail}`}
              className="text-[14px] text-slate transition-colors hover:text-ink"
            >
              {COMPANY.contactEmail}
            </a>
            {COMPANY.contactPhone ? (
              <p className="text-[14px] text-slate">{COMPANY.contactPhone}</p>
            ) : null}
            <FooterLink href={ROUTES.terms}>이용약관</FooterLink>
            <FooterLink href={ROUTES.privacy}>개인정보처리방침</FooterLink>
          </div>
        </div>

        {facts.length > 0 ? (
          <dl className="mt-12 flex flex-wrap gap-x-6 gap-y-1.5 border-t border-edge pt-7 text-[12.5px] text-slate-2">
            {facts.map((fact) => (
              <span key={fact.label} className="flex gap-1.5">
                <dt>{fact.label}</dt>
                <dd>{fact.value}</dd>
              </span>
            ))}
          </dl>
        ) : null}
      </div>
    </footer>
  );
}

function FooterLink({ href, children }: { href: string; children: React.ReactNode }) {
  return (
    <Link href={href} className="text-[14px] text-slate transition-colors hover:text-ink">
      {children}
    </Link>
  );
}
