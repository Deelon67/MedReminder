package com.example.medreminder

import com.example.medreminder.domain.model.ReminderPolicy
import org.junit.Assert.*
import org.junit.Test

class ReminderPolicyTest {
    @Test fun earlyWindowStartsExactlyThirtyMinutesBeforeDose() {
        val due = 10_000_000L
        assertFalse(ReminderPolicy.canMarkTaken(due, due - 30 * 60_000L - 1))
        assertTrue(ReminderPolicy.canMarkTaken(due, due - 30 * 60_000L))
        assertTrue(ReminderPolicy.canMarkTaken(due, due))
    }

    @Test fun pastDoseRepeatUsesScheduledTimeInsteadOfCreationTime() {
        val due = 10_000_000L
        val now = due + 10 * 60_000L
        assertEquals(due + 15 * 60_000L,
            ReminderPolicy.firstRepeatForPastDose(due, now, true, 15))
        assertEquals(due + 30 * 60_000L,
            ReminderPolicy.firstRepeatForPastDose(due, now, true, 30))
        assertEquals(now + 1_000,
            ReminderPolicy.firstRepeatForPastDose(due - 20 * 60_000L, now, true, 15))
        assertNull(ReminderPolicy.firstRepeatForPastDose(due, now, false, 15))
        assertNull(ReminderPolicy.firstRepeatForPastDose(due, due - 1, true, 15))
        assertNull(ReminderPolicy.firstRepeatForPastDose(due, due, true, 15))
        assertNull(ReminderPolicy.firstRepeatForPastDose(due, due + 24 * 60 * 60_000L, true, 15))
    }

    @Test fun automaticRemindersAreBoundedAndCanBeDisabled() {
        val now = 10_000_000L
        assertEquals(now + 15 * 60_000L, ReminderPolicy.nextRepeat(now, 0, true, 15))
        assertEquals(now + 30 * 60_000L, ReminderPolicy.nextRepeat(now, 1, true, 30))
        assertNull(ReminderPolicy.nextRepeat(now, 2, true, 15))
        assertNull(ReminderPolicy.nextRepeat(now, 0, false, 15))
    }
}
