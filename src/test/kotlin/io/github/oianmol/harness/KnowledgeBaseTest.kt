package io.github.oianmol.harness

import io.github.oianmol.harness.kb.GuardrailParser
import io.github.oianmol.harness.kb.KnowledgeBaseException
import io.github.oianmol.harness.kb.Outcome
import io.github.oianmol.harness.kb.toPromptMarkdown
import io.github.oianmol.harness.model.Confidence
import io.github.oianmol.harness.model.Verdict
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KnowledgeBaseTest {

    @Test
    fun `guardrails parse with retired and unconfigured rules excluded from active`() {
        val rules = exampleKb.guardrails()
        assertEquals(listOf("GR-001", "GR-002", "GR-003", "GR-004", "GR-005", "GR-006", "GR-007"), rules.map { it.id })
        assertTrue(rules.single { it.id == "GR-004" }.retired)
        assertFalse(rules.single { it.id == "GR-006" }.configured)
        assertEquals(listOf("GR-001", "GR-002", "GR-003", "GR-005", "GR-007"), rules.filter { it.active }.map { it.id })
    }

    @Test
    fun `the gate prompt never contains retired or unconfigured rules`() {
        val prompt = exampleKb.guardrails().toPromptMarkdown()
        assertTrue("GR-001" in prompt)
        assertFalse("GR-004" in prompt)
        assertFalse("GR-006" in prompt)
        assertFalse("[SET THIS]" in prompt)
    }

    @Test
    fun `duplicate rule ids are rejected`() {
        val markdown = "## GR-001: A\n\nx\n\n## GR-001: B\n\ny\n"
        assertFailsWith<KnowledgeBaseException> { GuardrailParser.parse(markdown) }
    }

    @Test
    fun `rubric weights sum to 100 and thresholds come from scoring md`() {
        val rubric = exampleKb.rubric()
        assertEquals(listOf("Maintenance health", "Security posture", "Stack fit", "Team familiarity"), rubric.dimensions.map { it.name })
        assertEquals(100, rubric.maxTotal)
        assertEquals(75, rubric.adoptAt)
        assertEquals(55, rubric.trialAt)
    }

    @Test
    fun `verdict boundaries`() {
        val rubric = exampleKb.rubric()
        assertEquals(Verdict.ADOPT, rubric.verdictFor(75))
        assertEquals(Verdict.TRIAL, rubric.verdictFor(74))
        assertEquals(Verdict.TRIAL, rubric.verdictFor(55))
        assertEquals(Verdict.REJECT, rubric.verdictFor(54))
    }

    @Test
    fun `confidence bands`() {
        val rubric = exampleKb.rubric()
        assertEquals(Confidence.HIGH, rubric.confidenceFor(0))
        assertEquals(Confidence.HIGH, rubric.confidenceFor(1))
        assertEquals(Confidence.MEDIUM, rubric.confidenceFor(3))
        assertEquals(Confidence.LOW, rubric.confidenceFor(4))
        assertEquals(Confidence.LOW, rubric.confidenceFor(40))
    }

    @Test
    fun `decisions parse with outcomes and provenance`() {
        val decisions = exampleKb.decisions()
        assertEquals(9, decisions.size)
        assertEquals(Outcome.REVERSED, decisions.single { it.library == "io.example:wiring-di" }.outcome)
        assertEquals("prior-team", decisions.single { it.library == "io.example:yamlkt" }.source)
    }

    @Test
    fun `reads are confined to the knowledge base`() {
        assertFailsWith<KnowledgeBaseException> { exampleKb.read("../README.md") }
        assertFailsWith<KnowledgeBaseException> { exampleKb.read("decisions/../../build.gradle.kts") }
        assertFailsWith<KnowledgeBaseException> { exampleKb.read("/etc/passwd") }
    }

    @Test
    fun `readAll is sorted and tagged so the prompt prefix is stable`() {
        val all = exampleKb.readAll("decisions")
        val paths = Regex("""<file path="([^"]+)">""").findAll(all).map { it.groupValues[1] }.toList()
        assertEquals(paths.sorted(), paths)
        assertEquals(9, paths.size)
        assertEquals(all, exampleKb.readAll("decisions"))
    }
}
