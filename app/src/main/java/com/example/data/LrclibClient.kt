package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import kotlin.math.abs

/** One lyrics candidate returned by one of the online lyrics sources. */
data class OnlineLyricsResult(
    val id: String,
    val source: String,
    val trackName: String,
    val artistName: String,
    val albumName: String,
    val durationSec: Int,
    val plainLyrics: String?,
    val syncedLyrics: String?
) {
    val hasSynced: Boolean
        get() = !syncedLyrics.isNullOrBlank()

    /** The text that gets saved: synced (LRC) lyrics if available, otherwise plain text. */
    val bestText: String?
        get() = if (hasSynced) syncedLyrics else plainLyrics?.takeIf { it.isNotBlank() }

    /** First two lines of the lyrics, shown as a preview in the picker. */
    val previewText: String
        get() {
            val source = plainLyrics?.takeIf { it.isNotBlank() }
                ?: LrcParser.stripTimestamps(syncedLyrics.orEmpty())
            return source.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("[") }
                .take(2)
                .joinToString("\n")
        }
}

/**
 * Client for https://lrclib.net (free lyrics database, no API key needed).
 * Uses plain HttpURLConnection + org.json so no extra dependencies are required.
 */
object LrclibClient {
    private const val BASE_URL = "https://lrclib.net/api/search"

    suspend fun search(
        title: String,
        artist: String,
        durationSec: Int,
        customQuery: String? = null
    ): List<OnlineLyricsResult> = withContext(Dispatchers.IO) {
        val cleanTitle = LyricsQuery.cleanTitle(title)
        val realArtist = artist.trim().takeIf { LyricsQuery.isRealArtist(it) }

        val queries = ArrayList<String>()
        if (!customQuery.isNullOrBlank()) {
            queries.add("q=${LyricsHttp.encode(customQuery)}")
        } else {
            if (realArtist != null) {
                queries.add("track_name=${LyricsHttp.encode(cleanTitle)}&artist_name=${LyricsHttp.encode(realArtist)}")
                queries.add("q=${LyricsHttp.encode("$cleanTitle $realArtist")}")
            }
            queries.add("q=${LyricsHttp.encode(cleanTitle)}")
            if (cleanTitle.contains(" - ")) {
                val before = cleanTitle.substringBefore(" - ").trim()
                val after = cleanTitle.substringAfter(" - ").trim()
                if (before.isNotEmpty() && after.isNotEmpty()) {
                    queries.add("q=${LyricsHttp.encode("$after $before")}")
                }
            }
        }

        val found = LinkedHashMap<String, OnlineLyricsResult>()
        var anySuccess = false
        var firstError: Exception? = null
        for (query in queries) {
            try {
                val batch = fetch("$BASE_URL?$query")
                anySuccess = true
                for (item in batch) {
                    if (!found.containsKey(item.id)) found[item.id] = item
                }
                if (found.size >= 6) break
            } catch (e: Exception) {
                if (firstError == null) firstError = e
            }
        }
        val error = firstError
        if (!anySuccess && error != null) throw error

        found.values.toList()
    }

    private fun fetch(url: String): List<OnlineLyricsResult> {
        val body = LyricsHttp.get(url)
        val array = JSONArray(body)
        val list = ArrayList<OnlineLyricsResult>()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            list.add(
                OnlineLyricsResult(
                    id = "lrclib:" + obj.optLong("id"),
                    source = "LRCLIB",
                    trackName = obj.strOrNull("trackName") ?: "",
                    artistName = obj.strOrNull("artistName") ?: "",
                    albumName = obj.strOrNull("albumName") ?: "",
                    durationSec = obj.optDouble("duration", 0.0).toInt(),
                    plainLyrics = obj.strOrNull("plainLyrics"),
                    syncedLyrics = obj.strOrNull("syncedLyrics")
                )
            )
        }
        return list
    }
}
