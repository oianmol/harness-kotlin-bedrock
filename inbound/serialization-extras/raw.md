# Intake: io.example:serialization-extras

| Field | Value |
|---|---|
| Intake date | 2026-10-23 |
| Requested by | Mobile / KMP team |
| Coordinates | io.example:serialization-extras:2.4.1 |
| License | Apache-2.0 |
| Latest release | 2.4.1, 2026-10-09 |
| Releases in last 12 months | 11 |
| Active maintainers (12 months) | 6 (across 3 companies) |
| Median issue response | 3 days |
| Platforms | JVM, Android, iOS, JS |
| Kotlin Multiplatform | Yes (jvm, android, iosArm64, iosSimulatorArm64) |
| Native code | None |
| Signed artifacts | Yes (Sigstore) |
| Transitive dependencies | 2 (kotlinx-serialization-core, kotlinx-datetime) |
| Uses reflection | No (compiler-plugin based) |

## Why the team wants it

Polymorphic sealed-class serializers, lenient enum decoding and a JSON-to-Protobuf
bridge for the shared KMP networking module. Today we maintain ~600 lines of custom
serializers that this would replace.

## Known advisories

| ID | CVSS | Affects | Fixed in |
|---|---|---|---|
| EXAMPLE-ADV-2025-0731 | 4.1 | < 2.2.0 | 2.2.0 (fixed 6 days after report) |

## Changelog

Every major version ships a migration guide. Last major (2.0) was 2025-03.
