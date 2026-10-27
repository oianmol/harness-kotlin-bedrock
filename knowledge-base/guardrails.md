# Guardrail Rules: Automatic Disqualifiers

Guardrails are binary. If any active rule triggers, the library is REJECTED and no
scoring happens. Rules are checked before anything else.

Conventions:

- Every rule has a stable ID (`GR-NNN`). Evaluations cite the ID, never a paraphrase.
- Rules are never deleted. A rule that no longer applies is marked `[RETIRED]` in its
  heading so old evaluations still resolve.
- A trigger containing `[SET THIS]` is **not configured**. It must not fire until a
  real threshold replaces the placeholder.

---

## GR-001: Incompatible License

**Trigger:** The library's license is AGPL (any version), SSPL, BUSL, or any license that
is not on the license allowlist in `stack-profile.md`.

**Why:** Our products ship as closed-source binaries and SaaS. Legal blocks these licenses
at release review; discovering it late costs a rewrite.

---

## GR-002: Abandoned Project

**Trigger:** No release in the last 24 months, measured from the evaluation date.

**Why:** Unmaintained dependencies stop receiving security fixes and block JVM and Kotlin
upgrades across every service that uses them.

---

## GR-003: Unpatched Critical Vulnerability

**Trigger:** Any published advisory with CVSS >= 9.0 affecting the latest release, with no
fixed version available.

**Why:** We cannot ship a known critical vulnerability, and we cannot patch a third-party
library faster than its maintainers.

---

## GR-004: [RETIRED] Requires Java 8 Runtime

**Trigger:** The library requires a Java 8 runtime.

**Retired:** 2025-11-02. We no longer build for Java 8, so the rule can never fire.
Kept so evaluations from before that date still resolve.

---

## GR-005: Unsupported Runtime

**Trigger:** The library requires a runtime we do not ship: a JVM older than 17, a Scala
runtime, a Node.js sidecar, or a Python interpreter.

**Why:** Each extra runtime is a new patching, packaging and on-call surface.

---

## GR-006: Bus Factor

**Trigger:** Fewer than [SET THIS] maintainers with merge rights in the last 12 months.

**Why:** Single-maintainer projects are a supply-chain risk. Threshold under discussion;
until it is set, this rule must not fire.

---

## GR-007: Native Code Without Our Targets

**Trigger:** The library ships native code but provides no prebuilt binaries for all of
`linux-x86_64`, `linux-aarch64` and `macos-aarch64`.

**Why:** We run on Graviton in production and Apple Silicon locally. Building native code
from source in CI is not something we support.

---

## Adding New Rules

1. Use the next free ID. Never reuse a retired ID.
2. Write the trigger so that a reader with only the input document could decide it.
3. Add a **Why** line. A rule without a reason gets deleted in the next review.
4. Open a PR; a rule change needs one platform-team approval.
