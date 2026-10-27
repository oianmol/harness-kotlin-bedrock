# Intake: io.example:reactive-sqlite

| Field | Value |
|---|---|
| Intake date | 2026-10-24 |
| Requested by | Backend team |
| Coordinates | io.example:reactive-sqlite:0.9.3 |
| License | Apache-2.0 |
| Latest release | 0.9.3, 2026-08-28 |
| Releases in last 12 months | 5 |
| Active maintainers (12 months) | 3 (all employed by the same company) |
| Median issue response | 9 days |
| Platforms | JVM |
| Kotlin Multiplatform | No |
| Native code | Bundles sqlite3 for linux-x86_64, linux-aarch64, macos-aarch64, windows-x86_64 |
| Signed artifacts | Yes |
| Transitive dependencies | 14 |
| Uses reflection | Only for optional row-mapping helpers |

## Why the team wants it

Non-blocking SQLite access with Flow-based queries for an edge cache service. Exposed
only offers blocking JDBC for SQLite.

## Known advisories

None published.

## Notes

Pre-1.0: the README warns that APIs may change between minor versions. Has a suspend
API and Flow queries. No changelog for 0.x releases; release notes are commit lists.
