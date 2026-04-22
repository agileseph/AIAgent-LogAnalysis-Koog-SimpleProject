package parser

import model.Platform
import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

class LogParserTest {

    private val parser = LogParser()

    // ── iOS positive ────────────────────────────────────────────────────────

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

    // ── Android positive ─────────────────────────────────────────────────────

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

    // ── Ambiguous / negative ─────────────────────────────────────────────────

    @Test
    fun `given unrecognised log content when detectPlatform then throws UnknownPlatformException`() {
        val content = "Something went wrong at line 42. No platform signals present."
        assertThrows<UnknownPlatformException> {
            parser.detectPlatform(content)
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
        // XCUITest + XCTAssert (2 iOS signals) vs androidx.test (1 Android signal)
        val content = """
            XCUITest suite started
            XCTAssertEqual failed
            androidx.test present in dependency list
        """.trimIndent()
        assertEquals(Platform.IOS, parser.detectPlatform(content))
    }

    // ── Helper ───────────────────────────────────────────────────────────────

    private fun readFixture(name: String): String =
        LogParserTest::class.java.classLoader
            ?.getResourceAsStream("fixtures/$name")
            ?.bufferedReader()
            ?.readText()
            ?: error("Fixture not found: $name")
}
