package com.tvsencilla.iptv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveFormatMemoryTest {

    private val hls = "http://panel.test/live/u/p/123.m3u8"
    private val ts = "http://panel.test/live/u/p/123.ts"

    @Test
    fun `sin nada aprendido la direccion no cambia`() {
        assertEquals(hls, LiveFormatMemory().adapt(hls))
    }

    @Test
    fun `tras funcionar un canal en TS, los siguientes se piden directamente en TS`() {
        val memory = LiveFormatMemory()
        memory.rememberWorking("http://panel.test/live/u/p/1.ts")

        assertEquals(ts, memory.adapt(hls))
        assertEquals(ts, memory.adapt(ts))
    }

    @Test
    fun `el formato alternativo va en los dos sentidos`() {
        assertEquals(ts, alternateLiveUrl(hls))
        assertEquals(hls, alternateLiveUrl(ts))
    }

    @Test
    fun `las direcciones sin formato conocido no se tocan`() {
        val other = "http://panel.test/streaming/timeshift.php?stream=1"
        val memory = LiveFormatMemory().apply { rememberWorking(ts) }

        assertEquals(other, memory.adapt(other))
        assertNull(alternateLiveUrl(other))
    }
}
