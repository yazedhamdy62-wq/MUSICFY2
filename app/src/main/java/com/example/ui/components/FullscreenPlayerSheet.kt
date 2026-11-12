package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextDirection
import com.example.data.shareSongFile
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppStrings
import com.example.data.LocalizationManager
import com.example.data.LrcParser
import com.example.data.LyricLine
import com.example.data.Song
import com.example.ui.dialogs.OnlineLyricsSheet
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ResonanceGreen
import com.example.ui.theme.ResonanceGreenGlow
import java.net.URLEncoder

enum class PlayerViewMode {
    SONG, LYRICS
}

@Composable
fun FullscreenPlayerSheet(
    song: Song,
    isPlaying: Boolean,
    currentPositionSec: Int,
    isShuffle: Boolean,
    isRepeat: Boolean,
    isLiked: Boolean,
    sleepTimerMinutes: Int?,
    sleepTimerSeconds: Int? = null,
    onCollapse: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Int) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenOptions: () -> Unit = {},
    onUpdateLyrics: (String) -> Unit = {},
    strings: AppStrings = LocalizationManager.Arabic,
    modifier: Modifier = Modifier
) {
    var playerMode by remember { mutableStateOf(PlayerViewMode.SONG) }
    val lyricsListState = rememberLazyListState()
    val context = LocalContext.current

    val isDark = MaterialTheme.colorScheme.background == DarkBackground || MaterialTheme.colorScheme.surface == DarkSurface
    val bgColor = if (isDark) DarkBackground else Color(0xFFFFFFFF)
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val iconColor = if (isDark) Color.White else Color(0xFF1E293B)
    val cardBg = if (isDark) Color(0xFF1D242C) else Color(0xFFF8FAFC)
    val borderColor = if (isDark) Color(0xFF26333D) else Color(0xFFE2E8F0)

    var showManualLyricsDialog by remember { mutableStateOf(false) }
    var showOnlineLyricsSheet by remember { mutableStateOf(false) }
    var manualLyricsInput by remember(song.id, song.rawLyricsText) {
        mutableStateOf(song.rawLyricsText ?: song.lyrics.joinToString("\n") { it.text })
    }

    // Synced (LRC) lyrics saved as text are parsed into timed lines so they follow the song
    val syncedFromRaw = remember(song.rawLyricsText) { LrcParser.parse(song.rawLyricsText) }
    val effectiveLyrics = if (syncedFromRaw.isNotEmpty()) syncedFromRaw else song.lyrics

    // Determine current active lyric line
    val activeLyricIndex = remember(effectiveLyrics, currentPositionSec) {
        val idx = effectiveLyrics.indexOfLast { it.timeSec <= currentPositionSec }
        if (idx >= 0) idx else 0
    }

    // Auto-scroll to active lyric in Lyrics mode
    LaunchedEffect(activeLyricIndex, playerMode) {
        if (playerMode == PlayerViewMode.LYRICS && effectiveLyrics.isNotEmpty()) {
            val targetIdx = (activeLyricIndex - 1).coerceAtLeast(0)
            lyricsListState.animateScrollToItem(targetIdx)
        }
    }

    val playerClickBlocker = remember { MutableInteractionSource() }

    // Spotify-style background: gradient built from the cover colors (stronger in lyrics mode)
    val coverModel = song.customImageUri?.takeIf { it.isNotBlank() }
        ?: song.albumArtUri?.takeIf { it.isNotBlank() }
    val coverAccent = rememberCoverAccent(
        imageModel = coverModel,
        fallback = if (coverModel != null) Color(0xFF2A3440)
        else (song.gradientColors.firstOrNull() ?: Color(0xFF334155))
    )
    val lyricsStrength by animateFloatAsState(
        targetValue = if (playerMode == PlayerViewMode.LYRICS) 1f else 0f,
        animationSpec = tween(500),
        label = "lyricsGradient"
    )
    val playerBackgroundBrush = coverGradientBrush(coverAccent, bgColor, isDark, lyricsStrength)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(playerBackgroundBrush)
            .clickable(
                interactionSource = playerClickBlocker,
                indication = null,
                onClick = { /* Consume clicks in empty spaces to prevent background clicks */ }
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = playerClickBlocker,
                    indication = null,
                    onClick = { /* Consume clicks in empty spaces */ }
                )
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Visual Drag Handle Indicator for collapsing the player sheet
            Box(
                modifier = Modifier
                    .padding(top = 6.dp, bottom = 2.dp)
                    .width(42.dp)
                    .height(4.5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(if (isDark) Color.White.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.22f))
                    .clickable(onClick = onCollapse)
                    .pointerInput(Unit) {
                        var totalDragY = 0f
                        detectVerticalDragGestures(
                            onDragStart = { totalDragY = 0f },
                            onVerticalDrag = { change, dragAmount ->
                                totalDragY += dragAmount
                                if (totalDragY > 20f) {
                                    change.consume()
                                    onCollapse()
                                }
                            }
                        )
                    }
            )

            // Top Bar: Collapse arrow, Mode Toggle, 3-dots
            PlayerTopBar(
                currentMode = playerMode,
                onModeChange = { playerMode = it },
                onCollapse = onCollapse,
                onMenuClick = onOpenOptions,
                isDark = isDark,
                strings = strings,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .pointerInput(Unit) {
                        var totalDragY = 0f
                        detectVerticalDragGestures(
                            onDragStart = { totalDragY = 0f },
                            onVerticalDrag = { change, dragAmount ->
                                totalDragY += dragAmount
                                if (totalDragY > 25f) {
                                    change.consume()
                                    onCollapse()
                                }
                            }
                        )
                    }
            )

            // Animated content between "Song" (Album Art) and "Lyrics" with swipe down to collapse & horizontal swipe gestures
            AnimatedContent(
                targetState = playerMode,
                transitionSpec = {
                    if (targetState == PlayerViewMode.LYRICS) {
                        (slideInHorizontally(tween(260)) { it } + fadeIn(tween(260))) togetherWith
                                (slideOutHorizontally(tween(220)) { -it } + fadeOut(tween(220)))
                    } else {
                        (slideInHorizontally(tween(260)) { -it } + fadeIn(tween(260))) togetherWith
                                (slideOutHorizontally(tween(220)) { it } + fadeOut(tween(220)))
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .pointerInput(playerMode) {
                        var totalDragX = 0f
                        var totalDragY = 0f
                        detectDragGestures(
                            onDragStart = {
                                totalDragX = 0f
                                totalDragY = 0f
                            },
                            onDrag = { change, dragAmount ->
                                totalDragX += dragAmount.x
                                totalDragY += dragAmount.y

                                // If user drags DOWN from top to bottom
                                if (totalDragY > 45f && kotlin.math.abs(totalDragY) > kotlin.math.abs(totalDragX) * 1.2f) {
                                    change.consume()
                                    onCollapse()
                                }
                            },
                            onDragEnd = {
                                val threshold = 60f
                                if (kotlin.math.abs(totalDragX) > kotlin.math.abs(totalDragY) * 1.3f) {
                                    if (totalDragX < -threshold) {
                                        // Swiped left -> open Lyrics
                                        playerMode = PlayerViewMode.LYRICS
                                    } else if (totalDragX > threshold) {
                                        // Swiped right -> return to Song
                                        playerMode = PlayerViewMode.SONG
                                    }
                                }
                            }
                        )
                    },
                label = "player_mode_transition"
            ) { mode ->
                when (mode) {
                    PlayerViewMode.SONG -> {
                        SongArtworkView(
                            song = song,
                            isPlaying = isPlaying,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    PlayerViewMode.LYRICS -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Top action row for Lyrics: "Write Lyrics" & "Search Web Lyrics"
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Button 1: Write Lyrics Manually
                                OutlinedButton(
                                    onClick = {
                                        manualLyricsInput = song.rawLyricsText ?: song.lyrics.joinToString("\n") { it.text }
                                        showManualLyricsDialog = true
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("lyrics_write_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = if (isDark) ResonanceGreen else Color(0xFF059669)
                                    ),
                                    border = BorderStroke(1.dp, borderColor)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.writeLyrics,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }

                                // Button 2: Search lyrics online (LRCLIB) and pick a version
                                OutlinedButton(
                                    onClick = { showOnlineLyricsSheet = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("lyrics_search_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = textColor
                                    ),
                                    border = BorderStroke(1.dp, borderColor)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.onlineLyricsBtn,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            // Lyrics list
                            LyricsScrollableListView(
                                lyrics = effectiveLyrics,
                                rawLyricsText = song.rawLyricsText,
                                activeIndex = activeLyricIndex,
                                listState = lyricsListState,
                                isDark = isDark,
                                noLyricsMsg = strings.noLyricsAvailable,
                                onLineClick = { line ->
                                    onSeek(line.timeSec)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Bottom Section: Song Meta, Progress Slider, Playback Controls, Chips
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Song Metadata Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (playerMode == PlayerViewMode.LYRICS) {
                        SongCoverArtwork(
                            coverType = song.coverType,
                            gradientColors = song.gradientColors,
                            customImageUri = song.customImageUri,
                            albumArtUri = song.albumArtUri,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            isPlaying = isPlaying
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = song.title,
                            color = textColor,
                            fontSize = if (playerMode == PlayerViewMode.SONG) 22.sp else 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = song.artist,
                            color = subTextColor,
                            fontSize = if (playerMode == PlayerViewMode.SONG) 15.sp else 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onToggleLike,
                        modifier = Modifier.testTag("fullscreen_like_btn")
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isLiked) ResonanceGreen else subTextColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Progress Bar and Timestamps
                val currentFraction = if (song.durationSec > 0) {
                    currentPositionSec.toFloat() / song.durationSec.toFloat()
                } else 0f

                var isDragging by remember { mutableStateOf(false) }
                var dragFraction by remember { mutableStateOf(0f) }

                Slider(
                    value = if (isDragging) dragFraction else currentFraction.coerceIn(0f, 1f),
                    onValueChange = { frac ->
                        isDragging = true
                        dragFraction = frac
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        onSeek((dragFraction * song.durationSec).toInt())
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = ResonanceGreen,
                        activeTrackColor = ResonanceGreen,
                        inactiveTrackColor = if (isDark) Color(0xFF26333D) else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .testTag("fullscreen_seek_slider")
                )

                // Current time & Remaining/Total time
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val displaySec = if (isDragging) (dragFraction * song.durationSec).toInt() else currentPositionSec
                    Text(
                        text = formatTime(displaySec),
                        color = subTextColor,
                        fontSize = 12.sp
                    )
                    Text(
                        text = song.durationFormatted,
                        color = subTextColor,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Playback Control Buttons (Shuffle, Prev, Big Neon Play, Next, Sleep Timer)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Shuffle
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier.testTag("fullscreen_shuffle_btn")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (isShuffle) ResonanceGreen else (if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)),
                                modifier = Modifier.size(24.dp)
                            )
                            if (isShuffle) {
                                Box(
                                    modifier = Modifier
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(ResonanceGreen)
                                )
                            }
                        }
                    }

                    // Previous
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("fullscreen_prev_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous",
                            tint = iconColor,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Big Circular Neon Play/Pause Button
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .shadow(
                                elevation = 16.dp,
                                shape = CircleShape,
                                spotColor = ResonanceGreenGlow,
                                ambientColor = ResonanceGreen.copy(alpha = 0.4f)
                            )
                            .clip(CircleShape)
                            .background(ResonanceGreen)
                            .clickable(onClick = onPlayPause)
                            .testTag("fullscreen_play_pause_btn"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Next
                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("fullscreen_next_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next",
                            tint = iconColor,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    // Sleep Timer with countdown indicator
                    val isTimerActive = (sleepTimerSeconds != null && sleepTimerSeconds > 0) || sleepTimerMinutes != null
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isTimerActive) ResonanceGreen.copy(alpha = 0.15f) else Color.Transparent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onOpenSleepTimer)
                            .testTag("fullscreen_timer_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = "Sleep Timer",
                                tint = if (isTimerActive) ResonanceGreen else subTextColor,
                                modifier = Modifier.size(22.dp)
                            )
                            if (sleepTimerSeconds != null && sleepTimerSeconds > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                val m = sleepTimerSeconds / 60
                                val s = sleepTimerSeconds % 60
                                Text(
                                    text = "%02d:%02d".format(m, s),
                                    color = ResonanceGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom actions: Share (audio file), Queue
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = { shareSongFile(context, song) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = subTextColor,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        IconButton(
                            onClick = onOpenQueue,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = "Queue",
                                tint = subTextColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Online lyrics search (LRCLIB) with a list of results to choose from
    if (showOnlineLyricsSheet) {
        OnlineLyricsSheet(
            title = song.title,
            artist = song.artist,
            durationSec = song.durationSec,
            isDark = isDark,
            strings = strings,
            onSelect = { lyricsText ->
                onUpdateLyrics(lyricsText)
                showOnlineLyricsSheet = false
            },
            onOpenBrowser = {
                val query = "${song.title} ${song.artist} lyrics".trim()
                val url = "https://www.google.com/search?q=" + URLEncoder.encode(query, "UTF-8")
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(browserIntent)
                } catch (e: Exception) {
                    // Browser not available
                }
            },
            onDismiss = { showOnlineLyricsSheet = false }
        )
    }

    // Dialog for entering lyrics manually
    if (showManualLyricsDialog) {
        AlertDialog(
            onDismissRequest = { showManualLyricsDialog = false },
            containerColor = cardBg,
            title = {
                Text(
                    text = strings.editLyricsTitle,
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = manualLyricsInput,
                        onValueChange = { manualLyricsInput = it },
                        placeholder = {
                            Text(
                                text = strings.editLyricsHint,
                                color = subTextColor,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp, max = 280.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ResonanceGreen,
                            unfocusedBorderColor = borderColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedContainerColor = if (isDark) Color(0xFF151B20) else Color.White,
                            unfocusedContainerColor = if (isDark) Color(0xFF151B20) else Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateLyrics(manualLyricsInput)
                        showManualLyricsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ResonanceGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(strings.saveBtn, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualLyricsDialog = false }) {
                    Text(strings.cancelBtn, color = subTextColor)
                }
            }
        )
    }
}

@Composable
private fun PlayerTopBar(
    currentMode: PlayerViewMode,
    onModeChange: (PlayerViewMode) -> Unit,
    onCollapse: () -> Unit,
    onMenuClick: () -> Unit,
    isDark: Boolean,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val iconTint = if (isDark) Color.White else Color(0xFF0F172A)
    val pillBg = if (isDark) Color(0xFF1E262E) else Color(0xFFE2E8F0)
    val unselectedText = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Collapse arrow
        IconButton(
            onClick = onCollapse,
            modifier = Modifier.testTag("player_collapse_btn")
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Collapse",
                tint = iconTint,
                modifier = Modifier.size(32.dp)
            )
        }

        // Center Segmented Toggle: Song | Lyrics
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(pillBg)
                .padding(3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isSong = currentMode == PlayerViewMode.SONG
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSong) ResonanceGreen else Color.Transparent)
                    .clickable { onModeChange(PlayerViewMode.SONG) }
                    .padding(horizontal = 14.dp, vertical = 5.dp)
                    .testTag("toggle_song_mode"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.playerSongMode,
                    color = if (isSong) Color.Black else unselectedText,
                    fontSize = 12.sp,
                    fontWeight = if (isSong) FontWeight.Bold else FontWeight.Medium
                )
            }

            val isLyrics = currentMode == PlayerViewMode.LYRICS
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isLyrics) ResonanceGreen else Color.Transparent)
                    .clickable { onModeChange(PlayerViewMode.LYRICS) }
                    .padding(horizontal = 14.dp, vertical = 5.dp)
                    .testTag("toggle_lyrics_mode"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.playerLyricsMode,
                    color = if (isLyrics) Color.Black else unselectedText,
                    fontSize = 12.sp,
                    fontWeight = if (isLyrics) FontWeight.Bold else FontWeight.Medium
                )
            }
        }

        // 3 Dots menu
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier.testTag("player_options_btn")
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun SongArtworkView(
    song: Song,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val clickBlocker = remember { MutableInteractionSource() }
    Box(
        modifier = modifier.clickable(
            interactionSource = clickBlocker,
            indication = null,
            onClick = { /* Intercept clicks on empty space around artwork */ }
        ),
        contentAlignment = Alignment.Center
    ) {
        SongCoverArtwork(
            coverType = song.coverType,
            gradientColors = song.gradientColors,
            customImageUri = song.customImageUri,
            albumArtUri = song.albumArtUri,
            contentUri = song.contentUri,
            highRes = true,
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .aspectRatio(1f)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = Color.Black.copy(alpha = 0.5f)
                )
                .clip(RoundedCornerShape(24.dp)),
            cornerRadius = 24.dp,
            isPlaying = isPlaying
        )
    }
}

@Composable
private fun LyricsScrollableListView(
    lyrics: List<LyricLine>,
    rawLyricsText: String?,
    activeIndex: Int,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isDark: Boolean,
    noLyricsMsg: String,
    onLineClick: (LyricLine) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = if (isDark) Color.White else Color(0xFF0F172A)
    val inactiveColor = if (isDark) Color.White.copy(alpha = 0.42f) else Color(0xFF94A3B8)

    if (lyrics.isEmpty() && rawLyricsText.isNullOrBlank()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = noLyricsMsg,
                color = inactiveColor,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    } else if (lyrics.isNotEmpty()) {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            itemsIndexed(lyrics) { idx, line ->
                val isActive = idx == activeIndex
                LyricLineItem(
                    line = line,
                    isActive = isActive,
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    onClick = { onLineClick(line) }
                )
            }
        }
    } else {
        // Plain raw lyrics
        val lines = remember(rawLyricsText) {
            rawLyricsText?.lines()?.filter { it.isNotBlank() } ?: emptyList()
        }
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(lines) { _, lineText ->
                Text(
                    text = lineText,
                    color = activeColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 26.sp,
                    textAlign = TextAlign.Start,
                    style = LocalTextStyle.current.copy(textDirection = TextDirection.Content),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun LyricLineItem(
    line: LyricLine,
    isActive: Boolean,
    activeColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit
) {
    // 0f = inactive line, 1f = active line. Animated so lines glide between states.
    val progress by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(380),
        label = "lyricLine"
    )
    val isRtlText = remember(line.text) {
        val first = line.text.firstOrNull { Character.isLetter(it) }
        first != null && (
            Character.getDirectionality(first) == Character.DIRECTIONALITY_RIGHT_TO_LEFT ||
                Character.getDirectionality(first) == Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC
            )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = line.text,
            color = lerp(inactiveColor, activeColor, progress),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 32.sp,
            textAlign = TextAlign.Start,
            style = LocalTextStyle.current.copy(textDirection = TextDirection.Content),
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    val scale = 0.78f + 0.22f * progress
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(if (isRtlText) 1f else 0f, 0.5f)
                }
        )

        if (isActive && line.sectionTag != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = line.sectionTag,
                color = ResonanceGreen,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }
    }
}

private fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}
