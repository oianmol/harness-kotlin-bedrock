Run a full dependency evaluation.

**Usage:** `/evaluate <slug>` evaluates `inbound/<slug>/raw.md`.

---

## Pre-flight

Read before starting:

- `knowledge-base/scoring.md`
- `knowledge-base/stack-profile.md`
- `knowledge-base/approved-libraries.md`
- ALL files in `knowledge-base/decisions/`
- `templates/evaluation.md`

(`guardrails.md` and `team.md` are read by the subagents in Step 1.)

## Step 1: Guardrails (blocking)

Launch the `guardrail-checker` subagent with the intake path. Wait for its result.

- **FAILED:** write `inbound/<slug>/evaluation.md` with verdict REJECT, the triggered-rule
  table from the subagent, and "Evaluation stopped at the gate. No scoring was performed."
  Stop here.
- **PASSED:** continue. In the same turn, launch two subagents in parallel:
  - `security-researcher` with the intake path (feeds Step 3)
  - `team-fit-matcher` with the intake path (feeds Step 4)

  Do not wait for them; do Step 2 while they run.

## Step 2: Intake

Fill the intake table in the template from the intake file. Use `[NOT FOUND IN SOURCE]`
for every field the intake does not state. Count those markers: they set confidence.

## Step 3: Security findings

From the `security-researcher` result.

## Step 4: Team fit

From the `team-fit-matcher` result. Keep its prior-company labels in the same sentence.

## Step 5: Scorecard

For each dimension in `scoring.md`, list every deduction that applies with points, the
table row it comes from, and evidence. Missing evidence counts against the library.
Show your arithmetic: dimension scores, then the total. Apply the thresholds exactly.

## Step 6: Risks and recommendation

Concrete risks, then one paragraph: the verdict, and what would change it.

## Finish

Write `inbound/<slug>/evaluation.md` following `templates/evaluation.md`. The PostToolUse
hook posts a summary once the file is written; you do not need to notify anyone.
