package com.example.audio

import com.example.data.LocalizationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import com.example.data.MusicRepository
import com.example.data.Song
import com.example.data.local.PreferencesManager
import com.example.widget.MusicAppWidgetProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sin

class AudioPlayerController(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val preferencesManager = PreferencesManager(context)

    // Audio Focus & System Audio Manager
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    private var resumeOnFocusGain = false
    private var isNoisyReceiverRegistered = false

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent loss of audio focus (e.g. YouTube, Spotify, video player opened)
                hasAudioFocus = false
                resumeOnFocusGain = false
                pause(abandonFocus = true)
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Temporary loss (e.g. phone call, video preview)
                if (_isPlaying.value) {
                    resumeOnFocusGain = true
                    pause(abandonFocus = false)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // Brief interruption (e.g. notification chime) -> pause to avoid audio clash
                if (_isPlaying.value) {
                    resumeOnFocusGain = true
                    pause(abandonFocus = false)
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Regained focus after temporary pause
                hasAudioFocus = true
                if (resumeOnFocusGain) {
                    resumeOnFocusGain = false
                    play()
                }
            }
        }
    }

    private val becomingNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                // Headphones unplugged or Bluetooth disconnected -> pause immediately
                pause()
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AudioPlayerController? = null

        fun getInstance(context: Context): AudioPlayerController {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AudioPlayerController(context.applicationContext).also {
                    INSTANCE = it
                    PlaybackBridge.controller = it
                }
            }
        }
    }

    private val _currentSong = MutableStateFlow<Song>(MusicRepository.fallbackSong)
    val currentSong: StateFlow<Song> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionSec = MutableStateFlow(0)
    val currentPositionSec: StateFlow<Int> = _currentPositionSec.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isRepeat = MutableStateFlow(false)
    val isRepeat: StateFlow<Boolean> = _isRepeat.asStateFlow()

    private val _sleepTimerSeconds = MutableStateFlow<Int?>(null)
    val sleepTimerSeconds: StateFlow<Int?> = _sleepTimerSeconds.asStateFlow()

    // Playlist queue
    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()
    private var playlistQueue: List<Song>
        get() = _queue.value
        set(value) {
            _queue.value = value
        }

    private val priorityNextQueue = mutableListOf<Song>()

    // Equalizer Bands (-10 to +10 dB)
    private val _eqBands = MutableStateFlow(listOf(2, 4, 1, 3, 5))
    val eqBands: StateFlow<List<Int>> = _eqBands.asStateFlow()

    private val _activePreset = MutableStateFlow("Vocal & Bass")
    val activePreset: StateFlow<String> = _activePreset.asStateFlow()

    // Hardware Media Player for real device tracks
    private var mediaPlayer: MediaPlayer? = null

    // Audio synthesizer track for soft pleasant tone feedback when no media file is attached
    private var audioTrack: AudioTrack? = null
    private var toneJob: Job? = null

    init {
        INSTANCE = this
        PlaybackBridge.controller = this
        restoreFromPreferences()
        startPlaybackTicker()
    }

    fun setQueue(songs: List<Song>) {
        if (playlistQueue.isEmpty()) {
            playlistQueue = songs
        }
    }

    fun persistState() {
        try {
            val song = _currentSong.value
            if (song.id != "none" && song.id != "empty_state") {
                preferencesManager.savePlaybackState(
                    song = song,
                    positionSec = _currentPositionSec.value,
                    queueSongIds = playlistQueue.map { it.id },
                    isShuffle = _isShuffle.value,
                    isRepeat = _isRepeat.value
                )
            }
        } catch (_: Exception) { }
    }

    fun restoreFromPreferences() {
        if (_currentSong.value.id != "none" && _currentSong.value.id != "empty_state") {
            return
        }
        val savedState = preferencesManager.loadPlaybackState()
        if (!savedState.lastSongId.isNullOrBlank() && savedState.lastSongId != "none") {
            val song = Song(
                id = savedState.lastSongId,
                title = savedState.title ?: "Musicfy",
                artist = savedState.artist
                    ?: LocalizationManager.getStrings(preferencesManager.loadSettings().language).defaultPlayerArtist,
                durationSec = savedState.durationSec,
                contentUri = savedState.contentUri,
                albumArtUri = savedState.artUri,
                customImageUri = savedState.artUri,
                isLiked = savedState.isLiked
            )
            restorePlaybackState(
                song = song,
                queue = listOf(song),
                positionSec = savedState.lastPositionSec,
                isShuffle = savedState.isShuffle,
                isRepeat = savedState.isRepeat
            )
        }
    }

    fun restorePlaybackState(
        song: Song,
        queue: List<Song>,
        positionSec: Int = 0,
        isShuffle: Boolean = false,
        isRepeat: Boolean = false
    ) {
        _currentSong.value = song
        if (queue.isNotEmpty()) {
            playlistQueue = queue
        } else {
            playlistQueue = listOf(song)
        }
        _currentPositionSec.value = positionSec.coerceIn(0, song.durationSec.coerceAtLeast(1))
        _isShuffle.value = isShuffle
        _isRepeat.value = isRepeat
        _isPlaying.value = false
        releaseMediaPlayer()
        stopAudioTone()
        MusicAppWidgetProvider.updateAllWidgets(context, song, false, positionSec, isShuffle, isRepeat)
    }

    private fun startPlaybackTicker() {
        scope.launch {
            var tickCount = 0
            while (isActive) {
                delay(1000L)
                if (_isPlaying.value) {
                    val mp = mediaPlayer
                    if (mp != null && mp.isPlaying) {
                        try {
                            val pos = mp.currentPosition / 1000
                            if (_currentPositionSec.value != pos) {
                                _currentPositionSec.value = pos
                            }
                        } catch (_: Exception) { }
                    } else if (mp == null) {
                        val nextPos = _currentPositionSec.value + 1
                        val songDuration = _currentSong.value.durationSec
                        if (songDuration > 0 && nextPos >= songDuration) {
                            if (_isRepeat.value) {
                                _currentPositionSec.value = 0
                            } else {
                                playNext(autoAdvance = true)
                            }
                        } else {
                            _currentPositionSec.value = nextPos
                        }
                    }

                    // Handle sleep timer countdown
                    _sleepTimerSeconds.value?.let { rem ->
                        if (rem > 1) {
                            _sleepTimerSeconds.value = rem - 1
                        } else {
                            _sleepTimerSeconds.value = null
                            pause()
                        }
                    }

                    // Persist position and sync notification scrubber and widget.
                    // PERF: this loop runs on the Main thread every second. Rebuilding the
                    // notification + widget (and decoding cover bitmaps) every second caused
                    // visible scroll jank, so we only do it every 5 seconds. The notification
                    // scrubber extrapolates the position by itself (playback speed = 1.0).
                    tickCount++
                    if (tickCount % 5 == 0) {
                        preferencesManager.saveLastPosition(_currentPositionSec.value)
                        updateForegroundService()
                    }
                }
            }
        }
    }

    fun updateForegroundService() {
        val song = _currentSong.value
        if (song.id == "none") return
        val art = song.customImageUri ?: song.albumArtUri
        val durMs = song.durationSec.toLong() * 1000L
        val posMs = _currentPositionSec.value.toLong() * 1000L
        try {
            MusicPlaybackService.startService(
                context = context,
                title = song.title,
                artist = song.artist,
                isPlaying = _isPlaying.value,
                artUri = art,
                contentUri = song.contentUri,
                durationMs = durMs,
                positionMs = posMs,
                isLiked = song.isLiked
            )
        } catch (_: Exception) { }

        try {
            MusicAppWidgetProvider.updateAllWidgets(
                context = context,
                currentSong = song,
                isPlaying = _isPlaying.value,
                currentPositionSec = _currentPositionSec.value,
                isShuffle = _isShuffle.value,
                isRepeat = _isRepeat.value
            )
        } catch (_: Exception) { }
    }

    fun setShuffle(enabled: Boolean) {
        _isShuffle.value = enabled
        persistState()
        updateForegroundService()
    }

    private fun initAndStartMediaPlayer(startPositionSec: Int = 0) {
        releaseMediaPlayer()
        stopAudioTone()

        val song = _currentSong.value
        val uriStr = song.contentUri
        if (!uriStr.isNullOrBlank()) {
            try {
                val uri = Uri.parse(uriStr)
                val mp = MediaPlayer().apply {
                    setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(context, uri)
                    prepare()
                    if (startPositionSec > 0) {
                        val targetMs = (startPositionSec * 1000).coerceIn(0, duration.coerceAtLeast(1))
                        seekTo(targetMs)
                    }
                    start()
                    setOnCompletionListener {
                        if (_isRepeat.value) {
                            seekTo(0)
                            start()
                        } else {
                            playNext(autoAdvance = true)
                        }
                    }
                    setOnErrorListener { _, _, _ ->
                        startAudioTone()
                        true
                    }
                }
                mediaPlayer = mp
            } catch (e: Exception) {
                e.printStackTrace()
                startAudioTone()
            }
        } else {
            startAudioTone()
        }
        updateForegroundService()
    }

    private fun registerNoisyReceiver() {
        if (!isNoisyReceiverRegistered) {
            try {
                val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                context.registerReceiver(becomingNoisyReceiver, filter)
                isNoisyReceiverRegistered = true
            } catch (_: Exception) {}
        }
    }

    private fun unregisterNoisyReceiver() {
        if (isNoisyReceiverRegistered) {
            try {
                context.unregisterReceiver(becomingNoisyReceiver)
                isNoisyReceiverRegistered = false
            } catch (_: Exception) {}
        }
    }

    private fun requestAudioFocus(): Boolean {
        val am = audioManager ?: return true
        if (hasAudioFocus) return true

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioFocusRequest = request
            am.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            am.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        hasAudioFocus = (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
        return hasAudioFocus
    }

    private fun abandonAudioFocus() {
        val am = audioManager ?: return
        if (!hasAudioFocus) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(audioFocusChangeListener)
            }
        } catch (_: Exception) {}
        hasAudioFocus = false
        resumeOnFocusGain = false
    }

    fun playSong(song: Song, contextQueue: List<Song>? = null) {
        if (!requestAudioFocus()) {
            return
        }
        registerNoisyReceiver()

        _currentSong.value = song
        _currentPositionSec.value = 0
        _isPlaying.value = true

        if (contextQueue != null && contextQueue.isNotEmpty()) {
            playlistQueue = contextQueue
            priorityNextQueue.clear()
        } else if (!playlistQueue.any { it.id == song.id }) {
            playlistQueue = listOf(song) + playlistQueue
        }

        persistState()
        initAndStartMediaPlayer(0)
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun play() {
        if (!requestAudioFocus()) {
            return
        }
        registerNoisyReceiver()

        _isPlaying.value = true
        val mp = mediaPlayer
        if (mp != null) {
            try {
                mp.start()
            } catch (_: Exception) {
                initAndStartMediaPlayer(_currentPositionSec.value)
            }
        } else if (_currentSong.value.id != "none" && _currentSong.value.id != "empty_state") {
            initAndStartMediaPlayer(_currentPositionSec.value)
        } else {
            startAudioTone()
        }
        persistState()
        updateForegroundService()
    }

    fun pause(abandonFocus: Boolean = true) {
        _isPlaying.value = false
        if (abandonFocus) {
            abandonAudioFocus()
        }
        unregisterNoisyReceiver()

        val mp = mediaPlayer
        if (mp != null) {
            try {
                mp.pause()
            } catch (_: Exception) { }
        }
        stopAudioTone()
        persistState()
        updateForegroundService()
    }

    fun seekTo(seconds: Int) {
        val clamped = seconds.coerceIn(0, _currentSong.value.durationSec.coerceAtLeast(1))
        _currentPositionSec.value = clamped
        mediaPlayer?.let { mp ->
            try {
                mp.seekTo(clamped * 1000)
            } catch (_: Exception) { }
        }
        persistState()
        updateForegroundService()
    }

    fun seekBy(deltaSeconds: Int) {
        val target = (_currentPositionSec.value + deltaSeconds).coerceIn(0, _currentSong.value.durationSec.coerceAtLeast(1))
        seekTo(target)
    }

    fun seekToFraction(fraction: Float) {
        val duration = _currentSong.value.durationSec
        if (duration > 0) {
            val target = (fraction * duration).toInt().coerceIn(0, duration)
            seekTo(target)
        }
    }

    fun playPlaylist(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (songs.isEmpty()) return
        priorityNextQueue.clear()
        if (shuffle) {
            val shuffled = songs.shuffled()
            playlistQueue = shuffled
            _isShuffle.value = true
            playSong(shuffled[0], shuffled)
        } else {
            playlistQueue = songs
            _isShuffle.value = false
            val idx = startIndex.coerceIn(0, songs.size - 1)
            playSong(songs[idx], songs)
        }
    }

    fun playNext(autoAdvance: Boolean = false) {
        // First check if a song was explicitly prioritized via "التشغيل التالي"
        if (priorityNextQueue.isNotEmpty()) {
            val nextSong = priorityNextQueue.removeAt(0)
            playSong(nextSong)
            return
        }

        if (playlistQueue.isEmpty()) return
        val currentIdx = playlistQueue.indexOfFirst { it.id == _currentSong.value.id }
        if (currentIdx >= 0) {
            if (_isRepeat.value) {
                seekTo(0)
                play()
                return
            }

            if (_isShuffle.value && playlistQueue.size > 1) {
                val nextIdx = (playlistQueue.indices).filter { it != currentIdx }.random()
                playSong(playlistQueue[nextIdx])
                return
            }

            val nextIdx = currentIdx + 1
            if (nextIdx < playlistQueue.size) {
                playSong(playlistQueue[nextIdx])
            } else {
                // Reached the end of the playlist -> loop back to the beginning
                playSong(playlistQueue.first())
            }
        } else if (playlistQueue.isNotEmpty()) {
            playSong(playlistQueue.first())
        }
    }

    fun playNextInQueue(song: Song) {
        moveQueueSongToTop(song.id)
    }

    fun updateCurrentSongData(updated: Song) {
        if (_currentSong.value.id == updated.id) {
            _currentSong.value = updated
            updateForegroundService()
        }
        playlistQueue = playlistQueue.map { if (it.id == updated.id) updated else it }
        persistState()
    }

    fun removeSongFromQueue(songId: String) {
        val wasCurrent = _currentSong.value.id == songId
        val current = _currentSong.value
        val upcoming = playlistQueue.filter { it.id != current.id && it.id != songId }
        priorityNextQueue.removeAll { it.id == songId }
        
        if (wasCurrent) {
            if (upcoming.isNotEmpty()) {
                playlistQueue = upcoming
                playSong(upcoming[0])
            } else {
                playlistQueue = emptyList()
                pause()
                _currentSong.value = MusicRepository.fallbackSong
            }
        } else {
            playlistQueue = listOf(current) + upcoming
        }
        persistState()
    }

    fun setUpcomingQueue(newUpcoming: List<Song>) {
        _isShuffle.value = false
        priorityNextQueue.clear()
        val current = _currentSong.value
        val distinct = newUpcoming.filter { it.id != current.id }
        playlistQueue = listOf(current) + distinct
        persistState()
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        reorderUpcomingQueue(fromIndex, toIndex)
    }

    fun reorderUpcomingQueue(fromIndex: Int, toIndex: Int) {
        val current = _currentSong.value
        val upcoming = playlistQueue.filter { it.id != current.id }.toMutableList()
        if (fromIndex in upcoming.indices && toIndex in upcoming.indices && fromIndex != toIndex) {
            val item = upcoming.removeAt(fromIndex)
            upcoming.add(toIndex, item)
            _isShuffle.value = false
            priorityNextQueue.clear()
            playlistQueue = listOf(current) + upcoming
            persistState()
        }
    }

    fun moveQueueSongUp(songId: String) {
        val current = _currentSong.value
        val upcoming = playlistQueue.filter { it.id != current.id }.toMutableList()
        val index = upcoming.indexOfFirst { it.id == songId }
        if (index > 0) {
            val item = upcoming.removeAt(index)
            upcoming.add(index - 1, item)
            _isShuffle.value = false
            priorityNextQueue.clear()
            playlistQueue = listOf(current) + upcoming
            persistState()
        }
    }

    fun moveQueueSongDown(songId: String) {
        val current = _currentSong.value
        val upcoming = playlistQueue.filter { it.id != current.id }.toMutableList()
        val index = upcoming.indexOfFirst { it.id == songId }
        if (index >= 0 && index < upcoming.size - 1) {
            val item = upcoming.removeAt(index)
            upcoming.add(index + 1, item)
            _isShuffle.value = false
            priorityNextQueue.clear()
            playlistQueue = listOf(current) + upcoming
            persistState()
        }
    }

    fun moveQueueSongToTop(songId: String) {
        val current = _currentSong.value
        val upcoming = playlistQueue.filter { it.id != current.id }.toMutableList()
        val song = upcoming.find { it.id == songId } ?: playlistQueue.find { it.id == songId } ?: return
        upcoming.removeAll { it.id == songId }
        upcoming.add(0, song)
        _isShuffle.value = false
        priorityNextQueue.clear()
        playlistQueue = listOf(current) + upcoming
        persistState()
    }

    fun playPrevious() {
        if (_currentPositionSec.value > 3) {
            seekTo(0)
            return
        }
        if (playlistQueue.isEmpty()) return
        val currentIdx = playlistQueue.indexOfFirst { it.id == _currentSong.value.id }
        if (currentIdx >= 0) {
            val prevIdx = if (currentIdx - 1 < 0) playlistQueue.size - 1 else currentIdx - 1
            playSong(playlistQueue[prevIdx])
        } else {
            playSong(playlistQueue.last())
        }
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
        persistState()
        updateForegroundService()
    }

    fun toggleRepeat() {
        _isRepeat.value = !_isRepeat.value
        persistState()
        updateForegroundService()
    }

    fun setSleepTimer(minutes: Int?) {
        if (minutes == null || minutes <= 0) {
            _sleepTimerSeconds.value = null
        } else {
            _sleepTimerSeconds.value = minutes * 60
        }
    }

    fun setSleepTimerToEndOfTrack() {
        val rem = (_currentSong.value.durationSec - _currentPositionSec.value).coerceAtLeast(5)
        _sleepTimerSeconds.value = rem
    }

    fun updateEqBand(index: Int, value: Int) {
        val current = _eqBands.value.toMutableList()
        if (index in current.indices) {
            current[index] = value.coerceIn(-10, 10)
            _eqBands.value = current
            _activePreset.value = "Custom"
        }
    }

    fun applyPreset(presetName: String) {
        _activePreset.value = presetName
        when (presetName) {
            "Bass Boost" -> _eqBands.value = listOf(8, 6, 2, 0, -2)
            "Vocal & Bass" -> _eqBands.value = listOf(4, 5, 2, 4, 6)
            "Rock" -> _eqBands.value = listOf(5, 3, -1, 3, 6)
            "Electronic" -> _eqBands.value = listOf(7, 5, 0, 4, 7)
            "Flat" -> _eqBands.value = listOf(0, 0, 0, 0, 0)
        }
    }

    private fun startAudioTone() {
        toneJob?.cancel()
        toneJob = scope.launch(Dispatchers.Default) {
            try {
                val sampleRate = 22050
                val bufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (bufferSize <= 0) return@launch

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack = track
                track.play()

                val buffer = ShortArray(bufferSize)
                var phase = 0.0
                val freq = 220.0
                val volume = 0.08

                while (isActive && _isPlaying.value && mediaPlayer == null) {
                    for (i in buffer.indices) {
                        val sample = (sin(phase) * 32767 * volume).toInt().toShort()
                        buffer[i] = sample
                        phase += 2.0 * Math.PI * freq / sampleRate
                        if (phase > 2.0 * Math.PI) phase -= 2.0 * Math.PI
                    }
                    track.write(buffer, 0, buffer.size)
                    delay(30L)
                }
            } catch (_: Exception) { }
        }
    }

    private fun stopAudioTone() {
        toneJob?.cancel()
        toneJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        } catch (_: Exception) { }
        audioTrack = null
    }

    private fun releaseMediaPlayer() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (_: Exception) { }
        mediaPlayer = null
    }

    fun release() {
        unregisterNoisyReceiver()
        abandonAudioFocus()
        scope.cancel()
        releaseMediaPlayer()
        stopAudioTone()
        try {
            MusicPlaybackService.stopService(context)
        } catch (_: Exception) { }
        if (INSTANCE == this) {
            INSTANCE = null
            PlaybackBridge.controller = null
        }
    }
}
