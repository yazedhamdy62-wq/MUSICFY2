package com.example.ui.dialogs

import com.example.data.AppStrings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.GlassOverlay
import com.example.ui.theme.ResonanceGreen

/**
 * Sleep timer. Same options and behaviour as before, shown as a liquid-glass panel that
 * floats in and out smoothly.
 */
@Composable
fun SleepTimerDialog(
    strings: AppStrings,
    currentMinutes: Int?,
    remainingSeconds: Int?,
    onSelectMinutes: (Int?) -> Unit,
    onSelectEndOfTrack: () -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        Pair(strings.sleepTimerTurnOff, null),
        Pair(strings.minutesLabel(15), 15),
        Pair(strings.minutesLabel(30), 30),
        Pair(strings.minutesLabel(45), 45),
        Pair(strings.oneHourLabel, 60)
    )

    GlassOverlay(onDismiss = onDismiss, bottomSheet = false) { close ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = strings.sleepTimerDialogTitle,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = close) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            // Active timer countdown banner
            if (remainingSeconds != null && remainingSeconds > 0) {
                val timeStr = "%02d:%02d".format(remainingSeconds / 60, remainingSeconds % 60)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.12f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.sleepTimerRunning,
                        color = ResonanceGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = strings.sleepTimeRemaining(timeStr),
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            GlassTimerRow(
                label = strings.endOfCurrentTrack,
                selected = false,
                onClick = {
                    onSelectEndOfTrack()
                    close()
                }
            )

            options.forEach { (label, minutes) ->
                Spacer(modifier = Modifier.height(8.dp))
                GlassTimerRow(
                    label = label,
                    selected = currentMinutes == minutes,
                    onClick = {
                        onSelectMinutes(minutes)
                        close()
                    }
                )
            }
        }
    }
}

@Composable
private fun GlassTimerRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.09f))
            .border(
                BorderStroke(1.dp, if (selected) Color.White.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.15f)),
                shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = ResonanceGreen,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
