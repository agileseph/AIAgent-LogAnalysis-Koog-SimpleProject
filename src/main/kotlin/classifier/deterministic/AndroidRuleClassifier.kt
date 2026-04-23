package classifier.deterministic

import model.ClassifierSource
import model.FailureCategory
import model.FailureCategory.APP_CRASH
import model.FailureCategory.ASSERTION_FAILURE
import model.FailureCategory.ELEMENT_NOT_FOUND
import model.LogAnalysis
import model.Platform
import parser.ParsedLog

class AndroidRuleClassifier {

    fun classify(log: ParsedLog): LogAnalysis? {
        if (log.relevantLines.isEmpty()) return null
        return matchAppCrash(log.relevantLines)
            ?: matchAssertionFailure(log.relevantLines)
            ?: matchElementNotFound(log.relevantLines)
    }

    private fun matchAppCrash(lines: List<String>): LogAnalysis? {
        val evidence = lines.filter { it.containsAny(APP_CRASH_PATTERNS) }
        return evidence.toAnalysis(
            category = APP_CRASH,
            recommendedAction = "Check the stack trace for the root exception; look for unhandled exceptions or null pointer dereferences in the application code",
            rawSummary = "Android app crashed with a fatal exception during the test run"
        )
    }

    private fun matchAssertionFailure(lines: List<String>): LogAnalysis? {
        val evidence = lines.filter { it.containsAny(ASSERTION_PATTERNS) }
        return evidence.toAnalysis(
            category = ASSERTION_FAILURE,
            recommendedAction = "Check the assertion values and verify the expected test state matches the actual app state",
            rawSummary = "JUnit assertion failed — actual value did not match expected value"
        )
    }

    private fun matchElementNotFound(lines: List<String>): LogAnalysis? {
        val evidence = lines.filter { it.containsAny(ELEMENT_NOT_FOUND_PATTERNS) }
        return evidence.toAnalysis(
            category = ELEMENT_NOT_FOUND,
            recommendedAction = "Verify the view matcher and confirm the target view is present and visible in the hierarchy at the point of interaction",
            rawSummary = "Espresso could not find a matching view in the current activity's hierarchy"
        )
    }

    private fun List<String>.toAnalysis(
        category: FailureCategory,
        recommendedAction: String,
        rawSummary: String
    ): LogAnalysis? {
        if (isEmpty()) return null
        return LogAnalysis(
            platform = Platform.ANDROID,
            failureCategory = category,
            confidence = 1.0f,
            classifiedBy = ClassifierSource.DETERMINISTIC,
            evidence = this,
            recommendedAction = recommendedAction,
            rawSummary = rawSummary
        )
    }

    private fun String.containsAny(patterns: List<Regex>) =
        patterns.any { it.containsMatchIn(this) }

    companion object {
        private val APP_CRASH_PATTERNS = listOf(
            Regex("""FATAL EXCEPTION"""),
            Regex("""Process:.*PID:"""),
            Regex("""ANR in"""),
            Regex("""Application Not Responding""", RegexOption.IGNORE_CASE),
        )

        private val ASSERTION_PATTERNS = listOf(
            Regex("""AssertionFailedError"""),
            Regex("""expected:<"""),
            Regex("""but was:<"""),
            Regex("""ComparisonFailure"""),
            Regex("""junit\.framework\.Assert"""),
        )

        private val ELEMENT_NOT_FOUND_PATTERNS = listOf(
            Regex("""NoMatchingViewException"""),
            Regex("""No views in hierarchy""", RegexOption.IGNORE_CASE),
            Regex("""matches nothing in the view hierarchy""", RegexOption.IGNORE_CASE),
            Regex("""AmbiguousViewMatcherException"""),
        )
    }
}