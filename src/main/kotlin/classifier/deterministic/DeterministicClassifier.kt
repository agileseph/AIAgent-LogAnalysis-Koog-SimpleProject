package classifier.deterministic

import model.LogAnalysis
import model.Platform
import parser.ParsedLog

/**
 * Orchestrates deterministic rule classifiers.
 *
 * Returns null when no rule matches — signals LLM fallback required.
 * Never calls any external service.
 */
class DeterministicClassifier(
    private val iosClassifier: (ParsedLog) -> LogAnalysis? = IosRuleClassifier()::classify,
    private val androidClassifier: (ParsedLog) -> LogAnalysis? = AndroidRuleClassifier()::classify
) {

    fun classify(log: ParsedLog): LogAnalysis? = when (log.platform) {
        Platform.IOS -> iosClassifier(log)
        Platform.ANDROID -> androidClassifier(log)
    }
}