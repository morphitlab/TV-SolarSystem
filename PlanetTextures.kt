package com.solarsystemlive.render

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.solarsystemlive.R
import com.solarsystemlive.data.CelestialBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Single-file texture registry.
 * To swap in real NASA / Solar System Scope textures (see ASSETS.md),
 * simply replace the drawable files in res/drawable-nodpi/ with the exact same names.
 */
object PlanetTextures {

    @DrawableRes
    fun drawableFor(body: CelestialBody): Int = when (body) {
        CelestialBody.SUN     -> R.drawable.planet_sun
        CelestialBody.MERCURY -> R.drawable.planet_mercury
        CelestialBody.VENUS   -> R.drawable.planet_venus
        CelestialBody.EARTH   -> R.drawable.planet_earth
        CelestialBody.MOON    -> R.drawable.planet_moon
        CelestialBody.MARS    -> R.drawable.planet_mars
        CelestialBody.JUPITER -> R.drawable.planet_jupiter
        CelestialBody.SATURN  -> R.drawable.planet_saturn
        CelestialBody.URANUS  -> R.drawable.planet_uranus
        CelestialBody.NEPTUNE -> R.drawable.planet_neptune
    }

    @DrawableRes
    val earthNightLightsDrawable: Int = R.drawable.planet_earth_night
}

/**
 * Pre-scales and caches overview bitmaps once, and manages an LRU cache
 * for high-resolution detail bitmaps (current + neighbors).
 */
class TextureStore(private val context: Context) {

    // Small pre-scaled overview bitmaps (128x128 px) for 60 fps zero-alloc drawing
    private val overviewCache = HashMap<CelestialBody, ImageBitmap>(10)

    // Detail bitmaps (512px in Low quality, 1024px in High quality) — keeps max 3 planets in RAM
    private val detailCache = LruCache<String, ImageBitmap>(4)

    fun preloadOverview(): Map<CelestialBody, ImageBitmap> {
        if (overviewCache.isEmpty()) {
            for (body in CelestialBody.entries) {
                overviewCache[body] = decodeScaled(PlanetTextures.drawableFor(body), 128)
            }
        }
        return overviewCache
    }

    suspend fun getDetailBitmap(body: CelestialBody, highQuality: Boolean): ImageBitmap =
        withContext(Dispatchers.IO) {
            val size = if (highQuality) 1024 else 512
            val key = "${body.name}_$size"
            detailCache.get(key) ?: decodeScaled(PlanetTextures.drawableFor(body), size).also {
                detailCache.put(key, it)
            }
        }

    suspend fun getEarthNightBitmap(highQuality: Boolean): ImageBitmap =
        withContext(Dispatchers.IO) {
            val size = if (highQuality) 1024 else 512
            val key = "EARTH_NIGHT_$size"
            detailCache.get(key) ?: decodeScaled(PlanetTextures.earthNightLightsDrawable, size).also {
                detailCache.put(key, it)
            }
        }

    suspend fun preloadNeighbors(body: CelestialBody, highQuality: Boolean) {
        val list = CelestialBody.navigationOrder
        val idx = list.indexOf(body)
        val prev = list[(idx - 1 + list.size) % list.size]
        val next = list[(idx + 1) % list.size]
        getDetailBitmap(prev, highQuality)
        getDetailBitmap(next, highQuality)
    }

    private fun decodeScaled(@DrawableRes resId: Int, targetPx: Int): ImageBitmap {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, resId, opts)
        var sample = 1
        while (opts.outWidth / sample > targetPx * 2 && opts.outHeight / sample > targetPx * 2) {
            sample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val raw = BitmapFactory.decodeResource(context.resources, resId, decodeOpts)
        val scaled = if (raw.width != targetPx || raw.height != targetPx) {
            Bitmap.createScaledBitmap(raw, targetPx, targetPx, true).also {
                if (it != raw) raw.recycle()
            }
        } else {
            raw
        }
        return scaled.asImageBitmap()
    }
}
