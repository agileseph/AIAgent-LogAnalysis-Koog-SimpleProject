package parser

import model.Platform

/**
 * Output of the parser stage.
 * Contains the detected platform and the relevant log lines
 * (noise stripped, failure signal preserved).
 */
data class ParsedLog(
    val platform: Platform,
    val relevantLines: List<String>,
    val filePath: String
)
