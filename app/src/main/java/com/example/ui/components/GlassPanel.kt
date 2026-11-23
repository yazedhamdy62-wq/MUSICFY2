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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
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

    BackHandler(onBack = close)

    Box(modifier = Modifier.fillMaxSize()) {
        // Dim layer
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress.value }
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
                    .graphicsLayer {
                        translationY = (1f - progress.value) * size.height
                        alpha = (progress.value * 1.6f).coerceIn(0f, 1f)
                    }
                    .glassPanel(shape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
                    .navigationBarsPadding()
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
                    .statusBarsPadding()
                    .navigationBarsPadding()
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
