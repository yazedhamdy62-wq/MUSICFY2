package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

/** One lyrics candidate returned by the LRCLIB search API. */
data class OnlineLyricsResult(
    val id: Long,
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
                .filter { it.isNotEmpty() }
                .take(2)
                .joinToString("\n")
        }
}

/**
 * Minimal client for https://lrclib.net (free lyrics database, no API key needed).
 * Uses plain HttpURLConnection + org.json so no extra dependencies are required.
 */
object LrclibClient {
    private const val BASE_URL = "https://lrclib.net/api/search"

    suspend fun search(title: String, artist: String, durationSec: Int): List<OnlineLyricsResult> =
        withContext(Dispatchers.IO) {
            val cleanTitle = cleanTitle(title)
            val realArtist = artist.trim().takeIf { isRealArtist(it) }

            val queries = ArrayList<String>()
            if (realArtist != null) {
                queries.add("track_name=${enc(cleanTitle)}&artist_name=${enc(realArtist)}")
                queries.add("q=${enc("$cleanTitle $realArtist")}")
            }
            queries.add("q=${enc(cleanTitle)}")
            if (cleanTitle.contains(" - ")) {
                val before = cleanTitle.substringBefore(" - ").trim()
                val after = cleanTitle.substringAfter(" - ").trim()
                if (before.isNotEmpty() && after.isNotEmpty()) {
                    queries.add("q=${enc("$after $before")}")
                }
            }

            val found = LinkedHashMap<Long, OnlineLyricsResult>()
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

            found.values
                .filter { it.bestText != null }
                .sortedWith(
                    compareByDescending<OnlineLyricsResult> {
                        durationSec > 0 && abs(it.durationSec - durationSec) <= 3
                    }
                        .thenByDescending { it.hasSynced }
                        .thenBy { if (durationSec > 0) abs(it.durationSec - durationSec) else 0 }
                )
                .take(20)
        }

    private fun fetch(url: String): List<OnlineLyricsResult> {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("User-Agent", "Musicfy/1.0")
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return emptyList()
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val array = JSONArray(body)
            val list = ArrayList<OnlineLyricsResult>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                list.add(
                    OnlineLyricsResult(
                        id = obj.optLong("id"),
                        trackName = obj.str("trackName") ?: "",
                        artistName = obj.str("artistName") ?: "",
                        albumName = obj.str("albumName") ?: "",
                        durationSec = obj.optDouble("duration", 0.0).toInt(),
                        plainLyrics = obj.str("plainLyrics"),
                        syncedLyrics = obj.str("syncedLyrics")
                    )
                )
            }
            return list
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.str(name: String): String? {
        if (isNull(name)) return null
        return optString(name).takeIf { it.isNotBlank() }
    }

    private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    /** Removes things like "(Official Video)" or "[HD]" that break the lyrics search. */
    private fun cleanTitle(raw: String): String {
        var t = raw.replace(Regex("""[(\[][^)\]]*[)\]]"""), " ")
        t = t.replace('_', ' ')
        t = t.replace(Regex("""\s+"""), " ").trim()
        return if (t.isNotEmpty()) t else raw.trim()
    }

    private fun isRealArtist(artist: String): Boolean {
        val a = artist.trim().lowercase()
        return a.isNotEmpty() &&
            a != "<unknown>" &&
            a != "unknown" &&
            a != "unknown artist" &&
            a != "فنان غير معروف" &&
            a != "artiste inconnu"
    }
}
