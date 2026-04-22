package parser

import model.Platform

/**
 * Task 2: Detects platform from raw log content.
 * Task 3: Extracts relevant lines into ParsedLog.
 */
class LogParser {

    fun detectPlatform(content: String): Platform = when {
        content.contains("XCTest") || content.contains("xcresult") ||
        content.contains("XCUITest") || content.contains("com.apple") -> Platform.IOS
        content.contains("Espresso") || content.contains("AndroidJUnitRunner") ||
        content.contains("androidx.test") || content.contains("adb") -> Platform.ANDROID
        else -> throw UnknownPlatformException(content.take(200))
    }

    fun parse(filePath: String): ParsedLog {
        TODO("Task 3: implement log file reading and line extraction")
    }
}

class UnknownPlatformException(excerpt: String) :
    IllegalArgumentException("Cannot detect platform from log content. Excerpt: $excerpt")
