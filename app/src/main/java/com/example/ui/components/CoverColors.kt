package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val coverColorCache = LruCache<String, Int>(128)

/**
 * Dominant (vibrant) color of a cover image, animated smoothly when the song changes.
 * [fallback] is shown until the color is extracted (or when there is no image).
 */
@Composable
fun rememberCoverAccent(imageModel: String?, fallback: Color): Color {
    val context = LocalContext.current
    var target by remember(imageModel, fallback) {
        val cached: Int? = if (imageModel != null) coverColorCache.get(imageModel) else null
        mutableStateOf(if (cached != null) Color(cached) else fallback)
    }

    LaunchedEffect(imageModel) {
        if (imageModel.isNullOrBlank()) return@LaunchedEffect
        if (coverColorCache.get(imageModel) != null) return@LaunchedEffect
        val extracted = withContext(Dispatchers.IO) { extractAccentColor(context, imageModel) }
        if (extracted != null) {
            coverColorCache.put(imageModel, extracted.toArgb())
            target = extracted
        }
    }

    val animated by animateColorAsState(
        targetValue = target,
        animationSpec = tween(700),
        label = "coverAccent"
    )
    return animated
}

/**
 * Spotify-style vertical gradient: the cover color at the top fading into [base].
 * [strength] (0..1) makes the color reach further down (used for the lyrics view).
 */
fun coverGradientBrush(accent: Color, base: Color, isDark: Boolean, strength: Float = 0f): Brush {
    val top = if (isDark) lerp(accent, Color.Black, 0.40f) else lerp(accent, Color.White, 0.50f)
    val end = 0.55f + 0.40f * strength.coerceIn(0f, 1f)
    return Brush.verticalGradient(0f to top, end to base, 1f to base)
}

private suspend fun extractAccentColor(context: Context, model: String): Color? {
    return try {
        val request = ImageRequest.Builder(context)
            .data(model)
            .size(64, 64)
            .allowHardware(false)
            .build()
        val result = context.imageLoader.execute(request) as? SuccessResult ?: return null
        val size = 40
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val drawable = result.drawable
        drawable.setBounds(0, 0, size, size)
        drawable.draw(canvas)
        val pixels = IntArray(size * size)
        bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
        bitmap.recycle()
        dominantColor(pixels)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        null
    }
}

/** Picks the most prominent colorful hue; falls back to the average for grey/monochrome covers. */
private fun dominantColor(pixels: IntArray): Color? {
    val buckets = 12
    val weight = DoubleArray(buckets)
    val redSum = DoubleArray(buckets)
    val greenSum = DoubleArray(buckets)
    val blueSum = DoubleArray(buckets)
    var totalR = 0.0
    var totalG = 0.0
    var totalB = 0.0
    var count = 0
    val hsv = FloatArray(3)

    for (p in pixels) {
        if (android.graphics.Color.alpha(p) < 128) continue
        val r = android.graphics.Color.red(p)
        val g = android.graphics.Color.green(p)
        val b = android.graphics.Color.blue(p)
        totalR += r
        totalG += g
        totalB += b
        count++

        android.graphics.Color.colorToHSV(p, hsv)
        val saturation = hsv[1]
        val value = hsv[2]
        if (value < 0.18f || saturation < 0.15f) continue

        val w = (saturation * saturation * (0.4f + value)).toDouble()
        val bucket = ((hsv[0] / 360f) * buckets).toInt().coerceIn(0, buckets - 1)
        weight[bucket] += w
        redSum[bucket] += r * w
        greenSum[bucket] += g * w
        blueSum[bucket] += b * w
    }
    if (count == 0) return null

    var best = -1
    var bestWeight = 0.0
    for (i in 0 until buckets) {
        if (weight[i] > bestWeight) {
            bestWeight = weight[i]
            best = i
        }
    }

    return if (best >= 0 && bestWeight > count * 0.02) {
        Color(
            red = (redSum[best] / bestWeight / 255.0).toFloat().coerceIn(0f, 1f),
            green = (greenSum[best] / bestWeight / 255.0).toFloat().coerceIn(0f, 1f),
            blue = (blueSum[best] / bestWeight / 255.0).toFloat().coerceIn(0f, 1f)
        )
    } else {
        Color(
            red = (totalR / count / 255.0).toFloat().coerceIn(0f, 1f),
            green = (totalG / count / 255.0).toFloat().coerceIn(0f, 1f),
            blue = (totalB / count / 255.0).toFloat().coerceIn(0f, 1f)
        )
    }
}
