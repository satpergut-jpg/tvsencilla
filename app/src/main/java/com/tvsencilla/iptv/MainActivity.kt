package com.tvsencilla.iptv

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import com.tvsencilla.iptv.ui.navigation.AppNavHost
import com.tvsencilla.iptv.ui.remote.RemoteKey
import com.tvsencilla.iptv.ui.remote.RemoteKeyBus
import com.tvsencilla.iptv.ui.theme.TvSencillaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var remoteKeyBus: RemoteKeyBus

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()

            TvSencillaTheme(
                fontSize = state.settings.fontSize,
                subtitleSize = state.settings.subtitleSize,
            ) {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                    // Deciding the start destination before the stored source is known would send
                    // a configured box back to the setup screen on every launch.
                    if (state.isReady) {
                        AppNavHost(
                            remoteKeyBus = remoteKeyBus,
                            startOnSetup = state.needsSetup,
                            onExitApp = { finish() },
                        )
                    }
                }
            }
        }
    }

    /**
     * Number keys, CH+/CH- and the media keys must work whatever happens to hold the focus, so
     * they are taken here and offered to whichever screen is listening. Anything nobody claims
     * falls through to the system, which is what keeps D-pad navigation and text entry intact.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val key = keyCode.toRemoteKey()
        if (key != null && remoteKeyBus.dispatch(key)) return true
        return super.onKeyDown(keyCode, event)
    }

    private fun Int.toRemoteKey(): RemoteKey? = when (this) {
        KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_NUMPAD_0 -> RemoteKey.Digit(0)
        KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_NUMPAD_1 -> RemoteKey.Digit(1)
        KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_NUMPAD_2 -> RemoteKey.Digit(2)
        KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_NUMPAD_3 -> RemoteKey.Digit(3)
        KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_NUMPAD_4 -> RemoteKey.Digit(4)
        KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_NUMPAD_5 -> RemoteKey.Digit(5)
        KeyEvent.KEYCODE_6, KeyEvent.KEYCODE_NUMPAD_6 -> RemoteKey.Digit(6)
        KeyEvent.KEYCODE_7, KeyEvent.KEYCODE_NUMPAD_7 -> RemoteKey.Digit(7)
        KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_NUMPAD_8 -> RemoteKey.Digit(8)
        KeyEvent.KEYCODE_9, KeyEvent.KEYCODE_NUMPAD_9 -> RemoteKey.Digit(9)
        KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_PAGE_UP -> RemoteKey.ChannelUp
        KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_PAGE_DOWN -> RemoteKey.ChannelDown
        KeyEvent.KEYCODE_INFO, KeyEvent.KEYCODE_GUIDE -> RemoteKey.Info
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK -> RemoteKey.PlayPause
        KeyEvent.KEYCODE_MEDIA_PLAY -> RemoteKey.Play
        KeyEvent.KEYCODE_MEDIA_PAUSE -> RemoteKey.Pause
        KeyEvent.KEYCODE_MEDIA_FAST_FORWARD, KeyEvent.KEYCODE_MEDIA_NEXT -> RemoteKey.FastForward
        KeyEvent.KEYCODE_MEDIA_REWIND, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> RemoteKey.Rewind
        else -> null
    }
}
