package io.github.oianmol.harness.kb

/**
 * Minimal YAML-frontmatter reader: flat `key: value` pairs between two `---` lines.
 * Trailing `# comments` are stripped. Anything richer belongs in the body, not the frontmatter.
 */
data class Frontmatter(val fields: Map<String, String>, val body: String) {
    operator fun get(key: String): String? = fields[key]

    fun require(key: String, source: String): String =
        fields[key] ?: throw KnowledgeBaseException("$source: missing frontmatter field '$key'")

    companion object {
        fun parse(text: String): Frontmatter {
            val lines = text.lines()
            if (lines.firstOrNull()?.trim() != "---") return Frontmatter(emptyMap(), text)
            val end = lines.drop(1).indexOfFirst { it.trim() == "---" }
            if (end < 0) return Frontmatter(emptyMap(), text)

            val fields = lines.subList(1, end + 1)
                .mapNotNull { line ->
                    val idx = line.indexOf(':')
                    if (idx <= 0) return@mapNotNull null
                    val key = line.substring(0, idx).trim()
                    val value = line.substring(idx + 1).substringBefore(" #").trim()
                    key to value
                }
                .toMap()
            val body = lines.drop(end + 2).joinToString("\n")
            return Frontmatter(fields, body)
        }
    }
}

class KnowledgeBaseException(message: String) : RuntimeException(message)
