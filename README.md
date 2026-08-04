# 답해줘

업체가 홈페이지에 스크립트 한 줄을 붙이면, 그 사이트를 학습한 챗봇이 방문자 질문에 답해주는 멀티 테넌트 SaaS.

```
dabhaejwo/
├── api/       Spring Boot · Java 21 · PostgreSQL (pgvector)
├── admin/     Next.js — 운영 콘솔 (내부용)
├── tenant/    Next.js — 업체 대시보드
├── widget/    Vite library — 방문자 임베드 위젯
└── docs/      기획서 · 계약 · 프로토타입
```

모노레포가 아니다. 네 프로젝트는 설정을 각자 관리하며 공유 패키지를 두지 않는다
(`docs/kickoff-prompt.md` §3). 공유가 필요한 파일은 복제하되 상단에 출처 주석을 남긴다.

## 준비물

| 도구 | 버전 | 비고 |
|---|---|---|
| Java | 21+ | |
| Node.js | 22+ | |
| PostgreSQL | 14+ | **pgvector 확장 필요** — `knowledge_chunks.embedding` |
| Docker | 선택 | 있으면 통합 테스트가 돌아간다 |

```sh
cp .env.example .env    # 값을 채운다. .env 는 커밋되지 않는다
```

```sh
docker run -d --name dabhaejwo-db -p 5432:5432 \
  -e POSTGRES_DB=dabhaejwo -e POSTGRES_USER=dabhaejwo -e POSTGRES_PASSWORD=... \
  pgvector/pgvector:pg17
```

### DB 준비

> 새 서버에 DB 를 처음 올리는 절차는 [docs/postgre-db-guide.md](docs/postgre-db-guide.md) 에 따로 있다.
> 설치·계정 생성·접속 경로(SSH 터널/포트 개방)와 실제로 걸렸던 문제들까지 적어 두었다.

`V1__init.sql` 은 `CREATE EXTENSION IF NOT EXISTS vector` 로 시작한다.
서버에 pgvector 패키지가 없거나 계정에 확장 생성 권한이 없으면 거기서 멈춘다.
PostgreSQL 은 DDL 이 트랜잭션이라 실패해도 깨끗하게 롤백되지만, 원인을 찾는 시간이 아깝다.
새 DB 를 받았으면 먼저 점검한다.

```sh
# 드라이버 jar 경로는 ./gradlew build 후 Gradle 캐시에서 찾는다
java --class-path <postgresql.jar> scripts/DbProbe.java \
     "$POSTGRES_HOST" "$POSTGRES_PORT" "$POSTGRES_DB" "$POSTGRES_USER" "$POSTGRES_PASSWORD"
```

연결·버전·pgvector 설치 여부·기존 테이블을 확인하고, 적용해도 되는 상태면 exit 0 을 낸다.
읽기 전용이라 아무것도 바꾸지 않는다.

```sh
cd api && ./gradlew bootRun     # Flyway 가 기동 시 마이그레이션을 적용한다
```

#### pgvector 가 없다면

```sh
sudo apt install postgresql-17-pgvector    # 버전 번호는 서버의 PG 메이저에 맞춘다
```

확장 생성에는 보통 superuser 권한이 필요하다. 앱 계정에 주기 싫으면 DBA 가 미리
`CREATE EXTENSION vector;` 를 실행해 두면 된다 — `V1` 의 `IF NOT EXISTS` 가 그냥 통과한다.

#### 마이그레이션이 실패했다면

PostgreSQL 은 DDL 이 트랜잭션이므로 실패한 마이그레이션은 통째로 롤백된다.
반쯤 만들어진 테이블이 남지 않는다. 원인을 고치고 다시 띄우면 된다.
`flyway_schema_history` 에 실패 기록이 남았다면 `flyway repair` 로 지운다.

## 실행

| 대상 | 포트 |
|---|---|
| api | **4310** |
| admin (운영 콘솔) | **4311** |
| tenant (업체 대시보드) | **4312** |
| widget (데모 서버) | 5173 (Vite 기본) |

```sh
cd api    && ./gradlew bootRun     # :4310 — 기동 시 Flyway가 스키마를 만든다
cd admin  && npm run dev           # :4311
cd tenant && npm run dev           # :4312
cd widget && npm run dev           # /demo/index.html 에서 실제 임베드 확인
```

**Spring Boot 는 `.env` 를 읽지 않는다** — Node/Docker 쪽 관례라서다. 그래서
`DotenvEnvironmentPostProcessor` 가 저장소 루트의 `.env` 를 프로퍼티로 얹어준다.
`bootRun`·IDE·`java -jar` 어느 쪽으로 띄우든 똑같이 동작한다.

- 실제 환경변수가 항상 `.env` 를 이긴다 (우선순위가 가장 낮다)
- `production` 프로파일에서는 파일을 아예 읽지 않는다 — 운영 자격증명은
  배포 환경의 시크릿 관리로 주입한다

## 설정 파일

```
api/src/main/resources/
├── application.yml             공통. 프로파일과 무관한 값
├── application-local.yml       개발. 기본값이 넉넉하다 (기본 프로파일)
└── application-production.yml  운영. 기본값을 주지 않는다
```

운영 프로파일은 `SPRING_PROFILES_ACTIVE=production` 으로 켠다.
**기본값을 일부러 비워 두었다** — 환경변수가 빠지면 기동에 실패한다.
운영에서 조용히 `localhost` 나 빈 비밀번호로 뜨는 것보다 못 뜨는 편이 낫다.

비밀은 어느 yml 에도 적지 않는다. 파일로 두어야 하면 `application-secret.yml`
(gitignore 대상)을 만든다.

## 검증

프로젝트 하나가 그린이 된 뒤 다음으로 넘어간다. 깨진 채로 진행하지 않는다.

| 대상 | 명령 |
|---|---|
| api | `./gradlew build` |
| admin · tenant | `npm run lint && npm run typecheck && npm run build` |
| widget | `npm run typecheck && npm test && npm run build && npm run size` |

## 먼저 읽을 것

| 무엇 | 어디 |
|---|---|
| 프로젝트 정책·핵심 결정·마일스톤 | [CLAUDE.md](CLAUDE.md) |
| 기획서 분석과 스택 선택 근거 | [docs/intake.md](docs/intake.md) |
| DB 설치·생성 (서버 이전 시) | [docs/postgre-db-guide.md](docs/postgre-db-guide.md) |
| API 계약 (서버·클라이언트 단일 기준) | [docs/architecture/api-contracts.md](docs/architecture/api-contracts.md) |
| 기획서 | [docs/plan/](docs/plan/) |
| 개선 백로그 | [docs/IMPROVEMENTS.md](docs/IMPROVEMENTS.md) |

`docs/prototype/`의 HTML은 **시각 참조 전용**이다. 마크업이나 인라인 스타일을 복사하지 않는다.
