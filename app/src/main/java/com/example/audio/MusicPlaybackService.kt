package com.example.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.example.MainActivity
import com.example.R

/**
 * Foreground Service for persistent background & lockscreen music playback.
 * Displays standard media controls in notifications and lockscreen,
 * including a scrubber seekbar/timeline and quick favorite/like button.
 */
class MusicPlaybackService : Service() {

    private val binder = MusicBinder()
    private var mediaSession: MediaSessionCompat? = null

    inner class MusicBinder : Binder() {
        fun getService(): MusicPlaybackService = this@MusicPlaybackService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        mediaSession = MediaSessionCompat(this, "MusicfyMediaSession").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    PlaybackBridge.onPlay()
                }

                override fun onPause() {
                    PlaybackBridge.onPause()
                }

                override fun onSkipToNext() {
                    PlaybackBridge.onNext()
                }

                override fun onSkipToPrevious() {
                    PlaybackBridge.onPrevious()
                }

                override fun onSeekTo(pos: Long) {
                    PlaybackBridge.onSeekTo(pos)
                }

                override fun onCustomAction(action: String?, extras: android.os.Bundle?) {
                    if (action == ACTION_LIKE) {
                        PlaybackBridge.onToggleLike()
                    }
                }

                override fun onStop() {
                    PlaybackBridge.onStop()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_TOGGLE -> PlaybackBridge.onTogglePlayPause()
            ACTION_PLAY -> PlaybackBridge.onPlay()
            ACTION_PAUSE -> PlaybackBridge.onPause()
            ACTION_NEXT -> PlaybackBridge.onNext()
            ACTION_PREVIOUS -> PlaybackBridge.onPrevious()
            ACTION_LIKE -> PlaybackBridge.onToggleLike()
            ACTION_STOP -> {
                PlaybackBridge.onStop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        val title = intent?.getStringExtra(EXTRA_SONG_TITLE) ?: "Musicfy"
        val artist = intent?.getStringExtra(EXTRA_SONG_ARTIST) ?: "مشغل الموسيقى"
        val isPlaying = intent?.getBooleanExtra(EXTRA_IS_PLAYING, true) ?: true
        val artUriStr = intent?.getStringExtra(EXTRA_ART_URI)
        val durationMs = intent?.getLongExtra(EXTRA_DURATION_MS, 0L) ?: 0L
        val positionMs = intent?.getLongExtra(EXTRA_POSITION_MS, 0L) ?: 0L
        val isLiked = intent?.getBooleanExtra(EXTRA_IS_LIKED, false) ?: false

        val albumArtBitmap = loadArtworkBitmap(artUriStr)

        // Update MediaSession metadata (sets song duration for scrubber / timeline)
        val metadata = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, durationMs)
            .apply {
                if (albumArtBitmap != null) {
                    putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArtBitmap)
                }
            }
            .build()
        mediaSession?.setMetadata(metadata)

        // Update MediaSession state with ACTION_SEEK_TO for interactive timeline
        val state = if (isPlaying) PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
        mediaSession?.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_STOP or
                    PlaybackStateCompat.ACTION_SEEK_TO
                )
                .setState(state, positionMs, if (isPlaying) 1.0f else 0.0f)
                .build()
        )

        val notification = buildNotification(title, artist, isPlaying, albumArtBitmap, isLiked)
        startForeground(NOTIFICATION_ID, notification)

        return START_STICKY
    }

    private fun buildNotification(
        title: String,
        artist: String,
        isPlaying: Boolean,
        albumArtBitmap: Bitmap?,
        isLiked: Boolean
    ): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val prevPendingIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_PREVIOUS },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val togglePendingIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_TOGGLE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextPendingIntent = PendingIntent.getService(
            this,
            3,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_NEXT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val likePendingIntent = PendingIntent.getService(
            this,
            4,
            Intent(this, MusicPlaybackService::class.java).apply { action = ACTION_LIKE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val playPauseIcon = if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        val playPauseTitle = if (isPlaying) "Pause" else "Play"

        val likeIcon = if (isLiked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
        val likeTitle = if (isLiked) "Unlike" else "Like"

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(artist)
            .setSubText("Musicfy")
            .setSmallIcon(R.drawable.ic_musicfy_logo)
            .setLargeIcon(albumArtBitmap)
            .setContentIntent(contentPendingIntent)
            .setOngoing(isPlaying)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(likeIcon, likeTitle, likePendingIntent)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            .addAction(playPauseIcon, playPauseTitle, togglePendingIntent)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        return builder.build()
    }

    // PERF: remember the last decoded cover so we don't decode the same image on every update
    private var cachedArtUri: String? = null
    private var cachedArtBitmap: Bitmap? = null

    private fun loadArtworkBitmap(artUriStr: String?): Bitmap? {
        if (artUriStr.isNullOrBlank()) return null
        if (artUriStr == cachedArtUri && cachedArtBitmap != null) return cachedArtBitmap
        try {
            contentResolver.openInputStream(Uri.parse(artUriStr))?.use { input ->
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 4
                }
                val bmp = BitmapFactory.decodeStream(input, null, options)
                cachedArtUri = artUriStr
                cachedArtBitmap = bmp
                return bmp
            }
        } catch (_: Exception) { }
        return null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "تشغيل الموسيقى - Musicfy",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "إشعار التحكم في تشغيل الموسيقى وشاشة القفل"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        mediaSession?.release()
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "musicfy_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val EXTRA_SONG_TITLE = "extra_song_title"
        const val EXTRA_SONG_ARTIST = "extra_song_artist"
        const val EXTRA_IS_PLAYING = "extra_is_playing"
        const val EXTRA_ART_URI = "extra_art_uri"
        const val EXTRA_DURATION_MS = "extra_duration_ms"
        const val EXTRA_POSITION_MS = "extra_position_ms"
        const val EXTRA_IS_LIKED = "extra_is_liked"

        const val ACTION_TOGGLE = "com.example.action.TOGGLE"
        const val ACTION_PLAY = "com.example.action.PLAY"
        const val ACTION_PAUSE = "com.example.action.PAUSE"
        const val ACTION_NEXT = "com.example.action.NEXT"
        const val ACTION_PREVIOUS = "com.example.action.PREVIOUS"
        const val ACTION_LIKE = "com.example.action.LIKE"
        const val ACTION_STOP = "com.example.action.STOP"

        fun startService(
            context: Context,
            title: String,
            artist: String,
            isPlaying: Boolean,
            artUri: String? = null,
            durationMs: Long = 0L,
            positionMs: Long = 0L,
            isLiked: Boolean = false
        ) {
            val intent = Intent(context, MusicPlaybackService::class.java).apply {
                putExtra(EXTRA_SONG_TITLE, title)
                putExtra(EXTRA_SONG_ARTIST, artist)
                putExtra(EXTRA_IS_PLAYING, isPlaying)
                putExtra(EXTRA_ART_URI, artUri)
                putExtra(EXTRA_DURATION_MS, durationMs)
                putExtra(EXTRA_POSITION_MS, positionMs)
                putExtra(EXTRA_IS_LIKED, isLiked)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (_: Exception) { }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, MusicPlaybackService::class.java)
            context.stopService(intent)
        }
    }
}
