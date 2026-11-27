package com.solarsystemlive.ui.overview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.solarsystemlive.data.CelestialBody
import com.solarsystemlive.data.OrbitEngine
import com.solarsystemlive.data.TimeController
import com.solarsystemlive.render.PlanetRenderer.drawLitPlanet
import com.solarsystemlive.ui.common.Starfield
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun OverviewScreen(
    timeController: TimeController,
    overviewBitmaps: Map<CelestialBody, ImageBitmap>,
    focusedBody: CelestialBody,
    highQuality: Boolean,
    onToggleQuality: () -> Unit,
    modifier: Modifier = Modifier
) {
    val starfield = remember { Starfield(count = if (highQuality) 200 else 90) }
    val reusablePath = remember { Path() }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }

    // Frame-driven clock state read inside draw phase (no recomposition per frame)
    var frameEpochMillis by remember { mutableLongStateOf(timeController.simulationEpochMillis) }
    var elapsedSec by remember { mutableStateOf(0f) }
    var displayedDate by remember { mutableStateOf(dateFormatter.format(Date(frameEpochMillis))) }

    LaunchedEffect(Unit) {
        var lastNanos = 0L
        var lastDateUpdate = 0L
        while (true) {
            withFrameNanos { nowNanos ->
                if (lastNanos != 0L) {
                    val dt = (nowNanos - lastNanos) / 1_000_000_000.0
                    timeController.tick(dt)
                    elapsedSec += dt.toFloat()
                    frameEpochMillis = timeController.simulationEpochMillis
                    if (nowNanos - lastDateUpdate > 150_000_000L) {
                        displayedDate = dateFormatter.format(Date(frameEpochMillis))
                        lastDateUpdate = nowNanos
                    }
                }
                lastNanos = nowNanos
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .drawWithCache {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val maxOrbitRadius = min(size.width, size.height) * 0.45f
                val ringStep = maxOrbitRadius / 8.5f
                val basePlanetUnit = min(size.width, size.height) * 0.017f

                onDrawBehind {
                    // 1. Twinkling stars
                    with(starfield) { draw(elapsedSec) }

                    // 2. Evenly spaced concentric orbit rings
                    for (i in 1..8) {
                        drawCircle(
                            color = Color(0x26FFFFFF),
                            radius = ringStep * (i + 0.4f),
                            center = Offset(cx, cy),
                            style = Stroke(width = 1.2f)
                        )
                    }

                    // 3. Real planetary positions for current simulation epoch
                    val positions = OrbitEngine.computeAllPositions(frameEpochMillis)
                    var earthCenter = Offset(cx, cy)

                    // Draw Sun + 8 planets
                    for (body in CelestialBody.entries) {
                        if (body == CelestialBody.MOON) continue
                        val pos = positions[body] ?: continue
                        val bmp = overviewBitmaps[body] ?: continue

                        val center = if (body == CelestialBody.SUN) {
                            Offset(cx, cy)
                        } else {
                            val r = ringStep * (body.orbitIndex + 0.4f)
                            val x = cx + (r * cos(pos.heliocentricLongitudeRad)).toFloat()
                            val y = cy - (r * sin(pos.heliocentricLongitudeRad)).toFloat()
                            Offset(x, y)
                        }
                        if (body == CelestialBody.EARTH) {
                            earthCenter = center
                        }

                        val visualRadius = basePlanetUnit * body.visualRadiusFraction
                        drawLitPlanet(
                            body = body,
                            bitmap = bmp,
                            earthNightBitmap = null,
                            center = center,
                            radius = visualRadius,
                            sunDirectionRad = pos.sunDirectionRad,
                            clipPathReusable = reusablePath,
                            drawAtmosphere = highQuality || body == CelestialBody.SUN
                        )

                        // Focus ring on currently selected body
                        if (body == focusedBody) {
                            drawCircle(
                                color = Color(0xEE80D8FF),
                                radius = visualRadius + 8f,
                                center = center,
                                style = Stroke(width = 3f)
                            )
                        }
                    }

                    // 4. Moon orbiting Earth
                    val moonPos = positions[CelestialBody.MOON]
                    val moonBmp = overviewBitmaps[CelestialBody.MOON]
                    if (moonPos != null && moonBmp != null) {
                        val moonOrbitR = basePlanetUnit * 2.1f
                        drawCircle(
                            color = Color(0x33FFFFFF),
                            radius = moonOrbitR,
                            center = earthCenter,
                            style = Stroke(width = 1f)
                        )
                        val mx = earthCenter.x + (moonOrbitR * cos(moonPos.heliocentricLongitudeRad)).toFloat()
                        val my = earthCenter.y - (moonOrbitR * sin(moonPos.heliocentricLongitudeRad)).toFloat()
                        val moonCenter = Offset(mx, my)
                        val moonR = basePlanetUnit * CelestialBody.MOON.visualRadiusFraction
                        drawLitPlanet(
                            body = CelestialBody.MOON,
                            bitmap = moonBmp,
                            earthNightBitmap = null,
                            center = moonCenter,
                            radius = moonR,
                            sunDirectionRad = moonPos.sunDirectionRad,
                            clipPathReusable = reusablePath,
                            drawAtmosphere = false
                        )
                        if (focusedBody == CelestialBody.MOON) {
                            drawCircle(
                                color = Color(0xEE80D8FF),
                                radius = moonR + 7f,
                                center = moonCenter,
                                style = Stroke(width = 3f)
                            )
                        }
                    }
                }
            }
            // Overscan-safe margins (5% of 1080p base)
            .padding(horizontal = 48.dp, vertical = 27.dp)
    ) {
        // Top-left: Focused body label
        Text(
            text = focusedBody.displayName.uppercase(),
            color = Color.White,
            fontSize = 28.sp,
            modifier = Modifier.align(Alignment.TopStart)
        )

        // Top-right: Date + Speed readout
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .background(Color(0x66000000), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = displayedDate, color = Color.White, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = timeController.speedLabel(), color = Color(0xFF80D8FF), fontSize = 22.sp)
        }

        // Bottom-left: Remote hint strip (10-foot UI)
        Row(
            modifier = Modifier.align(Alignment.BottomStart),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(text = "◀ ▶ Select Body", color = Color(0xFFB0BEC5), fontSize = 20.sp)
            Text(text = "OK Inspect", color = Color(0xFFB0BEC5), fontSize = 20.sp)
            Text(text = "⏪ ⏯ ⏩ Time", color = Color(0xFFB0BEC5), fontSize = 20.sp)
            Text(text = "▲ Live  ▼ Quality: ${if (highQuality) "High" else "Low"}", color = Color(0xFFB0BEC5), fontSize = 20.sp)
        }
    }
}
