package agent

import model.ClassifierSource
import model.FailureCategory
import model.LogAnalysis
import model.Platform

/**
 * Task 9b: Parses and corrects LLM JSON responses into LogAnalysis.
 *
 * Correction pipeline (applied in order):
 *   1. Strip markdown fences (```json ... ```)
 *   2. Remove trailing commas before } or ]
 *   3. Trim leading/trailing whitespace and noise
 *   4. Attempt JSON parse → LogAnalysis
 *   5. On unrecoverable failure → LogAnalysis(UNKNOWN) with rawSummary = raw response
 */
class JsonResponseParser {

    fun parse(rawResponse: String, platform: Platform): LogAnalysis {
        TODO("Task 9b: implement JSON correction and parsing pipeline")
    }

    private fun stripMarkdownFences(raw: String): String =
        raw.replace(Regex("```json\\s*"), "")
           .replace(Regex("```\\s*"), "")
           .trim()

    private fun fixTrailingCommas(raw: String): String =
        raw.replace(Regex(",\\s*([}\\]])"), "$1")

    fun fallback(rawResponse: String, platform: Platform): LogAnalysis = LogAnalysis(
        platform = platform,
        failureCategory = FailureCategory.UNKNOWN,
        confidence = 0.0f,
        classifiedBy = ClassifierSource.LLM,
        evidence = emptyList(),
        recommendedAction = "Inspect raw LLM response in rawSummary field",
        rawSummary = rawResponse.take(500)
    )
}
