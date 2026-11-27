package com.solarsystemlive.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.solarsystemlive.data.CelestialBody
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Shader-free planet renderer (API 28+ compatible, no AGSL/RuntimeShader).
 * Draws:
 * 1. Pre-made circular globe texture.
 * 2. Rotated soft terminator shadow gradient aligned to the real Sun direction.
 * 3. Earth night-lights layer revealed on the dark side.
 * 4. Saturn rings + baked ring-shadow band.
 */
object PlanetRenderer {

    private val atmosphereColors = mapOf(
        CelestialBody.SUN     to Color(0x66FFAA33),
        CelestialBody.MERCURY to Color(0x22B0BEC5),
        CelestialBody.VENUS   to Color(0x44FFE082),
        CelestialBody.EARTH   to Color(0x554FC3F7),
        CelestialBody.MOON    to Color(0x22CFD8DC),
        CelestialBody.MARS    to Color(0x44FF8A65),
        CelestialBody.JUPITER to Color(0x33FFCC80),
        CelestialBody.SATURN  to Color(0x33FFE0B2),
        CelestialBody.URANUS  to Color(0x4480DEEA),
        CelestialBody.NEPTUNE to Color(0x44448AFF)
    )

    fun DrawScope.drawLitPlanet(
        body: CelestialBody,
        bitmap: ImageBitmap,
        earthNightBitmap: ImageBitmap?,
        center: Offset,
        radius: Float,
        sunDirectionRad: Double,
        clipPathReusable: Path,
        drawAtmosphere: Boolean = true
    ) {
        val diameter = (radius * 2f).roundToInt().coerceAtLeast(2)
        val topLeft = IntOffset((center.x - radius).roundToInt(), (center.y - radius).roundToInt())

        // 1. Soft atmosphere / corona glow
        if (drawAtmosphere) {
            val glowColor = atmosphereColors[body] ?: Color(0x33FFFFFF)
            val glowRadius = if (body == CelestialBody.SUN) radius * 2.6f else radius * 1.28f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = center,
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = center
            )
        }

        // 2. Saturn back ring half
        if (body == CelestialBody.SATURN) {
            drawSaturnRings(center, radius)
        }

        // 3. Clipped globe + shader-free directional lighting
        clipPathReusable.reset()
        clipPathReusable.addOval(Rect(center.x - radius, center.y - radius, center.x + radius, center.y + radius))

        clipPath(clipPathReusable) {
            // Base day texture
            drawImage(
                image = bitmap,
                dstOffset = topLeft,
                dstSize = IntSize(diameter, diameter)
            )

            if (body != CelestialBody.SUN) {
                // Sun direction vector: light comes FROM sunDirectionRad
                val deg = Math.toDegrees(sunDirectionRad).toFloat()

                // Earth night lights on the dark hemisphere (additive blend masked by shadow)
                if (body == CelestialBody.EARTH && earthNightBitmap != null) {
                    rotate(degrees = deg, pivot = center) {
                        // Draw night lights subtly over the dark side
                        drawImage(
                            image = earthNightBitmap,
                            dstOffset = topLeft,
                            dstSize = IntSize(diameter, diameter),
                            alpha = 0.85f,
                            blendMode = BlendMode.Screen
                        )
                    }
                }

                // Soft terminator shadow rotated to the real Sun angle
                rotate(degrees = deg, pivot = center) {
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0.00f to Color.Transparent,
                            0.42f to Color.Transparent,
                            0.56f to Color(0x99000000),
                            0.74f to Color(0xE8000000),
                            1.00f to Color(0xF5000000),
                            startX = center.x - radius,
                            endX = center.x + radius
                        ),
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2f, radius * 2f)
                    )
                }

                // Saturn baked ring-shadow band across the globe
                if (body == CelestialBody.SATURN) {
                    rotate(degrees = -18f, pivot = center) {
                        drawRect(
                            color = Color(0x66000000),
                            topLeft = Offset(center.x - radius, center.y + radius * 0.12f),
                            size = Size(radius * 2f, radius * 0.16f)
                        )
                    }
                }
            }
        }
    }

    private fun DrawScope.drawSaturnRings(center: Offset, radius: Float) {
        rotate(degrees = -18f, pivot = center) {
            val rx = radius * 2.15f
            val ry = radius * 0.58f
            drawOval(
                color = Color(0x88D7C098),
                topLeft = Offset(center.x - rx, center.y - ry),
                size = Size(rx * 2f, ry * 2f),
                style = Stroke(width = radius * 0.24f)
            )
            val rxInner = radius * 1.65f
            val ryInner = radius * 0.44f
            drawOval(
                color = Color(0x55B89E72),
                topLeft = Offset(center.x - rxInner, center.y - ryInner),
                size = Size(rxInner * 2f, ryInner * 2f),
                style = Stroke(width = radius * 0.12f)
            )
        }
    }
}
