# API Contracts — 답해줘 (dabhaejwo)

> BE entity/DTO와 FE 타입의 **단일 진실 공급원**. 계약 변경 없이 코드에서 키를 바꾸지 않는다.
> 계약 ↔ 코드가 어긋나면 **코드가 틀린 것**이다. 변경은 서버·클라이언트를 같은 커밋에서 맞춘다.
> 규칙: `.claude/rules/api-contract-rules.md`

## 0. 공통

### 0-1. 네이밍 (엄수)

- **boolean `is` 접두사 금지** — `verified`, `active`, `sellable`, `shown`, `answered`, `helpful`
- JSON 키 camelCase / DB 컬럼 snake_case
- ID 참조는 `{resource}Id`. 중첩 객체로 줄 때는 리소스명 그대로 (`plan: { id, name }`)
- 시각 `~At` (ISO-8601 UTC, `2026-08-03T05:41:00Z`) / 날짜 `~Date` (`2026-08-03`) / 월 `~Month` (`2026-08`)
- enum 값 UPPER_SNAKE_CASE 문자열
- 금액은 원 단위 정수(`monthlyFee: 89000`). 단 모델 원가만 소수 4자리 허용(`costKrw: 71200.1234`) — 토큰 단위 계산이라 반올림하면 누적 오차가 난다
- 비율은 `~Percent` 정수 또는 소수 1자리 (`costRatioPercent: 183`)

### 0-2. 페이지네이션

```json
{
  "content": [],
  "page": { "number": 0, "size": 20, "totalElements": 0, "totalPages": 0 }
}
```
size 기본 20, **최대 100**. 무제한 조회 금지.

### 0-3. 에러

```json
{ "code": "TENANT_NOT_FOUND", "message": "해당 업체를 찾을 수 없습니다" }
```

| code | HTTP | 의미 |
|---|---|---|
| `VALIDATION_FAILED` | 400 | 입력 검증 실패 |
| `REASON_REQUIRED` | 400 | 사유 필수 액션에 사유 누락 (공백만 입력 포함) |
| `UNAUTHENTICATED` | 401 | 토큰 없음·만료 |
| `IMPERSONATION_EXPIRED` | 401 | 대리 로그인 세션 30분 경과 |
| `PERMISSION_DENIED` | 403 | 권한 없음 |
| `IMPERSONATION_FORBIDDEN_ACTION` | 403 | 대리 로그인 중 금지된 파괴적 조작 |
| `ORIGIN_NOT_ALLOWED` | 403 | 위젯 호출 Origin 미등록 |
| `TENANT_NOT_FOUND` / `{RESOURCE}_NOT_FOUND` | 404 | 대상 없음 |
| `INVALID_STATE_TRANSITION` | 409 | 허용되지 않은 상태 전이 |
| `CONCURRENT_MODIFICATION` | 409 | 두 운영자가 같은 업체를 동시 수정 |
| `QUOTA_EXCEEDED` | 429 | 대화·문서 한도 초과 |
| `RATE_LIMITED` | 429 | IP 분당 질문 수 초과 |
| `COST_CAP_REACHED` | 503 | 일일 원가 상한 도달 — 챗봇 중단 |

### 0-4. 인증 (3종 분리)

| Prefix | 주체 | 헤더 |
|---|---|---|
| `/api/ops/**` | 운영자 | `Authorization: Bearer {opsAccessToken}` |
| `/api/app/**` | 업체 담당자 **또는 대리 로그인 세션** | `Authorization: Bearer {appAccessToken}` |
| `/api/widget/**` | 방문자(익명) | `X-Dabhaejwo-Key: pk_live_...` + `Origin` 검증 |
| `/api/auth/**` | 공개 | — |

```
POST /api/auth/ops/login      → { accessToken, refreshToken, operator: {...} }
POST /api/auth/app/login      → { accessToken, refreshToken, member: {...} }
POST /api/auth/refresh        → { accessToken }
```

access token 은 **메모리에만** 보관한다 (`kickoff-prompt.md` §1.3). localStorage 금지.

---

## 1. Operator (운영자)

```json
{
  "id": "8f3c...",
  "name": "정OO",
  "email": "ops@dabhaejwo.com",
  "role": "OPS_ADMIN",
  "active": true,
  "lastSeenAt": "2026-08-03T05:41:00Z"
}
```

`role`: `OPS_ADMIN` · `CS` · `SALES` · `DEV`

권한 키는 `{RESOURCE}_{ACTION}`. 역할→권한 매핑은 서버가 진실이며 `docs/intake.md` §3-1 매트릭스와 일치한다.

---

## 2. Tenant (업체) — 운영 콘솔의 중심

### 2-1. TenantSummary (목록)

```json
{
  "id": "a8f3...",
  "name": "노르드하임 가구",
  "primaryDomain": "nordheim.co.kr",
  "status": "ACTIVE",
  "plan": { "id": "p2...", "name": "비즈니스" },
  "convCount": 1142,
  "convLimit": 3000,
  "costKrw": 24100,
  "billedKrw": 89000,
  "costRatioPercent": 27
}
```

### 2-2. TenantDetail (상세) — Summary 의 상위집합

```json
{
  "id": "a8f3...",
  "name": "노르드하임 가구",
  "primaryDomain": "nordheim.co.kr",
  "publishableKey": "pk_live_a8f3k2m9x7q1",
  "status": "ACTIVE",
  "currency": "KRW",
  "plan": { "id": "p2...", "name": "비즈니스", "monthlyFee": 89000 },
  "convCount": 1142,
  "convLimit": 3000,
  "docCount": 248,
  "docLimit": 500,
  "faqCount": 6,
  "savedAnswerPercent": 39,
  "costKrw": 24100,
  "billedKrw": 89000,
  "costRatioPercent": 27,
  "joinedDate": "2026-01-08",
  "trialEndsAt": null,
  "nextBillingDate": "2026-08-28",
  "lastSeenAt": "2026-08-03T05:39:00Z"
}
```

겹치는 필드는 Summary 와 **이름·타입 완전 동일**. Summary 는 필드를 빼기만 한다.

- `faqCount` 가 0이면 프론트가 강조 표시한다 — 원가 급증의 선행 지표
- `costRatioPercent` = `costKrw / billedKrw * 100`. 파생 컬럼으로 저장하지 않는다

### 2-3. 상태 전이

```
TRIAL → ACTIVE | CHURNED
ACTIVE → SUSPENDED | CHURNED
SUSPENDED → ACTIVE | CHURNED
CHURNED → (종착)
```
무효 전이는 `INVALID_STATE_TRANSITION`.

### 2-4. 엔드포인트

| Method | Path | 권한 | 응답 |
|---|---|---|---|
| GET | `/api/ops/tenants` | `TENANT_READ` | 페이지네이션(TenantSummary) |
| GET | `/api/ops/tenants/{id}` | `TENANT_READ` | TenantDetail |
| GET | `/api/ops/tenants/filters` | `TENANT_READ` | 필터 칩별 건수 |
| PATCH | `/api/ops/tenants/{id}/status` | `TENANT_STATUS_WRITE` | TenantDetail |
| PATCH | `/api/ops/tenants/{id}/plan` | `TENANT_PLAN_WRITE` | TenantDetail |
| POST | `/api/ops/tenants/{id}/trial-extension` | `TENANT_TRIAL_WRITE` | TenantDetail |
| GET | `/api/ops/tenants/{id}/activities` | `TENANT_READ` | 페이지네이션(TenantActivity) |

`GET /api/ops/tenants` 쿼리 파라미터:

| 파라미터 | 값 | 기본 |
|---|---|---|
| `filter` | `ALL` `TRIAL` `PAYMENT_FAILED` `COST_EXCEEDED` `INACTIVE_7D` `SUSPENDED` `CHURNED` | `ALL` (해지 제외) |
| `q` | 업체명·도메인(서브도메인 포함)·공개 키·담당자 이메일 부분 일치 | — |
| `sort` | `COST_RATIO_DESC` `NAME_ASC` `CONV_DESC` | **`COST_RATIO_DESC`** |

기본 정렬이 원가율 내림차순인 이유는 운영자가 매일 가장 먼저 봐야 할 대상이 손실 계정이기 때문이다.

**사유 필수** — `status` 를 `SUSPENDED`/`CHURNED` 로 바꾸거나 `plan` 을 변경할 때 `reason` 이 없으면 `REASON_REQUIRED`.

---

## 3. 대리 로그인 (Impersonation)

```json
{
  "sessionId": "imp_9f2...",
  "tenant": { "id": "a8f3...", "name": "노르드하임 가구" },
  "reason": "문의 #482 — PDF 업로드 실패 재현",
  "accessToken": "...",
  "startedAt": "2026-08-03T05:41:00Z",
  "expiresAt": "2026-08-03T06:11:00Z",
  "status": "ACTIVE"
}
```

`status`: `ACTIVE` → `ENDED` | `EXPIRED` | `REVOKED`

| Method | Path | 권한 | 비고 |
|---|---|---|---|
| POST | `/api/ops/tenants/{id}/impersonate` | `TENANT_IMPERSONATE` | `reason` 필수. 공백만이면 `REASON_REQUIRED` |
| POST | `/api/ops/impersonations/{sessionId}/extend` | `TENANT_IMPERSONATE` | `reason` 재입력 필수 |
| DELETE | `/api/ops/impersonations/{sessionId}` | — | 세션 종료 |
| GET | `/api/app/impersonation/current` | 업체 담당자 | 진행 중이면 배너용 정보, 없으면 `null` |
| GET | `/api/app/impersonation/history` | 업체 담당자 | **업체가 보는 운영팀 접속 이력** (시각·사유) |

세션 토큰으로 다음을 호출하면 `IMPERSONATION_FORBIDDEN_ACTION`:
결제 수단 변경 · 팀원 초대/삭제 · 계정 해지 · 문서 삭제.

대상 업체가 `CHURNED` 가 되면 세션은 즉시 `REVOKED`.

---

## 4. QuotaOverride · TenantNote

```json
{ "id": 41, "periodMonth": "2026-08", "convDelta": 2000, "docDelta": 0,
  "reason": "체험 중 한도 초과 — 영업 요청", "operator": { "id": "...", "name": "정OO" },
  "createdAt": "2026-08-02T08:03:00Z" }
```
```json
{ "id": 88, "body": "PDF 1,800건 일괄 업로드로 원가 급증...",
  "operator": { "id": "...", "name": "정OO" }, "createdAt": "2026-07-29T02:10:00Z" }
```

| Method | Path | 권한 |
|---|---|---|
| POST | `/api/ops/tenants/{id}/quota-overrides` | `QUOTA_GRANT` (사유 필수) |
| GET | `/api/ops/tenants/{id}/notes` | `TENANT_READ` |
| POST | `/api/ops/tenants/{id}/notes` | `TENANT_NOTE_WRITE` |

**메모는 누적이다.** 수정·삭제 엔드포인트를 두지 않는다 — 영업 이력과 CS 맥락이 담기므로 덮어쓰면 안 된다.
쿼터 증량은 `periodMonth` 에만 적용되고 다음 달 자동 원복, 이력만 남는다.

---

## 5. 오늘 (TodaySummary)

```json
{
  "headline": { "tenantCount": 42, "costExceededCount": 3 },
  "stats": { "payingTenantCount": 42, "mrrKrw": 3182000,
             "todayConvCount": 18402, "todayCostKrw": 142700 },
  "actions": [
    { "type": "COST_EXCEEDED", "tenantId": "x92...", "title": "스튜디오 하우스",
      "detail": "스타터 39,000원 / 원가 71,200원", "targetPath": "/profitability" }
  ],
  "system": { "chatApiP95Ms": 1900, "embedQueueDepth": 412, "crawlerWorkers": "4/4",
              "vectorDbUsagePercent": 68, "todayError5xxCount": 14,
              "recentErrors": [ { "at": "2026-08-03T05:33:00Z", "tenantName": "한빛물산", "code": "pdf_parse_timeout" } ] }
}
```

`actions[].type`: `COST_EXCEEDED` · `JOB_FAILED` · `PAYMENT_FAILED` · `TRIAL_ENDING` · `TICKET_WAITING`

각 항목은 `targetPath` 로 **바로 그 대상**에 도달해야 한다. 이름만 확인하고 다시 검색하게 만들지 않는다.

`GET /api/ops/today` — 전 역할.

---

## 6. 수익성 · AI 사용량

```json
{ "tenant": { "id": "...", "name": "스튜디오 하우스" }, "planName": "스타터",
  "billedKrw": 39000, "costKrw": 71200, "costRatioPercent": 183, "savedAnswerPercent": 2 }
```
```json
{ "provider": "GOOGLE", "model": "gemini-2.5-flash", "purpose": "ANSWER",
  "callCount": 128402, "inputTokens": 41600000, "outputTokens": 6600000,
  "costKrw": 96412.5, "sharePercent": 68 }
```

`purpose`: `ANSWER` · `EMBED_DOC` · `EMBED_QUERY` · `ETC`
`provider`: `GOOGLE` · `ANTHROPIC` · `OPENAI` · `STUB`

| Method | Path | 권한 |
|---|---|---|
| GET | `/api/ops/profitability` | `PROFITABILITY_READ` |
| GET | `/api/ops/ai-usage/summary` | `AI_USAGE_READ` |
| GET | `/api/ops/ai-usage/daily?days=14` | `AI_USAGE_READ` |
| GET | `/api/ops/ai-usage/by-model?periodMonth=2026-08` | `AI_USAGE_READ` |
| GET | `/api/ops/ai-usage/top-tenants` | `AI_USAGE_READ` |

조회 대상 테이블은 화면마다 다르다 (admin-console-plan §6.1):
오늘·업체 목록·수익성 → `tenant_daily_usage` / 모델별 표 → `ai_usage` 월 단위 캐시 5분 / 업체 상세 → `ai_usage` 실시간 단일 테넌트.

---

## 7. 요금제 · 모델 단가 · 안전장치

```json
{ "id": "p2...", "name": "비즈니스", "monthlyFee": 89000,
  "convLimit": 3000, "docLimit": 500, "tenantCount": 19, "sellable": true }
```
```json
{ "id": 12, "provider": "GOOGLE", "model": "gemini-2.5-flash",
  "inputPer1m": 1100, "outputPer1m": 5500, "effectiveFrom": "2026-08-01T00:00:00Z" }
```
```json
{ "tenantDailyCapKrw": 20000, "globalDailyCapKrw": 400000,
  "ipQuestionsPerMin": 10, "bulkUploadLimit": 100,
  "costRatioWarnPercent": 70, "answerFailSimilarity": 0.72,
  "slackAlertEnabled": true }
```

| Method | Path | 권한 |
|---|---|---|
| GET · POST | `/api/ops/plans` | `PLAN_READ` / `PLAN_WRITE` |
| PATCH | `/api/ops/plans/{id}` | `PLAN_WRITE` |
| GET · POST | `/api/ops/model-prices` | `MODEL_PRICE_READ` / `MODEL_PRICE_WRITE` |
| GET · PUT | `/api/ops/cost-guards` | `COST_GUARD_READ` / `COST_GUARD_WRITE` |

- **요금제는 삭제하지 않는다.** `sellable: false` 로 판매 중단만. 기존 계약 업체가 남아 있다
- **모델 단가는 이력이다.** `POST /model-prices` 는 새 행을 추가하며 기존 행을 수정하지 않는다. 과거 `ai_usage.costKrw` 는 소급되지 않는다
- 단가·안전장치 쓰기는 `OPS_ADMIN` 전용

---

## 8. 작업 큐 · 기능 공개 · 문의 · 감사 기록

```json
{ "id": 9021, "kind": "EMBED_DOC", "tenant": { "id": "...", "name": "한빛물산" },
  "target": "2025_카탈로그_v3.pdf", "status": "FAILED",
  "errorCode": "pdf_parse_timeout", "attempts": 3, "maxAttempts": 3,
  "updatedAt": "2026-08-03T05:33:00Z" }
```
```json
{ "key": "image_product_recommend", "name": "이미지로 상품 추천",
  "scope": "TENANTS", "targetTenantIds": ["a8f3..."], "targetPlanId": null, "enabled": true }
```
```json
{ "id": 771, "at": "2026-08-03T02:20:00Z", "operator": { "id": "...", "name": "정OO" },
  "action": "IMPERSONATE", "tenant": { "id": "...", "name": "한빛물산" },
  "reason": "문의 #482 — PDF 업로드 실패 재현", "meta": {} }
```

`Job.kind`: `CRAWL` · `RECRAWL` · `EMBED_DOC` / `status`: `QUEUED` `RUNNING` `DONE` `FAILED`
`FeatureFlag.scope`: `INTERNAL` · `TENANTS` · `PLAN` · `ALL`
`AuditLog.action`: `IMPERSONATE` · `VIEW_CONVERSATIONS` · `CHANGE_PLAN` · `GRANT_QUOTA` · `SUSPEND` · `CHURN`

| Method | Path | 권한 |
|---|---|---|
| GET | `/api/ops/jobs?status=FAILED` | `JOB_READ` |
| POST | `/api/ops/jobs/{id}/retry` · `/api/ops/jobs/retry-all` | `JOB_RETRY` |
| GET · PATCH | `/api/ops/feature-flags` | `FLAG_READ` / `FLAG_WRITE` |
| GET · PATCH | `/api/ops/tickets` | `TICKET_READ` / `TICKET_WRITE` |
| GET | `/api/ops/audit-logs` | `AUDIT_READ` |

**감사 기록에 쓰기/삭제 엔드포인트는 존재하지 않는다.** 적재는 서버 내부에서만 일어난다.
오류 코드는 운영자용이므로 원문(`pdf_parse_timeout`)을 그대로 주고, 한글 설명은 프론트가 매핑한다.

---

## 9. 업체 대시보드 (`/api/app`)

### 9-0. 로그인과 컨텍스트

```json
{ "id": "m1...", "name": "정OO", "email": "owner@nordheim.co.kr",
  "role": "OWNER", "inviteState": "ACCEPTED", "lastSeenAt": "2026-08-03T05:41:00Z" }
```

`role`: `OWNER` · `EDITOR` · `VIEWER` / `inviteState`: `PENDING` · `ACCEPTED`

`PENDING` 인 담당자는 비밀번호가 없다. 로그인 시도는 `UNAUTHENTICATED` 로 거부한다 —
"초대 대기 중"이라고 알려주면 어떤 이메일이 등록돼 있는지 알려주는 셈이 된다.

```
POST /api/auth/app/login   { email, password } → { accessToken, refreshToken, member }
GET  /api/app/me           → { member, tenant, usage, impersonation }
```

```json
{
  "member": { "id": "m1...", "name": "정OO", "role": "OWNER" },
  "tenant": { "id": "a8f3...", "name": "노르드하임 가구", "primaryDomain": "nordheim.co.kr",
              "publishableKey": "pk_live_a8f3k2m9x7q1", "status": "ACTIVE",
              "plan": { "id": "p2...", "name": "비즈니스", "monthlyFee": 89000 } },
  "usage": { "convCount": 1142, "convLimit": 3000, "docCount": 248, "docLimit": 500 },
  "impersonation": null
}
```

`tenant` 와 `usage` 의 필드는 §2 TenantDetail 과 **이름·타입이 완전히 같다.** 부분집합일 뿐이다.

### 9-1. Faq (공통 질문 = 저장 답변)

```json
{ "id": "f3a1...", "question": "소파 재질이 궁금해요",
  "answer": "노르드 라인 소파는 북미산 화이트 오크...",
  "links": ["오크 3인 소파 — 노르드 라인", "원목 관리 방법"],
  "followUpFaqIds": ["f9c2..."],
  "shown": true, "sortOrder": 3, "hitCount": 504 }
```

`shown: false` 여도 방문자가 비슷한 내용을 직접 입력하면 이 답변이 쓰인다. 버튼 노출 여부일 뿐이다.
**끄는 것과 지우는 것은 다르다** — 자주 묻지는 않지만 답이 정해진 질문(세금계산서, 경쟁사 비교)에 쓴다.

| Method | Path | 권한 |
|---|---|---|
| GET | `/api/app/faqs` | 전 역할 |
| POST | `/api/app/faqs` | OWNER · EDITOR |
| PATCH · DELETE | `/api/app/faqs/{id}` | OWNER · EDITOR |
| PATCH | `/api/app/faqs/order` | OWNER · EDITOR |

`PATCH /api/app/faqs/order` 는 `{ "faqIds": ["f3a1...", "f9c2..."] }` 를 받아 그 순서대로 `sortOrder` 를 다시 매긴다.
개별 항목의 순서만 보내면 나머지와 충돌한다 — 전체 순서를 한 번에 보낸다.

### 9-2. 지식 소스

```json
{ "id": "s1...", "type": "WEBSITE", "origin": "nordheim.co.kr",
  "autoRefresh": true, "lastCrawledAt": "2026-08-03T02:41:00Z", "documentCount": 230 }
```
```json
{ "id": "d1...", "title": "배송 및 반품 안내", "path": "/guide/delivery",
  "status": "INDEXED", "chunkCount": 12, "sizeBytes": 18402, "indexedAt": "2026-08-03T02:41:00Z" }
```

`KnowledgeSource.type`: `WEBSITE` · `FILE` · `MANUAL`
`KnowledgeDocument.status`: `PENDING` `PROCESSING` `INDEXED` `FAILED` `EXCLUDED`

### 9-3. 답변 개선 · 대화 · 리드

```json
{ "id": 55, "question": "제주도까지 배송되나요? 추가 비용 있어요?",
  "reason": "ANSWER_FAILED", "occurrenceCount": 7,
  "lastAskedAt": "2026-08-03T05:22:00Z", "lastPath": "/product/1204",
  "botAnswer": "죄송합니다, 해당 내용은 확인이 어렵습니다.", "status": "OPEN" }
```

`reason`: `ANSWER_FAILED` · `THUMBS_DOWN` / `status`: `OPEN` · `RESOLVED` · `DISMISSED`

같은 질문을 표현만 바꿔 물어도 하나로 묶인다 — 공백·문장부호를 걷어낸 정규화 키로 누적한다.
`resolve` 는 FAQ 를 만들고 gap 을 `RESOLVED` 로 바꾼다. `dismiss` 는 목록에서만 감춘다(`DISMISSED`).

```json
{ "id": "L9...", "name": "김OO", "contact": "010-****-3391",
  "reason": "조립 서비스 신청 문의", "status": "NEW", "createdAt": "2026-08-03T04:11:00Z" }
```

`contact` 는 **뒷자리 마스킹된 값**으로 내려온다 (admin-console-plan §8 로그 마스킹). 원문은 CSV 내보내기에서만.

| Method | Path |
|---|---|
| GET | `/api/app/home` — 홈 요약 |
| GET | `/api/app/answer-gaps` |
| POST | `/api/app/answer-gaps/{id}/resolve` (→ Faq 생성) · `/dismiss` |
| GET | `/api/app/conversations` · `/api/app/conversations/{id}` |
| GET · PATCH | `/api/app/leads` · `/api/app/leads/{id}` |
| GET · POST · DELETE | `/api/app/members` |
| GET · PUT | `/api/app/appearance` |
| GET · POST · DELETE | `/api/app/allowed-origins` |
| GET | `/api/app/plan` — 요금제·사용량·결제 내역 |

### 9-4. 홈 요약 · 챗봇 설정

```json
{ "todayConvCount": 128, "todayConvDelta": 21,
  "answerSuccessPercent": 93, "answerSuccessPercentLastWeek": 96,
  "openGapCount": 9, "todayLeadCount": 3, "weekLeadCount": 11, "avgResponseMs": 1900,
  "knowledge": { "documentCount": 248, "indexedCount": 231, "processingCount": 12, "failedCount": 5 },
  "topQuestions": [ { "question": "배송은 며칠 걸리나요?", "askCount": 84 } ] }
```

```json
{ "botName": "노르드 도우미", "brandColor": "#17222E",
  "greeting": "안녕하세요! 가구 고르시는 것 도와드릴게요.",
  "persona": "노르드하임 가구의 상담 직원입니다...",
  "fallbackMessage": "제가 확인하기 어려운 내용이네요...",
  "forbiddenTopics": ["타사 브랜드 비교", "할인 협상", "재고 수량"],
  "leadCaptureEnabled": true, "supportPhone": "1588-0000",
  "agentHandoffEnabled": false, "agentHours": "평일 09:00–18:00",
  "widgetPosition": "BOTTOM_RIGHT", "pageScope": "ALL", "pagePatterns": [],
  "nudgeDelaySeconds": 15 }
```

`widgetPosition`: `BOTTOM_RIGHT` · `BOTTOM_LEFT` / `pageScope`: `ALL` · `INCLUDE` · `EXCLUDE`
`nudgeDelaySeconds` 가 `0` 이면 자동으로 말 걸지 않는다.
`forbiddenTopics` 는 화면에서 쉼표로 입력받되 **배열로 저장한다** — 문자열로 두면 나중에 항목 단위로 못 다룬다.

`GET /api/app/conversations/{id}` 는 고객 데이터 열람이므로 **대리 로그인 세션으로 호출되면 감사 기록에 `VIEW_CONVERSATIONS` 를 남긴다.**

---

## 10. 위젯 (`/api/widget`)

인증은 `X-Dabhaejwo-Key` + `Origin`. 시크릿을 받지 않는다.

```
POST /api/widget/session      → { sessionId, greeting, faqs: [ { id, question } ], brandColor, position }
POST /api/widget/ask          { sessionId, question }
                              → { answered, saved, answer, links, messageId }
POST /api/widget/faq/{id}     { sessionId } → { answer, links, followUpFaqIds, messageId }
POST /api/widget/feedback     { messageId, helpful } → 204
POST /api/widget/lead         { sessionId, name, contact } → 201
```

- `saved: true` 면 저장 답변으로 나간 것이다 — **모델을 거치지 않았고 `ai_usage` 에 기록되지 않으며 대화 사용량에도 잡히지 않는다.** 위젯은 이때 "저장된 답변" 태그를 표시한다
- `answered: false` 면 답변 실패 — 위젯이 연락처 폼을 제안하고, 그 질문은 업체 대시보드의 `answer-gaps` 로 올라간다
- 레이트 리밋 초과 `RATE_LIMITED`, 일일 원가 상한 도달 `COST_CAP_REACHED` (챗봇이 안내 메시지만 표시하고 멈춘다)
- 응답에 내부 구조·스택트레이스를 노출하지 않는다
