package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ResonanceGreen

@Composable
fun SleepTimerDialog(
    currentMinutes: Int?,
    remainingSeconds: Int?,
    onSelectMinutes: (Int?) -> Unit,
    onSelectEndOfTrack: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == DarkBackground || MaterialTheme.colorScheme.surface == DarkSurface
    val dialogBg = if (isDark) Color(0xFF14191F) else Color.White
    val cardBg = if (isDark) Color(0xFF1A222A) else Color(0xFFF8FAFC)
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0xFF8E9BAE) else Color(0xFF64748B)
    val borderColor = if (isDark) Color(0xFF26333D) else Color(0xFFE2E8F0)

    val options = listOf(
        Pair("إيقاف المؤقت (Off)", null),
        Pair("15 دقيقة (15 Minutes)", 15),
        Pair("30 دقيقة (30 Minutes)", 30),
        Pair("45 دقيقة (45 Minutes)", 45),
        Pair("60 دقيقة (1 Hour)", 60)
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg),
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
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
                                .background(ResonanceGreen.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = null,
                                tint = ResonanceGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "مؤقت النوم (Sleep Timer)",
                            color = textColor,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = subTextColor
                        )
                    }
                }

                // Show active timer countdown banner if active
                if (remainingSeconds != null && remainingSeconds > 0) {
                    val remMins = remainingSeconds / 60
                    val remSecs = remainingSeconds % 60
                    val timeStr = "%02d:%02d".format(remMins, remSecs)

                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ResonanceGreen.copy(alpha = 0.12f))
                            .border(1.dp, ResonanceGreen.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "المؤقت يعمل الآن:",
                                color = if (isDark) ResonanceGreen else Color(0xFF047857),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "$timeStr متبقية",
                                color = if (isDark) Color.White else Color(0xFF0F172A),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // End of current track option
                val isEndOfTrack = false
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(cardBg)
                        .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                        .clickable {
                            onSelectEndOfTrack()
                            onDismiss()
                        }
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "نهاية المسار الحالي (End of current track)",
                        color = textColor,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                options.forEach { (label, minutes) ->
                    val isSelected = currentMinutes == minutes
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) ResonanceGreen.copy(alpha = 0.15f) else cardBg)
                            .border(
                                BorderStroke(
                                    1.dp,
                                    if (isSelected) ResonanceGreen else borderColor
                                ),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                onSelectMinutes(minutes)
                                onDismiss()
                            }
                            .padding(horizontal = 16.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) (if (isDark) ResonanceGreen else Color(0xFF047857)) else textColor,
                            fontSize = 13.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )

                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(ResonanceGreen)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
