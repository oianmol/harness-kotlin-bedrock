package io.github.oianmol.harness.pipeline

import io.github.oianmol.harness.kb.KnowledgeBase
import io.github.oianmol.harness.kb.Rubric
import io.github.oianmol.harness.kb.toPromptMarkdown
import io.github.oianmol.harness.llm.Json
import io.github.oianmol.harness.llm.LlmClient
import io.github.oianmol.harness.llm.LlmRequest
import io.github.oianmol.harness.llm.structured
import io.github.oianmol.harness.model.DimensionScore
import io.github.oianmol.harness.model.Gate
import io.github.oianmol.harness.model.GuardrailReport
import io.github.oianmol.harness.model.Intake
import io.github.oianmol.harness.model.ScoringResponse
import io.github.oianmol.harness.model.SecurityFindings
import io.github.oianmol.harness.model.TeamFit
import io.github.oianmol.harness.tools.KbTools

object Prompts {
    fun load(name: String): String =
        Prompts::class.java.getResource("/prompts/$name.md")?.readText()
            ?: error("Missing prompt resource /prompts/$name.md")
}

/** A step's typed result plus anything the harness had to correct on the way. */
data class Checked<T>(val value: T, val notes: List<String> = emptyList())

/**
 * Step 1, blocking. Sees only the active rules, the stack profile and the input: nothing that
 * could talk it out of a rule. Its output is validated before anyone downstream trusts it.
 */
class GuardrailGate(private val kb: KnowledgeBase, private val llm: LlmClient) {
    data class Result(val gate: Gate, val rulesChecked: Int)

    suspend fun check(slug: String, input: String): Checked<Result> {
        val active = kb.guardrails().filter { it.active }
        val report: GuardrailReport = llm.structured(
            LlmRequest(
                purpose = "guardrail",
                slug = slug,
                system = listOf(Prompts.load("guardrail"), active.toPromptMarkdown(), kb.read("stack-profile.md")),
                user = input,
            ),
        )

        val activeIds = active.map { it.id }.toSet()
        val (valid, invalid) = report.triggered.partition { it.ruleId in activeIds }
        val notes = buildList {
            invalid.forEach { add("Gate cited ${it.ruleId}, which is not an active rule; ignored.") }
            if (report.rulesChecked != active.size) {
                add("Gate reported ${report.rulesChecked} rules checked; ${active.size} are active.")
            }
        }
        val gate = if (valid.isEmpty()) Gate.Passed else Gate.Failed(valid.distinctBy { it.ruleId })
        return Checked(Result(gate, active.size), notes)
    }
}

/** Facts table. Runs in parallel with the specialists; its `[NOT FOUND IN SOURCE]` count drives confidence. */
class IntakeExtractor(private val kb: KnowledgeBase, private val llm: LlmClient) {
    suspend fun run(slug: String, input: String): Intake = llm.structured(
        LlmRequest("intake", slug, listOf(Prompts.load("intake"), kb.read("stack-profile.md")), input),
    )
}

/** Specialist: advisories, signing, dependency surface. Gets the input and nothing else. */
class SecurityResearcher(private val llm: LlmClient) {
    suspend fun run(slug: String, input: String): SecurityFindings = llm.structured(
        LlmRequest("security", slug, listOf(Prompts.load("security")), input),
    )
}

/**
 * Specialist: who on the team has used this, and what we decided about similar libraries.
 * Gets read-only knowledge-base tools instead of the whole KB, and pulls what it needs.
 */
class TeamFitMatcher(private val kb: KnowledgeBase, private val llm: LlmClient) {
    suspend fun run(slug: String, input: String): TeamFit = llm.structured(
        LlmRequest(
            purpose = "team-fit",
            slug = slug,
            system = listOf(Prompts.load("team-fit"), kb.read("team.md")),
            user = input,
            tools = KbTools.all(kb),
        ),
    )
}

/**
 * Asks the model for deductions with evidence, per rubric dimension. Never asks it for a total:
 * the arithmetic and the verdict happen in [Rubric], in code.
 */
class Scorer(private val kb: KnowledgeBase, private val llm: LlmClient) {
    data class Scored(val dimensions: List<DimensionScore>, val response: ScoringResponse)

    suspend fun score(
        slug: String,
        input: String,
        rubric: Rubric,
        security: SecurityFindings,
        teamFit: TeamFit,
    ): Checked<Scored> {
        val response: ScoringResponse = llm.structured(
            LlmRequest(
                purpose = "scoring",
                slug = slug,
                system = listOf(
                    Prompts.load("scoring"),
                    kb.read("scoring.md"),
                    kb.read("stack-profile.md"),
                    kb.read("approved-libraries.md"),
                    kb.readAll("decisions"),
                ),
                user = buildString {
                    appendLine(input)
                    appendLine("\n## Security specialist findings\n")
                    appendLine(Json.mapper.writeValueAsString(security))
                    appendLine("\n## Team-fit specialist findings\n")
                    appendLine(Json.mapper.writeValueAsString(teamFit))
                },
            ),
        )

        val notes = mutableListOf<String>()
        val dimensions = rubric.dimensions.map { dim ->
            val assessment = response.dimensions.firstOrNull { it.dimension.equals(dim.name, ignoreCase = true) }
                ?: throw ContractViolation("Scoring response is missing dimension '${dim.name}'")
            val (kept, dropped) = assessment.deductions.partition { it.points > 0 }
            dropped.forEach { notes += "Dropped non-positive deduction in ${dim.name}: '${it.reason}' (${it.points})." }
            DimensionScore(dim.name, dim.weight, kept)
        }
        val known = rubric.dimensions.map { it.name.lowercase() }.toSet()
        response.dimensions.filter { it.dimension.lowercase() !in known }
            .forEach { notes += "Ignored unknown dimension '${it.dimension}' from scoring response." }

        return Checked(Scored(dimensions, response), notes)
    }
}

class ContractViolation(message: String) : RuntimeException(message)
