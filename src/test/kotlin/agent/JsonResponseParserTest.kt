package agent

import model.ClassifierSource
import model.FailureCategory
import model.Platform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonResponseParserTest {

    private val parser = JsonResponseParser()

    // ── Valid JSON ─────────────────────────────────────────────────────────────

    @Test
    fun `given valid json response when parse then returns correct log analysis`() {
        val raw = """
            {
              "failureCategory": "ASSERTION_FAILURE",
              "confidence": 0.9,
              "evidence": ["XCTAssertEqual failed"],
              "recommendedAction": "Check assertion values",
              "rawSummary": "Assertion failed in LoginTests"
            }
        """.trimIndent()

        val result = parser.parse(raw, Platform.IOS)

        assertEquals(Platform.IOS, result.platform)
        assertEquals(FailureCategory.ASSERTION_FAILURE, result.failureCategory)
        assertEquals(0.9f, result.confidence)
        assertEquals(ClassifierSource.LLM, result.classifiedBy)
        assertEquals(listOf("XCTAssertEqual failed"), result.evidence)
        assertEquals("Check assertion values", result.recommendedAction)
        assertEquals("Assertion failed in LoginTests", result.rawSummary)
    }

    // ── Markdown fence stripping ───────────────────────────────────────────────

    @Test
    fun `given json with markdown fences when parse then strips and parses`() {
        val raw = """
            ```json
            {
              "failureCategory": "TIMEOUT",
              "confidence": 0.8,
              "evidence": ["Exceeded timeout of 30 seconds"],
              "recommendedAction": "Increase timeout",
              "rawSummary": "Test timed out"
            }
            ```
        """.trimIndent()

        val result = parser.parse(raw, Platform.IOS)

        assertEquals(FailureCategory.TIMEOUT, result.failureCategory)
        assertEquals(0.8f, result.confidence)
        assertEquals(ClassifierSource.LLM, result.classifiedBy)
    }

    @Test
    fun `given json with plain code fences when parse then strips and parses`() {
        val raw = """
            ```
            {
              "failureCategory": "APP_CRASH",
              "confidence": 1.0,
              "evidence": ["FATAL EXCEPTION: main"],
              "recommendedAction": "Check crash log",
              "rawSummary": "App crashed during test"
            }
            ```
        """.trimIndent()

        val result = parser.parse(raw, Platform.ANDROID)

        assertEquals(FailureCategory.APP_CRASH, result.failureCategory)
        assertEquals(Platform.ANDROID, result.platform)
    }

    // ── Trailing comma correction ──────────────────────────────────────────────

    @Test
    fun `given json with trailing comma when parse then fixes and parses`() {
        val raw = """
            {
              "failureCategory": "ELEMENT_NOT_FOUND",
              "confidence": 0.75,
              "evidence": ["No matching view found"],
              "recommendedAction": "Check view hierarchy",
              "rawSummary": "Element not found in hierarchy",
            }
        """.trimIndent()

        val result = parser.parse(raw, Platform.ANDROID)

        assertEquals(FailureCategory.ELEMENT_NOT_FOUND, result.failureCategory)
        assertEquals(0.75f, result.confidence)
    }

    @Test
    fun `given json with trailing comma in array when parse then fixes and parses`() {
        val raw = """
            {
              "failureCategory": "ASSERTION_FAILURE",
              "confidence": 0.9,
              "evidence": ["line 1", "line 2",],
              "recommendedAction": "Fix assertion",
              "rawSummary": "Assertion failed"
            }
        """.trimIndent()

        val result = parser.parse(raw, Platform.IOS)

        assertEquals(FailureCategory.ASSERTION_FAILURE, result.failureCategory)
        assertEquals(2, result.evidence.size)
    }

    // ── Fallback on invalid input ──────────────────────────────────────────────

    @Test
    fun `given completely invalid response when parse then returns unknown fallback`() {
        val raw = "Sorry, I could not analyse this log."

        val result = parser.parse(raw, Platform.IOS)

        assertEquals(Platform.IOS, result.platform)
        assertEquals(FailureCategory.UNKNOWN, result.failureCategory)
        assertEquals(0.0f, result.confidence)
        assertEquals(ClassifierSource.LLM, result.classifiedBy)
        assertTrue(result.evidence.isEmpty())
        assertEquals("Inspect raw LLM response in rawSummary field", result.recommendedAction)
    }

    @Test
    fun `given fallback when called then rawSummary contains original response`() {
        val raw = "This is my raw LLM response that could not be parsed."

        val result = parser.fallback(raw, Platform.ANDROID)

        assertEquals(raw, result.rawSummary)
        assertEquals(Platform.ANDROID, result.platform)
        assertEquals(FailureCategory.UNKNOWN, result.failureCategory)
        assertEquals(ClassifierSource.LLM, result.classifiedBy)
    }

    @Test
    fun `given fallback when rawResponse exceeds 500 chars then rawSummary is truncated`() {
        val raw = "x".repeat(1000)

        val result = parser.fallback(raw, Platform.IOS)

        assertEquals(500, result.rawSummary.length)
    }

    // ── Confidence clamping ────────────────────────────────────────────────────

    @Test
    fun `given out of range confidence when parse then clamps to valid range`() {
        val rawAbove = """
            {
              "failureCategory": "TIMEOUT",
              "confidence": 1.5,
              "evidence": ["timed out"],
              "recommendedAction": "Increase timeout",
              "rawSummary": "Test timed out"
            }
        """.trimIndent()

        val rawBelow = """
            {
              "failureCategory": "TIMEOUT",
              "confidence": -0.3,
              "evidence": ["timed out"],
              "recommendedAction": "Increase timeout",
              "rawSummary": "Test timed out"
            }
        """.trimIndent()

        assertEquals(1.0f, parser.parse(rawAbove, Platform.IOS).confidence)
        assertEquals(0.0f, parser.parse(rawBelow, Platform.IOS).confidence)
    }

    // ── Evidence fallback ──────────────────────────────────────────────────────

    @Test
    fun `given non-unknown category with empty evidence when parse then uses rawSummary as evidence`() {
        val raw = """
            {
              "failureCategory": "NETWORK_ERROR",
              "confidence": 0.7,
              "evidence": [],
              "recommendedAction": "Check network",
              "rawSummary": "Network connection failed"
            }
        """.trimIndent()

        val result = parser.parse(raw, Platform.ANDROID)

        assertEquals(listOf("Network connection failed"), result.evidence)
    }

    @Test
    fun `given unknown category with empty evidence when parse then evidence stays empty`() {
        val raw = """
            {
              "failureCategory": "UNKNOWN",
              "confidence": 0.1,
              "evidence": [],
              "recommendedAction": "Manual investigation needed",
              "rawSummary": "Could not classify"
            }
        """.trimIndent()

        val result = parser.parse(raw, Platform.IOS)

        assertEquals(FailureCategory.UNKNOWN, result.failureCategory)
        assertTrue(result.evidence.isEmpty())
    }
}
