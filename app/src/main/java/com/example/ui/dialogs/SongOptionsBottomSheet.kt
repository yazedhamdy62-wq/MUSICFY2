package com.example.ui.dialogs

import androidx.compose.ui.platform.LocalContext
import com.example.data.shareSongFile
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ui.components.GlassOverlay
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
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.AppStrings
import com.example.data.CoverArtType
import com.example.data.LocalizationManager
import com.example.data.Playlist
import com.example.data.Song
import com.example.ui.components.SongCoverArtwork
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ResonanceGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongOptionsBottomSheet(
    song: Song,
    playlists: List<Playlist> = emptyList(),
    currentPlaylist: Playlist? = null,
    showPlayNext: Boolean = false,
    strings: AppStrings = LocalizationManager.Arabic,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit = {},
    onAddToPlaylist: (Playlist) -> Unit = {},
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onChangeCover: (CoverArtType, List<Color>) -> Unit,
    onChangeCustomCover: (String) -> Unit = {},
    onEditTitleAndArtist: (String, String) -> Unit,
    onDeleteSong: () -> Unit
) {
    val context = LocalContext.current
    var showCoverDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRemoveFromPlaylistDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background == DarkBackground || MaterialTheme.colorScheme.surface == DarkSurface
    val cardBg = if (isDark) Color(0xFF1A2128) else Color(0xFFF1F5F9)
    val optionCardBg = if (isDark) Color(0xFF1C242C) else Color(0xFFF8FAFC)
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0xFF8E9BAE) else Color(0xFF64748B)
    val borderColor = if (isDark) Color(0xFF26333D) else Color(0xFFE2E8F0)

    GlassOverlay(
        onDismiss = onDismiss,
        bottomSheet = true,
        modifier = Modifier.testTag("song_options_bottom_sheet")
    ) { close ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Song header: cover + title + artist
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SongCoverArtwork(
                    coverType = song.coverType,
                    gradientColors = song.gradientColors,
                    customImageUri = song.customImageUri,
                    albumArtUri = song.albumArtUri,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    isPlaying = false
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${strings.displayArtist(song.artist)} • ${song.durationFormatted}",
                        color = Color.White.copy(alpha = 0.62f),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Actions: same set as before, laid out like the Figma sheet (2 on top, 3 below)
            val tiles = buildList<GlassOption> {
                if (showPlayNext) add(GlassOption(strings.playNext, Icons.AutoMirrored.Outlined.QueueMusic) { onPlayNext(); close() })
                add(GlassOption(strings.addToPlaylist, Icons.Outlined.PlaylistAdd) { showAddToPlaylistDialog = true })
                add(GlassOption(strings.changeCover, Icons.Outlined.Image) { showCoverDialog = true })
                if (onRemoveFromPlaylist != null) {
                    add(GlassOption(strings.removeFromPlaylist, Icons.Outlined.PlaylistRemove) { showRemoveFromPlaylistDialog = true })
                }
                add(GlassOption(strings.shareSong, Icons.Outlined.Share) { shareSongFile(context, song); close() })
                add(GlassOption(strings.editSongInfo, Icons.Outlined.Edit) { showEditDialog = true })
                add(GlassOption(strings.deleteSong, Icons.Outlined.Delete, danger = true) { showDeleteDialog = true })
            }
            val firstRowCount = if (showPlayNext) 3 else 2
            val rows = buildList {
                add(tiles.take(firstRowCount))
                addAll(tiles.drop(firstRowCount).chunked(3))
            }
            rows.forEachIndexed { rowIdx, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { opt ->
                        GlassOptionTile(opt, Modifier.weight(1f))
                    }
                    // keep tile width equal when a row is not full
                    val full = if (rowIdx == 0) firstRowCount else 3
                    repeat(full - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }

    // Dialog 1: Change Cover (Includes Device Photo Picker & Presets)
    if (showCoverDialog) {
        ChangeCoverPresetDialog(
            currentCoverType = song.coverType,
            strings = strings,
            isDark = isDark,
            onSelectCustom = { uri ->
                onChangeCustomCover(uri)
                showCoverDialog = false
                onDismiss()
            },
            onSelect = { type, colors ->
                onChangeCover(type, colors)
                showCoverDialog = false
                onDismiss()
            },
            onDismiss = { showCoverDialog = false }
        )
    }

    // Dialog 2: Add to Playlist
    if (showAddToPlaylistDialog) {
        val eligiblePlaylists = remember(playlists) {
            playlists.filter { it.id != "p_liked" }
        }
        AlertDialog(
            onDismissRequest = { showAddToPlaylistDialog = false },
            containerColor = if (isDark) Color(0xFF192027) else Color.White,
            title = {
                Text(
                    text = strings.selectPlaylistTitle,
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (eligiblePlaylists.isEmpty()) {
                    Text(
                        text = strings.emptyPlaylist,
                        color = subTextColor,
                        fontSize = 14.sp
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(eligiblePlaylists) { pl ->
                            val alreadyIn = pl.songIds.contains(song.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDark) Color(0xFF222B35) else Color(0xFFF1F5F9))
                                    .clickable {
                                        onAddToPlaylist(pl)
                                        showAddToPlaylistDialog = false
                                        onDismiss()
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SongCoverArtwork(
                                    coverType = pl.coverType,
                                    gradientColors = pl.gradientColors,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pl.title,
                                        color = textColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${pl.songCount} ${strings.songsCountSuffix}",
                                        color = subTextColor,
                                        fontSize = 12.sp
                                    )
                                }
                                if (alreadyIn) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = ResonanceGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddToPlaylistDialog = false }) {
                    Text(strings.cancelBtn, color = subTextColor)
                }
            }
        )
    }

    // Dialog 3: Edit Title and Artist
    if (showEditDialog) {
        EditSongTagsDialog(
            initialTitle = song.title,
            initialArtist = song.artist,
            strings = strings,
            isDark = isDark,
            onSave = { newTitle, newArtist ->
                onEditTitleAndArtist(newTitle, newArtist)
                showEditDialog = false
                onDismiss()
            },
            onDismiss = { showEditDialog = false }
        )
    }

    // Dialog 4: Delete from device & app Confirmation
    if (showDeleteDialog) {
        DeleteSongConfirmDialog(
            songTitle = song.title,
            strings = strings,
            isDark = isDark,
            onConfirm = {
                onDeleteSong()
                showDeleteDialog = false
                onDismiss()
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    // Dialog 5: Remove from Playlist Confirmation (Explicitly does NOT delete from device)
    if (showRemoveFromPlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveFromPlaylistDialog = false },
            containerColor = if (isDark) Color(0xFF192027) else Color.White,
            title = {
                Text(
                    text = strings.removeFromPlaylist,
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = strings.removeFromPlaylistConfirm,
                    color = subTextColor,
                    fontSize = 14.sp,
                    lineHeight = 21.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRemoveFromPlaylistDialog = false
                        onRemoveFromPlaylist?.invoke()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = strings.removeFromPlaylist,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRemoveFromPlaylistDialog = false }
                ) {
                    Text(
                        text = strings.cancelBtn,
                        color = subTextColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

private class GlassOption(
    val title: String,
    val icon: ImageVector,
    val danger: Boolean = false,
    val onClick: () -> Unit
)

@Composable
private fun GlassOptionTile(option: GlassOption, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(18.dp)
    val contentColor = if (option.danger) Color(0xFFFF8A80) else Color.White
    Column(
        modifier = modifier
            .height(78.dp)
            .clip(shape)
            .background(Color.White.copy(alpha = 0.10f))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)), shape)
            .clickable(onClick = option.onClick)
            .padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = option.icon,
            contentDescription = option.title,
            tint = contentColor,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = option.title,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Cover Presets Dialog with custom device photo picker
@Composable
fun ChangeCoverPresetDialog(
    currentCoverType: CoverArtType,
    strings: AppStrings,
    isDark: Boolean,
    onSelectCustom: (String) -> Unit,
    onSelect: (CoverArtType, List<Color>) -> Unit,
    onDismiss: () -> Unit
) {
    data class CoverPreset(val name: String, val type: CoverArtType, val colors: List<Color>)

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onSelectCustom(uri.toString())
        }
    }

    val presets = remember {
        listOf(
            CoverPreset("Tawahin Radial", CoverArtType.TAWAHIN_WINDMILL, listOf(Color(0xFF0F172A), Color(0xFF020617))),
            CoverPreset("Synthwave Pink", CoverArtType.GRADIENT_PURPLE, listOf(Color(0xFFEC4899), Color(0xFF8B5CF6))),
            CoverPreset("Neon Cyber", CoverArtType.GRADIENT_TEAL, listOf(Color(0xFF10B981), Color(0xFF064E3B))),
            CoverPreset("Sunset Orange", CoverArtType.GRADIENT_ORANGE, listOf(Color(0xFFF97316), Color(0xFF9A3412))),
            CoverPreset("Deep Ocean", CoverArtType.GRADIENT_BLUE, listOf(Color(0xFF06B6D4), Color(0xFF0C4A6E))),
            CoverPreset("Vintage Vinyl", CoverArtType.ALBUM_VINYL, listOf(Color(0xFF1F2937), Color(0xFF111827)))
        )
    }

    val dialogBg = if (isDark) Color(0xFF161C23) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0xFF8E9BAE) else Color(0xFF64748B)
    val borderColor = if (isDark) Color(0xFF26333D) else Color(0xFFE2E8F0)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.changeCover,
                        color = textColor,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = textColor)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Photo Picker from Device
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E2833) else Color(0xFFF1F5F9)),
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("pick_cover_from_device_btn")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ResonanceGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoLibrary,
                                contentDescription = null,
                                tint = ResonanceGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.chooseFromDevice,
                                color = textColor,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = strings.chooseFromDeviceSub,
                                color = subTextColor,
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = subTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = strings.presetStyles,
                    color = subTextColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presets) { preset ->
                        val isCurrent = preset.type == currentCoverType
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onSelect(preset.type, preset.colors) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (isCurrent) 2.5.dp else 1.dp,
                                        color = if (isCurrent) ResonanceGreen else borderColor,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                SongCoverArtwork(
                                    coverType = preset.type,
                                    gradientColors = preset.colors,
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(ResonanceGreen),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.Black,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = strings.coverPresetLabel(preset.name),
                                color = if (isCurrent) ResonanceGreen else textColor,
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// Edit Tags Dialog
@Composable
fun EditSongTagsDialog(
    initialTitle: String,
    initialArtist: String,
    strings: AppStrings,
    isDark: Boolean,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var artist by remember { mutableStateOf(initialArtist) }

    val dialogBg = if (isDark) Color(0xFF141A20) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0xFF8E9BAE) else Color(0xFF64748B)
    val inputBg = if (isDark) DarkSurfaceElevated else Color(0xFFF8FAFC)
    val borderColor = if (isDark) Color(0xFF2E3944) else Color(0xFFCBD5E1)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = strings.editSongTitlePrompt,
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(text = strings.songTitleLabel, color = subTextColor, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ResonanceGreen,
                        unfocusedBorderColor = borderColor,
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(text = strings.artistLabel, color = subTextColor, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ResonanceGreen,
                        unfocusedBorderColor = borderColor,
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.cancelBtn, color = subTextColor)
                    }

                    Button(
                        onClick = {
                            if (title.isNotBlank()) {
                                onSave(title.trim(), artist.trim().ifBlank { "Unknown Artist" })
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ResonanceGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(strings.saveBtn, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Delete Confirmation Dialog
@Composable
fun DeleteSongConfirmDialog(
    songTitle: String,
    strings: AppStrings,
    isDark: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val dialogBg = if (isDark) Color(0xFF141A20) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0xFF8E9BAE) else Color(0xFF64748B)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = dialogBg,
        title = {
            Text(
                text = strings.deleteSong,
                color = textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "${strings.deleteSongConfirm}\n\"$songTitle\"",
                color = subTextColor,
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(strings.deleteSong, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancelBtn, color = subTextColor)
            }
        }
    )
}
