package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppStrings
import com.example.data.LocalizationManager
import com.example.data.Playlist
import com.example.data.Song
import com.example.ui.components.*
import com.example.ui.components.MusicfyLogo
import com.example.ui.components.PlaylistArtwork
import com.example.ui.components.SongCoverArtwork
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ResonanceGreen
import com.example.ui.theme.ResonanceGreenGlow

@Composable
fun HomeScreen(
    allSongs: List<Song>,
    playlists: List<Playlist>,
    currentSong: Song,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onShuffleAll: () -> Unit,
    onPlayAll: () -> Unit,
    onOpenEqualizer: () -> Unit = {},
    onOpenPlaylist: (Playlist) -> Unit,
    onToggleLike: (Song) -> Unit,
    onSongOptionsClick: (Song) -> Unit = {},
    onScanDeviceClick: () -> Unit = {},
    onDeleteSongs: (List<String>) -> Unit = {},
    strings: AppStrings = LocalizationManager.Arabic,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var sortOrder by remember { mutableStateOf("Date Added") }
    var showSortMenu by remember { mutableStateOf(false) }

    // Batch deletion / selection mode
    var isBatchMode by remember { mutableStateOf(false) }
    val selectedSongIds = remember { mutableStateListOf<String>() }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background == DarkBackground || MaterialTheme.colorScheme.surface == DarkSurface
    val cardBackground = if (isDark) DarkSurfaceElevated else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val textSubColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val borderColor = if (isDark) Color(0xFF26333D) else Color(0xFFE2E8F0)

    val filteredSongs = remember(allSongs, searchQuery, sortOrder) {
        val list = if (searchQuery.isBlank()) {
            allSongs
        } else {
            allSongs.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.artist.contains(searchQuery, ignoreCase = true)
            }
        }
        when (sortOrder) {
            "Title" -> list.sortedBy { it.title }
            "Artist" -> list.sortedBy { it.artist }
            "Duration" -> list.sortedByDescending { it.durationSec }
            else -> list
        }
    }

    val playlistFirstSongs = remember(allSongs, playlists) {
        playlists.associate { p -> p.id to findPlaylistFirstSong(p, allSongs) }
    }

    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // App Header: Circular Musicfy Monogram Logo + "Musicfy" + Search icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo without background box, adapts black/white to dark/light theme
            MusicfyLogo(
                isDarkTheme = isDark,
                size = 36.dp
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "Musicfy",
                color = textColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.weight(1f))

            // Search Icon
            IconButton(
                onClick = { isSearchActive = !isSearchActive },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("home_search_toggle")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = if (isSearchActive) ResonanceGreen else textSubColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Search Bar (Animated Expand)
        AnimatedVisibility(visible = isSearchActive) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchHint, color = textSubColor) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ResonanceGreen,
                    unfocusedBorderColor = borderColor,
                    focusedContainerColor = cardBackground,
                    unfocusedContainerColor = cardBackground,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                ),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = textSubColor)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .testTag("home_search_field")
            )
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 210.dp)
        ) {
            // "LOCAL LIBRARY" Subtitle & "All Songs" Header
            item(key = "header_local_library", contentType = "header") {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                    Text(
                        text = strings.localLibraryLabel,
                        color = ResonanceGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.allSongs,
                            color = textColor,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Batch selection toggle button
                            IconButton(
                                onClick = {
                                    isBatchMode = !isBatchMode
                                    if (!isBatchMode) selectedSongIds.clear()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isBatchMode) ResonanceGreen.copy(alpha = 0.2f) else cardBackground)
                                    .border(BorderStroke(1.dp, if (isBatchMode) ResonanceGreen else borderColor), CircleShape)
                                    .testTag("batch_select_toggle_btn")
                            ) {
                                Icon(
                                    imageVector = if (isBatchMode) Icons.Default.Close else Icons.Default.Checklist,
                                    contentDescription = "Batch Select",
                                    tint = if (isBatchMode) ResonanceGreen else textSubColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Sort chip dropdown
                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(cardBackground)
                                        .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(20.dp))
                                        .clickable { showSortMenu = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = strings.sortLabel(sortOrder),
                                        color = textSubColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = ResonanceGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showSortMenu,
                                    onDismissRequest = { showSortMenu = false },
                                    modifier = Modifier.background(cardBackground)
                                ) {
                                    listOf("Date Added", "Title", "Artist", "Duration").forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(strings.sortLabel(opt), color = if (sortOrder == opt) ResonanceGreen else textColor) },
                                            onClick = {
                                                sortOrder = opt
                                                showSortMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Batch selection action bar
            if (isBatchMode) {
                item(key = "batch_action_bar", contentType = "batch_bar") {
                    Surface(
                        color = if (isDark) Color(0xFF1E2833) else Color(0xFFE2E8F0),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        isBatchMode = false
                                        selectedSongIds.clear()
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel",
                                        tint = textColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = strings.selectedCountLabel(selectedSongIds.size),
                                    color = textColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TextButton(
                                    onClick = {
                                        if (selectedSongIds.size == filteredSongs.size) {
                                            selectedSongIds.clear()
                                        } else {
                                            selectedSongIds.clear()
                                            selectedSongIds.addAll(filteredSongs.map { it.id })
                                        }
                                    }
                                ) {
                                    Text(
                                        text = if (selectedSongIds.size == filteredSongs.size) strings.deselectAllShort else strings.selectAllSongs,
                                        color = ResonanceGreen,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (selectedSongIds.isNotEmpty()) {
                                            showDeleteConfirmDialog = true
                                        }
                                    },
                                    enabled = selectedSongIds.isNotEmpty(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFDC2626),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = strings.deleteWithCount(selectedSongIds.size),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Shuffle & Play All Buttons
            item(key = "shuffle_play_actions", contentType = "actions") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onShuffleAll,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ResonanceGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .shadow(8.dp, RoundedCornerShape(24.dp), ambientColor = ResonanceGreenGlow, spotColor = ResonanceGreenGlow)
                            .testTag("home_shuffle_all_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.shuffleBtn,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onPlayAll,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = textColor
                        ),
                        border = BorderStroke(1.dp, borderColor),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("home_play_all_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = ResonanceGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.playBtn,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Playlists Section
            if (playlists.isNotEmpty()) {
                item(key = "playlists_section", contentType = "playlists_grid") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (i in playlists.indices step 2) {
                            val p1 = playlists[i]
                            val p1FirstSong = playlistFirstSongs[p1.id]

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QuickPlaylistItem(
                                    playlist = p1,
                                    firstSong = p1FirstSong,
                                    cardBg = cardBackground,
                                    textColor = textColor,
                                    borderColor = borderColor,
                                    isPlayingThis = (p1.id == "p_liked" && isPlaying),
                                    onClick = { onOpenPlaylist(p1) },
                                    modifier = Modifier.weight(1f)
                                )
                                if (i + 1 < playlists.size) {
                                    val p2 = playlists[i + 1]
                                    val p2FirstSong = playlistFirstSongs[p2.id]

                                    QuickPlaylistItem(
                                        playlist = p2,
                                        firstSong = p2FirstSong,
                                        cardBg = cardBackground,
                                        textColor = textColor,
                                        borderColor = borderColor,
                                        isPlayingThis = false,
                                        onClick = { onOpenPlaylist(p2) },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Songs List or Empty State
            if (filteredSongs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(cardBackground)
                            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(20.dp))
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(if (isDark) Color(0xFF132A1C) else Color(0xFFD1FAE5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.LibraryMusic,
                                    contentDescription = null,
                                    tint = ResonanceGreen,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = strings.noSongsFound,
                                color = textColor,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = strings.noSongsSub,
                                color = textSubColor,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(18.dp))

                            Button(
                                onClick = onScanDeviceClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ResonanceGreen,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(20.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                                modifier = Modifier.testTag("home_scan_empty_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.scanDevice,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            } else {
                items(
                    items = filteredSongs,
                    key = { it.id },
                    contentType = { "song_row" }
                ) { song ->
                    val isSelected = song.id == currentSong.id
                    val isChecked = selectedSongIds.contains(song.id)
                    SongRowItem(
                        strings = strings,
                        song = song,
                        isSelected = isSelected,
                        isPlaying = isSelected && isPlaying,
                        textColor = textColor,
                        subTextColor = textSubColor,
                        isBatchMode = isBatchMode,
                        isChecked = isChecked,
                        onCheckedChange = { checked ->
                            if (checked) selectedSongIds.add(song.id)
                            else selectedSongIds.remove(song.id)
                        },
                        onClick = { onSongClick(song) },
                        onLongClick = {
                            isBatchMode = true
                            if (!selectedSongIds.contains(song.id)) {
                                selectedSongIds.add(song.id)
                            }
                        },
                        onToggleLike = { onToggleLike(song) },
                        onSongOptionsClick = { onSongOptionsClick(song) }
                    )
                }
            }
        }
    }

    // Confirmation dialog for batch deletion
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = if (isDark) Color(0xFF14191F) else Color.White,
            title = {
                Text(
                    text = strings.deleteSongsTitle(selectedSongIds.size),
                    color = textColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = strings.deleteNowConfirmMsg,
                    color = textSubColor,
                    fontSize = 13.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = selectedSongIds.toList()
                        onDeleteSongs(toDelete)
                        selectedSongIds.clear()
                        isBatchMode = false
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(strings.deletePermanentlyBtn, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(strings.cancelBtn, color = textSubColor)
                }
            }
        )
    }
}

@Composable
private fun QuickPlaylistItem(
    playlist: Playlist,
    firstSong: Song? = null,
    allSongs: List<Song> = emptyList(),
    cardBg: Color,
    textColor: Color,
    borderColor: Color,
    isPlayingThis: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(end = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlaylistArtwork(
            playlist = playlist,
            firstSong = firstSong,
            allSongs = allSongs,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)),
            cornerRadius = 0.dp,
            isPlaying = isPlayingThis
        )

        Spacer(modifier = Modifier.width(10.dp))

        Text(
            text = playlist.title,
            color = textColor,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun SongRowItem(
    strings: AppStrings,
    song: Song,
    isSelected: Boolean,
    isPlaying: Boolean,
    textColor: Color,
    subTextColor: Color,
    isBatchMode: Boolean = false,
    isChecked: Boolean = false,
    onCheckedChange: (Boolean) -> Unit = {},
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onToggleLike: () -> Unit,
    onSongOptionsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) ResonanceGreen.copy(alpha = 0.10f) else Color.Transparent)
            .combinedClickable(
                onClick = {
                    if (isBatchMode) {
                        onCheckedChange(!isChecked)
                    } else {
                        onClick()
                    }
                },
                onLongClick = onLongClick
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .testTag("song_item_${song.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isBatchMode) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = ResonanceGreen,
                    checkmarkColor = Color.Black,
                    uncheckedColor = subTextColor
                ),
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            SongCoverArtwork(
                coverType = song.coverType,
                gradientColors = song.gradientColors,
                customImageUri = song.customImageUri,
                albumArtUri = song.albumArtUri,
                modifier = Modifier.fillMaxSize(),
                isPlaying = isPlaying
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Title & Artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (isSelected) ResonanceGreen else textColor,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = strings.displayArtist(song.artist),
                color = subTextColor,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Now-playing indicator (animated bars while playing, static when paused)
        if (isSelected) {
            PlayingEqualizerBars(
                isPlaying = isPlaying,
                modifier = Modifier.padding(end = 10.dp)
            )
        }

        // Duration text
        Text(
            text = song.durationFormatted,
            color = if (isSelected) ResonanceGreen else subTextColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(end = 6.dp)
        )

        if (!isBatchMode) {
            // Heart Icon
            IconButton(
                onClick = onToggleLike,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (song.isLiked) ResonanceGreen else subTextColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 3 Dots Menu
            IconButton(
                onClick = onSongOptionsClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("song_more_${song.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    tint = subTextColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
