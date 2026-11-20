package com.example.presentation.components

import androidx.annotation.StringRes
import com.example.R
import com.example.domain.model.AppError
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object TelemetryFormatter {
    private val ptBrLocale: Locale = Locale.forLanguageTag("pt-BR")

    fun formatPlayerCount(count: Int?): String {
        if (count == null || count < 0) return "--"
        return when {
            count >= 1_000_000 -> String.format(Locale.US, "%.2fM", count / 1_000_000.0)
            count >= 10_000 -> String.format(Locale.US, "%.1fK", count / 1_000.0)
            else -> String.format(ptBrLocale, "%,d", count)
        }
    }

    fun formatRelativeTimeFromEpochSeconds(
        epochSeconds: Long,
        nowMillis: Long = System.currentTimeMillis(),
    ): String {
        if (epochSeconds <= 0L) return "Recente"
        val diffSeconds = ((nowMillis / 1000L) - epochSeconds).coerceAtLeast(0L)
        return when {
            diffSeconds < 60 -> "Agora mesmo"
            diffSeconds < 3600 -> "Há ${diffSeconds / 60} min"
            diffSeconds < 86_400 -> "Há ${diffSeconds / 3600}h"
            diffSeconds < 604_800 -> "Há ${diffSeconds / 86_400}d"
            else -> {
                val formatter = SimpleDateFormat("dd/MM/yyyy", ptBrLocale)
                formatter.format(Date(epochSeconds * 1000L))
            }
        }
    }

    fun formatSyncClock(epochMillis: Long?): String {
        if (epochMillis == null || epochMillis <= 0L) return "Nunca sincronizado"
        val formatter = SimpleDateFormat("HH:mm", ptBrLocale)
        return "Sync ${formatter.format(Date(epochMillis))}"
    }

    fun formatDuration(seconds: Int): String {
        val safe = abs(seconds)
        val mins = safe / 60
        val secs = safe % 60
        return String.format(Locale.US, "%d:%02d", mins, secs)
    }

    @StringRes
    fun errorTitleRes(error: AppError): Int = when (error) {
        AppError.NetworkUnavailable -> R.string.error_network_title
        AppError.Timeout -> R.string.error_timeout_title
        is AppError.ServerError -> R.string.error_server_title
        AppError.NotFound -> R.string.error_not_found_title
        is AppError.Unknown -> R.string.error_unknown_title
    }

    @StringRes
    fun errorMessageRes(error: AppError): Int = when (error) {
        AppError.NetworkUnavailable -> R.string.error_network_message
        AppError.Timeout -> R.string.error_timeout_message
        is AppError.ServerError -> R.string.error_server_message
        AppError.NotFound -> R.string.error_not_found_message
        is AppError.Unknown -> R.string.error_unknown_message
    }
}
