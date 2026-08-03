import { Card, CardBody } from "@/shared/common/card";
import { PageHeader } from "@/widgets/page-header/page-header";

/**
 * 라우트와 셸은 살아 있지만 화면 구현이 아직 없는 곳에 쓴다.
 * 빈 화면을 두면 "고장 난 것"과 "아직 안 만든 것"을 구분할 수 없다.
 */
export function PlannedView({
  title,
  description,
  planRef,
  contents,
}: {
  title: string;
  description: string;
  planRef: string;
  contents: string[];
}) {
  return (
    <>
      <PageHeader title={title} description={description} />
      <Card>
        <CardBody>
          <p className="text-[13px] text-slate">
            이 화면은 아직 구현되지 않았습니다. 라우트와 셸만 준비되어 있습니다.
          </p>
          <ul className="mt-4 space-y-1.5 text-[13px] text-slate">
            {contents.map((item) => (
              <li key={item} className="flex gap-2">
                <span aria-hidden className="text-slate-2">
                  ·
                </span>
                {item}
              </li>
            ))}
          </ul>
          <p className="mt-4 border-t border-line-2 pt-3.5 font-mono text-[11.5px] text-slate-2">
            {planRef}
          </p>
        </CardBody>
      </Card>
    </>
  );
}
