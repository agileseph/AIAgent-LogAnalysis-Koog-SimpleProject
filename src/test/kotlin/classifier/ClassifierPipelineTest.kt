package classifier

import kotlinx.coroutines.runBlocking
import model.ClassifierSource
import model.FailureCategory
import model.LogAnalysis
import model.Platform
import parser.ParsedLog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ClassifierPipelineTest {

    private val deterministicResult = LogAnalysis(
        platform = Platform.IOS,
        failureCategory = FailureCategory.ASSERTION_FAILURE,
        confidence = 1.0f,
        classifiedBy = ClassifierSource.DETERMINISTIC,
        evidence = listOf("XCTAssertEqual failed"),
        recommendedAction = "Check assertion",
        rawSummary = "Deterministic match"
    )

    private val llmResult = LogAnalysis(
        platform = Platform.IOS,
        failureCategory = FailureCategory.FLAKY_TEST,
        confidence = 0.7f,
        classifiedBy = ClassifierSource.LLM,
        evidence = listOf("Intermittent failure"),
        recommendedAction = "Re-run test",
        rawSummary = "LLM match"
    )

    private val log = ParsedLog(
        platform = Platform.IOS,
        relevantLines = listOf("some log line"),
        filePath = "test.log"
    )

    @Test
    fun `given deterministic match when classify then llm never called`() = runBlocking {
        val pipeline = ClassifierPipeline(
            deterministic = { deterministicResult },
            llm = { throw AssertionError("LLM must not be called when deterministic matches") }
        )

        // Should not throw
        pipeline.classify(log)
    }

    @Test
    fun `given deterministic miss when classify then llm called`() = runBlocking {
        var llmCalled = false
        val pipeline = ClassifierPipeline(
            deterministic = { null },
            llm = { llmCalled = true; llmResult }
        )

        pipeline.classify(log)

        assertEquals(true, llmCalled)
    }

    @Test
    fun `given deterministic match when classify then classifiedBy is DETERMINISTIC`() = runBlocking {
        val pipeline = ClassifierPipeline(
            deterministic = { deterministicResult },
            llm = { llmResult }
        )

        val result = pipeline.classify(log)

        assertEquals(ClassifierSource.DETERMINISTIC, result.classifiedBy)
    }

    @Test
    fun `given deterministic miss when classify then classifiedBy is LLM`() = runBlocking {
        val pipeline = ClassifierPipeline(
            deterministic = { null },
            llm = { llmResult }
        )

        val result = pipeline.classify(log)

        assertEquals(ClassifierSource.LLM, result.classifiedBy)
    }

    @Test
    fun `given deterministic match when classify then deterministic result is returned unchanged`() = runBlocking {
        val pipeline = ClassifierPipeline(
            deterministic = { deterministicResult },
            llm = { llmResult }
        )

        val result = pipeline.classify(log)

        assertEquals(deterministicResult, result)
    }

    @Test
    fun `given deterministic miss when classify then llm result is returned unchanged`() = runBlocking {
        val pipeline = ClassifierPipeline(
            deterministic = { null },
            llm = { llmResult }
        )

        val result = pipeline.classify(log)

        assertEquals(llmResult, result)
    }
}
