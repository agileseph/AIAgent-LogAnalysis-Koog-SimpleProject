package report

import kotlinx.serialization.json.Json
import model.ClassifierSource
import model.FailureCategory
import model.LogAnalysis
import model.Platform
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonReportWriterTest {

    private val writer = JsonReportWriter()

    private val analysis = LogAnalysis(
        platform = Platform.IOS,
        failureCategory = FailureCategory.ASSERTION_FAILURE,
        confidence = 0.95f,
        classifiedBy = ClassifierSource.DETERMINISTIC,
        evidence = listOf("XCTAssertEqual failed: (\"0\") is not equal to (\"1\")"),
        recommendedAction = "Check the assertion values",
        rawSummary = "XCTest assertion failed"
    )

    @Test
    fun `given log analysis when write then returns valid json`() {
        val result = writer.write(analysis)
        // Parseable without throwing
        Json.parseToJsonElement(result)
    }

    @Test
    fun `given log analysis when write then all fields present in output`() {
        val result = writer.write(analysis)
        assertTrue(result.contains("\"platform\""))
        assertTrue(result.contains("\"IOS\""))
        assertTrue(result.contains("\"failureCategory\""))
        assertTrue(result.contains("\"ASSERTION_FAILURE\""))
        assertTrue(result.contains("\"confidence\""))
        assertTrue(result.contains("\"classifiedBy\""))
        assertTrue(result.contains("\"DETERMINISTIC\""))
        assertTrue(result.contains("\"evidence\""))
        assertTrue(result.contains("\"recommendedAction\""))
        assertTrue(result.contains("\"rawSummary\""))
    }

    @Test
    fun `given log analysis when write then deserialises back to equal object`() {
        val json = writer.write(analysis)
        val deserialised = Json.decodeFromString<LogAnalysis>(json)
        assertEquals(analysis, deserialised)
    }

    @Test
    fun `given log analysis when writeToFile then file contains valid json`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("output/result.json").toString()

        writer.writeToFile(analysis, file)

        val content = Path.of(file).readText()
        val deserialised = Json.decodeFromString<LogAnalysis>(content)
        assertEquals(analysis, deserialised)
    }

    @Test
    fun `given log analysis when writeToFile then overwrites existing file`(@TempDir tempDir: Path) {
        val file = tempDir.resolve("result.json").toString()
        writer.writeToFile(analysis, file)

        val updated = analysis.copy(rawSummary = "Updated summary")
        writer.writeToFile(updated, file)

        val content = Path.of(file).readText()
        assertTrue(content.contains("Updated summary"))
    }
}
