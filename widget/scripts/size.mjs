import { gzipSync } from "node:zlib";
import { readFileSync, statSync } from "node:fs";

/**
 * 번들 크기 예산. 위젯은 사이즈가 곧 제품 품질이다 — 남의 사이트 로딩에
 * 얹히기 때문이다. 초과하면 빌드를 실패시켜 조용히 커지는 것을 막는다.
 *
 * 초과 시 대응 순서: ① 패널을 동적 임포트로 분리 → ② 의존성 제거 → ③ 기능 축소
 */
const BUDGET_GZIP_BYTES = 30 * 1024;
const BUNDLE = "dist/w.js";

let raw;
try {
  raw = readFileSync(BUNDLE);
} catch {
  console.error(`빌드 산출물이 없습니다: ${BUNDLE}. 먼저 npm run build 를 실행하세요.`);
  process.exit(1);
}

const gzipped = gzipSync(raw).length;
const rawKb = (statSync(BUNDLE).size / 1024).toFixed(1);
const gzipKb = (gzipped / 1024).toFixed(1);
const budgetKb = (BUDGET_GZIP_BYTES / 1024).toFixed(0);

console.log(`${BUNDLE}  raw ${rawKb}KB  gzip ${gzipKb}KB  (예산 ${budgetKb}KB)`);

if (gzipped > BUDGET_GZIP_BYTES) {
  console.error(`사이즈 예산 초과: ${gzipKb}KB > ${budgetKb}KB`);
  process.exit(1);
}
