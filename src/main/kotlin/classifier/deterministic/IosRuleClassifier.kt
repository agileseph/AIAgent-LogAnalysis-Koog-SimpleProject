package classifier.deterministic

import model.ClassifierSource
import model.FailureCategory
import model.FailureCategory.ASSERTION_FAILURE
import model.FailureCategory.ELEMENT_NOT_FOUND
import model.FailureCategory.TIMEOUT
import model.LogAnalysis
import model.Platform
import parser.ParsedLog

class IosRuleClassifier {

    fun classify(log: ParsedLog): LogAnalysis? {
        if (log.relevantLines.isEmpty()) return null
        return matchAssertionFailure(log.relevantLines)
            ?: matchElementNotFound(log.relevantLines)
            ?: matchTimeout(log.relevantLines)
    }

    private fun matchAssertionFailure(lines: List<String>): LogAnalysis? {
        val evidence = lines.filter { it.containsAny(ASSERTION_PATTERNS) }
        return evidence.toAnalysis(
            category = ASSERTION_FAILURE,
            recommendedAction = "Check the assertion values and verify the expected test state matches actual app state",
            rawSummary = "XCTest assertion failed — actual value did not match expected value"
        )
    }

    private fun matchElementNotFound(lines: List<String>): LogAnalysis? {
        val evidence = lines.filter { it.containsAny(ELEMENT_NOT_FOUND_PATTERNS) }
        return evidence.toAnalysis(
            category = ELEMENT_NOT_FOUND,
            recommendedAction = "Verify accessibility identifiers in the app and confirm the UI hierarchy is in the expected state before querying",
            rawSummary = "XCUITest could not locate the target element in the view hierarchy"
        )
    }

    private fun matchTimeout(lines: List<String>): LogAnalysis? {
        val evidence = lines.filter { it.containsAny(TIMEOUT_PATTERNS) }
        return evidence.toAnalysis(
            category = TIMEOUT,
            recommendedAction = "Check for slow animations or network delays; consider increasing the waitForExistence timeout or adding an explicit wait",
            rawSummary = "XCUITest timed out waiting for a condition or element to appear"
        )
    }

    private fun List<String>.toAnalysis(
        category: FailureCategory,
        recommendedAction: String,
        rawSummary: String
    ): LogAnalysis? {
        if (isEmpty()) return null
        return LogAnalysis(
            platform = Platform.IOS,
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
        private val ASSERTION_PATTERNS = listOf(
            Regex("""XCTAssertEqual\b"""),
            Regex("""XCTAssertTrue\b"""),
            Regex("""XCTAssertFalse\b"""),
            Regex("""XCTAssertNil\b"""),
            Regex("""XCTAssert\b.*failed""", RegexOption.IGNORE_CASE),
            Regex("""assert.*failed""", RegexOption.IGNORE_CASE),
        )

        private val ELEMENT_NOT_FOUND_PATTERNS = listOf(
            Regex("""Unable to find""", RegexOption.IGNORE_CASE),
            Regex("""No matches found""", RegexOption.IGNORE_CASE),
            Regex("""Failed to find accessibility""", RegexOption.IGNORE_CASE),
            Regex("""element not found""", RegexOption.IGNORE_CASE),
            Regex("""Failed to get matching snapshots""", RegexOption.IGNORE_CASE),
        )

        private val TIMEOUT_PATTERNS = listOf(
            Regex("""Exceeded timeout""", RegexOption.IGNORE_CASE),
            Regex("""timed? out""", RegexOption.IGNORE_CASE),
            Regex("""kCFRunLoopDefaultMode"""),
            Regex("""waitForExistence"""),
            Regex("""Timeout waiting""", RegexOption.IGNORE_CASE),
        )
    }
}