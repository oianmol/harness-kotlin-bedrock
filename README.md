# harness-kotlin-bedrock

An example LLM evaluation harness with guardrails, a knowledge base and typed contracts
between steps. It runs two ways from the same files:

- **In Claude Code**, using `CLAUDE.md`, slash commands, subagents and hooks.
- **In Kotlin on AWS Bedrock**, using Anthropic's Java SDK with its Bedrock backend.

The example domain is **dependency vetting**: given an intake document for a third-party
library, decide ADOPT, TRIAL or REJECT, with cited evidence. Every library, advisory and
engineer in this repo is invented (`io.example:*`).

Companion code for the blog post *Building an LLM Evaluation Harness the Right Way*.

## Try it in 30 seconds (no AWS needed)

```bash
./gradlew run --args="evaluate-all --offline"
```

```
abandoned-cronparse: REJECT (guardrail GR-002)
agpl-chartkit: REJECT (guardrail GR-001)
llm-wrapper: REJECT 30/100, confidence LOW
reactive-sqlite: TRIAL 70/100, confidence HIGH
serialization-extras: ADOPT 90/100, confidence HIGH
vulnerable-xmlstream: REJECT (guardrail GR-003)
```

`--offline` replays recorded model responses from `examples/recorded/`. Everything else
runs for real: knowledge-base parsing, the guardrail gate, contract validation, scoring,
rendering and listeners. Read the results in `inbound/<slug>/evaluation.md`.

```bash
./gradlew run --args="rules"     # guardrails and whether each one is enforced
./gradlew run --args="health"    # audit the knowledge base and the evaluation ledger
./gradlew test                   # 29 tests, no network
```

## What's in the box

```text
CLAUDE.md                     # the constitution: role, stance, output rules
knowledge-base/
  guardrails.md               # hard disqualifiers with stable IDs (one retired, one unconfigured)
  scoring.md                  # rubric, deduction tables, thresholds, confidence, change log
  stack-profile.md            # what we run, license allowlist
  approved-libraries.md
  team.md                     # who has shipped what (some of it at prior companies)
  decisions/                  # 9 past decisions with outcomes, two of them reversed
inbound/<slug>/raw.md         # 6 example intakes, one per kind of outcome
inbound/<slug>/evaluation.md  # the harness's output, committed so you can read it
examples/recorded/            # recorded model responses for offline replay
templates/evaluation.md       # the shape every evaluation fills
.claude/                      # Claude Code: commands, subagents, hook settings
scripts/notify-on-verdict.sh  # the PostToolUse hook
src/main/kotlin/              # the Kotlin/Bedrock implementation
```

The six examples each exercise a different path:

| Example | What it shows |
|---|---|
| `agpl-chartkit` | License guardrail (GR-001) stops the pipeline before any scoring |
| `vulnerable-xmlstream` | Unpatched critical advisory (GR-003) |
| `abandoned-cronparse` | No release in 24 months (GR-002) |
| `serialization-extras` | A clean ADOPT |
| `reactive-sqlite` | A borderline TRIAL, with prior-company experience attributed inline |
| `llm-wrapper` | A thin intake: REJECT on missing evidence, LOW confidence, and a recorded gate response that cites an unconfigured rule, which the harness catches and notes |

## Running it in Claude Code

Open the repo in Claude Code and run:

```
/guardrail-check agpl-chartkit
/evaluate reactive-sqlite
```

`/evaluate` runs `guardrail-checker` as a blocking gate, then `security-researcher` and
`team-fit-matcher` in parallel, and writes `inbound/<slug>/evaluation.md`. The
`PostToolUse` hook in `.claude/settings.json` prints (or posts, with `HARNESS_WEBHOOK_URL`
set) the verdict line whenever an evaluation is written.

## Running it on Bedrock

Live mode calls Claude on Amazon Bedrock through `BedrockMantleBackend`, which reads the
standard AWS credential chain and region.

```bash
export AWS_REGION=us-east-1          # a region where your account has Claude access
export AWS_PROFILE=your-profile
./gradlew run --args="evaluate reactive-sqlite"

# Save the responses so the run can be replayed offline:
./gradlew run --args="evaluate reactive-sqlite --record"
```

The default model is `anthropic.claude-opus-5-5`; override it with `HARNESS_MODEL`.

## How the Kotlin side maps to Claude Code

| Claude Code | Kotlin / Bedrock | File |
|---|---|---|
| `CLAUDE.md` + agent prompts | System prompt blocks per step | `src/main/resources/prompts/` |
| `guardrail-checker` subagent | `GuardrailGate`, sees only active rules | `pipeline/Steps.kt` |
| `GUARDRAIL RESULT:` text block | `GuardrailReport` + `sealed interface Gate` | `model/Models.kt` |
| Parallel subagents | `coroutineScope { async { } }` | `pipeline/Evaluator.kt` |
| Read / Grep tools, permissions | `KbTool`s confined to the KB root | `tools/KbTools.kt` |
| Agent loop | Manual tool loop with every stop reason handled | `llm/BedrockLlmClient.kt` |
| Model adds up the scorecard | `Rubric.verdictFor()` in code | `kb/Rubric.kt` |
| `PostToolUse` hooks | `EvaluationListener`s | `pipeline/Listeners.kt` |
| Weekly pipeline audit | `HealthCheck` | `health/HealthCheck.kt` |

## Design rules this repo follows

1. **Policy lives in data.** Weights, thresholds, rules and confidence bands are parsed from
   markdown. Changing policy is a reviewed diff to `knowledge-base/`, not a code change.
2. **Guardrails first, in isolation.** The gate sees only active rules and the input.
   Retired rules and `[SET THIS]` placeholders are filtered out in code, not by asking the
   model to ignore them.
3. **Contracts, not prose.** Every model call returns a typed object whose schema is derived
   from a Kotlin data class. The harness validates it and records what it corrected under
   "Harness notes" in the evaluation.
4. **Missing information is a value.** `[NOT FOUND IN SOURCE]` is counted, and the count
   sets confidence.
5. **Arithmetic stays out of the model.** The model returns deductions with evidence; code
   computes totals and verdicts.
6. **Attribution travels with the sentence.** Prior-company experience is labelled by the
   renderer, so no prompt can forget it.
7. **Audit the harness.** `health` reports stale outcomes, unconfigured rules, broken
   references, guardrail firing rates and calibration by score band.

## License

MIT
