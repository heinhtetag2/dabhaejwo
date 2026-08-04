# IMPROVEMENTS — 개선 백로그

> 지금 안 고치기로 한 문제를 기록. 발견 즉시 등록, 해결 시 삭제.
> - **P0**: 런칭/배포 차단 — 새 기능보다 먼저 처리
> - **P1**: 기능 결함이지만 우회 가능
> - **P2**: 개선/리팩토링
> - 항목이 rules/하네스 자체의 문제면 비고에 `[harness]` 태그 → 마일스톤 종료 시 E:\_agent 팩토리로 환류

## P0

| 항목 | 위치 | 등록일 | 비고 |
|------|------|--------|------|
| **약관·개인정보처리방침 본문 없음** | `tenant/src/app/(public)/terms`·`privacy` | 2026-08-04 | 가입 화면이 동의를 받는데 가리키는 문서가 비어 있다. 라우트와 목차만 있고 본문은 "준비 중"이다 — **법무 검토 없이 쓴 약관은 없느니만 못하다.** 개인정보처리방침에는 운영팀 대리 접속을 반드시 명시해야 한다(`tenant-plan.md` §6.3 이 이력 공개의 근거로 삼은 고지 의무). 정식 공개 전 필수. 사용자 조치 필요 |
| **사업자 정보 미기재** | `tenant/src/shared/config/company.ts` | 2026-08-04 | 상호·대표자·사업자등록번호·통신판매업 신고번호·주소가 비어 있다. **전자상거래법상 표기 의무**다. 값을 지어내지 않고 빈 문자열로 뒀으며, 푸터는 빈 항목을 그리지 않는다. 채우면 자동으로 나타난다. 사용자 조치 필요 |
| **R2 자격증명 폐기·재발급** | `docs/r2-info.md` | 2026-08-04 | Access Key ID·Secret Access Key·API Token 이 평문으로 한 번 커밋됐다(`git add -A` 에 쓸림). **원격이 없어 이 PC 밖으로 나가지 않았고**, 커밋 재작성 + reflog 만료 + gc 로 객체 DB 에서 제거했다. 그래도 **폐기·재발급이 안전하다** — Cloudflare 대시보드에서 R2 API 토큰을 새로 만들고 기존 것을 지운다. 새 값은 `.env` 에만 둔다. 사용자 조치 필요 |
| 노출됐던 Gemini API 키 폐기·재발급 | 구 `ai-info.md` | 2026-08-03 | 문서에서는 제거했으나 **키 자체는 아직 유효하다.** Google AI Studio 에서 폐기 후 재발급 → `.env` 에만 보관. 사용자 조치 필요 |

## P1

| 항목 | 위치 | 등록일 | 비고 |
|------|------|--------|------|
| 팩토리 Spring Boot 모듈이 MySQL을 고정 | `E:\_agent\stacks\backend-spring-boot.md` | 2026-08-03 | `[harness]` 모듈이 "관계형 DB"를 MySQL로 못박아 PG 프로젝트마다 치환이 필요하다. `_guide.md` 규칙 4(버전 하드코딩 금지)의 DB판. DB 선택을 기획서에 위임하도록 일반화 |
| 팩토리 FSD 모듈에 특정 프로젝트 사정이 섞임 | `E:\_agent\skills\fsd-rules.md` | 2026-08-03 | `[harness]` 상단 주의에 "ScoreShot admin 은 TanStack·Zustand 미설치"라는 **그 프로젝트만의 상태**가 재사용 모듈 본문에 들어 있다. 재사용 모듈은 스택 규칙만 담아야 한다 |
| 스택 모듈 선택 기준 중복 | `E:\_agent\stacks\` | 2026-08-03 | `[harness]` `admin-next-console.md` 와 `frontend-rules.md` 의 적용 조건이 겹쳐(둘 다 관리자 화면 있는 Next 앱) 판단이 어렵다. 전자는 "외부 UI 라이브러리 금지", 후자는 허용이라 결과가 크게 갈린다 |
| widget 스택 모듈 팩토리 미저장 | `.claude/rules/widget-embed-script.md` | 2026-08-03 | `[harness]` 신규 작성했으나 아직 `E:\_agent\stacks\` 에 없다. `_guide.md` 규칙 2에 따라 환류 필요 |
| **Docker 미설치 — 통합 테스트 불가 (확정)** | `api/` | 2026-08-03 | 로컬에 Docker 가 없어 Testcontainers 를 못 쓴다. 원가 계산은 순수 단위 테스트(`ModelPriceLookupTest`)로 커버했으나, **Flyway 마이그레이션·JPA 매핑·벡터 검색은 아직 한 번도 실행되지 않았다.** Docker 확보 후 `@SpringBootTest` + Testcontainers(`pgvector/pgvector:pg17`)로 검증할 것. **H2로 때우지 않는다** |
| **DB 클러스터가 공유 자산이고 `pg_hba` 가 전면 개방** | DB 서버 `192.168.0.254:6001` | 2026-08-04 | `pg_hba.conf` 마지막 줄이 `host all all 0.0.0.0/0 md5` 다 — 포트만 닿으면 **어느 IP 에서든 아무 DB 에나** 인증을 시도할 수 있고, `md5` 는 구식이다. 같은 클러스터에 다른 프로젝트(`mbeauty`)가 있어 함부로 못 고친다. 지금은 6001 이 공유기에서 막혀 있어 실害가 없지만, **운영 개시 전 필수**: catch-all 을 걷어내고 `scram-sha-256` + 소스 IP 로 좁힌다. 먼저 `pg_stat_activity` 로 기존 접속 IP 를 확인할 것. `local all postgres` 를 `peer` 로 바꿔 둔 상태이며 원본은 `pg_hba.conf.bak` |
| 임시로 바꾼 `pg_hba.conf` 원복 검토 | DB 서버 | 2026-08-04 | `postgres` 계정 접근을 위해 `local all postgres md5` → `peer` 로 바꿨다. `peer` 가 더 안전하므로 그대로 두어도 되지만, `mbeauty` 쪽 운영 스크립트가 비밀번호 접속에 의존한다면 깨진다. 확인 필요 |
| **새로고침하면 로그아웃된다 (업체 대시보드)** | `tenant/src/shared/lib/auth-store.ts` | 2026-08-04 | access token 을 메모리에만 두는 규칙(`kickoff-prompt.md` §1.3)에 맞춰 리프레시 토큰도 메모리에 뒀다. 그래서 새로고침·탭 재열기마다 다시 로그인해야 한다. 개발 중 특히 번거롭다. **localStorage 로 옮기지 않는다** — 지속 세션은 서버가 리프레시 토큰을 httpOnly·SameSite 쿠키로 내려줘야 안전해진다. `/api/auth/app/login` 이 쿠키를 세팅하도록 바꾸는 것이 정답 |
| 한 이메일이 여러 업체에 속하는 경우 미지원 | `AppAuthService#findByCredentials` | 2026-08-04 | `tenant_members` 의 UNIQUE 가 `(tenant_id, email)` 이라 같은 이메일이 여러 업체에 있을 수 있다. 지금은 비밀번호가 맞는 첫 번째를 고른다. 대행사가 여러 업체를 관리하는 경우가 생기면 로그인 후 업체를 고르는 단계가 필요하다 |
| 가입 레이트 리밋이 인스턴스 메모리에만 있다 | `SignupRateLimiter` | 2026-08-04 | 서버가 여러 대가 되면 대당 한도가 되어 실효가 떨어지고, 재시작하면 초기화된다. 단일 인스턴스인 지금은 충분하다. 확장 시 Redis 로 옮긴다 |
| 가입 시 이메일 인증 없음 | `SignupService` | 2026-08-04 | 가짜 주소로 체험 계정을 반복 생성할 수 있다. 체험 한도(대화 100·문서 30)와 일일 원가 상한, IP 레이트 리밋으로 피해는 제한된다. Mailer 연동 시 함께 넣는다 |
| 도메인 소유 확인 없음 | `SignupService` · `AllowedOriginService` | 2026-08-04 | 서로 다른 업체가 같은 도메인을 등록할 수 있다. 위젯 키가 달라 동작 충돌은 없고, 남의 사이트에 스크립트를 넣을 수 있는 건 그 주인뿐이라 실질 위험은 낮다. 확인 절차는 가입 마찰을 크게 키우므로 문제가 실제로 생기면 넣는다 (`tenant-public-plan.md` §5.3) |
| SpringDoc OpenAPI 미도입 | `api/build.gradle.kts` | 2026-08-03 | 최신 `springdoc-openapi-starter-webmvc-ui` 2.8.6 은 Spring Boot 3.x 대상이라 Boot 4.1 호환이 불확실해 제외했다. 호환 버전 확인 후 추가 |
| 프로토타입 HTML에 doctype·html·head·body 없음 | `docs/prototype/*.html` | 2026-08-03 | quirks mode 로 렌더될 수 있어 "UI 100% 동일" 판정의 기준이 흔들린다. 코드 전환은 표준 모드 기준으로 맞춘다 |

## P2

| 항목 | 위치 | 등록일 | 비고 |
|------|------|--------|------|
| 프로토타입 인라인 스타일 대량 | `docs/prototype/*.html` | 2026-08-03 | `style=` 속성 수백 개. `kickoff-prompt.md` §1.1이 복사 금지이므로 전부 토큰으로 재작성해야 한다. 전환 비용이 작지 않음 |
| 프로토타입 접근성 부채 | `docs/prototype/*.html` | 2026-08-03 | 토글이 `div+onclick` 이라 키보드 조작 불가, 업체 목록 행도 `<tr onclick>` 뿐. 재작성 시 radix 기반으로 해소 |
| 위젯 프로토타입이 Google Fonts 로드 | `docs/prototype/chatbot-visitor-widget.html` | 2026-08-03 | 방문자에게 폰트 다운로드를 강제하고 호스트 사이트 LCP에 영향. 실 구현은 system font stack (`widget-embed-script.md`) |
| 프로토타입 위젯 매칭이 글자 겹침 휴리스틱 | `docs/prototype/chatbot-visitor-widget.html` | 2026-08-03 | 데모용. 실 구현은 질문 임베딩 유사도로 판정하며 임계값은 설정값(`answer_fail_similarity`) |
