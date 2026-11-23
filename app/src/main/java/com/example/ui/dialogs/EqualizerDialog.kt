package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioEffectsManager
import com.example.data.AppStrings
import com.example.ui.components.GlassOverlay
import com.example.ui.theme.ResonanceGreen

/** Working equalizer: profiles (built in + saved), 5 bands, bass, spatial and loudness. */
@Composable
fun EqualizerDialog(
    strings: AppStrings,
    fx: AudioEffectsManager,
    onDismiss: () -> Unit
) {
    val bands by fx.bands.collectAsState()
    val active by fx.activeProfile.collectAsState()
    val enabled by fx.enabled.collectAsState()
    val bass by fx.bass.collectAsState()
    val spatial by fx.spatial.collectAsState()
    val loudness by fx.loudness.collectAsState()
    val custom by fx.customProfiles.collectAsState()
    val profiles = AudioEffectsManager.BUILT_IN + custom
    val bandLabels = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")
    var naming by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    val muted = Color.White.copy(alpha = 0.65f)

    GlassOverlay(onDismiss = onDismiss, bottomSheet = true) { close ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
                .testTag("equalizer_dialog")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(strings.eqDialogTitle, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(strings.eqDialogSubtitle, color = muted, fontSize = 12.sp)
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { fx.setEnabled(it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = ResonanceGreen, checkedThumbColor = Color.Black)
                )
                IconButton(onClick = close) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }

            Spacer(Modifier.height(14.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                items(profiles) { p ->
                    Chip(strings.eqPresetLabel(p.name), p.name == active) { fx.selectProfile(p.name) }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth().height(170.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                bands.forEachIndexed { index, value ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(
                            if (value > 0) "+$value" else "$value",
                            color = if (value != 0) ResonanceGreen else muted,
                            fontSize = 11.sp, fontWeight = FontWeight.Bold
                        )
                        VerticalSlider(
                            value = value.toFloat(),
                            onValueChange = { fx.setBand(index, it.toInt()) },
                            enabled = enabled,
                            modifier = Modifier.weight(1f).width(44.dp)
                        )
                        Text(bandLabels[index], color = muted, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            EffectSlider(strings.tr("الباس", "Bass", "Basses"), bass, enabled) { fx.setBass(it) }
            EffectSlider(strings.tr("صوت محيطي", "Spatial", "Spatial"), spatial, enabled) { fx.setSpatial(it) }
            EffectSlider(strings.tr("رفع الصوت", "Loudness", "Volume"), loudness, enabled) { fx.setLoudness(it) }

            Spacer(Modifier.height(14.dp))

            if (naming) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.10f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        if (newName.isEmpty()) Text(strings.tr("اسم البروفايل", "Profile name", "Nom du profil"), color = muted, fontSize = 14.sp)
                        BasicTextField(
                            value = newName, onValueChange = { newName = it.take(24) }, singleLine = true,
                            textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                            cursorBrush = SolidColor(ResonanceGreen)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { fx.saveCurrentAs(newName); naming = false; newName = "" },
                        enabled = newName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = ResonanceGreen, contentColor = Color.Black),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text(strings.tr("حفظ", "Save", "Enregistrer"), fontWeight = FontWeight.Bold) }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { naming = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                    ) { Text(strings.tr("حفظ كبروفايل", "Save as profile", "Enregistrer le profil"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    if (custom.any { it.name == active }) {
                        OutlinedButton(
                            onClick = { fx.deleteProfile(active) },
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF6B6B).copy(alpha = 0.6f))
                        ) { Text(strings.tr("حذف", "Delete", "Supprimer"), color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Button(
                onClick = close,
                colors = ButtonDefaults.buttonColors(containerColor = ResonanceGreen, contentColor = Color.Black),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text(strings.eqSaved, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) ResonanceGreen else Color.White.copy(alpha = 0.12f))
            .border(1.dp, if (selected) Color.Transparent else Color.White.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(label, color = if (selected) Color.Black else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EffectSlider(label: String, value: Int, enabled: Boolean, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.width(90.dp))
        Slider(
            value = value.toFloat(), onValueChange = { onChange(it.toInt()) }, valueRange = 0f..100f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = ResonanceGreen, activeTrackColor = ResonanceGreen,
                inactiveTrackColor = Color.White.copy(alpha = 0.18f)
            ),
            modifier = Modifier.weight(1f)
        )
        Text("$value", color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp, modifier = Modifier.width(30.dp))
    }
}

/** A Slider turned 90 degrees (bottom = -10 dB, top = +10 dB). */
@Composable
private fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = -10f..10f,
        steps = 19,
        enabled = enabled,
        colors = SliderDefaults.colors(
            thumbColor = ResonanceGreen,
            activeTrackColor = ResonanceGreen,
            inactiveTrackColor = Color.White.copy(alpha = 0.18f)
        ),
        modifier = modifier
            .layout { measurable, constraints ->
                val p = measurable.measure(
                    androidx.compose.ui.unit.Constraints(
                        minWidth = constraints.minHeight,
                        maxWidth = constraints.maxHeight,
                        minHeight = constraints.minWidth,
                        maxHeight = constraints.maxWidth
                    )
                )
                layout(p.height, p.width) {
                    p.place(-(p.width / 2 - p.height / 2), -(p.height / 2 - p.width / 2))
                }
            }
            .graphicsLayer { rotationZ = 270f }
    )
}
