import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  /**
   * 컨테이너 배포용 최소 산출물. admin 과 같은 이유·같은 설정이다.
   * `public` 과 `.next/static` 은 Dockerfile 이 따로 복사한다.
   */
  output: "standalone",
};

export default nextConfig;
