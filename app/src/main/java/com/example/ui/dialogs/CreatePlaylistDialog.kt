package com.example.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppStrings
import com.example.ui.components.GlassOverlay
import com.example.ui.theme.ResonanceGreen

/** "Create New Playlist" as a liquid-glass panel. */
@Composable
fun CreatePlaylistDialog(
    strings: AppStrings,
    name: String,
    onNameChange: (String) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    GlassOverlay(onDismiss = onDismiss, bottomSheet = false) { close ->
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Text(strings.createPlaylistTitle, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(6.dp))
            Text(strings.playlistNameLabel, color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ResonanceGreen,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.28f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = ResonanceGreen
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = close,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                    modifier = Modifier.weight(1f)
                ) { Text(strings.cancelBtn, color = Color.White) }
                Button(
                    onClick = {
                        onCreate(name.trim().ifEmpty { strings.newPlaylistFallback })
                        close()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ResonanceGreen, contentColor = Color.Black),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.weight(1f)
                ) { Text(strings.saveBtn, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
