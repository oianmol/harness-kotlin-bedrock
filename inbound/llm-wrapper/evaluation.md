# Evaluation: llm-wrapper

**Verdict:** REJECT (30/100)
**Confidence:** LOW

## 1. Guardrail check

PASSED. 5 active rules checked, none triggered.

## 2. Intake

| Field | Value |
|---|---|
| Coordinates | io.example:llm-wrapper:0.3.0 |
| License | MIT |
| Latest release | 0.3.0, 2026-10-01 |
| Releases in last 12 months | [NOT FOUND IN SOURCE] |
| Active maintainers | [NOT FOUND IN SOURCE] |
| Median issue response | [NOT FOUND IN SOURCE] |
| Platforms | JVM |
| Kotlin Multiplatform | [NOT FOUND IN SOURCE] |
| Native code | None |
| Signed artifacts | [NOT FOUND IN SOURCE] |
| Transitive dependencies | [NOT FOUND IN SOURCE] |
| Uses reflection | [NOT FOUND IN SOURCE] |
| Known advisories | [NOT FOUND IN SOURCE] |
| Why the team wants it | Prototype a summarisation feature next sprint with one API across LLM providers |

## 3. Security findings

The intake records no advisories, but the repository has no security policy and no advisory entries, so 'none' here means 'unknown', not 'clean'. Signing and the dependency tree are not stated.

- Signed artifacts: [NOT FOUND IN SOURCE]
- Transitive dependencies: [NOT FOUND IN SOURCE]
- Open advisories: none found

## 4. Team fit

Nobody on the team has shipped llm-wrapper or a comparable multi-provider LLM client, and there are no related past decisions.

- No engineer has shipped production code with this library.

## 5. Scorecard

| Dimension | Score | Max |
|---|---|---|
| Maintenance health | 0 | 30 |
| Security posture | 10 | 25 |
| Stack fit | 14 | 25 |
| Team familiarity | 6 | 20 |
| **Total** | **30** | **100** |

Thresholds: ADOPT >= 75, TRIAL 55-74, REJECT < 55.

### Deductions

| Dimension | Points | Reason | Evidence |
|---|---|---|---|
| Maintenance health | -6 | Fewer than 4 releases in the last 12 months | [NOT FOUND IN SOURCE] |
| Maintenance health | -8 | Fewer than 3 active maintainers in the last 12 months | [NOT FOUND IN SOURCE] |
| Maintenance health | -6 | Median issue response time over 14 days | [NOT FOUND IN SOURCE] |
| Maintenance health | -4 | No changelog or migration notes for major versions | README: 'Early days: expect breaking changes.' No changelog mentioned |
| Maintenance health | -6 | Single company controls all commits | [NOT FOUND IN SOURCE] |
| Security posture | -5 | Artifacts not signed | [NOT FOUND IN SOURCE] |
| Security posture | -6 | Last advisory took more than 30 days to fix | No security policy or advisory history; [NOT FOUND IN SOURCE] |
| Security posture | -4 | More than 25 transitive dependencies | [NOT FOUND IN SOURCE] |
| Stack fit | -6 | Uses runtime reflection (breaks under R8 or GraalVM native image) | [NOT FOUND IN SOURCE] |
| Stack fit | -5 | Blocking I/O with no coroutine or suspend API | [NOT FOUND IN SOURCE] |
| Team familiarity | -10 | Nobody on the team has shipped production code with it | Team-fit: no matches |
| Team familiarity | -4 | No related past decision in decisions/ to learn from | Team-fit: no related decisions |

## 6. Risks

- Most of the intake is blank; the real risk profile is unknown.
- Pre-1.0 with breaking changes expected, in a fast-moving area where provider APIs change monthly.

## 7. Recommendation

Reject as submitted. This is a verdict on the evidence, not on the library: most deductions are for missing information. Re-submit with release history, maintainer count, signing and the dependency tree filled in. For next sprint's prototype, call one provider's official SDK directly; it needs no approval and keeps the option open.

## Harness notes

Contract problems the harness caught and corrected:

- Gate cited GR-006, which is not an active rule; ignored.
- Gate reported 6 rules checked; 5 are active.
