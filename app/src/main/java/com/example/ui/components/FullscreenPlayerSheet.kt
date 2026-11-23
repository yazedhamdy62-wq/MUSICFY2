package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.VolumeMute
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.ui.draw.drawBehind
import kotlin.math.roundToInt
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
    // Figma look: the whole player is one rich colour taken from the cover, a bit darker at the bottom
    val playerBackgroundBrush = androidx.compose.ui.graphics.Brush.verticalGradient(
        listOf(lerp(coverAccent, Color.Black, 0.28f), lerp(coverAccent, Color.Black, 0.52f))
    )
    // Light version of the song colour used for icons, times and the sliders
    val tone = lerp(coverAccent, Color.White, 0.62f)

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
                tone = tone,
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
                                    shape = RoundedCornerShape(21.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.Black.copy(alpha = 0.18f),
                                        contentColor = Color.White
                                    ),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
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
                                    shape = RoundedCornerShape(21.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = Color.Black.copy(alpha = 0.18f),
                                        contentColor = Color.White
                                    ),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
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
                                isDark = true,
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

            // Bottom Section: Song Meta, Progress, Playback Controls, Volume, Extra actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Song Metadata Row: title/artist on the left, star + more on the right
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
                            color = Color.White,
                            fontSize = if (playerMode == PlayerViewMode.SONG) 20.sp else 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = strings.displayArtist(song.artist),
                            color = tone,
                            fontSize = if (playerMode == PlayerViewMode.SONG) 16.sp else 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Star (favorite)
                    PlayerCircleButton(
                        onClick = onToggleLike,
                        fill = tone.copy(alpha = if (isLiked) 0.55f else 0.30f),
                        modifier = Modifier.testTag("fullscreen_like_btn")
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = "Favorite",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // More (opens the glass options sheet)
                    PlayerCircleButton(
                        onClick = onOpenOptions,
                        fill = tone.copy(alpha = 0.30f),
                        modifier = Modifier.testTag("player_options_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = "Options",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Progress bar + times (elapsed on the left, remaining on the right)
                val currentFraction = if (song.durationSec > 0) {
                    currentPositionSec.toFloat() / song.durationSec.toFloat()
                } else 0f

                var isDragging by remember { mutableStateOf(false) }
                var dragFraction by remember { mutableStateOf(0f) }

                ThinSeekBar(
                    value = if (isDragging) dragFraction else currentFraction.coerceIn(0f, 1f),
                    onChange = { frac ->
                        isDragging = true
                        dragFraction = frac
                    },
                    onFinished = {
                        isDragging = false
                        onSeek((dragFraction * song.durationSec).toInt())
                    },
                    activeColor = tone,
                    inactiveColor = tone.copy(alpha = 0.28f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp)
                        .testTag("fullscreen_seek_slider")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val displaySec = if (isDragging) (dragFraction * song.durationSec).toInt() else currentPositionSec
                    val remaining = (song.durationSec - displaySec).coerceAtLeast(0)
                    Text(text = formatTime(displaySec), color = tone, fontSize = 11.sp)
                    Text(text = "\u200E-" + formatTime(remaining), color = tone, fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Rewind / Play-Pause / Forward
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onPrevious,
                        modifier = Modifier
                            .size(64.dp)
                            .testTag("fullscreen_prev_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FastRewind,
                            contentDescription = "Previous",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(72.dp)
                            .testTag("fullscreen_play_pause_btn")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                    }

                    IconButton(
                        onClick = onNext,
                        modifier = Modifier
                            .size(64.dp)
                            .testTag("fullscreen_next_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FastForward,
                            contentDescription = "Next",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Volume (real device music volume)
                VolumeRow(tone = tone)

                Spacer(modifier = Modifier.height(10.dp))

                // Shuffle | Sleep timer | Queue. All take the song's colour.
                val isTimerActive = (sleepTimerSeconds != null && sleepTimerSeconds > 0) || sleepTimerMinutes != null
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleShuffle,
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("fullscreen_shuffle_btn")
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (isShuffle) Color.White else tone,
                                modifier = Modifier.size(26.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .padding(top = 3.dp)
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(if (isShuffle) Color.White else Color.Transparent)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .height(52.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .clickable(onClick = onOpenSleepTimer)
                            .padding(horizontal = 14.dp)
                            .testTag("fullscreen_timer_btn"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = "Sleep Timer",
                            tint = if (isTimerActive) Color.White else tone,
                            modifier = Modifier.size(26.dp)
                        )
                        if (sleepTimerSeconds != null && sleepTimerSeconds > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "%02d:%02d".format(sleepTimerSeconds / 60, sleepTimerSeconds % 60),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenQueue,
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("fullscreen_queue_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                            contentDescription = "Queue",
                            tint = tone,
                            modifier = Modifier.size(26.dp)
                        )
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
            containerColor = lerp(coverAccent, Color.Black, 0.72f),
            shape = RoundedCornerShape(28.dp),
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
    tone: Color,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    // Glass segmented control: Song | Lyrics. The selected segment slides smoothly.
    val isSong = currentMode == PlayerViewMode.SONG
    val segmentWidth = 84.dp
    val offset by animateDpAsState(
        targetValue = if (isSong) 0.dp else segmentWidth,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = 0.8f, stiffness = 380f
        ),
        label = "segmentOffset"
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black.copy(alpha = 0.22f))
                .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .offset(x = offset)
                    .width(segmentWidth)
                    .height(32.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                SegmentLabel(
                    text = strings.playerSongMode,
                    selected = isSong,
                    tone = tone,
                    width = segmentWidth,
                    onClick = { onModeChange(PlayerViewMode.SONG) },
                    modifier = Modifier.testTag("toggle_song_mode")
                )
                SegmentLabel(
                    text = strings.playerLyricsMode,
                    selected = !isSong,
                    tone = tone,
                    width = segmentWidth,
                    onClick = { onModeChange(PlayerViewMode.LYRICS) },
                    modifier = Modifier.testTag("toggle_lyrics_mode")
                )
            }
        }
    }
}

@Composable
private fun SegmentLabel(
    text: String,
    selected: Boolean,
    tone: Color,
    width: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(width)
            .height(32.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) ResonanceGreen else Color.White,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun PlayerCircleButton(
    onClick: () -> Unit,
    fill: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(fill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

/** Thin progress bar: tap or drag anywhere on it. Always drawn left-to-right, even in RTL. */
@Composable
private fun ThinSeekBar(
    value: Float,
    onChange: (Float) -> Unit,
    onFinished: () -> Unit,
    activeColor: Color,
    inactiveColor: Color,
    modifier: Modifier = Modifier,
    trackHeight: androidx.compose.ui.unit.Dp = 5.dp,
    showThumb: Boolean = true
) {
    val latestChange by rememberUpdatedState(onChange)
    val latestFinished by rememberUpdatedState(onFinished)
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    latestChange((offset.x / size.width).coerceIn(0f, 1f))
                    latestFinished()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { offset -> latestChange((offset.x / size.width).coerceIn(0f, 1f)) },
                    onDragEnd = { latestFinished() },
                    onDragCancel = { latestFinished() },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        latestChange((change.position.x / size.width).coerceIn(0f, 1f))
                    }
                )
            }
            .drawBehind {
                val h = trackHeight.toPx()
                val y = (size.height - h) / 2f
                val r = androidx.compose.ui.geometry.CornerRadius(h / 2f)
                drawRoundRect(
                    color = inactiveColor,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, y),
                    size = androidx.compose.ui.geometry.Size(size.width, h),
                    cornerRadius = r
                )
                val w = size.width * value.coerceIn(0f, 1f)
                drawRoundRect(
                    color = activeColor,
                    topLeft = androidx.compose.ui.geometry.Offset(0f, y),
                    size = androidx.compose.ui.geometry.Size(w.coerceAtLeast(h), h),
                    cornerRadius = r
                )
                if (showThumb) {
                    drawCircle(
                        color = activeColor,
                        radius = 6.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(
                            w.coerceIn(6.dp.toPx(), size.width - 6.dp.toPx()),
                            size.height / 2f
                        )
                    )
                }
            }
    )
}

/** Device music volume: mute icon, slider, speaker icon. Follows the hardware volume keys too. */
@Composable
private fun VolumeRow(tone: Color) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager }
    val maxVol = remember { audioManager.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    var volume by remember { mutableStateOf(audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).toFloat() / maxVol) }
    var userDragging by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: android.content.Context?, i: Intent?) {
                if (!userDragging) {
                    volume = audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC).toFloat() / maxVol
                }
            }
        }
        val filter = android.content.IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        context.registerReceiver(receiver, filter)
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("fullscreen_volume_row"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.VolumeMute,
            contentDescription = "Volume down",
            tint = tone,
            modifier = Modifier
                .size(18.dp)
                .clickable {
                    val v = (audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) - 1).coerceAtLeast(0)
                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, v, 0)
                    volume = v.toFloat() / maxVol
                }
        )
        Spacer(modifier = Modifier.width(10.dp))
        ThinSeekBar(
            value = volume,
            onChange = { f ->
                userDragging = true
                volume = f
                audioManager.setStreamVolume(
                    android.media.AudioManager.STREAM_MUSIC,
                    (f * maxVol).roundToInt().coerceIn(0, maxVol),
                    0
                )
            },
            onFinished = { userDragging = false },
            activeColor = tone,
            inactiveColor = tone.copy(alpha = 0.28f),
            trackHeight = 4.dp,
            showThumb = false,
            modifier = Modifier
                .weight(1f)
                .height(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Icon(
            imageVector = Icons.Rounded.VolumeUp,
            contentDescription = "Volume up",
            tint = tone,
            modifier = Modifier
                .size(20.dp)
                .clickable {
                    val v = (audioManager.getStreamVolume(android.media.AudioManager.STREAM_MUSIC) + 1).coerceAtMost(maxVol)
                    audioManager.setStreamVolume(android.media.AudioManager.STREAM_MUSIC, v, 0)
                    volume = v.toFloat() / maxVol
                }
        )
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
