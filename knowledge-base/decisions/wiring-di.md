---
library: io.example:wiring-di
decision: ADOPT
decided: 2025-02-18
score: 77
outcome: reversed
source: internal
---
# wiring-di

**Decision:** ADOPT (77/100).

Compile-time dependency injection via annotation processing. Scored well on maintenance
and security.

**Outcome (2025-10-30):** Reversed. Annotation processing added 35-50% to incremental
build times on the Android app, which the evaluation did not measure. Replaced with
manual constructor injection.

**Lesson:** Stack fit must consider build-time cost, not only runtime behaviour.
