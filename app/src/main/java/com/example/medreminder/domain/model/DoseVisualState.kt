package com.example.medreminder.domain.model

enum class DoseVisualState { UPCOMING, TAKEN, WAITING, MISSED }

fun doseVisualState(
    scheduledAt: Long,
    now: Long,
    takenAt: Long? = null,
    skippedAt: Long? = null,
    snoozedUntil: Long? = null
): DoseVisualState = when {
    takenAt != null -> DoseVisualState.TAKEN
    skippedAt != null -> DoseVisualState.MISSED
    now > scheduledAt && now - scheduledAt > 60 * 60_000L -> DoseVisualState.MISSED
    (snoozedUntil != null && snoozedUntil > now) || now >= scheduledAt -> DoseVisualState.WAITING
    else -> DoseVisualState.UPCOMING
}

fun overallDoseVisualState(states: List<DoseVisualState>): DoseVisualState = when {
    DoseVisualState.MISSED in states -> DoseVisualState.MISSED
    DoseVisualState.WAITING in states -> DoseVisualState.WAITING
    DoseVisualState.TAKEN in states -> DoseVisualState.TAKEN
    else -> DoseVisualState.UPCOMING
}
