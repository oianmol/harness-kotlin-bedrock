package io.github.oianmol.harness

import io.github.oianmol.harness.model.Deduction
import io.github.oianmol.harness.model.DimensionAssessment
import io.github.oianmol.harness.model.DimensionScore
import io.github.oianmol.harness.model.Fact
import io.github.oianmol.harness.model.Gate
import io.github.oianmol.harness.model.GuardrailReport
import io.github.oianmol.harness.model.Intake
import io.github.oianmol.harness.model.NOT_FOUND
import io.github.oianmol.harness.model.RuleHit
import io.github.oianmol.harness.model.ScoringResponse
import io.github.oianmol.harness.model.SecurityFindings
import io.github.oianmol.harness.model.TeamFit
import io.github.oianmol.harness.model.Verdict
import io.github.oianmol.harness.pipeline.ContractViolation
import io.github.oianmol.harness.pipeline.Evaluator
import io.github.oianmol.harness.pipeline.GuardrailGate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PipelineTest {
    private val passing = GuardrailReport(rulesChecked = 5, triggered = emptyList())
    private val security = SecurityFindings(emptyList(), "Yes", "3", "Clean.")
    private val team = TeamFit(emptyList(), emptyList(), "Nobody has used it.")

    private fun scoring(vararg deductions: Pair<String, Int>) = ScoringResponse(
        dimensions = listOf("Maintenance health", "Security posture", "Stack fit", "Team familiarity").map { dim ->
            DimensionAssessment(dim, deductions.filter { it.first == dim }.map { Deduction(it.second, "reason", "evidence") })
        },
        risks = listOf("A risk."),
        recommendation = "A recommendation.",
    )

    private fun inbound(): java.nio.file.Path {
        val dir = Files.createTempDirectory("inbound")
        dir.resolve("lib").createDirectories().resolve("raw.md").writeText("# Intake: io.example:lib\n")
        return dir
    }

    @Test
    fun `a failed gate stops the pipeline before any other call`() = runTest {
        val llm = FakeLlmClient(mapOf("guardrail" to { GuardrailReport(5, listOf(RuleHit("GR-001", "AGPL"))) }))
        val evaluation = Evaluator(exampleKb, llm, inbound()).evaluate("lib")

        assertEquals(listOf("guardrail"), llm.purposes())
        assertEquals(Verdict.REJECT, evaluation.verdict)
        assertIs<Gate.Failed>(evaluation.gate)
        assertEquals(null, evaluation.total)
    }

    @Test
    fun `the gate ignores hits on rules that are not active and says so`() = runTest {
        val llm = FakeLlmClient(
            mapOf("guardrail" to { GuardrailReport(5, listOf(RuleHit("GR-006", "bus factor"), RuleHit("GR-404", "made up"))) }),
        )
        val result = GuardrailGate(exampleKb, llm).check("lib", "input")

        assertEquals(Gate.Passed, result.value.gate)
        assertEquals(2, result.notes.size)
        assertTrue(result.notes.all { "not an active rule" in it })
    }

    @Test
    fun `the gate only sees active rules`() = runTest {
        val llm = FakeLlmClient(mapOf("guardrail" to { passing }))
        GuardrailGate(exampleKb, llm).check("lib", "input")

        val system = llm.requests.single().system.joinToString("\n")
        assertTrue("GR-001" in system)
        assertTrue("GR-004" !in system && "GR-006" !in system)
    }

    @Test
    fun `total and verdict are computed in code from deductions`() = runTest {
        val llm = FakeLlmClient(
            mapOf(
                "guardrail" to { passing },
                "intake" to { Intake(listOf(Fact("License", "MIT"), Fact("Maintainers", NOT_FOUND))) },
                "security" to { security },
                "team-fit" to { team },
                "scoring" to { scoring("Team familiarity" to 10, "Stack fit" to 6, "Maintenance health" to 8) },
            ),
        )
        val dir = inbound()
        val evaluation = Evaluator(exampleKb, llm, dir).evaluate("lib")

        assertEquals(76, evaluation.total)
        assertEquals(Verdict.ADOPT, evaluation.verdict)
        assertTrue("**Verdict:** ADOPT (76/100)" in dir.resolve("lib/evaluation.md").readText())
    }

    @Test
    fun `specialists run in parallel`() = runTest {
        val securityStarted = CompletableDeferred<Unit>()
        val teamStarted = CompletableDeferred<Unit>()
        val llm = FakeLlmClient(
            mapOf(
                "guardrail" to { passing },
                "intake" to { Intake(emptyList()) },
                // Each specialist waits for the other to start: this deadlocks if they run sequentially.
                "security" to { securityStarted.complete(Unit); withTimeout(5_000) { teamStarted.await() }; security },
                "team-fit" to { teamStarted.complete(Unit); withTimeout(5_000) { securityStarted.await() }; team },
                "scoring" to { scoring() },
            ),
        )
        val evaluation = Evaluator(exampleKb, llm, inbound()).evaluate("lib")
        assertEquals(100, evaluation.total)
    }

    @Test
    fun `a scoring response missing a dimension is a contract violation`() = runTest {
        val incomplete = ScoringResponse(listOf(DimensionAssessment("Stack fit", emptyList())), emptyList(), "")
        val llm = FakeLlmClient(
            mapOf(
                "guardrail" to { passing },
                "intake" to { Intake(emptyList()) },
                "security" to { security },
                "team-fit" to { team },
                "scoring" to { incomplete },
            ),
        )
        assertFailsWith<ContractViolation> { Evaluator(exampleKb, llm, inbound()).evaluate("lib") }
    }

    @Test
    fun `non-positive deductions are dropped and noted`() = runTest {
        val llm = FakeLlmClient(
            mapOf(
                "guardrail" to { passing },
                "intake" to { Intake(emptyList()) },
                "security" to { security },
                "team-fit" to { team },
                "scoring" to { scoring("Stack fit" to -6, "Stack fit" to 0) },
            ),
        )
        val evaluation = Evaluator(exampleKb, llm, inbound()).evaluate("lib")
        assertEquals(100, evaluation.total)
        assertEquals(2, evaluation.notes.size)
    }

    @Test
    fun `missing facts lower confidence`() = runTest {
        val missing = (1..5).map { Fact("Field $it", NOT_FOUND) }
        val llm = FakeLlmClient(
            mapOf(
                "guardrail" to { passing },
                "intake" to { Intake(missing) },
                "security" to { security },
                "team-fit" to { team },
                "scoring" to { scoring() },
            ),
        )
        val evaluation = Evaluator(exampleKb, llm, inbound()).evaluate("lib")
        assertEquals(io.github.oianmol.harness.model.Confidence.LOW, evaluation.confidence)
    }

    @Test
    fun `dimension scores clamp at zero and at max`() {
        assertEquals(0, DimensionScore("x", 10, listOf(Deduction(25, "r", "e"))).score)
        assertEquals(10, DimensionScore("x", 10, emptyList()).score)
    }
}
