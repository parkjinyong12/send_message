## Send Message Service (Spring Boot)

Spring Boot 기반 Slack 메시지 전송 마이크로서비스. 본 리포지토리에서는 애플리케이션 이미지를 빌드/퍼블리시하는 데에 집중합니다(기동/운영은 외부 인프라에서 수행).

### 개요
- 언어/런타임: Java 17, Spring Boot 3.x
- 빌드: Maven + Jib (Docker 데몬 불필요)
- 보안: JWT 리소스 서버 검증(권장) 또는 API Key(옵션)
- Slack 전송: Incoming Webhook(우선) → Bot 토큰(chat.postMessage)
- 회복성: Resilience4j 레지스트리 Bean 기반

### 빌드/퍼블리시
- Maven settings.xml에 레지스트리/프로필 설정(publish) 후 실행
  ```bash
  mvn -Ppublish install
  # 결과 이미지: ${image.registry}/${image.namespace}/send-message:${image.tag}
  ```

### 런타임(외부 인프라에서 처리)
- 컨테이너 기동/구성, 비밀 주입, 네트워킹은 상위 인프라 레포 혹은 배포 시스템에서 관리하세요(K8s/Compose 등).
- 런타임 필요한 환경변수(예)
  - `SLACK_WEBHOOK_URL` 또는 `SLACK_BOT_TOKEN`
  - `spring.security.oauth2.resourceserver.jwt.issuer-uri` 또는 `...jwk-set-uri`
  - `JAVA_OPTS` 등

### 트러블슈팅
- Jib 푸시 실패: settings.xml 프로필/크리덴셜/레지스트리 접근 확인
- JWT 401: issuer/jwks 접근성, 토큰 iss 불일치 확인
- Slack 오류: 권한(chat:write), 채널 접근/초대, 웹훅 URL 확인

### 참고
- 이 프로젝트는 Cursor로 개발되었습니다. 