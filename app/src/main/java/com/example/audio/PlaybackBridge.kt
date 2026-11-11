package com.example.audio

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Bridge between MusicPlaybackService, MusicAppWidgetProvider, and AudioPlayerController.
 */
object PlaybackBridge {
    var controller: AudioPlayerController? = null
    var onToggleLikeCallback: (() -> Unit)? = null

    fun getOrInitController(context: Context): AudioPlayerController {
        return AudioPlayerController.getInstance(context.applicationContext).also {
            controller = it
        }
    }

    fun onTogglePlayPause(context: Context? = null) {
        val c = controller ?: context?.let { getOrInitController(it) }
        c?.togglePlayPause()
    }

    fun onPlay(context: Context? = null) {
        val c = controller ?: context?.let { getOrInitController(it) }
        c?.play()
    }

    fun onPause(context: Context? = null) {
        val c = controller ?: context?.let { getOrInitController(it) }
        c?.pause()
    }

    fun onNext(context: Context? = null) {
        val c = controller ?: context?.let { getOrInitController(it) }
        c?.playNext()
    }

    fun onPrevious(context: Context? = null) {
        val c = controller ?: context?.let { getOrInitController(it) }
        c?.playPrevious()
    }

    fun onStop(context: Context? = null) {
        val c = controller ?: context?.let { getOrInitController(it) }
        c?.pause()
    }

    fun onToggleShuffle(context: Context? = null) {
        val c = controller ?: context?.let { getOrInitController(it) }
        c?.toggleShuffle()
    }

    fun onToggleRepeat(context: Context? = null) {
        val c = controller ?: context?.let { getOrInitController(it) }
        c?.toggleRepeat()
    }

    fun onSeekTo(positionMs: Long, context: Context? = null) {
        val c = controller ?: context?.let { getOrInitController(it) }
        c?.seekTo((positionMs / 1000L).toInt())
    }

    fun onSeekBy(deltaSeconds: Int, context: Context? = null) {
        val c = controller ?: context?.let { getOrInitController(it) }
        c?.seekBy(deltaSeconds)
    }

    fun onSeekToFraction(fraction: Float, context: Context? = null) {
        val c = controller ?: context?.let { getOrInitController(it) }
        c?.seekToFraction(fraction)
    }

    fun onToggleLike(context: Context? = null) {
        if (onToggleLikeCallback != null) {
            onToggleLikeCallback?.invoke()
        } else {
            val c = controller ?: context?.let { getOrInitController(it) }
            c?.let { player ->
                val song = player.currentSong.value
                if (song.id != "none" && song.id != "empty_state") {
                    val updated = song.copy(isLiked = !song.isLiked)
                    player.updateCurrentSongData(updated)
                    context?.let { ctx ->
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                com.example.data.local.LocalMusicRepository(ctx).setSongLiked(song.id, updated.isLiked)
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
        }
    }
}

