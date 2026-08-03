import { defineConfig } from "vite";

/**
 * 로더는 `<script src>` 하나로 끝나야 한다. 청크가 쪼개지면 호스트 사이트가
 * 추가 파일을 받아야 하므로 IIFE 단일 파일로 낸다.
 *
 * JSX 설정(jsx / jsxImportSource)은 tsconfig.json 에만 둔다.
 * Vite 8 은 esbuild 대신 oxc 를 쓰므로 여기 `esbuild: {...}` 를 적으면
 * 조용히 무시된다 — 두 곳에 적어두면 어느 쪽이 유효한지 알 수 없어진다.
 */
export default defineConfig({
  build: {
    lib: {
      entry: "src/loader.tsx",
      name: "DabhaejwoWidget",
      formats: ["iife"],
      fileName: () => "w.js",
    },
    // IIFE 는 코드 분할 자체가 없으므로 inlineDynamicImports 를 따로 켜지 않는다.
    // CSS 는 ?inline 으로 문자열화해 Shadow Root 에 주입한다 — 별도 .css 를 내면
    // 호스트가 로드해 주지 않아 스타일이 통째로 빠진다.
    cssCodeSplit: false,
    target: "es2020",
  },
  test: {
    environment: "happy-dom",
    include: ["tests/**/*.test.ts"],
  },
});
