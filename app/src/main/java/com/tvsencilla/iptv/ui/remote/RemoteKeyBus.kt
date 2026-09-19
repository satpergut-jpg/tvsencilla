package com.tvsencilla.iptv.ui.remote

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface RemoteKey {
    data class Digit(val value: Int) : RemoteKey
    data object ChannelUp : RemoteKey
    data object ChannelDown : RemoteKey
    data object Info : RemoteKey
    data object PlayPause : RemoteKey
    data object Play : RemoteKey
    data object Pause : RemoteKey
    data object FastForward : RemoteKey
    data object Rewind : RemoteKey
}

/**
 * Number keys, CH+/CH- and the media keys have to work wherever the focus happens to be, which
 * Compose's per-element key handling cannot do. The activity funnels them through here instead.
 *
 * Keys are only swallowed while a screen is actually listening; otherwise they fall through to the
 * system, so typing a URL into a text field still works.
 */
@Singleton
class RemoteKeyBus @Inject constructor() {

    private val _events = MutableSharedFlow<RemoteKey>(extraBufferCapacity = 16)
    val events: SharedFlow<RemoteKey> = _events.asSharedFlow()

    private val listeners = AtomicInteger(0)

    fun register() {
        listeners.incrementAndGet()
    }

    fun unregister() {
        listeners.decrementAndGet()
    }

    /** Returns true when the key was consumed, which is what the activity reports back. */
    fun dispatch(key: RemoteKey): Boolean {
        if (listeners.get() <= 0) return false
        return _events.tryEmit(key)
    }
}

/** Subscribes the current screen to the remote keys for as long as it is on screen. */
@Composable
fun RemoteKeyHandler(bus: RemoteKeyBus, onKey: (RemoteKey) -> Unit) {
    val handler = rememberUpdatedState(onKey)

    DisposableEffect(bus) {
        bus.register()
        onDispose { bus.unregister() }
    }

    LaunchedEffect(bus) {
        bus.events.collect { key -> handler.value(key) }
    }
}
