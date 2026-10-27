package io.github.oianmol.harness.kb

import java.nio.file.Path
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.walk

/**
 * The knowledge base is a directory of markdown files. This class is the only way the harness
 * reads it, and every read is confined to [root]: the same rule backs the model-facing tools.
 */
class KnowledgeBase(root: Path) {
    val root: Path = root.toAbsolutePath().normalize()

    init {
        if (!this.root.isDirectory()) throw KnowledgeBaseException("Knowledge base not found at ${this.root}")
    }

    /** Resolves [relative] inside the knowledge base, refusing anything that escapes it. */
    fun resolve(relative: String): Path {
        val path = root.resolve(relative).normalize()
        if (!path.startsWith(root)) throw KnowledgeBaseException("Path escapes the knowledge base: $relative")
        return path
    }

    fun read(relative: String): String {
        val path = resolve(relative)
        if (!path.isRegularFile()) throw KnowledgeBaseException("No such knowledge-base file: $relative")
        return path.readText()
    }

    fun exists(relative: String): Boolean = runCatching { resolve(relative).isRegularFile() }.getOrDefault(false)

    /** Markdown files under [dir], relative to the root, in a stable order. */
    fun list(dir: String = ""): List<String> {
        val base = resolve(dir)
        if (!base.isDirectory()) return emptyList()
        return base.walk()
            .filter { it.isRegularFile() && it.toString().endsWith(".md") }
            .map { it.relativeTo(root).invariantSeparatorsPathString }
            .sorted()
            .toList()
    }

    /**
     * "Read ALL files in dir/" as one prompt block. Sorted, so the same KB always produces the
     * same bytes: a different order would be a different prompt prefix and a cache miss.
     */
    fun readAll(dir: String): String =
        list(dir).joinToString("\n\n") { "<file path=\"$it\">\n${read(it).trim()}\n</file>" }

    fun guardrails(): List<GuardrailRule> = GuardrailParser.parse(read("guardrails.md"))

    fun rubric(): Rubric = RubricParser.parse(read("scoring.md"))

    fun decisions(): List<Decision> = list("decisions").map { Decision.parse(it, read(it)) }
}
