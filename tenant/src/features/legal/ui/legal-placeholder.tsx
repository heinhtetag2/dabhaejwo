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
    <div className="mx-auto max-w-[720px] px-5 pt-16 pb-24 sm:pt-24">
      <h1 className="text-[30px] leading-[1.3] font-bold tracking-[-0.04em] sm:text-[36px]">
        {title}
      </h1>

      <Notice tone="warn" size="md" className="mt-7">
        정식 문서를 준비하고 있습니다. 서비스 정식 공개 전에 이 자리에 게시되며, 게시 전에
        가입하신 분께는 변경 내용을 알려드립니다.
      </Notice>

      <div className="mt-10 rounded-panel bg-fill p-7 text-[15px] leading-[1.8] text-slate sm:p-8">
        {summary}
      </div>

      <p className="mt-10 text-[14px] text-slate-2">
        문의{" "}
        <a
          href={`mailto:${COMPANY.contactEmail}`}
          className="font-medium text-slate underline underline-offset-2 hover:text-ink"
        >
          {COMPANY.contactEmail}
        </a>
      </p>
    </div>
  );
}
