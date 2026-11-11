package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AppStrings
import com.example.data.Song
import com.example.ui.components.SongCoverArtwork
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ResonanceGreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongsToPlaylistDialog(
    playlistTitle: String,
    allSongs: List<Song>,
    existingSongIds: List<String>,
    strings: AppStrings,
    onDismiss: () -> Unit,
    onAddSongs: (List<String>) -> Unit
) {
    // Only show songs that aren't already in this playlist
    val availableSongs = remember(allSongs, existingSongIds) {
        allSongs.filter { !existingSongIds.contains(it.id) }
    }

    var searchQuery by remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateListOf<String>() }

    val filteredSongs = remember(availableSongs, searchQuery) {
        if (searchQuery.isBlank()) availableSongs
        else availableSongs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    val isDark = MaterialTheme.colorScheme.background == DarkBackground || MaterialTheme.colorScheme.surface == DarkSurface
    val dialogBg = if (isDark) Color(0xFF161C22) else Color(0xFFFFFFFF)
    val cardBg = if (isDark) DarkSurfaceElevated else Color(0xFFF1F5F9)
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val borderColor = if (isDark) Color(0xFF26333D) else Color(0xFFE2E8F0)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .border(BorderStroke(1.2.dp, borderColor), RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            color = dialogBg,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "إضافة أغانٍ إلى القائمة",
                            color = textColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = playlistTitle,
                            color = ResonanceGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF222C35) else Color(0xFFE2E8F0))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = strings.cancelBtn,
                            tint = textColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "ابحث بالاسم أو الفنان...",
                            color = subTextColor,
                            fontSize = 13.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "بحث",
                            tint = subTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "مسح",
                                    tint = subTextColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ResonanceGreen,
                        unfocusedBorderColor = borderColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedContainerColor = cardBg,
                        unfocusedContainerColor = cardBg
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Multi-select actions bar: "تحديد الكل" & count badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedIds.isEmpty()) "المتاح: ${filteredSongs.size} مسار"
                        else "تم تحديد: ${selectedIds.size} من ${filteredSongs.size}",
                        color = if (selectedIds.isEmpty()) subTextColor else ResonanceGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = {
                                if (selectedIds.size == filteredSongs.size) {
                                    selectedIds.clear()
                                } else {
                                    selectedIds.clear()
                                    selectedIds.addAll(filteredSongs.map { it.id })
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (selectedIds.size == filteredSongs.size && filteredSongs.isNotEmpty()) "إلغاء التحديد" else "تحديد الكل",
                                color = ResonanceGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Songs List
                if (filteredSongs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (availableSongs.isEmpty()) "جميع أغانيك مضافة بالفعل إلى هذه القائمة!"
                            else "لا توجد أغانٍ تطابق بحثك",
                            color = subTextColor,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredSongs, key = { it.id }) { song ->
                            val isSelected = selectedIds.contains(song.id)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) (if (isDark) Color(0xFF1E3228) else Color(0xFFE8F5E9)) else cardBg)
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isSelected) ResonanceGreen.copy(alpha = 0.6f) else borderColor
                                        ),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        if (isSelected) selectedIds.remove(song.id)
                                        else selectedIds.add(song.id)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SongCoverArtwork(
                                    coverType = song.coverType,
                                    gradientColors = song.gradientColors,
                                    customImageUri = song.customImageUri,
                                    albumArtUri = song.albumArtUri,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        color = textColor,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        color = subTextColor,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedIds.add(song.id)
                                        else selectedIds.remove(song.id)
                                    },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = ResonanceGreen,
                                        uncheckedColor = subTextColor,
                                        checkmarkColor = Color.Black
                                    ),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Confirmation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = textColor),
                        border = BorderStroke(1.dp, borderColor)
                    ) {
                        Text(text = "إلغاء", fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            if (selectedIds.isNotEmpty()) {
                                onAddSongs(selectedIds.toList())
                            }
                        },
                        enabled = selectedIds.isNotEmpty(),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("confirm_add_songs_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ResonanceGreen,
                            contentColor = Color.Black,
                            disabledContainerColor = if (isDark) Color(0xFF26333D) else Color(0xFFE2E8F0),
                            disabledContentColor = subTextColor
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (selectedIds.isEmpty()) "إضافة" else "إضافة (${selectedIds.size})",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
