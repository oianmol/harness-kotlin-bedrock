# Evaluation: reactive-sqlite

**Verdict:** TRIAL (70/100)
**Confidence:** HIGH

## 1. Guardrail check

PASSED. 5 active rules checked, none triggered.

## 2. Intake

| Field | Value |
|---|---|
| Coordinates | io.example:reactive-sqlite:0.9.3 |
| License | Apache-2.0 |
| Latest release | 0.9.3, 2026-08-28 |
| Releases in last 12 months | 5 |
| Active maintainers | 3 (all employed by the same company) |
| Median issue response | 9 days |
| Platforms | JVM |
| Kotlin Multiplatform | No |
| Native code | Bundles sqlite3 for linux-x86_64, linux-aarch64, macos-aarch64, windows-x86_64 |
| Signed artifacts | Yes |
| Transitive dependencies | 14 |
| Uses reflection | Only for optional row-mapping helpers |
| Known advisories | None published |
| Why the team wants it | Non-blocking SQLite access with Flow queries for an edge cache service |

## 3. Security findings

No published advisories, so there is no track record of how fast fixes ship. Artifacts are signed. The bundled native sqlite3 means sqlite CVEs reach us only when the maintainers re-bundle; that lag is worth tracking.

- Signed artifacts: Yes
- Transitive dependencies: 14
- Open advisories: none found

## 4. Team fit

Nobody has shipped reactive-sqlite. Alex's closest experience is R2DBC at a previous company. pdfkit-native was rejected for missing aarch64 binaries; this library does ship our targets, so that lesson is satisfied here.

- Alex (prior-company experience, not our delivery): Shipped R2DBC drivers in production, a comparable non-blocking database access pattern.
- Related decisions: decisions/pdfkit-native.md

## 5. Scorecard

| Dimension | Score | Max |
|---|---|---|
| Maintenance health | 20 | 30 |
| Security posture | 25 | 25 |
| Stack fit | 19 | 25 |
| Team familiarity | 6 | 20 |
| **Total** | **70** | **100** |

Thresholds: ADOPT >= 75, TRIAL 55-74, REJECT < 55.

### Deductions

| Dimension | Points | Reason | Evidence |
|---|---|---|---|
| Maintenance health | -4 | No changelog or migration notes for major versions | 'No changelog for 0.x releases; release notes are commit lists' |
| Maintenance health | -6 | Single company controls all commits | '3 (all employed by the same company)' |
| Security posture | 0 | No deductions | |
| Stack fit | -6 | Overlaps a library already on the approved list | Exposed is approved for SQL; this adds a second database access library, justified only for non-blocking SQLite |
| Team familiarity | -10 | Nobody on the team has shipped production code with it | Team-fit: 'Nobody has shipped reactive-sqlite' |
| Team familiarity | -4 | No related past decision in decisions/ to learn from | No prior decision on SQLite or non-blocking database access |

## 6. Risks

- Pre-1.0: APIs may change between minor versions, and there is no changelog to plan upgrades against.
- Single-company control: a relicense or a change of priorities at that company leaves us stranded.
- Bundled sqlite3 must be re-released for each upstream sqlite security fix.

## 7. Recommendation

Trial it in the edge cache service only, behind our own repository interface so it can be swapped. Avoid the reflection-based row-mapping helpers. Revisit at 1.0: a changelog and a second contributing organisation would move this to ADOPT; a breaking minor release without notes would move it to REJECT.
