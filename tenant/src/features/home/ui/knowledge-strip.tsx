import type { KnowledgeStatus } from "@/entities/dashboard/home";

/** 막대 칸 수. 문서가 몇 개든 이 개수로 압축한다. */
const CELLS = 33;

/**
 * 지식 상태 막대.
 *
 * <p>문서 수백 개의 상태를 스크롤 없이 한눈에 보여주기 위한 장치다 (tenant-plan.md §4.1).
 * 빨간 칸이 보이면 실패한 문서가 있다는 뜻이다.
 *
 * <p>색만으로 구분하지 않는다 — 아래 범례에 상태명과 건수를 함께 적는다 (WCAG 2.1 AA).
 */
export function KnowledgeStrip({ knowledge }: { knowledge: KnowledgeStatus }) {
  const total = knowledge.documentCount;
  const failedCells = cellsFor(knowledge.failedCount, total);
  const processingCells = cellsFor(knowledge.processingCount, total);
  const indexedCells = Math.max(0, CELLS - failedCells - processingCells);

  const cells = [
    ...Array.from({ length: indexedCells }, () => "indexed" as const),
    ...Array.from({ length: processingCells }, () => "processing" as const),
    ...Array.from({ length: failedCells }, () => "failed" as const),
  ];

  return (
    <div>
      <div
        role="img"
        aria-label={`전체 ${total}개 중 학습 완료 ${knowledge.indexedCount}, 처리 중 ${knowledge.processingCount}, 실패 ${knowledge.failedCount}`}
        className="flex flex-wrap gap-[3px]"
      >
        {cells.map((kind, index) => (
          <i
            key={index}
            aria-hidden
            className={
              kind === "failed"
                ? "h-4 flex-1 rounded-[2px] bg-brick"
                : kind === "processing"
                  ? "h-4 flex-1 rounded-[2px] bg-mark"
                  : "h-4 flex-1 rounded-[2px] bg-seal"
            }
          />
        ))}
      </div>

      <ul className="mt-3 flex flex-wrap gap-x-4 gap-y-1 text-[11.5px] text-slate">
        <Legend color="bg-seal" label="학습 완료" count={knowledge.indexedCount} />
        <Legend color="bg-mark" label="처리 중" count={knowledge.processingCount} />
        <Legend color="bg-brick" label="실패" count={knowledge.failedCount} />
      </ul>
    </div>
  );
}

function Legend({ color, label, count }: { color: string; label: string; count: number }) {
  return (
    <li className="flex items-center gap-1.5">
      <i aria-hidden className={`size-2 rounded-[2px] ${color}`} />
      {label} <span className="tabular">{count.toLocaleString()}</span>
    </li>
  );
}

/**
 * 0 건이 아니면 최소 한 칸은 준다. 실패 1건이 반올림으로 사라지면
 * 이 막대의 존재 이유가 없어진다.
 */
function cellsFor(count: number, total: number): number {
  if (count <= 0 || total <= 0) {
    return 0;
  }
  return Math.max(1, Math.round((count / total) * CELLS));
}
