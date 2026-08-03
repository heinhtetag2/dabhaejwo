# backend-spring-boot — Spring Boot 백엔드 모듈 (PostgreSQL)

> 팩토리 `E:\_agent\stacks\backend-spring-boot.md` 기반. 이 프로젝트는 **PostgreSQL + pgvector** 를 쓴다.
> 팩토리 원본이 MySQL을 고정하고 있어 치환했다 — `docs/IMPROVEMENTS.md` `[harness]` 참조.

## 적용 조건
관계형 데이터 중심의 API 서버/풀스택 웹 백엔드. 도메인 모델·권한·트랜잭션이 있는 서비스.

## 스택
- Java 21+ / Spring Boot 최신 안정판 / Gradle Kotlin DSL (`build.gradle.kts`)
- Spring Data JPA + **PostgreSQL 16+ / pgvector**, 마이그레이션 Flyway, `ddl-auto: validate`
- Spring Security + JWT(jjwt), 캐시 Caffeine, API 문서 SpringDoc OpenAPI
- 테스트: JUnit 5 + **Testcontainers(`pgvector/pgvector`)**

## 디렉토리 구조 (도메인별 레이어드)

```
com.dabhaejwo/
├── domain/
│   └── {도메인}/
│       ├── controller/
│       ├── service/
│       ├── repository/
│       ├── entity/
│       └── dto/{request,response}/
└── global/                       # 크로스커팅만. 도메인 로직 금지
    ├── config/
    ├── security/                 # JWT, 필터, 인증 주체 (3종 분리)
    ├── exception/                # ErrorCode enum, GlobalExceptionHandler, BusinessException
    ├── audit/                    # AuditLogService
    ├── llm/                      # LlmProvider · LlmGateway (이 프로젝트 고유)
    └── common/                   # BaseEntity, 페이지 응답
```

## 레이어/의존성 규칙
- Controller: 요청 검증(`@Valid`) + service 호출 + 응답 변환만. 비즈니스 로직 금지.
- Service: 비즈니스 로직, 트랜잭션 경계(`@Transactional`), 소유권 검증.
- Repository: Spring Data 인터페이스. 복잡 쿼리는 `@Query`/Querydsl.
- 도메인 간 호출은 service → 타 도메인 service만. 타 도메인 repository 직접 참조 금지.
- Controller 밖으로 entity 노출 금지 — 응답은 항상 response DTO.

## 컨벤션
- **boolean 필드 `is` 접두사 금지**: `private boolean verified;` — Lombok이 `isVerified()` getter를 만들어도 Jackson 키는 `verified`로 유지된다. 필드명을 `isVerified`로 쓰면 엔드포인트마다 키가 뒤섞인다.
- Entity: setter 금지. 상태 변경은 의미 있는 메서드(`suspend()`, `churn()`)로, 전이 검증은 entity 메서드 안에서.
- 예외: `BusinessException(ErrorCode.XXX)` 단일 계열. ErrorCode enum에 HTTP status 포함.
- `Optional.ifPresent()`로 실패를 조용히 삼키지 않는다 — 대상 없으면 명시적 예외.
- 권한: `@RequirePermission("RESOURCE_ACTION")` 어노테이션 + AOP.
- 감사 로그: 운영자 쓰기 액션은 service에서 `auditLogService.record(...)` 호출, 같은 커밋에 포함.
- Flyway: `V{n}__{설명}.sql` 순차 증가. 적용된 마이그레이션 수정 금지.
- 설정값은 `application.yml` + `@ConfigurationProperties`. 매직 넘버/금액/계산식 하드코딩 금지.

## PostgreSQL 고유 규칙
- 식별자는 `uuid` (테넌트 소유 엔티티), 원장·로그성 테이블은 `bigserial`. 기획서 DDL의 선택을 유지한다.
- 시각은 전부 `timestamptz`. JPA는 `OffsetDateTime` 으로 받는다 — `LocalDateTime` 을 쓰면 타임존이 소리 없이 유실된다.
- `jsonb` 컬럼은 Hibernate `@JdbcTypeCode(SqlTypes.JSON)`. 문자열로 다루지 않는다.
- 벡터 검색은 `KnowledgeChunkRepository` 의 native query로 격리한다. `<=>` 연산자가 서비스 레이어로 새어나가지 않게 한다.
- 배열 컬럼(`uuid[]`)은 조회 조건으로 쓰지 않는다 — 조인 테이블이 필요한 신호다.

## 이 프로젝트 고유 — LLM 호출
- **모든 LLM 호출은 `global/llm/LlmGateway` 를 지난다.** Provider 를 직접 주입받아 호출하는 코드 금지.
- Gateway 가 `purpose`(ANSWER/EMBED_DOC/EMBED_QUERY/ETC)를 필수 인자로 받고, 호출 시점 `model_prices` 로 원가를 계산해 `ai_usage` 에 확정 저장한다.
- 모델명·단가는 코드에 없다. DB에서 읽는다.

## 테스트
- Service 통합 테스트: `@SpringBootTest` + Testcontainers(**PostgreSQL + pgvector 이미지**). H2 대체 금지 (방언 차이 거짓 통과).
- 상태 전이·권한은 성공/거부 케이스 모두. Controller는 주요 플로우만.
- **Docker가 없는 환경**: Testcontainers 불가 시 ① 도메인 로직을 entity 메서드로 모아 순수 단위 테스트로 커버, ② 통합 테스트는 IMPROVEMENTS P1로 등록하고 Docker 확보 후 추가. H2로 때우지 않는다.

## 검증 명령
| 대상 | 명령 |
|------|------|
| 빌드+테스트 | `./gradlew build` |
| 테스트만 | `./gradlew test` |

## 빌드 래퍼
- Gradle 래퍼는 **직접 작성 금지** — `gradle wrapper` 공식 배포본을 그대로 쓴다.
- `gradle-wrapper.properties` 는 **BOM 없는 UTF-8**. BOM이 붙으면 첫 키(`distributionUrl`)를 못 읽는다.
- 검증 파이프에서 `명령 | tail` 처럼 파이프로 끝내면 exit code가 가려진다. 빌드 성공 판정은 원 명령의 exit code로.
