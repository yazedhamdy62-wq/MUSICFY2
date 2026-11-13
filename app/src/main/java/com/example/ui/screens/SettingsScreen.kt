package com.example.ui.screens

import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.NightlightRound
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SettingsBrightness
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppLanguage
import com.example.data.AppStrings
import com.example.data.AppThemeMode
import com.example.data.LocalizationManager
import com.example.data.PlayerSettings
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ResonanceGreen

@Composable
fun SettingsScreen(
    settings: PlayerSettings,
    onUpdateSettings: (PlayerSettings) -> Unit,
    onBack: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onScanFiles: () -> Unit,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (settings.themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemDark
    }

    val strings: AppStrings = remember(settings.language) { LocalizationManager.getStrings(settings.language) }

    val bgColor = MaterialTheme.colorScheme.background
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBackground = if (isDark) DarkSurfaceElevated else Color.White
    val innerCardBg = if (isDark) Color(0xFF141A20) else Color(0xFFF1F5F9)
    val borderColor = if (isDark) Color(0xFF26333D) else Color(0xFFE2E8F0)
    val dividerColor = if (isDark) Color(0xFF232D36) else Color(0xFFE2E8F0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
    ) {
        // Top Bar: Matching Home, Library, and Folders screen typography
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.settingsTitle,
                color = textColor,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f)
            )

            // Rescan library button
            IconButton(
                onClick = onScanFiles,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("settings_top_scan_btn")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = strings.scanFiles,
                    tint = subTextColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 220.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Scan Files Card (stacked layout so long texts in any language never squeeze the button)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(cardBackground)
                        .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(18.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = strings.scanDeviceAudio,
                        color = textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = strings.refreshScanDesc,
                        color = subTextColor,
                        fontSize = 11.5.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onScanFiles,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ResonanceGreen,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("settings_scan_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.scanFiles,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // SECTION 1: THEME & APPEARANCE (المظهر والسمة)
            item {
                SettingsSectionHeader(title = strings.themeSection, color = ResonanceGreen)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(cardBackground)
                        .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 3 Theme Choices: Dark, Light, System (Now fully functional!)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemeSelectionItem(
                            title = strings.themeDark,
                            icon = Icons.Outlined.NightlightRound,
                            isSelected = settings.themeMode == AppThemeMode.DARK,
                            isDark = isDark,
                            textColor = textColor,
                            subTextColor = subTextColor,
                            innerBg = innerCardBg,
                            borderColor = borderColor,
                            onClick = { onUpdateSettings(settings.copy(themeMode = AppThemeMode.DARK)) },
                            modifier = Modifier.weight(1f)
                        )

                        ThemeSelectionItem(
                            title = strings.themeLight,
                            icon = Icons.Outlined.WbSunny,
                            isSelected = settings.themeMode == AppThemeMode.LIGHT,
                            isDark = isDark,
                            textColor = textColor,
                            subTextColor = subTextColor,
                            innerBg = innerCardBg,
                            borderColor = borderColor,
                            onClick = { onUpdateSettings(settings.copy(themeMode = AppThemeMode.LIGHT)) },
                            modifier = Modifier.weight(1f)
                        )

                        ThemeSelectionItem(
                            title = strings.themeSystem,
                            icon = Icons.Outlined.SettingsBrightness,
                            isSelected = settings.themeMode == AppThemeMode.SYSTEM,
                            isDark = isDark,
                            textColor = textColor,
                            subTextColor = subTextColor,
                            innerBg = innerCardBg,
                            borderColor = borderColor,
                            onClick = { onUpdateSettings(settings.copy(themeMode = AppThemeMode.SYSTEM)) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // System mode status indicator when active
                    if (settings.themeMode == AppThemeMode.SYSTEM) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(ResonanceGreen.copy(alpha = 0.12f))
                                .border(BorderStroke(1.dp, ResonanceGreen.copy(alpha = 0.3f)), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.BrightnessAuto,
                                    contentDescription = null,
                                    tint = ResonanceGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = strings.autoThemeActive(isSystemDark),
                                    color = ResonanceGreen,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = dividerColor)

                    // Glass Effect switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.glassEffectTitle,
                                color = textColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.glassEffectDesc,
                                color = subTextColor,
                                fontSize = 11.5.sp
                            )
                        }

                        Switch(
                            checked = settings.glassEffect,
                            onCheckedChange = { onUpdateSettings(settings.copy(glassEffect = it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ResonanceGreen,
                                uncheckedThumbColor = subTextColor,
                                uncheckedTrackColor = borderColor
                            )
                        )
                    }
                }
            }

            // SECTION 2: PLAYBACK & SOUND (تشغيل الصوت والمعادل)
            item {
                SettingsSectionHeader(title = strings.playbackSection, color = ResonanceGreen)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(cardBackground)
                        .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Equalizer Link Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenEqualizer)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ResonanceGreen.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Equalizer,
                                    contentDescription = null,
                                    tint = ResonanceGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = strings.eqTitle,
                                    color = textColor,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = strings.eqSubtitle,
                                    color = subTextColor,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        Icon(
                            imageVector = if (settings.language == AppLanguage.ARABIC) Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = subTextColor
                        )
                    }

                    HorizontalDivider(color = dividerColor)

                    // Sleep Timer Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onOpenSleepTimer)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0x2238BDF8)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Timer,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = strings.sleepTimerTitle,
                                    color = textColor,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (settings.sleepTimerMinutes != null)
                                        strings.sleepTimerActive.format(settings.sleepTimerMinutes)
                                    else strings.sleepTimerOff,
                                    color = if (settings.sleepTimerMinutes != null) ResonanceGreen else subTextColor,
                                    fontSize = 11.5.sp
                                )
                            }
                        }

                        Icon(
                            imageVector = if (settings.language == AppLanguage.ARABIC) Icons.AutoMirrored.Filled.KeyboardArrowLeft else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = subTextColor
                        )
                    }

                    HorizontalDivider(color = dividerColor)

                    // Gapless Playback Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.gaplessTitle,
                                color = textColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = strings.gaplessDesc,
                                color = subTextColor,
                                fontSize = 11.5.sp
                            )
                        }

                        Switch(
                            checked = settings.gaplessPlayback,
                            onCheckedChange = { onUpdateSettings(settings.copy(gaplessPlayback = it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = ResonanceGreen,
                                uncheckedThumbColor = subTextColor,
                                uncheckedTrackColor = borderColor
                            )
                        )
                    }
                }
            }

            // SECTION 3: LANGUAGE (اللغة)
            item {
                SettingsSectionHeader(title = strings.languageSection, color = ResonanceGreen)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(cardBackground)
                        .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(18.dp))
                        .padding(vertical = 6.dp)
                ) {
                    LanguageRowItem(
                        nativeName = strings.langArabic,
                        subName = strings.langArabicSub,
                        code = "ض",
                        isSelected = settings.language == AppLanguage.ARABIC,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        onClick = { onUpdateSettings(settings.copy(language = AppLanguage.ARABIC)) }
                    )

                    HorizontalDivider(color = dividerColor, modifier = Modifier.padding(horizontal = 14.dp))

                    LanguageRowItem(
                        nativeName = strings.langEnglish,
                        subName = strings.langEnglishSub,
                        code = "EN",
                        isSelected = settings.language == AppLanguage.ENGLISH,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        onClick = { onUpdateSettings(settings.copy(language = AppLanguage.ENGLISH)) }
                    )

                    HorizontalDivider(color = dividerColor, modifier = Modifier.padding(horizontal = 14.dp))

                    LanguageRowItem(
                        nativeName = strings.langFrench,
                        subName = strings.langFrenchSub,
                        code = "FR",
                        isSelected = settings.language == AppLanguage.FRENCH,
                        textColor = textColor,
                        subTextColor = subTextColor,
                        onClick = { onUpdateSettings(settings.copy(language = AppLanguage.FRENCH)) }
                    )
                }
            }

            // SECTION 4: STORAGE & CACHE (التخزين والبيانات)
            item {
                SettingsSectionHeader(title = strings.storageSection, color = ResonanceGreen)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(cardBackground)
                        .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.cacheSize,
                                color = textColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${"%.1f".format(settings.cacheSizeMb)} MB",
                                color = subTextColor,
                                fontSize = 12.sp
                            )
                        }

                        Button(
                            onClick = onClearCache,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF4C1D24) else Color(0xFFFEE2E2),
                                contentColor = Color(0xFFDC2626)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(strings.clearBtn, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // SECTION 5: ABOUT MUSICFY
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Musicfy • ${strings.aboutSubtitle}",
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 3.dp, height = 12.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun ThemeSelectionItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    isDark: Boolean,
    textColor: Color,
    subTextColor: Color,
    innerBg: Color,
    borderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeBorderColor by animateColorAsState(
        targetValue = if (isSelected) ResonanceGreen else borderColor,
        label = "theme_border"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(innerBg)
            .border(1.5.dp, activeBorderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = if (isSelected) ResonanceGreen else subTextColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title,
            color = if (isSelected) (if (isDark) Color.White else ResonanceGreen) else subTextColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )

        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(ResonanceGreen)
            )
        }
    }
}

@Composable
private fun LanguageRowItem(
    nativeName: String,
    subName: String,
    code: String,
    isSelected: Boolean,
    textColor: Color,
    subTextColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (isSelected) ResonanceGreen.copy(alpha = 0.18f) else Color(0x3364748B)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = code,
                color = if (isSelected) ResonanceGreen else subTextColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nativeName,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = subName,
                color = subTextColor,
                fontSize = 11.5.sp
            )
        }

        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = ResonanceGreen,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(1.5.dp, subTextColor.copy(alpha = 0.4f), CircleShape)
            )
        }
    }
}
