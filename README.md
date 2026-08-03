# 답해줘

업체가 홈페이지에 스크립트 한 줄을 붙이면, 그 사이트를 학습한 챗봇이 방문자 질문에 답해주는 멀티 테넌트 SaaS.

```
dabhaejwo/
├── api/       Spring Boot · Java 21 · MariaDB 11.8 LTS (VECTOR)
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
| MariaDB | **11.8 LTS 이상** | 그 아래 버전에는 `VECTOR` 타입이 없어 스키마가 만들어지지 않는다 |
| Docker | 선택 | 있으면 통합 테스트가 돌아간다 |

```sh
cp .env.example .env    # 값을 채운다. .env 는 커밋되지 않는다
```

```sh
docker run -d --name dabhaejwo-db -p 3306:3306 \
  -e MARIADB_DATABASE=dabhaejwo -e MARIADB_USER=dabhaejwo \
  -e MARIADB_PASSWORD=... -e MARIADB_ROOT_PASSWORD=... \
  mariadb:11.8
```

### DB 준비 — 마이그레이션 전에 반드시 점검한다

**MariaDB 는 DDL 이 트랜잭션이 아니다.** `V1__init.sql` 이 중간에 실패하면 앞쪽 테이블은
만들어진 채 남고 Flyway 는 되돌리지 못한다. 반쯤 만들어진 스키마와 실패한 이력을
손으로 치워야 하므로, 서버 상태를 먼저 확인한 뒤에 마이그레이션을 건다.

```sh
# 드라이버 jar 경로는 ./gradlew build 후 Gradle 캐시에서 찾는다
java --class-path <mariadb-java-client.jar> scripts/DbProbe.java \
     "$MARIADB_HOST" "$MARIADB_PORT" "$MARIADB_DB" "$MARIADB_USER" "$MARIADB_PASSWORD"
```

버전·VECTOR 지원·기존 테이블 유무를 확인하고, 적용해도 되는 상태면 exit 0 을 낸다.
**exit 0 을 받은 뒤에** 앱을 띄운다 — Flyway 가 기동 시 마이그레이션을 적용한다.

```sh
cd api && ./gradlew bootRun
```

앱에도 같은 검사가 들어 있다(`FlywayVersionGuardConfig`). 버전이 못 미치면
Flyway 가 스키마 이력 테이블을 만들기도 전에 멈추므로 DB 는 손도 대지 않은 상태로 남는다.

#### 마이그레이션이 도중에 실패했다면

MariaDB 는 DDL 이 트랜잭션이 아니라 되돌리기가 자동으로 되지 않는다.
절반만 만들어진 테이블과 `flyway_schema_history` 의 실패 기록이 남는다.
개발 DB 라면 전부 지우고 다시 시작하는 편이 빠르다.

```sql
SET FOREIGN_KEY_CHECKS = 0;
-- information_schema 로 목록을 뽑아 DROP TABLE 을 만든다
SELECT CONCAT('DROP TABLE IF EXISTS `', table_name, '`;')
FROM information_schema.tables WHERE table_schema = DATABASE();
SET FOREIGN_KEY_CHECKS = 1;
```

**운영 DB 에서는 이렇게 하지 않는다.** 실패 지점까지 적용된 내용을 확인하고
되돌리는 마이그레이션을 별도로 작성한 뒤 `flyway repair` 로 이력을 정리한다.

#### Ubuntu 24.04 기본 패키지는 10.11 이다

`apt install mariadb-server` 로 깔면 10.11 이 오고, 여기에는 `VECTOR` 타입이 없다.
11.8 LTS 는 MariaDB 공식 저장소를 추가해야 한다.

```sh
curl -LsS https://r.mariadb.com/downloads/mariadb_repo_setup \
  | sudo bash -s -- --mariadb-server-version="11.8"
sudo apt update && sudo apt install mariadb-server
sudo mariadb-upgrade      # 메이저 버전을 건너뛰므로 반드시 실행
```

10.11 → 11.8 은 메이저 버전을 여러 개 넘는 업그레이드다. 같은 서버에 다른 DB 가 있으면
먼저 백업할 것. 명령은 MariaDB 공식 문서로 한 번 더 확인하는 편이 안전하다.

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
| API 계약 (서버·클라이언트 단일 기준) | [docs/architecture/api-contracts.md](docs/architecture/api-contracts.md) |
| 기획서 | [docs/plan/](docs/plan/) |
| 개선 백로그 | [docs/IMPROVEMENTS.md](docs/IMPROVEMENTS.md) |

`docs/prototype/`의 HTML은 **시각 참조 전용**이다. 마크업이나 인라인 스타일을 복사하지 않는다.
