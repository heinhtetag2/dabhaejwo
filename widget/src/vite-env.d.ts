/// <reference types="vite/client" />

/**
 * 빌드 시점에 박히는 값. `vite/client` 의 기본 선언은 인덱스 시그니처라 무엇이든 `any` 로
 * 통과시키므로, 우리가 쓰는 키만 여기에 명시해 오타를 컴파일 타임에 잡는다.
 */
interface ImportMetaEnv {
  /** 위젯이 물어볼 API 주소. 배포 환경마다 다르다 — `config.ts` 참조. */
  readonly VITE_API_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
