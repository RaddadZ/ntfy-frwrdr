# F-Droid Build Notes

## Application ID

`io.github.raddadz.ntfyforwarder`

## Build Flavor

There is a single build variant — no proprietary flavor exists. The F-Droid build is **the same as the upstream release build**:

```
./gradlew assembleRelease
```

## Toolchain

| Tool | Version |
|------|---------|
| JDK | 17 (Temurin) |
| Android Gradle Plugin | 8.2.x (via `build.gradle.kts`) |
| Gradle | 8.5 (via `gradle/wrapper/gradle-wrapper.properties`) |
| Kotlin | 1.9 |
| compileSdk | 34 |
| minSdk | 26 (Android 8.0) |
| targetSdk | 34 |

The `gradle-wrapper.jar` is committed to the repository at `gradle/wrapper/gradle-wrapper.jar` and does not require a network fetch at build time.

## Dependencies

All dependencies are fetched from `google()` and `mavenCentral()` repositories. There are no local `.aar` or `.jar` binary blobs bundled in the source tree.

All runtime dependencies are FOSS:

| Library | License |
|---------|---------|
| AndroidX / Jetpack | Apache 2.0 |
| Jetpack Compose | Apache 2.0 |
| OkHttp | Apache 2.0 |
| Room | Apache 2.0 |
| DataStore | Apache 2.0 |
| WorkManager | Apache 2.0 |
| AndroidX Security Crypto | Apache 2.0 |
| Kotlin / Coroutines | Apache 2.0 |

## Anti-features

None declared. The app:

- Contains no advertising
- Contains no analytics or crash-reporting SDKs
- Does not track users
- Has no self-update mechanism
- Uses no proprietary network services (all traffic goes to a user-configured ntfy server)

## Signing & Reproducibility

Release APKs are signed in GitHub Actions CI using a private key stored as an encrypted Actions secret. The keystore is not committed to the repository. The build is not yet reproducible in the [Reproducible Builds](https://reproducible-builds.org/) sense; APKs must be obtained from GitHub Releases.

## Version Scheme

- `versionName` — derived from the Git tag (e.g. `v1.2.3` → `1.2.3`)
- `versionCode` — count of all `v*` tags at build time (monotonically increasing)
