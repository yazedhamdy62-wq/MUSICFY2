package com.example.data

import androidx.compose.ui.graphics.Color
import java.io.File

enum class FolderSourceType {
    WHATSAPP,
    TELEGRAM,
    DOWNLOADS,
    MUSIC,
    BLUETOOTH,
    RECORDINGS,
    SNAPTUBE,
    VIDMATE,
    GENERAL
}

data class MusicFolder(
    val id: String,
    val name: String,
    val fullPath: String,
    val sourceType: FolderSourceType,
    val songs: List<Song>,
    val songCount: Int,
    val totalDurationSec: Int,
    val accentColor: Color,
    val badgeName: String
) {
    val totalDurationFormatted: String
        get() {
            val totalMins = totalDurationSec / 60
            val hours = totalMins / 60
            val mins = totalMins % 60
            return if (hours > 0) "$hours س $mins د" else "$mins دقيقة"
        }
}

object FolderManager {

    fun groupSongsByFolder(songs: List<Song>): List<MusicFolder> {
        if (songs.isEmpty()) return emptyList()

        val folderMap = mutableMapOf<String, MutableList<Song>>()

        for (song in songs) {
            val dirPath = resolveSongDirectory(song)
            val list = folderMap.getOrPut(dirPath) { mutableListOf() }
            list.add(song)
        }

        return folderMap.map { (dirPath, folderSongs) ->
            val meta = analyzeFolderInfo(dirPath)
            val totalDuration = folderSongs.sumOf { it.durationSec }

            MusicFolder(
                id = "folder_${dirPath.hashCode()}",
                name = meta.name,
                fullPath = dirPath,
                sourceType = meta.sourceType,
                songs = folderSongs,
                songCount = folderSongs.size,
                totalDurationSec = totalDuration,
                accentColor = meta.color,
                badgeName = meta.badge
            )
        }.sortedWith(
            compareByDescending<MusicFolder> {
                when (it.sourceType) {
                    FolderSourceType.WHATSAPP -> 100
                    FolderSourceType.TELEGRAM -> 90
                    FolderSourceType.DOWNLOADS -> 80
                    FolderSourceType.MUSIC -> 70
                    FolderSourceType.SNAPTUBE, FolderSourceType.VIDMATE -> 60
                    FolderSourceType.BLUETOOTH -> 50
                    FolderSourceType.RECORDINGS -> 40
                    FolderSourceType.GENERAL -> 10
                }
            }.thenByDescending { it.songCount }
        )
    }

    private fun resolveSongDirectory(song: Song): String {
        val path = song.filePath
        if (!path.isNullOrBlank()) {
            val lastSlash = path.lastIndexOf('/')
            if (lastSlash > 0) {
                return path.substring(0, lastSlash)
            }
        }
        return if (!song.albumName.isNullOrBlank() && song.albumName != "Musicfy") {
            "/storage/emulated/0/Music/${song.albumName}"
        } else {
            "/storage/emulated/0/Music"
        }
    }

    private fun analyzeFolderInfo(dirPath: String): FolderMeta {
        val lower = dirPath.lowercase()
        return when {
            lower.contains("whatsapp") -> {
                val displayName = when {
                    lower.contains("whatsapp audio") -> "WhatsApp Audio"
                    lower.contains("whatsapp voice") -> "WhatsApp Voice Notes"
                    lower.contains("whatsapp documents") -> "WhatsApp Documents"
                    lower.contains("business") -> "WhatsApp Business"
                    else -> "WhatsApp"
                }
                FolderMeta(
                    name = displayName,
                    sourceType = FolderSourceType.WHATSAPP,
                    color = Color(0xFF25D366),
                    badge = "WhatsApp"
                )
            }
            lower.contains("telegram") -> {
                FolderMeta(
                    name = "Telegram Audio",
                    sourceType = FolderSourceType.TELEGRAM,
                    color = Color(0xFF2AABEE),
                    badge = "Telegram"
                )
            }
            lower.contains("snaptube") -> {
                FolderMeta(
                    name = "SnapTube Downloads",
                    sourceType = FolderSourceType.SNAPTUBE,
                    color = Color(0xFFFFB300),
                    badge = "SnapTube"
                )
            }
            lower.contains("vidmate") -> {
                FolderMeta(
                    name = "VidMate Downloads",
                    sourceType = FolderSourceType.VIDMATE,
                    color = Color(0xFFE53935),
                    badge = "VidMate"
                )
            }
            lower.contains("/download") || lower.contains("/downloads") -> {
                FolderMeta(
                    name = "Downloads / التنزيلات",
                    sourceType = FolderSourceType.DOWNLOADS,
                    color = Color(0xFFFF9800),
                    badge = "Downloads"
                )
            }
            lower.contains("bluetooth") -> {
                FolderMeta(
                    name = "Bluetooth / بلوتوث",
                    sourceType = FolderSourceType.BLUETOOTH,
                    color = Color(0xFF0284C7),
                    badge = "Bluetooth"
                )
            }
            lower.contains("record") || lower.contains("soundrecorder") || lower.contains("voice") -> {
                FolderMeta(
                    name = "Recordings / التسجيلات",
                    sourceType = FolderSourceType.RECORDINGS,
                    color = Color(0xFFF43F5E),
                    badge = "Recordings"
                )
            }
            lower.contains("/music") -> {
                val subDir = dirPath.substringAfterLast('/', "")
                val name = if (subDir.isNotBlank() && subDir != "Music") "Music / $subDir" else "Music / الموسيقى"
                FolderMeta(
                    name = name,
                    sourceType = FolderSourceType.MUSIC,
                    color = Color(0xFF5DD62C),
                    badge = "Music"
                )
            }
            else -> {
                val folderName = dirPath.substringAfterLast('/', "مجلد عام")
                FolderMeta(
                    name = if (folderName.isNotBlank()) folderName else "مجلد النظام",
                    sourceType = FolderSourceType.GENERAL,
                    color = Color(0xFF10B981),
                    badge = "مجلد"
                )
            }
        }
    }

    private data class FolderMeta(
        val name: String,
        val sourceType: FolderSourceType,
        val color: Color,
        val badge: String
    )
}
