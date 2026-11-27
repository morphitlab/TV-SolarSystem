package com.solarsystemlive.data

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Pure-Kotlin offline orbit engine (zero Android dependencies, unit-testable).
 * Uses JPL J2000.0 approximate Keplerian elements for the 8 planets and
 * Meeus's simplified lunar longitude model for the Moon.
 *
 * Accuracy: within ~0.5°–1.5° for planets and ~1° for the Moon across 1900–2100,
 * which is indistinguishable to the eye on a 4K top-down solar system map.
 */
object OrbitEngine {

    private const val DEG2RAD = PI / 180.0
    private const val TWO_PI = 2.0 * PI

    private data class OrbitalElements(
        val a0: Double, val aDot: Double,     // Semi-major axis (AU, AU/century)
        val e0: Double, val eDot: Double,     // Eccentricity
        val l0: Double, val lDot: Double,     // Mean longitude (deg, deg/century)
        val wBar0: Double, val wBarDot: Double // Longitude of perihelion (deg, deg/century)
    )

    // JPL Standish (1992) Table 1: 1800 AD - 2050 AD
    private val planetElements = mapOf(
        CelestialBody.MERCURY to OrbitalElements(0.38709927, 0.00000037, 0.20563593, 0.00001906, 252.25032350, 149472.67411175, 77.45779628, 0.16047689),
        CelestialBody.VENUS   to OrbitalElements(0.72333566, 0.00000390, 0.00677672, -0.00004107, 181.97909950, 58517.81538729, 131.60246718, 0.00268329),
        CelestialBody.EARTH   to OrbitalElements(1.00000261, 0.00000562, 0.01671123, -0.00004392, 100.46457166, 35999.37244981, 102.93768193, 0.32327364),
        CelestialBody.MARS    to OrbitalElements(1.52371034, 0.00001847, 0.09339410, 0.00007882, -4.55343205, 19140.30268499, -23.94362959, 0.44441088),
        CelestialBody.JUPITER to OrbitalElements(5.20288700, -0.00011607, 0.04838624, -0.00013253, 34.39644051, 3034.74612775, 14.72847983, 0.21252668),
        CelestialBody.SATURN  to OrbitalElements(9.53667594, -0.00125060, 0.05386179, -0.00050991, 49.95424423, 1222.49362201, 92.59887831, -0.41897216),
        CelestialBody.URANUS  to OrbitalElements(19.18916464, -0.00196176, 0.04725744, -0.00004397, 313.23810451, 428.48202785, 170.95427630, 0.40805281),
        CelestialBody.NEPTUNE to OrbitalElements(30.06992276, 0.00026291, 0.00859048, 0.00005105, -55.12002969, 218.45945325, 44.96476227, -0.32241464)
    )

    /** Convert Unix epoch milliseconds to Julian Date. */
    fun epochMillisToJulianDate(epochMillis: Long): Double {
        return epochMillis / 86_400_000.0 + 2440587.5
    }

    /** Compute all body positions for a given Unix epoch millisecond timestamp. */
    fun computeAllPositions(epochMillis: Long): Map<CelestialBody, BodyPosition> {
        val jd = epochMillisToJulianDate(epochMillis)
        val t = (jd - 2451545.0) / 36525.0 // Julian centuries since J2000.0

        val result = LinkedHashMap<CelestialBody, BodyPosition>(10)
        result[CelestialBody.SUN] = BodyPosition(
            body = CelestialBody.SUN,
            heliocentricLongitudeRad = 0.0,
            distanceAu = 0.0,
            sunDirectionRad = 0.0
        )

        var earthLon = 0.0
        for ((body, el) in planetElements) {
            val a = el.a0 + el.aDot * t
            val e = el.e0 + el.eDot * t
            val l = normalizeDeg(el.l0 + el.lDot * t)
            val wBar = normalizeDeg(el.wBar0 + el.wBarDot * t)
            val mRad = normalizeDeg(l - wBar) * DEG2RAD

            // Solve Kepler's equation E - e*sin(E) = M via Newton-Raphson (5 iterations)
            var eccAnomaly = mRad
            for (i in 0 until 5) {
                eccAnomaly -= (eccAnomaly - e * sin(eccAnomaly) - mRad) / (1.0 - e * cos(eccAnomaly))
            }

            // True anomaly & heliocentric distance
            val xv = a * (cos(eccAnomaly) - e)
            val yv = a * sqrt(1.0 - e * e) * sin(eccAnomaly)
            val trueAnomaly = atan2(yv, xv)
            val r = sqrt(xv * xv + yv * yv)
            val lonRad = normalizeRad(trueAnomaly + wBar * DEG2RAD)

            if (body == CelestialBody.EARTH) {
                earthLon = lonRad
            }

            // Vector from planet toward the Sun is opposite its heliocentric longitude
            val sunDir = normalizeRad(lonRad + PI)

            result[body] = BodyPosition(
                body = body,
                heliocentricLongitudeRad = lonRad,
                distanceAu = r,
                sunDirectionRad = sunDir
            )
        }

        // Moon geocentric ecliptic longitude (Meeus simplified model)
        val dDays = jd - 2451545.0
        val moonMeanLon = normalizeDeg(218.316 + 13.176396 * dDays)
        val moonMeanAnomaly = normalizeDeg(134.963 + 13.064993 * dDays) * DEG2RAD
        val moonLonDeg = normalizeDeg(moonMeanLon + 6.289 * sin(moonMeanAnomaly))
        val moonLonRad = moonLonDeg * DEG2RAD

        // Sun's geocentric longitude is opposite Earth's heliocentric longitude
        val sunGeoLonRad = normalizeRad(earthLon + PI)
        val phaseAngle = normalizeRad(moonLonRad - sunGeoLonRad)
        val phaseFraction = phaseAngle / TWO_PI

        result[CelestialBody.MOON] = BodyPosition(
            body = CelestialBody.MOON,
            heliocentricLongitudeRad = moonLonRad,
            distanceAu = 0.00257, // ~384,400 km in AU
            sunDirectionRad = sunGeoLonRad,
            moonPhaseFraction = phaseFraction
        )

        return result
    }

    private fun normalizeDeg(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        return d
    }

    private fun normalizeRad(rad: Double): Double {
        var r = rad % TWO_PI
        if (r < 0) r += TWO_PI
        return r
    }
}
