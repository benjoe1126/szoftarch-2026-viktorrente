package org.vik.torrente.ui.util

import kotlin.math.roundToInt

/**
 * Utility functions for formatting byte counts, speeds, and percentages.
 */
object Formatters {

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return "${(kb * 10).roundToInt() / 10.0} KB"
        val mb = kb / 1024.0
        if (mb < 1024.0) return "${(mb * 10).roundToInt() / 10.0} MB"
        val gb = mb / 1024.0
        return "${(gb * 100).roundToInt() / 100.0} GB"
    }

    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0L) return "0 B/s"
        return "${formatBytes(bytesPerSec)}/s"
    }
}
