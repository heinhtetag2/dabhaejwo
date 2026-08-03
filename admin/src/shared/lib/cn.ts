import clsx, { type ClassValue } from "clsx";

/** 조건부 클래스는 항상 이걸 쓴다. */
export function cn(...values: ClassValue[]): string {
  return clsx(values);
}
