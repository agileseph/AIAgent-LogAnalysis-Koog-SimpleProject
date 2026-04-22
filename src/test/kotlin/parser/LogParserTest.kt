package parser

import model.Platform
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LogParserTest {

    private val parser = LogParser()

    // ── Task 2: iOS detection ─────────────────────────────────────────────────

    @Test
    fun `given XCUITest log when detectPlatform then returns IOS`() {
        val content = readFixture("ios_assertion_failure.log")
        assertEquals(Platform.IOS, parser.detectPlatform(content))
    }

    @Test
    fun `given XCTAssert signal when detectPlatform then returns IOS`() {
        val content = "XCTAssertEqual failed: (\"foo\") is not equal to (\"bar\")"
        assertEquals(Platform.IOS, parser.detectPlatform(content))
    }

    @Test
    fun `given ObjC test method signature when detectPlatform then returns IOS`() {
        val content = "Test Case '-[LoginUITests testLogin]' started."
        assertEquals(Platform.IOS, parser.detectPlatform(content))
    }

    @Test
    fun `given xcresult signal when detectPlatform then returns IOS`() {
        val content = "Build succeeded. xcresult written to /DerivedData/MyApp.xcresult"
        assertEquals(Platform.IOS, parser.detectPlatform(content))
    }

    // ── Task 2: Android detection ─────────────────────────────────────────────

    @Test
    fun `given Espresso log when detectPlatform then returns ANDROID`() {
        val content = readFixture("android_element_not_found.log")
        assertEquals(Platform.ANDROID, parser.detectPlatform(content))
    }

    @Test
    fun `given AndroidJUnitRunner signal when detectPlatform then returns ANDROID`() {
        val content = "INSTRUMENTATION_STATUS: class=com.example.app.LoginTest\nAndroidJUnitRunner"
        assertEquals(Platform.ANDROID, parser.detectPlatform(content))
    }

    @Test
    fun `given FATAL EXCEPTION signal when detectPlatform then returns ANDROID`() {
        val content = "03-15 10:23:49 E AndroidRuntime: FATAL EXCEPTION: main\nProcess: com.example.app"
        assertEquals(Platform.ANDROID, parser.detectPlatform(content))
    }

    @Test
    fun `given NoMatchingViewException signal when detectPlatform then returns ANDROID`() {
        val content = "androidx.test.espresso.NoMatchingViewException: No views in hierarchy found"
        assertEquals(Platform.ANDROID, parser.detectPlatform(content))
    }

    // ── Task 2: unknown / negative ────────────────────────────────────────────

    @Test
    fun `given unrecognised log content when detectPlatform then throws UnknownPlatformException`() {
        assertThrows<UnknownPlatformException> {
            parser.detectPlatform("Something went wrong at line 42.")
        }
    }

    @Test
    fun `given empty string when detectPlatform then throws UnknownPlatformException`() {
        assertThrows<UnknownPlatformException> {
            parser.detectPlatform("")
        }
    }

    @Test
    fun `given mixed signals with higher iOS score when detectPlatform then returns IOS`() {
        val content = """
            XCUITest suite started
            XCTAssertEqual failed
            androidx.test present in dependency list
        """.trimIndent()
        assertEquals(Platform.IOS, parser.detectPlatform(content))
    }

    // ── Task 3: parse() file I/O ──────────────────────────────────────────────

    @Test
    fun `given iOS fixture file when parse then returns IOS platform`(@TempDir tempDir: Path) {
        val file = writeFixtureToTemp(tempDir, "ios_assertion_failure.log")
        val result = parser.parse(file.absolutePath)
        assertEquals(Platform.IOS, result.platform)
    }

    @Test
    fun `given Android fixture file when parse then returns ANDROID platform`(@TempDir tempDir: Path) {
        val file = writeFixtureToTemp(tempDir, "android_element_not_found.log")
        val result = parser.parse(file.absolutePath)
        assertEquals(Platform.ANDROID, result.platform)
    }

    @Test
    fun `given log file when parse then filePath is preserved in ParsedLog`(@TempDir tempDir: Path) {
        val file = writeFixtureToTemp(tempDir, "ios_assertion_failure.log")
        val result = parser.parse(file.absolutePath)
        assertEquals(file.absolutePath, result.filePath)
    }

    @Test
    fun `given non-existent file when parse then throws IllegalArgumentException`() {
        assertThrows<IllegalArgumentException> {
            parser.parse("/does/not/exist/test.log")
        }
    }

    // ── Task 3: extractRelevantLines ──────────────────────────────────────────

    @Test
    fun `given iOS log when extractRelevantLines then contains failure signal lines`() {
        val content = readFixture("ios_assertion_failure.log")
        val lines = parser.extractRelevantLines(content, Platform.IOS)

        assertTrue(lines.isNotEmpty(), "Expected at least one relevant line")
        assertTrue(
            lines.any { it.contains("XCTAssertEqual") || it.contains("failed") || it.contains("error:") },
            "Expected failure signal in extracted lines. Got:\n${lines.joinToString("\n")}"
        )
    }

    @Test
    fun `given iOS log when extractRelevantLines then timing lines are stripped`() {
        val content = readFixture("ios_assertion_failure.log")
        val lines = parser.extractRelevantLines(content, Platform.IOS)

        assertFalse(
            lines.any { it.startsWith("t =") },
            "Timing lines (t = ...) should be stripped as noise"
        )
    }

    @Test
    fun `given Android log when extractRelevantLines then contains exception lines`() {
        val content = readFixture("android_element_not_found.log")
        val lines = parser.extractRelevantLines(content, Platform.ANDROID)

        assertTrue(lines.isNotEmpty(), "Expected at least one relevant line")
        assertTrue(
            lines.any { it.contains("Exception") || it.contains("FAILED") || it.contains("Error") },
            "Expected exception signal in extracted lines. Got:\n${lines.joinToString("\n")}"
        )
    }

    @Test
    fun `given Android log when extractRelevantLines then UI noise lines are stripped`() {
        val noisyContent = """
            AndroidJUnitRunner started
            I Choreographer: Skipped 42 frames!
            I OpenGLRenderer: Initialized EGL
            E TestRunner: FAILED testLogin
            androidx.test.espresso.NoMatchingViewException: No views found
        """.trimIndent()
        val lines = parser.extractRelevantLines(noisyContent, Platform.ANDROID)

        assertFalse(lines.any { it.contains("Choreographer") }, "Choreographer noise should be stripped")
        assertFalse(lines.any { it.contains("OpenGLRenderer") }, "OpenGLRenderer noise should be stripped")
        assertTrue(lines.any { it.contains("FAILED") || it.contains("Exception") }, "Failure lines should be kept")
    }

    @Test
    fun `given log with many lines when extractRelevantLines then result is capped at MAX_RELEVANT_LINES`() {
        val manyFailureLines = (1..200).joinToString("\n") {
            "XCTAssertEqual failed: ($it) is not equal to (${it + 1})"
        }
        val content = "XCUITest suite\n$manyFailureLines"
        val lines = parser.extractRelevantLines(content, Platform.IOS)

        assertTrue(lines.size <= LogParser.MAX_RELEVANT_LINES, "Result should not exceed MAX_RELEVANT_LINES")
    }

    @Test
    fun `given log with only noise when extractRelevantLines then returns empty list`() {
        val noiseOnly = """
            XCUITest suite started
            t =     0.50s     Find the button
            t =     1.20s     Tap the button
            Build succeeded
        """.trimIndent()
        val lines = parser.extractRelevantLines(noiseOnly, Platform.IOS)

        assertTrue(lines.isEmpty(), "Pure noise log should yield no relevant lines")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun readFixture(name: String): String =
        LogParserTest::class.java.classLoader
            ?.getResourceAsStream("fixtures/$name")
            ?.bufferedReader()
            ?.readText()
            ?: error("Fixture not found: $name")

    private fun writeFixtureToTemp(tempDir: Path, name: String): File {
        val content = readFixture(name)
        val file = tempDir.resolve(name).toFile()
        file.writeText(content)
        return file
    }
}
