package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.FolderSourceType

/**
 * App-style picture for a folder.
 *
 * Well-known sources get their own look (WhatsApp, Telegram, Downloads). Every other folder, or a
 * folder from a less-known app (SnapTube, VidMate, plain folders ...) gets the same blue glass
 * folder picture.
 */
@Composable
fun FolderSourceBadge(
    sourceType: FolderSourceType,
    size: Dp,
    modifier: Modifier = Modifier
) {
    when (sourceType) {
        FolderSourceType.WHATSAPP -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(size * 0.24f))
                    .background(Color(0xFF25D366)),
                contentAlignment = Alignment.Center
            ) {
                // white speech-bubble ring with a phone handset inside, like the WhatsApp logo
                Box(
                    modifier = Modifier
                        .size(size * 0.62f)
                        .drawBehind {
                            val w = this.size.width
                            val stroke = w * 0.11f
                            drawCircle(
                                color = Color.White,
                                radius = (w - stroke) / 2f,
                                style = Stroke(width = stroke)
                            )
                            // tail of the bubble (bottom-left)
                            val tail = androidx.compose.ui.graphics.Path().apply {
                                moveTo(w * 0.04f, w * 0.98f)
                                lineTo(w * 0.10f, w * 0.64f)
                                lineTo(w * 0.34f, w * 0.88f)
                                close()
                            }
                            drawPath(tail, Color.White)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Call,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(size * 0.32f)
                    )
                }
            }
        }

        FolderSourceType.TELEGRAM -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(Color(0xFF37AEE2), Color(0xFF1E96C8)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Send,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.5f)
                )
            }
        }

        FolderSourceType.DOWNLOADS -> {
            Box(
                modifier = modifier
                    .size(size)
                    .clip(RoundedCornerShape(size * 0.2f))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = null,
                    tint = Color(0xFF0B0F12),
                    modifier = Modifier.size(size * 0.62f)
                )
            }
        }

        else -> GlassFolderPicture(size = size, modifier = modifier)
    }
}

/** Blue folder with a frosted glass sheet in front (the SnapTube-style generic picture). */
@Composable
private fun GlassFolderPicture(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .drawBehind {
                val w = this.size.width
                val h = this.size.height
                val folderBrush = Brush.linearGradient(
                    listOf(Color(0xFF00C6FF), Color(0xFF0072FF)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
                // folder tab
                drawRoundRect(
                    brush = folderBrush,
                    topLeft = Offset(w * 0.18f, h * 0.10f),
                    size = Size(w * 0.34f, h * 0.20f),
                    cornerRadius = CornerRadius(w * 0.06f)
                )
                // folder body
                drawRoundRect(
                    brush = folderBrush,
                    topLeft = Offset(w * 0.18f, h * 0.20f),
                    size = Size(w * 0.70f, h * 0.60f),
                    cornerRadius = CornerRadius(w * 0.08f)
                )
                // frosted glass sheet in front, shifted to the lower left
                val sheetTopLeft = Offset(w * 0.08f, h * 0.42f)
                val sheetSize = Size(w * 0.66f, h * 0.48f)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.50f), Color.White.copy(alpha = 0.16f)),
                        startY = sheetTopLeft.y,
                        endY = sheetTopLeft.y + sheetSize.height
                    ),
                    topLeft = sheetTopLeft,
                    size = sheetSize,
                    cornerRadius = CornerRadius(w * 0.08f)
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.55f),
                    topLeft = sheetTopLeft,
                    size = sheetSize,
                    cornerRadius = CornerRadius(w * 0.08f),
                    style = Stroke(width = w * 0.015f)
                )
            }
    )
}
