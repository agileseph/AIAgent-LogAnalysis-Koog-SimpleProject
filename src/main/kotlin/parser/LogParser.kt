package parser

import model.Platform

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
        TODO("Task 3: implement log file reading and line extraction")
    }

    companion object {
        // Signals ordered by specificity — more specific markers first
        val IOS_SIGNALS = listOf(
            "XCUITest",
            "XCTest",
            "XCTAssert",
            "xcresult",
            "-[",                   // ObjC test method signature e.g. -[LoginUITests testLogin]
            "com.apple.test",
            "com.apple.dt.xctest",
            "UIKitCore",
            "CoreSimulator"
        )

        val ANDROID_SIGNALS = listOf(
            "AndroidJUnitRunner",
            "androidx.test",
            "Espresso",
            "NoMatchingViewException",
            "FATAL EXCEPTION",
            "AndroidRuntime",
            "dalvik",
            "com.android",
            "adb shell"
        )
    }
}

class UnknownPlatformException(excerpt: String) :
    IllegalArgumentException(
        "Cannot detect platform from log content.\nExcerpt:\n$excerpt"
    )
