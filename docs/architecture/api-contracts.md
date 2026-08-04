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
| `FEATURE_NOT_READY` | 503 | 외부 의존성 미연동 (크롤러·결제·메일 등). 조용히 성공시키지 않는다 |

### 0-4. 인증 (3종 분리 + 공개)

| Prefix | 주체 | 헤더 |
|---|---|---|
| `/api/ops/**` | 운영자 | `Authorization: Bearer {opsAccessToken}` |
| `/api/app/**` | 업체 담당자 **또는 대리 로그인 세션** | `Authorization: Bearer {appAccessToken}` |
| `/api/widget/**` | 방문자(익명) | `X-Dabhaejwo-Key: pk_live_...` + `Origin` 검증 |
| `/api/public/**` | **없음** — 누구나 읽는 정보. **GET 전용** | — |
| `/api/auth/**` | 공개 — 인증을 만들어내는 행위 | — |

`/api/public/**` 에 쓰기를 두지 않는다. 가입은 인증을 만들어내므로 `/api/auth/**` 에 있고
레이트 리밋도 거기 붙는다 (`docs/plan/tenant-public-plan.md` §7.1).

```
POST /api/auth/ops/login      → { accessToken, refreshToken, operator: {...} }
POST /api/auth/app/login      → { accessToken, refreshToken, member: {...} }
POST /api/auth/app/signup     → { accessToken, refreshToken, member: {...} }   ← login 과 같은 형태
POST /api/auth/refresh        → { accessToken }
GET  /api/public/plans        → [ PublicPlan ]
```

`/api/auth/refresh` 는 **하나뿐이다.** 리프레시 토큰의 `scope`(`ops` | `app`)를 서버가 읽어
원래 주체 종류로만 액세스 토큰을 다시 만든다. 운영자 리프레시 토큰으로 업체 액세스 토큰을
받아내는 경로는 없다.

```json
{ "email": "ops@dabhaejwo.com", "password": "..." }
```

운영자 로그인은 **로컬 계정 + BCrypt** 다. 기획서 §8 은 SSO + 2FA 를 요구하지만
`operators.totp_secret` 자리만 있고 미구현이다 (CLAUDE.md Stub 목록).
실패 사유를 구분해 응답하지 않는다 — 없는 이메일인지 비밀번호가 틀렸는지
비활성 계정인지 알려주면 계정 존재 여부를 확인하는 수단이 된다.

`signup` 이 `login` 과 같은 형태인 이유는 가입 직후 로그인 상태여야 하고,
클라이언트가 두 응답을 다르게 다룰 이유가 없기 때문이다.

```json
{ "email": "...", "password": "...", "tenantName": "노르드하임 가구",
  "primaryDomain": "nordheim.co.kr", "termsAgreed": true }
```

가입은 **업체·담당자(OWNER)·챗봇 설정·허용 주소를 한 트랜잭션에서** 만든다.
하나라도 실패하면 전부 되돌린다 — 반쯤 만들어진 계정은 아무것도 할 수 없고 본인은 이유를 모른다.

```json
{ "id": "p2...", "code": "BUSINESS", "name": "비즈니스", "monthlyFee": 89000,
  "negotiable": false, "convLimit": 3000, "docLimit": 500 }
```

`PublicPlan` 은 §7 의 요금제와 **같은 리소스**이며 `tenantCount` 를 뺐을 뿐이다 —
"몇 곳이 쓰는지"는 대외 공개 정보가 아니다. `sellable = false` 는 목록에 나오지 않는다.

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

### 2-5. 필터 건수 · 활동 이력 · 요청 본문

```json
{ "all": 42, "trial": 6, "paymentFailed": 1, "costExceeded": 3,
  "inactive7d": 4, "suspended": 2, "churned": 0 }
```

`GET /api/ops/tenants/filters`. 건수가 0인 칩도 내려준다 — 화면은 흐리게 처리하되 숨기지 않는다
(admin-console-tenant-plan.md §4.1.1). 키는 필터 파라미터 값의 camelCase 형이며 **같은 집합**을 가리킨다.

```json
{ "id": 512, "type": "CHANGE_PLAN", "at": "2026-08-02T05:15:00Z",
  "summary": "스타터 → 비즈니스", "reason": "기업 요금제 계약 반영",
  "operator": { "id": "...", "name": "정OO" } }
```

`GET /api/ops/tenants/{id}/activities` — `type`: `CHANGE_PLAN` · `GRANT_QUOTA` · `SUSPEND` · `CHURN`
· `EXTEND_TRIAL` · `IMPERSONATE` · `PAYMENT` · `NOTE`. 감사 기록·결제 기록·메모를 시각 역순으로 합친
**읽기 전용 합성 뷰**다. 별도 테이블을 두지 않는다 — 같은 사실을 두 곳에 쓰면 언젠가 갈라진다.
`operator` 는 시스템이 만든 항목(결제)에서 `null`.

```json
{ "status": "SUSPENDED", "reason": "결제 3회 실패 — 영업 확인 완료" }
{ "planId": "p2...", "reason": "기업 요금제 계약 반영" }
{ "days": 7, "reason": "도입 검토 연장 요청" }
```

`PATCH /status` · `PATCH /plan` · `POST /trial-extension` 의 본문이다.
체험 연장은 `TRIAL` 상태에서만 허용되며 그 외에는 `INVALID_STATE_TRANSITION`.

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

**`system` 의 미측정 필드는 `null` 이다.** 답변 파이프라인·크롤러 워커·APM 이 아직 없어
`chatApiP95Ms` · `crawlerWorkers` · `vectorDbUsagePercent` · `todayError5xxCount` 는 잴 곳이 없다.
0 으로 채우면 "응답 0ms, 오류 0건"이라는 **거짓**이 되고, 운영자는 정상이라고 읽는다.
`null` 로 내리고 화면은 "집계 없음"으로 표시한다. 측정 지점이 생기면 값이 채워진다.
`embedQueueDepth` 는 `jobs` 테이블에서 실제로 세므로 지금도 실값(현재 0)이다.

```json
{ "aggregatedAt": "2026-08-04T05:00:00Z" }
```

`stats` 와 `headline` 은 `tenant_daily_usage` 를 읽는다. 당일분은 마지막 집계 시각을
함께 내려 화면이 "몇 시 기준"인지 밝힌다 (admin-console-plan §6.1). 한 번도 집계되지
않았으면 `null`.

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

`GET /api/ops/profitability` 는 지표와 목록을 함께 준다 — 두 번 부를 이유가 없다.

```json
{ "stats": { "revenueKrw": 3182000, "costKrw": 1411300, "avgCostRatioPercent": 44,
             "savedAnswerPercent": 39, "costExceededCount": 3,
             "costRatioWarnPercent": 70 },
  "content": [ /* 위 항목 */ ], "page": { } }
```

`costRatioWarnPercent` 를 응답에 싣는 이유는 경고선이 `cost_guards` 설정값이기 때문이다 —
프론트가 70을 상수로 갖고 있으면 설정을 바꿔도 색이 안 바뀐다.

```json
{ "todayTokensIn": 41600000, "todayTokensOut": 6600000, "todayCostKrw": 142700,
  "costPerConvKrw": 7.8, "monthCostKrw": 1411300, "monthProjectedCostKrw": 1890000 }
```
```json
{ "day": "2026-08-03", "answerKrw": 96412.5, "embedDocKrw": 30100, "embedQueryKrw": 11200, "etcKrw": 4987.5 }
```
```json
{ "tenant": { "id": "...", "name": "한빛물산" }, "planName": "비즈니스",
  "tokens": 312000000, "costKrw": 104800, "costPerConvKrw": 11.8 }
```

`monthProjectedCostKrw` 는 **경과일 평균 × 그 달의 일수**다. 추정치이며 화면이 그렇게 밝힌다.
`daily` 의 용도별 키를 배열이 아니라 고정 필드로 둔 이유는 `purpose` 가 4종으로 닫혀 있고
누적 막대의 층 순서가 화면 계약이기 때문이다.

---

## 7. 요금제 · 모델 단가 · 안전장치

```json
{ "id": "p2...", "name": "비즈니스", "monthlyFee": 89000,
  "convLimit": 3000, "docLimit": 500, "tenantCount": 19, "sellable": true }
```
```json
{ "id": 12, "provider": "GOOGLE", "model": "gemini-3.5-flash", "purposeKind": "GENERATE",
  "inputPer1m": 2100, "outputPer1m": 12600, "effectiveFrom": "2026-08-01T00:00:00Z",
  "note": "$1.50/$9.00", "current": true }
```
```json
{ "tenantDailyCapKrw": 20000, "globalDailyCapKrw": 400000,
  "ipQuestionsPerMin": 10, "bulkUploadLimit": 100,
  "costRatioWarnPercent": 70, "answerFailSimilarity": 0.72,
  "defaultChunkCount": 8, "answerMaxLength": 400, "churnPurgeGraceDays": 30,
  "quotaExceededBehavior": "STOP_AND_NOTICE", "slackAlertEnabled": true,
  "commonPrompt": "주어진 문서 조각만을 근거로 답한다...",
  "updatedAt": "2026-08-04T05:00:00Z" }
```
```json
{ "plan": { "id": "p2...", "name": "비즈니스" }, "provider": "GOOGLE",
  "model": "gemini-3.5-flash", "chunkCount": 8, "estimatedCostPerConvKrw": 8.4 }
```

| Method | Path | 권한 |
|---|---|---|
| GET · POST | `/api/ops/plans` | `PLAN_READ` / `PLAN_WRITE` |
| PATCH | `/api/ops/plans/{id}` | `PLAN_WRITE` |
| GET · POST | `/api/ops/model-prices` | `MODEL_PRICE_READ` / `MODEL_PRICE_WRITE` |
| GET · PUT | `/api/ops/plan-model-assignments` | `PLAN_READ` / `MODEL_PRICE_WRITE` |
| GET · PUT | `/api/ops/cost-guards` | `COST_GUARD_READ` / `COST_GUARD_WRITE` |

- **요금제는 삭제하지 않는다.** `sellable: false` 로 판매 중단만. 기존 계약 업체가 남아 있다.
  `DELETE` 엔드포인트가 존재하지 않는다
- **모델 단가는 이력이다.** `POST /model-prices` 는 새 행을 추가하며 **기존 행을 수정하지 않는다.**
  `PATCH`·`DELETE` 가 존재하지 않는다. 과거 `ai_usage.costKrw` 는 소급되지 않는다
- `current: true` 는 그 `(provider, model)` 조합에서 **지금 시각 기준으로 적용 중인 행**이라는 표시다.
  서버가 계산해 내려준다 — 프론트가 `effectiveFrom` 을 비교하다 보면 경계에서 어긋난다
- `quotaExceededBehavior`: `STOP_AND_NOTICE` · `OVERAGE_BILLING` · `NOTIFY_ONLY`
- `estimatedCostPerConvKrw` 는 조각 수 × 조각당 토큰 추정 × 현재 단가로 **서버가 계산한 추정치**다.
  저장하지 않는다 — 단가가 바뀌면 따라 움직여야 한다
- 단가·안전장치·모델 배정 쓰기는 `OPS_ADMIN` 전용

> **화면 주의** — 프로토타입(`chatbot-admin-console.html`)은 모델 단가를 표 안 `<input>` 으로
> 직접 고치는 형태다. 그대로 옮기면 소급 변경이 된다. 화면은 **"새 단가 등록 + 적용 시점 지정"**
> 이어야 하고, 기존 행은 읽기 전용 이력으로만 보여준다.

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
| GET | `/api/ops/jobs/stats` | `JOB_READ` |
| POST | `/api/ops/jobs/{id}/retry` · `/api/ops/jobs/retry-all` | `JOB_RETRY` |
| GET · PATCH | `/api/ops/feature-flags` · `/api/ops/feature-flags/{key}` | `FLAG_READ` / `FLAG_WRITE` |
| GET · PATCH | `/api/ops/tickets` · `/api/ops/tickets/{id}` | `TICKET_READ` / `TICKET_WRITE` |
| GET | `/api/ops/audit-logs` | `AUDIT_READ` |

```json
{ "queuedCount": 412, "runningCount": 18, "doneTodayCount": 6204,
  "successPercent": 99.1, "failedCount": 7 }
```
```json
{ "id": 441, "tenant": { "id": "...", "name": "한빛물산" },
  "subject": "PDF를 올렸는데 계속 실패로 뜹니다", "body": "...",
  "status": "OPEN", "elapsedMinutes": 1140,
  "answeredBy": { "id": "...", "name": "정OO" }, "answeredAt": null,
  "createdAt": "2026-08-02T10:41:00Z" }
```

- **`POST /jobs/{id}/retry` 와 `/retry-all` 은 지금 `FEATURE_NOT_READY`(503) 로 거절한다.**
  임베딩 워커·크롤러가 없어 큐에 다시 넣어도 아무도 집어가지 않는다. 상태만 `QUEUED` 로
  돌려놓으면 운영자는 복구된 줄 알고 기다린다 — 조용한 성공 처리 금지 (`workflow-rules.md`).
  `/api/app/knowledge/**` 의 `recrawl`·`retry` 와 같은 정책이다
- 문의 정렬은 **경과 시간 내림차순 고정**이다. 오래된 것이 위로 온다 (admin-console-plan §4.9).
  `PATCH /tickets/{id}` 는 `{ status }` 만 받는다 — 답변 본문은 이메일로 나가고 여기엔 남기지 않는다
- 기능 플래그 `PATCH` 본문은 `{ scope, targetTenantIds, targetPlanId, enabled }`. `key` 는 불변이다
- 감사 기록 조회 파라미터: `tenantId` · `operatorId` · `action` · `from` · `to`

**감사 기록에 쓰기/삭제 엔드포인트는 존재하지 않는다.** 적재는 서버 내부에서만 일어난다.
DB 트리거가 `UPDATE`/`DELETE` 자체를 막고 있다 — 앱이 뚫려도 기록은 못 고친다.
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

`EXCLUDED` 는 업체가 "이 페이지는 학습하지 않겠다"고 뺀 것이다 — **실패가 아니다.**
요금제 한도(`docCount`)에도 잡히지 않고 홈의 3분류(완료/처리중/실패)에도 들어가지 않는다.

| Method | Path | 권한 |
|---|---|---|
| GET | `/api/app/knowledge/sources` | 전 역할 |
| PATCH | `/api/app/knowledge/sources/{id}` | OWNER · EDITOR — `{ autoRefresh }` |
| POST | `/api/app/knowledge/sources/{id}/recrawl` | OWNER · EDITOR |
| GET | `/api/app/knowledge/documents?sourceId=&q=&status=&page=&size=` | 전 역할 |
| POST | `/api/app/knowledge/documents` | OWNER · EDITOR — **multipart** `file` |
| DELETE | `/api/app/knowledge/documents/{id}` | OWNER · EDITOR. 대리 접속 중 금지 |
| PATCH | `/api/app/knowledge/documents/{id}` | OWNER · EDITOR — `{ excluded }` |
| POST | `/api/app/knowledge/documents/{id}/retry` · `/retry-failed?sourceId=` | OWNER · EDITOR |

`recrawl` 과 `retry` 는 **크롤러·임베딩 워커가 붙어야 실제로 동작한다.** 지금은
`FEATURE_NOT_READY`(503) 를 돌려준다 — 상태만 바꾸고 아무 일도 일어나지 않으면
업체는 학습된 줄 알고 기다린다. 조용한 성공 처리 금지 (`workflow-rules.md`).

**업로드** — 원본은 오브젝트 저장소(S3 호환)에 두고 문서 행이 키로 가리킨다.

| 규칙 | 값 |
|---|---|
| 허용 확장자 | `pdf` `docx` `xlsx` `txt` `md` `csv` — **화이트리스트** |
| 파일당 크기 | 20MB. 넘으면 `VALIDATION_FAILED`(400) |
| MIME | 서버가 **확장자로 정한다.** 클라이언트가 보낸 값은 대조에만 쓰고, 어긋나면 거절 |
| 중복 | 내용 SHA-256 이 같으면 거절. 문서가 두 벌 생기고 한도만 깎인다 |
| 한도 | 올리기 **전에** 요금제 문서 한도를 본다. 넘으면 `QUOTA_EXCEEDED`(429) |
| 저장소 미설정 | `FEATURE_NOT_READY`(503). **로컬 디스크로 대체하지 않는다** |

업로드 응답의 `status` 는 `PENDING` 이다. **저장은 됐지만 학습은 시작되지 않는다** —
글자를 뽑아 임베딩하는 워커가 아직 없다. 화면이 이 사실을 그대로 표시한다.

`storageKey` 는 업로드 문서만 값이 있다. 웹페이지 문서는 원본 파일이 없어 `null` 이며,
화면은 이 값으로 삭제 버튼 노출을 정한다.

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
| POST | `/api/app/plan/upgrade-request` — 유료 전환 신청 → `tickets` 적재. **OWNER 전용** |

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
