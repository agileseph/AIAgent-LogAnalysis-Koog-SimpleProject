package classifier

import classifier.deterministic.AndroidRuleClassifier
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

class AndroidRuleClassifierFixtureTest {

    private val parser = LogParser()
    private val classifier = AndroidRuleClassifier()

    @Test
    fun `given android_element_not_found fixture when classify then returns ELEMENT_NOT_FOUND`(
        @TempDir tempDir: Path
    ) {
        val result = classifyFixture("android_element_not_found.log", tempDir)

        assertNotNull(result)
        assertEquals(FailureCategory.ELEMENT_NOT_FOUND, result.failureCategory)
        assertEquals(1.0f, result.confidence)
        assertEquals(ClassifierSource.DETERMINISTIC, result.classifiedBy)
        assertTrue(result.evidence.isNotEmpty())
    }

    @Test
    fun `given android_assertion_failure fixture when classify then returns ASSERTION_FAILURE`(
        @TempDir tempDir: Path
    ) {
        val result = classifyFixture("android_assertion_failure.log", tempDir)

        assertNotNull(result)
        assertEquals(FailureCategory.ASSERTION_FAILURE, result.failureCategory)
        assertEquals(1.0f, result.confidence)
        assertEquals(ClassifierSource.DETERMINISTIC, result.classifiedBy)
        assertTrue(result.evidence.isNotEmpty())
    }

    @Test
    fun `given android_app_crash fixture when classify then returns APP_CRASH`(
        @TempDir tempDir: Path
    ) {
        val result = classifyFixture("android_app_crash.log", tempDir)

        assertNotNull(result)
        assertEquals(FailureCategory.APP_CRASH, result.failureCategory)
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
