# Project task runner — mirrors CI pipeline locally
# Install: https://github.com/casey/just

# Full CI pipeline (matches GitHub Actions): test → lint → release APK
ci: test lint release

# Run unit tests
test:
    ./gradlew testDebugUnitTest

# Run lint checks
lint:
    ./gradlew lintDebug

# Validate without building (test + lint)
check: test lint

# Quick debug build (no checks — for dev iteration)
build:
    ./gradlew assembleDebug

# Release build (R8/ProGuard enabled)
release:
    ./gradlew assembleRelease

# Version-aware release build using latest git tag
release-tag:
    #!/usr/bin/env bash
    TAG=$(git describe --tags --abbrev=0 2>/dev/null || echo "v0.0.0")
    CODE=$(git tag -l 'v*' | wc -l)
    export APP_VERSION_NAME="${TAG#v}"
    export APP_VERSION_CODE="$CODE"
    echo "Building version $APP_VERSION_NAME (code $APP_VERSION_CODE)"
    ./gradlew assembleRelease

# Clean build outputs
clean:
    ./gradlew clean
