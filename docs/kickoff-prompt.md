# 프로젝트 부트스트랩 규약

> 새 프로젝트를 시작할 때의 단일 참조 문서.
> **Frontend 규약 · Backend 규약 · 킥오프 프롬프트** 세 파트로 구성된다.
> 킥오프 시 아래 규약을 컨텍스트로 두고 §3의 프롬프트로 부트스트랩한다.

---

## 1. Frontend

### 1.1 프로토타입 → 코드 전환 규약

HTML 프로토타입을 **Next.js + TypeScript + Tailwind + FSD** 코드로 전환한다.
`/HTML`은 **레퍼런스일 뿐**이며, 인라인 스타일을 복사하지 않고 토큰·컴포넌트로 재작성한다.
UI는 100% 동일하게 한다.

### 1.2 프로젝트 구조 (FSD + Next.js App Router)

```
[user|admin]/
├─ src/
│  ├─ app/
│  │  ├─ (shell)/layout.tsx          ← 공통 셸
│  │  ├─ (shell)/[화면]/page.tsx     ← 20줄 이하, view import + return만
│  │  ├─ globals.css                 ← CSS 변수 + 폰트
│  │  └─ layout.tsx                  ← 폰트 import, data-density 기본값
│  ├─ entities/{domain}/{resource}/  ← types·schema·query·mutation·index
│  ├─ features/{domain}/{feature}/   ← ui/{feature}-view·table·search·form-modal + index
│  ├─ widgets/                       ← sidebar·header·bottom-nav
│  └─ shared/
│     ├─ common/                     ← Button, Input, Modal, DataTable…
│     ├─ ui/                         ← StatusBadge, ConfirmModal, Pagination, QuillEditor…
│     ├─ api/                        ← httpClient (스텁)
│     ├─ lib/                        ← 유틸·store (Zustand)
│     └─ config/                     ← 환경변수·상수·라우트
├─ public/assets/
├─ public/fonts/                     ← 영문·한글 폰트 따로 사용 (제공 예정)
├─ tailwind.config.ts
└─ package.json
```

### 1.3 상태 관리

- 서버/비동기 상태: **TanStack Query**
- 클라이언트 전역 상태: **Zustand** (access token은 메모리에만 — localStorage 금지)
- URL 상태 우선: `useSearchParams`

### 1.4 성능·리소스

- `next/image` · `next/font` 사용
- 대용량 라이브러리(jsPDF · html2canvas 등)는 `next/dynamic`으로 지연 로드

### 1.5 사용할 라이브러리
- radix-ui
- dayjs
- html2canvas
- lucide-react
- next-pwa
- react-hook-form
- chartjs
- swiper
- zod
- zustand 
- tanstack/react-query
- clsx

---

## 2. Backend

### 2.1 API 계약 규칙 — 계약 단일 진실 공급원

> API(백엔드↔프론트, 또는 외부 공개 API)가 있는 프로젝트에만 적용. 없는 프로젝트는 이 파일을 복사하지 않는다.

`docs/architecture/api-contracts.md`가 서버 모델/DTO와 클라이언트 타입의 유일한 기준이다.
계약 ↔ 코드가 어긋나면 **코드가 틀린 것**이다.

#### 네이밍 (가장 자주 깨지는 규칙 — 엄수)

**boolean: `is` 접두사 금지**

- JSON 키, 서버 필드, 클라이언트 타입 전부 `is` 없이: `verified`, `active`, `open`
- 이유: 직렬화 프레임워크가 getter 규약에 따라 키를 변형해 엔드포인트마다 `isX`/`x`가 뒤섞이는 사고가 잦다. 스택 불문 처음부터 통일한다.
- 클라이언트 스키마/타입도 동일 키. 변환 레이어를 만들지 않는다.

**공통 네이밍**

- JSON 키: camelCase. DB 컬럼: snake_case.
- ID 참조: `{resource}Id`. 중첩 객체로 줄 때는 리소스명 그대로 (`business: { id, name }`).
- 날짜/시간: ISO-8601, UTC. `~At`(시각) / `~Date`(날짜).
- enum 값: UPPER_SNAKE_CASE 문자열.
- 다국어 필드가 필요한 프로젝트: `{ "ko": "...", "en": "..." }` 객체로 통일.

#### 단일 표현 규칙 (one resource, one shape)

- 같은 리소스는 **모든 엔드포인트에서 동일한 필드명**. 공개/소유자/관리자 조회에서 키가 달라지면 안 된다.
- 노출 범위가 다르면 필드를 **빼는 것만** 허용. 같은 의미의 키를 다른 이름으로 만들지 않는다.
- 변형이 필요하면 `XxxSummary`(부분집합) / `XxxDetail`(전체)로 명명, 겹치는 필드는 이름·타입 완전 동일.

#### 응답 포맷

- 페이지네이션: `{ "content": [...], "page": { "number", "size", "totalElements", "totalPages" } }`
- 에러: `{ "code": "RESOURCE_NOT_FOUND", "message": "..." }` — code는 기계용, message는 사람용.
- 리스트 조회는 항상 size 상한 (기본 20, 최대 100). 무제한 조회 금지.

#### 계약 문서 작성

- 리소스마다: 실제 JSON 예시(키가 진실) + 필드 설명 + 상태 전이(있다면) + 권한.
- 계약 변경 시 같은 커밋에서 서버·클라이언트를 함께 맞춘다. "나중에 맞추기" 금지.

### 2.2 Spring Boot 백엔드 모듈

#### 적용 조건

관계형 데이터 중심의 API 서버 / 풀스택 웹 백엔드. 도메인 모델·권한·트랜잭션이 있는 서비스.

#### 스택

- Java 21+ / Spring Boot 최신 안정판 / Gradle Kotlin DSL (`build.gradle.kts`)
- Spring Data JPA + MariaDB, 마이그레이션 Flyway, `ddl-auto: validate`
- Spring Security + JWT(jjwt)
- 테스트: JUnit 5 + Testcontainers(MariaDB)

#### 디렉토리 구조 (도메인별 레이어드)

```
com.{org}.{project}/
├── domain/
│   └── {도메인}/                 # 예: member, order
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── entity/
│       └── dto/
│           ├── request/
│           └── response/
└── global/                       # 크로스커팅만. 도메인 로직 금지
    ├── config/
    ├── security/                 # JWT, 필터, 인증 주체
    ├── exception/                # ErrorCode enum, GlobalExceptionHandler, BusinessException
    ├── audit/                    # AuditLogService (core security-rules의 감사 로그 구현)
    └── common/                   # BaseEntity(createdAt/updatedAt), 페이지 응답
```

#### 레이어 / 의존성 규칙

- Controller: 요청 검증(`@Valid`) + service 호출 + 응답 변환만. 비즈니스 로직 금지.
- Service: 비즈니스 로직, 트랜잭션 경계(`@Transactional`), 소유권 검증.
- Repository: Spring Data 인터페이스. 복잡 쿼리는 `@Query`/Querydsl.
- 도메인 간 호출은 service → 타 도메인 service만. 타 도메인 repository 직접 참조 금지.
- Controller 밖으로 entity 노출 금지 — 응답은 항상 response DTO.

#### 컨벤션

- **boolean 필드 `is` 접두사 금지**: `private boolean verified;` — Lombok이 `isVerified()` getter를 만들어도 Jackson 키는 `verified`로 유지된다. 필드명을 `isVerified`로 쓰면 엔드포인트마다 키가 뒤섞인다 (core api-contract-rules의 이 스택 구체 사유).
- Entity: setter 금지. 상태 변경은 의미 있는 메서드(`approve()`, `close()`)로, 전이 검증은 entity 메서드 안에서 (core workflow-rules의 상태 전이 구현).
- 예외: `BusinessException(ErrorCode.XXX)` 단일 계열. ErrorCode enum에 HTTP status 포함. 도메인별 예외 클래스 남발 금지.
- `Optional.ifPresent()`로 실패를 조용히 삼키지 않는다 — 대상 없으면 명시적 예외.
- 권한: `@RequirePermission("RESOURCE_ACTION")` 어노테이션 + AOP (core security-rules의 선언적 인가 구현).
- 감사 로그: admin 쓰기 액션은 service에서 `auditLogService.record(...)` 호출, 같은 커밋에 포함.
- Flyway: `V{n}__{설명}.sql` 순차 증가. 적용된 마이그레이션 수정 금지.
- 설정값은 `application.yml` + `@ConfigurationProperties`. 매직 넘버/금액/계산식 하드코딩 금지.

#### 테스트

- Service 통합 테스트: `@SpringBootTest` + Testcontainers(MySQL). H2 대체 금지 (방언 차이 거짓 통과).
- 상태 전이·권한은 성공/거부 케이스 모두. Controller는 주요 플로우만.
- **Docker가 없는 환경**(scoreshot M1 회고): Testcontainers 불가 시 ① 도메인 로직을 entity 메서드로 모아 순수 단위 테스트로 커버, ② 통합 테스트는 IMPROVEMENTS P1로 등록하고 Docker 확보 후 추가. H2로 때우지 않는다.

#### 검증 명령

| 대상 | 명령 |
|------|------|
| 빌드+테스트 | `./gradlew build` |
| 테스트만 | `./gradlew test` |

---

## 3. 킥오프 프롬프트 템플릿 — 기획서 → 프로젝트 부트스트랩

새 프로젝트를 시작할 때 아래 프롬프트를 복사해 **기획서 경로만 바꿔** 사용한다.
절차 자체는 `new-project` 스킬(+ `E:\_agent` 팩토리)에 코드화되어 있으므로, 이 프롬프트는 모델이 바뀌어도(Opus · Sonnet 등) 게이트를 명시적으로 강제하는 용도다.

> 팁: 큰 부트스트랩은 플랜 모드(Shift+Tab)로 계획을 먼저 받아 승인한 뒤 실행시키면 더 안정적이다.

```
새 프로젝트 시작한다. new-project 스킬로 부트스트랩해줘.

기획서 (진실 공급원):
- plan/admin.md — 관리자 콘솔 기획
- plan/user.md — 사용자 앱 기획
- prototype/admin.html, prototype/user.html, main_page_mn.html — 시각 참조 전용 (마크업/스타일 복붙 금지)

저장소 구조 (고정):
- 패키지 매니저는 npm.
- 모노레포/워크스페이스로 묶지 말고, 루트에 admin/, user/, api/ 3개 독립 프로젝트로 구성한다.
- admin·user는 설정(package.json·tsconfig·eslint·tailwind 등)을 전부 개별 관리 — 공유 패키지를 만들지 않는다.
- 공유가 필요한 상수·타입은 각 프로젝트에 복제하되 출처 주석으로 동기화 지점을 명시한다.

진행 규칙 — 순서와 게이트를 엄수해라:
1. Phase 1~4 (intake → 스택 결정 → 하네스 조립 → API 계약)를 코드보다 먼저 완료.
   블로킹 Open Question은 이 시점에 모아서 한 번에 질문. 나머지는 합리적 기본값 + intake에 기록.
2. 스캐폴드 전에 주요 의존성의 최신 안정 버전을 반드시 웹에서 확인 (학습 데이터의 버전 금지).
3. 스캐폴드는 프로젝트 단위로: api → admin → user 순서로 하나씩 완성.
   각 프로젝트 완료마다 해당 프로젝트의 lint·typecheck·build 그린 확인 후 다음으로. 깨진 채로 진행 금지.
4. 완료 후: 의미 단위로 커밋 분할 → CLAUDE.md 마일스톤 기록 → 결과 보고
   (스택 선택 이유, 생성 구조, Open Questions, 다음 마일스톤 제안 포함).
5. 검증이 깨진 상태로 완료 보고 절대 금지. 실패하면 원인과 함께 보고.
```