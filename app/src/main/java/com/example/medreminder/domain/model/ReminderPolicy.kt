package com.example.medreminder.domain.model

object ReminderPolicy {
    const val EARLY_WINDOW_MS = 30 * 60_000L
    const val MAX_REPEATS = 2
    fun canMarkTaken(scheduledAt: Long, now: Long): Boolean = now >= scheduledAt - EARLY_WINDOW_MS
    fun firstRepeatForPastDose(scheduledAt: Long, now: Long, enabled: Boolean, intervalMinutes: Int): Long? =
        if (enabled && now > scheduledAt && now - scheduledAt < 24 * 60 * 60_000L)
            maxOf(now + 1_000, scheduledAt + intervalMinutes * 60_000L)
        else null

    fun nextRepeat(now: Long, count: Int, enabled: Boolean, intervalMinutes: Int): Long? =
        if (enabled && count < MAX_REPEATS) now + intervalMinutes * 60_000L else null
}
