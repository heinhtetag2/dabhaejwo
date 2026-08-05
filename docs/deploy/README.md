# 배포 — 답해줘

단일 리눅스 서버에 네 프로젝트를 올린다. GitLab shell 러너가 **서버에서 직접 빌드**하고,
TLS·라우팅은 호스트 Apache 가 맡는다. 컨테이너 레지스트리를 쓰지 않는다.

```
                    ┌─ dabhaejwo-api.tagoplus.co.kr ──→ 127.0.0.1:4310  api (컨테이너)
 인터넷 ─→ Apache ──┼─ dabhaejwo-mng.tagoplus.co.kr ──→ 127.0.0.1:4311  admin (컨테이너)
          (TLS)     ├─ dabhaejwo.tagoplus.co.kr ──────→ 127.0.0.1:4312  tenant (컨테이너)
                    └─ dabhaejwo-cdn.tagoplus.co.kr ──→ /srv/dabhaejwo/widget/w.js (파일)
```

**위젯만 컨테이너가 아니다.** 남의 사이트가 `<script src>` 로 불러가는 파일 하나라 서버를
띄울 이유가 없다. CI 가 빌드 이미지에서 산출물을 꺼내 디스크에 놓고 Apache 가 서빙한다.

DB 는 원격 PostgreSQL(pgvector) 을 쓴다 — DB 컨테이너가 없다.

---

## 1. 서버 준비 (한 번만)

```bash
# Docker
sudo apt install docker.io docker-compose-plugin
sudo usermod -aG docker gitlab-runner && sudo systemctl restart gitlab-runner

# 러너 등록 — executor: shell, tag: dabhaejwo-prod

# 위젯 배포 위치 (CI 가 여기에 쓴다)
sudo mkdir -p /srv/dabhaejwo/widget
sudo chown -R gitlab-runner:gitlab-runner /srv/dabhaejwo/widget

# Apache
sudo a2enmod proxy proxy_http proxy_wstunnel rewrite headers deflate ssl
sudo mkdir -p /var/log/apache2/dabhaejwo
sudo cp deploy/apache/*.conf /etc/apache2/sites-available/
sudo a2ensite dabhaejwo-api dabhaejwo-mng dabhaejwo-app dabhaejwo-cdn
sudo systemctl reload apache2

# TLS — vhost 를 보고 certbot 이 :443 을 만든다
sudo certbot --apache
```

> **`proxy_wstunnel` 을 빠뜨리면 알림 소켓만 조용히 죽는다.** 화면은 멀쩡히 뜨고 목록도
> 나오므로(REST + 60초 폴링) 알아채기까지 오래 걸린다.

---

## 2. 서버에 Java·Node 를 깔지 않는다

네 프로젝트 모두 컨테이너 안에서 빌드된다. 검증 잡도 마찬가지다
(`docker run --rm -v $PWD/api:/app eclipse-temurin:21-jdk ./gradlew build`).

서버에 필요한 것은 **Docker 하나**다. 런타임을 서버에 깔기 시작하면 "이 서버에서만 되는 빌드"가
생기고, 서버를 새로 만들 때 무엇이 필요했는지 아무도 모르게 된다.

---

## 3. GitLab CI/CD Variables

> **전체 목록과 "없으면 무슨 일이 나는지"는 [`ci-variables.md`](ci-variables.md) 에 있다.**
> 서버를 준비할 때는 그쪽을 체크리스트로 쓴다. 아래는 요약이다.

Settings → CI/CD → Variables. **Masked + Protected** 권장.

### 없으면 기동에 실패하는 것

운영 프로파일은 기본값을 주지 않는다. 조용히 `localhost` 로 뜨는 것보다 못 뜨는 편이 낫다 —
전자는 몇 시간 뒤 이상한 증상으로 발견되고 후자는 배포 즉시 발견된다.

| 변수 | 설명 |
|---|---|
| `POSTGRES_HOST` `POSTGRES_PORT` `POSTGRES_DB` `POSTGRES_USER` `POSTGRES_PASSWORD` | DB 접속 |
| `JWT_SECRET` | **바뀌면 전 사용자가 로그아웃된다.** 한 번 정하고 고정 |
| `ENCRYPTION_KEY` | 공급사 API 키 암호화 마스터 키(AES-256-GCM). **바꾸면 등록된 키를 복호화할 수 없어 콘솔에서 전부 다시 등록해야 한다** |
| `R2_BUCKET` `R2_ENDPOINT` `R2_ACCESS_KEY_ID` `R2_SECRET_ACCESS_KEY` | 문서 저장소 |
| `CORS_ALLOWED_ORIGINS` | admin·tenant 도메인(쉼표 구분). **위젯 도메인은 넣지 않는다** — 방문자 출처는 미리 알 수 없어 `/api/widget/**` 이 별도로 열려 있고, 실제 판정은 공개 키 + 등록 주소로 한다 |
| `OPS_IP_ALLOWLIST` | 값을 요구하지만 **아직 코드가 읽지 않는다**(아래 §6) |
| `LLM_DEFAULT_PROVIDER` | `GOOGLE`. 실제 키는 여기가 아니라 **운영 콘솔에서 암호화 등록**한다 |

### 프론트 번들에 박히는 것 (빌드 시점)

| 변수 | 설명 |
|---|---|
| `NEXT_PUBLIC_API_BASE_URL` | admin·tenant 가 부를 API 주소 |
| `NEXT_PUBLIC_WIDGET_SRC` | 설치 화면이 업체에게 보여줄 스크립트 주소 (`https://…/w.js`) |
| `WIDGET_API_BASE_URL` | 위젯 번들에 박히는 API 주소. 보통 `NEXT_PUBLIC_API_BASE_URL` 과 같다 (미설정 시 그 값을 쓴다) |

> **런타임에 못 바꾼다.** `NEXT_PUBLIC_*` 과 `VITE_*` 는 빌드 시점에 번들 문자열로 인라인된다.
> 값을 바꾸려면 다시 빌드해야 한다. 컨테이너 환경변수를 고쳐도 아무 일도 일어나지 않는다.

### 메일·기타

`MAIL_HOST` `MAIL_PORT` `MAIL_USERNAME` `MAIL_PASSWORD` `MAIL_FROM` `MAIL_FROM_NAME`
`APP_BASE_URL` `OPS_BASE_URL` (초대·비밀번호 재설정 메일의 절대 주소)
`OPS_SEED_EMAIL` (운영자 시드 주소 — 로그인 2단계 코드가 여기로 온다)
`WIDGET_DEPLOY_DIR` (선택, 기본 `/srv/dabhaejwo/widget`)

---

## 4. 배포 흐름

기본 브랜치에 push → `verify` → `deploy`.

1. **verify** — api `./gradlew build`(테스트 포함), admin·tenant lint·typecheck,
   widget typecheck·test·build·**size**. 깨지면 배포가 시작되지 않는다
2. **deploy** — Variables → `deploy/.env` 생성 → `docker compose up -d --build`
3. 위젯은 이미지를 만들고 `/dist` 를 꺼내 **원자적으로 교체**한다
   (임시 폴더 → `mv`. 문서 루트에 바로 쓰면 복사 도중의 반쪽짜리 파일이 방문자에게 나간다)

`deploy/.env` 는 서버 디스크에만 있고 레포에는 없다(`.gitignore`).

### 첫 배포에서 일어나는 일

Flyway 가 V1~V10 을 순서대로 적용한다. **PostgreSQL 은 DDL 이 트랜잭션이라** 중간에
실패하면 통째로 롤백된다 — 반쯤 만들어진 스키마가 남지 않는다.

이어서 `ddl-auto: validate` 가 엔티티와 스키마를 대조하고, 어긋나면 **기동하지 않는다.**

---

## 5. 확인

```bash
docker compose -f deploy/docker-compose.yml ps        # 3개 up, api healthy
curl -fsS https://dabhaejwo-api.tagoplus.co.kr/actuator/health
curl -fsS https://dabhaejwo-cdn.tagoplus.co.kr/w.js | head -c 60

# 알림 소켓 (핸드셰이크가 101 로 올라가는지)
curl -i -N -H "Connection: Upgrade" -H "Upgrade: websocket" \
     -H "Sec-WebSocket-Version: 13" -H "Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==" \
     -H "Origin: https://dabhaejwo.tagoplus.co.kr" \
     https://dabhaejwo-api.tagoplus.co.kr/ws/notifications
```

101 이 아니라 200·400 이 오면 `proxy_wstunnel` 이 안 켜졌거나 vhost 의 WebSocket 블록이
catch-all `ProxyPass` 아래로 내려간 것이다.

### 롤백

```bash
git revert <commit> && git push     # 파이프라인이 다시 돌며 이전 상태로 빌드
```

이미지 태그가 `latest` 하나뿐이라 **이미지 단위 롤백은 없다.** 커밋을 되돌려 다시 빌드하는
것이 유일한 경로다. 되돌리기 어려운 것은 이미지가 아니라 **마이그레이션**이므로
(Flyway 는 down 스크립트가 없다) 스키마를 바꾸는 배포는 특히 신중해야 한다.

---

## 6. 알고 배포하는 것

| | |
|---|---|
| **운영 콘솔 IP 제한이 앱에 없다** | `OPS_IP_ALLOWLIST` 를 요구하지만 그 값을 읽는 코드가 없다. 실효 차단은 `dabhaejwo-mng.conf` 의 `Require ip` 뿐이다 — **주석을 풀지 않으면 콘솔이 인터넷에 열려 있다**(로그인은 필요하지만 기획서 §8 의 전제와 다르다) |
| **인스턴스 하나를 전제한다** | 알림 소켓 세션·레이트 리밋·학습 워커·스케줄러가 전부 인스턴스 메모리 기준이다. `docker compose up --scale` 로 늘리면 조용히 어긋난다 |
| **무중단 배포가 아니다** | `up -d --build` 는 컨테이너를 교체한다. api 는 재기동에 30~60초가 걸리고 그동안 챗봇이 답하지 않는다 |
| **결제가 미연동이다** | 유료 전환이 문의 티켓으로 접수돼 사람이 처리한다. 토스 키는 `.env` 에 있지만 코드가 읽지 않는다 |
| **알림이 콘솔 밖으로 안 나간다** | 원가 상한 도달이 새벽에 떠도 콘솔을 열기 전까지 모른다 |

전부 `docs/IMPROVEMENTS.md` 에 등록돼 있다.
