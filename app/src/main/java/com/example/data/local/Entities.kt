package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String,
    val durationText: String,
    val coverTypeName: String,
    val gradientColor1: Long,
    val gradientColor2: Long,
    val isPinned: Boolean,
    val isPodcast: Boolean,
    val songIdsCsv: String,
    val customImageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "song_metadata")
data class SongMetadataEntity(
    @PrimaryKey val songId: String,
    val isLiked: Boolean = false,
    val customTitle: String? = null,
    val customArtist: String? = null,
    val coverTypeName: String? = null,
    val gradientColor1: Long? = null,
    val gradientColor2: Long? = null,
    val customImageUri: String? = null,
    val rawLyricsText: String? = null
)
