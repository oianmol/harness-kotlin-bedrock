package io.github.oianmol.harness.model

/** The literal marker every step uses instead of guessing. Greppable, countable, never a value. */
const val NOT_FOUND = "[NOT FOUND IN SOURCE]"

// ---------------------------------------------------------------------------------------------
// Contracts the model fills. Each is the output schema of exactly one call.
// ---------------------------------------------------------------------------------------------

data class RuleHit(val ruleId: String, val evidence: String)

data class GuardrailReport(val rulesChecked: Int, val triggered: List<RuleHit>)

data class Fact(val field: String, val value: String)

data class Intake(val facts: List<Fact>)

data class Advisory(val id: String, val cvss: Double, val fixedIn: String)

data class SecurityFindings(
    val openAdvisories: List<Advisory>,
    val signedArtifacts: String,
    val transitiveDependencies: String,
    val summary: String,
)

data class EngineerMatch(val engineer: String, val evidence: String, val priorCompany: Boolean)

data class TeamFit(
    val matches: List<EngineerMatch>,
    val relatedDecisions: List<String>,
    val summary: String,
)

data class Deduction(val points: Int, val reason: String, val evidence: String)

data class DimensionAssessment(val dimension: String, val deductions: List<Deduction>)

data class ScoringResponse(
    val dimensions: List<DimensionAssessment>,
    val risks: List<String>,
    val recommendation: String,
)

// ---------------------------------------------------------------------------------------------
// Values the harness computes. The model never produces these directly.
// ---------------------------------------------------------------------------------------------

sealed interface Gate {
    data object Passed : Gate
    data class Failed(val hits: List<RuleHit>) : Gate
}

enum class Verdict { ADOPT, TRIAL, REJECT }

enum class Confidence { HIGH, MEDIUM, LOW }

data class DimensionScore(val name: String, val max: Int, val deductions: List<Deduction>) {
    val score: Int get() = (max - deductions.sumOf { it.points }).coerceIn(0, max)
}

data class Evaluation(
    val slug: String,
    val verdict: Verdict,
    val gate: Gate,
    val rulesChecked: Int,
    /** Null when a guardrail disqualified the library before intake ran. */
    val confidence: Confidence? = null,
    val facts: List<Fact> = emptyList(),
    val dimensions: List<DimensionScore> = emptyList(),
    val security: SecurityFindings? = null,
    val teamFit: TeamFit? = null,
    val risks: List<String> = emptyList(),
    val recommendation: String = "",
    /** Contract problems the harness caught and corrected. Surfaced in the evaluation, never hidden. */
    val notes: List<String> = emptyList(),
) {
    val total: Int? get() = if (gate is Gate.Passed) dimensions.sumOf { it.score } else null
    val disqualified: Boolean get() = gate is Gate.Failed
}
