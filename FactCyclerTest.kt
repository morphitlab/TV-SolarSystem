package com.solarsystemlive

import com.solarsystemlive.data.FactCycler
import org.junit.Assert.assertEquals
import org.junit.Test

class FactCyclerTest {

    @Test
    fun cyclesAndWrapsAround() {
        val cycler = FactCycler(3)
        assertEquals(0, cycler.currentIndex)
        assertEquals(1, cycler.next())
        assertEquals(2, cycler.next())
        assertEquals(0, cycler.next())
    }
}
