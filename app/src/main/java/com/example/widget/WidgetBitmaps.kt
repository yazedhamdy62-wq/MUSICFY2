package com.example.widget

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Draws the backgrounds of the home-screen widgets.
 *
 * A widget cannot blur or bend what is behind it (the wallpaper) the way the in-app glass does, so
 * the glass widget builds its own little "world": the blurred cover art is the backdrop, then the
 * same edge refraction (with a touch of rainbow dispersion), a dark smoky tint and a light rim as
 * the in-app liquid glass are painted on top of it. Everything is cached per song and size.
 */
object WidgetBitmaps {

    // ---------------------------------------------------------------------------------------
    // Basic helpers
    // ---------------------------------------------------------------------------------------

    fun centerCrop(src: Bitmap, w: Int, h: Int): Bitmap {
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val scale = max(w / src.width.toFloat(), h / src.height.toFloat())
        val m = Matrix()
        m.setScale(scale, scale)
        m.postTranslate((w - src.width * scale) / 2f, (h - src.height * scale) / 2f)
        Canvas(out).drawBitmap(src, m, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        return out
    }

    /** Cheap blur: shrink the image, then scale it back up smoothly. */
    private fun blurred(src: Bitmap, w: Int, h: Int, downWidth: Int): Bitmap {
        val dh = max(1, (downWidth * h / w.toFloat()).roundToInt())
        val small = centerCrop(src, downWidth, dh)
        return Bitmap.createScaledBitmap(small, w, h, true)
    }

    private fun gradientBitmap(w: Int, h: Int, from: Int, to: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val p = Paint().apply {
            shader = LinearGradient(0f, 0f, w.toFloat(), h.toFloat(), from, to, Shader.TileMode.CLAMP)
        }
        Canvas(bmp).drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        return bmp
    }

    fun roundedThumb(art: Bitmap, size: Int, radius: Float): Bitmap {
        val sq = centerCrop(art, size, size)
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(sq, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        Canvas(out).drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), radius, radius, p)
        return out
    }

    fun circleThumb(art: Bitmap, size: Int): Bitmap = roundedThumb(art, size, size / 2f)

    // ---------------------------------------------------------------------------------------
    // Design 2: liquid glass card
    // ---------------------------------------------------------------------------------------

    fun glassBackground(art: Bitmap?, w: Int, h: Int, pxPerDp: Float): Bitmap {
        val base = if (art != null) {
            blurred(art, w, h, 120)
        } else {
            gradientBitmap(w, h, 0xFF1F5135.toInt(), 0xFF0B150A.toInt())
        }

        // A little more colour and a little darker, so white text always reads well
        val graded = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val gc = Canvas(graded)
        val cm = ColorMatrix().apply { setSaturation(1.35f) }
        gc.drawBitmap(base, 0f, 0f, Paint().apply { colorFilter = ColorMatrixColorFilter(cm) })
        gc.drawColor(0x26000000)

        val pixels = IntArray(w * h)
        graded.getPixels(pixels, 0, w, 0, 0, w, h)

        val radius = min(26f * pxPerDp, min(w, h) / 2f)
        val refracted = refract(
            src = pixels,
            w = w,
            h = h,
            radius = radius,
            refractionHeight = 16f * pxPerDp,
            refractionAmount = 22f * pxPerDp,
            dispersion = 1f
        )
        val refractedBmp = Bitmap.createBitmap(refracted, w, h, Bitmap.Config.ARGB_8888)

        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val rect = RectF(0f, 0f, w.toFloat(), h.toFloat())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1) the bent backdrop, clipped to the rounded card
        paint.shader = BitmapShader(refractedBmp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(rect, radius, radius, paint)

        // 2) dark smoky tint (same family as the in-app glass)
        paint.shader = null
        paint.color = 0x8C1E1E1E.toInt()
        canvas.drawRoundRect(rect, radius, radius, paint)

        // 3) soft sheen on the upper half
        paint.shader = LinearGradient(
            0f, 0f, 0f, h * 0.55f,
            0x2EFFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(rect, radius, radius, paint)

        // 4) thin light rim
        val stroke = max(1f, 1.5f * pxPerDp)
        paint.shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            intArrayOf(0x99FFFFFF.toInt(), 0x1FFFFFFF, 0x59FFFFFF),
            null, Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = stroke
        val half = stroke / 2f
        canvas.drawRoundRect(
            RectF(half, half, w - half, h - half),
            max(0f, radius - half), max(0f, radius - half), paint
        )
        return out
    }

    /**
     * Port of the in-app edge refraction: near the border the backdrop is sampled from further
     * inside the card (like a thick glass edge), with a small red/blue split for the rainbow rim.
     */
    private fun refract(
        src: IntArray,
        w: Int,
        h: Int,
        radius: Float,
        refractionHeight: Float,
        refractionAmount: Float,
        dispersion: Float
    ): IntArray {
        val out = IntArray(w * h)
        val hx = w / 2f
        val hy = h / 2f

        for (y in 0 until h) {
            for (x in 0 until w) {
                val i = y * w + x
                val cx = x + 0.5f - hx
                val cy = y + 0.5f - hy

                // signed distance to the rounded rectangle
                val qx = abs(cx) - (hx - radius)
                val qy = abs(cy) - (hy - radius)
                val mx = max(qx, 0f)
                val my = max(qy, 0f)
                val sd = hypot(mx, my) - radius + min(max(qx, qy), 0f)

                if (-sd >= refractionHeight) {
                    out[i] = src[i]
                    continue
                }

                val t = 1f - (-min(sd, 0f)) / refractionHeight
                val d = (1f - sqrt(max(0f, 1f - t * t))) * refractionAmount

                // outward direction of the edge
                val sx = if (cx < 0f) -1f else 1f
                val sy = if (cy < 0f) -1f else 1f
                val gx: Float
                val gy: Float
                if (qx >= 0f || qy >= 0f) {
                    val len = hypot(mx, my)
                    if (len > 0.0001f) {
                        gx = sx * mx / len
                        gy = sy * my / len
                    } else {
                        gx = if (qx >= qy) sx else 0f
                        gy = if (qx >= qy) 0f else sy
                    }
                } else {
                    val gxs = if (qy <= qx) 1f else 0f
                    gx = sx * gxs
                    gy = sy * (1f - gxs)
                }

                val px = x + 0.5f - d * gx
                val py = y + 0.5f - d * gy
                val intensity = dispersion * ((cx * cy) / (hx * hy))
                val dx = -d * gx * intensity
                val dy = -d * gy * intensity

                val r = sample(src, w, h, px + dx, py + dy, 16)
                val g = sample(src, w, h, px, py, 8)
                val b = sample(src, w, h, px - dx, py - dy, 0)
                out[i] = (0xFF shl 24) or
                    (r.roundToInt().coerceIn(0, 255) shl 16) or
                    (g.roundToInt().coerceIn(0, 255) shl 8) or
                    b.roundToInt().coerceIn(0, 255)
            }
        }
        return out
    }

    /** Bilinear sample of one colour channel. */
    private fun sample(src: IntArray, w: Int, h: Int, x: Float, y: Float, shift: Int): Float {
        val fx = (x - 0.5f).coerceIn(0f, w - 1f)
        val fy = (y - 0.5f).coerceIn(0f, h - 1f)
        val x0 = fx.toInt()
        val y0 = fy.toInt()
        val x1 = min(x0 + 1, w - 1)
        val y1 = min(y0 + 1, h - 1)
        val tx = fx - x0
        val ty = fy - y0
        val c00 = ((src[y0 * w + x0] shr shift) and 0xFF).toFloat()
        val c10 = ((src[y0 * w + x1] shr shift) and 0xFF).toFloat()
        val c01 = ((src[y1 * w + x0] shr shift) and 0xFF).toFloat()
        val c11 = ((src[y1 * w + x1] shr shift) and 0xFF).toFloat()
        return (c00 * (1f - tx) + c10 * tx) * (1f - ty) + (c01 * (1f - tx) + c11 * tx) * ty
    }

    // ---------------------------------------------------------------------------------------
    // Design 1: full-bleed cover card
    // ---------------------------------------------------------------------------------------

    fun coverBackground(art: Bitmap?, w: Int, h: Int, pxPerDp: Float): Bitmap {
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val outerR = min(26f * pxPerDp, min(w, h) / 2f)
        val inset = 7f * pxPerDp
        val innerR = max(0f, outerR - inset)
        val outer = RectF(0f, 0f, w.toFloat(), h.toFloat())

        // Dark smoky frame made from the blurred cover
        val frame = if (art != null) {
            blurred(art, w, h, 48)
        } else {
            gradientBitmap(w, h, 0xFF2A2F2C.toInt(), 0xFF0B150A.toInt())
        }
        paint.shader = BitmapShader(frame, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(outer, outerR, outerR, paint)
        paint.shader = null
        paint.color = 0x99141414.toInt()
        canvas.drawRoundRect(outer, outerR, outerR, paint)

        // Inner picture
        val iw = max(1, (w - 2 * inset).roundToInt())
        val ih = max(1, (h - 2 * inset).roundToInt())
        val inner = if (art != null) {
            centerCrop(art, iw, ih)
        } else {
            gradientBitmap(iw, ih, 0xFF1F5135.toInt(), 0xFF0B150A.toInt())
        }

        canvas.save()
        canvas.translate(inset, inset)
        val innerRect = RectF(0f, 0f, iw.toFloat(), ih.toFloat())
        val clip = Path().apply { addRoundRect(innerRect, innerR, innerR, Path.Direction.CW) }
        canvas.clipPath(clip)
        canvas.drawBitmap(inner, 0f, 0f, null)

        // Shade the top (pill + buttons) and bottom (progress + controls) so they stay readable
        paint.shader = LinearGradient(
            0f, 0f, 0f, ih * 0.40f,
            0x99000000.toInt(), 0x00000000, Shader.TileMode.CLAMP
        )
        canvas.drawRect(innerRect, paint)
        paint.shader = LinearGradient(
            0f, ih * 0.45f, 0f, ih.toFloat(),
            0x00000000, 0xCC000000.toInt(), Shader.TileMode.CLAMP
        )
        canvas.drawRect(innerRect, paint)
        canvas.restore()

        // Rims
        paint.shader = null
        paint.style = Paint.Style.STROKE
        val rim = max(1f, 1.2f * pxPerDp)
        paint.strokeWidth = rim
        paint.color = 0x40FFFFFF
        canvas.drawRoundRect(
            RectF(rim / 2f, rim / 2f, w - rim / 2f, h - rim / 2f),
            outerR - rim / 2f, outerR - rim / 2f, paint
        )
        paint.color = 0x1FFFFFFF
        canvas.drawRoundRect(
            RectF(inset, inset, w - inset, h - inset),
            innerR, innerR, paint
        )
        return out
    }
}
