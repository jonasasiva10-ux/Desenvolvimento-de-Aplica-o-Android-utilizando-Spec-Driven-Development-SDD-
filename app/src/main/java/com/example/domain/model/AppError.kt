package com.example.domain.model

sealed interface AppError {
    data object NetworkUnavailable : AppError
    data object Timeout : AppError
    data class ServerError(val code: Int, val message: String? = null) : AppError
    data object NotFound : AppError
    data class Unknown(val message: String? = null) : AppError
}

sealed interface SyncOutcome {
    data object Success : SyncOutcome
    data class Failure(val error: AppError) : SyncOutcome
}
