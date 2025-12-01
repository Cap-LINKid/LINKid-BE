#!/bin/bash

IMAGE_NAME="allday1234/linkd-be"
IMAGE_TAG="latest"
TIMESTAMP_TAG=$(date +"%Y%m%d-%H%M%S")

echo "🔧 Docker Buildx 확인 중..."

# buildx builder 존재하는지 확인
BUILDER_NAME="multiarch_builder"

if ! docker buildx inspect "$BUILDER_NAME" >/dev/null 2>&1; then
  echo "🔨 buildx builder 생성: $BUILDER_NAME"
  docker buildx create --name "$BUILDER_NAME" --use
else
  echo "✔ buildx builder 존재함: $BUILDER_NAME"
  docker buildx use "$BUILDER_NAME"
fi

echo "📦 Docker 이미지 빌드 시작..."

docker buildx build \
  --platform linux/amd64 \
  --no-cache \
  -t "${IMAGE_NAME}:${IMAGE_TAG}" \
  -t "${IMAGE_NAME}:${TIMESTAMP_TAG}" \
  . \
  --push

echo "🎉 완료!"
echo "Pushed → ${IMAGE_NAME}:${IMAGE_TAG}"
echo "Pushed → ${IMAGE_NAME}:${TIMESTAMP_TAG}"