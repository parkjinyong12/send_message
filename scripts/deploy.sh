#!/usr/bin/env bash
set -euo pipefail

# 구성 가능한 변수 (환경변수로 오버라이드 가능)
IMAGE_NAME=${IMAGE_NAME:-send-message}
CONTAINER_NAME=${CONTAINER_NAME:-send-message}
APP_PORT=${APP_PORT:-8000}
REMOTE_HOST=${REMOTE_HOST:-}
REMOTE_USER=${REMOTE_USER:-}
REMOTE_SSH_PORT=${REMOTE_SSH_PORT:-22}
REMOTE_APP_DIR=${REMOTE_APP_DIR:-/opt/send-message}

# 로컬 .env 파일 (Slack 토큰 등 비밀 값 포함)
ENV_FILE_PATH=${ENV_FILE_PATH:-.env}

if [[ -z "${REMOTE_HOST}" || -z "${REMOTE_USER}" ]]; then
  echo "REMOTE_HOST와 REMOTE_USER를 환경변수로 설정하세요. 예) REMOTE_HOST=1.2.3.4 REMOTE_USER=ubuntu make deploy"
  exit 1
fi

if [[ ! -f "${ENV_FILE_PATH}" ]]; then
  echo "${ENV_FILE_PATH} 파일이 존재하지 않습니다. SLACK_BOT_TOKEN 등이 포함된 .env를 준비하세요."
  exit 1
fi

echo "[1/5] 로컬 Docker 이미지 빌드..."
docker build -t "${IMAGE_NAME}:latest" .

SSH="ssh -p ${REMOTE_SSH_PORT} ${REMOTE_USER}@${REMOTE_HOST}"
SCP="scp -P ${REMOTE_SSH_PORT}"

echo "[2/5] 원격 디렉터리 준비..."
$SSH "mkdir -p ${REMOTE_APP_DIR}"

echo "[3/5] 환경 변수 파일 업로드 (.env)"
$SCP "${ENV_FILE_PATH}" "${REMOTE_USER}@${REMOTE_HOST}:${REMOTE_APP_DIR}/.env"

echo "[4/5] 이미지 전송 및 로드 (스트리밍)"
docker save "${IMAGE_NAME}:latest" | bzip2 | $SSH 'bunzip2 | docker load'

echo "[5/5] 컨테이너 재시작"
$SSH "docker rm -f ${CONTAINER_NAME} >/dev/null 2>&1 || true && \
      docker run -d --name ${CONTAINER_NAME} --restart unless-stopped \
      -p ${APP_PORT}:8000 --env-file ${REMOTE_APP_DIR}/.env ${IMAGE_NAME}:latest"

echo "배포 완료: http://${REMOTE_HOST}:${APP_PORT}" 