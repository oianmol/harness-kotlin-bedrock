---
library: io.example:fastjson-kt
decision: REJECT
decided: 2026-03-14
score: 48
outcome: confirmed
source: internal
---
# fastjson-kt

**Decision:** REJECT (48/100).

Reflection-heavy JSON binding. A spike on the Android app broke under R8 full mode and
needed 40+ keep rules. No Kotlin Multiplatform support. Overlapped kotlinx.serialization,
which already covers our needs.

**Outcome (2026-06-20):** Confirmed. Re-checked after their 2.0 release; still
reflection-based and still JVM-only.
