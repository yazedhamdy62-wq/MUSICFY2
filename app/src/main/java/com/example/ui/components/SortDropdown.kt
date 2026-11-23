package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.ui.theme.ResonanceGreen

private val SmoothOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

/**
 * Sort chip with an iPhone-style menu: the glass menu grows out of the chip and shrinks back
 * smoothly, and the green arrow flips while the menu is open.
 */
@Composable
fun SortDropdown(
    options: List<String>,
    selected: String,
    labelFor: (String) -> String,
    onSelect: (String) -> Unit,
    isDark: Boolean,
    textColor: Color,
    subTextColor: Color,
    modifier: Modifier = Modifier
) {
    val menuState = remember { MutableTransitionState(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (menuState.targetState) 180f else 0f,
        animationSpec = tween(320, easing = SmoothOut),
        label = "sortArrow"
    )
    val offsetY = with(LocalDensity.current) { 42.dp.roundToPx() }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .glassChip(RoundedCornerShape(20.dp), isDark)
                .clickable { menuState.targetState = !menuState.targetState }
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = labelFor(selected),
                color = subTextColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = ResonanceGreen,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(arrowRotation)
            )
        }

        if (menuState.currentState || menuState.targetState) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(0, offsetY),
                onDismissRequest = { menuState.targetState = false },
                properties = PopupProperties(focusable = true)
            ) {
                AnimatedVisibility(
                    visibleState = menuState,
                    enter = fadeIn(tween(200)) +
                        scaleIn(tween(320, easing = SmoothOut), initialScale = 0.82f, transformOrigin = TransformOrigin(1f, 0f)) +
                        expandVertically(tween(320, easing = SmoothOut), expandFrom = Alignment.Top),
                    exit = fadeOut(tween(160)) +
                        scaleOut(tween(220), targetScale = 0.86f, transformOrigin = TransformOrigin(1f, 0f)) +
                        shrinkVertically(tween(220), shrinkTowards = Alignment.Top)
                ) {
                    val shape = RoundedCornerShape(18.dp)
                    val panelFill = if (isDark) Color(0xFF26292D).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.94f)
                    val rim = Brush.verticalGradient(
                        if (isDark) listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.10f))
                        else listOf(Color.Black.copy(alpha = 0.14f), Color.Black.copy(alpha = 0.05f))
                    )
                    Column(
                        modifier = Modifier
                            .widthIn(min = 150.dp)
                            .clip(shape)
                            .background(panelFill)
                            .border(BorderStroke(1.dp, rim), shape)
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        options.forEach { opt ->
                            val isSel = opt == selected
                            val itemShape = RoundedCornerShape(12.dp)
                            Box(
                                modifier = Modifier
                                    .clip(itemShape)
                                    .background(
                                        if (isSel) ResonanceGreen.copy(alpha = 0.16f)
                                        else if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.05f)
                                    )
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isSel) ResonanceGreen.copy(alpha = 0.6f)
                                            else if (isDark) Color.White.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.10f)
                                        ),
                                        itemShape
                                    )
                                    .clickable {
                                        onSelect(opt)
                                        menuState.targetState = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                    .widthIn(min = 118.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = labelFor(opt),
                                    color = if (isSel) ResonanceGreen else textColor,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
