package com.mediadock.app.core.common

import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

object FormattingUtils {

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
    }

    fun formatSpeed(bytesPerSec: Long): String {
        return "${formatBytes(bytesPerSec)}/s"
    }

    fun formatDuration(seconds: Long): String {
        if (seconds <= 0) return "00:00"
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, secs)
        }
    }

    fun sanitizeFilename(name: String, fallback: String = "media"): String {
        val clean = name.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
            .replace("_+".toRegex(), "_")
            .trim('_', ' ')
        return if (clean.isBlank()) fallback else clean
    }

    fun isValidUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val trimmed = url.trim()
        return (trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)) &&
                trimmed.length >= 10
    }
}
