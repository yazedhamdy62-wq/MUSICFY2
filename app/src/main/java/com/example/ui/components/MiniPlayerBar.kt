package com.example.ui.components

import com.example.data.AppStrings
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Song
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ResonanceGreen
import com.example.ui.theme.ResonanceGreenGlow

@Composable
fun MiniPlayerBar(
    strings: AppStrings,
    song: Song,
    isPlaying: Boolean,
    progressFraction: Float,
    isLiked: Boolean,
    onToggleLike: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isGlassEffect: Boolean = true
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground || MaterialTheme.colorScheme.surface == DarkSurface

    val titleColor = if (isDark) Color.White else Color(0xFF0F172A)
    val artistColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
    val nextIconColor = if (isDark) Color.White else Color(0xFF1E293B)
    val progressTrackColor = if (isDark) Color(0xFF2D3748) else Color(0xFFE2E8F0)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .shadow(
                elevation = if (isDark) 20.dp else 10.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.15f),
                spotColor = if (isGlassEffect && isDark) ResonanceGreen.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.15f)
            )
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .pointerInput(Unit) {
                var totalDragY = 0f
                detectVerticalDragGestures(
                    onDragStart = { totalDragY = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        totalDragY += dragAmount
                        if (totalDragY < -20f) { // Swiped UP -> expand player
                            change.consume()
                            onClick()
                        }
                    }
                )
            }
            .testTag("mini_player_bar")
    ) {
        // Liquid glass or solid background layer (ultra-smooth 0ms GPU cost)
        val glassBgBrush: Brush? = remember(isGlassEffect, isDark) {
            if (isGlassEffect) {
                if (isDark) {
                    Brush.verticalGradient(listOf(Color(0xF218222C), Color(0xEA101620)))
                } else {
                    Brush.verticalGradient(listOf(Color(0xF6FFFFFF), Color(0xEEF1F5F9)))
                }
            } else null
        }

        val glassBorderBrush: Brush? = remember(isGlassEffect, isDark) {
            if (isGlassEffect) {
                if (isDark) {
                    Brush.verticalGradient(listOf(Color(0x60FFFFFF), Color(0x3000E676), Color(0x15FFFFFF)))
                } else {
                    Brush.verticalGradient(listOf(Color(0xE0FFFFFF), Color(0x6094A3B8)))
                }
            } else null
        }

        Box(
            modifier = Modifier
                .matchParentSize()
                .then(
                    if (isGlassEffect && glassBgBrush != null && glassBorderBrush != null) {
                        val bgBrush: Brush = glassBgBrush
                        val borderBrush: Brush = glassBorderBrush
                        Modifier
                            .background(bgBrush)
                            .border(
                                BorderStroke(1.2.dp, borderBrush),
                                RoundedCornerShape(18.dp)
                            )
                    } else {
                        if (isDark) {
                            Modifier
                                .background(Color(0xFF15191E))
                                .border(BorderStroke(1.2.dp, Color(0xFF2B3540)), RoundedCornerShape(18.dp))
                        } else {
                            Modifier
                                .background(Color(0xFFFFFFFF))
                                .border(BorderStroke(1.2.dp, Color(0xFFE2E8F0)), RoundedCornerShape(18.dp))
                        }
                    }
                )
        )
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Album artwork thumbnail
                SongCoverArtwork(
                    coverType = song.coverType,
                    gradientColors = song.gradientColors,
                    customImageUri = song.customImageUri,
                    albumArtUri = song.albumArtUri,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    isPlaying = isPlaying
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Song Title & Artist
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 6.dp)
                ) {
                    Text(
                        text = song.title,
                        color = titleColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = strings.displayArtist(song.artist),
                        color = artistColor,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Heart / Like button
                IconButton(
                    onClick = onToggleLike,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("mini_player_like")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isLiked) ResonanceGreen else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Circular Play/Pause button with Spotify-like neon green accent
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = CircleShape,
                            spotColor = ResonanceGreenGlow
                        )
                        .clip(CircleShape)
                        .background(ResonanceGreen)
                        .clickable(onClick = onPlayPause)
                        .testTag("mini_player_play_pause"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // Next button
                IconButton(
                    onClick = onNext,
                    modifier = Modifier
                        .size(38.dp)
                        .testTag("mini_player_next")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = nextIconColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Slim progress bar along the bottom edge of the mini player
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .background(progressTrackColor)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progressFraction.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(ResonanceGreen)
                )
            }
        }
    }
}
