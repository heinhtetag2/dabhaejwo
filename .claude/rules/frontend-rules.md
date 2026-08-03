---
version: 1.0
title: Frontend Rules — Next.js + TypeScript + Tailwind + FSD
---

# Frontend Rules — Next.js + TypeScript + Tailwind + FSD

> 프론트엔드 코드 작성 시 최우선 규칙. 작업 절차는 `frontend-engineer.md`,
> 스택 무관 운영 규칙은 core의 `HARNESS.md`·`rules/`를 따른다.

## 적용 조건

API 백엔드와 통신하는 웹 프론트엔드(SSR/SEO 포함). 화면·도메인이 여럿인 서비스.
단순 랜딩/정적 사이트엔 과하다 — 그 경우 별도 모듈 작성.

## 요약 (필수 규칙)

- React 18+, Next.js App Router, TypeScript **strict** 모드 (`any` 금지)
- Tailwind CSS **토큰 기반** 스타일링 — HEX 직접 사용 금지, 테마 토큰(CSS 변수)만 참조
- 컴포넌트 props는 TS `interface`로 명시
- 기본 Server Component, 상호작용 필요 시에만 `"use client"` — 경계는 잎(leaf) 쪽으로
- **디자인 JSX/HTML은 레퍼런스만** — 인라인 스타일을 복사하지 않는다 (동적 값 예외)

## 아키텍처 (FSD)

- 레이어: `app → views → widgets → features → entities → shared` (위 → 아래 단방향만)
- 역방향 import 금지. **같은 레이어 내 슬라이스 간 import 금지** (`features/a` → `features/b` 불가, 조합은 상위 레이어에서)
- 슬라이스 외부 공개는 `index.ts`로만. 깊은 경로 import 금지
- `app/`은 라우팅 전용. `page.tsx`는 얇게(import + return만) — 화면 조합은 `views/` 또는 feature view로
- features UI는 도메인 타입을 `@entities/...`에서 import — 직접 재정의 금지
- `shared/common`·`shared/ui` 기존 컴포넌트 확인 후 중복 구현 금지
- 배치 판단: 리소스 타입/통신 → entities, 사용자 행동(동사) → features, 화면 블록 → widgets, 도메인 무관 → shared. 애매하면 상위 레이어에 두고 두 번째 사용처가 생길 때 내린다. 선제적 shared 승격 금지

## 상태 관리

- 서버/비동기 상태: **TanStack Query** — Query key `[{리소스},'list',params]` / `[{리소스},'detail',id]`, mutation 성공 시 invalidate
- 클라이언트 전역 상태: **Zustand** (전역 UI 상태만 — 서버 데이터를 Zustand에 넣지 않는다. access token은 메모리에만, localStorage 금지)
- URL 상태 우선: `useSearchParams`

## 스타일 & 접근성

- Mobile-first 반응형 (`sm`·`md`·`lg`) + `dark:` 대응
- 조건부 클래스는 `cn()`, 테마 토큰 사용 (임의 값 남발 금지)
- 이미지 `alt` 필수, 대화형 요소는 시맨틱 엘리먼트 사용
- Status는 색만으로 구분 금지 — **텍스트 라벨 동반** (WCAG 2.1 AA)
- 인라인 스타일 금지 (동적 값 예외)
- 정렬 필요한 숫자·날짜는 `font-mono tabular-nums`

## 성능·리소스

- `next/image`·`next/font` 사용
- 대용량 라이브러리는 `next/dynamic`으로 지연 로드
- 로딩/에러/빈 상태 3종을 모든 목록·상세 화면에서 처리

## 데이터 / API 규약

- 모든 API 호출은 `shared/api`의 http-client 단일 인스턴스 경유 — 컴포넌트에서 fetch 직접 호출 금지
- 타입은 entities의 `types.ts`에서만 import. 컴포넌트 안 응답 타입 재정의 금지. 같은 리소스 타입 복제 금지 — 부분집합은 `Pick<>`
- 폼은 react-hook-form + zod. zod 스키마 키 = API 계약(JSON) 키
- Base URL 등 환경변수는 `shared/config/env.ts`에서 zod 검증 후 export. 라우트 경로는 `shared/config/routes.ts` 상수로만
- 백엔드 미연동 단계는 `shared/api`를 목데이터(`data/` JSON 반환) 스텁으로 시작, 실 API로 교체 가능하게 설계 (core stub 정책)

## 보안

- XSS 방지 (`dangerouslySetInnerHTML` 지양), 토큰은 메모리 저장
- 외부 입력은 zod로 검증
- 보호 라우트: `middleware.ts` 일괄 처리 + 화면단 role 가드 병행

## 컨벤션

- 파일명 kebab-case, 컴포넌트 PascalCase. 파일 250줄 초과 시 분리

## 검증 명령

| 대상 | 명령 |
|------|------|
| 빌드 | `npm run build` |
| 린트 | `npm run lint` |
| 테스트 | `npm run test` (핵심 로직·model·유틸은 Vitest 단위 테스트, E2E 필요 시 Playwright) |
