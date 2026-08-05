# syntax=docker/dockerfile:1

# ── deps stage ──────────────────────────────────────────────────────
FROM node:22-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

# ── build stage ─────────────────────────────────────────────────────
FROM node:22-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .

# NEXT_PUBLIC_* 는 빌드 시점에 클라이언트 번들로 인라인된다 — 런타임 환경변수로는 못 바꾼다.
# 기본값을 주지 않는 이유: 빠뜨리면 localhost 를 가리킨 채 배포되고, 화면은 멀쩡히 뜨는데
# 모든 요청이 실패한다. 빌드가 실패하는 편이 낫다.
ARG NEXT_PUBLIC_API_BASE_URL
ENV NEXT_PUBLIC_API_BASE_URL=$NEXT_PUBLIC_API_BASE_URL
ENV NEXT_TELEMETRY_DISABLED=1
RUN npm run build

# ── runtime stage: standalone 서버 ──────────────────────────────────
FROM node:22-alpine AS runtime
WORKDIR /app
ENV NODE_ENV=production
ENV NEXT_TELEMETRY_DISABLED=1
ENV PORT=4311
ENV HOSTNAME=0.0.0.0

RUN addgroup -g 1001 -S nodejs && adduser -S nextjs -u 1001

# next.config.ts 의 output:'standalone' 산출물. public 과 .next/static 은 자동으로
# 들어가지 않아 따로 복사한다 — 빠뜨리면 화면은 뜨는데 CSS·JS 가 404 로 죽는다.
COPY --from=build /app/public ./public
COPY --from=build --chown=nextjs:nodejs /app/.next/standalone ./
COPY --from=build --chown=nextjs:nodejs /app/.next/static ./.next/static

USER nextjs
EXPOSE 4311
CMD ["node", "server.js"]
