package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {
    // Playlists
    @Query("SELECT * FROM playlists ORDER BY isPinned DESC, createdAt ASC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists ORDER BY isPinned DESC, createdAt ASC")
    suspend fun getAllPlaylistsSync(): List<PlaylistEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylists(playlists: List<PlaylistEntity>)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deletePlaylistById(id: String)

    @Query("UPDATE playlists SET title = :newTitle WHERE id = :id")
    suspend fun renamePlaylist(id: String, newTitle: String)

    @Query("UPDATE playlists SET songIdsCsv = :songIdsCsv WHERE id = :id")
    suspend fun updatePlaylistSongs(id: String, songIdsCsv: String)

    // Song Metadata (Likes, custom covers, custom tags, custom lyrics)
    @Query("SELECT * FROM song_metadata")
    fun getAllSongMetadata(): Flow<List<SongMetadataEntity>>

    @Query("SELECT * FROM song_metadata")
    suspend fun getAllSongMetadataSync(): List<SongMetadataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongMetadata(metadata: SongMetadataEntity)

    @Query("UPDATE song_metadata SET isLiked = :isLiked WHERE songId = :songId")
    suspend fun setSongLiked(songId: String, isLiked: Boolean)

    @Query("DELETE FROM song_metadata WHERE songId = :songId")
    suspend fun deleteSongMetadata(songId: String)
}
