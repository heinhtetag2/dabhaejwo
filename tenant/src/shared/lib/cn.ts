// admin/shared/lib/cn.ts 과 동일한 파일이다. 공유 패키지를 만들지 않기로 했으므로
// (kickoff-prompt.md §3) 복제해 두었다. 한쪽을 고치면 다른 쪽도 같은 커밋에서 맞춘다.

import clsx, { type ClassValue } from "clsx";

/** 조건부 클래스는 항상 이걸 쓴다. */
export function cn(...values: ClassValue[]): string {
  return clsx(values);
}
