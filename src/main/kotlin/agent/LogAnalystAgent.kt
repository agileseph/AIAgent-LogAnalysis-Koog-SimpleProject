package agent

/**
 * Task 9a: Koog agent scaffolding.
 *
 * Connects to local Ollama (qwen2.5-coder:7b) and sends
 * a structured prompt requesting JSON-only LogAnalysis output.
 *
 * Task 9b: JsonResponseParser handles correction of malformed responses.
 */
class LogAnalystAgent {

    suspend fun analyse(logContent: String): String {
        TODO("Task 9a: implement Koog agent with Ollama backend")
    }
}
