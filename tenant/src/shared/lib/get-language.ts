import { cookies } from "next/headers";

import { LANGUAGE_COOKIE, parseLanguage, type Language } from "./language";

/** 현재 요청의 언어. Server Component 전용 — `cookies()` 가 요청 시점 API 다. */
export async function getLanguage(): Promise<Language> {
  const store = await cookies();
  return parseLanguage(store.get(LANGUAGE_COOKIE)?.value);
}
