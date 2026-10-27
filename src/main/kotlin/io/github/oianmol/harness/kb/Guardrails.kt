package io.github.oianmol.harness.kb

const val PLACEHOLDER = "[SET THIS]"

/**
 * One rule from `guardrails.md`. Only [active] rules are ever shown to the model:
 * retired rules and rules with an unfilled `[SET THIS]` threshold are filtered out in code,
 * so the model can't "helpfully" invent a threshold for them.
 */
data class GuardrailRule(
    val id: String,
    val title: String,
    val retired: Boolean,
    val configured: Boolean,
    val text: String,
) {
    val active: Boolean get() = !retired && configured
}

object GuardrailParser {
    private val heading = Regex("""^## (GR-\d{3}): (.+)$""")

    fun parse(markdown: String): List<GuardrailRule> {
        val rules = mutableListOf<GuardrailRule>()
        var current: Pair<MatchResult, MutableList<String>>? = null

        fun flush() {
            val (match, body) = current ?: return
            val (id, title) = match.destructured
            val text = body.joinToString("\n").trim().removeSuffix("---").trim()
            rules += GuardrailRule(
                id = id,
                title = title.replace("[RETIRED]", "").trim(),
                retired = "[RETIRED]" in title,
                configured = PLACEHOLDER !in text,
                text = text,
            )
            current = null
        }

        for (line in markdown.lines()) {
            val match = heading.matchEntire(line)
            when {
                match != null -> { flush(); current = match to mutableListOf() }
                line.startsWith("## ") -> flush()
                else -> current?.second?.add(line)
            }
        }
        flush()

        val duplicates = rules.groupBy { it.id }.filterValues { it.size > 1 }.keys
        if (duplicates.isNotEmpty()) throw KnowledgeBaseException("guardrails.md: duplicate rule IDs $duplicates")
        return rules
    }
}

/** Renders the active rules exactly as the gate sees them. */
fun List<GuardrailRule>.toPromptMarkdown(): String =
    filter { it.active }.joinToString("\n\n") { "## ${it.id}: ${it.title}\n\n${it.text}" }
