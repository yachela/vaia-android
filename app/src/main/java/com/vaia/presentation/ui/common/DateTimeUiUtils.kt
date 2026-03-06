package com.vaia.presentation.ui.common

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    isLenient = false
}

private val backendDateCandidates = listOf(
    "yyyy-MM-dd",
    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
    "yyyy-MM-dd'T'HH:mm:ss'Z'",
    "yyyy-MM-dd'T'HH:mm:ss",
    "yyyy-MM-dd HH:mm:ss"
)

private val backendTimeCandidates = listOf(
    "HH:mm",
    "HH:mm:ss",
    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
    "yyyy-MM-dd'T'HH:mm:ss'Z'",
    "yyyy-MM-dd'T'HH:mm:ss",
    "yyyy-MM-dd HH:mm:ss"
)

fun normalizeDateForApi(rawValue: String): String? {
    val value = rawValue.trim()
    if (value.isEmpty()) return null

    backendDateCandidates.forEach { pattern ->
        val parser = SimpleDateFormat(pattern, Locale.US).apply {
            isLenient = false
            if (pattern.contains("'Z'")) {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        }
        try {
            val parsed = parser.parse(value) ?: return@forEach
            return apiDateFormat.format(parsed)
        } catch (_: ParseException) {
            // keep trying patterns
        }
    }
    return null
}

fun normalizeTimeForApi(rawValue: String): String? {
    val value = rawValue.trim()
    if (value.isEmpty()) return null

    value.toLongOrNull()?.let { epoch ->
        val millis = if (value.length <= 10) epoch * 1000 else epoch
        return SimpleDateFormat("HH:mm", Locale.US).format(Date(millis))
    }

    backendTimeCandidates.forEach { pattern ->
        val parser = SimpleDateFormat(pattern, Locale.US).apply {
            isLenient = false
            if (pattern.contains("'Z'")) {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        }
        try {
            val parsed = parser.parse(value) ?: return@forEach
            return SimpleDateFormat("HH:mm", Locale.US).format(parsed)
        } catch (_: ParseException) {
            // keep trying patterns
        }
    }
    return null
}

fun formatDateForDisplay(rawValue: String): String {
    return normalizeDateForApi(rawValue) ?: rawValue
}

fun formatTimeForDisplay(rawValue: String): String {
    return normalizeTimeForApi(rawValue) ?: rawValue
}
