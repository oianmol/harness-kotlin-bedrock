package io.github.oianmol.harness.llm

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oianmol.harness.tools.KbTool

/**
 * One call to the model with a typed result. Every step of the harness goes through this
 * interface, which is what lets the same pipeline run against Bedrock, recorded fixtures,
 * or a fake in tests.
 */
interface LlmClient {
    suspend fun <T : Any> structured(request: LlmRequest, type: Class<T>): T
}

suspend inline fun <reified T : Any> LlmClient.structured(request: LlmRequest): T =
    structured(request, T::class.java)

data class LlmRequest(
    /** Which step this is: `guardrail`, `intake`, `security`, `team-fit`, `scoring`. */
    val purpose: String,
    val slug: String,
    /**
     * Stable system blocks, most stable first. The last block carries the cache breakpoint,
     * so put per-request content in [user], never here.
     */
    val system: List<String>,
    val user: String,
    val tools: List<KbTool> = emptyList(),
)

class LlmException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

object Json {
    val mapper: ObjectMapper = jacksonObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .enable(SerializationFeature.INDENT_OUTPUT)
}
