package com.solarsystemlive.ui.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.solarsystemlive.data.BodyFacts
import com.solarsystemlive.data.CelestialBody
import com.solarsystemlive.data.OrbitEngine
import com.solarsystemlive.data.TimeController
import com.solarsystemlive.render.PlanetRenderer.drawLitPlanet
import com.solarsystemlive.render.TextureStore
import com.solarsystemlive.ui.common.Starfield
import kotlin.math.min

@Composable
fun DetailScreen(
    body: CelestialBody,
    facts: BodyFacts,
    factIndex: Int,
    timeController: TimeController,
    textureStore: TextureStore,
    highQuality: Boolean,
    modifier: Modifier = Modifier
) {
    var detailBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var earthNightBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var entered by remember { mutableStateOf(false) }

    // ~500 ms zoom-in transition
    val zoomScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.35f,
        animationSpec = tween(durationMillis = 500),
        label = "detailZoom"
    )

    LaunchedEffect(body, highQuality) {
        entered = true
        detailBitmap = textureStore.getDetailBitmap(body, highQuality)
        earthNightBitmap = if (body == CelestialBody.EARTH) {
            textureStore.getEarthNightBitmap(highQuality)
        } else null
        textureStore.preloadNeighbors(body, highQuality)
    }

    val positions = remember(timeController.simulationEpochMillis, body) {
        OrbitEngine.computeAllPositions(timeController.simulationEpochMillis)
    }
    val pos = positions[body]
    val liveDistanceText = if (body == CelestialBody.SUN) {
        "0.00 AU"
    } else if (body == CelestialBody.MOON) {
        "384,400 km (from Earth)"
    } else {
        String.format("%.2f AU", pos?.distanceAu ?: 0.0)
    }

    val starfield = remember { Starfield(count = 120, seed = 99) }
    val reusablePath = remember { Path() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 48.dp, vertical = 27.dp)
    ) {
        // Background stars
        Canvas(modifier = Modifier.fillMaxSize()) {
            with(starfield) { draw(timeSec = 1f, alphaScale = 0.5f) }
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left half: Large lit planet
            Box(
                modifier = Modifier
                    .weight(0.48f)
                    .fillMaxHeight()
                    .graphicsLayer(scaleX = zoomScale, scaleY = zoomScale),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val bmp = detailBitmap ?: return@Canvas
                    val radius = min(size.width, size.height) * 0.38f
                    drawLitPlanet(
                        body = body,
                        bitmap = bmp,
                        earthNightBitmap = earthNightBitmap,
                        center = Offset(size.width / 2f, size.height / 2f),
                        radius = radius,
                        sunDirectionRad = pos?.sunDirectionRad ?: 0.0,
                        clipPathReusable = reusablePath,
                        drawAtmosphere = true
                    )
                }
            }

            // Right half: Title, Stats Strip, Cross-fading Facts
            Column(
                modifier = Modifier
                    .weight(0.52f)
                    .fillMaxHeight()
                    .padding(start = 24.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = body.displayName,
                    color = Color.White,
                    fontSize = 48.sp
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Stats strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatItem("Diameter", facts.diameter)
                    StatItem("Day", facts.dayLength)
                    StatItem("Year", facts.yearLength)
                    StatItem("Moons", facts.moons)
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Live Distance: $liveDistanceText",
                    color = Color(0xFF80D8FF),
                    fontSize = 24.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Cross-fading fact card (OK cycles to next fact)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(14.dp))
                        .padding(24.dp)
                ) {
                    AnimatedContent(
                        targetState = factIndex,
                        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
                        label = "factCrossfade"
                    ) { idx ->
                        val text = facts.facts.getOrElse(idx) { "" }
                        Column {
                            Text(
                                text = "FACT ${idx + 1} OF ${facts.facts.size}",
                                color = Color(0xFF90CAF9),
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = text,
                                color = Color.White,
                                fontSize = 26.sp,
                                lineHeight = 34.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "OK Next Fact   •   ◀ ▶ Switch Body   •   BACK Overview",
                    color = Color(0xFFB0BEC5),
                    fontSize = 20.sp
                )
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column {
        Text(text = label.uppercase(), color = Color(0xFFB0BEC5), fontSize = 15.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = value, color = Color.White, fontSize = 20.sp)
    }
}
