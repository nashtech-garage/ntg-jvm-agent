package com.ntgjvmagent.orchestrator.utils

import java.time.Duration
import java.time.Instant

fun Instant?.toRelativeString(format: RelativeFormat = RelativeFormat.FRIENDLY): String? {
    if (this == null) return null

    val now = Instant.now()
    val duration = Duration.between(this, now)

    val result =
        if (duration.isNegative) {
            "in the future"
        } else {
            when (format) {
                RelativeFormat.FRIENDLY -> formatFriendly(duration)
                RelativeFormat.COMPACT -> formatCompact(duration)
            }
        }

    return result
}

// -----------------------------------------------------------
// Constants
// -----------------------------------------------------------
private const val MINUTES_PER_HOUR = 60L
private const val HOURS_PER_DAY = 24L
private const val DAYS_PER_MONTH = 30L // Approximation
private const val MONTHS_PER_YEAR = 12L

private const val MINUTES_PER_DAY = MINUTES_PER_HOUR * HOURS_PER_DAY
private const val MINUTES_PER_MONTH = MINUTES_PER_DAY * DAYS_PER_MONTH
private const val MINUTES_PER_YEAR = MINUTES_PER_MONTH * MONTHS_PER_YEAR

// -----------------------------------------------------------
// FRIENDLY FORMAT
// -----------------------------------------------------------
private fun formatFriendly(duration: Duration): String {
    val minutes = duration.toMinutes()

    val years = minutes / MINUTES_PER_YEAR
    val months = (minutes % MINUTES_PER_YEAR) / MINUTES_PER_MONTH
    val days = (minutes % MINUTES_PER_MONTH) / MINUTES_PER_DAY
    val hours = (minutes % MINUTES_PER_DAY) / MINUTES_PER_HOUR
    val mins = minutes % MINUTES_PER_HOUR

    val parts = mutableListOf<String>()

    if (years > 0) parts += plural(years, "year")
    if (months > 0) parts += plural(months, "month")
    if (days > 0) parts += plural(days, "day")
    if (hours > 0) parts += plural(hours, "hour")
    if (mins > 0) parts += plural(mins, "minute")

    return if (parts.isEmpty()) "just now" else parts.take(2).joinToString(" ") + " ago"
}

// -----------------------------------------------------------
// COMPACT FORMAT
// -----------------------------------------------------------
private fun formatCompact(duration: Duration): String {
    val minutes = duration.toMinutes()

    val years = minutes / MINUTES_PER_YEAR
    val months = (minutes % MINUTES_PER_YEAR) / MINUTES_PER_MONTH
    val days = (minutes % MINUTES_PER_MONTH) / MINUTES_PER_DAY
    val hours = (minutes % MINUTES_PER_DAY) / MINUTES_PER_HOUR
    val mins = minutes % MINUTES_PER_HOUR

    return when {
        years > 0 -> "${years}y${if (months > 0) months.toString() + "mo" else ""} ago"
        months > 0 -> "${months}mo${if (days > 0) days.toString() + "d" else ""} ago"
        days > 0 -> "${days}d${if (hours > 0) hours.toString() + "h" else ""} ago"
        hours > 0 -> "${hours}h${if (mins > 0) mins.toString() + "m" else ""} ago"
        mins > 0 -> "${mins}m ago"
        else -> "just now"
    }
}

// -----------------------------------------------------------
// Helper
// -----------------------------------------------------------
private fun plural(
    value: Long,
    label: String,
): String = "$value $label${if (value > 1) "s" else ""}"

enum class RelativeFormat { FRIENDLY, COMPACT }
