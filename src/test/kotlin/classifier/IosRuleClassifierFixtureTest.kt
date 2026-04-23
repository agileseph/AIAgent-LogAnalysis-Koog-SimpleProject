package classifier

import classifier.deterministic.IosRuleClassifier
import model.ClassifierSource
import model.FailureCategory
import org.junit.jupiter.api.io.TempDir
import parser.LogParser
import java.nio.file.Path
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IosRuleClassifierFixtureTest {

    private val parser = LogParser()
    private val classifier = IosRuleClassifier()

    @Test
    fun `given ios_assertion_failure fixture when classify then returns ASSERTION_FAILURE`(
        @TempDir tempDir: Path
    ) {
        val result = classifyFixture("ios_assertion_failure.log", tempDir)

        assertNotNull(result)
        assertEquals(FailureCategory.ASSERTION_FAILURE, result.failureCategory)
        assertEquals(1.0f, result.confidence)
        assertEquals(ClassifierSource.DETERMINISTIC, result.classifiedBy)
        assertTrue(result.evidence.isNotEmpty())
    }

    @Test
    fun `given ios_element_not_found fixture when classify then returns ELEMENT_NOT_FOUND`(
        @TempDir tempDir: Path
    ) {
        val result = classifyFixture("ios_element_not_found.log", tempDir)

        assertNotNull(result)
        assertEquals(FailureCategory.ELEMENT_NOT_FOUND, result.failureCategory)
        assertEquals(1.0f, result.confidence)
        assertEquals(ClassifierSource.DETERMINISTIC, result.classifiedBy)
        assertTrue(result.evidence.isNotEmpty())
    }

    @Test
    fun `given ios_timeout fixture when classify then returns TIMEOUT`(
        @TempDir tempDir: Path
    ) {
        val result = classifyFixture("ios_timeout.log", tempDir)

        assertNotNull(result)
        assertEquals(FailureCategory.TIMEOUT, result.failureCategory)
        assertEquals(1.0f, result.confidence)
        assertEquals(ClassifierSource.DETERMINISTIC, result.classifiedBy)
        assertTrue(result.evidence.isNotEmpty())
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun classifyFixture(fixtureName: String, tempDir: Path) =
        javaClass.classLoader.getResourceAsStream("fixtures/$fixtureName")!!
            .bufferedReader()
            .use { it.readText() }
            .let { content ->
                val file = tempDir.resolve(fixtureName).also { it.writeText(content) }
                parser.parse(file.toString())
            }
            .let { classifier.classify(it) }
}
