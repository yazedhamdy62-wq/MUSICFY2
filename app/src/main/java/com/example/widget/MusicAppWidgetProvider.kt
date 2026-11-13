package com.example.widget

import com.example.data.LocalizationManager
import com.example.data.ArtworkLoader
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.audio.PlaybackBridge
import com.example.data.Song
import com.example.data.local.PreferencesManager

class MusicAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateAllWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return

        when (action) {
            ACTION_PLAY_PAUSE -> {
                PlaybackBridge.onTogglePlayPause(context)
                updateAllWidgets(context)
            }
            ACTION_NEXT -> {
                PlaybackBridge.onNext(context)
                updateAllWidgets(context)
            }
            ACTION_PREV -> {
                PlaybackBridge.onPrevious(context)
                updateAllWidgets(context)
            }
            ACTION_SHUFFLE -> {
                PlaybackBridge.onToggleShuffle(context)
                updateAllWidgets(context)
            }
            ACTION_REPEAT -> {
                PlaybackBridge.onToggleRepeat(context)
                updateAllWidgets(context)
            }
            ACTION_LIKE -> {
                PlaybackBridge.onToggleLike(context)
                updateAllWidgets(context)
            }
            ACTION_SEEK_FRACTION -> {
                val fraction = intent.getFloatExtra(EXTRA_FRACTION, 0f)
                PlaybackBridge.onSeekToFraction(fraction, context)
                updateAllWidgets(context)
            }
            ACTION_SEEK_BY -> {
                val delta = intent.getIntExtra(EXTRA_DELTA_SEC, 0)
                PlaybackBridge.onSeekBy(delta, context)
                updateAllWidgets(context)
            }
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.example.action.WIDGET_PLAY_PAUSE"
        const val ACTION_NEXT = "com.example.action.WIDGET_NEXT"
        const val ACTION_PREV = "com.example.action.WIDGET_PREV"
        const val ACTION_SHUFFLE = "com.example.action.WIDGET_SHUFFLE"
        const val ACTION_REPEAT = "com.example.action.WIDGET_REPEAT"
        const val ACTION_LIKE = "com.example.action.WIDGET_LIKE"
        const val ACTION_SEEK_FRACTION = "com.example.action.WIDGET_SEEK_FRACTION"
        const val ACTION_SEEK_BY = "com.example.action.WIDGET_SEEK_BY"

        const val EXTRA_FRACTION = "extra_fraction"
        const val EXTRA_DELTA_SEC = "extra_delta_sec"

        fun updateAllWidgets(
            context: Context,
            currentSong: Song? = null,
            isPlaying: Boolean? = null,
            currentPositionSec: Int? = null,
            isShuffle: Boolean? = null,
            isRepeat: Boolean? = null
        ) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, MusicAppWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds.isEmpty()) return

            val controller = PlaybackBridge.controller ?: PlaybackBridge.getOrInitController(context)
            val prefs = PreferencesManager(context)
            val savedState = prefs.loadPlaybackState()

            val song = currentSong ?: controller?.currentSong?.value
            val playing = isPlaying ?: controller?.isPlaying?.value ?: false
            val posSec = currentPositionSec ?: controller?.currentPositionSec?.value ?: savedState.lastPositionSec
            val shuffle = isShuffle ?: controller?.isShuffle?.value ?: savedState.isShuffle
            val repeat = isRepeat ?: controller?.isRepeat?.value ?: savedState.isRepeat

            val title = if (song != null && song.id != "none" && song.id != "empty_state") {
                song.title
            } else {
                savedState.title ?: "Musicfy"
            }

            val artist = if (song != null && song.id != "none" && song.id != "empty_state") {
                song.artist
            } else {
                savedState.artist
                    ?: LocalizationManager.getStrings(PreferencesManager(context).loadSettings().language).defaultPlayerArtist
            }

            val durSec = if (song != null && song.durationSec > 0) {
                song.durationSec
            } else {
                savedState.durationSec
            }

            val isLiked = song?.isLiked ?: savedState.isLiked

            val artUri = song?.customImageUri ?: song?.albumArtUri ?: savedState.artUri
            val trackUri = song?.contentUri ?: savedState.contentUri
            val coverBitmap = loadArtworkBitmap(context, artUri, trackUri)

            val formattedPos = formatTime(posSec)
            val formattedDur = formatTime(durSec)
            val progressVal = if (durSec > 0) {
                ((posSec.toFloat() / durSec.toFloat()) * 1000).toInt().coerceIn(0, 1000)
            } else 0

            for (widgetId in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_music_player)

                views.setTextViewText(R.id.widget_song_title, title)
                views.setTextViewText(R.id.widget_song_artist, artist)
                views.setTextViewText(R.id.widget_pos_text, formattedPos)
                views.setTextViewText(R.id.widget_dur_text, formattedDur)
                views.setProgressBar(R.id.widget_progress_bar, 1000, progressVal, false)

                // Cover Art
                if (coverBitmap != null) {
                    views.setImageViewBitmap(R.id.widget_cover_art, coverBitmap)
                } else {
                    views.setImageViewResource(R.id.widget_cover_art, R.drawable.ic_widget_default_note)
                }

                // Play / Pause Icon
                if (playing) {
                    views.setImageViewResource(R.id.widget_btn_play_pause, R.drawable.ic_widget_pause)
                } else {
                    views.setImageViewResource(R.id.widget_btn_play_pause, R.drawable.ic_widget_play)
                }

                // Like Icon (Green if liked, White if unliked)
                if (isLiked) {
                    views.setImageViewResource(R.id.widget_btn_like, R.drawable.ic_widget_heart_green)
                } else {
                    views.setImageViewResource(R.id.widget_btn_like, R.drawable.ic_widget_heart_white)
                }

                // Shuffle Icon (Green if active, White if off)
                if (shuffle) {
                    views.setImageViewResource(R.id.widget_btn_shuffle, R.drawable.ic_widget_shuffle_green)
                } else {
                    views.setImageViewResource(R.id.widget_btn_shuffle, R.drawable.ic_widget_shuffle_white)
                }

                // Repeat Icon (Green if active, White if off)
                if (repeat) {
                    views.setImageViewResource(R.id.widget_btn_repeat, R.drawable.ic_widget_repeat_green)
                } else {
                    views.setImageViewResource(R.id.widget_btn_repeat, R.drawable.ic_widget_repeat_white)
                }

                // Pending Intents for All Controls
                views.setOnClickPendingIntent(
                    R.id.widget_btn_play_pause,
                    createBroadcastPendingIntent(context, ACTION_PLAY_PAUSE, 101)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_btn_next,
                    createBroadcastPendingIntent(context, ACTION_NEXT, 102)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_btn_prev,
                    createBroadcastPendingIntent(context, ACTION_PREV, 103)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_btn_shuffle,
                    createBroadcastPendingIntent(context, ACTION_SHUFFLE, 104)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_btn_repeat,
                    createBroadcastPendingIntent(context, ACTION_REPEAT, 105)
                )
                views.setOnClickPendingIntent(
                    R.id.widget_btn_like,
                    createBroadcastPendingIntent(context, ACTION_LIKE, 106)
                )

                // 16 High-Precision Seek Touch Targets across the entire timeline
                views.setOnClickPendingIntent(R.id.widget_seek_01, createSeekFractionPendingIntent(context, 0.03f, 201))
                views.setOnClickPendingIntent(R.id.widget_seek_02, createSeekFractionPendingIntent(context, 0.09f, 202))
                views.setOnClickPendingIntent(R.id.widget_seek_03, createSeekFractionPendingIntent(context, 0.15f, 203))
                views.setOnClickPendingIntent(R.id.widget_seek_04, createSeekFractionPendingIntent(context, 0.21f, 204))
                views.setOnClickPendingIntent(R.id.widget_seek_05, createSeekFractionPendingIntent(context, 0.28f, 205))
                views.setOnClickPendingIntent(R.id.widget_seek_06, createSeekFractionPendingIntent(context, 0.34f, 206))
                views.setOnClickPendingIntent(R.id.widget_seek_07, createSeekFractionPendingIntent(context, 0.40f, 207))
                views.setOnClickPendingIntent(R.id.widget_seek_08, createSeekFractionPendingIntent(context, 0.47f, 208))
                views.setOnClickPendingIntent(R.id.widget_seek_09, createSeekFractionPendingIntent(context, 0.53f, 209))
                views.setOnClickPendingIntent(R.id.widget_seek_10, createSeekFractionPendingIntent(context, 0.60f, 210))
                views.setOnClickPendingIntent(R.id.widget_seek_11, createSeekFractionPendingIntent(context, 0.66f, 211))
                views.setOnClickPendingIntent(R.id.widget_seek_12, createSeekFractionPendingIntent(context, 0.72f, 212))
                views.setOnClickPendingIntent(R.id.widget_seek_13, createSeekFractionPendingIntent(context, 0.78f, 213))
                views.setOnClickPendingIntent(R.id.widget_seek_14, createSeekFractionPendingIntent(context, 0.85f, 214))
                views.setOnClickPendingIntent(R.id.widget_seek_15, createSeekFractionPendingIntent(context, 0.91f, 215))
                views.setOnClickPendingIntent(R.id.widget_seek_16, createSeekFractionPendingIntent(context, 0.97f, 216))

                // +/- 5 Seconds Fine-Adjustment Rewind & Fast-Forward
                views.setOnClickPendingIntent(R.id.widget_rewind_wrapper, createSeekByPendingIntent(context, -5, 220))
                views.setOnClickPendingIntent(R.id.widget_pos_text, createSeekByPendingIntent(context, -5, 221))
                views.setOnClickPendingIntent(R.id.widget_forward_wrapper, createSeekByPendingIntent(context, 5, 222))
                views.setOnClickPendingIntent(R.id.widget_dur_text, createSeekByPendingIntent(context, 5, 223))

                // Clicking Cover or Song Info opens MainActivity
                val openAppIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val openAppPendingIntent = PendingIntent.getActivity(context, 100, openAppIntent, flags)
                views.setOnClickPendingIntent(R.id.widget_cover_art, openAppPendingIntent)
                views.setOnClickPendingIntent(R.id.widget_info_container, openAppPendingIntent)

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }

        private fun createBroadcastPendingIntent(context: Context, action: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, MusicAppWidgetProvider::class.java).apply {
                this.action = action
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            return PendingIntent.getBroadcast(context, requestCode, intent, flags)
        }

        private fun createSeekFractionPendingIntent(context: Context, fraction: Float, requestCode: Int): PendingIntent {
            val intent = Intent(context, MusicAppWidgetProvider::class.java).apply {
                this.action = ACTION_SEEK_FRACTION
                putExtra(EXTRA_FRACTION, fraction)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            return PendingIntent.getBroadcast(context, requestCode, intent, flags)
        }

        private fun createSeekByPendingIntent(context: Context, deltaSec: Int, requestCode: Int): PendingIntent {
            val intent = Intent(context, MusicAppWidgetProvider::class.java).apply {
                this.action = ACTION_SEEK_BY
                putExtra(EXTRA_DELTA_SEC, deltaSec)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            return PendingIntent.getBroadcast(context, requestCode, intent, flags)
        }

        private fun formatTime(seconds: Int): String {
            val m = seconds / 60
            val s = seconds % 60
            return if (s < 10) "$m:0$s" else "$m:$s"
        }

        // PERF: remember the last decoded cover so we don't decode it on every widget update
        @Volatile private var cachedArtKey: String? = null
        @Volatile private var cachedArtBitmap: Bitmap? = null

        private fun loadArtworkBitmap(context: Context, artUriStr: String?, trackUriStr: String?): Bitmap? {
            if (artUriStr.isNullOrBlank()) return null
            val key = "$artUriStr|$trackUriStr"
            if (key == cachedArtKey && cachedArtBitmap != null) return cachedArtBitmap
            val bmp = ArtworkLoader.loadBitmap(context, artUriStr, trackUriStr, 400)
            if (bmp != null) {
                cachedArtKey = key
                cachedArtBitmap = bmp
            }
            return bmp
        }
    }
}
