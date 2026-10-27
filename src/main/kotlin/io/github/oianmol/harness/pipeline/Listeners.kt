package io.github.oianmol.harness.pipeline

import com.fasterxml.jackson.databind.SerializationFeature
import io.github.oianmol.harness.llm.Json
import io.github.oianmol.harness.model.Evaluation
import io.github.oianmol.harness.model.Gate
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.LocalDate
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readLines

/** Prints one line per evaluation. */
class ConsoleNotifier : EvaluationListener {
    override fun onEvaluationWritten(evaluation: Evaluation, file: Path) {
        println(summaryLine(evaluation) + "  ->  $file")
    }
}

/**
 * Posts the summary line to a Slack-compatible incoming webhook. The model never sees the URL;
 * it lives in the environment of the process that runs the harness.
 */
class WebhookNotifier(
    private val webhookUrl: String,
    private val http: HttpClient = HttpClient.newHttpClient(),
) : EvaluationListener {
    override fun onEvaluationWritten(evaluation: Evaluation, file: Path) {
        val body = Json.mapper.writeValueAsString(mapOf("text" to summaryLine(evaluation)))
        val request = HttpRequest.newBuilder(URI.create(webhookUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.discarding())
        if (response.statusCode() !in 200..299) System.err.println("Webhook returned ${response.statusCode()}")
    }
}

/** One JSON line per evaluation. The health check reads it for firing rates and verdict drift. */
data class LedgerEntry(
    val slug: String,
    val date: String,
    val verdict: String,
    val total: Int?,
    val confidence: String?,
    val triggeredRules: List<String>,
)

class LedgerListener(private val ledger: Path, private val today: () -> LocalDate = LocalDate::now) : EvaluationListener {
    override fun onEvaluationWritten(evaluation: Evaluation, file: Path) {
        ledger.parent?.createDirectories()
        val entry = LedgerEntry(
            slug = evaluation.slug,
            date = today().toString(),
            verdict = evaluation.verdict.name,
            total = evaluation.total,
            confidence = evaluation.confidence?.name,
            triggeredRules = (evaluation.gate as? Gate.Failed)?.hits?.map { it.ruleId }.orEmpty(),
        )
        val line = Json.mapper.copy().disable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(entry)
        Files.writeString(ledger, line + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND)
    }

    companion object {
        fun read(ledger: Path): List<LedgerEntry> =
            if (!ledger.exists()) emptyList()
            else ledger.readLines().filter { it.isNotBlank() }.map { Json.mapper.readValue(it, LedgerEntry::class.java) }
    }
}

fun summaryLine(e: Evaluation): String {
    val score = e.total?.let { " $it/100" } ?: ""
    val why = when (val gate = e.gate) {
        is Gate.Failed -> " (guardrail ${gate.hits.joinToString { it.ruleId }})"
        Gate.Passed -> e.confidence?.let { ", confidence $it" } ?: ""
    }
    return "${e.slug}: ${e.verdict}$score$why"
}
