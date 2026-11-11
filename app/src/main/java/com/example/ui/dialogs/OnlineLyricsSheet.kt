package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppStrings
import com.example.data.LrclibClient
import com.example.data.OnlineLyricsResult
import com.example.ui.theme.ResonanceGreen
import kotlinx.coroutines.CancellationException

/**
 * Bottom sheet that searches lyrics online (LRCLIB) for the current song and lets the user
 * pick one of the results. [onSelect] receives the lyrics text (LRC if synced, else plain).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineLyricsSheet(
    title: String,
    artist: String,
    durationSec: Int,
    isDark: Boolean,
    strings: AppStrings,
    onSelect: (String) -> Unit,
    onOpenBrowser: () -> Unit,
    onDismiss: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<OnlineLyricsResult>>(emptyList()) }
    var selectedId by remember { mutableStateOf<Long?>(null) }
    var reloadKey by remember { mutableStateOf(0) }

    LaunchedEffect(title, artist, reloadKey) {
        isLoading = true
        hasError = false
        try {
            val found = LrclibClient.search(title, artist, durationSec)
            results = found
            selectedId = found.firstOrNull()?.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            results = emptyList()
            selectedId = null
            hasError = true
        }
        isLoading = false
    }

    val sheetBg = if (isDark) Color(0xFF14191F) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val selectedResult = results.firstOrNull { it.id == selectedId }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = sheetBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = strings.onlineLyricsTitle,
                color = textColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$title • $artist",
                color = subColor,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 220.dp, max = 420.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = ResonanceGreen)
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = strings.onlineLyricsSearching,
                            color = subColor,
                            fontSize = 14.sp
                        )
                    }
                } else if (hasError || results.isEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (hasError) strings.onlineLyricsError else strings.onlineLyricsNoResults,
                            color = subColor,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        if (hasError) {
                            TextButton(onClick = { reloadKey += 1 }) {
                                Text(
                                    text = strings.onlineLyricsRetry,
                                    color = ResonanceGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        TextButton(onClick = onOpenBrowser) {
                            Text(
                                text = strings.searchWebLyrics,
                                color = ResonanceGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(results, key = { it.id }) { result ->
                            LyricsResultRow(
                                result = result,
                                selected = result.id == selectedId,
                                strings = strings,
                                textColor = textColor,
                                subColor = subColor,
                                onClick = { selectedId = result.id }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(text = strings.cancelBtn, color = subColor, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = {
                        val text = selectedResult?.bestText
                        if (text != null) onSelect(text)
                    },
                    enabled = selectedResult != null,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ResonanceGreen,
                        contentColor = Color.Black
                    )
                ) {
                    Text(text = strings.onlineLyricsSelect, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun LyricsResultRow(
    result: OnlineLyricsResult,
    selected: Boolean,
    strings: AppStrings,
    textColor: Color,
    subColor: Color,
    onClick: () -> Unit
) {
    val minutes = result.durationSec / 60
    val seconds = result.durationSec % 60
    val durationText = "%d:%02d".format(minutes, seconds)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) ResonanceGreen.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = ResonanceGreen,
                unselectedColor = subColor
            )
        )
        Column(modifier = Modifier.weight(1f).padding(top = 10.dp, end = 6.dp)) {
            Text(
                text = result.trackName,
                color = textColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${result.artistName} • $durationText",
                color = subColor,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (result.hasSynced) ResonanceGreen.copy(alpha = 0.18f)
                        else subColor.copy(alpha = 0.18f)
                    )
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (result.hasSynced) strings.onlineLyricsSynced else strings.onlineLyricsPlain,
                    color = if (result.hasSynced) ResonanceGreen else subColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            val preview = result.previewText
            if (preview.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = preview,
                    color = textColor.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
