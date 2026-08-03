# 기획서 인테이크 — 답해줘 (dabhaejwo)

> 기획서: `docs/plan/admin-console-plan.md`, `docs/plan/tenant-plan.md` / 규약: `docs/kickoff-prompt.md`
> 시각 참조: `docs/prototype/*.html` (마크업·인라인 스타일 복사 금지)
> 작성일: 2026-08-03 · new-project 스킬 Phase 1

## 1. 한 줄 요약

업체가 자기 홈페이지에 스크립트 한 줄을 붙이면, 그 사이트의 내용을 학습한 챗봇이 방문자 질문에 답해주는 멀티 테넌트 SaaS.

---

## 2. 프로젝트 성격과 스택 결정

- **성격**: 풀스택 웹 (멀티 테넌트 SaaS) — API 서버 1 + 웹 콘솔 2 + 임베드 스크립트 1
- **선택 스택**: MariaDB 11.8 LTS + Spring Boot(api) + Next.js(admin·tenant) + Vite library(widget)

### 선택 이유

| 결정 | 이유 |
|---|---|
| **MariaDB 11.8 LTS** | 사용자 지정. 챗봇의 본질이 문서 검색이라 벡터 저장소가 반드시 필요한데, **MariaDB Vector 가 11.8 LTS 에서 GA** 되어 별도 벡터 DB 없이 한 DB에서 끝낼 수 있다. 별도 벡터 DB를 두면 테넌트 격리·백업·해지 시 삭제를 두 시스템에서 각각 보장해야 한다. `kickoff-prompt.md` §2.2 백엔드 규약과도 일치한다 |
| **Spring Boot** | 도메인 16개, 역할 7종, 상태머신 6개, 트랜잭션 경계가 분명한 관계형 서비스. `backend-spring-boot` 모듈 적용 조건에 정확히 부합 |
| **Next.js + FSD** | `kickoff-prompt.md` §1이 이 프로젝트의 FE 규약으로 지정. 화면 20개, 도메인 다수 → `frontend-rules` 모듈 적용 조건 부합 |
| **Vite library (widget)** | 남의 사이트에서 도는 스크립트. 프레임워크 런타임·라우터·폰트를 얹을 수 없다. 사이즈가 곧 제품 품질 |
| **모노레포 금지** | `kickoff-prompt.md` §3 고정. 루트에 4개 독립 프로젝트, 설정 개별 관리 |

### 사용한 프리셋

| 프로젝트 | 모듈 | 비고 |
|---|---|---|
| api | `backend-spring-boot.md` | 재사용. MySQL → **MariaDB 11.8** 로 치환 |
| admin · tenant | `frontend-rules.md` + `fsd-rules.md` | 재사용 |
| widget | `widget-embed-script.md` | **신규 작성** — 팩토리에 저장 (`_guide.md` 규칙 2) |

`admin-next-console.md`는 채택하지 않았다. 적용 조건이 "CRUD 게시판·설정 화면 중심"인데 운영 콘솔은 집계·대시보드 중심이라 `CrudPage` 패턴이 맞는 화면이 10개 중 2개뿐이고, "외부 UI 라이브러리 금지"·"localStorage 토큰"이 `kickoff-prompt.md` §1.3·§1.5(radix-ui, access token 메모리 전용)와 정면으로 충돌한다.

---

## 3. 액터 / 역할 매트릭스

인증 체계가 셋으로 분리된다. 토큰도 분리한다 (core security-rules "사용자/관리자 인증 체계가 분리되면 토큰/세션도 분리").

### 3-1. 운영자 (운영 콘솔) — `operators`

| 역할 | 인원 | 주요 권한 |
|---|---|---|
| `OPS_ADMIN` 운영 관리자 | 1~2 | 전체 |
| `CS` CS 담당 | 1~3 | 조회 + 대리 로그인 + 쿼터 증량 + 작업 큐 재시도 |
| `SALES` 영업 담당 | 1~2 | 조회 + 메모 + 체험 연장 + 요금제 변경 + 수익성 |
| `DEV` 개발 | 2~3 | 조회 + 대리 로그인 + AI 사용량 + 작업 큐 + 기능 공개 |

권한 키는 `{RESOURCE}_{ACTION}` (core security-rules). 예: `TENANT_READ`, `TENANT_IMPERSONATE`, `QUOTA_GRANT`, `MODEL_PRICE_WRITE`, `AUDIT_READ`.

모델 단가·비용 안전장치 수정은 `OPS_ADMIN` 전용 — 잘못 건드리면 전체 원가 계산과 서비스 가용성에 즉시 영향이 간다.

### 3-2. 업체 담당자 (업체 대시보드) — `tenant_members`

| 역할 | 권한 |
|---|---|
| `OWNER` 소유자 | 전체 + 결제 수단 + 팀원 관리 + 해지 |
| `EDITOR` 편집 | 지식 소스·공통 질문·말투·설치 설정 변경 |
| `VIEWER` 보기만 | 조회 전용 |

### 3-3. 방문자 (위젯)

익명. 인증 없음. `pk_live_*` 공개 키 + Origin 검증으로 테넌트를 식별한다. 연락처를 남기면 `leads` 에 기록되지만 계정은 만들지 않는다.

### 3-4. 대리 로그인 (교차 인증)

운영자가 업체 담당자 권한으로 업체 대시보드에 진입하는 유일한 경로. **별도 토큰 종류**로 발급하고 다음을 강제한다.

- 사유 필수 (공백만 입력도 거부), 진입 즉시 감사 기록 적재 (삭제·수정 불가)
- 30분 만료. 연장하려면 사유 재입력
- 파괴적 조작 차단 — 결제 수단 변경, 팀원 초대·삭제, 계정 해지, 문서 삭제
- 세션 중 업체 대시보드 상단에 배너 고정
- 접속 이력(시각·사유)을 업체에게 공개

관리자 화면 분리 필요 여부: **예.** 운영 콘솔은 별도 도메인 + 검색엔진 색인 차단 + IP allowlist.

---

## 4. 도메인 모델

### 4-1. 테넌트·과금

| 엔티티 | 설명 | 주요 필드 | 관계 |
|---|---|---|---|
| `Tenant` | 업체. 시스템상 격리 단위 | name, primaryDomain, publishableKey, status, trialEndsAt, currency | → Plan |
| `Plan` | 요금제 | name, monthlyFee, convLimit, docLimit, sellable | ← Tenant |
| `PlanModelAssignment` | 요금제별 답변 모델·조각 수 | planId, model, chunkCount | → Plan |
| `QuotaOverride` | 이번 달 한정 쿼터 증량 이력 | period, convDelta, docDelta, reason, operatorId | → Tenant |
| `TenantNote` | 내부 메모 (누적. 덮어쓰기 아님) | body, operatorId, createdAt | → Tenant |
| `TenantMember` | 업체 팀원 | email, role, lastSeenAt | → Tenant |
| `BillingRecord` | 결제 내역 | period, amount, status, receiptUrl | → Tenant |

### 4-2. 원가·사용량 (이 서비스의 심장)

| 엔티티 | 설명 | 주요 필드 |
|---|---|---|
| `AiUsage` | 모델 호출 원장. **모든 LLM 호출이 여기 남는다** | tenantId, purpose, provider, model, inputTokens, outputTokens, costKrw, createdAt |
| `TenantDailyUsage` | 일 단위 집계. 목록·대시보드 전용 | tenantId, day, convCount, savedCount, tokensIn/Out, costKrw |
| `ModelPrice` | 모델 단가 이력 | provider, model, inputPer1m, outputPer1m, effectiveFrom |
| `CostGuard` | 비용 안전장치 설정 | tenantDailyCapKrw, globalDailyCapKrw, ipQuestionsPerMin, bulkUploadLimit, costRatioWarnPercent |

`purpose`: `ANSWER` / `EMBED_DOC` / `EMBED_QUERY` / `ETC`. 용도를 나누지 않으면 같은 모델이 답변용인지 요약용인지 구분되지 않아 절감 지점을 찾을 수 없다.

**원가율 = 이번 달 모델 원가 ÷ 이번 달 청구액 × 100.** 경고선 70%, 손실선 100%. 파생 컬럼으로 저장하지 않고 집계에서 계산한다.

### 4-3. 지식·챗봇

| 엔티티 | 설명 | 주요 필드 |
|---|---|---|
| `KnowledgeSource` | 소스 단위 (사이트·업로드·직접입력) | type, origin, autoRefresh, lastCrawledAt |
| `KnowledgeDocument` | 개별 문서(페이지·파일·메모) | title, path, status, indexedAt, sizeBytes |
| `KnowledgeChunk` | 임베딩 조각 | documentId, ordinal, content, embedding `vector` |
| `Faq` | 공통 질문 = **저장 답변** | question, answer, links, shown, sortOrder, hitCount |
| `Conversation` | 대화 세션 | tenantId, startedPath, visitorRegion, startedAt |
| `Message` | 대화 메시지 | role, content, answered, sourceDocIds |
| `MessageFeedback` | 👍👎 | messageId, helpful |
| `Lead` | 남긴 연락처 | name, contact, reason, status |

**공통 질문은 모델을 거치지 않는다.** 저장된 답변이 그대로 나가고 `ai_usage`에 기록되지 않으며 대화 사용량에도 안 잡힌다. 원가율이 높은 업체가 대부분 공통 질문 0~2개인 것이 이 설계의 근거다.

### 4-4. 운영

| 엔티티 | 설명 |
|---|---|
| `Operator` | 운영자 계정 (SSO 주체) |
| `AuditLog` | 고객 데이터 접근 이력. **수정·삭제 불가, 3년 보존** |
| `ImpersonationSession` | 대리 로그인 세션. 30분 만료 |
| `Job` | 크롤링·임베딩 작업 큐 |
| `FeatureFlag` | 기능 공개 범위 |
| `Ticket` | 업체 문의 |
| `AllowedOrigin` | 위젯 허용 도메인 |

### 4-5. 상태머신

```
Tenant:        TRIAL → ACTIVE → SUSPENDED → ACTIVE
               TRIAL/ACTIVE/SUSPENDED → CHURNED   (해지는 종착. 복귀는 신규 계약)

Job:           QUEUED → RUNNING → DONE
                                → FAILED → QUEUED  (재시도. attempts < maxAttempts)

KnowledgeDocument:
               PENDING → PROCESSING → INDEXED
                                    → FAILED → PENDING  (재학습)
               * → EXCLUDED → PENDING              (소스에서 제외/복귀)

ImpersonationSession:
               ACTIVE → ENDED        (운영자가 종료)
                      → EXPIRED      (30분 경과)
                      → REVOKED      (대상 업체가 해지됨 — 세션 즉시 종료)

Ticket:        OPEN → ANSWERED → CLOSED
Lead:          NEW → CONTACTED → CLOSED
```

허용 전이는 코드로 먼저 정의하고 entity 메서드 안에서 검증한다 (core workflow-rules). "일단 값만 바꾸기" 금지.

---

## 5. 인터페이스 목록

### 5-1. 운영 콘솔 (`admin/`) — 별도 도메인, `noindex`

| 경로 | 화면 | 접근 권한 |
|---|---|---|
| `/today` | 오늘 — 조치가 필요한 항목만 | 전원 |
| `/tenants` | 업체 목록 + 상세 (좌우 분할) | 전원 |
| `/profitability` | 수익성 | `OPS_ADMIN`, `SALES` |
| `/ai-usage` | AI 사용량 | `OPS_ADMIN`, `DEV` |
| `/jobs` | 작업 큐 | `OPS_ADMIN`, `CS`, `DEV` |
| `/plans` | 요금제 | `OPS_ADMIN`, `SALES` |
| `/models` | 모델과 프롬프트 | `OPS_ADMIN` |
| `/flags` | 기능 공개 | `OPS_ADMIN`, `DEV` |
| `/tickets` | 문의 | 전원 |
| `/audit` | 감사 기록 | `OPS_ADMIN` |

목록과 상세는 **한 화면 좌우 분할**. 페이지를 이동하면 필터·스크롤이 초기화되어 여러 업체를 연속 확인하는 CS 흐름이 끊긴다.

### 5-2. 업체 대시보드 (`tenant/`)

| 경로 | 화면 | 권한 |
|---|---|---|
| `/` | 홈 | 전원 |
| `/improve` | 답변 개선 — 답변 실패·👎 질문에 답 달기 | `OWNER`, `EDITOR` |
| `/conversations` | 대화 로그 | 전원 |
| `/leads` | 남긴 연락처 | 전원 |
| `/sources` | 지식 소스 (웹페이지·파일·직접입력) | `OWNER`, `EDITOR` |
| `/faq` | 공통 질문 | `OWNER`, `EDITOR` |
| `/appearance` | 말투와 모양 | `OWNER`, `EDITOR` |
| `/install` | 설치 | `OWNER`, `EDITOR` |
| `/plan` | 요금제·결제 | `OWNER` |
| `/team` | 팀원 | `OWNER` |

`/plan` 에 **운영팀 접속 이력**(시각·사유)을 노출한다. 대리 접속 중에는 전 화면 상단에 배너 고정.

### 5-3. 위젯 (`widget/`)

| 산출물 | 설명 |
|---|---|
| `w.js` | 로더. `window.dabhaejwo.key` 를 읽어 host element 생성 → Shadow DOM 부착 → 마운트 |
| 버블 / 넛지 / 패널 | 자동 말걸기, 저장 답변 태그, 출처 링크, 👍👎, 연락처 폼 |

### 5-4. API 표면 (`api/`)

인증 주체별로 prefix를 나눈다 — 권한 경계를 URL에서부터 분명히 한다.

| Prefix | 인증 | 용도 |
|---|---|---|
| `/api/ops/**` | 운영자 JWT | 운영 콘솔 |
| `/api/app/**` | 업체 담당자 JWT (또는 대리 로그인 토큰) | 업체 대시보드 |
| `/api/widget/**` | `pk_live_*` + Origin 검증 | 방문자 위젯 |
| `/api/auth/**` | 공개 | 로그인·토큰 갱신 |

계약 상세는 `docs/architecture/api-contracts.md`.

---

## 6. 외부 의존성 (전부 stub 정책 대상)

core workflow-rules: 인터페이스를 정의하고 로컬/더미 구현을 stub으로 둔다. `// TODO(stub):` 주석 필수. 조용히 성공 처리 금지.

| 항목 | 용도 | stub 방식 |
|---|---|---|
| **LLM 공급사** (Gemini·Claude·OpenAI) | 답변 생성, 임베딩 | `LlmProvider` 인터페이스 + `StubLlmProvider` — 고정 응답 + 결정적 가짜 임베딩 + 토큰 수 추정. **`ai_usage` 적재는 stub에서도 동작**시켜 원가 파이프라인을 처음부터 검증 |
| **PG 결제** | 월 구독 청구 | `PaymentGateway` 인터페이스 + 로그만 남기는 stub. 결제 실패 시나리오를 수동 트리거 가능하게 |
| **슬랙 알림** | `#ops-alert` 상한 도달·오류 급증 | `Notifier` 인터페이스 + 로그 stub |
| **이메일** | 한도 안내, 초대, 결제 실패 | `Mailer` 인터페이스 + 로그 stub |
| **파일 저장소** (S3 호환) | 업로드 문서 원본 | `FileStorage` 인터페이스 + 로컬 디스크 구현 |
| **SSO + 2FA** | 운영 콘솔 인증 | 로컬 계정 + TOTP 자리만. 실 IdP 연동은 이후 |
| **웹 크롤러** | 사이트 재크롤링 | 실제 구현하되 robots.txt 준수·동시성 상한·타임아웃 필수. 초기엔 수동 트리거만 |
| **세금계산서** | 업체 요청 | stub. 문의 티켓으로 우회 |

---

## 7. Open Questions

| # | 질문 | 상태 | 결정 |
|---|---|---|---|
| 1 | 원가율 경고 기준 70% | 기본값 진행 | `cost_guards.cost_ratio_warn_percent = 70`. **설정값**으로 두어 실서버비 확인 후 조정. 하드코딩 금지 |
| 2 | 해지 업체 데이터 보존 | 기본값 진행 | 30일 유예 후 벡터·문서 삭제. 감사 기록과 결제 내역은 3년 보존 |
| 3 | 대리 로그인 이력 공개 범위 | 기본값 진행 | 시각 + 사유 전문 공개. 숨기는 편이 편하지만 공개가 신뢰에 유리하고 고지 의무에 부합 (tenant-plan §6.3) |
| 4 | 몽골 법인 통화 | 기본값 진행 | `tenants.currency` 기본 `KRW`. 스키마만 다통화 대비, UI는 KRW 고정 |
| 5 | 한도 초과 기본 동작 | 기본값 진행 | 챗봇 중단 + 안내 표시. 원가가 예측 가능 (admin-console-plan §4.6 "초기 권장") |
| 6 | 운영 콘솔 접근 IP | 기본값 진행 | IP allowlist 설정값. VPN 구축 여부는 인프라 결정 사항이라 코드 밖 |
| 7 | **답변 실패 판정 기준** | 기본값 진행 | 최근접 조각 유사도 `< 0.72` → 실패. `cost_guards` 와 같은 설정 테이블에 둔다 |
| 8 | **임베딩 모델 차원** | 기본값 진행 | `VECTOR(1536)`. 모델 교체 시 전체 재임베딩이 필요하므로 **되돌리기 어려운 결정** — CLAUDE.md 핵심 결정에 기록 |
| 13 | **테넌트 필터가 붙은 벡터 검색 성능** | 기본값 진행 | MariaDB 의 `VECTOR INDEX` 는 `WHERE` 없는 `ORDER BY ... LIMIT` 에서만 쓰인다. 테넌트 격리 때문에 필터가 항상 붙으므로 인덱스를 못 탈 수 있다. 정확성이 먼저이므로 필터를 유지하고, 성능이 문제가 되면 테넌트별 파티셔닝을 검토한다 (IMPROVEMENTS P1) |
| 9 | **위젯 격리 방식** | 기본값 진행 | Shadow DOM. iframe은 버블·넛지 오버레이와 반응형 크기 조절이 번거롭다. 호스트 CSS 충돌이 실제로 발생하면 패널만 iframe으로 전환 |
| 10 | **`purpose=ETC` 범위** | 기본값 진행 | 제목 요약·언어 감지만. 새 용도가 생기면 enum 추가 (기타로 뭉뚱그리면 절감 지점을 잃는다) |
| 11 | 프로토타입 단가표의 Claude 모델 | 미확인 | 실제 사용 공급사는 `model_prices` 로 결정. 시드는 Gemini + OpenAI 임베딩으로 두되 **운영 개시 전 실단가 확인 필요** |
| 12 | 상위 `e:\_tago_product\CLAUDE.md` 가 FinBridge 정책 | 미확인 | 이 프로젝트의 `CLAUDE.md` 가 더 구체적이라 우선하지만, 상위 파일이 MySQL·Ant Design을 "절대 규칙"으로 선언하고 있어 혼선 소지. 사용자 확인 필요 |

---

## 8. 이 프로젝트에서 절대 흔들리면 안 되는 것

기획서 전체를 관통하는 두 가지. 새 도메인을 추가할 때마다 이 둘을 확인한다.

1. **모든 LLM 호출은 `LlmGateway` 를 지난다.** 여기서만 `ai_usage` 를 적재하므로 원가 누락이 구조적으로 불가능해진다. Provider 를 직접 주입받아 호출하는 코드가 생기는 순간 원가 데이터에 구멍이 뚫리고, 그건 나중에 복구할 수 없다.
2. **테넌트 격리.** 모든 테넌트 소유 엔티티는 `tenant_id` 를 갖고, 조회는 항상 현재 테넌트 컨텍스트로 제한한다. 타 테넌트 데이터 접근은 P0.
