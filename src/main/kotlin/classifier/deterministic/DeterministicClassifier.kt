package classifier.deterministic

import model.LogAnalysis
import parser.ParsedLog

/**
 * Task 6: Orchestrates deterministic rule classifiers.
 *
 * Returns null when no rule matches — signals LLM fallback required.
 * Never calls any external service.
 */
class DeterministicClassifier {

    private val iosClassifier = IosRuleClassifier()
    private val androidClassifier = AndroidRuleClassifier()

    fun classify(log: ParsedLog): LogAnalysis? {
        TODO("Task 6: route to platform classifier, return null on miss")
    }
}
