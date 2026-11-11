package com.example.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*

/**
 * Scans real audio files on device storage via MediaStore.
 */
object MediaStoreAudioScanner {

    fun scanDeviceAudio(context: Context): List<Song> {
        val songs = mutableListOf<Song>()
        val contentResolver = context.contentResolver

        val collectionUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.RELATIVE_PATH
            )
        } else {
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DATA
            )
        }

        // Include all audio files across all apps/paths (WhatsApp, Telegram, Downloads, Music, Recordings, etc.)
        // while excluding system ringtones, alarms, and notifications
        val selection = "(${MediaStore.Audio.Media.IS_NOTIFICATION} = 0 AND ${MediaStore.Audio.Media.IS_ALARM} = 0 AND ${MediaStore.Audio.Media.IS_RINGTONE} = 0) OR ${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val gradientPresets = listOf(
            Pair(CoverArtType.GRADIENT_PURPLE, listOf(GradientLiked1, GradientLiked2)),
            Pair(CoverArtType.GRADIENT_ORANGE, listOf(GradientStarboy1, GradientStarboy2)),
            Pair(CoverArtType.GRADIENT_BLUE, listOf(GradientJazz1, GradientJazz2)),
            Pair(CoverArtType.GRADIENT_TEAL, listOf(GradientGym1, GradientGym2)),
            Pair(CoverArtType.GRADIENT_MAGENTA, listOf(GradientRecent1, GradientRecent2)),
            Pair(CoverArtType.GRADIENT_DARK, listOf(GradientChill1, GradientChill2)),
            Pair(CoverArtType.ALBUM_VINYL, listOf(Color(0xFF451A03), Color(0xFF78350F))),
            Pair(CoverArtType.TAWAHIN_WINDMILL, listOf(Color(0xFFE2E8F0), Color(0xFFCBD5E1)))
        )

        try {
            contentResolver.query(collectionUri, projection, selection, null, sortOrder)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else -1

                var index = 0
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawTitle = cursor.getString(titleCol) ?: "مسار غير مسمى"
                    val rawArtist = cursor.getString(artistCol)
                    val artist = if (rawArtist == null || rawArtist.contains("<unknown>", ignoreCase = true)) {
                        "فنان غير معروف"
                    } else rawArtist

                    val durationMs = cursor.getLong(durationCol)
                    // Skip tiny sound effects/beeps under 3 seconds
                    if (durationMs < 3000) continue

                    val durationSec = (durationMs / 1000).toInt()
                    val album = cursor.getString(albumCol) ?: "Musicfy"
                    val albumId = cursor.getLong(albumIdCol)
                    val rawFilePath = if (dataCol >= 0) cursor.getString(dataCol) else null
                    val relativePath = if (relPathCol >= 0) cursor.getString(relPathCol) else null
                    val filePath = rawFilePath ?: if (!relativePath.isNullOrBlank()) "/storage/emulated/0/$relativePath" else null

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val rawAlbumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )

                    // Verify if album art actually exists in MediaStore
                    val hasAlbumArt = try {
                        context.contentResolver.openFileDescriptor(rawAlbumArtUri, "r")?.use { true } ?: false
                    } catch (_: Exception) {
                        false
                    }

                    val finalAlbumArt = if (hasAlbumArt) rawAlbumArtUri.toString() else null

                    songs.add(
                        Song(
                            id = "device_$id",
                            title = rawTitle,
                            artist = artist,
                            durationSec = durationSec,
                            coverType = if (finalAlbumArt != null) CoverArtType.DEFAULT_PLACEHOLDER else CoverArtType.DEFAULT_PLACEHOLDER,
                            gradientColors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
                            isLiked = false,
                            albumName = album,
                            contentUri = contentUri.toString(),
                            albumArtUri = finalAlbumArt,
                            filePath = filePath
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return songs
    }
}
