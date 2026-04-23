package classifier

import agent.JsonResponseParser
import classifier.deterministic.DeterministicClassifier
import kotlinx.coroutines.runBlocking
import model.ClassifierSource
import model.FailureCategory
import model.Platform
import parser.ParsedLog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration tests for ClassifierPipeline.
 * Ollama is NOT required — the LLM call is replaced by a hardcoded JSON lambda.
 * Real DeterministicClassifier and JsonResponseParser run in every test.
 */
class ClassifierPipelineIntegrationTest {

    // ── Fixture logs ─────────────────────────────────────────────────────────

    private val iosKnownLog = ParsedLog(
        platform = Platform.IOS,
        relevantLines = listOf(
            """/Users/dev/MyApp/LoginUITests.swift:42: error: -[LoginUITests testLogin] : XCTAssertEqual failed: ("0") is not equal to ("1")"""
        ),
        filePath = "test.log"
    )

    private val iosUnknownLog = ParsedLog(
        platform = Platform.IOS,
        relevantLines = listOf(
            "Test Case '-[FeatureTests testSomething]' started.",
            "Test Case '-[FeatureTests testSomething]' failed (0.123 seconds)."
        ),
        filePath = "test.log"
    )

    private val androidUnknownLog = ParsedLog(
        platform = Platform.ANDROID,
        relevantLines = listOf(
            "03-15 10:00:00.000  1234  5678 E TestRunner: failed: testFeature(com.example.FeatureTest)",
            "03-15 10:00:00.001  1234  5678 I TestRunner: finished: testFeature(com.example.FeatureTest)"
        ),
        filePath = "test.log"
    )

    // ── Valid LLM JSON (FLAKY_TEST — no deterministic rule covers this) ──────

    private val validLlmJson = """
        {
          "failureCategory": "FLAKY_TEST",
          "confidence": 0.75,
          "evidence": ["Test passed on re-run — likely a timing issue"],
          "recommendedAction": "Re-run the test suite and add retry logic",
          "rawSummary": "Intermittent failure with no consistent root cause"
        }
    """.trimIndent()

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    fun `given known ios pattern when pipeline classify then no llm call`() = runBlocking {
        val pipeline = ClassifierPipeline(
            deterministic = DeterministicClassifier()::classify,
            llm = { throw AssertionError("LLM must not be called when deterministic matches") }
        )

        // Should not throw
        pipeline.classify(iosKnownLog)
    }

    @Test
    fun `given unknown ios pattern when pipeline classify then llm called`() = runBlocking {
        val parser = JsonResponseParser()
        var llmCalled = false
        val pipeline = ClassifierPipeline(
            deterministic = DeterministicClassifier()::classify,
            llm = { log ->
                llmCalled = true
                parser.parse(validLlmJson, log.platform)
            }
        )

        val result = pipeline.classify(iosUnknownLog)

        assertTrue(llmCalled)
        assertEquals(ClassifierSource.LLM, result.classifiedBy)
    }

    @Test
    fun `given unknown android pattern when pipeline classify then llm called`() = runBlocking {
        val parser = JsonResponseParser()
        var llmCalled = false
        val pipeline = ClassifierPipeline(
            deterministic = DeterministicClassifier()::classify,
            llm = { log ->
                llmCalled = true
                parser.parse(validLlmJson, log.platform)
            }
        )

        val result = pipeline.classify(androidUnknownLog)

        assertTrue(llmCalled)
        assertEquals(ClassifierSource.LLM, result.classifiedBy)
        assertEquals(Platform.ANDROID, result.platform)
    }

    @Test
    fun `given llm returns malformed json when pipeline classify then returns unknown fallback`() = runBlocking {
        val parser = JsonResponseParser()
        val pipeline = ClassifierPipeline(
            deterministic = DeterministicClassifier()::classify,
            llm = { log -> parser.parse("Sorry, I cannot analyse this.", log.platform) }
        )

        val result = pipeline.classify(iosUnknownLog)

        assertEquals(FailureCategory.UNKNOWN, result.failureCategory)
        assertEquals(ClassifierSource.LLM, result.classifiedBy)
        assertEquals(0.0f, result.confidence)
        assertTrue(result.evidence.isEmpty())
    }

    @Test
    fun `given llm returns valid json when pipeline classify then all fields populated`() = runBlocking {
        val parser = JsonResponseParser()
        val pipeline = ClassifierPipeline(
            deterministic = DeterministicClassifier()::classify,
            llm = { log -> parser.parse(validLlmJson, log.platform) }
        )

        val result = pipeline.classify(iosUnknownLog)

        assertEquals(ClassifierSource.LLM, result.classifiedBy)
        assertFalse(result.evidence.isEmpty(), "evidence should be non-empty")
        assertTrue(result.recommendedAction.isNotBlank(), "recommendedAction should be non-empty")
        assertTrue(result.rawSummary.isNotBlank(), "rawSummary should be non-empty")
        assertEquals(FailureCategory.FLAKY_TEST, result.failureCategory)
        assertEquals(0.75f, result.confidence)
    }

    @Test
    fun `given known ios pattern when pipeline classify then classifiedBy is DETERMINISTIC`() = runBlocking {
        val pipeline = ClassifierPipeline(
            deterministic = DeterministicClassifier()::classify,
            llm = { log -> JsonResponseParser().parse(validLlmJson, log.platform) }
        )

        val result = pipeline.classify(iosKnownLog)

        assertEquals(ClassifierSource.DETERMINISTIC, result.classifiedBy)
        assertEquals(FailureCategory.ASSERTION_FAILURE, result.failureCategory)
    }
}
