package io.github.oianmol.harness.tools

import io.github.oianmol.harness.kb.KnowledgeBase
import io.github.oianmol.harness.kb.KnowledgeBaseException

/**
 * A tool the model may call. Inputs are flat string parameters; that is all a knowledge-base
 * tool needs, and it keeps the schema trivially valid.
 *
 * This is the Bedrock-side replacement for Claude Code's permission system: the only things
 * a specialist can do are the handlers you write here, so write them read-only and scoped.
 */
class KbTool(
    val name: String,
    val description: String,
    /** Parameter name to description. Every parameter is a required string. */
    val parameters: Map<String, String>,
    private val handler: (Map<String, String>) -> String,
) {
    /** Runs the tool. Failures become an error string for the model, not an exception. */
    fun run(input: Map<String, String>): ToolResult = try {
        val missing = parameters.keys - input.keys
        if (missing.isNotEmpty()) ToolResult("Missing required parameters: $missing", isError = true)
        else ToolResult(handler(input), isError = false)
    } catch (e: KnowledgeBaseException) {
        ToolResult(e.message ?: "Knowledge-base error", isError = true)
    }
}

data class ToolResult(val content: String, val isError: Boolean)

object KbTools {
    private const val MAX_SEARCH_HITS = 40

    fun listFiles(kb: KnowledgeBase) = KbTool(
        name = "list_kb_files",
        description = "List markdown files in a knowledge-base directory. Use an empty string for the root.",
        parameters = mapOf("directory" to "Directory relative to the knowledge base, e.g. 'decisions' or ''"),
    ) { input -> kb.list(input.getValue("directory")).joinToString("\n").ifEmpty { "(no files)" } }

    fun readFile(kb: KnowledgeBase) = KbTool(
        name = "read_kb_file",
        description = "Read one knowledge-base file by its path relative to the knowledge base root.",
        parameters = mapOf("path" to "File path relative to the knowledge base, e.g. 'decisions/quickhttp.md'"),
    ) { input -> kb.read(input.getValue("path")) }

    fun search(kb: KnowledgeBase) = KbTool(
        name = "search_kb",
        description = "Case-insensitive text search across all knowledge-base files. Returns path:line: text for each hit.",
        parameters = mapOf("query" to "Text to search for, e.g. a library name or a technology"),
    ) { input ->
        val query = input.getValue("query").trim()
        if (query.isEmpty()) throw KnowledgeBaseException("Query must not be empty")
        kb.list().asSequence()
            .flatMap { path ->
                kb.read(path).lineSequence().mapIndexedNotNull { i, line ->
                    if (line.contains(query, ignoreCase = true)) "$path:${i + 1}: ${line.trim()}" else null
                }
            }
            .take(MAX_SEARCH_HITS)
            .joinToString("\n")
            .ifEmpty { "No matches for '$query'." }
    }

    fun all(kb: KnowledgeBase): List<KbTool> = listOf(listFiles(kb), readFile(kb), search(kb))
}
