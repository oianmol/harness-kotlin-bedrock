package io.github.oianmol.harness.pipeline

import io.github.oianmol.harness.kb.Rubric
import io.github.oianmol.harness.model.Evaluation
import io.github.oianmol.harness.model.Gate

/**
 * Renders an [Evaluation] in the same section order as `templates/evaluation.md`, which the
 * Claude Code harness fills by hand. Both runtimes produce comparable files.
 */
object EvaluationRenderer {
    fun render(e: Evaluation, rubric: Rubric): String = buildString {
        appendLine("# Evaluation: ${e.slug}")
        appendLine()
        appendLine("**Verdict:** ${e.verdict}" + (e.total?.let { " (${it}/${rubric.maxTotal})" } ?: ""))
        appendLine("**Confidence:** ${e.confidence ?: "Not assessed (disqualified by guardrail)"}")
        appendLine()

        appendLine("## 1. Guardrail check")
        appendLine()
        when (val gate = e.gate) {
            Gate.Passed -> appendLine("PASSED. ${e.rulesChecked} active rules checked, none triggered.")
            is Gate.Failed -> {
                appendLine("FAILED. ${gate.hits.size} of ${e.rulesChecked} active rules triggered:")
                appendLine()
                appendLine("| Rule | Evidence from source |")
                appendLine("|---|---|")
                gate.hits.forEach { appendLine("| ${it.ruleId} | ${cell(it.evidence)} |") }
                appendLine()
                appendLine("Evaluation stopped at the gate. No scoring was performed.")
            }
        }

        if (e.disqualified) {
            appendNotes(e)
            return@buildString
        }

        appendLine()
        appendLine("## 2. Intake")
        appendLine()
        appendLine("| Field | Value |")
        appendLine("|---|---|")
        e.facts.forEach { appendLine("| ${cell(it.field)} | ${cell(it.value)} |") }

        e.security?.let { s ->
            appendLine()
            appendLine("## 3. Security findings")
            appendLine()
            appendLine(s.summary)
            appendLine()
            appendLine("- Signed artifacts: ${s.signedArtifacts}")
            appendLine("- Transitive dependencies: ${s.transitiveDependencies}")
            if (s.openAdvisories.isEmpty()) appendLine("- Open advisories: none found")
            s.openAdvisories.forEach { appendLine("- Open advisory ${it.id}: CVSS ${it.cvss}, fixed in ${it.fixedIn}") }
        }

        e.teamFit?.let { t ->
            appendLine()
            appendLine("## 4. Team fit")
            appendLine()
            appendLine(t.summary)
            appendLine()
            if (t.matches.isEmpty()) appendLine("- No engineer has shipped production code with this library.")
            // Attribution travels with the sentence: prior-company experience is labelled inline,
            // by the renderer, so no prompt can forget to do it.
            t.matches.forEach {
                val label = if (it.priorCompany) " (prior-company experience, not our delivery)" else ""
                appendLine("- ${it.engineer}$label: ${it.evidence}")
            }
            if (t.relatedDecisions.isNotEmpty()) appendLine("- Related decisions: ${t.relatedDecisions.joinToString(", ")}")
        }

        appendLine()
        appendLine("## 5. Scorecard")
        appendLine()
        appendLine("| Dimension | Score | Max |")
        appendLine("|---|---|---|")
        e.dimensions.forEach { appendLine("| ${it.name} | ${it.score} | ${it.max} |") }
        appendLine("| **Total** | **${e.total}** | **${rubric.maxTotal}** |")
        appendLine()
        appendLine("Thresholds: ADOPT >= ${rubric.adoptAt}, TRIAL ${rubric.trialAt}-${rubric.adoptAt - 1}, REJECT < ${rubric.trialAt}.")
        appendLine()
        appendLine("### Deductions")
        appendLine()
        appendLine("| Dimension | Points | Reason | Evidence |")
        appendLine("|---|---|---|---|")
        e.dimensions.forEach { d ->
            if (d.deductions.isEmpty()) appendLine("| ${d.name} | 0 | No deductions | |")
            d.deductions.forEach { appendLine("| ${d.name} | -${it.points} | ${cell(it.reason)} | ${cell(it.evidence)} |") }
        }

        appendLine()
        appendLine("## 6. Risks")
        appendLine()
        if (e.risks.isEmpty()) appendLine("None identified.")
        e.risks.forEach { appendLine("- $it") }

        appendLine()
        appendLine("## 7. Recommendation")
        appendLine()
        appendLine(e.recommendation)

        appendNotes(e)
    }

    private fun StringBuilder.appendNotes(e: Evaluation) {
        if (e.notes.isEmpty()) return
        appendLine()
        appendLine("## Harness notes")
        appendLine()
        appendLine("Contract problems the harness caught and corrected:")
        appendLine()
        e.notes.forEach { appendLine("- $it") }
    }

    private fun cell(text: String) = text.replace("|", "\\|").replace("\n", " ")
}
