package io.github.oianmol.harness.pipeline

import io.github.oianmol.harness.kb.KnowledgeBase
import io.github.oianmol.harness.llm.LlmClient
import io.github.oianmol.harness.model.Evaluation
import io.github.oianmol.harness.model.Gate
import io.github.oianmol.harness.model.NOT_FOUND
import io.github.oianmol.harness.model.Verdict
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.writeText

/** The Bedrock equivalent of a PostToolUse hook: plain code that runs after an evaluation is written. */
fun interface EvaluationListener {
    fun onEvaluationWritten(evaluation: Evaluation, file: Path)
}

/**
 * The `/evaluate` runbook as code:
 *
 * 1. Guardrail gate (blocking). A failure short-circuits to REJECT; nothing else is paid for.
 * 2. Intake, security and team-fit run in parallel, like subagents.
 * 3. Scoring: the model returns deductions, the rubric computes the total and verdict.
 * 4. The evaluation is rendered to `inbound/<slug>/evaluation.md` and listeners fire.
 */
class Evaluator(
    private val kb: KnowledgeBase,
    llm: LlmClient,
    private val inbound: Path,
    private val listeners: List<EvaluationListener> = emptyList(),
) {
    private val gate = GuardrailGate(kb, llm)
    private val intake = IntakeExtractor(kb, llm)
    private val security = SecurityResearcher(llm)
    private val teamFit = TeamFitMatcher(kb, llm)
    private val scorer = Scorer(kb, llm)

    suspend fun evaluate(slug: String): Evaluation = coroutineScope {
        val input = readInput(slug)
        val rubric = kb.rubric()

        val gateCheck = gate.check(slug, input)
        val (gateResult, rulesChecked) = gateCheck.value
        if (gateResult is Gate.Failed) {
            return@coroutineScope write(
                Evaluation(slug, Verdict.REJECT, gateResult, rulesChecked, notes = gateCheck.notes),
            )
        }

        val facts = async { intake.run(slug, input) }
        val findings = async { security.run(slug, input) }
        val team = async { teamFit.run(slug, input) }

        val scored = scorer.score(slug, input, rubric, findings.await(), team.await())
        val dimensions = scored.value.dimensions
        val intakeFacts = facts.await().facts
        val missing = intakeFacts.count { NOT_FOUND in it.value }

        write(
            Evaluation(
                slug = slug,
                verdict = rubric.verdictFor(dimensions.sumOf { it.score }),
                gate = gateResult,
                rulesChecked = rulesChecked,
                confidence = rubric.confidenceFor(missing),
                facts = intakeFacts,
                dimensions = dimensions,
                security = findings.await(),
                teamFit = team.await(),
                risks = scored.value.response.risks,
                recommendation = scored.value.response.recommendation,
                notes = gateCheck.notes + scored.notes,
            ),
        )
    }

    private fun readInput(slug: String): String {
        val file = inbound.resolve(slug).resolve("raw.md")
        require(file.isRegularFile()) { "No intake found at $file" }
        return file.readText()
    }

    private fun write(evaluation: Evaluation): Evaluation {
        val file = inbound.resolve(evaluation.slug).resolve("evaluation.md")
        file.parent.createDirectories()
        file.writeText(EvaluationRenderer.render(evaluation, kb.rubric()))
        listeners.forEach { it.onEvaluationWritten(evaluation, file) }
        return evaluation
    }
}
