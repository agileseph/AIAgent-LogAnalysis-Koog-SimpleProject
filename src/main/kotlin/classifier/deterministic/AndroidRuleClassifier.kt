package classifier.deterministic

import model.LogAnalysis
import parser.ParsedLog

/**
 * Task 5: Deterministic rule classifier for Android Espresso/Logcat logs.
 *
 * Rules (minimum 3 for v1):
 *   - ASSERTION_FAILURE : "AssertionFailedError" / "expected:<> but was:<>"
 *   - ELEMENT_NOT_FOUND : "NoMatchingViewException" / "matches nothing in the view hierarchy"
 *   - APP_CRASH         : "FATAL EXCEPTION" / "Process: ... PID:" in logcat
 */
class AndroidRuleClassifier {

    fun classify(log: ParsedLog): LogAnalysis? {
        TODO("Task 5: implement Android deterministic rules")
    }
}
