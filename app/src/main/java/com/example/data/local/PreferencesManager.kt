package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.AppLanguage
import com.example.data.AppThemeMode
import com.example.data.PlayerSettings
import com.example.data.Song

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("musicfy_settings_prefs", Context.MODE_PRIVATE)

    fun loadSettings(): PlayerSettings {
        val themeModeStr = prefs.getString("theme_mode", AppThemeMode.DARK.name) ?: AppThemeMode.DARK.name
        val themeMode = try {
            AppThemeMode.valueOf(themeModeStr)
        } catch (e: Exception) {
            AppThemeMode.DARK
        }

        val languageStr = prefs.getString("language", AppLanguage.ARABIC.name) ?: AppLanguage.ARABIC.name
        val language = try {
            AppLanguage.valueOf(languageStr)
        } catch (e: Exception) {
            AppLanguage.ARABIC
        }

        val glassEffect = prefs.getBoolean("glass_effect", true)
        val gapless = prefs.getBoolean("gapless_playback", true)
        val crossfade = prefs.getInt("crossfade_seconds", 4)
        val replayGain = prefs.getBoolean("replay_gain", true)

        return PlayerSettings(
            themeMode = themeMode,
            language = language,
            glassEffect = glassEffect,
            gaplessPlayback = gapless,
            crossfadeSeconds = crossfade,
            replayGain = replayGain
        )
    }

    fun saveSettings(settings: PlayerSettings) {
        prefs.edit()
            .putString("theme_mode", settings.themeMode.name)
            .putString("language", settings.language.name)
            .putBoolean("glass_effect", settings.glassEffect)
            .putBoolean("gapless_playback", settings.gaplessPlayback)
            .putInt("crossfade_seconds", settings.crossfadeSeconds)
            .putBoolean("replay_gain", settings.replayGain)
            .apply()
    }

    fun getDeletedSongIds(): Set<String> {
        return prefs.getStringSet("deleted_song_ids", emptySet()) ?: emptySet()
    }

    fun addDeletedSongIds(ids: Collection<String>) {
        val current = getDeletedSongIds().toMutableSet()
        current.addAll(ids)
        prefs.edit().putStringSet("deleted_song_ids", current).apply()
    }

    fun savePlaybackState(
        song: Song,
        positionSec: Int,
        queueSongIds: List<String>,
        isShuffle: Boolean = false,
        isRepeat: Boolean = false
    ) {
        val art = song.customImageUri ?: song.albumArtUri
        prefs.edit()
            .putString("last_played_song_id", song.id)
            .putInt("last_played_position_sec", positionSec)
            .putString("last_queue_ids", queueSongIds.joinToString(","))
            .putBoolean("last_is_shuffle", isShuffle)
            .putBoolean("last_is_repeat", isRepeat)
            .putString("last_played_title", song.title)
            .putString("last_played_artist", song.artist)
            .putInt("last_played_duration_sec", song.durationSec)
            .putString("last_played_content_uri", song.contentUri)
            .putString("last_played_art_uri", art)
            .putBoolean("last_played_is_liked", song.isLiked)
            .apply()
    }

    fun saveLastPosition(positionSec: Int) {
        prefs.edit().putInt("last_played_position_sec", positionSec).apply()
    }

    fun loadPlaybackState(): PlaybackState {
        val songId = prefs.getString("last_played_song_id", null)
        val pos = prefs.getInt("last_played_position_sec", 0)
        val queueStr = prefs.getString("last_queue_ids", "") ?: ""
        val queueIds = if (queueStr.isNotBlank()) queueStr.split(",").filter { it.isNotBlank() } else emptyList()
        val isShuffle = prefs.getBoolean("last_is_shuffle", false)
        val isRepeat = prefs.getBoolean("last_is_repeat", false)
        val title = prefs.getString("last_played_title", null)
        val artist = prefs.getString("last_played_artist", null)
        val durationSec = prefs.getInt("last_played_duration_sec", 0)
        val contentUri = prefs.getString("last_played_content_uri", null)
        val artUri = prefs.getString("last_played_art_uri", null)
        val isLiked = prefs.getBoolean("last_played_is_liked", false)
        return PlaybackState(
            lastSongId = songId,
            lastPositionSec = pos,
            queueSongIds = queueIds,
            isShuffle = isShuffle,
            isRepeat = isRepeat,
            title = title,
            artist = artist,
            durationSec = durationSec,
            contentUri = contentUri,
            artUri = artUri,
            isLiked = isLiked
        )
    }
}

data class PlaybackState(
    val lastSongId: String?,
    val lastPositionSec: Int = 0,
    val queueSongIds: List<String> = emptyList(),
    val isShuffle: Boolean = false,
    val isRepeat: Boolean = false,
    val title: String? = null,
    val artist: String? = null,
    val durationSec: Int = 0,
    val contentUri: String? = null,
    val artUri: String? = null,
    val isLiked: Boolean = false
)


