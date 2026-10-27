# Intake: io.example:xmlstream

| Field | Value |
|---|---|
| Intake date | 2026-10-21 |
| Requested by | Integrations team |
| Coordinates | io.example:xmlstream:3.1.4 |
| License | Apache-2.0 |
| Latest release | 3.1.4, 2026-04-02 |
| Releases in last 12 months | 3 |
| Active maintainers (12 months) | 2 |
| Platforms | JVM |
| Kotlin Multiplatform | No |
| Native code | None |
| Signed artifacts | Yes |
| Transitive dependencies | 11 |

## Why the team wants it

Streaming XML parser for a partner's 2 GB nightly export; our current parser loads the
whole document into memory.

## Known advisories

| ID | CVSS | Affects | Fixed in |
|---|---|---|---|
| EXAMPLE-ADV-2026-0042 | 9.8 | <= 3.1.4 (XXE via external entity resolution enabled by default) | No fix released |
| EXAMPLE-ADV-2025-0110 | 5.3 | < 3.0.2 | 3.0.2 |

Maintainer comment on the 0042 issue (2026-08-14): "Working on it, no ETA."
