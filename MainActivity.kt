package com.solarsystemlive

import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.solarsystemlive.data.CelestialBody
import com.solarsystemlive.data.FactCycler
import com.solarsystemlive.data.FactsRepository
import com.solarsystemlive.data.TimeController
import com.solarsystemlive.render.TextureStore
import com.solarsystemlive.ui.detail.DetailScreen
import com.solarsystemlive.ui.overview.OverviewScreen

class MainActivity : ComponentActivity() {

    private val timeController = TimeController()
    private lateinit var factsRepository: FactsRepository
    private lateinit var textureStore: TextureStore

    private var focusedIndex by mutableIntStateOf(3) // Start focused on Earth
    private var inDetailView by mutableStateOf(false)
    private var highQuality by mutableStateOf(true)
    private var factIndex by mutableIntStateOf(0)
    private var factCycler = FactCycler(12)

    private val focusedBody: CelestialBody
        get() = CelestialBody.navigationOrder[focusedIndex]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        factsRepository = FactsRepository(this)
        textureStore = TextureStore(this)
        val overviewBitmaps = textureStore.preloadOverview()

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                if (inDetailView) {
                    val facts = factsRepository.getFacts(focusedBody)
                    DetailScreen(
                        body = focusedBody,
                        facts = facts,
                        factIndex = factIndex,
                        timeController = timeController,
                        textureStore = textureStore,
                        highQuality = highQuality
                    )
                } else {
                    OverviewScreen(
                        timeController = timeController,
                        overviewBitmaps = overviewBitmaps,
                        focusedBody = focusedBody,
                        highQuality = highQuality,
                        onToggleQuality = { highQuality = !highQuality }
                    )
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val navSize = CelestialBody.navigationOrder.size
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                focusedIndex = (focusedIndex + 1) % navSize
                resetFactsForCurrentBody()
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                focusedIndex = (focusedIndex - 1 + navSize) % navSize
                resetFactsForCurrentBody()
                true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                if (!inDetailView) {
                    resetFactsForCurrentBody()
                    inDetailView = true
                } else {
                    factIndex = factCycler.next()
                }
                true
            }
            KeyEvent.KEYCODE_BACK -> {
                if (inDetailView) {
                    inDetailView = false
                    true
                } else {
                    super.onKeyDown(keyCode, event)
                }
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                timeController.togglePlayPause()
                true
            }
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                timeController.fastForward()
                true
            }
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                timeController.rewind()
                true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (!inDetailView) {
                    timeController.resetToLive()
                    true
                } else false
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (!inDetailView) {
                    highQuality = !highQuality
                    true
                } else false
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    private fun resetFactsForCurrentBody() {
        val count = factsRepository.getFacts(focusedBody).facts.size
        factCycler = FactCycler(count)
        factIndex = 0
    }
}
