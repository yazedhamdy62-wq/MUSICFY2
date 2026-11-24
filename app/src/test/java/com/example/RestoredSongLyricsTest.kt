package com.example

import com.example.data.Song
import com.example.data.local.refreshedCurrentSong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Regression: lyrics saved for a song must show on the player's restored current song. */
class RestoredSongLyricsTest {
    private val scanned = Song(id = "device_42", title = "Flower In A Hurricane", artist = "Justice", durationSec = 219)

    @Test
    fun stubRestoredFromPreferencesGetsStoredLyrics() {
        val stub = Song(id = "device_42", title = "Flower In A Hurricane", artist = "Justice", durationSec = 219)
        val library = listOf(scanned.copy(rawLyricsText = "Only love never hesitates"))
        val refreshed = refreshedCurrentSong(stub, library)
        assertEquals("Only love never hesitates", refreshed?.rawLyricsText)
    }

    @Test
    fun noRefreshWhenAlreadyUpToDateOrSongMissing() {
        assertNull(refreshedCurrentSong(scanned, listOf(scanned)))
        assertNull(refreshedCurrentSong(scanned, emptyList()))
    }
}
