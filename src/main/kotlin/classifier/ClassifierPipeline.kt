package classifier

import classifier.deterministic.DeterministicClassifier
import classifier.llm.LlmClassifier
import model.LogAnalysis
import parser.ParsedLog

/**
 * Composes deterministic and LLM classifiers.
 *
 * Contract:
 *   1. DeterministicClassifier always runs first (zero LLM cost)
 *   2. LlmClassifier called ONLY when deterministic returns null
 *   3. classifiedBy is always set on the returned LogAnalysis
 */
class ClassifierPipeline(
    private val deterministic: (ParsedLog) -> LogAnalysis? = DeterministicClassifier()::classify,
    private val llm: suspend (ParsedLog) -> LogAnalysis = LlmClassifier()::classify
) {

    suspend fun classify(log: ParsedLog): LogAnalysis =
        deterministic(log) ?: llm(log)
}
