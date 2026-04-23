package classifier.llm

import agent.JsonResponseParser
import agent.LogAnalystAgent
import model.LogAnalysis
import parser.ParsedLog

/**
 * LLM-based fallback classifier.
 *
 * Wraps the Koog agent and maps its response to LogAnalysis
 * with classifiedBy = ClassifierSource.LLM.
 *
 * Only called when DeterministicClassifier returns null.
 */
class LlmClassifier(
    private val agent: LogAnalystAgent = LogAnalystAgent(),
    private val responseParser: JsonResponseParser = JsonResponseParser()
) {

    suspend fun classify(log: ParsedLog): LogAnalysis {
        val logContent = log.relevantLines.joinToString("\n")
        val rawResponse = agent.analyse(logContent)
        return responseParser.parse(rawResponse, log.platform)
    }
}
