# 답해줘 (dabhaejwo)

업체가 홈페이지에 스크립트 한 줄을 붙이면, 그 사이트를 학습한 챗봇이 방문자 질문에 답해주는 멀티 테넌트 SaaS.

- 프로젝트 성격: 풀스택 웹 (멀티 테넌트 SaaS) — API 서버 1 + 웹 콘솔 2 + 임베드 스크립트 1
- 스택: PostgreSQL(pgvector) + Spring Boot / Next.js(FSD) / Vite library — 선택 이유는 [docs/intake.md](docs/intake.md)

> ⚠️ 상위 `e:\_tago_product\CLAUDE.md` 는 **FinBridge 라는 다른 프로젝트**의 정책 파일이다.
> 이 프로젝트에는 적용되지 않는다 (그쪽은 MySQL·Ant Design·Maven). 이 파일이 dabhaejwo 의 기준이다.

## 구조

```
dabhaejwo/
├── CLAUDE.md · HARNESS.md · .env.example
├── .claude/rules/          ← 적용 규칙 (아래 규칙 섹션)
├── docs/
│   ├── intake.md           ← 기획서 분석·스택 결정 근거
│   ├── IMPROVEMENTS.md     ← 개선 백로그
│   ├── plan/               ← 기획서 (진실 공급원)
│   ├── prototype/          ← 시각 참조 전용. 마크업·인라인 스타일 복사 금지
│   └── architecture/
│       ├── api-contracts.md    ← 계약 단일 진실 공급원
│       └── llm-provider.md     ← 공급사 추상화 설계
├── api/        Spring Boot · Java 21 · Gradle KTS · PostgreSQL
├── admin/      Next.js — 운영 콘솔 (내부용)
├── tenant/     Next.js — 업체 대시보드
└── widget/     Vite library — 임베드 위젯
```

## 검증 명령

프로젝트 하나가 그린이 된 뒤 다음으로 넘어간다. 깨진 채로 마일스톤 완료 기록 금지.

| 대상 | 명령 |
|------|------|
| api | `./gradlew build` |
| admin | `npm run lint && npm run typecheck && npm run build` |
| tenant | `npm run lint && npm run typecheck && npm run build` |
| widget | `npm run typecheck && npm test && npm run build && npm run size` |

## 규칙 (모든 작업에 적용)

@.claude/rules/workflow-rules.md
@.claude/rules/security-rules.md
@.claude/rules/api-contract-rules.md
@.claude/rules/backend-spring-boot.md
@.claude/rules/frontend-rules.md
@.claude/rules/fsd-rules.md
@.claude/rules/widget-embed-script.md

운영 규칙(마일스톤, 세션 간 상태): @HARNESS.md

### 이 프로젝트에서 절대 흔들리면 안 되는 것

1. **모든 LLM 호출은 `global/llm/LlmGateway` 를 지난다.** Provider 를 직접 주입받아 호출하는 코드 금지. Gateway 에서만 `ai_usage` 를 적재하므로 원가 누락이 구조적으로 불가능해진다. 여기 구멍이 나면 나중에 복구할 수 없다 — 과거 원가는 다시 만들어낼 수 없기 때문이다.
2. **테넌트 격리.** 테넌트 소유 엔티티는 전부 `tenant_id` 를 갖고, 조회는 항상 현재 테넌트 컨텍스트로 제한한다. 타 테넌트 데이터 접근은 P0.
3. **감사 기록은 불변.** `audit_logs` 수정·삭제 금지, 3년 보존. 사유 필수 액션은 사유 없으면 서비스 레이어에서 거부한다.
4. **원가는 호출 시점 단가로 확정 저장.** `model_prices` 수정이 과거 `ai_usage` 에 소급되면 지난달 정산이 틀어지고 언제부터 적자였는지 추적할 수 없게 된다.

## 핵심 결정 (Decisions)

| 날짜 | 결정 | 사유 |
|---|---|---|
| 2026-08-03 | **PostgreSQL 단일 DB (pgvector 포함)** | 별도 벡터 DB를 두면 테넌트 격리·백업·해지 시 삭제를 두 시스템에서 각각 보장해야 한다. 기획서 DDL도 이미 PG 문법 |
| 2026-08-03 | **임베딩 차원 `vector(1536)`** | 모델 교체 시 전체 재임베딩이 필요 — 되돌리기 비쌈. 문서 24만 건 기준 재학습 비용 발생 |
| 2026-08-03 | **LLM 공급사 추상화, 단가는 DB** | 공급사 가격 변동·환율 대응. 코드에 모델명·단가 하드코딩 금지 |
| 2026-08-03 | **인증 3종 분리** (`/api/ops`, `/api/app`, `/api/widget`) | 운영자·업체 담당자·방문자는 신뢰 수준이 전혀 다르다. 토큰과 필터 체인을 나눈다 |
| 2026-08-03 | **위젯 Shadow DOM 격리** | 남의 사이트 CSS와 충돌 방지. iframe 은 버블·넛지 오버레이와 반응형 크기 조절이 번거로움. 충돌이 실제로 나면 패널만 iframe 전환 |
| 2026-08-03 | **모노레포 금지 — 4개 독립 프로젝트** | `docs/kickoff-prompt.md` §3 고정. 공유 상수·타입은 복제하되 출처 주석으로 동기화 지점 명시 |

## 마일스톤

<!-- 최신이 위. HARNESS.md 규칙대로 기록. -->

- **M0 부트스트랩 완료** (2026-08-03) — 4개 프로젝트 전부 검증 그린
  - 문서 정리·인테이크·하네스 조립·API 계약·DB 스키마(V1, 27개 테이블)
  - `api` — Spring Boot 4.1 / Java 21 / Gradle 9.5.1. `LlmGateway` + 원가 파이프라인,
    인증 3종 분리, `@RequirePermission` AOP, 감사 기록, 업체 목록 엔드포인트.
    **`./gradlew build` 그린** (단위 테스트 21개)
  - `admin` — 운영 콘솔 FSD 셸 + 10화면 라우트. **lint/typecheck/build 그린**
  - `tenant` — 업체 대시보드 FSD 셸 + 10화면 라우트 + 대리 접속 배너.
    **lint/typecheck/build 그린**
  - `widget` — Vite 8 IIFE + Preact + Shadow DOM. **typecheck/test(9)/build/size 그린**,
    gzip 8.86KB (예산 30KB)
  - 환경 제약 — Docker 없음(통합 테스트 불가, IMPROVEMENTS P1), Gradle 없음(Initializr 공식 래퍼 사용)
  - 다음 마일스톤 후보는 아래 "다음 마일스톤 제안" 참조

## 다음 마일스톤 제안

기획서의 개발 단계(admin-console-plan.md §11)는 **관측 → 대응 → 통제 → 확장** 순이다.
그 1단계가 "원가 데이터를 오픈과 동시에 쌓기 시작한다"이고, M0 에서 그 배관을 이미 깔았다.

| 후보 | 내용 | 이유 |
|---|---|---|
| **M1 — 챗봇이 실제로 답한다** | 지식 소스 → 임베딩 → 벡터 검색 → 답변 생성 → `ai_usage` 적재. 위젯 ↔ api 연결 | 제품의 본체이자, 원가 파이프라인이 실제 데이터로 도는지 확인하는 유일한 방법 |
| M1' — Docker 확보 후 통합 테스트 | Flyway·JPA 매핑·pgvector 검색 검증 | M0 에서 한 번도 실행되지 않은 부분이다. M1 착수 전에 하는 편이 싸다 |
| M2 — 오늘·수익성 화면 | 일 집계 배치 + 두 화면 실동작 | 원가 데이터가 쌓이기 시작하면 바로 볼 수 있어야 한다 |
| M3 — 대리 로그인 전 구간 | 세션 발급 → 배너 → 접속 이력 공개 → 감사 기록 | 골격은 있고 화면과 연결만 남았다 |

## 미완료 / Stub 목록

외부 의존성은 임의 구현하지 않는다. 인터페이스 + stub, `// TODO(stub):` 주석 필수, 조용한 성공 처리 금지.

| 항목 | 상태 | 필요 시점 |
|------|------|----------|
| LLM 공급사 (Gemini·Claude·OpenAI) | 인터페이스 + `StubLlmProvider`. **`ai_usage` 적재는 stub에서도 동작** | 실 응답 품질 검증 시 |
| PG 결제 | `PaymentGateway` 인터페이스 + 로그 stub | 유료 전환 전 |
| 슬랙 알림 `#ops-alert` | `Notifier` 인터페이스 + 로그 stub | 운영 개시 전 |
| 이메일 (한도 안내·초대·결제 실패) | `Mailer` 인터페이스 + 로그 stub | 운영 개시 전 |
| 파일 저장소 (S3 호환) | `FileStorage` 인터페이스 + 로컬 디스크 구현 | 문서 업로드 정식 오픈 전 |
| SSO + 2FA (운영 콘솔) | 로컬 계정 + TOTP 자리만 | 운영자 3명 초과 시 |
| 세금계산서 발행 | 미구현. 문의 티켓으로 우회 | 사업자 고객 유입 시 |
| 웹 크롤러 | 실 구현하되 초기엔 수동 트리거만. robots.txt 준수·동시성 상한 필수 | 자동 재크롤링 오픈 전 |
