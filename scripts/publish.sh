#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   IMAGE_REF=registry.example.com/namespace/send-message:TAG \
#   ./scripts/publish.sh
#
# Optional:
#   DOCKER_BUILD_PLATFORM=linux/amd64,linux/arm64 (default: host platform)
#   DOCKER_BUILD_ARGS="--no-cache" etc.
#
# Login example (interactive):
#   docker login registry.example.com

IMAGE_REF=${IMAGE_REF:-}
PLATFORMS=${DOCKER_BUILD_PLATFORM:-}
BUILD_ARGS=${DOCKER_BUILD_ARGS:-}

if [[ -z "${IMAGE_REF}" ]]; then
  echo "IMAGE_REF를 환경변수로 지정하세요. 예: IMAGE_REF=docker.io/username/send-message:latest"
  exit 1
fi

# Ensure buildx builder
if ! docker buildx ls >/dev/null 2>&1; then
  docker buildx create --use --name send-message-builder >/dev/null
fi

set -x
if [[ -n "${PLATFORMS}" ]]; then
  docker buildx build \
    --platform "${PLATFORMS}" \
    -t "${IMAGE_REF}" \
    --push \
    ${BUILD_ARGS} \
    .
else
  docker buildx build \
    -t "${IMAGE_REF}" \
    --push \
    ${BUILD_ARGS} \
    .
fi
set +x

echo "Pushed: ${IMAGE_REF}" 