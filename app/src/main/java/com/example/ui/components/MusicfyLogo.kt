package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Musicfy Logo Component
 * Matches the circular serpentine monogram from "Simple Circle Logo By Nymy Media".
 * - In Dark Theme: Circle is White with Dark cutout path (stands out crisply on dark background).
 * - In Light Theme: Circle is Black with White cutout path (stands out crisply on light background).
 */
@Composable
fun MusicfyLogo(
    isDarkTheme: Boolean = true,
    size: Dp = 38.dp,
    modifier: Modifier = Modifier
) {
    val circleColor = if (isDarkTheme) Color.White else Color.Black
    val pathColor = if (isDarkTheme) Color(0xFF0D1114) else Color(0xFFF8FAFC)

    Box(modifier = modifier.size(size)) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = this.size.width
            val h = this.size.height
            val center = Offset(w / 2f, h / 2f)
            val radius = w * 0.48f

            // Draw outer circular disc
            drawCircle(
                color = circleColor,
                radius = radius,
                center = center
            )

            // Scaled geometry for the winding monogram
            // Reference coordinate space: (108 x 108) with center at (54, 54), radius = 28
            val scale = (radius / 28f)
            fun scaleX(x: Float): Float = center.x + (x - 54f) * scale
            fun scaleY(y: Float): Float = center.y + (y - 54f) * scale

            val strokeWidth = 4.2f * scale

            val path = Path().apply {
                // Leg 1: starts at bottom circumference (40, 78.2)
                moveTo(scaleX(40f), scaleY(78.2f))
                lineTo(scaleX(40f), scaleY(47f))

                // Top Arc 1: (40,47) to (47,47) arching up to y=43.5
                cubicTo(
                    scaleX(40f), scaleY(43.5f),
                    scaleX(47f), scaleY(43.5f),
                    scaleX(47f), scaleY(47f)
                )

                // Leg 2: (47,47) to (47,61)
                lineTo(scaleX(47f), scaleY(61f))

                // Bottom Valley 1: (47,61) to (54,61) curving down to y=64.5
                cubicTo(
                    scaleX(47f), scaleY(64.5f),
                    scaleX(54f), scaleY(64.5f),
                    scaleX(54f), scaleY(61f)
                )

                // Leg 3: (54,61) to (54,47)
                lineTo(scaleX(54f), scaleY(47f))

                // Top Arc 2: (54,47) to (61,47) arching up to y=43.5
                cubicTo(
                    scaleX(54f), scaleY(43.5f),
                    scaleX(61f), scaleY(43.5f),
                    scaleX(61f), scaleY(47f)
                )

                // Leg 4: (61,47) to (61,61)
                lineTo(scaleX(61f), scaleY(61f))

                // Bottom Valley 2: (61,61) to (68,61) curving down to y=64.5
                cubicTo(
                    scaleX(61f), scaleY(64.5f),
                    scaleX(68f), scaleY(64.5f),
                    scaleX(68f), scaleY(61f)
                )

                // Leg 5: (68,61) up to top circumference (68, 29.8)
                lineTo(scaleX(68f), scaleY(29.8f))
            }

            drawPath(
                path = path,
                color = pathColor,
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
