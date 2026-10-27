---
library: io.example:pdfkit-native
decision: REJECT
decided: 2025-09-05
score: 0
outcome: confirmed
source: internal
---
# pdfkit-native

**Decision:** REJECT. Guardrail GR-007: native binaries only for `linux-x86_64`.

**Outcome (2026-01-12):** Confirmed. Still no aarch64 build; we generate PDFs with a
pure-JVM library instead.
