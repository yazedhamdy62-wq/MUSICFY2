package com.example.ui.dialogs

import com.example.data.AppStrings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ResonanceGreen

@Composable
fun EqualizerDialog(
    strings: AppStrings,
    bands: List<Int>,
    activePreset: String,
    onBandChange: (Int, Int) -> Unit,
    onPresetSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val bandLabels = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
    val presets = listOf("Bass Boost", "Vocal & Bass", "Rock", "Electronic", "Flat")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF141A20)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("equalizer_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = strings.eqDialogTitle,
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = strings.eqDialogSubtitle,
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Presets Horizontal Row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presets) { preset ->
                        val isSelected = activePreset == preset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) ResonanceGreen else DarkSurfaceElevated)
                                .clickable { onPresetSelect(preset) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = strings.eqPresetLabel(preset),
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 5 Sliders Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    bands.forEachIndexed { index, value ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (value > 0) "+$value" else "$value",
                                color = if (value != 0) ResonanceGreen else Color(0xFF64748B),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Vertical Slider simulation using column with discrete steps
                            Slider(
                                value = value.toFloat(),
                                onValueChange = { onBandChange(index, it.toInt()) },
                                valueRange = -10f..10f,
                                steps = 19,
                                colors = SliderDefaults.colors(
                                    thumbColor = ResonanceGreen,
                                    activeTrackColor = ResonanceGreen,
                                    inactiveTrackColor = Color(0xFF26333D)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = bandLabels.getOrElse(index) { "" },
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ResonanceGreen,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(strings.eqSaved, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
