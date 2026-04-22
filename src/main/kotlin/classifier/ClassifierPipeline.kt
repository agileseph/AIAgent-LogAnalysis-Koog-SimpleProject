package classifier

import classifier.deterministic.DeterministicClassifier
import classifier.llm.LlmClassifier
import model.LogAnalysis
import parser.ParsedLog

/**
 * Task 11: Composes deterministic and LLM classifiers.
 *
 * Contract:
 *   1. DeterministicClassifier always runs first (zero LLM cost)
 *   2. LlmClassifier called ONLY when deterministic returns null
 *   3. classifiedBy is always set on the returned LogAnalysis
 */
class ClassifierPipeline(
    private val deterministic: DeterministicClassifier = DeterministicClassifier(),
    private val llm: LlmClassifier = LlmClassifier()
) {

    suspend fun classify(log: ParsedLog): LogAnalysis {
        TODO("Task 11: implement pipeline composition")
    }
}
