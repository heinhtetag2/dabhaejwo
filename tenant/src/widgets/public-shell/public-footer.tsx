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
    <footer className="mt-20 border-t border-line bg-card">
      <div className="mx-auto max-w-[1080px] px-5 py-10">
        <div className="flex flex-wrap gap-x-8 gap-y-4">
          <div className="min-w-[180px]">
            <p className="font-semibold tracking-[-0.01em]">{COMPANY.name}</p>
            <p className="mt-1.5 text-[12.5px] text-slate-2">
              홈페이지에 붙이는 챗봇
            </p>
          </div>

          <nav aria-label="약관" className="flex flex-col gap-1.5 text-[12.5px]">
            <Link href={ROUTES.pricing} className="text-slate hover:text-ink">
              요금제
            </Link>
            <Link href={ROUTES.terms} className="text-slate hover:text-ink">
              이용약관
            </Link>
            <Link href={ROUTES.privacy} className="text-slate hover:text-ink">
              개인정보처리방침
            </Link>
          </nav>

          <div className="text-[12.5px] text-slate">
            <p>문의</p>
            <a href={`mailto:${COMPANY.contactEmail}`} className="mt-1.5 block font-mono text-slate-2 hover:text-ink">
              {COMPANY.contactEmail}
            </a>
            {COMPANY.contactPhone ? (
              <p className="font-mono text-slate-2">{COMPANY.contactPhone}</p>
            ) : null}
          </div>
        </div>

        {facts.length > 0 ? (
          <dl className="mt-8 flex flex-wrap gap-x-5 gap-y-1 border-t border-line-2 pt-5 text-[11.5px] text-slate-2">
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
