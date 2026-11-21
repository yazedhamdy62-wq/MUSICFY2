package com.example.ui.components

import kotlin.math.abs
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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

    // Liquid glass looks through the screen content provided by MainActivity
    val screenBackdrop = LocalGlassBackdrop.current
    val barBackdrop = rememberGlassBackdrop()      // the bar's own glass (seen through by the lens)
    val accentBackdrop = rememberGlassBackdrop()   // invisible green copy of the icons (revealed by the lens)
    val liquid = isGlassEffect && screenBackdrop != null

    // Dark smoky glass (charcoal tint) with near-white icons, like iOS 26 / the reference app
    val unselectedColor = if (isDark) Color(0xFFF2F2F2) else Color(0xFF0B1220)
    val barTint = if (isDark) Color(0xB3262626) else Color(0x8CFFFFFF)
    val barRim = Brush.linearGradient(
        if (isDark) listOf(Color(0x66FFFFFF), Color(0x1AFFFFFF), Color(0x33FFFFFF))
        else listOf(Color(0xE6FFFFFF), Color(0x3394A3B8), Color(0x99FFFFFF))
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer capsule container taking 90% of screen width (not clipped: the lens can grow taller than the bar)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .height(barHeight)
                .testTag("floating_bottom_nav_bar")
        ) {
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
            // The lens is evaluated in LTR space (offset = distance from the bar's left edge).
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

            // "Lift": while the lens travels (or is dragged) it grows into a glass bubble that bends
            // the light at its edges; at rest it settles back into a flat capsule.
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

            // Icons magnify a little while the lens is over them
            val iconScales = items.indices.map { index ->
                val tabVisualIndex = if (isRtl) items.size - 1 - index else index
                val tabCenter = horizontalPadding + tabWidth * tabVisualIndex + tabWidth / 2
                val distance = abs((lensCenter - tabCenter).value) / tabWidth.value
                val proximity = (1f - distance).coerceIn(0f, 1f)
                1f + 0.08f * proximity + 0.22f * lift * proximity
            }

            // (A) Bar surface. Recorded into barBackdrop so the lens can bend it.
            Box(modifier = Modifier.matchParentSize().glassSource(barBackdrop)) {
                if (liquid && screenBackdrop != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .glassSurface(
                                worlds = listOf(screenBackdrop),
                                shape = barShape,
                                blur = 4.dp,
                                refractionHeight = 14.dp,
                                refractionAmount = 20.dp,
                                onDrawSurface = {
                                    drawRect(barTint)
                                    drawGlassRim(size.minDimension / 2f, barRim, 1.dp.toPx())
                                }
                            )
                    )
                } else if (isGlassEffect) {
                    // Glass without a backdrop (preview / very old devices): plain tinted capsule
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(barShape)
                            .background(barTint)
                            .border(1.dp, barRim, barShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(
                                elevation = if (isDark) 24.dp else 12.dp,
                                shape = barShape,
                                ambientColor = if (isDark) Color.Black.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.15f),
                                spotColor = Color.Black.copy(alpha = 0.2f)
                            )
                            .clip(barShape)
                            .background(if (isDark) Color(0xFF15191E) else Color(0xFFFFFFFF))
                            .border(1.2.dp, if (isDark) Color(0xFF2B3540) else Color(0xFFE2E8F0), barShape)
                    )
                }
            }

            // (B) Visible icons + touch / swipe handling
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
                        Box(modifier = Modifier.scale(iconScales[index])) {
                            NavIcon(destination = destination, tint = unselectedColor, describe = true)
                        }
                    }
                }
            }

            // (C) Invisible copy of the icons in green. It is only recorded (never shown) and the lens
            // reveals it, so exactly the part of an icon under the lens turns green.
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .glassSource(accentBackdrop, showOnScreen = false)
                    .padding(horizontal = horizontalPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, destination ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(modifier = Modifier.scale(iconScales[index])) {
                            NavIcon(destination = destination, tint = ResonanceGreen, describe = false)
                        }
                    }
                }
            }

            // (D) The sliding glass lens (always on top). It shows the world behind it
            // (screen + bar + green icons) and bends it near its edges while it moves.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                val lensShape = RoundedCornerShape(percent = 50)
                val lensWorlds = remember(liquid, screenBackdrop, barBackdrop, accentBackdrop) {
                    if (liquid && screenBackdrop != null) {
                        listOf(screenBackdrop, barBackdrop, accentBackdrop)
                    } else {
                        listOf(barBackdrop, accentBackdrop)
                    }
                }
                val liftNow = lift
                val lensRimBrush = Brush.linearGradient(
                    if (isDark) listOf(
                        Color.White.copy(alpha = 0.10f + 0.55f * liftNow),
                        Color.White.copy(alpha = 0.05f + 0.15f * liftNow),
                        Color.White.copy(alpha = 0.08f + 0.35f * liftNow)
                    ) else listOf(
                        Color.Black.copy(alpha = 0.06f + 0.12f * liftNow),
                        Color.Black.copy(alpha = 0.03f + 0.06f * liftNow),
                        Color.Black.copy(alpha = 0.05f + 0.10f * liftNow)
                    )
                )

                Box(
                    modifier = Modifier
                        .offset(x = animatedOffset - (lensWidth - pillWidth) / 2)
                        .align(Alignment.CenterStart)
                        .requiredSize(width = lensWidth, height = lensHeight)
                        .glassSurface(
                            worlds = lensWorlds,
                            shape = lensShape,
                            blur = 0.dp,
                            refractionHeight = if (liftNow > 0.02f) 18.dp else 0.dp,
                            refractionAmount = 44.dp * liftNow,
                            chromatic = true,
                            onDrawSurface = {
                                // soft light fill: lighter than the bar, like the reference lens
                                drawRect(
                                    if (isDark) Color.White.copy(alpha = 0.13f + 0.05f * liftNow)
                                    else Color.Black.copy(alpha = 0.07f)
                                )
                                drawGlassRim(size.minDimension / 2f, lensRimBrush, 1.2.dp.toPx())
                            }
                        )
                )
            }
        }
    }
}

/** Draws the icon of a navigation destination with the given tint. */
@Composable
private fun NavIcon(
    destination: AppNavDestination,
    tint: Color,
    describe: Boolean
) {
    when (destination) {
        AppNavDestination.HOME -> Icon(
            imageVector = Icons.Filled.Home,
            contentDescription = if (describe) "Home" else null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        AppNavDestination.FOLDERS -> FolderNavIcon(
            tint = tint,
            modifier = Modifier.size(23.dp)
        )
        AppNavDestination.LIBRARY -> BeamedMusicNotesIcon(
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        AppNavDestination.SETTINGS -> Icon(
            imageVector = Icons.Outlined.Settings,
            contentDescription = if (describe) "Settings" else null,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
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

