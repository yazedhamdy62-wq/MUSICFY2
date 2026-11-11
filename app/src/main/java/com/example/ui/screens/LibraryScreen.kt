package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppStrings
import com.example.data.CoverArtType
import com.example.data.LocalizationManager
import com.example.data.Playlist
import com.example.data.Song
import com.example.ui.components.*
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.LightSurfaceElevated
import com.example.ui.theme.ResonanceGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    playlists: List<Playlist>,
    allSongs: List<Song> = emptyList(),
    onPlaylistClick: (Playlist) -> Unit,
    onCreateNewPlaylist: (String) -> Unit,
    onRenamePlaylist: (Playlist, String) -> Unit = { _, _ -> },
    onDeletePlaylist: (Playlist) -> Unit = {},
    onScanDeviceClick: () -> Unit = {},
    strings: AppStrings = LocalizationManager.Arabic,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var isGridView by remember { mutableStateOf(false) }

    // Playlist options states
    var selectedPlaylistForOptions by remember { mutableStateOf<Playlist?>(null) }
    var playlistToRename by remember { mutableStateOf<Playlist?>(null) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }
    var renameInput by remember { mutableStateOf("") }

    val isDark = MaterialTheme.colorScheme.background == DarkBackground || MaterialTheme.colorScheme.surface == DarkSurface
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val textSubColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBackground = if (isDark) DarkSurfaceElevated else Color.White
    val borderColor = if (isDark) Color(0xFF26333D) else Color(0xFFE2E8F0)

    val filteredPlaylists = remember(playlists, searchQuery) {
        if (searchQuery.isBlank()) playlists
        else playlists.filter {
            it.title.contains(searchQuery, ignoreCase = true) || it.subtitle.contains(searchQuery, ignoreCase = true)
        }
    }

    val playlistFirstSongs = remember(allSongs, filteredPlaylists) {
        filteredPlaylists.associate { p -> p.id to findPlaylistFirstSong(p, allSongs) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Top Bar: "Your Library" + Scan + Search + Add
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.libraryTitle,
                color = textColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onScanDeviceClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("library_scan_device_btn")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = strings.scanDevice,
                    tint = textSubColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = { isSearchActive = !isSearchActive },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("library_search_toggle")
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = strings.searchPlaylistsHint,
                    tint = if (isSearchActive) ResonanceGreen else textSubColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = {
                    newPlaylistName = "قائمة تشغيل ${playlists.size + 1}"
                    showCreateDialog = true
                },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("library_add_playlist_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = strings.createNewPlaylist,
                    tint = textSubColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // Search Field
        AnimatedVisibility(visible = isSearchActive) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(strings.searchPlaylistsHint, color = textSubColor) },
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        // Filter / Sort row: Recents + Count
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
                text = "${strings.playlistsHeader} (${playlists.size})",
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.weight(1f))

            IconButton(
                onClick = { isGridView = !isGridView },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isGridView) ResonanceGreen.copy(alpha = 0.15f) else Color.Transparent)
                    .testTag("library_grid_toggle_btn")
            ) {
                Icon(
                    imageVector = if (isGridView) Icons.Outlined.ViewList else Icons.Outlined.GridView,
                    contentDescription = "Toggle Grid/List",
                    tint = if (isGridView) ResonanceGreen else textSubColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Playlists: List or Grid view
        if (isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 210.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = filteredPlaylists,
                    key = { it.id },
                    contentType = { "playlist_grid_card" }
                ) { playlist ->
                    val firstSong = playlistFirstSongs[playlist.id]

                    PlaylistItemGridCard(
                        playlist = playlist,
                        firstSong = firstSong,
                        allSongs = allSongs,
                        cardBg = cardBackground,
                        textColor = textColor,
                        subTextColor = textSubColor,
                        borderColor = borderColor,
                        onClick = { onPlaylistClick(playlist) },
                        onOptionsClick = {
                            selectedPlaylistForOptions = playlist
                        }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 210.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(
                    items = filteredPlaylists,
                    key = { it.id },
                    contentType = { "playlist_list_row" }
                ) { playlist ->
                    val firstSong = playlistFirstSongs[playlist.id]

                    PlaylistItemRow(
                        playlist = playlist,
                        firstSong = firstSong,
                        allSongs = allSongs,
                        textColor = textColor,
                        subTextColor = textSubColor,
                        onClick = { onPlaylistClick(playlist) },
                        onOptionsClick = {
                            selectedPlaylistForOptions = playlist
                        }
                    )
                }
            }
        }
    }

    // Modal Sheet for Playlist Options (Rename / Delete)
    selectedPlaylistForOptions?.let { targetPlaylist ->
        val targetFirstSong = remember(allSongs, targetPlaylist) {
            findPlaylistFirstSong(targetPlaylist, allSongs)
        }

        ModalBottomSheet(
            onDismissRequest = { selectedPlaylistForOptions = null },
            containerColor = if (isDark) Color(0xFF14191F) else Color.White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header preview
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isDark) Color(0xFF1E2833) else Color(0xFFF1F5F9))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlaylistArtwork(
                        playlist = targetPlaylist,
                        firstSong = targetFirstSong,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = targetPlaylist.title,
                            color = textColor,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${targetPlaylist.songCount} ${strings.songsCountSuffix}",
                            color = textSubColor,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Options Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Option: Rename
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0xFF1C242C) else Color(0xFFF8FAFC))
                            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(14.dp))
                            .clickable {
                                renameInput = targetPlaylist.title
                                playlistToRename = targetPlaylist
                                selectedPlaylistForOptions = null
                            }
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFBBF24).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.renamePlaylist,
                            color = textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Option: Delete
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0xFF1C242C) else Color(0xFFF8FAFC))
                            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(14.dp))
                            .clickable {
                                playlistToDelete = targetPlaylist
                                selectedPlaylistForOptions = null
                            }
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF87171).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = strings.deletePlaylist,
                            color = textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Rename Playlist Dialog
    playlistToRename?.let { pl ->
        AlertDialog(
            onDismissRequest = { playlistToRename = null },
            containerColor = if (isDark) Color(0xFF192027) else Color.White,
            title = {
                Text(
                    text = strings.renamePlaylist,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ResonanceGreen,
                            unfocusedBorderColor = borderColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            onRenamePlaylist(pl, renameInput.trim())
                            playlistToRename = null
                        }
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
                TextButton(onClick = { playlistToRename = null }) {
                    Text(strings.cancelBtn, color = textSubColor)
                }
            }
        )
    }

    // Delete Playlist Confirmation Dialog
    playlistToDelete?.let { pl ->
        AlertDialog(
            onDismissRequest = { playlistToDelete = null },
            containerColor = if (isDark) Color(0xFF192027) else Color.White,
            title = {
                Text(
                    text = strings.deletePlaylist,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "${strings.deletePlaylistConfirm}\n\"${pl.title}\"",
                    color = textSubColor,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeletePlaylist(pl)
                        playlistToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(strings.deletePlaylist, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { playlistToDelete = null }) {
                    Text(strings.cancelBtn, color = textSubColor)
                }
            }
        )
    }

    // Create Playlist Dialog
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = if (isDark) Color(0xFF192027) else Color.White,
            title = {
                Text(
                    text = strings.createPlaylistTitle,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = strings.playlistNameLabel,
                        color = textSubColor,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ResonanceGreen,
                            unfocusedBorderColor = borderColor,
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newPlaylistName.trim().ifEmpty { "قائمة جديدة" }
                        onCreateNewPlaylist(name)
                        showCreateDialog = false
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
                TextButton(onClick = { showCreateDialog = false }) {
                    Text(strings.cancelBtn, color = textSubColor)
                }
            }
        )
    }
}

@Composable
private fun PlaylistItemRow(
    playlist: Playlist,
    firstSong: Song? = null,
    allSongs: List<Song> = emptyList(),
    textColor: Color,
    subTextColor: Color,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag("playlist_item_${playlist.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail Cover
        PlaylistArtwork(
            playlist = playlist,
            firstSong = firstSong,
            allSongs = allSongs,
            modifier = Modifier
                .size(62.dp)
                .clip(
                    if (playlist.coverType == CoverArtType.ARTIST_CIRCLE) CircleShape
                    else RoundedCornerShape(12.dp)
                ),
            cornerRadius = 12.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        // Title and Subtitle with Pin badge
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.title,
                color = if (playlist.id == "p_liked") ResonanceGreen else textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (playlist.isPinned) {
                    Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = "Pinned",
                        tint = ResonanceGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                val count = playlist.songCount
                val countText = if (count == 0) "0 أغانٍ" else if (count == 1) "أغنية واحدة" else "$count أغانٍ"
                val displaySubtitle = if (playlist.id == "p_liked") "المفضلة • $countText" else "قائمة تشغيل • $countText"

                Text(
                    text = displaySubtitle,
                    color = subTextColor,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // 3-dots options menu for non-system playlists
        if (playlist.id != "p_liked") {
            IconButton(
                onClick = onOptionsClick,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("playlist_options_${playlist.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Playlist options",
                    tint = subTextColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PlaylistItemGridCard(
    playlist: Playlist,
    firstSong: Song? = null,
    allSongs: List<Song> = emptyList(),
    cardBg: Color,
    textColor: Color,
    subTextColor: Color,
    borderColor: Color,
    onClick: () -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            PlaylistArtwork(
                playlist = playlist,
                firstSong = firstSong,
                allSongs = allSongs,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(
                        if (playlist.coverType == CoverArtType.ARTIST_CIRCLE) CircleShape
                        else RoundedCornerShape(12.dp)
                    ),
                cornerRadius = 12.dp
            )

            if (playlist.id != "p_liked") {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .clickable(onClick = onOptionsClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (playlist.isPinned) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = "Pinned",
                        tint = ResonanceGreen,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = playlist.title,
            color = if (playlist.id == "p_liked") ResonanceGreen else textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        val count = playlist.songCount
        val countText = if (count == 0) "0 أغانٍ" else if (count == 1) "أغنية واحدة" else "$count أغانٍ"
        Text(
            text = countText,
            color = subTextColor,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}
