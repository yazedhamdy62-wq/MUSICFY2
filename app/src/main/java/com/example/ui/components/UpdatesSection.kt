package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppRelease
import com.example.data.AppStrings
import com.example.data.UpdateManager
import com.example.ui.theme.ResonanceGreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val release: AppRelease) : UpdateState
    data class Downloading(val progress: Float) : UpdateState
    data object NeedsPermission : UpdateState
    data object Ready : UpdateState
    data object Failed : UpdateState
}

/** Settings card: check GitHub for a newer build, download it and open the installer. */
@Composable
fun UpdatesSection(
    strings: AppStrings,
    cardBackground: Color,
    borderColor: Color,
    textColor: Color,
    subTextColor: Color
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }
    var file by remember { mutableStateOf<java.io.File?>(null) }
    val current = remember { UpdateManager.installedBuild(context) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBackground)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            strings.tr("النسخة الحالية: Build $current", "Current version: Build $current", "Version actuelle : Build $current"),
            color = textColor, fontSize = 14.5.sp, fontWeight = FontWeight.Bold
        )
        val msg = when (val s = state) {
            UpdateState.Idle -> null
            UpdateState.Checking -> strings.tr("بدور على تحديث...", "Checking...", "Recherche...")
            UpdateState.UpToDate -> strings.tr("أنت على آخر نسخة", "You have the latest version", "Vous avez la dernière version")
            is UpdateState.Available -> strings.tr(
                "فيه نسخة أحدث: ${s.release.name} (${s.release.sizeBytes / 1_000_000} MB)",
                "Newer version: ${s.release.name} (${s.release.sizeBytes / 1_000_000} MB)",
                "Nouvelle version : ${s.release.name} (${s.release.sizeBytes / 1_000_000} MB)"
            )
            is UpdateState.Downloading -> strings.tr("بنزّل... ${(s.progress * 100).toInt()}%", "Downloading... ${(s.progress * 100).toInt()}%", "Téléchargement... ${(s.progress * 100).toInt()}%")
            UpdateState.NeedsPermission -> strings.tr(
                "اسمح للتطبيق بتثبيت التحديثات من الإعدادات اللي اتفتحت، وارجع واضغط تثبيت تاني",
                "Allow installs from this app in the settings that opened, then come back and tap Install again",
                "Autorisez l'installation depuis cette appli dans les réglages, puis revenez et appuyez sur Installer"
            )
            UpdateState.Ready -> strings.tr("جاهز للتثبيت", "Ready to install", "Prêt à installer")
            UpdateState.Failed -> strings.tr("حصلت مشكلة، جرّب تاني", "Something went wrong, try again", "Un problème est survenu, réessayez")
        }
        if (msg != null) Text(msg, color = subTextColor, fontSize = 12.5.sp)
        val s = state
        if (s is UpdateState.Available && s.release.notes.isNotBlank()) {
            Text(s.release.notes.take(400), color = subTextColor, fontSize = 11.5.sp)
        }
        if (s is UpdateState.Downloading) {
            LinearProgressIndicator(progress = { s.progress }, color = ResonanceGreen, modifier = Modifier.fillMaxWidth())
        }

        val busy = s is UpdateState.Checking || s is UpdateState.Downloading
        val label = when (s) {
            is UpdateState.Available -> strings.tr("تنزيل", "Download", "Télécharger")
            UpdateState.Ready, UpdateState.NeedsPermission -> strings.tr("تثبيت", "Install", "Installer")
            else -> strings.tr("تحقق من التحديثات", "Check for updates", "Rechercher des mises à jour")
        }
        Button(
            onClick = {
                scope.launch {
                    try {
                        when (val st = state) {
                            is UpdateState.Available -> {
                                state = UpdateState.Downloading(0f)
                                val f = UpdateManager.download(context, st.release) { state = UpdateState.Downloading(it) }
                                file = f
                                state = if (UpdateManager.install(context, f)) UpdateState.Ready else UpdateState.NeedsPermission
                            }
                            UpdateState.Ready, UpdateState.NeedsPermission -> {
                                val f = file
                                state = if (f != null && UpdateManager.install(context, f)) UpdateState.Ready else UpdateState.NeedsPermission
                            }
                            else -> {
                                state = UpdateState.Checking
                                val latest = UpdateManager.fetchLatest()
                                state = if (latest != null && latest.build > current) UpdateState.Available(latest) else UpdateState.UpToDate
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        state = UpdateState.Failed
                    }
                }
            },
            enabled = !busy,
            colors = ButtonDefaults.buttonColors(containerColor = ResonanceGreen, contentColor = Color.Black),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) { Text(label, fontWeight = FontWeight.Bold) }
    }
}
