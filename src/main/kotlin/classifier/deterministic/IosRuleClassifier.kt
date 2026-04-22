package classifier.deterministic

import model.LogAnalysis
import parser.ParsedLog

/**
 * Task 4: Deterministic rule classifier for iOS XCUITest logs.
 *
 * Rules (minimum 3 for v1):
 *   - ASSERTION_FAILURE : XCTAssertEqual / XCTAssert failure patterns
 *   - ELEMENT_NOT_FOUND : "Unable to find element" / accessibility identifier not found
 *   - TIMEOUT           : "Exceeded timeout" / "wait for element to exist" exceeded
 */
class IosRuleClassifier {

    fun classify(log: ParsedLog): LogAnalysis? {
        TODO("Task 4: implement iOS deterministic rules")
    }
}
