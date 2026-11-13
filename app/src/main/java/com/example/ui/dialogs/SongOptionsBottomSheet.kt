package com.example.ui.dialogs

import androidx.compose.ui.platform.LocalContext
import com.example.data.shareSongFile
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    var showCoverDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRemoveFromPlaylistDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.background == DarkBackground || MaterialTheme.colorScheme.surface == DarkSurface
    val sheetContainerColor = if (isDark) Color(0xFF14191F) else Color(0xFFFFFFFF)
    val cardBg = if (isDark) Color(0xFF1A2128) else Color(0xFFF1F5F9)
    val optionCardBg = if (isDark) Color(0xFF1C242C) else Color(0xFFF8FAFC)
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0xFF8E9BAE) else Color(0xFF64748B)
    val borderColor = if (isDark) Color(0xFF26333D) else Color(0xFFE2E8F0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetContainerColor,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .width(42.dp)
                    .height(4.5.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF333E48) else Color(0xFFCBD5E1))
            )
        },
        modifier = Modifier.testTag("song_options_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Song Info Preview Header Card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(cardBg)
                    .then(if (!isDark) Modifier.border(BorderStroke(1.dp, borderColor), RoundedCornerShape(16.dp)) else Modifier)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cover art thumbnail
                SongCoverArtwork(
                    coverType = song.coverType,
                    gradientColors = song.gradientColors,
                    customImageUri = song.customImageUri,
                    albumArtUri = song.albumArtUri,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    isPlaying = false
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Title and details
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${strings.displayArtist(song.artist)} • ${song.durationFormatted}",
                        color = subTextColor,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Tiles Grid
            // Row 1: Play Next (if applicable) + Add to Playlist
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (showPlayNext) {
                    OptionCardItem(
                        title = strings.playNext,
                        subtitle = strings.optSubQueue,
                        icon = Icons.AutoMirrored.Outlined.QueueMusic,
                        iconTint = ResonanceGreen,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        cardBg = optionCardBg,
                        borderColor = borderColor,
                        onClick = {
                            onPlayNext()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                OptionCardItem(
                    title = strings.addToPlaylist,
                    subtitle = strings.optSubPlaylists,
                    icon = Icons.Outlined.PlaylistAdd,
                    iconTint = ResonanceGreen,
                    textColor = textColor,
                    subTextColor = subTextColor,
                    cardBg = optionCardBg,
                    borderColor = borderColor,
                    onClick = { showAddToPlaylistDialog = true },
                    modifier = Modifier.weight(1f)
                )

                OptionCardItem(
                    title = strings.changeCover,
                    subtitle = strings.optSubArtwork,
                    icon = Icons.Outlined.Image,
                    iconTint = Color(0xFF38BDF8),
                    textColor = textColor,
                    subTextColor = subTextColor,
                    cardBg = optionCardBg,
                    borderColor = borderColor,
                    onClick = { showCoverDialog = true },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (onRemoveFromPlaylist != null) {
                // When opened from a playlist (except Liked/Favorites):
                // Row 2: Edit Info + Remove from this Playlist
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OptionCardItem(
                        title = strings.editSongInfo,
                        subtitle = strings.optSubTags,
                        icon = Icons.Outlined.Edit,
                        iconTint = Color(0xFFFBBF24),
                        textColor = textColor,
                        subTextColor = subTextColor,
                        cardBg = optionCardBg,
                        borderColor = borderColor,
                        onClick = { showEditDialog = true },
                        modifier = Modifier.weight(1f)
                    )

                    OptionCardItem(
                        title = strings.removeFromPlaylist,
                        subtitle = currentPlaylist?.title ?: strings.playlistWord,
                        icon = Icons.Outlined.PlaylistRemove,
                        iconTint = Color(0xFFFF7043),
                        textColor = textColor,
                        subTextColor = subTextColor,
                        cardBg = optionCardBg,
                        borderColor = borderColor,
                        onClick = { showRemoveFromPlaylistDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Row 3: Delete from Device & App (Full width)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OptionCardItem(
                        title = strings.shareSong,
                        subtitle = strings.optSubAudio,
                        icon = Icons.Outlined.Share,
                        iconTint = Color(0xFF60A5FA),
                        textColor = textColor,
                        subTextColor = subTextColor,
                        cardBg = optionCardBg,
                        borderColor = borderColor,
                        onClick = {
                            shareSongFile(context, song)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    OptionCardItem(
                        title = strings.deleteSong,
                        subtitle = strings.optSubDeviceApp,
                        icon = Icons.Outlined.Delete,
                        iconTint = Color(0xFFF87171),
                        textColor = textColor,
                        subTextColor = subTextColor,
                        cardBg = optionCardBg,
                        borderColor = borderColor,
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                // Standard: Row 2 with Edit Info + Delete Song
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OptionCardItem(
                        title = strings.editSongInfo,
                        subtitle = strings.optSubTags,
                        icon = Icons.Outlined.Edit,
                        iconTint = Color(0xFFFBBF24),
                        textColor = textColor,
                        subTextColor = subTextColor,
                        cardBg = optionCardBg,
                        borderColor = borderColor,
                        onClick = { showEditDialog = true },
                        modifier = Modifier.weight(1f)
                    )

                    OptionCardItem(
                        title = strings.shareSong,
                        subtitle = strings.optSubAudio,
                        icon = Icons.Outlined.Share,
                        iconTint = Color(0xFF60A5FA),
                        textColor = textColor,
                        subTextColor = subTextColor,
                        cardBg = optionCardBg,
                        borderColor = borderColor,
                        onClick = {
                            shareSongFile(context, song)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )

                    OptionCardItem(
                        title = strings.deleteSong,
                        subtitle = strings.optSubRemove,
                        icon = Icons.Outlined.Delete,
                        iconTint = Color(0xFFF87171),
                        textColor = textColor,
                        subTextColor = subTextColor,
                        cardBg = optionCardBg,
                        borderColor = borderColor,
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
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

@Composable
private fun OptionCardItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    textColor: Color,
    subTextColor: Color,
    cardBg: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(cardBg)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = subtitle,
            color = subTextColor,
            fontSize = 10.5.sp,
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
