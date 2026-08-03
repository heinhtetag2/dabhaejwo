// admin/shared/lib/format.ts 과 동일한 파일이다. 공유 패키지를 만들지 않기로 했으므로
// (kickoff-prompt.md §3) 복제해 두었다. 한쪽을 고치면 다른 쪽도 같은 커밋에서 맞춘다.

import dayjs from "dayjs";

const WON = new Intl.NumberFormat("ko-KR");

/** 금액. 원 단위 정수로 온다 (api-contracts.md §0-1). */
export function won(value: number): string {
  return `${WON.format(Math.round(value))}원`;
}

export function count(value: number): string {
  return WON.format(value);
}

/** `사용 / 한도` 형식. 한도가 사실상 무제한이면 그렇게 표시한다. */
export function quota(used: number, limit: number): string {
  return `${count(used)} / ${limit >= 999_999 ? "무제한" : count(limit)}`;
}

export function date(value: string | null | undefined): string {
  return value ? dayjs(value).format("YYYY-MM-DD") : "—";
}

export function dateTime(value: string | null | undefined): string {
  return value ? dayjs(value).format("YYYY-MM-DD HH:mm") : "—";
}

/** "3일 전" 같은 상대 표현. 마지막 접속처럼 정확한 시각이 중요하지 않은 곳에 쓴다. */
export function relative(value: string | null | undefined): string {
  if (!value) return "—";
  const target = dayjs(value);
  const diffMinutes = dayjs().diff(target, "minute");
  if (diffMinutes < 1) return "방금";
  if (diffMinutes < 60) return `${diffMinutes}분 전`;
  const diffHours = dayjs().diff(target, "hour");
  if (diffHours < 24) return `${diffHours}시간 전`;
  const diffDays = dayjs().diff(target, "day");
  if (diffDays < 30) return `${diffDays}일 전`;
  return target.format("YYYY-MM-DD");
}
