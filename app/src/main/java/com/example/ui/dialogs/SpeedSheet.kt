package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppStrings
import com.example.ui.components.GlassOverlay
import com.example.ui.theme.ResonanceGreen

/** Playback speed picker as a liquid-glass sheet. */
@Composable
fun SpeedSheet(
    strings: AppStrings,
    speed: Float,
    onSelect: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
    GlassOverlay(onDismiss = onDismiss, bottomSheet = true) { close ->
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(strings.tr("سرعة التشغيل", "Playback speed", "Vitesse de lecture"), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("%.2fx".format(speed).replace(".00x", "x").replace(Regex("(\\.\\d)0x"), "$1x"), color = ResonanceGreen, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Slider(
                value = speed, onValueChange = { onSelect((Math.round(it * 20f) / 20f)) },
                valueRange = 0.5f..2f,
                colors = SliderDefaults.colors(thumbColor = ResonanceGreen, activeTrackColor = ResonanceGreen, inactiveTrackColor = Color.White.copy(alpha = 0.18f))
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                options.forEach { o ->
                    val sel = o == speed
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                            .background(if (sel) ResonanceGreen else Color.White.copy(alpha = 0.12f))
                            .border(1.dp, if (sel) Color.Transparent else Color.White.copy(alpha = 0.22f), RoundedCornerShape(14.dp))
                            .clickable { onSelect(o) }.padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (o == 1f) "1x" else o.toString().trimEnd('0') + "x", color = if (sel) Color.Black else Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(strings.tr("يتحفظ ويتطبق على كل الأغاني", "Saved and applied to every song", "Enregistré pour tous les titres"), color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }
    }
}
