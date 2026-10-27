package io.github.oianmol.harness.cli

import io.github.oianmol.harness.health.HealthCheck
import io.github.oianmol.harness.kb.KnowledgeBase
import io.github.oianmol.harness.llm.BedrockLlmClient
import io.github.oianmol.harness.llm.LlmClient
import io.github.oianmol.harness.llm.RecordingLlmClient
import io.github.oianmol.harness.llm.ReplayLlmClient
import io.github.oianmol.harness.pipeline.ConsoleNotifier
import io.github.oianmol.harness.pipeline.EvaluationListener
import io.github.oianmol.harness.pipeline.Evaluator
import io.github.oianmol.harness.pipeline.LedgerListener
import io.github.oianmol.harness.pipeline.WebhookNotifier
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.system.exitProcess

private const val USAGE = """
Usage: harness <command> [options]

Commands:
  evaluate <slug>     Evaluate inbound/<slug>/raw.md and write inbound/<slug>/evaluation.md
  evaluate-all        Evaluate every folder under inbound/
  rules               List guardrail rules and whether each is enforced
  health              Audit the knowledge base and the evaluation ledger

Options:
  --offline           Replay recorded model responses from examples/recorded/ (no AWS needed)
  --record            Call Bedrock and save responses to examples/recorded/ for later replay

Environment:
  AWS_REGION, AWS_PROFILE / AWS credentials   Bedrock access (live mode)
  HARNESS_MODEL                               Override the Bedrock model ID
  HARNESS_WEBHOOK_URL                         Post a summary line per evaluation
"""

private val kbRoot = Path("knowledge-base")
private val inbound = Path("inbound")
private val recordings = Path("examples/recorded")
private val ledgerFile = Path("evaluations/ledger.jsonl")

fun main(args: Array<String>) {
    val flags = args.filter { it.startsWith("--") }.toSet()
    val positional = args.filterNot { it.startsWith("--") }
    val command = positional.firstOrNull() ?: run { println(USAGE); exitProcess(1) }

    val kb = KnowledgeBase(kbRoot)
    when (command) {
        "evaluate" -> {
            val slug = positional.getOrNull(1) ?: run { println(USAGE); exitProcess(1) }
            runBlocking { evaluator(kb, flags).evaluate(slug) }
        }
        "evaluate-all" -> {
            val evaluator = evaluator(kb, flags)
            runBlocking { slugs().forEach { evaluator.evaluate(it) } }
        }
        "rules" -> kb.guardrails().forEach {
            val status = when {
                it.retired -> "retired"
                !it.configured -> "NOT CONFIGURED"
                else -> "active"
            }
            println("${it.id}  %-15s %s".format(status, it.title))
        }
        "health" -> print(HealthCheck.format(HealthCheck(kb, LedgerListener.read(ledgerFile)).run()))
        else -> { println(USAGE); exitProcess(1) }
    }
}

private fun evaluator(kb: KnowledgeBase, flags: Set<String>): Evaluator {
    val llm: LlmClient = when {
        "--offline" in flags -> ReplayLlmClient(recordings)
        "--record" in flags -> RecordingLlmClient(BedrockLlmClient(), recordings)
        else -> BedrockLlmClient()
    }
    val listeners = buildList<EvaluationListener> {
        add(ConsoleNotifier())
        add(LedgerListener(ledgerFile))
        System.getenv("HARNESS_WEBHOOK_URL")?.let { add(WebhookNotifier(it)) }
    }
    return Evaluator(kb, llm, inbound, listeners)
}

private fun slugs(): List<String> =
    inbound.listDirectoryEntries()
        .filter { it.isDirectory() && it.resolve("raw.md").isRegularFile() }
        .map(Path::name)
        .sorted()
