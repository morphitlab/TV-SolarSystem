package com.solarsystemlive.ui.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin
import kotlin.random.Random

/**
 * Deterministic, allocation-free twinkling starfield.
 * Pre-generates star coordinates once; reads timeSec only in the draw phase.
 */
class Starfield(count: Int = 180, seed: Int = 42) {
    private val xs = FloatArray(count)
    private val ys = FloatArray(count)
    private val radii = FloatArray(count)
    private val phases = FloatArray(count)
    private val speeds = FloatArray(count)

    init {
        val rng = Random(seed)
        for (i in 0 until count) {
            xs[i] = rng.nextFloat()
            ys[i] = rng.nextFloat()
            radii[i] = 0.8f + rng.nextFloat() * 1.6f
            phases[i] = rng.nextFloat() * 6.283185f
            speeds[i] = 0.6f + rng.nextFloat() * 1.4f
        }
    }

    fun DrawScope.draw(timeSec: Float, alphaScale: Float = 1f) {
        val w = size.width
        val h = size.height
        for (i in xs.indices) {
            val twinkle = 0.45f + 0.55f * (0.5f + 0.5f * sin(timeSec * speeds[i] + phases[i]))
            drawCircle(
                color = Color.White.copy(alpha = twinkle * 0.75f * alphaScale),
                radius = radii[i],
                center = Offset(xs[i] * w, ys[i] * h)
            )
        }
    }
}
