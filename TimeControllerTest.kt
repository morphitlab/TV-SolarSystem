package com.solarsystemlive

import com.solarsystemlive.data.TimeController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeControllerTest {

    @Test
    fun defaultSpeedAdvancesTwoDaysPerSecond() {
        val start = 1_700_000_000_000L
        val tc = TimeController(start)
        // 10 ticks of 0.1s = 1.0s -> +2 days
        repeat(10) { tc.tick(0.1) }
        val expected = start + (2.0 * 86_400_000.0).toLong()
        assertEquals(expected, tc.simulationEpochMillis)
    }

    @Test
    fun playPauseFreezesTime() {
        val start = 1_700_000_000_000L
        val tc = TimeController(start)
        tc.togglePlayPause()
        assertTrue(tc.isPaused)
        tc.tick(0.1)
        assertEquals(start, tc.simulationEpochMillis)
        tc.resetToLive(start + 5000L)
        assertFalse(tc.isPaused)
        assertEquals(start + 5000L, tc.simulationEpochMillis)
    }
}
