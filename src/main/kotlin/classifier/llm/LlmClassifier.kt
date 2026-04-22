package classifier.llm

import model.LogAnalysis
import parser.ParsedLog

/**
 * Task 10: LLM-based fallback classifier.
 *
 * Wraps the Koog agent and maps its response to LogAnalysis
 * with classifiedBy = ClassifierSource.LLM.
 *
 * Only called when DeterministicClassifier returns null.
 */
class LlmClassifier {

    suspend fun classify(log: ParsedLog): LogAnalysis {
        TODO("Task 10: implement LLM classifier using Koog agent")
    }
}
