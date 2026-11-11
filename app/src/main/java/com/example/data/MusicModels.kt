package com.example.data

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

data class LyricLine(
    val timeSec: Int,
    val text: String,
    val sectionTag: String? = null
)

enum class CoverArtType {
    TAWAHIN_WINDMILL,
    GRADIENT_PURPLE,
    GRADIENT_ORANGE,
    GRADIENT_TEAL,
    GRADIENT_BLUE,
    GRADIENT_MAGENTA,
    GRADIENT_AMBER,
    GRADIENT_DARK,
    ARTIST_CIRCLE,
    ALBUM_VINYL,
    CUSTOM_IMAGE,
    DEFAULT_PLACEHOLDER
}

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val durationSec: Int,
    val coverType: CoverArtType = CoverArtType.GRADIENT_PURPLE,
    val gradientColors: List<Color> = listOf(GradientLiked1, GradientLiked2),
    val isLiked: Boolean = false,
    val albumName: String = "Musicfy",
    val contentUri: String? = null,
    val albumArtUri: String? = null,
    val customImageUri: String? = null,
    val filePath: String? = null,
    val lyrics: List<LyricLine> = emptyList(),
    val rawLyricsText: String? = null
) {
    val durationFormatted: String by lazy {
        val minutes = durationSec / 60
        val seconds = durationSec % 60
        if (seconds < 10) "$minutes:0$seconds" else "$minutes:$seconds"
    }
}

data class Playlist(
    val id: String,
    val title: String,
    val subtitle: String,
    val songCount: Int,
    val durationText: String,
    val coverType: CoverArtType,
    val gradientColors: List<Color>,
    val isPinned: Boolean = false,
    val isPodcast: Boolean = false,
    val songIds: List<String> = emptyList(),
    val customImageUri: String? = null
)

enum class AppThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

enum class AppLanguage {
    ARABIC,
    ENGLISH,
    FRENCH
}

data class PlayerSettings(
    val themeMode: AppThemeMode = AppThemeMode.DARK,
    val language: AppLanguage = AppLanguage.ARABIC,
    val glassEffect: Boolean = true,
    val audioBitrate: String = "320 kbps (عالي جداً)",
    val gaplessPlayback: Boolean = true,
    val crossfadeSeconds: Int = 4,
    val replayGain: Boolean = true,
    val autoSleepTimerEnabled: Boolean = false,
    val sleepTimerMinutes: Int? = null,
    val musicFolderPath: String = "/storage/emulated/0/Music",
    val cacheSizeMb: Float = 124.5f
)

object MusicRepository {
    // Initial songs list starts empty - populated via real device scanning (MediaStore)
    val allSongs: List<Song> = emptyList()

    // Fallback song when library is empty
    val fallbackSong: Song = Song(
        id = "none",
        title = "لم يتم تشغيل مقطع",
        artist = "اضغط لفحص مكتبة الجهاز",
        durationSec = 0,
        coverType = CoverArtType.GRADIENT_PURPLE,
        gradientColors = listOf(GradientLiked1, GradientLiked2),
        albumName = "Musicfy"
    )

    // Only "Liked Songs" is preserved by default as requested by the user
    val initialPlaylists: List<Playlist> = listOf(
        Playlist(
            id = "p_liked",
            title = "Liked Songs",
            subtitle = "Playlist • Favorite Tracks",
            songCount = 0,
            durationText = "0m",
            coverType = CoverArtType.GRADIENT_PURPLE,
            gradientColors = listOf(GradientLiked1, GradientLiked2),
            isPinned = true,
            songIds = emptyList()
        )
    )
}
