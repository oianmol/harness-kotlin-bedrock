package io.github.oianmol.harness.kb

import io.github.oianmol.harness.model.Confidence
import io.github.oianmol.harness.model.Verdict

data class RubricDimension(val name: String, val weight: Int)

data class ConfidenceBand(val minMissing: Int, val maxMissing: Int, val confidence: Confidence)

/**
 * The scoring policy, parsed from `scoring.md`. Weights, thresholds and confidence bands all
 * live in the markdown file so a policy change is a reviewed diff, not a code change.
 */
data class Rubric(
    val dimensions: List<RubricDimension>,
    val adoptAt: Int,
    val trialAt: Int,
    val confidenceBands: List<ConfidenceBand>,
) {
    val maxTotal: Int get() = dimensions.sumOf { it.weight }

    fun verdictFor(total: Int): Verdict = when {
        total >= adoptAt -> Verdict.ADOPT
        total >= trialAt -> Verdict.TRIAL
        else -> Verdict.REJECT
    }

    fun confidenceFor(missingFields: Int): Confidence =
        confidenceBands.first { missingFields in it.minMissing..it.maxMissing }.confidence
}

object RubricParser {
    private val dimensionRow = Regex("""^\|\s*([A-Za-z][^|]*?)\s*\|\s*(\d+)\s*\|$""")
    private val adopt = Regex("""ADOPT\s*>=\s*(\d+)""")
    private val trial = Regex("""TRIAL\s*(\d+)\s*-\s*(\d+)""")
    private val confidenceRow = Regex("""^\|\s*(\d+)\s*(?:-\s*(\d+)|(\+))\s*\|\s*(HIGH|MEDIUM|LOW)\s*\|$""")

    fun parse(markdown: String): Rubric {
        val dimensionsSection = section(markdown, "## Dimensions")
        val dimensions = dimensionsSection.lines()
            .takeWhile { !it.startsWith("### ") }
            .mapNotNull { dimensionRow.matchEntire(it.trim()) }
            .map { RubricDimension(it.groupValues[1], it.groupValues[2].toInt()) }
        if (dimensions.isEmpty()) throw KnowledgeBaseException("scoring.md: no dimensions table under '## Dimensions'")

        val adoptAt = adopt.find(markdown)?.groupValues?.get(1)?.toInt()
            ?: throw KnowledgeBaseException("scoring.md: missing 'ADOPT >= N' threshold")
        val trialAt = trial.find(markdown)?.groupValues?.get(1)?.toInt()
            ?: throw KnowledgeBaseException("scoring.md: missing 'TRIAL N-M' threshold")

        val bands = section(markdown, "## Confidence").lines()
            .mapNotNull { confidenceRow.matchEntire(it.trim()) }
            .map {
                val min = it.groupValues[1].toInt()
                val max = if (it.groupValues[3] == "+") Int.MAX_VALUE else it.groupValues[2].toInt()
                ConfidenceBand(min, max, Confidence.valueOf(it.groupValues[4]))
            }
        if (bands.isEmpty()) throw KnowledgeBaseException("scoring.md: no confidence table under '## Confidence'")

        return Rubric(dimensions, adoptAt, trialAt, bands)
    }

    private fun section(markdown: String, heading: String): String {
        val start = markdown.indexOf(heading)
        if (start < 0) throw KnowledgeBaseException("scoring.md: missing section '$heading'")
        val rest = markdown.substring(start + heading.length)
        val end = Regex("""^## """, RegexOption.MULTILINE).find(rest)?.range?.first ?: rest.length
        return rest.substring(0, end)
    }
}
