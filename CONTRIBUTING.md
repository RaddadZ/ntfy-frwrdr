# Contributing to Notify Forwarder

Thanks for your interest in contributing. This document covers how to build, what the code style expectations are, and the PR checklist.

---

## Building

### Docker (recommended — no local SDK needed)

```bash
bash build.sh
adb install build-output/app-debug.apk
```

### Android Studio

Open the project root in **Android Studio Hedgehog (2023.1.1)** or later. The project uses:

- Kotlin 1.9 / JVM 17
- Jetpack Compose with BOM `2024.02.02`
- compileSdk 34 / minSdk 26

Run the `app` configuration on a device or emulator running Android 8.0+.

### Unit Tests

```bash
./gradlew testDebugUnitTest
```

Tests live in `app/src/test/`. They cover pure logic only (OTP detection, URL security, `redactOtp`) and require no Android emulator.

---

## Code Style

| Area | Convention |
|------|-----------|
| Language | Kotlin only — no Java, no XML layouts |
| UI | Jetpack Compose + Material 3 only |
| Components | Small, private `@Composable` functions within screen files |
| State | `collectAsState` + DataStore/Room flows; no `LiveData` |
| Coroutines | `CoroutineScope` from `rememberCoroutineScope` in UI; `applicationScope` for background |
| Formatting | Standard Kotlin style (4-space indent, 120-char line limit) |

---

## PR Checklist

Before opening a pull request:

- [ ] `./gradlew assembleRelease` passes with no errors or warnings
- [ ] `./gradlew testDebugUnitTest` passes — all tests green
- [ ] No new `@Suppress` annotations without a comment explaining why
- [ ] No new permissions added to `AndroidManifest.xml` without discussion
- [ ] Security-sensitive changes (storage, network, permissions) include a brief explanation in the PR description
- [ ] New settings must be added to both `SettingsRepository` (interface) and `SettingsDataStore` (impl)

---

## Issue Labels

| Label | Use for |
|-------|---------|
| `bug` | Something broken or incorrect |
| `privacy` | Data leaving the device unexpectedly, permissions, encryption |
| `ux` | Visual or interaction improvements |
| `feature` | New capability request |
| `question` | General questions about usage or architecture |

---

## AI-Assisted Development

This project was originally built with AI-assisted tooling. Contributions that use AI tools (Copilot, Cascade, ChatGPT, etc.) are welcome — but all code must still pass the PR checklist above, including human review. If a significant portion of your PR was AI-generated, please note that in the PR description.

---

## License

By contributing, you agree that your contributions will be licensed under the **GNU GPL v3.0**.

Commercial use of this project — including forks and derivatives — is permitted, but any distributed version **must also be open-sourced under the same GPL v3 terms**.
