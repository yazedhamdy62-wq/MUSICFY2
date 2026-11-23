package com.example.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** A release found on GitHub. [build] is the CI run number, which is also the app's versionCode. */
data class AppRelease(
    val build: Int,
    val name: String,
    val notes: String,
    val apkUrl: String,
    val sizeBytes: Long
)

/**
 * In-app updates from the project's public GitHub releases. The app only offers a release whose
 * number is higher than its own versionCode, downloads the APK, checks it is this same app and
 * newer, then hands it to Android's installer (the user confirms). Android itself refuses the
 * install if the signing key differs.
 */
object UpdateManager {
    private const val LATEST_URL = "https://api.github.com/repos/yazedhamdy62-wq/MUSICFY2/releases/latest"

    suspend fun fetchLatest(): AppRelease? = withContext(Dispatchers.IO) {
        val body = LyricsHttp.get(LATEST_URL, mapOf("Accept" to "application/vnd.github+json"))
        val json = JSONObject(body)
        val tag = json.optString("tag_name")
        val build = Regex("""(\d+)\s*$""").find(tag)?.groupValues?.get(1)?.toIntOrNull() ?: return@withContext null
        val assets = json.optJSONArray("assets") ?: return@withContext null
        for (i in 0 until assets.length()) {
            val a = assets.optJSONObject(i) ?: continue
            if (a.optString("name").endsWith(".apk")) {
                return@withContext AppRelease(
                    build = build,
                    name = json.optString("name").ifBlank { tag },
                    notes = json.optString("body").trim(),
                    apkUrl = a.optString("browser_download_url"),
                    sizeBytes = a.optLong("size")
                )
            }
        }
        null
    }

    fun installedBuild(context: Context): Int = try {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= 28) info.longVersionCode.toInt() else @Suppress("DEPRECATION") info.versionCode
    } catch (_: Exception) { 0 }

    suspend fun download(context: Context, release: AppRelease, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            if (!release.apkUrl.startsWith("https://")) throw IOException("Bad download address")
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, "musicfy-update.apk")
            val c = URL(release.apkUrl).openConnection() as HttpURLConnection
            try {
                c.connectTimeout = 15_000
                c.readTimeout = 30_000
                c.instanceFollowRedirects = true
                if (c.responseCode != HttpURLConnection.HTTP_OK) throw IOException("HTTP ${c.responseCode}")
                val total = c.contentLengthLong.takeIf { it > 0 } ?: release.sizeBytes
                c.inputStream.use { input ->
                    file.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024)
                        var done = 0L
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            done += n
                            if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
                if (release.sizeBytes > 0 && file.length() != release.sizeBytes) {
                    file.delete()
                    throw IOException("Incomplete download")
                }
            } finally {
                c.disconnect()
            }
            // The file must be this app and newer than what is installed
            @Suppress("DEPRECATION")
            val archive = context.packageManager.getPackageArchiveInfo(file.absolutePath, 0)
            val archiveBuild = archive?.let {
                if (Build.VERSION.SDK_INT >= 28) it.longVersionCode.toInt() else @Suppress("DEPRECATION") it.versionCode
            } ?: -1
            if (archive == null || archive.packageName != context.packageName || archiveBuild <= installedBuild(context)) {
                file.delete()
                throw IOException("Downloaded file is not a newer version of this app")
            }
            file
        }

    /** Opens Android's installer. Returns false when the user must first allow installs from this app. */
    fun install(context: Context, file: File): Boolean {
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return false
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return true
    }
}
