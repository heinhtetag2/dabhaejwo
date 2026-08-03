# IMPROVEMENTS — 개선 백로그

> 지금 안 고치기로 한 문제를 기록. 발견 즉시 등록, 해결 시 삭제.
> - **P0**: 런칭/배포 차단 — 새 기능보다 먼저 처리
> - **P1**: 기능 결함이지만 우회 가능
> - **P2**: 개선/리팩토링
> - 항목이 rules/하네스 자체의 문제면 비고에 `[harness]` 태그 → 마일스톤 종료 시 E:\_agent 팩토리로 환류

## P0

| 항목 | 위치 | 등록일 | 비고 |
|------|------|--------|------|
| 노출됐던 Gemini API 키 폐기·재발급 | 구 `ai-info.md` | 2026-08-03 | 문서에서는 제거했으나 **키 자체는 아직 유효하다.** Google AI Studio 에서 폐기 후 재발급 → `.env` 에만 보관. 사용자 조치 필요 |
| **PostgreSQL 접속 정보 미확보** | `.env` | 2026-08-03 | MariaDB 에서 PostgreSQL 로 되돌렸다(사용자 결정). 사용자가 DB 를 생성해 접속 정보를 줄 예정이며, 그전까지는 앱을 띄울 수 없다. 받으면 `scripts/DbProbe.java` 로 pgvector 설치 여부를 먼저 확인한다. 사용자 조치 필요 |

## P1

| 항목 | 위치 | 등록일 | 비고 |
|------|------|--------|------|
| 팩토리 Spring Boot 모듈이 MySQL을 고정 | `E:\_agent\stacks\backend-spring-boot.md` | 2026-08-03 | `[harness]` 모듈이 "관계형 DB"를 MySQL로 못박아 PG 프로젝트마다 치환이 필요하다. `_guide.md` 규칙 4(버전 하드코딩 금지)의 DB판. DB 선택을 기획서에 위임하도록 일반화 |
| 팩토리 FSD 모듈에 특정 프로젝트 사정이 섞임 | `E:\_agent\skills\fsd-rules.md` | 2026-08-03 | `[harness]` 상단 주의에 "ScoreShot admin 은 TanStack·Zustand 미설치"라는 **그 프로젝트만의 상태**가 재사용 모듈 본문에 들어 있다. 재사용 모듈은 스택 규칙만 담아야 한다 |
| 스택 모듈 선택 기준 중복 | `E:\_agent\stacks\` | 2026-08-03 | `[harness]` `admin-next-console.md` 와 `frontend-rules.md` 의 적용 조건이 겹쳐(둘 다 관리자 화면 있는 Next 앱) 판단이 어렵다. 전자는 "외부 UI 라이브러리 금지", 후자는 허용이라 결과가 크게 갈린다 |
| widget 스택 모듈 팩토리 미저장 | `.claude/rules/widget-embed-script.md` | 2026-08-03 | `[harness]` 신규 작성했으나 아직 `E:\_agent\stacks\` 에 없다. `_guide.md` 규칙 2에 따라 환류 필요 |
| **Docker 미설치 — 통합 테스트 불가 (확정)** | `api/` | 2026-08-03 | 로컬에 Docker 가 없어 Testcontainers 를 못 쓴다. 원가 계산은 순수 단위 테스트(`ModelPriceLookupTest`)로 커버했으나, **Flyway 마이그레이션·JPA 매핑·벡터 검색은 아직 한 번도 실행되지 않았다.** Docker 확보 후 `@SpringBootTest` + Testcontainers(`pgvector/pgvector:pg17`)로 검증할 것. **H2로 때우지 않는다** |
| 앱 기동 미검증 (PostgreSQL) | `api/` | 2026-08-03 | `./gradlew build` 는 그린이지만 PostgreSQL 로는 아직 띄워본 적이 없다(`ddl-auto: validate` 가 DB를 요구). MariaDB 로는 한 번 기동해 27개 테이블·감사 트리거·seed 적재까지 확인했으나, 그 검증은 PG 로 이월되지 않는다. 접속 정보를 받는 즉시 기동 확인 |
| **Hibernate ↔ DDL 타입 일치 미검증 (PostgreSQL)** | `api/` | 2026-08-03 | `uuid`·`jsonb`·`timestamptz` 가 Hibernate PostgreSQLDialect 의 기대와 맞는지 확인되지 않았다. `ddl-auto: validate` 라 첫 기동 시 드러난다 |
| DB 노출 범위·계정 권한 확인 | 새 PostgreSQL 서버 | 2026-08-03 | 이전 MariaDB 서버는 공인 IP 에 열려 있고 계정에 호스트 제한이 없었다(`dabhaejwo@%`). 새 DB 를 받으면 같은 문제가 없는지 확인한다 — `pg_hba.conf` 범위, `listen_addresses`, 방화벽. **운영 개시 전 필수**. 운영 콘솔 IP allowlist(`dabhaejwo.ops.ip-allowlist`)와 함께 정리할 것 |
| SpringDoc OpenAPI 미도입 | `api/build.gradle.kts` | 2026-08-03 | 최신 `springdoc-openapi-starter-webmvc-ui` 2.8.6 은 Spring Boot 3.x 대상이라 Boot 4.1 호환이 불확실해 제외했다. 호환 버전 확인 후 추가 |
| 프로토타입 HTML에 doctype·html·head·body 없음 | `docs/prototype/*.html` | 2026-08-03 | quirks mode 로 렌더될 수 있어 "UI 100% 동일" 판정의 기준이 흔들린다. 코드 전환은 표준 모드 기준으로 맞춘다 |

## P2

| 항목 | 위치 | 등록일 | 비고 |
|------|------|--------|------|
| 프로토타입 인라인 스타일 대량 | `docs/prototype/*.html` | 2026-08-03 | `style=` 속성 수백 개. `kickoff-prompt.md` §1.1이 복사 금지이므로 전부 토큰으로 재작성해야 한다. 전환 비용이 작지 않음 |
| 프로토타입 접근성 부채 | `docs/prototype/*.html` | 2026-08-03 | 토글이 `div+onclick` 이라 키보드 조작 불가, 업체 목록 행도 `<tr onclick>` 뿐. 재작성 시 radix 기반으로 해소 |
| 위젯 프로토타입이 Google Fonts 로드 | `docs/prototype/chatbot-visitor-widget.html` | 2026-08-03 | 방문자에게 폰트 다운로드를 강제하고 호스트 사이트 LCP에 영향. 실 구현은 system font stack (`widget-embed-script.md`) |
| 프로토타입 위젯 매칭이 글자 겹침 휴리스틱 | `docs/prototype/chatbot-visitor-widget.html` | 2026-08-03 | 데모용. 실 구현은 질문 임베딩 유사도로 판정하며 임계값은 설정값(`answer_fail_similarity`) |
