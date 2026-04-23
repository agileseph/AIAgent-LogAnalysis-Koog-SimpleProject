package agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.llms.all.simpleOllamaAIExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.llm.OllamaLLMProvider
import kotlinx.coroutines.withTimeout

private const val OLLAMA_BASE_URL = "http://localhost:11434"
private const val MODEL_ID = "qwen2.5-coder:7b"
private const val TIMEOUT_MS = 60_000L

/**
 * Sends a structured log analysis prompt to local Ollama and returns the raw LLM response.
 * Task 9b (JsonResponseParser) handles correction of malformed JSON responses.
 */
class LogAnalystAgent {

    suspend fun analyse(logContent: String): String = withTimeout(TIMEOUT_MS) {
        AIAgent.builder()
            .promptExecutor(simpleOllamaAIExecutor(OLLAMA_BASE_URL))
            .llmModel(LLModel(OllamaLLMProvider, MODEL_ID))
            .build()
            .run(AnalysisPrompt.build(logContent))
    }
}
