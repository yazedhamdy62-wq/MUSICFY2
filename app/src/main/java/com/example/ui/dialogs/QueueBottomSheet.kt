package com.example.ui.dialogs

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.AppStrings
import com.example.data.Song
import com.example.ui.components.SongCoverArtwork
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ResonanceGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueBottomSheet(
    currentSong: Song,
    isPlaying: Boolean,
    queue: List<Song>,
    strings: AppStrings,
    onPlaySong: (Song) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onMoveToTop: (String) -> Unit,
    onReorderUpcoming: (List<Song>) -> Unit = {},
    onRemoveFromQueue: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground || MaterialTheme.colorScheme.surface == DarkSurface
    val bgColor = if (isDark) Color(0xFF14191F) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val textSubColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBg = if (isDark) DarkSurfaceElevated else Color(0xFFF8FAFC)
    val borderColor = if (isDark) Color(0xFF26333D) else Color(0xFFE2E8F0)

    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val itemHeightPx = with(density) { 72.dp.toPx() }

    // Derive upcoming items (all queue tracks except the currently playing one)
    val upcomingSongsState = remember { mutableStateListOf<Song>() }

    LaunchedEffect(queue, currentSong) {
        val currentIdx = queue.indexOfFirst { it.id == currentSong.id }
        val filtered = if (currentIdx >= 0) {
            queue.filterIndexed { index, _ -> index != currentIdx }
        } else {
            queue.filter { it.id != currentSong.id }
        }
        upcomingSongsState.clear()
        upcomingSongsState.addAll(filtered)
    }

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var selectedSongForAction by remember { mutableStateOf<Song?>(null) }

    val listState = rememberLazyListState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = bgColor,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = textSubColor.copy(alpha = 0.5f))
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(ResonanceGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QueueMusic,
                            contentDescription = null,
                            tint = ResonanceGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "قائمة الانتظار والتشغيل التالي",
                            color = textColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${upcomingSongsState.size} أغانٍ في الانتظار (اسحب للترتيب)",
                            color = textSubColor,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = textSubColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Now Playing Card
            Text(
                text = "قيد التشغيل الآن",
                color = ResonanceGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(cardBg)
                    .border(BorderStroke(1.5.dp, ResonanceGreen.copy(alpha = 0.7f)), RoundedCornerShape(14.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongCoverArtwork(
                    coverType = currentSong.coverType,
                    gradientColors = currentSong.gradientColors,
                    customImageUri = currentSong.customImageUri,
                    albumArtUri = currentSong.albumArtUri,
                    isPlaying = isPlaying,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentSong.title,
                        color = textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentSong.artist,
                        color = textSubColor,
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Playing animation badge
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ResonanceGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Upcoming Queue Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "التالي في قائمة التشغيل",
                        color = textColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF1E2833) else Color(0xFFE2E8F0))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${upcomingSongsState.size}",
                            color = ResonanceGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "اضغط مطولاً للسحب وتغيير الترتيب",
                    color = textSubColor,
                    fontSize = 11.sp
                )
            }

            if (upcomingSongsState.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .padding(bottom = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "لا توجد أغانٍ قادمة في قائمة الانتظار",
                        color = textSubColor,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 440.dp)
                        .padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = upcomingSongsState,
                        key = { _, s -> s.id }
                    ) { index, song ->
                        val isDragging = draggingIndex == index

                        val elevation by animateDpAsState(
                            targetValue = if (isDragging) 12.dp else 0.dp,
                            label = "elevation"
                        )
                        val itemScale by animateFloatAsState(
                            targetValue = if (isDragging) 1.03f else 1.0f,
                            label = "scale"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (isDragging) 10f else 1f)
                                .graphicsLayer {
                                    scaleX = itemScale
                                    scaleY = itemScale
                                    if (isDragging) {
                                        translationY = dragOffsetY
                                    }
                                }
                                .shadow(elevation, RoundedCornerShape(14.dp))
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isDragging) (if (isDark) Color(0xFF1B2631) else Color(0xFFEDE9FE)) else cardBg)
                                .border(
                                    BorderStroke(
                                        if (isDragging) 2.dp else 1.dp,
                                        if (isDragging) ResonanceGreen else borderColor
                                    ),
                                    RoundedCornerShape(14.dp)
                                )
                                .pointerInput(upcomingSongsState) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggingIndex = index
                                            dragOffsetY = 0f
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount.y

                                            val currentDragIdx = draggingIndex ?: return@detectDragGesturesAfterLongPress
                                            val targetIdx = when {
                                                dragOffsetY > itemHeightPx * 0.7f && currentDragIdx < upcomingSongsState.size - 1 -> currentDragIdx + 1
                                                dragOffsetY < -itemHeightPx * 0.7f && currentDragIdx > 0 -> currentDragIdx - 1
                                                else -> null
                                            }

                                            if (targetIdx != null) {
                                                val item = upcomingSongsState.removeAt(currentDragIdx)
                                                upcomingSongsState.add(targetIdx, item)
                                                draggingIndex = targetIdx
                                                dragOffsetY = 0f
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        },
                                        onDragEnd = {
                                            draggingIndex = null
                                            dragOffsetY = 0f
                                            onReorderUpcoming(upcomingSongsState.toList())
                                        },
                                        onDragCancel = {
                                            draggingIndex = null
                                            dragOffsetY = 0f
                                            onReorderUpcoming(upcomingSongsState.toList())
                                        }
                                    )
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Drag Handle & Track Order Number
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.width(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DragHandle,
                                        contentDescription = "Drag to reorder",
                                        tint = if (isDragging) ResonanceGreen else textSubColor.copy(alpha = 0.6f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "${index + 1}",
                                        color = if (isDragging) ResonanceGreen else textSubColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Artwork
                                SongCoverArtwork(
                                    coverType = song.coverType,
                                    gradientColors = song.gradientColors,
                                    customImageUri = song.customImageUri,
                                    albumArtUri = song.albumArtUri,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )

                                Spacer(modifier = Modifier.width(10.dp))

                                // Title & Artist
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                ) {
                                    Text(
                                        text = song.title,
                                        color = textColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = song.artist,
                                        color = textSubColor,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // Instant Action Buttons: Move Up, Move Down, Menu
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (index > 0) {
                                                val item = upcomingSongsState.removeAt(index)
                                                upcomingSongsState.add(index - 1, item)
                                                onReorderUpcoming(upcomingSongsState.toList())
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        },
                                        enabled = index > 0,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Move Up",
                                            tint = if (index > 0) textColor else textSubColor.copy(alpha = 0.3f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (index < upcomingSongsState.size - 1) {
                                                val item = upcomingSongsState.removeAt(index)
                                                upcomingSongsState.add(index + 1, item)
                                                onReorderUpcoming(upcomingSongsState.toList())
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        },
                                        enabled = index < upcomingSongsState.size - 1,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Move Down",
                                            tint = if (index < upcomingSongsState.size - 1) textColor else textSubColor.copy(alpha = 0.3f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { selectedSongForAction = song },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Options",
                                            tint = textSubColor,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal Sheet / Dialog for Reorder Options & Quick Actions
    selectedSongForAction?.let { song ->
        val currentRank = upcomingSongsState.indexOfFirst { it.id == song.id } + 1

        AlertDialog(
            onDismissRequest = { selectedSongForAction = null },
            containerColor = bgColor,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(ResonanceGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#$currentRank",
                            color = ResonanceGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = song.title,
                        color = textColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "اختر الإجراء لترتيب تشغيل هذه الأغنية في قائمة الانتظار:",
                        color = textSubColor,
                        fontSize = 13.sp
                    )

                    // 1. Play Next (Move to Top of Upcoming Queue)
                    Button(
                        onClick = {
                            val idx = upcomingSongsState.indexOfFirst { it.id == song.id }
                            if (idx >= 0) {
                                val item = upcomingSongsState.removeAt(idx)
                                upcomingSongsState.add(0, item)
                                onReorderUpcoming(upcomingSongsState.toList())
                            } else {
                                onMoveToTop(song.id)
                            }
                            selectedSongForAction = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ResonanceGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.VerticalAlignTop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تشغيل أولاً (التالي مباشرة بالدور)", fontWeight = FontWeight.Bold)
                    }

                    // 2. Play Right Now
                    OutlinedButton(
                        onClick = {
                            onPlaySong(song)
                            selectedSongForAction = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
                        border = BorderStroke(1.dp, borderColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ResonanceGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تشغيل الآن فوراً")
                    }

                    // 3. Move Down to End
                    OutlinedButton(
                        onClick = {
                            val idx = upcomingSongsState.indexOfFirst { it.id == song.id }
                            if (idx >= 0 && idx < upcomingSongsState.size - 1) {
                                val item = upcomingSongsState.removeAt(idx)
                                upcomingSongsState.add(item)
                                onReorderUpcoming(upcomingSongsState.toList())
                            }
                            selectedSongForAction = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
                        border = BorderStroke(1.dp, borderColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.VerticalAlignBottom, contentDescription = null, tint = ResonanceGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("نقل إلى نهاية قائمة الانتظار")
                    }

                    // 4. Remove from Queue
                    OutlinedButton(
                        onClick = {
                            upcomingSongsState.removeAll { it.id == song.id }
                            onRemoveFromQueue(song.id)
                            selectedSongForAction = null
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إزالة من قائمة الانتظار")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { selectedSongForAction = null }) {
                    Text("إلغاء", color = textSubColor)
                }
            }
        )
    }
}
