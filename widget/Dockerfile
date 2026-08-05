# syntax=docker/dockerfile:1
#
# 위젯은 **서버가 아니다.** 남의 사이트가 <script src> 로 불러가는 정적 파일 하나(w.js)라
# 컨테이너로 띄우지 않는다. 이 이미지는 산출물을 꺼내기 위한 **빌드 도구**다 —
# CI 가 이미지를 만든 뒤 `docker create` + `docker cp` 로 /dist 를 서버 디스크에 복사하고,
# 호스트 Apache 가 그 디렉터리를 그대로 서빙한다.
#
# 서버에 Node 를 깔지 않는 이유는 나머지 셋과 같다 — 배포에 필요한 것이 Docker 하나뿐이면
# 서버 상태가 재현 가능해진다.
#
#   docker build -t dabhaejwo-widget:latest ./widget
#   cid=$(docker create dabhaejwo-widget:latest)
#   docker cp "$cid:/dist/." /srv/dabhaejwo/widget/
#   docker rm "$cid"

# ── build stage ─────────────────────────────────────────────────────
FROM node:22-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

COPY . .

# 위젯이 물어볼 API 주소. 빌드 시점에 번들에 박힌다 — 호스트 사이트는 키만 적으면 되고
# 우리 도메인을 알 필요가 없다. 기본값을 주지 않는다: localhost 를 가리킨 채 배포되면
# 남의 사이트에서 챗봇이 조용히 죽는다.
ARG VITE_API_BASE_URL
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN test -n "$VITE_API_BASE_URL" || (echo "VITE_API_BASE_URL 이 필요합니다" && exit 1)

RUN npm run build
# 사이즈 예산(gzip 30KB) 초과면 여기서 배포가 멈춘다. 위젯은 남의 사이트 로딩에 얹히므로
# 크기가 곧 품질이다 — 조용히 커지는 것을 막는 유일한 지점이다.
RUN npm run size

# ── export stage: 산출물만 담는 껍데기 ───────────────────────────────
# scratch 가 아니라 alpine 인 이유 — `docker cp` 는 scratch 에서도 되지만,
# 문제가 생겼을 때 `docker run --rm -it ... sh` 로 안을 들여다볼 수가 없다.
FROM alpine:3 AS dist
COPY --from=build /app/dist /dist
