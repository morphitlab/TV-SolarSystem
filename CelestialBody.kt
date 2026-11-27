package com.solarsystemlive.data

enum class CelestialBody(
    val displayName: String,
    val orbitIndex: Int, // 0 = Sun, 1..8 = planets, -1 = Moon (orbits Earth)
    val visualRadiusFraction: Float, // relative to base unit on overview
    val hasRings: Boolean = false
) {
    SUN("Sun", 0, 2.3f),
    MERCURY("Mercury", 1, 0.55f),
    VENUS("Venus", 2, 0.85f),
    EARTH("Earth", 3, 0.95f),
    MOON("Moon", -1, 0.32f),
    MARS("Mars", 4, 0.72f),
    JUPITER("Jupiter", 5, 1.55f),
    SATURN("Saturn", 6, 1.35f, hasRings = true),
    URANUS("Uranus", 7, 1.05f),
    NEPTUNE("Neptune", 8, 1.02f);

    companion object {
        val navigationOrder: List<CelestialBody> = entries
    }
}

data class BodyPosition(
    val body: CelestialBody,
    val heliocentricLongitudeRad: Double, // radians [0, 2*PI)
    val distanceAu: Double,              // AU from Sun (or from Earth for Moon)
    val sunDirectionRad: Double,         // angle from body toward the Sun
    val moonPhaseFraction: Double = 0.0  // 0 = New, 0.5 = Full, 1 = New
)

data class BodyFacts(
    val id: String,
    val diameter: String,
    val dayLength: String,
    val yearLength: String,
    val moons: String,
    val facts: List<String>
)
