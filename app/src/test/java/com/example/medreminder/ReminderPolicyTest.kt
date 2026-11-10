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

    @Test fun automaticRemindersAreBoundedAndCanBeDisabled() {
        val now = 10_000_000L
        assertEquals(now + 15 * 60_000L, ReminderPolicy.nextRepeat(now, 0, true, 15))
        assertEquals(now + 30 * 60_000L, ReminderPolicy.nextRepeat(now, 1, true, 30))
        assertNull(ReminderPolicy.nextRepeat(now, 2, true, 15))
        assertNull(ReminderPolicy.nextRepeat(now, 0, false, 15))
    }
}
