package io.github.oianmol.harness.llm

import com.anthropic.bedrock.backends.BedrockMantleBackend
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.models.messages.CacheControlEphemeral
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.JsonOutputFormat
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import com.anthropic.models.messages.TextBlockParam
import com.anthropic.models.messages.Tool
import com.anthropic.models.messages.ToolResultBlockParam
import com.anthropic.models.messages.ToolUseBlock
import io.github.oianmol.harness.tools.KbTool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.jvm.optionals.getOrNull

/**
 * Claude on Amazon Bedrock through Anthropic's Java SDK and its Bedrock (Mantle) backend.
 *
 * What Claude Code gave us for free is spelled out here: the tool loop, the structured-output
 * contract, prompt caching of the knowledge base, and explicit handling of every stop reason.
 */
class BedrockLlmClient(
    private val client: AnthropicClient = defaultClient(),
    private val model: String = System.getenv("HARNESS_MODEL") ?: DEFAULT_MODEL,
    private val maxTokens: Long = 16_000L,
    private val maxToolTurns: Int = 10,
) : LlmClient {

    override suspend fun <T : Any> structured(request: LlmRequest, type: Class<T>): T = withContext(Dispatchers.IO) {
        val params = MessageCreateParams.builder()
            .model(model)
            .maxTokens(maxTokens)
            .systemOfTextBlockParams(systemBlocks(request.system))
            .outputConfig(OutputConfig.builder().format(outputFormatFor(type)).build())
            .addUserMessage(request.user)
        request.tools.forEach { params.addTool(it.toSdkTool()) }

        for (turn in 1..maxToolTurns) {
            val response = client.messages().create(params.build())
            when (response.stopReason().getOrNull()) {
                StopReason.END_TURN -> return@withContext parse(response, type, request)
                StopReason.TOOL_USE -> {
                    // Append the whole assistant turn (thinking blocks included), then every result
                    // in ONE user message, so the model keeps making parallel calls.
                    params.addMessage(response)
                    val results = response.content()
                        .mapNotNull { it.toolUse().getOrNull() }
                        .map { runTool(request.tools, it) }
                    params.addUserMessageOfContentBlockParams(results)
                }
                StopReason.REFUSAL -> throw LlmException("[${request.purpose}/${request.slug}] model declined the request")
                StopReason.MAX_TOKENS -> throw LlmException("[${request.purpose}/${request.slug}] hit max_tokens=$maxTokens; output truncated")
                else -> throw LlmException("[${request.purpose}/${request.slug}] unexpected stop reason ${response.stopReason()}")
            }
        }
        throw LlmException("[${request.purpose}/${request.slug}] no final answer after $maxToolTurns tool turns")
    }

    private fun <T : Any> parse(response: Message, type: Class<T>, request: LlmRequest): T {
        val text = response.content().mapNotNull { it.text().getOrNull()?.text() }.joinToString("")
        return try {
            Json.mapper.readValue(text, type)
        } catch (e: Exception) {
            throw LlmException("[${request.purpose}/${request.slug}] response did not match ${type.simpleName}", e)
        }
    }

    private fun runTool(tools: List<KbTool>, call: ToolUseBlock): ContentBlockParam {
        val tool = tools.firstOrNull { it.name == call.name() }
        val result = if (tool == null) {
            io.github.oianmol.harness.tools.ToolResult("Unknown tool '${call.name()}'", isError = true)
        } else {
            val input = call._input().convert(Map::class.java)
                ?.entries?.associate { (k, v) -> k.toString() to v.toString() }
                .orEmpty()
            tool.run(input)
        }
        return ContentBlockParam.ofToolResult(
            ToolResultBlockParam.builder()
                .toolUseId(call.id())
                .content(result.content)
                .isError(result.isError)
                .build(),
        )
    }

    companion object {
        /** Bedrock model IDs carry the `anthropic.` prefix. Override with HARNESS_MODEL. */
        const val DEFAULT_MODEL = "anthropic.claude-opus-5-5"

        /** AWS credentials and region come from the standard AWS environment / profile chain. */
        fun defaultClient(): AnthropicClient =
            AnthropicOkHttpClient.builder()
                .backend(BedrockMantleBackend.fromEnv())
                .build()

        /**
         * Derives the JSON schema the model must fill from a Kotlin data class, using the SDK's own
         * class-to-schema support (which also validates the schema locally). We keep the raw
         * format rather than the SDK's typed wrapper because the tool loop needs plain params.
         */
        @Suppress("UNCHECKED_CAST")
        fun outputFormatFor(type: Class<*>): JsonOutputFormat =
            MessageCreateParams.builder()
                .model(DEFAULT_MODEL)
                .maxTokens(1L)
                .addUserMessage("schema")
                .outputConfig(type as Class<Any>)
                .build()
                .rawParams
                .outputConfig().get()
                .format().get()

        /** The knowledge base is the stable prefix of every call: cache up to the last block. */
        fun systemBlocks(blocks: List<String>): List<TextBlockParam> =
            blocks.mapIndexed { i, text ->
                TextBlockParam.builder()
                    .text(text)
                    .apply { if (i == blocks.lastIndex) cacheControl(CacheControlEphemeral.builder().build()) }
                    .build()
            }
    }
}

internal fun KbTool.toSdkTool(): Tool {
    val properties = Tool.InputSchema.Properties.builder()
    parameters.forEach { (param, description) ->
        properties.putAdditionalProperty(param, JsonValue.from(mapOf("type" to "string", "description" to description)))
    }
    return Tool.builder()
        .name(name)
        .description(description)
        .inputSchema(
            Tool.InputSchema.builder()
                .properties(properties.build())
                .required(parameters.keys.toList())
                .build(),
        )
        .build()
}
