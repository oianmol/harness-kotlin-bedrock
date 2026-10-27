package io.github.oianmol.harness

import io.github.oianmol.harness.health.HealthCheck
import io.github.oianmol.harness.llm.BedrockLlmClient
import io.github.oianmol.harness.llm.ReplayLlmClient
import io.github.oianmol.harness.model.Confidence
import io.github.oianmol.harness.model.Gate
import io.github.oianmol.harness.model.GuardrailReport
import io.github.oianmol.harness.model.Intake
import io.github.oianmol.harness.model.ScoringResponse
import io.github.oianmol.harness.model.SecurityFindings
import io.github.oianmol.harness.model.TeamFit
import io.github.oianmol.harness.model.Verdict
import io.github.oianmol.harness.pipeline.Evaluator
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.time.LocalDate
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Replays the recorded responses for every example and checks the verdicts the README promises. */
class ExamplesTest {

    private fun evaluator(): Pair<Evaluator, java.nio.file.Path> {
        // Copy the intakes so the test never rewrites the committed evaluation.md files.
        val dir = Files.createTempDirectory("inbound")
        exampleInbound.listDirectoryEntries().forEach { src ->
            src.resolve("raw.md").copyTo(dir.resolve(src.name).createDirectories().resolve("raw.md"))
        }
        return Evaluator(exampleKb, ReplayLlmClient(exampleRecordings), dir) to dir
    }

    @Test
    fun `every example lands on its expected verdict`() = runTest {
        val (evaluator, _) = evaluator()
        val expected = mapOf(
            "agpl-chartkit" to Verdict.REJECT,
            "vulnerable-xmlstream" to Verdict.REJECT,
            "abandoned-cronparse" to Verdict.REJECT,
            "serialization-extras" to Verdict.ADOPT,
            "reactive-sqlite" to Verdict.TRIAL,
            "llm-wrapper" to Verdict.REJECT,
        )
        expected.forEach { (slug, verdict) -> assertEquals(verdict, evaluator.evaluate(slug).verdict, slug) }
    }

    @Test
    fun `guardrail rejections cite the right rule`() = runTest {
        val (evaluator, _) = evaluator()
        mapOf("agpl-chartkit" to "GR-001", "abandoned-cronparse" to "GR-002", "vulnerable-xmlstream" to "GR-003")
            .forEach { (slug, rule) ->
                val gate = evaluator.evaluate(slug).gate as Gate.Failed
                assertEquals(listOf(rule), gate.hits.map { it.ruleId })
            }
    }

    @Test
    fun `a thin intake is rejected with low confidence and the bogus gate hit is noted`() = runTest {
        val (evaluator, _) = evaluator()
        val evaluation = evaluator.evaluate("llm-wrapper")
        assertEquals(Confidence.LOW, evaluation.confidence)
        assertEquals(Gate.Passed, evaluation.gate)
        assertTrue(evaluation.notes.any { "GR-006" in it })
    }

    @Test
    fun `prior-company experience is attributed inline in the rendered evaluation`() = runTest {
        val (evaluator, dir) = evaluator()
        evaluator.evaluate("reactive-sqlite")
        val rendered = Files.readString(dir.resolve("reactive-sqlite/evaluation.md"))
        assertTrue("Alex (prior-company experience, not our delivery)" in rendered)
    }

    @Test
    fun `health check finds the planted problems`() {
        val report = HealthCheck(exampleKb, emptyList(), today = LocalDate.parse("2026-10-27")).run()
        val checks = report.findings.map { it.check }
        assertTrue("stale-outcome" in checks)
        assertTrue("placeholder" in checks)
        assertTrue("calibration" in checks)
        assertTrue("broken-reference" !in checks, report.findings.toString())
    }

    @Test
    fun `the SDK can derive an output schema for every contract`() {
        listOf(
            GuardrailReport::class.java,
            Intake::class.java,
            SecurityFindings::class.java,
            TeamFit::class.java,
            ScoringResponse::class.java,
        ).forEach { BedrockLlmClient.outputFormatFor(it) }
    }
}
