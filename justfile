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

# Clean build outputs
clean:
    ./gradlew clean
