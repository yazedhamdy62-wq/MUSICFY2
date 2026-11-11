package com.example.data

import android.util.Base64
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

/** Tiny HTTP helper shared by all lyrics sources (no extra libraries needed). */
internal object LyricsHttp {
    fun get(url: String, headers: Map<String, String> = emptyMap()): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 8_000
            connection.readTimeout = 12_000
            connection.setRequestProperty("User-Agent", "Musicfy/1.0")
            connection.setRequestProperty("Accept", "application/json")
            for ((key, value) in headers) {
                connection.setRequestProperty(key, value)
            }
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) throw IOException("HTTP $code")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}

internal fun JSONObject.strOrNull(name: String): String? {
    if (isNull(name)) return null
    return optString(name).takeIf { it.isNotBlank() }
}

internal object LyricsQuery {
    /** Removes things like "(Official Video)" or "[HD]" that break the lyrics search. */
    fun cleanTitle(raw: String): String {
        var t = raw.replace(Regex("""[(\[][^)\]]*[)\]]"""), " ")
        t = t.replace('_', ' ')
        t = t.replace(Regex("""\s+"""), " ").trim()
        return if (t.isNotEmpty()) t else raw.trim()
    }

    fun isRealArtist(artist: String): Boolean {
        val a = artist.trim().lowercase()
        return a.isNotEmpty() &&
            a != "<unknown>" &&
            a != "unknown" &&
            a != "unknown artist" &&
            a != "فنان غير معروف" &&
            a != "artiste inconnu"
    }

    /** Free-text keyword used by sources that don't support separate title/artist fields. */
    fun keyword(title: String, artist: String): String {
        val t = cleanTitle(title)
        val a = artist.trim()
        return if (isRealArtist(a)) "$a $t" else t
    }
}

/**
 * Searches several free lyrics sources at the same time and merges the results:
 * LRCLIB, KuGou and NetEase. If one source is down the others still work.
 */
object LyricsSearch {
    suspend fun search(
        title: String,
        artist: String,
        durationSec: Int,
        customQuery: String? = null
    ): List<OnlineLyricsResult> = coroutineScope {
        val query = customQuery?.trim()?.takeIf { it.isNotEmpty() }

        val outcomes = listOf(
            async { runProvider { LrclibClient.search(title, artist, durationSec, query) } },
            async { runProvider { KuGouClient.search(title, artist, durationSec, query) } },
            async { runProvider { NetEaseClient.search(title, artist, durationSec, query) } }
        ).awaitAll()

        if (outcomes.all { it.isFailure }) {
            throw outcomes.first().exceptionOrNull() ?: IOException("Lyrics search failed")
        }

        val merged = LinkedHashMap<String, OnlineLyricsResult>()
        for (outcome in outcomes) {
            for (item in outcome.getOrNull().orEmpty()) {
                if (!merged.containsKey(item.id)) merged[item.id] = item
            }
        }

        merged.values
            .filter { it.bestText != null }
            .sortedWith(
                compareByDescending<OnlineLyricsResult> {
                    durationSec > 0 && abs(it.durationSec - durationSec) <= 3
                }
                    .thenByDescending { it.hasSynced }
                    .thenBy { sourceRank(it.source) }
                    .thenBy { if (durationSec > 0) abs(it.durationSec - durationSec) else 0 }
            )
            .take(25)
    }

    private fun sourceRank(source: String): Int = when (source) {
        "LRCLIB" -> 0
        "KuGou" -> 1
        else -> 2
    }

    private suspend fun runProvider(
        block: suspend () -> List<OnlineLyricsResult>
    ): Result<List<OnlineLyricsResult>> {
        return try {
            val value = withTimeoutOrNull(15_000) { block() }
            if (value == null) {
                Result.failure<List<OnlineLyricsResult>>(IOException("timeout"))
            } else {
                Result.success(value)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure<List<OnlineLyricsResult>>(e)
        }
    }
}

/** KuGou: song search -> lyrics search -> base64 LRC download. */
internal object KuGouClient {
    suspend fun search(
        title: String,
        artist: String,
        durationSec: Int,
        customQuery: String?
    ): List<OnlineLyricsResult> = withContext(Dispatchers.IO) {
        val keyword = customQuery?.takeIf { it.isNotBlank() } ?: LyricsQuery.keyword(title, artist)
        val body = LyricsHttp.get(
            "https://mobileservice.kugou.com/api/v3/search/song?format=json" +
                "&keyword=${LyricsHttp.encode(keyword)}&page=1&pagesize=8&showtype=1"
        )
        val info = JSONObject(body).optJSONObject("data")?.optJSONArray("info")
            ?: return@withContext emptyList<OnlineLyricsResult>()

        val songs = ArrayList<JSONObject>()
        for (i in 0 until minOf(info.length(), 3)) {
            info.optJSONObject(i)?.let { songs.add(it) }
        }

        coroutineScope {
            songs.map { song -> async { fetchLyrics(song) } }.awaitAll().filterNotNull()
        }
    }

    private fun fetchLyrics(song: JSONObject): OnlineLyricsResult? {
        return try {
            val hash = song.optString("hash")
            if (hash.isBlank()) return null
            val songName = song.optString("songname").replace(Regex("<[^>]*>"), "")
            val singer = song.optString("singername").replace(Regex("<[^>]*>"), "")
            val duration = song.optInt("duration", 0)

            val searchBody = LyricsHttp.get(
                "https://lyrics.kugou.com/search?ver=1&man=yes&client=pc" +
                    "&keyword=${LyricsHttp.encode("$singer - $songName")}" +
                    "&hash=$hash&timelength=${duration * 1000}"
            )
            val candidate = JSONObject(searchBody).optJSONArray("candidates")?.optJSONObject(0)
                ?: return null
            val id = candidate.optString("id")
            val accessKey = candidate.optString("accesskey")
            if (id.isBlank() || accessKey.isBlank()) return null

            val downloadBody = LyricsHttp.get(
                "https://lyrics.kugou.com/download?ver=1&client=pc" +
                    "&id=${LyricsHttp.encode(id)}&accesskey=${LyricsHttp.encode(accessKey)}" +
                    "&fmt=lrc&charset=utf8"
            )
            val content = JSONObject(downloadBody).optString("content")
            if (content.isBlank()) return null
            val lrc = String(Base64.decode(content, Base64.DEFAULT), Charsets.UTF_8)
                .replace("\uFEFF", "")
                .trim()
            if (lrc.isBlank()) return null

            val synced = LrcParser.hasTimestamps(lrc)
            OnlineLyricsResult(
                id = "kugou:$hash",
                source = "KuGou",
                trackName = songName,
                artistName = singer,
                albumName = song.optString("album_name"),
                durationSec = duration,
                plainLyrics = if (synced) null else lrc,
                syncedLyrics = if (synced) lrc else null
            )
        } catch (e: Exception) {
            null
        }
    }
}

/** NetEase Cloud Music: song search -> LRC lyrics. */
internal object NetEaseClient {
    private val headers = mapOf(
        "Referer" to "https://music.163.com/",
        "User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36"
    )

    suspend fun search(
        title: String,
        artist: String,
        durationSec: Int,
        customQuery: String?
    ): List<OnlineLyricsResult> = withContext(Dispatchers.IO) {
        val keyword = customQuery?.takeIf { it.isNotBlank() } ?: LyricsQuery.keyword(title, artist)
        val body = LyricsHttp.get(
            "https://music.163.com/api/search/get/web?csrf_token=&hlpretag=&hlposttag=" +
                "&s=${LyricsHttp.encode(keyword)}&type=1&offset=0&total=true&limit=8",
            headers
        )
        val songsArray = JSONObject(body).optJSONObject("result")?.optJSONArray("songs")
            ?: return@withContext emptyList<OnlineLyricsResult>()

        val songs = ArrayList<JSONObject>()
        for (i in 0 until minOf(songsArray.length(), 3)) {
            songsArray.optJSONObject(i)?.let { songs.add(it) }
        }

        coroutineScope {
            songs.map { song -> async { fetchLyrics(song) } }.awaitAll().filterNotNull()
        }
    }

    private fun fetchLyrics(song: JSONObject): OnlineLyricsResult? {
        return try {
            val id = song.optLong("id")
            if (id == 0L) return null
            val body = LyricsHttp.get(
                "https://music.163.com/api/song/lyric?id=$id&lv=1&kv=1&tv=-1",
                headers
            )
            val lrc = JSONObject(body).optJSONObject("lrc")?.optString("lyric")?.trim()
            if (lrc.isNullOrBlank()) return null

            val artistName = song.optJSONArray("artists")?.optJSONObject(0)?.optString("name")
                ?: song.optJSONArray("ar")?.optJSONObject(0)?.optString("name")
                ?: ""
            val albumName = song.optJSONObject("album")?.optString("name")
                ?: song.optJSONObject("al")?.optString("name")
                ?: ""
            val durationMs = song.optLong("duration", song.optLong("dt", 0L))

            val synced = LrcParser.hasTimestamps(lrc)
            OnlineLyricsResult(
                id = "netease:$id",
                source = "NetEase",
                trackName = song.optString("name"),
                artistName = artistName,
                albumName = albumName,
                durationSec = (durationMs / 1000L).toInt(),
                plainLyrics = if (synced) null else lrc,
                syncedLyrics = if (synced) lrc else null
            )
        } catch (e: Exception) {
            null
        }
    }
}
