package com.solarsystemlive

import com.solarsystemlive.data.CelestialBody
import com.solarsystemlive.data.OrbitEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class OrbitEngineTest {

    @Test
    fun earthAtJ2000EpochMatchesReferenceLongitude() {
        // J2000.0 = 2000-01-01 12:00:00 UTC -> Earth heliocentric longitude ~100.46 deg
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2000, Calendar.JANUARY, 1, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val positions = OrbitEngine.computeAllPositions(cal.timeInMillis)
        val earth = positions[CelestialBody.EARTH]!!
        val lonDeg = Math.toDegrees(earth.heliocentricLongitudeRad)
        assertEquals(100.46, lonDeg, 1.5)
        assertEquals(0.983, earth.distanceAu, 0.02)
    }

    @Test
    fun allTenBodiesAreComputed() {
        val positions = OrbitEngine.computeAllPositions(System.currentTimeMillis())
        assertEquals(10, positions.size)
        assertTrue(positions.containsKey(CelestialBody.MOON))
    }
}
