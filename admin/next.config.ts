import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /**
   * 컨테이너 배포용 최소 산출물. `.next/standalone` 에 server.js 와 필요한 의존성만 남아
   * 런타임 이미지가 `node_modules` 전체를 들고 다니지 않는다.
   *
   * `public` 과 `.next/static` 은 standalone 에 자동으로 들어가지 않는다 —
   * Dockerfile 이 따로 복사한다. 빠뜨리면 화면은 뜨는데 CSS·JS 가 404 로 죽는다.
   */
  output: "standalone",
};

export default nextConfig;
