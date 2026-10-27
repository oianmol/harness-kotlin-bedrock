package io.github.oianmol.harness.health

import io.github.oianmol.harness.kb.KnowledgeBase
import io.github.oianmol.harness.kb.Outcome
import io.github.oianmol.harness.kb.PLACEHOLDER
import io.github.oianmol.harness.pipeline.LedgerEntry
import io.github.oianmol.harness.model.Verdict
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Audits the harness itself rather than any one evaluation. Run it weekly: it is the check that
 * tells you whether the system is still honest.
 */
class HealthCheck(
    private val kb: KnowledgeBase,
    private val ledger: List<LedgerEntry>,
    private val today: LocalDate = LocalDate.now(),
    private val staleAfterDays: Long = 90,
    private val reversalAlarm: Double = 0.30,
) {
    data class Finding(val check: String, val message: String)

    data class BandCalibration(val band: Verdict, val decided: Int, val confirmed: Int, val reversed: Int, val unknown: Int) {
        val reversalRate: Double get() = if (confirmed + reversed == 0) 0.0 else reversed.toDouble() / (confirmed + reversed)
    }

    data class Report(
        val findings: List<Finding>,
        val calibration: List<BandCalibration>,
        val verdictMix: Map<String, Int>,
        val firingRates: Map<String, Int>,
    )

    fun run(): Report {
        val findings = mutableListOf<Finding>()
        val decisions = kb.decisions()
        val rules = kb.guardrails()
        val rubric = kb.rubric()

        // Stale outcomes: a decision nobody followed up on can't teach the system anything.
        decisions.filter { it.outcome == Outcome.UNKNOWN }
            .filter { ChronoUnit.DAYS.between(it.decided, today) > staleAfterDays }
            .forEach {
                val days = ChronoUnit.DAYS.between(it.decided, today)
                findings += Finding("stale-outcome", "${it.path}: outcome still unknown $days days after the decision")
            }

        // KB rot: placeholders and references to files that no longer exist.
        rules.filter { !it.configured && !it.retired }
            .forEach { findings += Finding("placeholder", "${it.id} (${it.title}) has an unset $PLACEHOLDER threshold and is not enforced") }
        val reference = Regex("""`([\w./-]+\.md)`""")
        kb.list().forEach { path ->
            reference.findAll(kb.read(path)).map { it.groupValues[1] }.distinct().forEach { ref ->
                val candidates = listOf(ref, path.substringBeforeLast('/', "") + "/" + ref)
                if (candidates.none { kb.exists(it) } && !ref.startsWith("templates/")) {
                    findings += Finding("broken-reference", "$path references `$ref`, which does not exist")
                }
            }
        }

        // Guardrail firing rates: a rule that never fires may be dead weight.
        val fired = ledger.flatMap { it.triggeredRules }.groupingBy { it }.eachCount()
        val firingRates = rules.filter { it.active }.associate { it.id to (fired[it.id] ?: 0) }
        if (ledger.size >= 10) {
            firingRates.filterValues { it == 0 }.keys
                .forEach { findings += Finding("never-fires", "$it has not fired in ${ledger.size} evaluations") }
        }

        // Calibration: of the decisions in each score band, how many held up?
        val calibration = Verdict.entries.map { band ->
            val inBand = decisions.filter { it.score > 0 && rubric.verdictFor(it.score) == band }
            BandCalibration(
                band = band,
                decided = inBand.size,
                confirmed = inBand.count { it.outcome == Outcome.CONFIRMED },
                reversed = inBand.count { it.outcome == Outcome.REVERSED },
                unknown = inBand.count { it.outcome == Outcome.UNKNOWN },
            )
        }
        calibration.filter { it.band != Verdict.REJECT && it.reversalRate > reversalAlarm }.forEach {
            findings += Finding(
                "calibration",
                "${it.band} band: ${it.reversed} of ${it.confirmed + it.reversed} known outcomes reversed " +
                    "(${"%.0f".format(it.reversalRate * 100)}%). Revisit the thresholds in scoring.md.",
            )
        }

        return Report(findings, calibration, ledger.groupingBy { it.verdict }.eachCount(), firingRates)
    }

    companion object {
        fun format(report: Report): String = buildString {
            appendLine("# Harness health")
            appendLine()
            if (report.findings.isEmpty()) appendLine("No findings.")
            report.findings.groupBy { it.check }.forEach { (check, items) ->
                appendLine("## $check (${items.size})")
                items.forEach { appendLine("- ${it.message}") }
                appendLine()
            }
            appendLine("## Calibration by score band")
            appendLine()
            appendLine("| Band | Decided | Confirmed | Reversed | Unknown | Reversal rate |")
            appendLine("|---|---|---|---|---|---|")
            report.calibration.forEach {
                appendLine("| ${it.band} | ${it.decided} | ${it.confirmed} | ${it.reversed} | ${it.unknown} | ${"%.0f".format(it.reversalRate * 100)}% |")
            }
            appendLine()
            appendLine("## Ledger")
            appendLine()
            appendLine("- Verdict mix: ${report.verdictMix.ifEmpty { mapOf("none" to 0) }}")
            appendLine("- Guardrail firings: ${report.firingRates}")
        }
    }
}
