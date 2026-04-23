package classifier

import classifier.deterministic.DeterministicClassifier
import model.ClassifierSource
import model.FailureCategory
import model.LogAnalysis
import model.Platform
import parser.ParsedLog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class DeterministicClassifierTest {

    private val iosResult = LogAnalysis(
        platform = Platform.IOS,
        failureCategory = FailureCategory.ASSERTION_FAILURE,
        confidence = 1.0f,
        classifiedBy = ClassifierSource.DETERMINISTIC,
        evidence = listOf("XCTAssertEqual failed"),
        recommendedAction = "Check assertion",
        rawSummary = "iOS assertion failed"
    )

    private val androidResult = LogAnalysis(
        platform = Platform.ANDROID,
        failureCategory = FailureCategory.APP_CRASH,
        confidence = 1.0f,
        classifiedBy = ClassifierSource.DETERMINISTIC,
        evidence = listOf("FATAL EXCEPTION: main"),
        recommendedAction = "Check crash",
        rawSummary = "Android app crashed"
    )

    @Test
    fun `given ios log with known pattern when classify then returns result`() {
        val classifier = DeterministicClassifier(
            iosClassifier = { iosResult },
            androidClassifier = { null }
        )
        val result = classifier.classify(iosLog())
        assertNotNull(result)
        assertEquals(Platform.IOS, result.platform)
        assertEquals(FailureCategory.ASSERTION_FAILURE, result.failureCategory)
    }

    @Test
    fun `given android log with known pattern when classify then returns result`() {
        val classifier = DeterministicClassifier(
            iosClassifier = { null },
            androidClassifier = { androidResult }
        )
        val result = classifier.classify(androidLog())
        assertNotNull(result)
        assertEquals(Platform.ANDROID, result.platform)
        assertEquals(FailureCategory.APP_CRASH, result.failureCategory)
    }

    @Test
    fun `given ios log with unknown pattern when classify then returns null`() {
        val classifier = DeterministicClassifier(
            iosClassifier = { null },
            androidClassifier = { androidResult }
        )
        assertNull(classifier.classify(iosLog()))
    }

    @Test
    fun `given android log with unknown pattern when classify then returns null`() {
        val classifier = DeterministicClassifier(
            iosClassifier = { iosResult },
            androidClassifier = { null }
        )
        assertNull(classifier.classify(androidLog()))
    }

    @Test
    fun `given ios log when classify then android classifier is not called`() {
        var androidCalled = false
        val classifier = DeterministicClassifier(
            iosClassifier = { iosResult },
            androidClassifier = { androidCalled = true; androidResult }
        )
        classifier.classify(iosLog())
        assertEquals(false, androidCalled)
    }

    @Test
    fun `given android log when classify then ios classifier is not called`() {
        var iosCalled = false
        val classifier = DeterministicClassifier(
            iosClassifier = { iosCalled = true; iosResult },
            androidClassifier = { null }
        )
        classifier.classify(androidLog())
        assertEquals(false, iosCalled)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun iosLog() = ParsedLog(
        platform = Platform.IOS,
        relevantLines = listOf("XCTAssertEqual failed"),
        filePath = "test.log"
    )

    private fun androidLog() = ParsedLog(
        platform = Platform.ANDROID,
        relevantLines = listOf("FATAL EXCEPTION: main"),
        filePath = "test.log"
    )
}