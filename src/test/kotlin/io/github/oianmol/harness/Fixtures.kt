package io.github.oianmol.harness

import io.github.oianmol.harness.kb.KnowledgeBase
import io.github.oianmol.harness.llm.LlmClient
import io.github.oianmol.harness.llm.LlmRequest
import java.nio.file.Path
import kotlin.io.path.Path

/** Tests run from the project root, so the real example knowledge base doubles as the fixture. */
val exampleKb = KnowledgeBase(Path("knowledge-base"))
val exampleInbound: Path = Path("inbound")
val exampleRecordings: Path = Path("examples/recorded")

/**
 * Answers each step from a map keyed by purpose and records every request it saw,
 * so tests can assert both on results and on what the model was (or wasn't) asked.
 */
class FakeLlmClient(private val responses: Map<String, suspend (LlmRequest) -> Any>) : LlmClient {
    val requests = mutableListOf<LlmRequest>()

    override suspend fun <T : Any> structured(request: LlmRequest, type: Class<T>): T {
        synchronized(requests) { requests += request }
        val respond = responses[request.purpose] ?: error("Unexpected call: ${request.purpose}")
        return type.cast(respond(request))
    }

    fun purposes(): List<String> = synchronized(requests) { requests.map { it.purpose } }
}
