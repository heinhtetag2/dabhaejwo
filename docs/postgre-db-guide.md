# PostgreSQL 설치·DB 생성 가이드

> 새 리눅스 서버에 이 프로젝트용 DB 를 올릴 때 쓴다.
> 2026-08-04 에 Ubuntu 24.04 서버에 실제로 적용하면서 겪은 것을 그대로 적었다.
> "이렇게 하면 된다" 뿐 아니라 **어디서 걸렸는지**도 남겼다 — 다음에 같은 데서 안 걸리려고.

## 이 프로젝트가 DB 에 요구하는 것

| 항목 | 요구 | 이유 |
|---|---|---|
| PostgreSQL | **14 이상** | `scram-sha-256` 기본, `gen_random_uuid()` 코어 내장 |
| pgvector | **0.5.0 이상** | `knowledge_chunks` 가 HNSW 인덱스를 쓴다. HNSW 가 0.5.0 부터 |
| 인코딩 | UTF8 | 한글 |
| 확장 | `vector`, `pgcrypto` | `V1__init.sql` 첫 두 줄 |

검증된 조합: **PostgreSQL 16 + pgvector 0.6.0**.

임베딩 차원은 `vector(1536)` 이고 HNSW 상한(2000)보다 작다.

---

## 0. 기존 클러스터부터 확인한다

**설치 명령을 치기 전에 이걸 먼저 한다.** 서버에 이미 PostgreSQL 이 있을 수 있고,
그러면 포트도 인증 설정도 기본값이 아니다.

```sh
pg_lsclusters
```

```
Ver Cluster Port Status Owner    Data directory              Log file
16  main    6001 online postgres /var/lib/postgresql/16/main /var/log/postgresql/postgresql-16-main.log
```

실제로 이런 서버였다 — **포트가 5432 가 아니라 6001** 이었다. 클러스터가 이미 있는데
모르고 새로 깔면 `pg_createcluster` 가 다음 번호를 잡아 클러스터가 둘이 되고,
어느 쪽에 붙었는지 헷갈리기 시작한다.

이어서 누가 쓰고 있는지 본다.

```sh
sudo -u postgres psql -c '\l'
sudo ss -ltnp | grep -i postgres
```

**다른 프로젝트 DB 가 보이면 그 클러스터는 공유 자산이다.** 아래 작업 중
`ALTER SYSTEM`, `systemctl restart`, `pg_hba.conf` 전역 수정은 그쪽에도 영향이 간다.
이 문서는 그때마다 표시해 두었다.

기존 클러스터가 조건(PG 14+)을 만족하면 **그대로 쓰는 편이 낫다.** 클러스터를 하나 더
만들면 포트·백업·모니터링이 두 벌이 된다. DB 만 추가하면 격리는 충분하다.

---

## 1. 설치 (기존 클러스터가 없을 때만)

Ubuntu 기본 저장소에도 PostgreSQL 이 있지만, pgvector 를 서버 버전에 맞춰 받으려면
PGDG 저장소가 편하다.

```sh
sudo apt update && sudo apt install -y curl ca-certificates
sudo install -d /usr/share/postgresql-common/pgdg
sudo curl -o /usr/share/postgresql-common/pgdg/apt.postgresql.org.asc \
  --fail https://www.postgresql.org/media/keys/ACCC4CF8.asc

echo "deb [signed-by=/usr/share/postgresql-common/pgdg/apt.postgresql.org.asc] \
https://apt.postgresql.org/pub/repos/apt $(lsb_release -cs)-pgdg main" \
  | sudo tee /etc/apt/sources.list.d/pgdg.list

sudo apt update
sudo apt install -y postgresql-17 postgresql-contrib-17
```

## 2. pgvector

패키지 이름에 서버 메이저 버전이 들어간다. 클러스터가 16 이면 `postgresql-16-pgvector` 다.

```sh
apt-cache policy postgresql-16-pgvector   # 버전을 서버에 맞춰서
sudo apt install -y postgresql-16-pgvector
```

> ⚠️ **공유 서버에서는 `apt upgrade` 를 하지 않는다.** PostgreSQL 패키지가 올라가면서
> 클러스터가 재시작되면 그 위에 붙어 있던 다른 서비스의 연결이 끊긴다.
> 필요한 패키지만 `apt install` 로 집어서 깐다.

---

## 3. 계정과 DB

```sh
sudo -u postgres createuser --pwprompt dabhaejwo
sudo -u postgres createdb --owner=dabhaejwo dabhaejwo
```

- `--pwprompt` 로 비밀번호를 물어보게 한다. `-c "CREATE ROLE … PASSWORD '…'"` 로 넣으면
  **셸 히스토리에 평문으로 남는다.**
- `--owner` 를 반드시 준다. PG 15 부터 `public` 스키마에 아무나 테이블을 못 만든다.
  DB 소유자여야 Flyway 가 테이블을 만들 수 있다.
- 로케일·인코딩 옵션은 뺐다. `template1` 을 따라가므로 기존 클러스터의 기준과 자동으로 맞는다.
  클러스터가 UTF8 이 아닐 때만 `--encoding=UTF8 --template=template0 --locale=C.UTF-8` 을 준다.

비밀번호를 새로 뽑을 때:

```sh
openssl rand -base64 24 | tr -d '/+=' | cut -c1-24
```

`/ + =` 를 빼는 이유가 있다. 이 값이 `.env` 에 들어가는데 `=` 가 섞이면 `KEY=VALUE`
파싱이 어긋난다. `$` `!` 도 셸에서 확장되니 피한다.

### 앱 계정에 superuser 를 주지 않는다

`V1__init.sql` 첫 두 줄이 `CREATE EXTENSION IF NOT EXISTS` 인데 확장 생성에는 보통
superuser 가 필요하다. 앱 계정에 권한을 주는 대신 **미리 만들어 둔다.**
그러면 `IF NOT EXISTS` 가 그냥 통과하고 앱 계정은 평범한 권한으로 남는다.

```sh
sudo -u postgres psql -d dabhaejwo -c 'CREATE EXTENSION IF NOT EXISTS vector;'
sudo -u postgres psql -d dabhaejwo -c 'CREATE EXTENSION IF NOT EXISTS pgcrypto;'
```

`-d dabhaejwo` 를 빼먹지 않는다. 확장은 DB 단위라, 빼면 `postgres` DB 에 붙고
정작 우리 DB 엔 없는 채로 넘어간다.

---

## 4. 타임존

시각은 전부 `timestamptz` 로 저장하니 서버 타임존이 무엇이든 데이터는 안 틀어진다.
다만 psql 로 볼 때마다 변환돼 나와 디버깅이 헷갈리므로 UTC 로 맞춰 둔다.

```sh
sudo -u postgres psql -c "ALTER DATABASE dabhaejwo SET timezone = 'UTC';"
```

> ⚠️ **`ALTER SYSTEM SET timezone` 을 쓰지 않는다.** 그건 클러스터 전역이라 같은 서버의
> 다른 DB 까지 바뀐다. 공유 클러스터에서 남의 서비스 타임존을 바꾸게 된다.
> 이미 실행했다면 `ALTER SYSTEM RESET timezone;` 후 `SELECT pg_reload_conf();`.

---

## 5. 확인

```sh
sudo -u postgres psql -d dabhaejwo -c '\dx'
sudo -u postgres psql -d dabhaejwo -c 'SHOW timezone;'
sudo -u postgres psql -c '\du dabhaejwo'
```

기대값:

- `\dx` → `plpgsql`, `pgcrypto`, `vector`
- `SHOW timezone` → `UTC`
- `\du` → `dabhaejwo` 존재, **Attributes 비어 있음** (superuser 아님 — 맞는 상태)

---

## 6. 앱에서 붙기 — 두 갈래

### (권장) SSH 터널

DB 포트를 인터넷에 열지 않는다. SSH 만 열려 있으면 된다.

**개발 PC 에서** 실행한다. 서버 안에서 하면 포트 충돌로 실패한다:

```sh
ssh -N -L 6001:localhost:6001 root@<서버IP>
```

`-N` 이라 셸이 안 뜨고 터널만 유지된다. 이 창을 켜둔 채로 둔다.
`-L` 의 `localhost` 는 **서버 입장에서의** localhost 라서, 서버가 `listen_addresses` 를
어떻게 두든 상관없이 붙는다.

`.env`:
```
POSTGRES_HOST=localhost
POSTGRES_PORT=6001
```

### 포트를 여는 경우

배포 서버에서 붙는 등 터널이 곤란할 때만.

`postgresql.conf`:
```
listen_addresses = '*'
```

`pg_hba.conf` — **접속 IP 를 박아 넣는다**:
```
# TYPE    DATABASE    USER        ADDRESS          METHOD
hostssl   dabhaejwo   dabhaejwo   203.0.113.7/32   scram-sha-256
```

```sh
sudo ufw allow from 203.0.113.7 to any port 6001 proto tcp
sudo systemctl reload postgresql@16-main
```

`hostssl` 은 SSL 없는 접속을 거부한다. 공인망을 지나므로 비밀번호가 평문으로 흐르면 안 된다.
JDBC 쪽엔 `?sslmode=require` 를 붙인다.

> ⚠️ **`pg_hba` 는 먼저 맞는 줄이 이긴다.** 아래쪽에 `host all all 0.0.0.0/0 md5` 같은
> catch-all 이 있으면 우리 줄을 위에 넣어도 소용없다 — 다른 IP 에서 온 요청은 우리 줄의
> 주소가 안 맞아 건너뛰고 catch-all 에 걸린다. **catch-all 자체를 걷어내야 한다.**
> 공유 클러스터라면 다른 서비스가 그 줄에 의존하는지 먼저 확인한다:
> ```sh
> sudo -u postgres psql -c "SELECT datname, usename, client_addr FROM pg_stat_activity WHERE client_addr IS NOT NULL;"
> ```

`reload` 는 재시작이 아니라 기존 연결이 안 끊긴다. `pg_hba.conf` 와 `postgresql.conf`
대부분은 reload 로 충분하고, `listen_addresses` 만 restart 가 필요하다.

---

## 7. 사전 점검과 기동

접속 정보를 `.env` 에 넣는다. **`.env.local` 이 아니라 `.env` 다** — Spring Boot 는
`DotenvEnvironmentPostProcessor` 로 저장소 루트의 `.env` 만 읽는다.

```sh
# 드라이버 jar 는 ./gradlew build 후 Gradle 캐시에 있다
java --class-path <postgresql.jar> scripts/DbProbe.java \
     "$POSTGRES_HOST" "$POSTGRES_PORT" "$POSTGRES_DB" "$POSTGRES_USER" "$POSTGRES_PASSWORD"
```

읽기 전용이다. 연결·버전·확장 설치 여부·기존 테이블을 확인하고 exit 0/1 을 낸다.

```sh
cd api && ./gradlew bootRun
```

Flyway 가 `V1__init.sql` 을 적용해 27개 테이블과 감사 불변 트리거를 만든다.
`ddl-auto: validate` 라 엔티티 매핑이 어긋나면 그 자리에서 기동이 실패한다.

PostgreSQL 은 DDL 이 트랜잭션이므로 마이그레이션이 중간에 실패해도 통째로 롤백된다.
반쯤 만들어진 테이블이 남지 않는다. 원인을 고치고 다시 띄우면 된다.
`flyway_schema_history` 에 실패 기록이 남았으면 `flyway repair` 로 지운다.

---

## 겪은 문제들

### `password authentication failed for user "postgres"`

`sudo -u postgres` 는 보통 `peer` 인증이라 비밀번호를 안 묻는다. 묻는다면 그 클러스터의
`pg_hba.conf` 가 이미 손대어진 것이다 — 즉 **남이 쓰던 클러스터**일 가능성이 높다.
0단계로 돌아가 확인한다.

임시로 뚫으려면 (백업 먼저):

```sh
sudo cp /etc/postgresql/16/main/pg_hba.conf /etc/postgresql/16/main/pg_hba.conf.bak
sudo sed -i '0,/^local\s\+all\s\+postgres\s\+md5/s//local   all             postgres                                peer/' \
  /etc/postgresql/16/main/pg_hba.conf
sudo systemctl reload postgresql@16-main
```

### `Enter password for new role:` 에서 멈춤

기존 비밀번호를 맞히는 게 아니라 **지금 만드는 역할에 지정할** 값을 입력하는 것이다.
아무거나 정하면 된다. 입력해도 화면에 안 보이는 게 정상이고 확인용으로 한 번 더 묻는다.

### `Connection refused: getsockopt`

인증 이전, **TCP 단계에서 거부**된 것이다. 계정·비밀번호 문제가 아니다.
서버가 `0.0.0.0:<포트>` 로 듣고 있는데도 이러면 중간에서 막는 것이다.

포트별로 어디까지 닿는지 본다:

```powershell
foreach ($p in 6001, 22) { Test-NetConnection -ComputerName <서버IP> -Port $p }
```

22 는 되는데 DB 포트만 안 되면 방화벽(`ufw status`) 또는 **공유기 NAT 포워딩**이다.
`ufw` 가 `inactive` 인데도 막히면 호스트가 아니라 공유기다.

### `localhost:5432 refused` 인데 원격 DB 를 설정했다

`.env` 가 안 읽힌 것이다. 앱이 `application-local.yml` 의 기본값으로 떨어졌다는 뜻이다.
키 이름이 맞는지(`POSTGRES_*`), 파일 위치가 저장소 루트 `.env` 가 맞는지 본다.
`.env.local` 에 적으면 Spring 쪽은 읽지 않는다.

### 터널이 `Address already in use`

터널 명령을 서버 안에서 실행한 것이다. **개발 PC 에서** 실행해야 한다.
서버에서는 그 포트를 PostgreSQL 본인이 이미 쓰고 있다.

---

## 참고

- 접속 정보는 `.env` 에만 둔다. `.env.example` · `application*.yml` · 커밋에 절대 넣지 않는다.
- 스키마의 진실은 [`api/src/main/resources/db/migration/V1__init.sql`](../api/src/main/resources/db/migration/V1__init.sql)
- 사전 점검 도구는 [`scripts/DbProbe.java`](../scripts/DbProbe.java)
- 실행 절차 전반은 [`README.md`](../README.md)
