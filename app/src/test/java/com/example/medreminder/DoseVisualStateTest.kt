package com.example.medreminder

import com.example.medreminder.domain.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class DoseVisualStateTest {
    private val due = 10_000_000L

    @Test fun futureDoseIsNeutralAndDelayTurnsYellowThenRed() {
        assertEquals(DoseVisualState.UPCOMING, doseVisualState(due, due - 1))
        assertEquals(DoseVisualState.WAITING, doseVisualState(due, due))
        assertEquals(DoseVisualState.WAITING, doseVisualState(due, due + 60 * 60_000L))
        assertEquals(DoseVisualState.MISSED, doseVisualState(due, due + 60 * 60_000L + 1))
    }

    @Test fun takenOverridesDelayAndSkippedIsRed() {
        assertEquals(DoseVisualState.TAKEN, doseVisualState(due, due + 120 * 60_000L, takenAt = due))
        assertEquals(DoseVisualState.MISSED, doseVisualState(due, due, skippedAt = due))
        assertEquals(DoseVisualState.WAITING, doseVisualState(due, due, snoozedUntil = due + 15 * 60_000L))
        assertEquals(DoseVisualState.MISSED, doseVisualState(due, due + 61 * 60_000L,
            snoozedUntil = due + 90 * 60_000L))
    }

    @Test fun cardHighlightsUnresolvedDosesWithoutLosingIndividualStatuses() {
        assertEquals(DoseVisualState.MISSED, overallDoseVisualState(listOf(DoseVisualState.TAKEN, DoseVisualState.MISSED)))
        assertEquals(DoseVisualState.WAITING, overallDoseVisualState(listOf(DoseVisualState.TAKEN, DoseVisualState.WAITING)))
        assertEquals(DoseVisualState.TAKEN, overallDoseVisualState(listOf(DoseVisualState.TAKEN, DoseVisualState.UPCOMING)))
        assertEquals(DoseVisualState.UPCOMING, overallDoseVisualState(listOf(DoseVisualState.UPCOMING)))
    }
}
