package com.tvsencilla.iptv.domain.dialer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelDialerTest {

    @Test
    fun `one digit tunes once the two second wait has passed`() = runTest {
        val commits = mutableListOf<Int>()
        val dialer = ChannelDialer(backgroundScope)
        backgroundScope.launch { dialer.commits.collect { commits += it } }
        runCurrent()

        dialer.onDigit(5)
        assertEquals("5", dialer.typed.value)

        advanceTimeBy(1_900)
        runCurrent()
        assertTrue("must still be waiting for a second digit", commits.isEmpty())

        advanceTimeBy(200)
        runCurrent()
        assertEquals(listOf(5), commits)
        assertNull(dialer.typed.value)
    }

    @Test
    fun `a second digit within the window makes a two digit number`() = runTest {
        val commits = mutableListOf<Int>()
        val dialer = ChannelDialer(backgroundScope)
        backgroundScope.launch { dialer.commits.collect { commits += it } }
        runCurrent()

        dialer.onDigit(1)
        advanceTimeBy(1_000)
        runCurrent()
        dialer.onDigit(2)
        assertEquals("12", dialer.typed.value)

        // The wait restarts with every digit.
        advanceTimeBy(1_900)
        runCurrent()
        assertTrue(commits.isEmpty())

        advanceTimeBy(200)
        runCurrent()
        assertEquals(listOf(12), commits)
    }

    @Test
    fun `the third digit tunes immediately without waiting`() = runTest {
        val commits = mutableListOf<Int>()
        val dialer = ChannelDialer(backgroundScope)
        backgroundScope.launch { dialer.commits.collect { commits += it } }
        runCurrent()

        dialer.onDigit(1)
        dialer.onDigit(2)
        dialer.onDigit(5)
        runCurrent()

        assertEquals(listOf(125), commits)
        assertNull(dialer.typed.value)
    }

    @Test
    fun `OK tunes straight away`() = runTest {
        val commits = mutableListOf<Int>()
        val dialer = ChannelDialer(backgroundScope)
        backgroundScope.launch { dialer.commits.collect { commits += it } }
        runCurrent()

        dialer.onDigit(7)
        assertTrue(dialer.onConfirm())
        runCurrent()

        assertEquals(listOf(7), commits)
    }

    @Test
    fun `OK does nothing when no digits have been typed`() = runTest {
        val dialer = ChannelDialer(backgroundScope)
        assertFalse(dialer.onConfirm())
    }

    @Test
    fun `Back abandons the number and nothing is tuned`() = runTest {
        val commits = mutableListOf<Int>()
        val dialer = ChannelDialer(backgroundScope)
        backgroundScope.launch { dialer.commits.collect { commits += it } }
        runCurrent()

        dialer.onDigit(4)
        assertTrue(dialer.onCancel())
        assertNull(dialer.typed.value)

        advanceTimeBy(5_000)
        runCurrent()
        assertTrue("a cancelled number must never tune", commits.isEmpty())
    }

    @Test
    fun `a leading zero is kept so 0 and 05 stay distinct`() = runTest {
        val commits = mutableListOf<Int>()
        val dialer = ChannelDialer(backgroundScope)
        backgroundScope.launch { dialer.commits.collect { commits += it } }
        runCurrent()

        dialer.onDigit(0)
        dialer.onDigit(5)
        assertEquals("05", dialer.typed.value)
        dialer.onConfirm()
        runCurrent()

        assertEquals(listOf(5), commits)
    }

    @Test
    fun `dialling state reports correctly`() = runTest {
        val dialer = ChannelDialer(backgroundScope)
        assertFalse(dialer.isDialling)
        dialer.onDigit(3)
        assertTrue(dialer.isDialling)
        dialer.onCancel()
        assertFalse(dialer.isDialling)
    }
}
