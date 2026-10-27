# Stack Profile

What we run, so the evaluator can judge fit against reality rather than taste.

## Runtimes

- **JVM:** Java 21 (Temurin) on every backend service. Nothing older than 17 is supported.
- **Kotlin:** 2.x everywhere; coroutines are the default concurrency model.
- **Android:** minSdk 26, targetSdk 35, R8 full mode enabled on all release builds.
- **Kotlin Multiplatform:** shared modules target `jvm`, `android`, `iosArm64` and `iosSimulatorArm64`.
- **Native image:** two latency-sensitive services build with GraalVM native image.

## Hardware targets

- Production: AWS Graviton (`linux-aarch64`), plus a few `linux-x86_64` batch hosts.
- Developer machines: Apple Silicon (`macos-aarch64`).

## Frameworks

- Server: Ktor 3, kotlinx.serialization, Exposed for SQL, PostgreSQL 16.
- Android: Jetpack Compose, Room, WorkManager.
- Build: Gradle with Kotlin DSL and version catalogs.

## License allowlist

| License | Allowed |
|---|---|
| Apache-2.0 | Yes |
| MIT | Yes |
| BSD-2-Clause, BSD-3-Clause | Yes |
| EPL-2.0 | Yes (no source modifications) |
| MPL-2.0 | Yes (file-level changes must be published) |
| LGPL-2.1, LGPL-3.0 | Server-side only, never in mobile apps |

Anything not in this table needs Legal before it can be evaluated (see GR-001).
