package classifier

import classifier.deterministic.IosRuleClassifier
import model.ClassifierSource
import model.FailureCategory
import model.Platform
import parser.ParsedLog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IosRuleClassifierTest {

    private val classifier = IosRuleClassifier()

    // ── ASSERTION_FAILURE ─────────────────────────────────────────────────────

    @Test
    fun `given xctest assertion failure when classify then returns assertion category`() {
        val log = parsedLog(
            """/Users/dev/MyApp/LoginUITests.swift:42: error: XCTAssertEqual failed: ("Welcome, User!") is not equal to ("Welcome, Admin!")"""
        )
        val result = classifier.classify(log)
        assertNotNull(result)
        assertEquals(FailureCategory.ASSERTION_FAILURE, result.failureCategory)
        assertEquals(1.0f, result.confidence)
        assertEquals(ClassifierSource.DETERMINISTIC, result.classifiedBy)
        assertTrue(result.evidence.isNotEmpty())
    }

    @Test
    fun `given XCTAssertTrue failed line when classify then returns assertion category`() {
        val log = parsedLog("XCTAssertTrue failed — expected true but was false")
        val result = classifier.classify(log)
        assertNotNull(result)
        assertEquals(FailureCategory.ASSERTION_FAILURE, result.failureCategory)
    }

    @Test
    fun `given XCTAssertFalse failed line when classify then returns assertion category`() {
        val log = parsedLog("XCTAssertFalse failed")
        val result = classifier.classify(log)
        assertNotNull(result)
        assertEquals(FailureCategory.ASSERTION_FAILURE, result.failureCategory)
    }

    @Test
    fun `given XCTAssertNil failed line when classify then returns assertion category`() {
        val log = parsedLog("XCTAssertNil failed: got non-nil value")
        val result = classifier.classify(log)
        assertNotNull(result)
        assertEquals(FailureCategory.ASSERTION_FAILURE, result.failureCategory)
    }

    // ── ELEMENT_NOT_FOUND ─────────────────────────────────────────────────────

    @Test
    fun `given element not found signal when classify then returns element not found category`() {
        val log = parsedLog(
            """Failed to get matching snapshots: No matches found for criteria: identifierOrDescendantMatchingIdentifier("loginButton")"""
        )
        val result = classifier.classify(log)
        assertNotNull(result)
        assertEquals(FailureCategory.ELEMENT_NOT_FOUND, result.failureCategory)
        assertEquals(1.0f, result.confidence)
        assertEquals(ClassifierSource.DETERMINISTIC, result.classifiedBy)
        assertTrue(result.evidence.isNotEmpty())
    }

    @Test
    fun `given Unable to find signal when classify then returns element not found category`() {
        val log = parsedLog("Unable to find element with accessibility identifier: settingsButton")
        val result = classifier.classify(log)
        assertNotNull(result)
        assertEquals(FailureCategory.ELEMENT_NOT_FOUND, result.failureCategory)
    }

    @Test
    fun `given Failed to find accessibility signal when classify then returns element not found category`() {
        val log = parsedLog("Failed to find accessibility element for label 'Submit'")
        val result = classifier.classify(log)
        assertNotNull(result)
        assertEquals(FailureCategory.ELEMENT_NOT_FOUND, result.failureCategory)
    }

    // ── TIMEOUT ───────────────────────────────────────────────────────────────

    @Test
    fun `given timeout signal when classify then returns timeout category`() {
        val log = parsedLog(
            """Exceeded timeout of 30 seconds waiting for element to exist: "confirmationLabel""""
        )
        val result = classifier.classify(log)
        assertNotNull(result)
        assertEquals(FailureCategory.TIMEOUT, result.failureCategory)
        assertEquals(1.0f, result.confidence)
        assertEquals(ClassifierSource.DETERMINISTIC, result.classifiedBy)
        assertTrue(result.evidence.isNotEmpty())
    }

    @Test
    fun `given timed out signal when classify then returns timeout category`() {
        val log = parsedLog("UI test runner timed out waiting for app to become idle")
        val result = classifier.classify(log)
        assertNotNull(result)
        assertEquals(FailureCategory.TIMEOUT, result.failureCategory)
    }

    @Test
    fun `given waitForExistence signal when classify then returns timeout category`() {
        val log = parsedLog("""waitForExistence(timeout: 5.0) returned false""")
        val result = classifier.classify(log)
        assertNotNull(result)
        assertEquals(FailureCategory.TIMEOUT, result.failureCategory)
    }

    // ── Rule priority ─────────────────────────────────────────────────────────

    @Test
    fun `given assertion and timeout signals when classify then assertion wins`() {
        val log = parsedLog(
            """XCTAssertEqual failed: ("0") is not equal to ("1")""",
            """Exceeded timeout of 10 seconds"""
        )
        val result = classifier.classify(log)
        assertNotNull(result)
        assertEquals(FailureCategory.ASSERTION_FAILURE, result.failureCategory)
    }

    // ── Negative tests ────────────────────────────────────────────────────────

    @Test
    fun `given android log when classify then returns null`() {
        val log = parsedLog(
            "androidx.test.espresso.NoMatchingViewException: No views in hierarchy found matching",
            "INSTRUMENTATION_STATUS: class=com.example.app.LoginTest",
            "Process: com.example.app, PID: 1234",
            "FATAL EXCEPTION: main"
        )
        assertNull(classifier.classify(log))
    }

    @Test
    fun `given unrecognised ios log when classify then returns null`() {
        val log = parsedLog(
            "Test Suite 'LoginUITests' started at 2024-03-15 10:23:45.123",
            "Test Case '-[LoginUITests testLogin]' started.",
            "Test Case '-[LoginUITests testLogin]' passed (1.234 seconds)."
        )
        assertNull(classifier.classify(log))
    }

    @Test
    fun `given empty relevant lines when classify then returns null`() {
        val log = ParsedLog(platform = model.Platform.IOS, relevantLines = emptyList(), filePath = "test")
        assertNull(classifier.classify(log))
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun parsedLog(vararg lines: String) = ParsedLog(
        platform = Platform.IOS,
        relevantLines = lines.toList(),
        filePath = "test"
    )
}