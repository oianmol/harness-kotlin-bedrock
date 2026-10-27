# Evaluation: serialization-extras

**Verdict:** ADOPT (90/100)
**Confidence:** HIGH

## 1. Guardrail check

PASSED. 5 active rules checked, none triggered.

## 2. Intake

| Field | Value |
|---|---|
| Coordinates | io.example:serialization-extras:2.4.1 |
| License | Apache-2.0 |
| Latest release | 2.4.1, 2026-10-09 |
| Releases in last 12 months | 11 |
| Active maintainers | 6 (across 3 companies) |
| Median issue response | 3 days |
| Platforms | JVM, Android, iOS, JS |
| Kotlin Multiplatform | Yes (jvm, android, iosArm64, iosSimulatorArm64) |
| Native code | None |
| Signed artifacts | Yes (Sigstore) |
| Transitive dependencies | 2 (kotlinx-serialization-core, kotlinx-datetime) |
| Uses reflection | No (compiler-plugin based) |
| Known advisories | EXAMPLE-ADV-2025-0731, CVSS 4.1, fixed in 2.2.0 |
| Why the team wants it | Replace ~600 lines of custom serializers in the shared KMP networking module |

## 3. Security findings

No open advisories against 2.4.1. The only published advisory (EXAMPLE-ADV-2025-0731, CVSS 4.1) was fixed in 2.2.0 six days after report. Signed artifacts and a two-dependency tree keep the supply-chain surface small.

- Signed artifacts: Yes (Sigstore)
- Transitive dependencies: 2 (kotlinx-serialization-core, kotlinx-datetime)
- Open advisories: none found

## 4. Team fit

Nobody has shipped serialization-extras itself, but Riley owns the custom serializers it would replace. fastjson-kt was rejected for reflection and lack of KMP support, both of which this library avoids; yamlkt shows kotlinx.serialization add-ons have held up well for us.

- Riley: Ships kotlinx.serialization custom serializers in our KMP shared modules; this library builds on the same compiler plugin.
- Related decisions: decisions/fastjson-kt.md, decisions/yamlkt.md

## 5. Scorecard

| Dimension | Score | Max |
|---|---|---|
| Maintenance health | 30 | 30 |
| Security posture | 25 | 25 |
| Stack fit | 25 | 25 |
| Team familiarity | 10 | 20 |
| **Total** | **90** | **100** |

Thresholds: ADOPT >= 75, TRIAL 55-74, REJECT < 55.

### Deductions

| Dimension | Points | Reason | Evidence |
|---|---|---|---|
| Maintenance health | 0 | No deductions | |
| Security posture | 0 | No deductions | |
| Stack fit | 0 | No deductions | |
| Team familiarity | -10 | Nobody on the team has shipped production code with it | Team-fit: 'Nobody has shipped serialization-extras itself' |

## 6. Risks

- Compiler-plugin coupling: each Kotlin upgrade waits for a compatible serialization-extras release.
- The JSON-to-Protobuf bridge is the newest part of the library and the least battle-tested.

## 7. Recommendation

Adopt for the shared KMP networking module. It removes ~600 lines of custom serializers, adds two dependencies we already ship, and has a strong maintenance and security record. The only gap is first-hand experience; have Riley migrate one endpoint first and keep the old serializers until it has run a release cycle.
