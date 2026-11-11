package com.example.ui.components

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ResonanceGreen

enum class AppNavDestination {
    HOME,
    FOLDERS,
    LIBRARY,
    SETTINGS
}

@Composable
fun FloatingBottomNavBar(
    selectedDestination: AppNavDestination,
    onDestinationSelected: (AppNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    isGlassEffect: Boolean = true
) {
    val items = listOf(
        AppNavDestination.HOME,
        AppNavDestination.FOLDERS,
        AppNavDestination.LIBRARY,
        AppNavDestination.SETTINGS
    )

    val selectedIndex = when (selectedDestination) {
        AppNavDestination.HOME -> 0
        AppNavDestination.FOLDERS -> 1
        AppNavDestination.LIBRARY -> 2
        AppNavDestination.SETTINGS -> 3
    }

    val barHeight = 56.dp
    val isDark = MaterialTheme.colorScheme.surface == DarkSurface || MaterialTheme.colorScheme.background == DarkBackground
    
    // High contrast icons for maximum legibility on both glass and solid backgrounds
    val unselectedColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer capsule container taking 90% of screen width
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .height(barHeight)
                .shadow(
                    elevation = if (isDark) 24.dp else 12.dp,
                    shape = RoundedCornerShape(percent = 50),
                    ambientColor = if (isDark) Color.Black.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.15f),
                    spotColor = if (isGlassEffect && isDark) ResonanceGreen.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.2f)
                )
                .clip(RoundedCornerShape(percent = 50))
                .testTag("floating_bottom_nav_bar")
        ) {
            // Liquid glass background layer (ultra-smooth 0ms GPU cost)
            val glassBackgroundBrush = remember(isGlassEffect, isDark) {
                if (isGlassEffect) {
                    if (isDark) {
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color(0xF218222C), Color(0xEA101620))
                        )
                    } else {
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color(0xF6FFFFFF), Color(0xEEF1F5F9))
                        )
                    }
                } else null
            }
            val glassBorderBrush = remember(isGlassEffect, isDark) {
                if (isGlassEffect) {
                    if (isDark) {
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color(0x60FFFFFF), Color(0x3000E676), Color(0x15FFFFFF))
                        )
                    } else {
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            listOf(Color(0xE0FFFFFF), Color(0x6094A3B8))
                        )
                    }
                } else null
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isGlassEffect && glassBackgroundBrush != null && glassBorderBrush != null) {
                            Modifier
                                .background(brush = glassBackgroundBrush)
                                .border(
                                    BorderStroke(1.2.dp, glassBorderBrush),
                                    RoundedCornerShape(percent = 50)
                                )
                        } else {
                            Modifier
                                .background(if (isDark) Color(0xFF15191E) else Color(0xFFFFFFFF))
                                .border(1.2.dp, if (isDark) Color(0xFF2B3540) else Color(0xFFE2E8F0), RoundedCornerShape(percent = 50))
                        }
                    )
            )
            val totalWidth = maxWidth
            val horizontalPadding = 6.dp
            val availableWidth = totalWidth - (horizontalPadding * 2)
            val tabWidth = availableWidth / items.size
            val pillPadding = 4.dp
            val pillWidth = tabWidth - (pillPadding * 2)
            val pillHeight = 46.dp

            val density = LocalDensity.current
            val layoutDirection = LocalLayoutDirection.current
            val isRtl = layoutDirection == LayoutDirection.Rtl
            var dragAccumulator by remember { mutableFloatStateOf(0f) }
            val tabWidthPx = with(density) { tabWidth.toPx() }

            // In RTL, Row places Item 0 (Home) on the right and Item 3 (Settings) on the left.
            // When wrapping the indicator pill in LayoutDirection.Ltr, Alignment.CenterStart is
            // fixed to the visual left edge, so the base offset from the left edge is computed accordingly.
            val basePillOffset = if (isRtl) {
                horizontalPadding + (tabWidth * (items.size - 1 - selectedIndex)) + pillPadding
            } else {
                horizontalPadding + (tabWidth * selectedIndex) + pillPadding
            }
            val dragOffsetDp = with(density) { dragAccumulator.toDp() }
            val currentOffsetDp = (basePillOffset + dragOffsetDp).coerceIn(
                horizontalPadding + pillPadding,
                horizontalPadding + (tabWidth * (items.size - 1)) + pillPadding
            )

            // Ultra-smooth physics-based spring animation for the sliding indicator pill
            val animatedOffset by animateDpAsState(
                targetValue = if (dragAccumulator != 0f) currentOffsetDp else basePillOffset,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = if (dragAccumulator != 0f) Spring.StiffnessHigh else Spring.StiffnessMediumLow
                ),
                label = "nav_pill_sliding_offset"
            )

            val animatedPillWidth by animateDpAsState(
                targetValue = if (dragAccumulator != 0f) pillWidth * 1.08f else pillWidth,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "nav_pill_width"
            )

            // Sliding Active Capsule Pill Indicator (Smooth gliding between items)
            // Evaluated in LTR coordinate space so offset is strictly distance from the left edge
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Box(
                    modifier = Modifier
                        .offset(x = animatedOffset)
                        .align(Alignment.CenterStart)
                        .size(width = animatedPillWidth, height = pillHeight)
                        .clip(RoundedCornerShape(percent = 50))
                        .then(
                            if (isGlassEffect) {
                                if (isDark) {
                                    Modifier
                                        .background(
                                            androidx.compose.ui.graphics.Brush.linearGradient(
                                                listOf(Color(0x4400E676), Color(0x2200E676))
                                            )
                                        )
                                        .border(BorderStroke(1.dp, Color(0x5500E676)), RoundedCornerShape(percent = 50))
                                } else {
                                    Modifier
                                        .background(
                                            androidx.compose.ui.graphics.Brush.linearGradient(
                                                listOf(Color(0x3300E676), Color(0x1A00E676))
                                            )
                                        )
                                        .border(BorderStroke(1.dp, Color(0x4400E676)), RoundedCornerShape(percent = 50))
                                }
                            } else {
                                Modifier.background(if (isDark) Color(0xFF2E363D) else Color(0xFFE2E8F0))
                            }
                        )
                )
            }

            // 4 Interactive items row with accurate multi-tab horizontal drag and swipe support
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = horizontalPadding)
                    .pointerInput(selectedDestination, layoutDirection) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                dragAccumulator = 0f
                            },
                            onDragEnd = {
                                val threshold = tabWidthPx * 0.25f
                                val totalDrag = dragAccumulator
                                val targetIndex = if (isRtl) {
                                    // In RTL: dragging left (negative accumulator) moves towards higher index
                                    // dragging right (positive accumulator) moves towards lower index
                                    val deltaTabs = (-totalDrag / tabWidthPx)
                                    val roundedDelta = kotlin.math.round(deltaTabs).toInt()
                                    if (roundedDelta == 0 && kotlin.math.abs(totalDrag) >= threshold) {
                                        if (totalDrag < 0) (selectedIndex + 1).coerceIn(0, items.size - 1)
                                        else (selectedIndex - 1).coerceIn(0, items.size - 1)
                                    } else {
                                        (selectedIndex + roundedDelta).coerceIn(0, items.size - 1)
                                    }
                                } else {
                                    // In LTR: dragging right (positive accumulator) moves towards higher index
                                    // dragging left (negative accumulator) moves towards lower index
                                    val deltaTabs = (totalDrag / tabWidthPx)
                                    val roundedDelta = kotlin.math.round(deltaTabs).toInt()
                                    if (roundedDelta == 0 && kotlin.math.abs(totalDrag) >= threshold) {
                                        if (totalDrag > 0) (selectedIndex + 1).coerceIn(0, items.size - 1)
                                        else (selectedIndex - 1).coerceIn(0, items.size - 1)
                                    } else {
                                        (selectedIndex + roundedDelta).coerceIn(0, items.size - 1)
                                    }
                                }
                                onDestinationSelected(items[targetIndex])
                                dragAccumulator = 0f
                            },
                            onDragCancel = {
                                dragAccumulator = 0f
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                dragAccumulator += dragAmount
                            }
                        )
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, destination ->
                    val isSelected = destination == selectedDestination
                    val interactionSource = remember { MutableInteractionSource() }

                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.08f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        ),
                        label = "nav_icon_scale"
                    )

                    val iconColor by animateColorAsState(
                        targetValue = if (isSelected) ResonanceGreen else unselectedColor,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label = "nav_icon_color"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onDestinationSelected(destination) }
                            )
                            .testTag("nav_item_${destination.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.scale(iconScale)) {
                            when (destination) {
                                AppNavDestination.HOME -> {
                                    // Solid Home icon exactly as in screenshot
                                    Icon(
                                        imageVector = Icons.Filled.Home,
                                        contentDescription = "Home",
                                        tint = iconColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                AppNavDestination.FOLDERS -> {
                                    // Custom Outlined Folder icon with tab and inner crease
                                    FolderNavIcon(
                                        tint = iconColor,
                                        modifier = Modifier.size(23.dp)
                                    )
                                }
                                AppNavDestination.LIBRARY -> {
                                    // Outlined beamed double-music-note matching screenshot
                                    BeamedMusicNotesIcon(
                                        tint = iconColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                AppNavDestination.SETTINGS -> {
                                    // Outlined gear cog matching screenshot
                                    Icon(
                                        imageVector = Icons.Outlined.Settings,
                                        contentDescription = "Settings",
                                        tint = iconColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom Outlined Beamed Music Notes icon (♫) exactly matching the user's screenshot.
 * Features two note heads with vertical stems connected by a double angled beam.
 */
@Composable
fun BeamedMusicNotesIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = 2.0.dp.toPx()

        val leftNoteCenter = Offset(w * 0.28f, h * 0.74f)
        val rightNoteCenter = Offset(w * 0.76f, h * 0.62f)
        val noteRadius = w * 0.17f

        // Draw left note head outline
        drawCircle(
            color = tint,
            radius = noteRadius,
            center = leftNoteCenter,
            style = Stroke(width = strokeWidth)
        )

        // Draw right note head outline
        drawCircle(
            color = tint,
            radius = noteRadius,
            center = rightNoteCenter,
            style = Stroke(width = strokeWidth)
        )

        // Stems and Beams Path
        val path = Path().apply {
            // Left stem from right edge of left note head up to beam
            val leftStemX = leftNoteCenter.x + noteRadius - strokeWidth / 2
            moveTo(leftStemX, leftNoteCenter.y)
            lineTo(leftStemX, h * 0.22f)

            // Right stem from right edge of right note head up to beam
            val rightStemX = rightNoteCenter.x + noteRadius - strokeWidth / 2
            moveTo(rightStemX, rightNoteCenter.y)
            lineTo(rightStemX, h * 0.14f)

            // Top primary connecting beam
            moveTo(leftStemX, h * 0.22f)
            lineTo(rightStemX, h * 0.14f)

            // Secondary angled beam line underneath
            moveTo(leftStemX, h * 0.36f)
            lineTo(rightStemX, h * 0.28f)
        }

        drawPath(
            path = path,
            color = tint,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}

/**
 * Custom Folder icon matching the style, weight, and stroke width of the navigation icons.
 */
@Composable
fun FolderNavIcon(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeWidth = 2.0.dp.toPx()

        val tabWidth = w * 0.40f
        val tabHeight = h * 0.16f
        val bodyTop = h * 0.34f
        val bodyBottom = h * 0.82f
        val left = w * 0.12f
        val right = w * 0.88f
        val corner = 3.dp.toPx()

        val folderPath = Path().apply {
            // Tab on top-left
            moveTo(left + corner, bodyTop - tabHeight)
            lineTo(left + tabWidth - corner, bodyTop - tabHeight)
            quadraticTo(left + tabWidth, bodyTop - tabHeight, left + tabWidth + 2.dp.toPx(), bodyTop - tabHeight + 2.dp.toPx())
            lineTo(left + tabWidth + corner + 3.dp.toPx(), bodyTop)
            // Top of main folder body
            lineTo(right - corner, bodyTop)
            quadraticTo(right, bodyTop, right, bodyTop + corner)
            // Right edge down
            lineTo(right, bodyBottom - corner)
            quadraticTo(right, bodyBottom, right - corner, bodyBottom)
            // Bottom edge
            lineTo(left + corner, bodyBottom)
            quadraticTo(left, bodyBottom, left, bodyBottom - corner)
            // Left edge up
            lineTo(left, bodyTop - tabHeight + corner)
            quadraticTo(left, bodyTop - tabHeight, left + corner, bodyTop - tabHeight)
            close()
        }

        drawPath(
            path = folderPath,
            color = tint,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )

        // Subtle interior divider line
        val flapPath = Path().apply {
            moveTo(left + 2.5.dp.toPx(), bodyTop)
            lineTo(right - 2.5.dp.toPx(), bodyTop)
        }
        drawPath(
            path = flapPath,
            color = tint.copy(alpha = 0.55f),
            style = Stroke(
                width = 1.3.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}

