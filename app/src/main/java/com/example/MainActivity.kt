package com.example

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.audio.AudioPlayerController
import com.example.audio.PlaybackBridge
import com.example.data.*
import com.example.data.local.LocalMusicRepository
import com.example.data.local.PreferencesManager
import com.example.ui.components.FloatingBottomNavBar
import com.example.ui.components.AppNavDestination
import com.example.ui.components.FullscreenPlayerSheet
import com.example.ui.components.MiniPlayerBar
import com.example.ui.dialogs.AddSongsToPlaylistDialog
import com.example.ui.dialogs.EqualizerDialog
import com.example.ui.dialogs.QueueBottomSheet
import com.example.ui.dialogs.SleepTimerDialog
import com.example.ui.dialogs.SongOptionsBottomSheet
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.example.ui.screens.FoldersScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PlaylistDetailScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MusicfyTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class Screen(val destination: AppNavDestination) {
    object Home : Screen(AppNavDestination.HOME)
    object Folders : Screen(AppNavDestination.FOLDERS)
    object Library : Screen(AppNavDestination.LIBRARY)
    object Settings : Screen(AppNavDestination.SETTINGS)
    data class PlaylistDetail(val playlist: Playlist) : Screen(AppNavDestination.LIBRARY)
}

class MainActivity : ComponentActivity() {
    private lateinit var audioPlayerController: AudioPlayerController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        audioPlayerController = AudioPlayerController.getInstance(applicationContext)

        // Initialize fast singleton Coil ImageLoader with hardware bitmaps and caching
        val imageLoader = coil.ImageLoader.Builder(applicationContext)
            .memoryCache {
                coil.memory.MemoryCache.Builder(applicationContext)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(applicationContext.cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .allowHardware(true)
            .crossfade(false)
            .components {
                add(com.example.ui.components.HighResAlbumArtKeyer)
                add(com.example.ui.components.HighResAlbumArtFetcher.Factory(applicationContext))
            }
            .build()
        coil.Coil.setImageLoader(imageLoader)

        setContent {
            MusicfyApp(audioPlayerController)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Do not stop audio if playback is ongoing in the background/service/widget!
        if (isFinishing && !audioPlayerController.isPlaying.value) {
            audioPlayerController.release()
        }
    }
}

@Composable
fun MusicfyApp(audioPlayer: AudioPlayerController) {
    val context = LocalContext.current
    val musicRepository = remember { LocalMusicRepository(context) }
    val preferencesManager = remember { PreferencesManager(context) }

    var songsList by remember { mutableStateOf<List<Song>>(emptyList()) }
    var playlistsList by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
    var selectedPlaylistDetail by remember { mutableStateOf<Playlist?>(null) }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Home) }
    var previousScreen by remember { mutableStateOf<Screen?>(null) }
    var isPlayerExpanded by remember { mutableStateOf(false) }

    var showEqualizer by remember { mutableStateOf(false) }
    var showSleepTimer by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var playlistForAddSongs by remember { mutableStateOf<Playlist?>(null) }

    var playerSettings by remember { mutableStateOf(preferencesManager.loadSettings()) }
    var songForOptions by remember { mutableStateOf<Song?>(null) }
    var playlistForSongOptions by remember { mutableStateOf<Playlist?>(null) }
    var isOptionsFromPlayer by remember { mutableStateOf(false) }
    var showSplash by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Register notification favorite/like button action
    LaunchedEffect(Unit) {
        PlaybackBridge.onToggleLikeCallback = {
            val song = audioPlayer.currentSong.value
            if (song.id != "none" && song.id != "empty_state") {
                val newLiked = !song.isLiked
                val updated = song.copy(isLiked = newLiked)
                songsList = songsList.map { if (it.id == song.id) updated else it }
                audioPlayer.updateCurrentSongData(updated)
                scope.launch(Dispatchers.IO) {
                    musicRepository.setSongLiked(song.id, newLiked)
                }
            }
        }
    }

    val currentSong by audioPlayer.currentSong.collectAsStateWithLifecycle()
    val isPlaying by audioPlayer.isPlaying.collectAsStateWithLifecycle()
    val isShuffle by audioPlayer.isShuffle.collectAsStateWithLifecycle()
    val isRepeat by audioPlayer.isRepeat.collectAsStateWithLifecycle()

    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDarkTheme = when (playerSettings.themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemDark
    }

    val strings = remember(playerSettings.language) {
        LocalizationManager.getStrings(playerSettings.language)
    }

    // Permission handling and scanning
    val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_AUDIO
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

    fun restoreOrInitPlayback(allFilteredSongs: List<Song>) {
        if (allFilteredSongs.isEmpty()) return
        if (audioPlayer.currentSong.value.id == "none" || audioPlayer.currentSong.value.id == "empty_state") {
            val savedState = preferencesManager.loadPlaybackState()
            val targetSong = if (!savedState.lastSongId.isNullOrBlank()) {
                allFilteredSongs.find { it.id == savedState.lastSongId }
            } else null

            if (targetSong != null) {
                val restoredQueue = if (savedState.queueSongIds.isNotEmpty()) {
                    val found = savedState.queueSongIds.mapNotNull { qId -> allFilteredSongs.find { it.id == qId } }
                    if (found.any { it.id == targetSong.id }) found else (listOf(targetSong) + found)
                } else emptyList()

                val finalQueue = if (restoredQueue.isNotEmpty()) restoredQueue else allFilteredSongs
                audioPlayer.restorePlaybackState(
                    song = targetSong,
                    queue = finalQueue,
                    positionSec = savedState.lastPositionSec,
                    isShuffle = savedState.isShuffle,
                    isRepeat = savedState.isRepeat
                )
            } else {
                audioPlayer.setQueue(allFilteredSongs)
                audioPlayer.restorePlaybackState(
                    song = allFilteredSongs.first(),
                    queue = allFilteredSongs,
                    positionSec = 0
                )
            }
        } else {
            audioPlayer.setQueue(allFilteredSongs)
        }
    }

    fun scanFilesInternal(isManual: Boolean = false) {
        scope.launch(Dispatchers.IO) {
            val scanned = MediaStoreAudioScanner.scanDeviceAudio(context)
            val metadata = musicRepository.getAllMetadataSync()
            val merged = musicRepository.applyMetadataToSongs(scanned, metadata)
            val deletedIds = preferencesManager.getDeletedSongIds()
            val filtered = merged.filter { !deletedIds.contains(it.id) }
            withContext(Dispatchers.Main) {
                val previousIds = songsList.map { it.id }.toSet()
                val newlyAdded = filtered.filter { !previousIds.contains(it.id) }

                if (filtered.isNotEmpty()) {
                    songsList = filtered
                    restoreOrInitPlayback(filtered)
                    if (isManual) {
                        snackbarHostState.showSnackbar("تم فحص الذاكرة: تم العثور على ${filtered.size} أغنية")
                    } else if (newlyAdded.isNotEmpty() && previousIds.isNotEmpty()) {
                        snackbarHostState.showSnackbar("تم العثور على ${newlyAdded.size} أغانٍ جديدة تلقائياً")
                    }
                } else {
                    if (isManual) {
                        snackbarHostState.showSnackbar("لم يتم العثور على ملفات صوتية في مسارات الجهاز")
                    }
                }
            }
        }
    }

    val deleteRequestLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            scope.launch {
                snackbarHostState.showSnackbar("تم تأكيد حذف الملف من ذاكرة الجهاز بنجاح")
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            scanFilesInternal(isManual = false)
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("يلزم إذن الوصول لملفات الصوت لقراءة الأغاني من جهازك")
            }
        }
    }

    fun triggerScan() {
        if (ContextCompat.checkSelfPermission(context, permissionToRequest) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            scanFilesInternal(isManual = true)
        } else {
            permissionLauncher.launch(permissionToRequest)
        }
    }

    fun deleteSongs(songIds: List<String>) {
        if (songIds.isEmpty()) return
        val idSet = songIds.toSet()

        // 1. Persist to preferencesManager so they are permanently excluded
        preferencesManager.addDeletedSongIds(idSet)

        // 2. Remove from in-memory songsList immediately
        val toDeleteSongs = songsList.filter { idSet.contains(it.id) }
        songsList = songsList.filter { !idSet.contains(it.id) }

        // 3. Update all playlists
        playlistsList = playlistsList.map { p ->
            val updatedSongIds = p.songIds.filter { !idSet.contains(it) }
            p.copy(songIds = updatedSongIds, songCount = updatedSongIds.size)
        }

        // 4. Update audio queue & playback
        songIds.forEach { audioPlayer.removeSongFromQueue(it) }

        // 5. Update DB and execute real physical file deletion from disk & MediaStore
        scope.launch(Dispatchers.IO) {
            playlistsList.forEach { p ->
                musicRepository.updatePlaylistSongs(p.id, p.songIds)
            }

            val urisToDelete = mutableListOf<Uri>()

            for (s in toDeleteSongs) {
                // Try direct filesystem deletion
                s.filePath?.let { path ->
                    try {
                        val file = java.io.File(path)
                        if (file.exists()) {
                            file.delete()
                        }
                    } catch (_: Exception) {}
                }

                // Try MediaStore ContentResolver deletion
                s.contentUri?.let { uriStr ->
                    try {
                        val uri = Uri.parse(uriStr)
                        urisToDelete.add(uri)
                        context.contentResolver.delete(uri, null, null)
                    } catch (e: Exception) {
                        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && e is android.app.RecoverableSecurityException) {
                            withContext(Dispatchers.Main) {
                                deleteRequestLauncher.launch(
                                    IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build()
                                )
                            }
                        }
                    }
                }
            }

            // For Android 11+ (API 30+), request system deletion confirmation if bulk MediaStore delete is needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && urisToDelete.isNotEmpty()) {
                try {
                    val pendingIntent = MediaStore.createDeleteRequest(context.contentResolver, urisToDelete)
                    withContext(Dispatchers.Main) {
                        deleteRequestLauncher.launch(
                            IntentSenderRequest.Builder(pendingIntent.intentSender).build()
                        )
                    }
                } catch (_: Exception) {}
            }
        }

        scope.launch {
            val msg = if (songIds.size == 1) "تم حذف الأغنية نهائياً من الجهاز" else "تم حذف ${songIds.size} أغانٍ نهائياً من الجهاز"
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Auto-scan on app launch & when returning to app (ON_RESUME) and persist on PAUSE/STOP
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (ContextCompat.checkSelfPermission(context, permissionToRequest) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        scanFilesInternal(isManual = false)
                    }
                }
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    audioPlayer.persistState()
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            audioPlayer.persistState()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Load persisted playlists and metadata on launch
    LaunchedEffect(Unit) {
        val (initPlaylists, initMetadata) = withContext(Dispatchers.IO) {
            musicRepository.loadInitialData()
        }
        playlistsList = initPlaylists

        if (ContextCompat.checkSelfPermission(context, permissionToRequest) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            val scanned = withContext(Dispatchers.IO) {
                MediaStoreAudioScanner.scanDeviceAudio(context)
            }
            if (scanned.isNotEmpty()) {
                val merged = musicRepository.applyMetadataToSongs(scanned, initMetadata)
                val deletedIds = preferencesManager.getDeletedSongIds()
                val filtered = merged.filter { !deletedIds.contains(it.id) }
                if (filtered.isNotEmpty()) {
                    songsList = filtered
                    restoreOrInitPlayback(filtered)
                }
            }
        } else {
            // Automatically prompt for permission on initial open so songs are found automatically
            permissionLauncher.launch(permissionToRequest)
        }
    }

    // Keep All playlists and Liked Songs song count synchronized
    val synchronizedPlaylists = remember(playlistsList, songsList) {
        val likedCount = songsList.count { it.isLiked }
        playlistsList.map { p ->
            if (p.id == "p_liked") {
                p.copy(songCount = likedCount, subtitle = "قائمتك المفضلة • $likedCount أغنية")
            } else {
                val realCount = p.songIds.count { id -> songsList.any { it.id == id } }
                p.copy(songCount = realCount, subtitle = "قائمة تشغيل • $realCount أغانٍ")
            }
        }
    }

    // Handle back presses smoothly: Fullscreen Player -> Playlist Detail -> Non-Home Page Tab -> App Exit
    BackHandler(enabled = isPlayerExpanded || selectedPlaylistDetail != null || pagerState.currentPage != 0) {
        if (isPlayerExpanded) {
            isPlayerExpanded = false
        } else if (selectedPlaylistDetail != null) {
            selectedPlaylistDetail = null
        } else if (pagerState.currentPage != 0) {
            scope.launch {
                pagerState.animateScrollToPage(0)
            }
        }
    }

    MusicfyTheme(darkTheme = isDarkTheme) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Main Horizontal Pager for Seamless Finger-Following Screen Gestures
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1
                ) { page ->
                    when (page) {
                        0 -> {
                            HomeScreen(
                                allSongs = songsList,
                                playlists = synchronizedPlaylists,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                                onSongClick = { song ->
                                    audioPlayer.setShuffle(false)
                                    audioPlayer.playSong(song, songsList)
                                },
                                onShuffleAll = {
                                    audioPlayer.playPlaylist(songsList, shuffle = true)
                                },
                                onPlayAll = {
                                    audioPlayer.playPlaylist(songsList, startIndex = 0, shuffle = false)
                                },
                                onOpenEqualizer = { showEqualizer = true },
                                onOpenPlaylist = { playlist ->
                                    selectedPlaylistDetail = playlist
                                },
                                onToggleLike = { song ->
                                    val newLiked = !song.isLiked
                                    songsList = songsList.map {
                                        if (it.id == song.id) it.copy(isLiked = newLiked) else it
                                    }
                                    if (currentSong.id == song.id) {
                                        audioPlayer.updateCurrentSongData(currentSong.copy(isLiked = newLiked))
                                    }
                                    scope.launch(Dispatchers.IO) {
                                        musicRepository.setSongLiked(song.id, newLiked)
                                    }
                                },
                                onDeleteSongs = { ids ->
                                    deleteSongs(ids)
                                },
                                onSongOptionsClick = { song ->
                                    songForOptions = song
                                    playlistForSongOptions = null
                                    isOptionsFromPlayer = false
                                },
                                onScanDeviceClick = {
                                    triggerScan()
                                },
                                strings = strings
                            )
                        }
                        1 -> {
                            FoldersScreen(
                                allSongs = songsList,
                                currentSong = currentSong,
                                isPlaying = isPlaying,
                                onSongClick = { song, folderSongs ->
                                    audioPlayer.setShuffle(false)
                                    audioPlayer.playSong(song, folderSongs)
                                },
                                onPlayFolder = { folderSongs, shuffle ->
                                    audioPlayer.playPlaylist(folderSongs, startIndex = 0, shuffle = shuffle)
                                },
                                onToggleLike = { song ->
                                    val newLiked = !song.isLiked
                                    songsList = songsList.map {
                                        if (it.id == song.id) it.copy(isLiked = newLiked) else it
                                    }
                                    if (currentSong.id == song.id) {
                                        audioPlayer.updateCurrentSongData(currentSong.copy(isLiked = newLiked))
                                    }
                                    scope.launch(Dispatchers.IO) {
                                        musicRepository.setSongLiked(song.id, newLiked)
                                    }
                                },
                                onSongOptionsClick = { song ->
                                    songForOptions = song
                                    playlistForSongOptions = null
                                    isOptionsFromPlayer = false
                                },
                                onScanDeviceClick = {
                                    triggerScan()
                                },
                                strings = strings
                            )
                        }
                        2 -> {
                            LibraryScreen(
                                playlists = synchronizedPlaylists,
                                allSongs = songsList,
                                onPlaylistClick = { playlist ->
                                    selectedPlaylistDetail = playlist
                                },
                                onCreateNewPlaylist = { name ->
                                    val newP = Playlist(
                                        id = "p_${System.currentTimeMillis()}",
                                        title = name,
                                        subtitle = "قائمة مخصصة • 0 أغانٍ",
                                        songCount = 0,
                                        durationText = "0m",
                                        coverType = CoverArtType.GRADIENT_TEAL,
                                        gradientColors = listOf(Color(0xFF0D9488), Color(0xFF0F766E))
                                    )
                                    playlistsList = playlistsList + newP
                                    scope.launch(Dispatchers.IO) {
                                        musicRepository.insertPlaylist(newP)
                                    }
                                    scope.launch {
                                        snackbarHostState.showSnackbar("تم إنشاء \"$name\" وستظهر بالصفحة الرئيسية")
                                    }
                                },
                                onRenamePlaylist = { pl, newName ->
                                    playlistsList = playlistsList.map { if (it.id == pl.id) it.copy(title = newName) else it }
                                    scope.launch(Dispatchers.IO) {
                                        musicRepository.renamePlaylist(pl.id, newName)
                                    }
                                    scope.launch {
                                        snackbarHostState.showSnackbar(strings.playlistRenamedSuccess)
                                    }
                                },
                                onDeletePlaylist = { pl ->
                                    playlistsList = playlistsList.filter { it.id != pl.id }
                                    scope.launch(Dispatchers.IO) {
                                        musicRepository.deletePlaylist(pl.id)
                                    }
                                    scope.launch {
                                        snackbarHostState.showSnackbar(strings.playlistDeletedSuccess)
                                    }
                                },
                                onScanDeviceClick = {
                                    triggerScan()
                                },
                                strings = strings
                            )
                        }
                        3 -> {
                            SettingsScreen(
                                settings = playerSettings,
                                onUpdateSettings = {
                                    playerSettings = it
                                    preferencesManager.saveSettings(it)
                                },
                                onBack = {
                                    scope.launch {
                                        pagerState.animateScrollToPage(0)
                                    }
                                },
                                onOpenEqualizer = { showEqualizer = true },
                                onOpenSleepTimer = { showSleepTimer = true },
                                onScanFiles = {
                                    triggerScan()
                                },
                                onClearCache = {
                                    val updated = playerSettings.copy(cacheSizeMb = 0.0f)
                                    playerSettings = updated
                                    preferencesManager.saveSettings(updated)
                                    scope.launch {
                                        snackbarHostState.showSnackbar("تم مسح الذاكرة المؤقتة بنجاح")
                                    }
                                }
                            )
                        }
                    }
                }

                // Smooth Sliding Overlay for PlaylistDetailScreen
                AnimatedVisibility(
                    visible = selectedPlaylistDetail != null,
                    enter = slideInHorizontally(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                        initialOffsetX = { it }
                    ) + fadeIn(tween(240)),
                    exit = slideOutHorizontally(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                        targetOffsetX = { it }
                    ) + fadeOut(tween(180))
                ) {
                    selectedPlaylistDetail?.let { activePlaylist ->
                        val playlistId = activePlaylist.id
                        val detailPlaylist = synchronizedPlaylists.find { it.id == playlistId } ?: activePlaylist
                        val playlistSongs = remember(songsList, detailPlaylist) {
                            if (detailPlaylist.id == "p_liked") {
                                songsList.filter { it.isLiked }
                            } else {
                                detailPlaylist.songIds.mapNotNull { id -> songsList.find { it.id == id } }
                            }
                        }

                        PlaylistDetailScreen(
                            playlist = detailPlaylist,
                            playlistSongs = playlistSongs,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            onBack = {
                                selectedPlaylistDetail = null
                            },
                            onSongClick = { song ->
                                audioPlayer.setShuffle(false)
                                audioPlayer.playSong(song, playlistSongs)
                            },
                            onShufflePlaylist = {
                                audioPlayer.playPlaylist(playlistSongs, shuffle = true)
                            },
                            onPlayPlaylist = {
                                audioPlayer.playPlaylist(playlistSongs, startIndex = 0, shuffle = false)
                            },
                            onToggleLikeSong = { song ->
                                val newLiked = !song.isLiked
                                songsList = songsList.map {
                                    if (it.id == song.id) it.copy(isLiked = newLiked) else it
                                }
                                if (currentSong.id == song.id) {
                                    audioPlayer.updateCurrentSongData(currentSong.copy(isLiked = newLiked))
                                }
                                scope.launch(Dispatchers.IO) {
                                    musicRepository.setSongLiked(song.id, newLiked)
                                }
                            },
                            onAddSongs = {
                                playlistForAddSongs = detailPlaylist
                            },
                            onSongOptionsClick = { song ->
                                songForOptions = song
                                playlistForSongOptions = if (detailPlaylist.id != "p_liked") detailPlaylist else null
                                isOptionsFromPlayer = false
                            },
                            strings = strings
                        )
                    }
                }

                // Floating Mini Player & Floating Bottom Bar Overlay (when not in Fullscreen Player)
                if (!isPlayerExpanded) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Floating Mini Player (isolated progress ticker)
                        MiniPlayerOverlay(
                            audioPlayer = audioPlayer,
                            currentSong = currentSong,
                            isPlaying = isPlaying,
                            isGlassEffect = playerSettings.glassEffect,
                            onToggleLike = {
                                val newLiked = !currentSong.isLiked
                                val updated = currentSong.copy(isLiked = newLiked)
                                songsList = songsList.map {
                                    if (it.id == currentSong.id) updated else it
                                }
                                audioPlayer.updateCurrentSongData(updated)
                                scope.launch(Dispatchers.IO) {
                                    musicRepository.setSongLiked(currentSong.id, newLiked)
                                }
                            },
                            onClick = { isPlayerExpanded = true },
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Floating Bottom Navigation Pill Bar (Takes ~90% width with smooth gliding)
                        val activeDestination = if (selectedPlaylistDetail != null) {
                            AppNavDestination.LIBRARY
                        } else {
                            val activePage = if (pagerState.isScrollInProgress) pagerState.targetPage else pagerState.currentPage
                            when (activePage) {
                                0 -> AppNavDestination.HOME
                                1 -> AppNavDestination.FOLDERS
                                2 -> AppNavDestination.LIBRARY
                                else -> AppNavDestination.SETTINGS
                            }
                        }

                        FloatingBottomNavBar(
                            selectedDestination = activeDestination,
                            onDestinationSelected = { dest ->
                                selectedPlaylistDetail = null
                                val targetPage = when (dest) {
                                    AppNavDestination.HOME -> 0
                                    AppNavDestination.FOLDERS -> 1
                                    AppNavDestination.LIBRARY -> 2
                                    AppNavDestination.SETTINGS -> 3
                                }
                                scope.launch {
                                    pagerState.animateScrollToPage(targetPage)
                                }
                            },
                            isGlassEffect = playerSettings.glassEffect
                        )
                    }
                }

                // Smooth Slide-Up Fullscreen Player Modal Sheet
                AnimatedVisibility(
                    visible = isPlayerExpanded,
                    enter = slideInVertically(
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
                            dampingRatio = Spring.DampingRatioLowBouncy
                        ),
                        initialOffsetY = { fullHeight -> fullHeight }
                    ) + fadeIn(animationSpec = tween(250)),
                    exit = slideOutVertically(
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMedium,
                            dampingRatio = Spring.DampingRatioNoBouncy
                        ),
                        targetOffsetY = { fullHeight -> fullHeight }
                    ) + fadeOut(animationSpec = tween(200)),
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* Intercept all touches */ }
                        )
                ) {
                    val playerPositionSec by audioPlayer.currentPositionSec.collectAsStateWithLifecycle()
                    val playerSleepTimerSec by audioPlayer.sleepTimerSeconds.collectAsStateWithLifecycle()

                    FullscreenPlayerSheet(
                        song = currentSong,
                        isPlaying = isPlaying,
                        currentPositionSec = playerPositionSec,
                        isShuffle = isShuffle,
                        isRepeat = isRepeat,
                        isLiked = currentSong.isLiked,
                        sleepTimerMinutes = if (playerSleepTimerSec != null) (playerSleepTimerSec!! / 60) else null,
                        sleepTimerSeconds = playerSleepTimerSec,
                        onCollapse = { isPlayerExpanded = false },
                        onPlayPause = { audioPlayer.togglePlayPause() },
                        onPrevious = { audioPlayer.playPrevious() },
                        onNext = { audioPlayer.playNext() },
                        onSeek = { audioPlayer.seekTo(it) },
                        onToggleShuffle = { audioPlayer.toggleShuffle() },
                        onToggleRepeat = { audioPlayer.toggleRepeat() },
                        onToggleLike = {
                            val newLiked = !currentSong.isLiked
                            val updated = currentSong.copy(isLiked = newLiked)
                            songsList = songsList.map {
                                if (it.id == currentSong.id) updated else it
                            }
                            audioPlayer.updateCurrentSongData(updated)
                            scope.launch(Dispatchers.IO) {
                                musicRepository.setSongLiked(currentSong.id, newLiked)
                            }
                        },
                        onOpenSleepTimer = { showSleepTimer = true },
                        onOpenQueue = { showQueueSheet = true },
                        onOpenOptions = {
                            songForOptions = currentSong
                            playlistForSongOptions = null
                            isOptionsFromPlayer = true
                        },
                        onUpdateLyrics = { lyricsText ->
                            val updated = currentSong.copy(rawLyricsText = lyricsText)
                            songsList = songsList.map { if (it.id == currentSong.id) updated else it }
                            audioPlayer.updateCurrentSongData(updated)
                            scope.launch(Dispatchers.IO) {
                                musicRepository.updateSongDetails(
                                    songId = currentSong.id,
                                    rawLyrics = lyricsText
                                )
                            }
                            scope.launch {
                                snackbarHostState.showSnackbar(strings.lyricsSavedSuccess)
                            }
                        },
                        strings = strings,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Song Options Bottom Sheet (Change cover, device photo, edit tags/name, add to playlist, remove from playlist, delete song, play next)
                songForOptions?.let { targetSong ->
                    SongOptionsBottomSheet(
                        song = targetSong,
                        playlists = synchronizedPlaylists,
                        currentPlaylist = playlistForSongOptions,
                        showPlayNext = !isOptionsFromPlayer,
                        strings = strings,
                        onDismiss = {
                            songForOptions = null
                            playlistForSongOptions = null
                        },
                        onPlayNext = {
                            audioPlayer.playNextInQueue(targetSong)
                            scope.launch {
                                snackbarHostState.showSnackbar("تمت إضافة \"${targetSong.title}\" إلى التشغيل التالي")
                            }
                        },
                        onRemoveFromPlaylist = playlistForSongOptions?.let { currentPl ->
                            {
                                val updatedSongIds = currentPl.songIds.filter { it != targetSong.id }
                                playlistsList = playlistsList.map { p ->
                                    if (p.id == currentPl.id) {
                                        p.copy(songIds = updatedSongIds, songCount = updatedSongIds.size)
                                    } else p
                                }
                                scope.launch(Dispatchers.IO) {
                                    musicRepository.updatePlaylistSongs(currentPl.id, updatedSongIds)
                                }
                                scope.launch {
                                    snackbarHostState.showSnackbar(strings.removedFromPlaylistSuccess)
                                }
                            }
                        },
                        onAddToPlaylist = { targetPlaylist ->
                            val updatedIds = (targetPlaylist.songIds + targetSong.id).distinct()
                            playlistsList = playlistsList.map { p ->
                                if (p.id == targetPlaylist.id) {
                                    p.copy(songIds = updatedIds, songCount = updatedIds.size)
                                } else p
                            }
                            if (targetPlaylist.id == "p_liked") {
                                val updated = targetSong.copy(isLiked = true)
                                songsList = songsList.map { if (it.id == targetSong.id) updated else it }
                                if (currentSong.id == targetSong.id) {
                                    audioPlayer.updateCurrentSongData(currentSong.copy(isLiked = true))
                                }
                                scope.launch(Dispatchers.IO) {
                                    musicRepository.setSongLiked(targetSong.id, true)
                                }
                            }
                            scope.launch(Dispatchers.IO) {
                                musicRepository.updatePlaylistSongs(targetPlaylist.id, updatedIds)
                            }
                            scope.launch {
                                snackbarHostState.showSnackbar(strings.addedToPlaylistSuccess.format(targetPlaylist.title))
                            }
                        },
                        onChangeCover = { newType, newColors ->
                            val updated = targetSong.copy(coverType = newType, gradientColors = newColors)
                            songsList = songsList.map { if (it.id == targetSong.id) updated else it }
                            audioPlayer.updateCurrentSongData(updated)
                            scope.launch(Dispatchers.IO) {
                                musicRepository.updateSongDetails(
                                    songId = targetSong.id,
                                    coverType = newType,
                                    gradientColors = newColors
                                )
                            }
                            scope.launch {
                                snackbarHostState.showSnackbar(strings.coverUpdatedSuccess)
                            }
                        },
                        onChangeCustomCover = { uriString ->
                            val updated = targetSong.copy(
                                coverType = CoverArtType.CUSTOM_IMAGE,
                                customImageUri = uriString
                            )
                            songsList = songsList.map { if (it.id == targetSong.id) updated else it }
                            audioPlayer.updateCurrentSongData(updated)
                            scope.launch(Dispatchers.IO) {
                                musicRepository.updateSongDetails(
                                    songId = targetSong.id,
                                    coverType = CoverArtType.CUSTOM_IMAGE,
                                    customImageUri = uriString
                                )
                            }
                            scope.launch {
                                snackbarHostState.showSnackbar(strings.coverUpdatedSuccess)
                            }
                        },
                        onEditTitleAndArtist = { newTitle, newArtist ->
                            val updated = targetSong.copy(title = newTitle, artist = newArtist)
                            songsList = songsList.map { if (it.id == targetSong.id) updated else it }
                            audioPlayer.updateCurrentSongData(updated)
                            scope.launch(Dispatchers.IO) {
                                musicRepository.updateSongDetails(
                                    songId = targetSong.id,
                                    customTitle = newTitle,
                                    customArtist = newArtist
                                )
                            }
                            scope.launch {
                                snackbarHostState.showSnackbar("تم تعديل بيانات الأغنية")
                            }
                        },
                        onDeleteSong = {
                            deleteSongs(listOf(targetSong.id))
                            songForOptions = null
                        }
                    )
                }

                // Queue Bottom Sheet (Reordering tracks, move up/down, move to top, instant playback)
                if (showQueueSheet) {
                    val currentQueue by audioPlayer.queue.collectAsStateWithLifecycle()
                    QueueBottomSheet(
                        currentSong = currentSong,
                        isPlaying = isPlaying,
                        queue = currentQueue,
                        strings = strings,
                        onPlaySong = { s -> audioPlayer.playSong(s) },
                        onMoveUp = { sId -> audioPlayer.moveQueueSongUp(sId) },
                        onMoveDown = { sId -> audioPlayer.moveQueueSongDown(sId) },
                        onMoveToTop = { sId -> audioPlayer.moveQueueSongToTop(sId) },
                        onReorderUpcoming = { list -> audioPlayer.setUpcomingQueue(list) },
                        onRemoveFromQueue = { sId -> audioPlayer.removeSongFromQueue(sId) },
                        onDismiss = { showQueueSheet = false }
                    )
                }

                // Equalizer Dialog
                if (showEqualizer) {
                    val eqBands by audioPlayer.eqBands.collectAsStateWithLifecycle()
                    val activePreset by audioPlayer.activePreset.collectAsStateWithLifecycle()
                    EqualizerDialog(
                        bands = eqBands,
                        activePreset = activePreset,
                        onBandChange = { idx, valDb -> audioPlayer.updateEqBand(idx, valDb) },
                        onPresetSelect = { preset -> audioPlayer.applyPreset(preset) },
                        onDismiss = { showEqualizer = false }
                    )
                }

                // Sleep Timer Dialog
                if (showSleepTimer) {
                    val sleepTimerSec by audioPlayer.sleepTimerSeconds.collectAsStateWithLifecycle()
                    SleepTimerDialog(
                        currentMinutes = if (sleepTimerSec != null) (sleepTimerSec!! / 60) else null,
                        remainingSeconds = sleepTimerSec,
                        onSelectMinutes = { mins ->
                            audioPlayer.setSleepTimer(mins)
                            playerSettings = playerSettings.copy(sleepTimerMinutes = mins)
                            if (mins != null) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("تم ضبط مؤقت النوم على $mins دقيقة")
                                }
                            }
                        },
                        onSelectEndOfTrack = {
                            audioPlayer.setSleepTimerToEndOfTrack()
                            scope.launch {
                                snackbarHostState.showSnackbar("سيتم إيقاف التشغيل عند نهاية الأغنية الحالية")
                            }
                        },
                        onDismiss = { showSleepTimer = false }
                    )
                }

                // Add Songs to Playlist Multi-select Dialog
                playlistForAddSongs?.let { targetPlaylist ->
                    AddSongsToPlaylistDialog(
                        playlistTitle = targetPlaylist.title,
                        allSongs = songsList,
                        existingSongIds = targetPlaylist.songIds,
                        strings = strings,
                        onDismiss = { playlistForAddSongs = null },
                        onAddSongs = { selectedSongIds ->
                            val combined = (targetPlaylist.songIds + selectedSongIds).distinct()
                            playlistsList = playlistsList.map { p ->
                                if (p.id == targetPlaylist.id) {
                                    p.copy(songIds = combined, songCount = combined.size)
                                } else p
                            }
                            scope.launch(Dispatchers.IO) {
                                musicRepository.updatePlaylistSongs(targetPlaylist.id, combined)
                            }
                            scope.launch {
                                snackbarHostState.showSnackbar("تمت إضافة ${selectedSongIds.size} أغانٍ إلى ${targetPlaylist.title}")
                            }
                            playlistForAddSongs = null
                        }
                    )
                }

                // Boot / Splash Screen with Fade-in and Animated Loading Progress Bar
                AnimatedVisibility(
                    visible = showSplash,
                    enter = fadeIn(animationSpec = tween(200)),
                    exit = fadeOut(animationSpec = tween(500))
                ) {
                    SplashScreen(
                        onSplashFinished = {
                            showSplash = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Isolated MiniPlayerOverlay component:
 * Isolates high-frequency progress state updates (every 1s) from recomposing
 * the outer screens, LazyColumns, or lists.
 */
@Composable
private fun MiniPlayerOverlay(
    audioPlayer: AudioPlayerController,
    currentSong: Song,
    isPlaying: Boolean,
    isGlassEffect: Boolean,
    onToggleLike: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentPositionSec by audioPlayer.currentPositionSec.collectAsStateWithLifecycle()
    val progressFraction = remember(currentPositionSec, currentSong.durationSec) {
        if (currentSong.durationSec > 0) {
            currentPositionSec.toFloat() / currentSong.durationSec.toFloat()
        } else 0f
    }

    MiniPlayerBar(
        song = currentSong,
        isPlaying = isPlaying,
        progressFraction = progressFraction,
        isLiked = currentSong.isLiked,
        onToggleLike = onToggleLike,
        onPlayPause = { audioPlayer.togglePlayPause() },
        onNext = { audioPlayer.playNext() },
        onClick = onClick,
        isGlassEffect = isGlassEffect,
        modifier = modifier
    )
}
