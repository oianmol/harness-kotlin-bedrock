Run a fast guardrail check on one library. No scoring, no evaluation file.

**Usage:** `/guardrail-check <slug>` checks `inbound/<slug>/raw.md`.

Launch the `guardrail-checker` subagent with the intake path and return its result block to
the user unchanged. If it FAILED, add one line naming the cheapest way to clear each
triggered rule (for example, "a commercial license would clear GR-001"), or say that
nothing can.
