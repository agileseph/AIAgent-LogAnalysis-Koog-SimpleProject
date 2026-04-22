package agent

import model.FailureCategory

/**
 * Task 9a: Prompt template for the Koog agent.
 *
 * Instructs the LLM to respond ONLY with a JSON object
 * matching the LogAnalysis shape. No markdown, no preamble.
 */
object AnalysisPrompt {

    private val categories = FailureCategory.entries.joinToString(", ") { it.name }

    fun build(logContent: String): String = """
        You are a mobile test failure analyser. Analyse the following test log and respond ONLY with a JSON object.
        Do not include any explanation, markdown, or code fences. Return raw JSON only.
        
        Required JSON shape:
        {
          "failureCategory": "<one of: $categories>",
          "confidence": <float 0.0-1.0>,
          "evidence": ["<log excerpt>", ...],
          "recommendedAction": "<short actionable string>",
          "rawSummary": "<one sentence summary>"
        }
        
        Log content:
        $logContent
    """.trimIndent()
}
