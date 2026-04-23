package model

import kotlinx.serialization.Serializable

@Serializable
enum class FailureCategory {
    ASSERTION_FAILURE,
    ELEMENT_NOT_FOUND,
    TIMEOUT,
    APP_CRASH,
    NETWORK_ERROR,
    FLAKY_TEST,
    ENVIRONMENT_ISSUE,
    UNKNOWN
}
