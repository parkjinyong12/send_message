## Send Message Service (Spring Boot)

Spring Boot 기반 Slack 메시지 전송 마이크로서비스. 환경/백그라운드 구성이 용이하고, 인증·회복성·관측을 기본 제공하도록 설계했습니다.

### 개요
- 언어/런타임: Java 17, Spring Boot 3.x
- 빌드/런: Maven, Docker 멀티스테이지, docker compose
- 보안: JWT 리소스 서버 검증(권장) 또는 API Key(옵션)
- Slack 전송: Incoming Webhook(우선) → Bot 토큰(chat.postMessage)
- 관측: Spring Actuator (health, prometheus)
- 회복성: Resilience4j 레지스트리 Bean 기반(CircuitBreaker/RateLimiter)

### 디렉터리
- 서비스: `components/send_message/`
  - Spring Boot 앱, `Dockerfile`, `docker-compose.yml`, `.env.example`
- 인증서버(별도): `components/issuer/`
  - Keycloak `docker-compose.yml`, `keycloak/realm-export.json`

### 환경 변수(.env)
아래 키를 필요한 것만 설정하세요.
```
# 인증: 둘 중 하나 사용
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI=
# 또는
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=

# API Key 보호(옵션: issuer 미설정 시 대체)
API_KEY=

# Slack: 둘 중 하나 사용
SLACK_WEBHOOK_URL=
SLACK_BOT_TOKEN=

# JVM 옵션(옵션)
JAVA_OPTS=-Xms256m -Xmx512m
```
- macOS/Windows에서 로컬 Keycloak을 쓰면 컨테이너는 `localhost`에 접근 불가 → `host.docker.internal` 활용 권장
  - 예: `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI=http://host.docker.internal:8082/realms/send-message/protocol/openid-connect/certs`

### 실행
1) 인증서버(선택: 로컬 Keycloak)
```
cd components/issuer
docker compose up -d
# 콘솔: http://localhost:8082 (admin/admin)
# realm: send-message, client: send-message-client, scope: msg:send
```
2) 서비스
```
cd components/send_message
cp .env.example .env  # 값 채우기
docker compose up -d --build
# 헬스: http://localhost:8080/health  → {"status":"ok"}
```

### 보안
- 기본: JWT 리소스 서버 검증
  - POST `/api/v1/messages/send`에 `SCOPE_msg:send` 필요
  - issuer-uri는 토큰 `iss`와 완전히 동일해야 하며, 컨테이너에서 접근 가능해야 합니다
  - 로컬 개발은 jwk-set-uri 권장
- 옵션: API Key(X-API-Key)
  - `.env`에 `API_KEY`가 설정되어 있고 JWT 미설정인 경우 적용

### Slack 전송 모드
- Incoming Webhook (권장)
  - `.env`에 `SLACK_WEBHOOK_URL`이 있으면 웹훅으로 전송(요청의 `channel` 값은 무시)
- Bot 토큰 (대안)
  - `.env`에 `SLACK_BOT_TOKEN` 설정 시 `chat.postMessage`
  - 채널명 입력 시 내부에서 채널 ID 조회(권한 필요). 가능하면 채널 ID 사용 권장

### API 계약
- POST `/api/v1/messages/send`
  - 요청(JSON):
    ```json
    {
      "channel": "general",
      "text": "안녕하세요!",
      "blocks": [ { "type": "section", "text": { "type": "mrkdwn", "text": "*메시지*" } } ],
      "mrkdwn": true
    }
    ```
  - 응답(JSON): `{ "ok": true, "channel": "webhook|Cxxxx", "ts": "...", "error": null }`
- GET `/health` → `{ "status": "ok" }`

### 회복성(Resilience4j)
- 레지스트리 Bean 기반(CircuitBreakerRegistry, RateLimiterRegistry)
  - yml에 인스턴스를 나열하지 않고, 코드에서 목적지 키(webhook/bot)로 `slack-{dest}` 이름을 동적 사용
  - 기본값은 Bean 생성 시 base 설정, 운영은 환경변수/프로파일로 오버라이드 권장
- 기본 동작
  - RateLimiter: 초당 10건, 대기 1s
  - CircuitBreaker: 윈도우 50, 실패율 50%, 오픈 10s

### 트러블슈팅
- 401 Unauthorized: issuer-uri≠토큰 iss, jwk-uri 접근 불가, 헤더 누락 등
- 403 Forbidden: 토큰에 `msg:send` 스코프 없음
- Slack 오류: 웹훅 URL/봇 권한/채널 초대/워크스페이스 정책 확인
- 로컬 Keycloak + 컨테이너: `host.docker.internal` 사용

### 참고
- 메트릭/프로메테우스: `/actuator/prometheus`
- Swagger UI: `/swagger-ui.html` 