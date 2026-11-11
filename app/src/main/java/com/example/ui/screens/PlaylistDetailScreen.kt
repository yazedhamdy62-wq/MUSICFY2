package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.components.PlaylistArtwork
import com.example.ui.components.SongCoverArtwork
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ResonanceGreen
import com.example.ui.theme.ResonanceGreenGlow

@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    playlistSongs: List<Song>,
    currentSong: Song,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onSongClick: (Song) -> Unit,
    onShufflePlaylist: () -> Unit,
    onPlayPlaylist: () -> Unit,
    onToggleLikeSong: (Song) -> Unit,
    onAddSongs: () -> Unit,
    onSongOptionsClick: (Song) -> Unit = {},
    strings: AppStrings = LocalizationManager.Arabic,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }

    val isDark = MaterialTheme.colorScheme.background == DarkBackground || MaterialTheme.colorScheme.surface == DarkSurface
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val textSubColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBackground = if (isDark) DarkSurfaceElevated else Color.White
    val borderColor = if (isDark) Color(0xFF26333D) else Color(0xFFE2E8F0)

    val filteredSongs = remember(playlistSongs, searchQuery) {
        if (searchQuery.isBlank()) playlistSongs
        else playlistSongs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true)
        }
    }

    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Header: Back Arrow, "MUSICFY", 3 dots
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("playlist_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = strings.back,
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Text(
                text = "MUSICFY",
                color = textSubColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Box(modifier = Modifier.size(48.dp))
        }

        // Search Bar & Sort button row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("${strings.searchHint} ${playlist.title}...", color = textSubColor, fontSize = 13.sp) },
                singleLine = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = textSubColor,
                        modifier = Modifier.size(20.dp)
                    )
                },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ResonanceGreen,
                    unfocusedBorderColor = borderColor,
                    focusedContainerColor = cardBackground,
                    unfocusedContainerColor = cardBackground,
                    focusedTextColor = textColor,
                    unfocusedTextColor = textColor
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
            )
        }

        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 210.dp)
        ) {
            // Playlist Title & Meta + Action Buttons (Shuffle & Play)
            item(key = "playlist_header", contentType = "header") {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    val firstSong = remember(playlistSongs) { playlistSongs.firstOrNull() }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlaylistArtwork(
                            playlist = playlist,
                            firstSong = firstSong,
                            modifier = Modifier
                                .size(110.dp)
                                .shadow(12.dp, RoundedCornerShape(16.dp))
                                .clip(RoundedCornerShape(16.dp)),
                            cornerRadius = 16.dp
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = playlist.title,
                                color = textColor,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "${filteredSongs.size} ${strings.songsCountSuffix}",
                                color = textSubColor,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Row: Shuffle button and glowing Play button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onShufflePlaylist,
                            modifier = Modifier
                                .size(44.dp)
                                .testTag("playlist_shuffle_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = strings.shuffleBtn,
                                tint = ResonanceGreen,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Glowing circular play button
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .shadow(
                                    elevation = 16.dp,
                                    shape = CircleShape,
                                    spotColor = ResonanceGreenGlow,
                                    ambientColor = ResonanceGreen.copy(alpha = 0.5f)
                                )
                                .clip(CircleShape)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(ResonanceGreen, Color(0xFF16A34A))
                                    )
                                )
                                .clickable(onClick = onPlayPlaylist)
                                .testTag("playlist_play_btn"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying && playlistSongs.any { it.id == currentSong.id }) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = strings.playBtn,
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            // "Add to this playlist" Row
            item(key = "playlist_add_songs", contentType = "add_action") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardBackground)
                        .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(12.dp))
                        .clickable(onClick = onAddSongs)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDark) Color(0xFF1E262E) else Color(0xFFF1F5F9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = strings.addToPlaylist,
                            tint = ResonanceGreen,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = strings.addToPlaylist,
                            color = textColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.addSongsPrompt,
                            color = textSubColor,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Empty state if no songs
            if (filteredSongs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.emptyPlaylistPrompt,
                            color = textSubColor,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Songs list in playlist
                itemsIndexed(
                    items = filteredSongs,
                    key = { _, s -> s.id },
                    contentType = { _, _ -> "playlist_song_row" }
                ) { index, song ->
                    val isSelected = song.id == currentSong.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSongClick(song) }
                            .padding(horizontal = 20.dp, vertical = 7.dp)
                            .testTag("playlist_song_${song.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SongCoverArtwork(
                            coverType = song.coverType,
                            gradientColors = song.gradientColors,
                            customImageUri = song.customImageUri,
                            albumArtUri = song.albumArtUri,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            isPlaying = isSelected && isPlaying
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        // Title with Index & Artist
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${index + 1}  ${song.title}",
                                color = if (isSelected) ResonanceGreen else textColor,
                                fontSize = 15.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = song.artist,
                                color = textSubColor,
                                fontSize = 12.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Heart icon
                        IconButton(
                            onClick = { onToggleLikeSong(song) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (song.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (song.isLiked) ResonanceGreen else textSubColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // 3 dots
                        IconButton(
                            onClick = { onSongOptionsClick(song) },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("playlist_song_more_${song.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More",
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
