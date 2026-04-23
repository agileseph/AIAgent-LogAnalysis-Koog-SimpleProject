package agent

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import model.ClassifierSource
import model.FailureCategory
import model.LogAnalysis
import model.Platform

private val json = Json { ignoreUnknownKeys = true }

/**
 * Parses and corrects LLM JSON responses into LogAnalysis.
 *
 * Correction pipeline (applied in order):
 *   1. Strip markdown fences (```json ... ```)
 *   2. Remove trailing commas before } or ]
 *   3. Trim leading/trailing whitespace and noise
 *   4. Attempt JSON parse → LogAnalysis
 *   5. On unrecoverable failure → LogAnalysis(UNKNOWN) with rawSummary = raw response
 */
class JsonResponseParser {

    fun parse(rawResponse: String, platform: Platform): LogAnalysis = runCatching {
        val corrected = rawResponse
            .let(::stripMarkdownFences)
            .let(::fixTrailingCommas)
            .trim()

        val dto = json.decodeFromString<LlmResponseDto>(corrected)

        val confidence = dto.confidence.coerceIn(0.0f, 1.0f)

        val evidence = when {
            dto.evidence.isNotEmpty() -> dto.evidence
            dto.failureCategory == FailureCategory.UNKNOWN -> emptyList()
            else -> listOf(dto.rawSummary)
        }

        LogAnalysis(
            platform = platform,
            failureCategory = dto.failureCategory,
            confidence = confidence,
            classifiedBy = ClassifierSource.LLM,
            evidence = evidence,
            recommendedAction = dto.recommendedAction,
            rawSummary = dto.rawSummary
        )
    }.getOrElse { fallback(rawResponse, platform) }

    fun fallback(rawResponse: String, platform: Platform): LogAnalysis = LogAnalysis(
        platform = platform,
        failureCategory = FailureCategory.UNKNOWN,
        confidence = 0.0f,
        classifiedBy = ClassifierSource.LLM,
        evidence = emptyList(),
        recommendedAction = "Inspect raw LLM response in rawSummary field",
        rawSummary = rawResponse.take(500)
    )

    private fun stripMarkdownFences(raw: String): String =
        raw.replace(Regex("```json\\s*"), "")
           .replace(Regex("```\\s*"), "")
           .trim()

    private fun fixTrailingCommas(raw: String): String =
        raw.replace(Regex(",\\s*([}\\]])"), "$1")
}

@Serializable
private data class LlmResponseDto(
    val failureCategory: FailureCategory,
    val confidence: Float,
    val evidence: List<String>,
    val recommendedAction: String,
    val rawSummary: String
)
