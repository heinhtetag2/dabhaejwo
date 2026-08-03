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

```sh
cd api    && ./gradlew bootRun     # :8080 — 기동 시 Flyway가 스키마를 만든다
cd admin  && npm run dev           # :3000 운영 콘솔
cd tenant && npm run dev           # :3000 업체 대시보드 (포트 충돌 시 -p 로 변경)
cd widget && npm run dev           # /demo/index.html 에서 실제 임베드 확인
```

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
