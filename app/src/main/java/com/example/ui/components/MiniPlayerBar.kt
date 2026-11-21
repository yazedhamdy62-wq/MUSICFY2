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

    val titleColor = if (isDark) Color.White else Color(0xFF020617)
    val artistColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF1E293B)
    val nextIconColor = if (isDark) Color.White else Color(0xFF020617)
    val progressTrackColor = if (isGlassEffect) {
        if (isDark) Color(0x24FFFFFF) else Color(0x14000000)
    } else {
        if (isDark) Color(0xFF2D3748) else Color(0xFFE2E8F0)
    }
    // Rounder corners so the mini player matches the pill-shaped bottom navigation bar
    val miniShape = RoundedCornerShape(28.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .then(
                // Shadows look muddy through transparent glass, so they are only used for the solid style
                if (isGlassEffect) {
                    Modifier
                } else {
                    Modifier.shadow(
                        elevation = if (isDark) 20.dp else 10.dp,
                        shape = miniShape,
                        ambientColor = if (isDark) Color.Black.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.15f),
                        spotColor = Color.Black.copy(alpha = 0.15f)
                    )
                }
            )
            .clip(miniShape)
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
        // Liquid glass (blur + edge refraction of what is behind) or the plain fallbacks
        val screenBackdrop = LocalGlassBackdrop.current
        val glassTint = if (isDark) Color(0xB3262626) else Color(0x8CFFFFFF)
        val glassRim = Brush.linearGradient(
            if (isDark) listOf(Color(0x66FFFFFF), Color(0x1AFFFFFF), Color(0x33FFFFFF))
            else listOf(Color(0xE6FFFFFF), Color(0x3394A3B8), Color(0x99FFFFFF))
        )

        if (isGlassEffect && screenBackdrop != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .glassSurface(
                        worlds = listOf(screenBackdrop),
                        shape = miniShape,
                        blur = 4.dp,
                        refractionHeight = 12.dp,
                        refractionAmount = 18.dp,
                        onDrawSurface = {
                            drawRect(glassTint)
                            drawGlassRim(28.dp.toPx(), glassRim, 1.dp.toPx())
                        }
                    )
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        if (isGlassEffect) {
                            Modifier
                                .background(glassTint)
                                .border(BorderStroke(1.dp, glassRim), miniShape)
                        } else if (isDark) {
                            Modifier
                                .background(Color(0xFF15191E))
                                .border(BorderStroke(1.2.dp, Color(0xFF2B3540)), miniShape)
                        } else {
                            Modifier
                                .background(Color(0xFFFFFFFF))
                                .border(BorderStroke(1.2.dp, Color(0xFFE2E8F0)), miniShape)
                        }
                    )
            )
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
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
                        tint = if (isLiked) ResonanceGreen else (if (isDark) Color(0xFF8E8E93) else Color(0xFF334155)),
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

            // Slim rounded progress bar inset from the edges (fits the pill shape)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 7.dp)
                    .height(2.5.dp)
                    .clip(RoundedCornerShape(50))
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
