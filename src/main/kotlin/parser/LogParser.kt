package parser

import model.Platform
import java.io.File

/**
 * Parses mobile test failure logs.
 *
 * Task 2: Platform detection from raw log content.
 * Task 3: Relevant line extraction → ParsedLog.
 */
class LogParser {

    fun detectPlatform(content: String): Platform {
        val iosScore = IOS_SIGNALS.count { signal -> content.contains(signal, ignoreCase = false) }
        val androidScore = ANDROID_SIGNALS.count { signal -> content.contains(signal, ignoreCase = false) }

        return when {
            iosScore > 0 && iosScore >= androidScore -> Platform.IOS
            androidScore > 0 -> Platform.ANDROID
            else -> throw UnknownPlatformException(content.take(300))
        }
    }

    fun parse(filePath: String): ParsedLog {
        val file = File(filePath)
        require(file.exists()) { "Log file not found: $filePath" }
        require(file.isFile) { "Path is not a file: $filePath" }
        require(file.canRead()) { "Log file is not readable: $filePath" }

        val content = file.readText(Charsets.UTF_8)
        val platform = detectPlatform(content)
        val relevantLines = extractRelevantLines(content, platform)

        return ParsedLog(
            platform = platform,
            relevantLines = relevantLines,
            filePath = filePath
        )
    }

    fun extractRelevantLines(content: String, platform: Platform): List<String> {
        val noisePatterns = COMMON_NOISE + when (platform) {
            Platform.IOS -> IOS_NOISE
            Platform.ANDROID -> ANDROID_NOISE
        }
        val signalPatterns = when (platform) {
            Platform.IOS -> IOS_FAILURE_SIGNALS
            Platform.ANDROID -> ANDROID_FAILURE_SIGNALS
        }

        return content.lines()
            .map { it.trim() }
            .filter { line -> line.isNotBlank() }
            .filter { line -> noisePatterns.none { noise -> line.contains(noise) } }
            .filter { line -> signalPatterns.any { signal -> line.contains(signal) } }
            .take(MAX_RELEVANT_LINES)
    }

    companion object {
        const val MAX_RELEVANT_LINES = 50

        // ── Platform detection signals ───────────────────────────────────────
        val IOS_SIGNALS = listOf(
            "XCUITest", "XCTest", "XCTAssert", "xcresult",
            "-[", "com.apple.test", "com.apple.dt.xctest",
            "UIKitCore", "CoreSimulator"
        )
        val ANDROID_SIGNALS = listOf(
            "AndroidJUnitRunner", "androidx.test", "Espresso",
            "NoMatchingViewException", "FATAL EXCEPTION",
            "AndroidRuntime", "dalvik", "com.android", "adb shell"
        )

        // ── Failure signal patterns (lines worth keeping) ────────────────────
        val IOS_FAILURE_SIGNALS = listOf(
            "error:",           // compiler/test error lines
            "XCTAssert",        // assertion failures
            "failed",           // test case failed
            "Exception",        // any exception
            "Unable to find",   // element not found
            "No matches found",
            "Timeout",
            "timed out",
            "crashed",
            "SIGABRT", "SIGSEGV",
            "Test Case",        // test lifecycle — start/fail lines
            "Executed",         // summary line
            "assert",
            "kCFRunLoopDefaultMode"  // runloop timeout signal
        )
        val ANDROID_FAILURE_SIGNALS = listOf(
            "FAILED",
            "ERROR",
            "Exception",
            "Error",
            "FATAL EXCEPTION",
            "NoMatchingViewException",
            "AmbiguousViewMatcherException",
            "PerformException",
            "AssertionFailedError",
            "assert",
            "expected:",
            "but was:",
            "TestRunner",       // instrumentation runner output
            "Process:",         // crash header
            "Caused by:"
        )

        // ── Noise patterns (lines to discard) ────────────────────────────────
        val COMMON_NOISE = listOf(
            "OpenJDK",
            "VM warning",
            "Picked up",
            "Starting process"
        )
        val IOS_NOISE = listOf(
            "t =",              // timing lines: "t =   0.50s   Find the..."
            "Build succeeded",
            "CompileSwift",
            "CompileC",
            "Ld ",
            "Touch ",
            "CodeSign",
            "ProcessInfoPlistFile"
        )
        val ANDROID_NOISE = listOf(
            "I Choreographer",
            "I OpenGLRenderer",
            "D EGL_emulation",
            "V InputMethodManager",
            "D gralloc",
            "I art     :",
            "D NetworkSecurityConfig"
        )
    }
}

class UnknownPlatformException(excerpt: String) :
    IllegalArgumentException(
        "Cannot detect platform from log content.\nExcerpt:\n$excerpt"
    )
