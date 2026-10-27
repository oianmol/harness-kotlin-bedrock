package io.github.oianmol.harness.llm

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Replays recorded model responses from `examples/recorded/<slug>/<purpose>.json`.
 *
 * This is how the demo and CI run without AWS credentials. Everything except the model call
 * itself (KB parsing, gating, validation, scoring, rendering, listeners) runs for real.
 */
class ReplayLlmClient(private val recordings: Path) : LlmClient {
    override suspend fun <T : Any> structured(request: LlmRequest, type: Class<T>): T {
        val file = recordingPath(recordings, request)
        if (!file.exists()) {
            throw LlmException("No recording for ${request.purpose}/${request.slug} at $file. Run once live with --record.")
        }
        return Json.mapper.readValue(file.readText(), type)
    }
}

/** Wraps a live client and saves each response so it can be replayed offline later. */
class RecordingLlmClient(private val delegate: LlmClient, private val recordings: Path) : LlmClient {
    override suspend fun <T : Any> structured(request: LlmRequest, type: Class<T>): T {
        val result = delegate.structured(request, type)
        val file = recordingPath(recordings, request)
        file.parent.createDirectories()
        file.writeText(Json.mapper.writeValueAsString(result) + "\n")
        return result
    }
}

internal fun recordingPath(recordings: Path, request: LlmRequest): Path =
    recordings.resolve(request.slug).resolve("${request.purpose}.json")
