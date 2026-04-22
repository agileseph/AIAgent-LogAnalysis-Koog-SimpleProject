package model

data class LogAnalysis(
    val platform: Platform,
    val failureCategory: FailureCategory,
    val confidence: Float,               // 0.0–1.0
    val classifiedBy: ClassifierSource,
    val evidence: List<String>,          // log excerpts supporting the diagnosis
    val recommendedAction: String,
    val rawSummary: String
) {
    init {
        require(confidence in 0.0f..1.0f) {
            "confidence must be in range [0.0, 1.0], got $confidence"
        }
        require(evidence.isNotEmpty() || failureCategory == FailureCategory.UNKNOWN) {
            "evidence must not be empty unless category is UNKNOWN"
        }
    }
}
