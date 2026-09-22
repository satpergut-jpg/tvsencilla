package com.tvsencilla.iptv.domain.channel

import com.tvsencilla.iptv.domain.model.Channel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelQualityGroupingTest {

    private fun channel(
        name: String,
        listNumber: Int,
        categoryId: String? = "sports",
    ) = Channel(
        id = name,
        name = name,
        streamUrl = "http://test/$name",
        categoryId = categoryId,
        listNumber = listNumber,
    )

    @Test
    fun `versions that only differ in quality collapse into one channel`() {
        val channels = listOf(
            channel("Antena 3 FHD", listNumber = 1),
            channel("Antena 3 HD", listNumber = 2),
            channel("Antena 3 SD", listNumber = 3),
        )

        val grouped = ChannelQualityGrouping.group(channels)

        assertEquals(1, grouped.size)
    }

    @Test
    fun `the HD version is picked by default when there is one`() {
        val channels = listOf(
            channel("Antena 3 FHD", listNumber = 1),
            channel("Antena 3 HD", listNumber = 2),
            channel("Antena 3 SD", listNumber = 3),
        )

        val merged = ChannelQualityGrouping.group(channels).single()

        assertEquals("http://test/Antena 3 HD", merged.streamUrl)
        assertEquals("HD", merged.qualityOptions.first().label)
        assertEquals(setOf("HD", "FHD", "SD"), merged.qualityOptions.map { it.label }.toSet())
    }

    @Test
    fun `the best available quality is picked when there is no HD version`() {
        val channels = listOf(
            channel("Antena 3 FHD", listNumber = 1),
            channel("Antena 3 SD", listNumber = 2),
        )

        val merged = ChannelQualityGrouping.group(channels).single()

        assertEquals("http://test/Antena 3 FHD", merged.streamUrl)
    }

    @Test
    fun `an unlabelled version counts as HD`() {
        val channels = listOf(
            channel("Antena 3", listNumber = 1),
            channel("Antena 3 SD", listNumber = 2),
        )

        val merged = ChannelQualityGrouping.group(channels).single()

        assertEquals("http://test/Antena 3", merged.streamUrl)
    }

    @Test
    fun `a channel with a single version has no quality options`() {
        val merged = ChannelQualityGrouping.group(listOf(channel("La 1", listNumber = 1))).single()

        assertTrue(merged.qualityOptions.isEmpty())
        assertNull(merged.qualityOptions.firstOrNull())
    }

    @Test
    fun `channels that only share a name across categories are not merged`() {
        val channels = listOf(
            channel("Antena 3 HD", listNumber = 1, categoryId = "sports"),
            channel("Antena 3 HD", listNumber = 2, categoryId = "news"),
        )

        assertEquals(2, ChannelQualityGrouping.group(channels).size)
    }

    @Test
    fun `channels stay numbered from 1 after merging`() {
        val channels = listOf(
            channel("Antena 3 FHD", listNumber = 1),
            channel("Antena 3 HD", listNumber = 2),
            channel("La 1", listNumber = 3),
        )

        val grouped = ChannelQualityGrouping.group(channels).sortedBy { it.listNumber }

        assertEquals(listOf(1, 2), grouped.map { it.listNumber })
    }
}
