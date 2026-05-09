# Security Policy

## Supported Versions

Only the latest release is actively maintained. Security fixes are applied to the current `main` branch and released as a new tagged version.

| Version | Supported |
|---------|-----------|
| Latest  | ✅        |
| Older   | ❌        |

## Reporting a Vulnerability

Please **do not** open a public GitHub issue for security vulnerabilities.

Report vulnerabilities privately via [GitHub Security Advisories](https://github.com/RaddadZ/ntfy-frwrdr/security/advisories/new). You will receive a response within 7 days. If the issue is confirmed, a fix will be published as quickly as possible with a new tagged release.

## Signing

All official APKs published in GitHub Releases are signed with a consistent private key. The signing key is never changed between releases. If you receive an APK that cannot be installed over a previous version due to a signature mismatch, treat it as potentially tampered.

- APKs are built and signed in GitHub Actions using a key stored as an encrypted Actions secret.
- The keystore is **not** committed to the repository.

## On-device Security

- All secrets (server URL, topic, access token) are stored using `EncryptedSharedPreferences` with AES-256-GCM via AndroidX Security Crypto.
- Outbound traffic is HTTPS-only except for RFC 1918 private ranges and loopback addresses.
- `android:allowBackup="false"` — the app's data cannot be extracted via ADB or cloud backup.
- R8/ProGuard is enabled on all release builds.
- No self-update mechanism exists. Updates are delivered exclusively through the distribution channel (GitHub Releases, F-Droid, or IzzyOnDroid).
