package io.github.oianmol.harness.kb

import io.github.oianmol.harness.model.Verdict
import java.time.LocalDate

enum class Outcome { CONFIRMED, REVERSED, UNKNOWN }

/** A past adoption decision (an ADR) and, crucially, what happened afterwards. */
data class Decision(
    val path: String,
    val library: String,
    val decision: Verdict,
    val decided: LocalDate,
    val score: Int,
    val outcome: Outcome,
    /** `internal` or `prior-team`. Prior-team evidence must be attributed whenever it is cited. */
    val source: String,
    val body: String,
) {
    companion object {
        fun parse(path: String, text: String): Decision {
            val fm = Frontmatter.parse(text)
            return Decision(
                path = path,
                library = fm.require("library", path),
                decision = Verdict.valueOf(fm.require("decision", path)),
                decided = LocalDate.parse(fm.require("decided", path)),
                score = fm.require("score", path).toInt(),
                outcome = Outcome.valueOf(fm.require("outcome", path).uppercase()),
                source = fm["source"] ?: "internal",
                body = fm.body,
            )
        }
    }
}
