#!/usr/bin/env bash
set -euo pipefail

IMAGE_NAME="android-playground-builder"
CONTAINER_NAME="android-builder-$$"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
OUTPUT_DIR="${PROJECT_DIR}/build-output"

echo "==> Building Docker image (first run may take a few minutes)…"
docker build -t "${IMAGE_NAME}" -f "${PROJECT_DIR}/.devcontainer/Dockerfile" "${PROJECT_DIR}"

echo "==> Building APK inside container…"
docker run --rm \
  --name "${CONTAINER_NAME}" \
  -v "${PROJECT_DIR}:/workspace" \
  -v android-playground-gradle-cache:/home/vscode/.gradle \
  -w /workspace \
  "${IMAGE_NAME}" \
  bash -c "
    chmod +x gradlew && \
    curl -sL https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar -o gradle/wrapper/gradle-wrapper.jar && \
    ./gradlew assembleDebug --no-daemon
  "

echo "==> Copying APK…"
mkdir -p "${OUTPUT_DIR}"
cp "${PROJECT_DIR}/app/build/outputs/apk/debug/app-debug.apk" "${OUTPUT_DIR}/app-debug.apk"

echo ""
echo "✅ APK built: ${OUTPUT_DIR}/app-debug.apk"
echo ""
echo "Install on your phone:"
echo "  adb install ${OUTPUT_DIR}/app-debug.apk"
