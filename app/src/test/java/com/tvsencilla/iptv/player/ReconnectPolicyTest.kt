package com.tvsencilla.iptv.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReconnectPolicyTest {

    @Test
    fun `the wait doubles with every failed attempt`() {
        val policy = ReconnectPolicy(maxAttempts = 5, firstDelayMillis = 1_000L)

        assertEquals(1_000L, policy.nextDelayMillis())
        assertEquals(2_000L, policy.nextDelayMillis())
        assertEquals(4_000L, policy.nextDelayMillis())
        assertEquals(8_000L, policy.nextDelayMillis())
        assertEquals(16_000L, policy.nextDelayMillis())
    }

    @Test
    fun `once the attempts run out the user finally sees an error`() {
        val policy = ReconnectPolicy(maxAttempts = 2)

        policy.nextDelayMillis()
        policy.nextDelayMillis()

        assertTrue(policy.hasGivenUp)
        assertNull(policy.nextDelayMillis())
    }

    @Test
    fun `a stream that recovers resets the backoff`() {
        val policy = ReconnectPolicy(maxAttempts = 3, firstDelayMillis = 1_000L)

        policy.nextDelayMillis()
        policy.nextDelayMillis()
        policy.reset()

        assertFalse(policy.hasGivenUp)
        assertEquals(0, policy.attempts)
        assertEquals(1_000L, policy.nextDelayMillis())
    }
}
