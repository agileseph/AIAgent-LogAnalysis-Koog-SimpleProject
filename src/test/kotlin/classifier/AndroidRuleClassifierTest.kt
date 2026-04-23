package classifier

import classifier.deterministic.AndroidRuleClassifier
import model.ClassifierSource
import model.FailureCategory
import model.Platform
import parser.ParsedLog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AndroidRuleClassifierTest {

    private val classifier = AndroidRuleClassifier()

    // ── APP_CRASH ─────────────────────────────────────────────────────────────

    @Test
    fun `given fatal exception when classify then returns app crash category`() {
        val log = parsedLog(
            "03-15 10:23:49.512 E AndroidRuntime: FATAL EXCEPTION: main",
            "Process: com.example.app, PID: 1234",
            "java.lang.NullPointerException: Attempt to invoke virtual method on a null object reference"
        )
        val result = classifier.classify(log)
        assertNotNull(result)
        assertEquals(FailureCategory.APP_CRASH, result.failureCategory)
        assertEquals(1.0f, result.confidence)
        assertEquals(ClassifierSource.DETERMINISTIC, result.classifiedBy)
        assertTrue(result.evidence.isNotEmpty())
    }

    @Test
    fun `given ANR signal when classify then returns app crash category`() {
        val log = parsedLog("ANR in com.example.app (com.example.app/.MainActivity)")
        val result = classifier.classify(log)
        assertNotNull(result)
        assertEquals(FailureCategory.APP_CRASH, result.failureCategory)
    }

    // ── ASSERTION_FAILURE ─────────────────────────────────────────────────────

    @Test
    fun `given assertion failed error when classify then returns assertion category`() {
        val log = parsedLog(
            "junit.framework.AssertionFailedError: expected:<Welcome, Admin!> but was:<Welcome, User!>",
            "at com.example.app.LoginTest.testLoginWithValidCredentials(LoginTest.java:42)"
        )
        val result = classifier.classify(log)
        assertNotNull(result)
        assertEquals(FailureCategory.ASSERTION_FAILURE, result.failureCategory)
        assertEquals(1.0f, result.confidence)
        assertEquals(ClassifierSource.DETERMINISTIC, result.classifiedBy)
        assertTrue(result.evidence.isNotEmpty())
    }

    @Test
    fun `given ComparisonFailure signal when classify then returns assertion category`() {
        val log = parsedLog("org.junit.ComparisonFailure: expected:<[foo]> but was:<[bar]>")
        val result = classifier.classify(log)
        assertNotNull(result)
        assertEquals(FailureCategory.ASSERTION_FAILURE, result.failureCategory)
    }

    @Test
    fun `given junit framework Assert signal when classify then returns assertion category`() {
        val log = parsedLog("junit.framework.Assert.fail(Assert.java:55)")
        val result = classifier.classify(log)
        assertNotNull(result)
        assertEquals(FailureCategory.ASSERTION_FAILURE, result.failureCategory)
    }

    // ── ELEMENT_NOT_FOUND ─────────────────────────────────────────────────────

    @Test
    fun `given no matching view exception when classify then returns element not found category`() {
        val log = parsedLog(
            "androidx.test.espresso.NoMatchingViewException: No views in hierarchy found matching: with id: com.example.app:id/loginButton",
            "View Hierarchy:",
            "  +-- LinearLayout"
        )
        val result = classifier.classify(log)
        assertNotNull(result)
        assertEquals(FailureCategory.ELEMENT_NOT_FOUND, result.failureCategory)
        assertEquals(1.0f, result.confidence)
        assertEquals(ClassifierSource.DETERMINISTIC, result.classifiedBy)
        assertTrue(result.evidence.isNotEmpty())
    }

    @Test
    fun `given matches nothing in hierarchy signal when classify then returns element not found category`() {
        val log = parsedLog("'with id: com.example.app:id/submitBtn' matches nothing in the view hierarchy")
        val result = classifier.classify(log)
        assertNotNull(result)
        assertEquals(FailureCategory.ELEMENT_NOT_FOUND, result.failureCategory)
    }

    @Test
    fun `given AmbiguousViewMatcherException when classify then returns element not found category`() {
        val log = parsedLog("androidx.test.espresso.AmbiguousViewMatcherException: 'with text: Submit' matches multiple views")
        val result = classifier.classify(log)
        assertNotNull(result)
        assertEquals(FailureCategory.ELEMENT_NOT_FOUND, result.failureCategory)
    }

    // ── Rule priority ─────────────────────────────────────────────────────────

    @Test
    fun `given crash and assertion signals when classify then crash wins`() {
        val log = parsedLog(
            "FATAL EXCEPTION: main",
            "junit.framework.AssertionFailedError: expected:<1> but was:<0>"
        )
        val result = classifier.classify(log)
        assertNotNull(result)
        assertEquals(FailureCategory.APP_CRASH, result.failureCategory)
    }

    // ── Negative tests ────────────────────────────────────────────────────────

    @Test
    fun `given ios log when classify then returns null`() {
        val log = parsedLog(
            """/Users/dev/MyApp/LoginUITests.swift:42: error: XCTAssertEqual failed: ("foo") is not equal to ("bar")""",
            "Test Case '-[LoginUITests testLogin]' failed (1.234 seconds)."
        )
        assertNull(classifier.classify(log))
    }

    @Test
    fun `given unrecognised android log when classify then returns null`() {
        val log = parsedLog(
            "INSTRUMENTATION_STATUS: class=com.example.app.LoginTest",
            "INSTRUMENTATION_STATUS: current=1",
            "INSTRUMENTATION_STATUS_CODE: 1",
            "INSTRUMENTATION_STATUS_CODE: 0"
        )
        assertNull(classifier.classify(log))
    }

    @Test
    fun `given empty relevant lines when classify then returns null`() {
        val log = ParsedLog(platform = Platform.ANDROID, relevantLines = emptyList(), filePath = "test")
        assertNull(classifier.classify(log))
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun parsedLog(vararg lines: String) = ParsedLog(
        platform = Platform.ANDROID,
        relevantLines = lines.toList(),
        filePath = "test"
    )
}