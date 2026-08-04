import { COMPANY } from "@/shared/config/company";
import { Notice } from "@/shared/ui/notice";

/**
 * 약관·개인정보처리방침 자리.
 *
 * <p>본문을 채우지 않는다. <b>법무 검토 없이 쓴 약관은 없느니만 못하다</b>
 * (tenant-public-plan.md §4.4). 라우트와 링크만 만들어 두고 정식 공개 전에 채운다 —
 * docs/IMPROVEMENTS.md 에 P0 으로 등록되어 있다.
 *
 * <p>지금 상태를 숨기지 않고 그대로 알린다. 빈 페이지를 보여주면 방문자는 오류로 여긴다.
 */
export function LegalPlaceholder({
  title,
  summary,
}: {
  title: string;
  summary: React.ReactNode;
}) {
  return (
    <div className="mx-auto max-w-[720px] px-5 pt-14 pb-10">
      <h1 className="text-[26px] font-semibold tracking-[-0.03em]">{title}</h1>

      <Notice tone="warn" className="mt-5">
        정식 문서를 준비하고 있습니다. 서비스 정식 공개 전에 이 자리에 게시되며, 게시 전에
        가입하신 분께는 변경 내용을 알려드립니다.
      </Notice>

      <div className="mt-7 text-[13.5px] leading-relaxed text-slate">{summary}</div>

      <p className="mt-8 border-t border-line-2 pt-5 text-[12.5px] text-slate-2">
        문의{" "}
        <a href={`mailto:${COMPANY.contactEmail}`} className="underline">
          {COMPANY.contactEmail}
        </a>
      </p>
    </div>
  );
}
