package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CoverArtType
import com.example.ui.theme.ResonanceGreen
import kotlin.math.cos
import kotlin.math.sin
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R

@Composable
fun SongCoverArtwork(
    coverType: CoverArtType,
    gradientColors: List<Color>,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    isPlaying: Boolean = false,
    customImageUri: String? = null,
    albumArtUri: String? = null
) {
    val imageModel = remember(customImageUri, albumArtUri) {
        customImageUri?.takeIf { it.isNotBlank() } ?: albumArtUri?.takeIf { it.isNotBlank() }
    }

    val bgBrush = remember(gradientColors) {
        val colors = if (gradientColors.size >= 2) gradientColors else listOf(gradientColors.firstOrNull() ?: Color(0xFF334155), Color(0xFF1E293B))
        Brush.linearGradient(colors = colors)
    }

    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }

    Box(
        modifier = modifier
            .clip(shape)
            .background(bgBrush),
        contentAlignment = Alignment.Center
    ) {
        if (imageModel != null) {
            val context = LocalContext.current
            val imageRequest = remember(imageModel, context) {
                ImageRequest.Builder(context)
                    .data(imageModel)
                    .size(128, 128)
                    .crossfade(false)
                    .allowHardware(true)
                    .precision(coil.size.Precision.INEXACT)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .build()
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = "Song Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else if (coverType == CoverArtType.DEFAULT_PLACEHOLDER || coverType == CoverArtType.CUSTOM_IMAGE) {
            Image(
                painter = painterResource(id = R.drawable.ic_default_song_cover),
                contentDescription = "Default Music Cover",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            when (coverType) {
                CoverArtType.CUSTOM_IMAGE -> {
                    Image(
                        painter = painterResource(id = R.drawable.ic_default_song_cover),
                        contentDescription = "Song Cover",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                CoverArtType.TAWAHIN_WINDMILL -> {
                    TawahinWindmillGraphic(modifier = Modifier.fillMaxSize())
                }
                CoverArtType.GRADIENT_PURPLE -> {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.fillMaxSize(0.5f)
                    )
                }
            CoverArtType.GRADIENT_ORANGE -> {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxSize(0.5f)
                )
            }
            CoverArtType.GRADIENT_BLUE -> {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxSize(0.5f)
                )
            }
            CoverArtType.GRADIENT_TEAL -> {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxSize(0.5f)
                )
            }
            CoverArtType.GRADIENT_MAGENTA -> {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxSize(0.5f)
                )
            }
            CoverArtType.GRADIENT_AMBER -> {
                Text(
                    text = "FEVER\n105",
                    color = Color(0xFFFDE68A),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 11.sp
                )
            }
            CoverArtType.ARTIST_CIRCLE -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.85f)
                        .clip(CircleShape)
                        .background(Color(0xFF262626)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SHHB",
                        color = Color(0xFFF43F5E),
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp
                    )
                }
            }
            CoverArtType.ALBUM_VINYL -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF18181B)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ROXI\nAMIR EID",
                        color = Color(0xFFE4D5B7),
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
            CoverArtType.GRADIENT_DARK -> {
                Icon(
                    imageVector = Icons.Default.NightlightRound,
                    contentDescription = null,
                    tint = Color(0xFF818CF8),
                    modifier = Modifier.fillMaxSize(0.45f)
                )
            }
            CoverArtType.DEFAULT_PLACEHOLDER -> {
                Image(
                    painter = painterResource(id = R.drawable.ic_default_song_cover),
                    contentDescription = "Default Music Cover",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
}

/**
 * Renders the custom Tawahin geometric windmill fan emblem exactly as seen
 * in the user's screenshots (Screenshot 1 & Screenshot 3).
 */
@Composable
fun TawahinWindmillGraphic(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFFF8FAFC)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize(0.82f)) {
            val center = Offset(size.width / 2f, size.height * 0.44f)
            val outerRadius = size.width * 0.38f
            val innerRadius = size.width * 0.08f

            // Outer circle outline
            drawCircle(
                color = Color(0xFF1E293B),
                radius = outerRadius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
            )

            // Inner center hub
            drawCircle(
                color = Color(0xFF0F172A),
                radius = innerRadius,
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = innerRadius * 0.4f,
                center = center
            )

            // 16 radial fan blades/spokes
            val blades = 16
            val angleStep = (2 * Math.PI) / blades
            for (i in 0 until blades) {
                val angle = i * angleStep
                val startX = center.x + (innerRadius * 1.1f * cos(angle)).toFloat()
                val startY = center.y + (innerRadius * 1.1f * sin(angle)).toFloat()
                val endX = center.x + (outerRadius * 0.98f * cos(angle)).toFloat()
                val endY = center.y + (outerRadius * 0.98f * sin(angle)).toFloat()

                drawLine(
                    color = Color(0xFF0F172A),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (i % 2 == 0) 5.5f else 3.5f,
                    cap = StrokeCap.Round
                )
            }

            // Central vertical stand / pole
            val poleTop = center.y + outerRadius * 0.4f
            val poleBottom = size.height * 0.88f
            drawLine(
                color = Color(0xFF0F172A),
                start = Offset(center.x, poleTop),
                end = Offset(center.x, poleBottom),
                strokeWidth = 6.5f,
                cap = StrokeCap.Round
            )
        }

        // TAWAHIN label in bottom left
        Text(
            text = "TAWAHIN",
            color = Color(0xFF0F172A),
            fontWeight = FontWeight.Black,
            fontSize = 9.sp,
            letterSpacing = 1.2.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = 10.dp)
        )
    }
}

/**
 * Animated audio equalizer bars for active song item
 */
@Composable
fun PlayingEqualizerBars(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    barColor: Color = ResonanceGreen
) {
    if (isPlaying) {
        ActiveAnimatedEqualizerBars(modifier = modifier, barColor = barColor)
    } else {
        StaticEqualizerBars(modifier = modifier, barColor = barColor)
    }
}

@Composable
private fun ActiveAnimatedEqualizerBars(
    modifier: Modifier = Modifier,
    barColor: Color = ResonanceGreen
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "h3"
    )

    Row(
        modifier = modifier.height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(h1, h2, h3).forEach { frac ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(frac)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun StaticEqualizerBars(
    modifier: Modifier = Modifier,
    barColor: Color = ResonanceGreen
) {
    Row(
        modifier = modifier.height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(0.4f, 0.7f, 0.3f).forEach { frac ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(frac)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(barColor)
            )
        }
    }
}

/**
 * Helper to reliably resolve the first song of any playlist (Liked or user-created)
 */
fun findPlaylistFirstSong(
    playlist: com.example.data.Playlist,
    allSongs: List<com.example.data.Song>
): com.example.data.Song? {
    if (allSongs.isEmpty()) return null
    if (playlist.id == "p_liked") {
        return allSongs.firstOrNull { it.isLiked }
    }
    // 1. In order of playlist's songIds
    for (id in playlist.songIds) {
        val trimmed = id.trim()
        val s = allSongs.find { it.id == id || it.id == trimmed }
        if (s != null) return s
    }
    // 2. Check by set containment (same as PlaylistDetailScreen)
    val idSet = playlist.songIds.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    if (idSet.isNotEmpty()) {
        val s = allSongs.find { idSet.contains(it.id) || idSet.contains(it.id.trim()) }
        if (s != null) return s
    }
    return null
}

/**
 * Renders playlist cover art dynamically based on the first song in the playlist.
 * If the playlist has songs, it adapts the artwork from the first song (album art, image, or gradient style).
 * If empty, it falls back to the playlist's default artwork.
 */
@Composable
fun PlaylistArtwork(
    playlist: com.example.data.Playlist,
    firstSong: com.example.data.Song? = null,
    allSongs: List<com.example.data.Song> = emptyList(),
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    isPlaying: Boolean = false
) {
    val resolvedSong = firstSong ?: if (allSongs.isNotEmpty()) findPlaylistFirstSong(playlist, allSongs) else null

    if (resolvedSong != null) {
        SongCoverArtwork(
            coverType = resolvedSong.coverType,
            gradientColors = resolvedSong.gradientColors,
            customImageUri = resolvedSong.customImageUri ?: playlist.customImageUri,
            albumArtUri = resolvedSong.albumArtUri,
            modifier = modifier,
            cornerRadius = cornerRadius,
            isPlaying = isPlaying
        )
    } else {
        SongCoverArtwork(
            coverType = playlist.coverType,
            gradientColors = playlist.gradientColors,
            customImageUri = playlist.customImageUri,
            modifier = modifier,
            cornerRadius = cornerRadius,
            isPlaying = isPlaying
        )
    }
}

