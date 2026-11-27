package com.solarsystemlive.data

/**
 * Single simulation clock driving the entire app.
 * Default speed: 2.0 days per second (gentle time-lapse).
 */
class TimeController(
    initialEpochMillis: Long = System.currentTimeMillis()
) {
    companion object {
        private const val MILLIS_PER_DAY = 86_400_000.0
        val SPEED_STEPS_DAYS_PER_SEC: List<Double> = listOf(
            -365.0, -90.0, -30.0, -7.0, -2.0, -1.0,
            0.0,
            1.0, 2.0, 7.0, 30.0, 90.0, 365.0
        )
        const val DEFAULT_SPEED_INDEX = 8 // +2.0 days/sec
    }

    var simulationEpochMillis: Long = initialEpochMillis
        private set

    var speedIndex: Int = DEFAULT_SPEED_INDEX
        private set

    var isPaused: Boolean = false
        private set

    val currentSpeedDaysPerSec: Double
        get() = if (isPaused) 0.0 else SPEED_STEPS_DAYS_PER_SEC[speedIndex]

    fun tick(deltaSeconds: Double) {
        if (isPaused || deltaSeconds <= 0.0) return
        val clampedDelta = deltaSeconds.coerceAtMost(0.1) // guard against frame spikes
        val deltaDays = SPEED_STEPS_DAYS_PER_SEC[speedIndex] * clampedDelta
        simulationEpochMillis += (deltaDays * MILLIS_PER_DAY).toLong()
    }

    fun togglePlayPause() {
        isPaused = !isPaused
    }

    fun fastForward() {
        isPaused = false
        if (speedIndex < SPEED_STEPS_DAYS_PER_SEC.lastIndex) {
            speedIndex++
            if (SPEED_STEPS_DAYS_PER_SEC[speedIndex] == 0.0 && speedIndex < SPEED_STEPS_DAYS_PER_SEC.lastIndex) {
                speedIndex++
            }
        }
    }

    fun rewind() {
        isPaused = false
        if (speedIndex > 0) {
            speedIndex--
            if (SPEED_STEPS_DAYS_PER_SEC[speedIndex] == 0.0 && speedIndex > 0) {
                speedIndex--
            }
        }
    }

    fun resetToLive(nowMillis: Long = System.currentTimeMillis()) {
        simulationEpochMillis = nowMillis
        speedIndex = DEFAULT_SPEED_INDEX
        isPaused = false
    }

    fun speedLabel(): String {
        if (isPaused) return "PAUSED"
        val s = SPEED_STEPS_DAYS_PER_SEC[speedIndex]
        val sign = if (s < 0) "-" else "+"
        val absVal = kotlin.math.abs(s)
        return when {
            absVal >= 365.0 -> "${sign}1 yr/s"
            absVal >= 30.0  -> "${sign}${(absVal / 30).toInt()} mo/s"
            else            -> "${sign}${absVal.toInt()} d/s"
        }
    }
}
