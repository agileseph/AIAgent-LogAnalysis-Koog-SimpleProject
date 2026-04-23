package model

import kotlinx.serialization.Serializable

@Serializable
enum class ClassifierSource {
    DETERMINISTIC,
    LLM
}
