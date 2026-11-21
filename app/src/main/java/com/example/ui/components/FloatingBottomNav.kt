package com.example.ui.components

import kotlin.math.abs
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.Brush
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
    val barShape = RoundedCornerShape(percent = 50)
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
        // Outer capsule container taking 90% of screen width.
        // Not clipped in glass mode so the "lens" can grow taller than the bar while it moves.
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .height(barHeight)
                .then(
                    if (isGlassEffect) {
                        Modifier
                    } else {
                        Modifier
                            .shadow(
                                elevation = if (isDark) 24.dp else 12.dp,
                                shape = barShape,
                                ambientColor = if (isDark) Color.Black.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.15f),
                                spotColor = Color.Black.copy(alpha = 0.2f)
                            )
                            .clip(barShape)
                    }
                )
                .testTag("floating_bottom_nav_bar")
        ) {
            // Transparent "liquid glass" bar: clear tinted glass with a thin light rim
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(barShape)
                    .then(
                        if (isGlassEffect) {
                            Modifier
                                .background(
                                    brush = Brush.verticalGradient(
                                        if (isDark) listOf(Color(0x6B151B24), Color(0x5C0F141B))
                                        else listOf(Color(0x9EFFFFFF), Color(0x73F1F5F9))
                                    )
                                )
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        Brush.verticalGradient(
                                            if (isDark) listOf(Color(0x59FFFFFF), Color(0x14FFFFFF))
                                            else listOf(Color(0xE6FFFFFF), Color(0x4094A3B8))
                                        )
                                    ),
                                    barShape
                                )
                        } else {
                            Modifier
                                .background(if (isDark) Color(0xFF15191E) else Color(0xFFFFFFFF))
                                .border(1.2.dp, if (isDark) Color(0xFF2B3540) else Color(0xFFE2E8F0), barShape)
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

            // Physics-based spring animation for the sliding lens (follows the finger while dragging)
            val animatedOffset by animateDpAsState(
                targetValue = if (dragAccumulator != 0f) currentOffsetDp else basePillOffset,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = if (dragAccumulator != 0f) Spring.StiffnessHigh else Spring.StiffnessMediumLow
                ),
                label = "nav_pill_sliding_offset"
            )

            // "Lift": while the lens travels (or is dragged) it grows into a glass bubble that is
            // taller than the bar and magnifies what is under it; at rest it settles back into a flat capsule.
            val remainingDp = abs((animatedOffset - basePillOffset).value)
            val isMoving = isGlassEffect && (dragAccumulator != 0f || remainingDp > 0.75f)
            val lift by animateFloatAsState(
                targetValue = if (isMoving) 1f else 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "nav_lens_lift"
            )
            // Stretches a little while it is far from its destination (gooey feel)
            val stretch = if (isGlassEffect) {
                (remainingDp / tabWidth.value).coerceIn(0f, 1.5f) / 1.5f
            } else 0f

            val lensWidth = if (isGlassEffect) {
                pillWidth * (1f + 0.30f * lift + 0.16f * stretch)
            } else {
                pillWidth
            }
            val lensHeight = if (isGlassEffect) pillHeight + 20.dp * lift else pillHeight

            // Centre of the lens in LTR coordinates (distance from the bar's left edge)
            val lensCenter = animatedOffset + pillWidth / 2

            // Sliding lens / active capsule (evaluated in LTR space: offset is the distance from the left edge)
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                val lensShape = RoundedCornerShape(percent = 50)
                val lensFill: Brush
                val lensBorder: Brush
                if (isGlassEffect) {
                    val restFill = if (isDark) Color.White.copy(alpha = 0.13f) else Color.Black.copy(alpha = 0.07f)
                    val liftTop = if (isDark) Color.White.copy(alpha = 0.30f) else Color.White.copy(alpha = 0.78f)
                    val liftBottom = if (isDark) Color.White.copy(alpha = 0.09f) else Color.White.copy(alpha = 0.40f)
                    lensFill = Brush.verticalGradient(
                        listOf(lerpColor(restFill, liftTop, lift), lerpColor(restFill, liftBottom, lift))
                    )
                    val restBorder = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f)
                    val borderTop = if (isDark) Color.White.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.95f)
                    val borderBottom = if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.12f)
                    lensBorder = Brush.verticalGradient(
                        listOf(lerpColor(restBorder, borderTop, lift), lerpColor(restBorder, borderBottom, lift))
                    )
                } else {
                    val solid = if (isDark) Color(0xFF2E363D) else Color(0xFFE2E8F0)
                    lensFill = Brush.verticalGradient(listOf(solid, solid))
                    lensBorder = Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                }

                Box(
                    modifier = Modifier
                        .offset(x = animatedOffset - (lensWidth - pillWidth) / 2)
                        .align(Alignment.CenterStart)
                        .size(width = lensWidth, height = lensHeight)
                        .then(
                            if (isGlassEffect && lift > 0.02f) {
                                Modifier.shadow(
                                    elevation = 10.dp * lift,
                                    shape = lensShape,
                                    ambientColor = Color.Black.copy(alpha = 0.30f * lift),
                                    spotColor = ResonanceGreen.copy(alpha = 0.35f * lift)
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clip(lensShape)
                        .background(lensFill)
                        .then(
                            if (isGlassEffect) {
                                Modifier.border(BorderStroke(1.2.dp, lensBorder), lensShape)
                            } else {
                                Modifier
                            }
                        )
                )
            }

            // Interactive items row with accurate multi-tab horizontal drag and swipe support
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
                    val interactionSource = remember { MutableInteractionSource() }

                    // How much the lens covers this tab (1 = exactly under it, 0 = not at all).
                    // Icons under the lens turn green and get magnified as it passes over them.
                    val tabVisualIndex = if (isRtl) items.size - 1 - index else index
                    val tabCenter = horizontalPadding + tabWidth * tabVisualIndex + tabWidth / 2
                    val distance = abs((lensCenter - tabCenter).value) / tabWidth.value
                    val proximity = (1f - distance).coerceIn(0f, 1f)

                    val iconColor = lerpColor(unselectedColor, ResonanceGreen, proximity)
                    val iconScale = 1f + 0.08f * proximity + 0.22f * lift * proximity

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
                                    Icon(
                                        imageVector = Icons.Filled.Home,
                                        contentDescription = "Home",
                                        tint = iconColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                AppNavDestination.FOLDERS -> {
                                    FolderNavIcon(
                                        tint = iconColor,
                                        modifier = Modifier.size(23.dp)
                                    )
                                }
                                AppNavDestination.LIBRARY -> {
                                    BeamedMusicNotesIcon(
                                        tint = iconColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                AppNavDestination.SETTINGS -> {
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

