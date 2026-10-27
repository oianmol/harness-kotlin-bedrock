# Scoring Rubric

Scoring only runs after every guardrail passes. Each dimension starts at its full weight;
the evaluator records a deduction, with evidence, for every point lost. Totals and the
verdict are computed in code from these numbers, never by the model.

Missing evidence counts against the library. If the source does not establish that a
criterion is met, apply the deduction and use `[NOT FOUND IN SOURCE]` as its evidence.
A thin intake should produce a low score and LOW confidence, not a benefit of the doubt.

## Dimensions

| Dimension | Weight |
|---|---|
| Maintenance health | 30 |
| Security posture | 25 |
| Stack fit | 25 |
| Team familiarity | 20 |

### Maintenance health (30)

| Deduction | Points |
|---|---|
| Fewer than 4 releases in the last 12 months | -6 |
| Fewer than 3 active maintainers in the last 12 months | -8 |
| Median issue response time over 14 days | -6 |
| No changelog or migration notes for major versions | -4 |
| Single company controls all commits | -6 |

### Security posture (25)

| Deduction | Points |
|---|---|
| Any open advisory with CVSS 7.0-8.9 | -10 |
| Any open advisory with CVSS 4.0-6.9 | -4 |
| Artifacts not signed | -5 |
| Last advisory took more than 30 days to fix | -6 |
| More than 25 transitive dependencies | -4 |

### Stack fit (25)

| Deduction | Points |
|---|---|
| No Kotlin Multiplatform support where we need it | -8 |
| Uses runtime reflection (breaks under R8 or GraalVM native image) | -6 |
| Blocking I/O with no coroutine or suspend API | -5 |
| Overlaps a library already on the approved list | -6 |

### Team familiarity (20)

| Deduction | Points |
|---|---|
| Nobody on the team has shipped production code with it | -10 |
| No related past decision in `decisions/` to learn from | -4 |
| A related past decision was reversed | -6 |

## Verdict Thresholds

ADOPT >= 75 · TRIAL 55-74 · REJECT < 55

A guardrail failure is always REJECT, regardless of score.

## Confidence

Confidence reflects how much of the intake could be filled from the source, not how
much we like the library.

| Fields marked `[NOT FOUND IN SOURCE]` | Confidence |
|---|---|
| 0-1 | HIGH |
| 2-3 | MEDIUM |
| 4+ | LOW |

## Change Log

| Date | Change | Why |
|---|---|---|
| 2026-02-10 | TRIAL lower bound 50 -> 55 | Two TRIAL picks in the 50-54 band were reversed within a quarter |
| 2025-11-02 | Added "Single company controls all commits" (-6) | A vendor relicensed a library we depended on |
