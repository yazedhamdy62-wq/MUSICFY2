package com.example

import com.example.data.Song
import com.example.data.local.resolveRestoredQueue
import org.junit.Assert.assertEquals
import org.junit.Test

/** Regression: after reopening, the saved queue (library or playlist order) must be restored. */
class RestoredQueueTest {
    private fun song(n: Int) = Song(id = "device_$n", title = "Song $n", artist = "A", durationSec = 100)
    private val library = (1..6).map { song(it) }

    @Test
    fun playlistOrderIsRestoredAsSaved() {
        val saved = listOf("device_4", "device_2", "device_6")
        val q = resolveRestoredQueue(saved, library[1], library)
        assertEquals(listOf("device_4", "device_2", "device_6"), q.map { it.id })
    }

    @Test
    fun songsMissingFromLibraryAreSkipped() {
        val saved = listOf("device_3", "device_99", "device_5")
        val q = resolveRestoredQueue(saved, library[2], library)
        assertEquals(listOf("device_3", "device_5"), q.map { it.id })
    }

    @Test
    fun currentSongIsAddedWhenSavedQueueLacksIt() {
        val saved = listOf("device_2", "device_3")
        val q = resolveRestoredQueue(saved, library[0], library)
        assertEquals(listOf("device_1", "device_2", "device_3"), q.map { it.id })
    }

    @Test
    fun currentSongMissingFromLibraryKeepsItsSavedPosition() {
        val gone = song(9)
        val q = resolveRestoredQueue(listOf("device_4", "device_9", "device_2"), gone, library)
        assertEquals(listOf("device_4", "device_9", "device_2"), q.map { it.id })
    }

    @Test
    fun oneSongQueueStaysOneSongAndNothingIsAddedFromLibrary() {
        assertEquals(listOf("device_3"), resolveRestoredQueue(listOf("device_3"), library[2], library).map { it.id })
        assertEquals(listOf("device_1"), resolveRestoredQueue(emptyList(), library[0], library).map { it.id })
    }

    @Test
    fun emptyLibraryKeepsCurrentSong() {
        assertEquals(listOf("device_1"), resolveRestoredQueue(listOf("device_9"), library[0], emptyList()).map { it.id })
    }
}
