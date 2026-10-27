Recompute the scorecard of an existing evaluation, typically after `scoring.md` changes.

**Usage:** `/scorecard <slug>`

1. Read `knowledge-base/scoring.md` and `inbound/<slug>/evaluation.md`.
2. If the evaluation was stopped at the guardrail gate, say so and stop: there is no
   scorecard to recompute.
3. Re-apply the current deduction tables to the evidence already in the evaluation. Do not
   re-research; if a new rubric row needs evidence the evaluation does not have, apply the
   deduction with `[NOT FOUND IN SOURCE]`.
4. Replace only the Scorecard section and the verdict line. Note at the end of the
   scorecard which rubric change caused the difference, if any.
