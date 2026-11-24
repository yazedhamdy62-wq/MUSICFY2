package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.AudioPlayerController
import com.example.data.LyricLine
import com.example.data.Song
import com.example.data.local.PreferencesManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Drives the real AudioPlayerController (with real SharedPreferences under Robolectric) through
 * the reopen sequence: app closed with a saved queue, cold start, lifecycle persists before the
 * library is scanned, then the library arrives. Audio output itself is not exercised.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class QueueRestoreLifecycleTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private fun song(n: Int, lyrics: Boolean = false) = Song(
        id = "device_$n", title = "Song $n", artist = "A", durationSec = 200,
        lyrics = if (lyrics) listOf(LyricLine(1, "line $n")) else emptyList(),
        rawLyricsText = if (lyrics) "line $n" else null
    )
    private val library = (1..5).map { song(it, lyrics = true) }
    private fun prefs() = PreferencesManager(context)
    private fun savedIds() = prefs().loadPlaybackState().queueSongIds

    private fun saveBeforeClose(current: Int, queue: List<Int>, position: Int = 42) {
        prefs().savePlaybackState(song(current), position, queue.map { "device_$it" })
    }

    @Test
    fun playlistContinuesFromRestoredSongInSavedOrder() {
        saveBeforeClose(current = 3, queue = listOf(5, 3, 1, 2))
        val c = AudioPlayerController(context)
        assertTrue(c.isQueueRestorePending())
        // lifecycle persists and a stub refresh happen before the library is scanned
        c.persistState()
        c.updateCurrentSongData(c.currentSong.value.copy(title = "Song 3"))
        assertEquals(listOf("device_5", "device_3", "device_1", "device_2"), savedIds())

        assertTrue(c.restoreQueueFromLibrary(library))
        assertFalse(c.isQueueRestorePending())
        assertEquals(42, c.currentPositionSec.value)
        c.playNext(autoAdvance = true)
        assertEquals("device_1", c.currentSong.value.id)
        c.playNext(autoAdvance = true)
        assertEquals("device_2", c.currentSong.value.id)
        assertEquals(listOf("device_5", "device_3", "device_1", "device_2"), savedIds())
    }

    @Test
    fun restoredSongKeepsLyricsFromLibrary() {
        saveBeforeClose(current = 2, queue = listOf(1, 2, 3))
        val c = AudioPlayerController(context)
        assertTrue(c.currentSong.value.lyrics.isEmpty())
        c.restoreQueueFromLibrary(library)
        assertEquals("line 2", c.currentSong.value.lyrics.first().text)
        assertEquals("line 2", c.currentSong.value.rawLyricsText)
    }

    @Test
    fun deletedSongsAreSkippedButOrderIsKept() {
        saveBeforeClose(current = 1, queue = listOf(1, 99, 4))
        val c = AudioPlayerController(context)
        c.restoreQueueFromLibrary(library)
        assertEquals(listOf("device_1", "device_4"), savedIds())
        c.playNext(autoAdvance = true)
        assertEquals("device_4", c.currentSong.value.id)
    }

    @Test
    fun missingCurrentSongStillRestoresQueueAndResumesSaving() {
        saveBeforeClose(current = 9, queue = listOf(4, 9, 2))
        val c = AudioPlayerController(context)
        assertTrue(c.restoreQueueFromLibrary(library))
        assertFalse(c.isQueueRestorePending())
        assertEquals("device_9", c.currentSong.value.id)
        assertEquals(listOf("device_4", "device_9", "device_2"), savedIds())
        c.seekTo(10)
        assertEquals(10, prefs().loadPlaybackState().lastPositionSec)
    }

    @Test
    fun oldClobberedOneSongQueueIsNotPaddedWithLibrarySongs() {
        saveBeforeClose(current = 2, queue = listOf(2))
        val c = AudioPlayerController(context)
        c.restoreQueueFromLibrary(library)
        assertEquals(listOf("device_2"), savedIds())
    }

    @Test
    fun savedQueueIsNotOverwrittenWhileRestorePending() {
        saveBeforeClose(current = 2, queue = listOf(1, 2, 3))
        val c = AudioPlayerController(context)
        c.play(); c.pause(); c.seekTo(5); c.toggleShuffle(); c.toggleShuffle()
        c.persistState()
        assertEquals(listOf("device_1", "device_2", "device_3"), savedIds())
    }

    @Test
    fun userPickingASongBeforeLibraryLoadsReplacesPendingRestore() {
        saveBeforeClose(current = 2, queue = listOf(1, 2, 3))
        val c = AudioPlayerController(context)
        val picks = listOf(song(4), song(5))
        c.playSong(picks[0], picks)
        assertFalse(c.isQueueRestorePending())
        assertFalse(c.restoreQueueFromLibrary(library))
        assertEquals(listOf("device_4", "device_5"), savedIds())
    }

    @Test
    fun ordinaryPlaybackStillWorksWithoutAnyRestore() {
        val c = AudioPlayerController(context)
        assertFalse(c.isQueueRestorePending())
        c.playPlaylist(library, startIndex = 1)
        assertEquals("device_2", c.currentSong.value.id)
        c.playNext()
        assertEquals("device_3", c.currentSong.value.id)
        c.playPrevious()
        assertEquals("device_2", c.currentSong.value.id)
        assertEquals(library.map { it.id }, savedIds())
    }
}
