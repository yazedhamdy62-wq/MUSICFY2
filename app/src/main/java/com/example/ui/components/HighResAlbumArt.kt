package com.example.ui.components

import android.content.Context
import android.net.Uri
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.key.Keyer
import coil.request.Options
import com.example.data.ArtworkLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer

/** Coil data type meaning "load the full-quality cover of this track" (see [ArtworkLoader]). */
data class HighResAlbumArt(val albumArtUri: Uri, val trackUri: String?)

object HighResAlbumArtKeyer : Keyer<HighResAlbumArt> {
    override fun key(data: HighResAlbumArt, options: Options): String {
        return "hires:${data.albumArtUri}|${data.trackUri}"
    }
}

class HighResAlbumArtFetcher(
    private val context: Context,
    private val data: HighResAlbumArt,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? {
        val bytes = withContext(Dispatchers.IO) {
            ArtworkLoader.loadBestBytes(context, data.albumArtUri.toString(), data.trackUri)
        } ?: return null

        return SourceResult(
            source = ImageSource(Buffer().write(bytes), options.context),
            mimeType = null,
            dataSource = DataSource.DISK
        )
    }

    class Factory(private val context: Context) : Fetcher.Factory<HighResAlbumArt> {
        override fun create(
            data: HighResAlbumArt,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher? {
            return HighResAlbumArtFetcher(context, data, options)
        }
    }
}
