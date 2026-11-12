package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import java.io.ByteArrayOutputStream

/**
 * Loads the best-quality cover art available for a song.
 *
 * MediaStore's "albumart" URI only returns a small, low-resolution thumbnail. For those URIs we
 * read the full-size picture embedded in the audio file instead (or the system thumbnail at
 * 1024px as a second choice). Custom images picked by the user are loaded as they are.
 */
object ArtworkLoader {

    fun isMediaStoreAlbumArt(artUri: String?): Boolean {
        return !artUri.isNullOrBlank() && artUri.contains("/audio/albumart/")
    }

    /** Raw image bytes of the best cover, or null if nothing could be loaded. */
    fun loadBestBytes(context: Context, artUri: String?, trackUri: String?): ByteArray? {
        if (artUri.isNullOrBlank()) return null

        if (isMediaStoreAlbumArt(artUri) && !trackUri.isNullOrBlank()) {
            val track = Uri.parse(trackUri)

            val embedded = try {
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, track)
                    retriever.embeddedPicture
                } finally {
                    try {
                        retriever.release()
                    } catch (_: Exception) {
                    }
                }
            } catch (_: Exception) {
                null
            }
            if (embedded != null && embedded.isNotEmpty()) return embedded

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val thumb = context.contentResolver.loadThumbnail(track, Size(1024, 1024), null)
                    val out = ByteArrayOutputStream()
                    thumb.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    return out.toByteArray()
                } catch (_: Exception) {
                    // fall through to the original art URI
                }
            }
        }

        return try {
            context.contentResolver.openInputStream(Uri.parse(artUri))?.use { it.readBytes() }
        } catch (_: Exception) {
            null
        }
    }

    /** Best-quality cover decoded so its longest side is at least [maxSide] px (when possible). */
    fun loadBitmap(context: Context, artUri: String?, trackUri: String?, maxSide: Int): Bitmap? {
        val bytes = loadBestBytes(context, artUri, trackUri) ?: return null
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val largest = maxOf(bounds.outWidth, bounds.outHeight)
            var sample = 1
            while (largest / (sample * 2) >= maxSide) {
                sample *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        } catch (_: Exception) {
            null
        }
    }
}
