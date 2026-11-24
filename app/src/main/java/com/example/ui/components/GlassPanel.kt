package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.gestures.draggable
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

/**
 * The whole app (screens + full-screen player) recorded as one backdrop, so glass panels that open
 * on top (options sheet, sleep timer, play-next queue) can blur and refract what is really behind them.
 * Provided from MainActivity. When it is null the panels fall back to a plain tinted glass look.
 */
val LocalOverlayBackdrop = compositionLocalOf<GlassBackdrop?> { null }

private val SmoothOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/**
 * Liquid-glass look for a panel: blur + soft edge refraction (when a backdrop is available),
 * a dark tint so text stays readable, a light top sheen and a thin bright rim.
 */
@Composable
fun Modifier.glassPanel(
    shape: RoundedCornerShape,
    tint: Color = Color(0xFF111114),
    tintAlpha: Float = 0.50f
): Modifier {
    val backdrop = LocalOverlayBackdrop.current
    val rim = remember {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.60f),
                Color.White.copy(alpha = 0.10f),
                Color.White.copy(alpha = 0.28f)
            )
        )
    }
    val sheen = remember {
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.03f))
        )
    }
    return if (backdrop != null) {
        this.glassSurface(
            worlds = listOf(backdrop),
            shape = shape,
            blur = 26.dp,
            refractionHeight = 12.dp,
            refractionAmount = 16.dp
        ) {
            drawRect(tint.copy(alpha = tintAlpha))
            drawRect(sheen)
            drawGlassRim(shape.topStart.toPx(size, this), rim, 1.2.dp.toPx())
        }
    } else {
        this
            .clip(shape)
            .background(tint.copy(alpha = (tintAlpha + 0.38f).coerceAtMost(0.97f)))
            .drawWithContent {
                drawRect(sheen)
                drawContent()
                drawGlassRim(shape.topStart.toPx(size, this), rim, 1.2.dp.toPx())
            }
    }
}

/**
 * A modal glass panel with smooth enter AND exit animation.
 *
 * - [bottomSheet] = true: slides up from the bottom edge (options sheet, queue).
 * - [bottomSheet] = false: floats in the middle and scales/fades in (sleep timer).
 *
 * Tap the dim area, press back, or call the `close` lambda passed to [content]; the panel animates
 * out first and only then calls [onDismiss].
 */
@Composable
fun GlassOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    bottomSheet: Boolean = true,
    content: @Composable ColumnScope.(close: () -> Unit) -> Unit
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val closing = remember { booleanArrayOf(false) }
    // Finger-follow pull-down for bottom sheets
    val dragY = remember { Animatable(0f) }
    var panelH by remember { mutableStateOf(0f) }
    val dragBy: (Float) -> Unit = { d -> scope.launch { dragY.snapTo((dragY.value + d).coerceAtLeast(0f)) } }
    val settle: () -> Unit = {
        scope.launch {
            if (dragY.value > panelH * 0.22f && panelH > 0f && !closing[0]) {
                closing[0] = true
                dragY.animateTo(panelH, tween(200, easing = FastOutSlowInEasing))
                onDismiss()
            } else {
                dragY.animateTo(0f, androidx.compose.animation.core.spring(stiffness = 500f))
            }
        }
    }
    val pullConnection = remember {
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (dragY.value > 0f && available.y < 0f) {
                    val used = maxOf(available.y, -dragY.value)
                    dragBy(used)
                    return androidx.compose.ui.geometry.Offset(0f, used)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
            override fun onPostScroll(consumed: androidx.compose.ui.geometry.Offset, available: androidx.compose.ui.geometry.Offset, source: androidx.compose.ui.input.nestedscroll.NestedScrollSource): androidx.compose.ui.geometry.Offset {
                if (available.y > 0f && source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput) {
                    dragBy(available.y)
                    return androidx.compose.ui.geometry.Offset(0f, available.y)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
            override suspend fun onPreFling(available: androidx.compose.ui.unit.Velocity): androidx.compose.ui.unit.Velocity {
                if (dragY.value > 0f) { settle(); return available }
                return androidx.compose.ui.unit.Velocity.Zero
            }
        }
    }

    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(durationMillis = 420, easing = SmoothOut))
    }

    val close: () -> Unit = {
        if (!closing[0]) {
            closing[0] = true
            scope.launch {
                progress.animateTo(0f, tween(durationMillis = 260, easing = FastOutSlowInEasing))
                onDismiss()
            }
        }
    }

    // Render in a Dialog window, NOT inline: the backdrop records the whole screen into a
    // GraphicsLayer every frame, and a glass panel inside that recorded subtree would draw the
    // backdrop picture while it is still recording (re-entrant record/draw), which crashes.
    // A Dialog draws in its own window after the backdrop finished recording.
    // Dialog windows do not reliably deliver window insets to their content (and on
    // targetSdk 35+ the window is forced edge-to-edge), so in-dialog navigationBarsPadding can be
    // zero and the sheet bottom would sit at the physical screen edge under the gesture bar.
    // Capture the ACTIVITY window's visible height (status + nav bars excluded) here, in the
    // activity composition where insets are reliable, and size the dialog content to exactly
    // that region: the wrap-height floating window then centers over the visible area and the
    // bottom sheet lands above the navigation bar by construction.
    val ownerDensity = LocalDensity.current
    val ownerVisibleHeight = with(ownerDensity) {
        (LocalConfiguration.current.screenHeightDp * density -
            WindowInsets.statusBars.getTop(this) -
            WindowInsets.navigationBars.getBottom(this)).toDp()
    }
    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
    // When the keyboard is open the IME consumes the first back gesture to hide itself;
    // otherwise this closes the panel.
    BackHandler(onBack = close)
    Box(modifier = Modifier.fillMaxWidth().height(ownerVisibleHeight)) {
        // Dim layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value * (1f - (dragY.value / (panelH.coerceAtLeast(1f))).coerceIn(0f, 1f)) }
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = close
                )
        )

        if (bottomSheet) {
            val shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
            Column(
                modifier = modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { panelH = it.height.toFloat() }
                    .graphicsLayer {
                        translationY = (1f - progress.value) * size.height + dragY.value
                        alpha = (progress.value * 1.6f).coerceIn(0f, 1f)
                    }
                    .nestedScroll(pullConnection)
                    .draggable(
                        orientation = androidx.compose.foundation.gestures.Orientation.Vertical,
                        state = androidx.compose.foundation.gestures.rememberDraggableState { dragBy(it) },
                        onDragStopped = { settle() }
                    )
                    .glassPanel(shape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                // grab handle
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 10.dp, bottom = 6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.45f))
                        .padding(horizontal = 20.dp, vertical = 2.dp)
                )
                content(close)
            }
        } else {
            val shape = RoundedCornerShape(30.dp)
            Column(
                modifier = modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .graphicsLayer {
                        val s = 0.86f + 0.14f * progress.value
                        scaleX = s
                        scaleY = s
                        translationY = (1f - progress.value) * 40.dp.toPx()
                        alpha = progress.value.coerceIn(0f, 1f)
                    }
                    .glassPanel(shape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                content(close)
            }
        }
    }
    }
}

/** Small glass-style control (chips, round buttons): soft fill + light rim. Works on light and dark. */
fun Modifier.glassChip(shape: androidx.compose.ui.graphics.Shape, isDark: Boolean): Modifier {
    val fill = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
    val rim = if (isDark) {
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.34f), Color.White.copy(alpha = 0.08f)))
    } else {
        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.16f), Color.Black.copy(alpha = 0.05f)))
    }
    return this
        .clip(shape)
        .background(fill)
        .border(androidx.compose.foundation.BorderStroke(1.dp, rim), shape)
}
