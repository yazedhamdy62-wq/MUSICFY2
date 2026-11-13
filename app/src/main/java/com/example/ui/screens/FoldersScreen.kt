package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.components.SongCoverArtwork
import com.example.ui.theme.*

@Composable
fun FoldersScreen(
    allSongs: List<Song>,
    currentSong: Song,
    isPlaying: Boolean,
    onSongClick: (Song, List<Song>) -> Unit,
    onPlayFolder: (List<Song>, Boolean) -> Unit,
    onToggleLike: (Song) -> Unit,
    onSongOptionsClick: (Song) -> Unit,
    onScanDeviceClick: () -> Unit,
    strings: AppStrings,
    modifier: Modifier = Modifier
) {
    val folders = remember(allSongs) { FolderManager.groupSongsByFolder(allSongs) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(false) } // toggle between rectangular rows and square grid
    var selectedFolderId by remember { mutableStateOf<String?>(null) }

    val selectedFolder = remember(folders, selectedFolderId) {
        folders.find { it.id == selectedFolderId }
    }

    // Intercept back button when a folder's content is open
    BackHandler(enabled = selectedFolder != null) {
        selectedFolderId = null
    }

    val isDark = MaterialTheme.colorScheme.background == DarkBackground || MaterialTheme.colorScheme.surface == DarkSurface
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val textSubColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBackground = if (isDark) DarkSurfaceElevated else Color.White
    val borderColor = if (isDark) Color(0xFF26333D) else Color(0xFFE2E8F0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        AnimatedContent(
            targetState = selectedFolder,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally { it } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally { -it / 3 } + fadeOut(tween(180)))
                } else {
                    (slideInHorizontally { -it / 3 } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally { it } + fadeOut(tween(180)))
                }
            },
            label = "folder_view_transition"
        ) { activeFolder ->
            if (activeFolder != null) {
                // Folder Songs Detail View
                FolderDetailContent(
                    folder = activeFolder,
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    onBack = { selectedFolderId = null },
                    onSongClick = { song -> onSongClick(song, activeFolder.songs) },
                    onPlayAll = { shuffle -> onPlayFolder(activeFolder.songs, shuffle) },
                    onToggleLike = onToggleLike,
                    onSongOptionsClick = onSongOptionsClick,
                    cardBackground = cardBackground,
                    cardBorder = borderColor,
                    textColor = textColor,
                    subTextColor = textSubColor,
                    strings = strings
                )
            } else {
                // Main Folders Overview matching LibraryScreen design language
                FoldersOverviewContent(
                    folders = folders,
                    totalSongsCount = allSongs.size,
                    searchQuery = searchQuery,
                    isSearchActive = isSearchActive,
                    isGridView = isGridView,
                    onToggleSearch = { isSearchActive = !isSearchActive },
                    onToggleGridView = { isGridView = !isGridView },
                    onSearchQueryChange = { searchQuery = it },
                    onFolderClick = { folder -> selectedFolderId = folder.id },
                    onPlayFolder = onPlayFolder,
                    onScanDeviceClick = onScanDeviceClick,
                    cardBackground = cardBackground,
                    cardBorder = borderColor,
                    textColor = textColor,
                    subTextColor = textSubColor,
                    strings = strings
                )
            }
        }
    }
}

@Composable
private fun FoldersOverviewContent(
    folders: List<MusicFolder>,
    totalSongsCount: Int,
    searchQuery: String,
    isSearchActive: Boolean,
    isGridView: Boolean,
    onToggleSearch: () -> Unit,
    onToggleGridView: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onFolderClick: (MusicFolder) -> Unit,
    onPlayFolder: (List<Song>, Boolean) -> Unit,
    onScanDeviceClick: () -> Unit,
    cardBackground: Color,
    cardBorder: Color,
    textColor: Color,
    subTextColor: Color,
    strings: AppStrings
) {
    val filteredFolders = remember(folders, searchQuery, strings) {
        if (searchQuery.isBlank()) folders
        else folders.filter { folder ->
            folder.name.contains(searchQuery, ignoreCase = true) ||
                    strings.folderDisplayName(folder.name).contains(searchQuery, ignoreCase = true) ||
                    folder.fullPath.contains(searchQuery, ignoreCase = true) ||
                    folder.badgeName.contains(searchQuery, ignoreCase = true) ||
                    folder.songs.any { it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar: "Audio Folders" + Scan Device + Search
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.foldersTitle,
                color = textColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onScanDeviceClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("folders_scan_device_btn")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = strings.scanDevice,
                    tint = subTextColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = onToggleSearch,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("folders_search_toggle_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = strings.searchFoldersHint,
                    tint = if (isSearchActive) ResonanceGreen else subTextColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Expandable Search Field
        AnimatedVisibility(visible = isSearchActive) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(strings.searchFoldersHint, color = subTextColor, fontSize = 14.sp) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = subTextColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ResonanceGreen,
                    unfocusedBorderColor = cardBorder,
                    focusedContainerColor = cardBackground,
                    unfocusedContainerColor = cardBackground,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        // Filter / Header row with View Toggle Button (Squares / Rectangles)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.SwapVert,
                contentDescription = null,
                tint = ResonanceGreen,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${strings.navFolders} (${folders.size})",
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            // Toggle Button between Grid (Squares) and List (Rectangles)
            IconButton(
                onClick = onToggleGridView,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isGridView) ResonanceGreen.copy(alpha = 0.15f) else Color.Transparent)
                    .testTag("folders_grid_toggle_btn")
            ) {
                Icon(
                    imageVector = if (isGridView) Icons.Outlined.ViewList else Icons.Outlined.GridView,
                    contentDescription = "Toggle Grid or List View",
                    tint = if (isGridView) ResonanceGreen else subTextColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Empty state when no folders found
        if (filteredFolders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(cardBackground)
                            .border(BorderStroke(1.dp, cardBorder), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = subTextColor,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isEmpty()) strings.noFoldersFound else strings.noMatchingResults,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (searchQuery.isEmpty()) strings.noFoldersFoundDesc else strings.tryOtherFolderSearch,
                        fontSize = 13.sp,
                        color = subTextColor,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                    if (searchQuery.isEmpty()) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onScanDeviceClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ResonanceGreen,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(strings.scanStorageAndApps, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Folders Display: Grid View (Squares) OR List View (Rectangles)
            if (isGridView) {
                // Square Grid Mode (مربعات)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 220.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = filteredFolders,
                        key = { it.id },
                        contentType = { "folder_grid_card" }
                    ) { folder ->
                        FolderGridCardItem(
                            strings = strings,
                            folder = folder,
                            onClick = { onFolderClick(folder) },
                            onPlay = { onPlayFolder(folder.songs, false) },
                            cardBackground = cardBackground,
                            cardBorder = cardBorder,
                            textColor = textColor,
                            subTextColor = subTextColor
                        )
                    }
                }
            } else {
                // Rectangular List Mode (شكل المستطيل)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = filteredFolders,
                        key = { it.id },
                        contentType = { "folder_list_row" }
                    ) { folder ->
                        FolderListRowItem(
                            strings = strings,
                            folder = folder,
                            onClick = { onFolderClick(folder) },
                            onPlay = { onPlayFolder(folder.songs, false) },
                            cardBackground = cardBackground,
                            cardBorder = cardBorder,
                            textColor = textColor,
                            subTextColor = subTextColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * Rectangular List Row (شكل المستطيل) matching the exact Musicfy style of PlaylistItemRow.
 */
@Composable
private fun FolderListRowItem(
    strings: AppStrings,
    folder: MusicFolder,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    cardBackground: Color,
    cardBorder: Color,
    textColor: Color,
    subTextColor: Color,
    modifier: Modifier = Modifier
) {
    val sourceIcon = getFolderSourceIcon(folder.sourceType)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(cardBackground)
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag("folder_row_${folder.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // App Origin Thumbnail Container (56dp)
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(folder.accentColor.copy(alpha = 0.28f), folder.accentColor.copy(alpha = 0.12f))
                    )
                )
                .border(BorderStroke(1.dp, folder.accentColor.copy(alpha = 0.35f)), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = sourceIcon,
                contentDescription = folder.name,
                tint = folder.accentColor,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Center Details (Name, Badge, Monospace Path, Stats)
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = strings.folderDisplayName(folder.name),
                    color = textColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                // Origin Tag Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(folder.accentColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = strings.folderBadgeDisplay(folder.badgeName),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = folder.accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Full filesystem path
            Text(
                text = folder.fullPath,
                fontSize = 11.sp,
                color = subTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(3.dp))

            // Songs count and total duration
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = strings.songCountText(folder.songCount),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = folder.accentColor
                )
                Text(text = "•", fontSize = 11.sp, color = subTextColor)
                Text(text = folder.totalDurationFormatted, fontSize = 12.sp, color = subTextColor)
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Quick Play Button
        IconButton(
            onClick = onPlay,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(ResonanceGreen.copy(alpha = 0.14f))
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Play Folder",
                tint = ResonanceGreen,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Square Grid Card (شكل المربعات) matching PlaylistItemGridCard.
 */
@Composable
private fun FolderGridCardItem(
    strings: AppStrings,
    folder: MusicFolder,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    cardBackground: Color,
    cardBorder: Color,
    textColor: Color,
    subTextColor: Color,
    modifier: Modifier = Modifier
) {
    val sourceIcon = getFolderSourceIcon(folder.sourceType)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBackground)
            .border(BorderStroke(1.dp, cardBorder), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
            .testTag("folder_grid_${folder.id}")
    ) {
        // Square 1:1 Aspect Ratio Cover Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(folder.accentColor.copy(alpha = 0.32f), folder.accentColor.copy(alpha = 0.12f))
                    )
                )
                .border(BorderStroke(1.dp, folder.accentColor.copy(alpha = 0.35f)), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = sourceIcon,
                contentDescription = folder.name,
                tint = folder.accentColor,
                modifier = Modifier.size(54.dp)
            )

            // Origin Badge at Top-Start
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = strings.folderBadgeDisplay(folder.badgeName),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = folder.accentColor
                )
            }

            // Quick Play Floating Circle at Bottom-End
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.7f))
                    .clickable(onClick = onPlay),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Folder",
                    tint = ResonanceGreen,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Title
        Text(
            text = strings.folderDisplayName(folder.name),
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Full directory path
        Text(
            text = folder.fullPath,
            color = subTextColor,
            fontSize = 10.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Subtitle stats
        Text(
            text = "${strings.songCountText(folder.songCount)} • ${strings.durationHoursMinutes(folder.totalDurationSec)}",
            color = folder.accentColor,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FolderDetailContent(
    folder: MusicFolder,
    currentSong: Song,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onSongClick: (Song) -> Unit,
    onPlayAll: (Boolean) -> Unit,
    onToggleLike: (Song) -> Unit,
    onSongOptionsClick: (Song) -> Unit,
    cardBackground: Color,
    cardBorder: Color,
    textColor: Color,
    subTextColor: Color,
    strings: AppStrings
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredSongs = remember(folder.songs, searchQuery) {
        if (searchQuery.isBlank()) folder.songs
        else folder.songs.filter {
            it.title.contains(searchQuery, ignoreCase = true) || it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 220.dp)
    ) {
        // Back Bar & Title
        item(key = "detail_top_bar", contentType = "top_bar") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = textColor
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = strings.backToFolders,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = subTextColor
                )
            }
        }

        // Large Folder Header Card
        item(key = "detail_header_card", contentType = "header_card") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                folder.accentColor.copy(alpha = 0.22f),
                                cardBackground
                            )
                        )
                    )
                    .border(BorderStroke(1.dp, folder.accentColor.copy(alpha = 0.35f)), RoundedCornerShape(20.dp))
                    .padding(18.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(folder.accentColor.copy(alpha = 0.25f))
                                .border(BorderStroke(1.dp, folder.accentColor.copy(alpha = 0.5f)), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getFolderSourceIcon(folder.sourceType),
                                contentDescription = null,
                                tint = folder.accentColor,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.folderDisplayName(folder.name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${strings.songCountText(folder.songCount)} • ${strings.durationHoursMinutes(folder.totalDurationSec)}",
                                fontSize = 13.sp,
                                color = subTextColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Path chip container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.35f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = folder.fullPath,
                            fontSize = 11.sp,
                            color = folder.accentColor.copy(alpha = 0.9f),
                            fontFamily = FontFamily.Monospace,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Action Buttons (Play All & Shuffle)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { onPlayAll(false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ResonanceGreen,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = strings.playAll, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        OutlinedButton(
                            onClick = { onPlayAll(true) },
                            border = BorderStroke(1.2.dp, cardBorder),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = textColor
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = null,
                                tint = ResonanceGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = strings.shuffle, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        // Search Songs Inside Folder
        if (folder.songs.size > 5) {
            item(key = "folder_songs_search", contentType = "search") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        placeholder = {
                            Text(text = strings.searchInFolder(strings.folderDisplayName(folder.name)), color = subTextColor, fontSize = 13.sp)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = subTextColor, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = null, tint = subTextColor, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = cardBackground,
                            unfocusedContainerColor = cardBackground,
                            focusedBorderColor = ResonanceGreen.copy(alpha = 0.5f),
                            unfocusedBorderColor = cardBorder,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        )
                    )
                }
            }
        }

        // Songs list inside this folder
        items(
            items = filteredSongs,
            key = { it.id },
            contentType = { "folder_song_row" }
        ) { song ->
            val isCurrentThis = song.id == currentSong.id

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSongClick(song) }
                    .padding(horizontal = 20.dp, vertical = 7.dp)
                    .testTag("folder_song_${song.id}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album artwork thumbnail
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    SongCoverArtwork(
                        coverType = song.coverType,
                        gradientColors = song.gradientColors,
                        customImageUri = song.customImageUri,
                        albumArtUri = song.albumArtUri,
                        modifier = Modifier.fillMaxSize(),
                        isPlaying = isCurrentThis && isPlaying
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Song Title & Artist
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isCurrentThis) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCurrentThis) ResonanceGreen else textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${strings.displayArtist(song.artist)} • ${song.durationFormatted}",
                        style = MaterialTheme.typography.bodySmall,
                        color = subTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Favorite / Like Button
                IconButton(
                    onClick = { onToggleLike(song) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (song.isLiked) ResonanceGreen else subTextColor,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // 3-Dots Options Button
                IconButton(
                    onClick = { onSongOptionsClick(song) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Song options",
                        tint = subTextColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun getFolderSourceIcon(sourceType: FolderSourceType): ImageVector {
    return when (sourceType) {
        FolderSourceType.WHATSAPP -> Icons.Default.ChatBubble
        FolderSourceType.TELEGRAM -> Icons.Default.Send
        FolderSourceType.DOWNLOADS -> Icons.Default.Download
        FolderSourceType.MUSIC -> Icons.Default.MusicNote
        FolderSourceType.BLUETOOTH -> Icons.Default.Bluetooth
        FolderSourceType.RECORDINGS -> Icons.Default.Mic
        FolderSourceType.SNAPTUBE, FolderSourceType.VIDMATE -> Icons.Default.PlayCircle
        FolderSourceType.GENERAL -> Icons.Default.Folder
    }
}
