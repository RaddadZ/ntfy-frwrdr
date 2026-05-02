# Notify Forwarder

[![Build](https://github.com/RaddadZ/ntfy-frwrdr/actions/workflows/build.yml/badge.svg)](https://github.com/RaddadZ/ntfy-frwrdr/actions/workflows/build.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Min SDK](https://img.shields.io/badge/Android-8.0%2B%20(API%2026)-green.svg)](https://developer.android.com/about/versions/oreo)

A **privacy-first Android notification and call forwarder** that pushes your phone's notifications and incoming calls to a self-hosted [ntfy](https://ntfy.sh) server — with no hardcoded app list, no cloud dependency, and no third-party analytics.

> **For privacy-conscious users:** Nothing leaves your device except what you explicitly configure to be forwarded, and only to _your own_ server. See [Privacy](#privacy) below.

[![Get it on Obtainium](https://raw.githubusercontent.com/ImranR98/Obtainium/main/assets/graphics/badge_obtainium.png)](http://apps.obtainium.imranr.dev/redirect.html?r=obtainium://add/https://github.com/RaddadZ/ntfy-frwrdr)

---

## Features

- **Dynamic app selection** — searchable picker of all installed apps; no hardcoded list
- **Rich notifications** — per-app emoji tags, icons, markdown, OTP copy buttons, open-app deep links
- **OTP / verification code handling** — forward as-is, redact codes, or skip entirely
- **Incoming call forwarding** — optional contact name resolution
- **Heartbeat health check** — periodic "I'm alive" ping (1 h–24 h interval)
- **Smart log retention** — auto-delete on success, configurable retention (1 h–1 w)
- **Privacy focused settings UX** — permission status cards, section composables, back-arrow navigation
- **Security hardened** — HTTPS enforcement, `EncryptedSharedPreferences` (AES-256-GCM) for all secrets, R8/ProGuard, `android:allowBackup="false"`
- **Rate-limiting & dedup** — 30 notifications/min cap, 5 s deduplication window

---

## Privacy

| Data | Where it goes | When |
|------|--------------|------|
| Notification title + body | Your ntfy server only | When app is in your forwarding list |
| Incoming caller number/name | Your ntfy server only | When call forwarding is enabled |
| OTP / verification codes | Redacted or skipped (your choice) | Configurable in Settings |
| App list | Never leaves device | Used only for the in-app picker |
| Access token / topic / server URL | Stored on-device with AES-256-GCM | Never transmitted |

**Nothing is sent to third parties. No analytics, no crash reporting, no ads.**

---

## Requirements

- Android **8.0+ (API 26)**
- A self-hosted **ntfy server** (or ntfy.sh) accessible over HTTPS (local HTTP also accepted)
- Docker Desktop — for building without installing the Android SDK on your machine

---

## Build & Install

### Dev Container (recommended)

Open the project in any editor with [Dev Containers](https://containers.dev/) support (VS Code, Codespaces, etc.). The container includes JDK 17, Android SDK, and [`just`](https://github.com/casey/just).

```bash
just ci        # test → lint → release APK (mirrors GitHub Actions)
just build     # quick debug APK (no checks)
just check     # test + lint only
just clean     # wipe build outputs
```

### Manual build

```bash
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # release APK (R8/ProGuard)
```

### Install on device

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Android Studio

Open the project in Android Studio Hedgehog (2023.1.1) or later and run the `app` configuration.

---

## CI

GitHub Actions runs on every push to `main` and on pull requests:

1. **Unit tests** (`testDebugUnitTest`)
2. **Lint** (`lintDebug`)
3. **Release build** (`assembleRelease`)
4. **Sign & upload APK** (main branch only — signed with repository secrets)

The pipeline mirrors `just ci` locally, so running `just ci` before pushing catches the same failures.

---

## First Launch Setup

1. **Open Settings** (⚙ top-right) — grant all permission cards:
   - **Notification Listener** — required for forwarding app notifications
   - **Battery Unrestricted** — prevents Android from killing the service
   - **Phone & Call Log** — required for call forwarding (optional)
   - **Contacts** — optional; resolves caller names
2. **Configure ntfy Server** — enter your server URL, topic, and optional access token → tap **Send Test**
3. **Add Apps** — go to **Apps** tab → tap **Add App** → search and select apps to forward
4. **Configure each app** — tap any app card to set its label, emoji tag, icon URL, or open URL
5. **OTP Handling** — in Settings, choose Forward / Redact / Skip for verification codes
6. **Log Settings** — set retention period and auto-delete behaviour
7. **Health Check** — optionally enable a heartbeat notification at a fixed interval

---

## Architecture

```
app/src/main/java/com/playground/android/
├── MainActivity.kt                  # Compose UI: Apps + Log tabs, Settings via top bar
├── NotifyForwarderApp.kt            # Application class, in-memory caches, WorkManager
├── SettingsRepository.kt            # Interface for all settings (testable abstraction)
├── SettingsDataStore.kt             # DataStore + EncryptedSharedPreferences impl
├── NtfyNotifier.kt                  # HTTP POST to ntfy, rate-limiting, retry, logging
├── PermissionState.kt               # Runtime permission launchers (AtomicBoolean-safe)
├── NotificationForwarderService.kt  # NotificationListenerService — OTP detection, dedup
├── IncomingCallScreener.kt          # CallScreeningService — call forwarding
├── HeartbeatWorker.kt               # WorkManager periodic heartbeat
├── AppDatabase.kt / *Dao / *Entity  # Room database (registered apps + log entries)
└── ui/
    ├── screens/
    │   ├── SettingsScreen.kt        # Settings: permissions, OTP, ntfy config, log, heartbeat
    │   ├── AppsScreen.kt            # App list, swipe-to-delete, add picker
    │   └── LogScreen.kt             # Forwarded notification log
    └── components/
        ├── StatusCard.kt            # Permission status card (granted/not granted)
        ├── SettingsToggleItem.kt    # Flat ListItem + Switch
        ├── SettingsSegmentedButton.kt
        ├── EditableFieldGroup.kt
        └── SectionHeader.kt
```

---

## Security Notes

- **All secrets encrypted at rest** — server URL, topic, and access token are stored in `EncryptedSharedPreferences` with AES-256-GCM via `MasterKey.Builder`
- **HTTPS enforced** — plain HTTP is blocked except for RFC 1918 private ranges and loopback
- **No `QUERY_ALL_PACKAGES`** — app picker uses a scoped `<queries>` manifest block (launcher apps only)
- **Backup disabled** — `android:allowBackup="false"` prevents ADB/cloud backup of secrets
- **R8 / ProGuard** enabled on release builds

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md).

---

## Development Process

This application was built using **AI-assisted development** with continuous human oversight.

| Role | Responsibility |
|------|---------------|
| **AI** | Code generation, refactoring, test scaffolding, documentation drafting |
| **Human** | Architecture decisions, code review, manual testing, UX validation, security audit |

The human contributor is a **senior software engineer**. Every AI-generated change was reviewed for correctness, security, and adherence to SOLID / DRY / KISS / YAGNI principles before being committed.

Features were built incrementally, then subjected to a multi-phase quality review covering bugs, security, code cleanup, architecture, and documentation.

---

## License

Copyright (C) raddadz

This program is free software: you can redistribute it and/or modify it under the terms of the **GNU General Public License v3.0** as published by the Free Software Foundation.

Commercial use is permitted, but **any distributed derivative must also be open-sourced under GPL v3**.

See [LICENSE](LICENSE) for the full text.
