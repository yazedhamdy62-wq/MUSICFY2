package com.example.data.local

import android.content.Context
import androidx.compose.ui.graphics.Color
import com.example.data.*
import com.example.ui.theme.GradientLiked1
import com.example.ui.theme.GradientLiked2
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalMusicRepository(context: Context) {
    private val dao = MusicfyDatabase.getDatabase(context).musicDao()

    val playlistsFlow: Flow<List<Playlist>> = dao.getAllPlaylists().map { entities ->
        if (entities.isEmpty()) {
            emptyList()
        } else {
            entities.map { it.toDomain() }
        }
    }

    val songMetadataFlow: Flow<List<SongMetadataEntity>> = dao.getAllSongMetadata()

    suspend fun loadInitialData(): Pair<List<Playlist>, List<SongMetadataEntity>> {
        var playlists = dao.getAllPlaylistsSync().map { it.toDomain() }
        if (playlists.isEmpty() || playlists.none { it.id == "p_liked" }) {
            val defaultLiked = Playlist(
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
            dao.insertPlaylist(defaultLiked.toEntity())
            playlists = if (playlists.isEmpty()) listOf(defaultLiked) else listOf(defaultLiked) + playlists.filter { it.id != "p_liked" }
        }
        val metadata = dao.getAllSongMetadataSync()
        return Pair(playlists, metadata)
    }

    fun applyMetadataToSongs(songs: List<Song>, metadataList: List<SongMetadataEntity>): List<Song> {
        if (metadataList.isEmpty()) return songs
        val metaMap = metadataList.associateBy { it.songId }
        return songs.map { song ->
            val meta = metaMap[song.id]
            if (meta != null) {
                val cover = meta.coverTypeName?.let {
                    try { CoverArtType.valueOf(it) } catch (e: Exception) { null }
                }
                val gradColors = if (meta.gradientColor1 != null && meta.gradientColor2 != null) {
                    listOf(Color(meta.gradientColor1.toULong()), Color(meta.gradientColor2.toULong()))
                } else null

                song.copy(
                    isLiked = meta.isLiked,
                    title = meta.customTitle ?: song.title,
                    artist = meta.customArtist ?: song.artist,
                    coverType = cover ?: song.coverType,
                    gradientColors = gradColors ?: song.gradientColors,
                    customImageUri = meta.customImageUri ?: song.customImageUri,
                    rawLyricsText = meta.rawLyricsText ?: song.rawLyricsText
                )
            } else {
                song
            }
        }
    }

    suspend fun getAllMetadataSync(): List<SongMetadataEntity> {
        return dao.getAllSongMetadataSync()
    }

    suspend fun insertPlaylist(playlist: Playlist) {
        dao.insertPlaylist(playlist.toEntity())
    }

    suspend fun renamePlaylist(id: String, newTitle: String) {
        dao.renamePlaylist(id, newTitle)
    }

    suspend fun updatePlaylistSongs(id: String, songIds: List<String>) {
        val csv = songIds.joinToString(",")
        dao.updatePlaylistSongs(id, csv)
    }

    suspend fun deletePlaylist(id: String) {
        dao.deletePlaylistById(id)
    }

    suspend fun setSongLiked(songId: String, isLiked: Boolean) {
        val existing = dao.getAllSongMetadataSync().find { it.songId == songId }
        if (existing != null) {
            dao.insertSongMetadata(existing.copy(isLiked = isLiked))
        } else {
            dao.insertSongMetadata(SongMetadataEntity(songId = songId, isLiked = isLiked))
        }
    }

    suspend fun toggleLike(songId: String, currentIsLiked: Boolean) {
        setSongLiked(songId, !currentIsLiked)
    }

    suspend fun updateSongDetails(
        songId: String,
        customTitle: String? = null,
        customArtist: String? = null,
        coverType: CoverArtType? = null,
        gradientColors: List<Color>? = null,
        customImageUri: String? = null,
        rawLyrics: String? = null
    ) {
        val existing = dao.getAllSongMetadataSync().find { it.songId == songId }
        val updated = (existing ?: SongMetadataEntity(songId = songId)).copy(
            customTitle = customTitle ?: existing?.customTitle,
            customArtist = customArtist ?: existing?.customArtist,
            coverTypeName = coverType?.name ?: existing?.coverTypeName,
            gradientColor1 = gradientColors?.getOrNull(0)?.value?.toLong() ?: existing?.gradientColor1,
            gradientColor2 = gradientColors?.getOrNull(1)?.value?.toLong() ?: existing?.gradientColor2,
            customImageUri = customImageUri ?: existing?.customImageUri,
            rawLyricsText = rawLyrics ?: existing?.rawLyricsText
        )
        dao.insertSongMetadata(updated)
    }
}

fun Playlist.toEntity(): PlaylistEntity {
    val c1 = gradientColors.getOrNull(0)?.value?.toLong() ?: 0xFF8A2BE2
    val c2 = gradientColors.getOrNull(1)?.value?.toLong() ?: 0xFF4A00E0
    return PlaylistEntity(
        id = id,
        title = title,
        subtitle = subtitle,
        durationText = durationText,
        coverTypeName = coverType.name,
        gradientColor1 = c1,
        gradientColor2 = c2,
        isPinned = isPinned,
        isPodcast = isPodcast,
        songIdsCsv = songIds.joinToString(","),
        customImageUri = customImageUri
    )
}

fun PlaylistEntity.toDomain(): Playlist {
    val type = try {
        CoverArtType.valueOf(coverTypeName)
    } catch (e: Exception) {
        CoverArtType.GRADIENT_TEAL
    }
    val colors = listOf(Color(gradientColor1.toULong()), Color(gradientColor2.toULong()))
    val ids = if (songIdsCsv.isBlank()) emptyList() else songIdsCsv.split(",").filter { it.isNotBlank() }
    return Playlist(
        id = id,
        title = title,
        subtitle = subtitle,
        songCount = ids.size,
        durationText = durationText,
        coverType = type,
        gradientColors = colors,
        isPinned = isPinned,
        isPodcast = isPodcast,
        songIds = ids,
        customImageUri = customImageUri
    )
}

/**
 * Returns the library copy of [current] (same id, merged with stored metadata such as lyrics)
 * when it differs from what the player holds, or null when nothing needs refreshing.
 */
fun refreshedCurrentSong(current: Song, library: List<Song>): Song? {
    val match = library.find { it.id == current.id } ?: return null
    return if (match != current) match else null
}

/**
 * Rebuilds the playback queue saved before the app closed: same songs, same order. Ids that are
 * no longer in the library are skipped. The current song is always part of the queue. Nothing is
 * added from outside the saved queue, so a playlist stays a playlist and a one-song queue stays
 * one song.
 */
fun resolveRestoredQueue(savedIds: List<String>, current: Song, library: List<Song>): List<Song> {
    val byId = library.associateBy { it.id }
    val found = savedIds.mapNotNull { byId[it] }
    return if (found.any { it.id == current.id }) found else listOf(current) + found
}
