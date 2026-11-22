package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.util.LruCache
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.audio.PlaybackBridge
import com.example.data.ArtworkLoader
import com.example.data.LocalizationManager
import com.example.data.Song
import com.example.data.local.PreferencesManager
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Draws the two new home-screen widgets:
 *  - "Musicfy Glass": wide liquid-glass card (cover, title, progress, replay / prev / play / next / share)
 *  - "Musicfy Cover": square cover-art card (artist pill, share, like, progress, prev / play / next)
 *
 * Rendering (bitmap work) happens on a single background thread; the glass / cover backgrounds are
 * cached per song and widget size, so a progress tick only re-sends a few lines of text.
 */
object ModernWidgets {

    const val ACTION_PLAY_PAUSE = "com.example.action.MW_PLAY_PAUSE"
    const val ACTION_NEXT = "com.example.action.MW_NEXT"
    const val ACTION_PREV = "com.example.action.MW_PREV"
    const val ACTION_LIKE = "com.example.action.MW_LIKE"
    const val ACTION_REPLAY = "com.example.action.MW_REPLAY"
    const val ACTION_SEEK = "com.example.action.MW_SEEK"
    const val EXTRA_FRACTION = "mw_fraction"

    private val SEEK_IDS = intArrayOf(
        R.id.w_seek_1, R.id.w_seek_2, R.id.w_seek_3, R.id.w_seek_4, R.id.w_seek_5,
        R.id.w_seek_6, R.id.w_seek_7, R.id.w_seek_8, R.id.w_seek_9, R.id.w_seek_10
    )

    private val executor = Executors.newSingleThreadExecutor()

    private val bgCache = object : LruCache<String, Bitmap>(14 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    // last decoded cover (so it is not decoded again on every tick)
    @Volatile private var artKey: String? = null
    @Volatile private var artBitmap: Bitmap? = null

    private data class WState(
        val title: String,
        val artist: String,
        val posSec: Int,
        val durSec: Int,
        val playing: Boolean,
        val liked: Boolean,
        val artUri: String?,
        val trackUri: String?
    )

    private data class WSize(val w: Int, val h: Int, val pxPerDp: Float)

    /** Runs [block] after every render that is already queued. */
    fun whenIdle(block: () -> Unit) {
        executor.execute {
            try {
                block()
            } catch (_: Exception) {
            }
        }
    }

    fun updateAll(
        context: Context,
        currentSong: Song? = null,
        isPlaying: Boolean? = null,
        currentPositionSec: Int? = null
    ) {
        val app = context.applicationContext
        val manager = AppWidgetManager.getInstance(app)
        val glassIds = manager.getAppWidgetIds(ComponentName(app, MusicGlassWidgetProvider::class.java))
        val coverIds = manager.getAppWidgetIds(ComponentName(app, MusicCoverWidgetProvider::class.java))
        if (glassIds.isEmpty() && coverIds.isEmpty()) return

        val state = resolveState(app, currentSong, isPlaying, currentPositionSec)

        executor.execute {
            try {
                val art = loadArt(app, state)
                for (id in glassIds) {
                    try {
                        renderGlass(app, manager, id, state, art)
                    } catch (_: Exception) {
                    }
                }
                for (id in coverIds) {
                    try {
                        renderCover(app, manager, id, state, art)
                    } catch (_: Exception) {
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // State
    // ---------------------------------------------------------------------------------------

    private fun resolveState(
        context: Context,
        currentSong: Song?,
        isPlaying: Boolean?,
        currentPositionSec: Int?
    ): WState {
        val controller = PlaybackBridge.controller ?: PlaybackBridge.getOrInitController(context)
        val prefs = PreferencesManager(context)
        val saved = prefs.loadPlaybackState()

        val song = currentSong ?: controller.currentSong.value
        val valid = song.id != "none" && song.id != "empty_state"

        val title = if (valid) song.title else (saved.title ?: "Musicfy")
        val artist = if (valid) {
            song.artist
        } else {
            saved.artist
                ?: LocalizationManager.getStrings(prefs.loadSettings().language).defaultPlayerArtist
        }
        val dur = if (valid && song.durationSec > 0) song.durationSec else saved.durationSec
        val pos = currentPositionSec ?: controller.currentPositionSec.value
        return WState(
            title = title,
            artist = artist,
            posSec = pos.coerceIn(0, max(dur, 0)),
            durSec = dur,
            playing = isPlaying ?: controller.isPlaying.value,
            liked = if (valid) song.isLiked else saved.isLiked,
            artUri = if (valid) (song.customImageUri ?: song.albumArtUri) else saved.artUri,
            trackUri = if (valid) song.contentUri else saved.contentUri
        )
    }

    private fun loadArt(context: Context, s: WState): Bitmap? {
        if (s.artUri.isNullOrBlank()) {
            artKey = null
            artBitmap = null
            return null
        }
        val key = "${s.artUri}|${s.trackUri}"
        if (key == artKey && artBitmap != null) return artBitmap
        val bmp = ArtworkLoader.loadBitmap(context, s.artUri, s.trackUri, 640)
        artKey = if (bmp != null) key else null
        artBitmap = bmp
        return bmp
    }

    // ---------------------------------------------------------------------------------------
    // Rendering
    // ---------------------------------------------------------------------------------------

    private fun sizeFor(context: Context, manager: AppWidgetManager, id: Int, defW: Int, defH: Int): WSize {
        val o = manager.getAppWidgetOptions(id)
        val landscape = context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        var wDp = if (landscape) {
            o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 0)
        } else {
            o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 0)
        }
        var hDp = if (landscape) {
            o.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
        } else {
            o.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
        }
        if (wDp <= 0) wDp = defW
        if (hDp <= 0) hDp = defH

        val density = context.resources.displayMetrics.density
        var scale = density
        val longest = max(wDp, hDp) * density
        if (longest > 720f) scale = density * 720f / longest
        return WSize(
            w = max(64, (wDp * scale).roundToInt()),
            h = max(64, (hDp * scale).roundToInt()),
            pxPerDp = scale
        )
    }

    private fun renderGlass(context: Context, manager: AppWidgetManager, id: Int, s: WState, art: Bitmap?) {
        val size = sizeFor(context, manager, id, 320, 190)
        val key = "g|$artKey|${size.w}x${size.h}"
        val bg = bgCache.get(key)
            ?: WidgetBitmaps.glassBackground(art, size.w, size.h, size.pxPerDp).also { bgCache.put(key, it) }

        val views = RemoteViews(context.packageName, R.layout.widget_glass)
        views.setImageViewBitmap(R.id.w_bg, bg)

        if (art != null) {
            val thumb = thumbCache("t|$artKey|r") { WidgetBitmaps.roundedThumb(art, 192, 44f) }
            views.setImageViewBitmap(R.id.w_cover, thumb)
        } else {
            views.setImageViewResource(R.id.w_cover, R.drawable.ic_widget_default_note)
        }

        fillCommon(context, views, s)
        views.setImageViewResource(R.id.w_play, if (s.playing) R.drawable.ic_wg_pause else R.drawable.ic_wg_play)

        views.setOnClickPendingIntent(R.id.w_replay, broadcast(context, ACTION_REPLAY, 311))
        views.setOnClickPendingIntent(R.id.w_prev, broadcast(context, ACTION_PREV, 312))
        views.setOnClickPendingIntent(R.id.w_play, broadcast(context, ACTION_PLAY_PAUSE, 313))
        views.setOnClickPendingIntent(R.id.w_next, broadcast(context, ACTION_NEXT, 314))
        views.setOnClickPendingIntent(R.id.w_share, sharePendingIntent(context, 315))

        manager.updateAppWidget(id, views)
    }

    private fun renderCover(context: Context, manager: AppWidgetManager, id: Int, s: WState, art: Bitmap?) {
        val size = sizeFor(context, manager, id, 210, 210)
        val key = "c|$artKey|${size.w}x${size.h}"
        val bg = bgCache.get(key)
            ?: WidgetBitmaps.coverBackground(art, size.w, size.h, size.pxPerDp).also { bgCache.put(key, it) }

        val views = RemoteViews(context.packageName, R.layout.widget_cover)
        views.setImageViewBitmap(R.id.w_bg, bg)

        if (art != null) {
            val thumb = thumbCache("t|$artKey|c") { WidgetBitmaps.circleThumb(art, 128) }
            views.setImageViewBitmap(R.id.w_cover, thumb)
        } else {
            views.setImageViewResource(R.id.w_cover, R.drawable.ic_widget_default_note)
        }

        fillCommon(context, views, s)
        views.setImageViewResource(R.id.w_play, if (s.playing) R.drawable.ic_wg_pause else R.drawable.ic_wg_play)
        views.setImageViewResource(
            R.id.w_like,
            if (s.liked) R.drawable.ic_wg_heart_green else R.drawable.ic_wg_heart_outline
        )

        views.setOnClickPendingIntent(R.id.w_prev, broadcast(context, ACTION_PREV, 321))
        views.setOnClickPendingIntent(R.id.w_play, broadcast(context, ACTION_PLAY_PAUSE, 322))
        views.setOnClickPendingIntent(R.id.w_next, broadcast(context, ACTION_NEXT, 323))
        views.setOnClickPendingIntent(R.id.w_like, broadcast(context, ACTION_LIKE, 324))
        views.setOnClickPendingIntent(R.id.w_share, sharePendingIntent(context, 325))

        manager.updateAppWidget(id, views)
    }

    /** Title / artist / times / progress / seek targets / "open the app" (shared by both designs). */
    private fun fillCommon(context: Context, views: RemoteViews, s: WState) {
        views.setTextViewText(R.id.w_title, s.title)
        views.setTextViewText(R.id.w_artist, s.artist)
        views.setTextViewText(R.id.w_pos, formatTime(s.posSec))
        views.setTextViewText(R.id.w_dur, "-" + formatTime(max(0, s.durSec - s.posSec)))

        val progress = if (s.durSec > 0) {
            ((s.posSec.toFloat() / s.durSec.toFloat()) * 1000f).toInt().coerceIn(0, 1000)
        } else {
            0
        }
        views.setProgressBar(R.id.w_progress, 1000, progress, false)

        // 10 invisible touch zones across the progress bar: tap = jump to that spot
        for (i in SEEK_IDS.indices) {
            val fraction = (i + 0.5f) / SEEK_IDS.size
            views.setOnClickPendingIntent(SEEK_IDS[i], broadcast(context, ACTION_SEEK, 340 + i, fraction))
        }

        views.setOnClickPendingIntent(R.id.w_info, openAppPendingIntent(context, 300))
    }

    private val thumbs = object : LruCache<String, Bitmap>(2 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    private fun thumbCache(key: String, create: () -> Bitmap): Bitmap {
        return thumbs.get(key) ?: create().also { thumbs.put(key, it) }
    }

    // ---------------------------------------------------------------------------------------
    // PendingIntents
    // ---------------------------------------------------------------------------------------

    private val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    private fun broadcast(context: Context, action: String, requestCode: Int, fraction: Float? = null): PendingIntent {
        val intent = Intent(context, MusicGlassWidgetProvider::class.java).apply {
            this.action = action
            if (fraction != null) putExtra(EXTRA_FRACTION, fraction)
        }
        return PendingIntent.getBroadcast(context, requestCode, intent, FLAGS)
    }

    private fun sharePendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, WidgetShareActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_HISTORY)
        }
        return PendingIntent.getActivity(context, requestCode, intent, FLAGS)
    }

    private fun openAppPendingIntent(context: Context, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(context, requestCode, intent, FLAGS)
    }

    private fun formatTime(seconds: Int): String {
        val total = max(0, seconds)
        val m = total / 60
        val s = total % 60
        return if (s < 10) "$m:0$s" else "$m:$s"
    }
}
