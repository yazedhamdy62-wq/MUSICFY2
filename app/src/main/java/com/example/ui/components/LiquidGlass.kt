/*
 * Liquid-glass helpers for Musicfy.
 *
 * How it works:
 *  1. The screen content is recorded into a GraphicsLayer (glassSource).
 *  2. Every glass surface (nav bar, mini player, sliding lens) draws that layer again, shifted so it
 *     lines up with what is really behind the surface, and applies a RenderEffect to it:
 *       - blur                (Android 12+)
 *       - edge refraction     (Android 13+, AGSL shader, optional chromatic dispersion)
 *  3. A tint + a thin light rim are drawn on top.
 *
 * On older Android versions the effects are skipped and you get a plain tinted glass.
 *
 * The refraction shaders are adapted from Kyant0/AndroidLiquidGlass (Apache License 2.0):
 * https://github.com/Kyant0/AndroidLiquidGlass
 */
package com.example.ui.components

import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.RenderEffect as ComposeRenderEffect

/** The screen content that glass surfaces look through. Provided from MainActivity. */
val LocalGlassBackdrop = compositionLocalOf<GlassBackdrop?> { null }

/** A recorded copy of some content (a GraphicsLayer) plus where it sits on screen. */
@Stable
class GlassBackdrop internal constructor(internal val layer: GraphicsLayer) {
    internal var coordinates: LayoutCoordinates? by mutableStateOf(null)
}

@Composable
fun rememberGlassBackdrop(): GlassBackdrop {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassBackdrop(layer) }
}

/**
 * Records this node's content into [backdrop]. With [showOnScreen] = false the content is only
 * recorded (used for the invisible "green icons" copy that the sliding lens reveals).
 */
fun Modifier.glassSource(backdrop: GlassBackdrop, showOnScreen: Boolean = true): Modifier =
    this
        .onGloballyPositioned { c -> if (c.isAttached) backdrop.coordinates = c }
        .drawWithContent {
            val w = size.width.roundToInt().coerceAtLeast(1)
            val h = size.height.roundToInt().coerceAtLeast(1)
            val content = this
            backdrop.layer.record(this, layoutDirection, IntSize(w, h)) {
                // Make sure the node's content is really drawn INTO the layer (and not onto the screen):
                // point the content scope at the layer's canvas for the duration of drawContent().
                val contentContext = content.drawContext
                val screenCanvas = contentContext.canvas
                contentContext.canvas = this.drawContext.canvas
                try {
                    content.drawContent()
                } finally {
                    contentContext.canvas = screenCanvas
                }
            }
            if (showOnScreen) {
                drawLayer(backdrop.layer)
            }
        }

/**
 * Turns this node into a piece of liquid glass.
 *
 * @param worlds what the glass looks through, drawn in order (first = furthest back)
 * @param blur blur radius of the backdrop
 * @param refractionHeight how far from the edge the bending reaches (0 = no refraction)
 * @param refractionAmount how strongly the edge bends what is behind it
 * @param chromatic adds the rainbow dispersion on the rim (used by the moving lens)
 * @param onDrawSurface drawn over the glass, under the node's own content (tint, rim ...)
 */
fun Modifier.glassSurface(
    worlds: List<GlassBackdrop>,
    shape: CornerBasedShape,
    blur: Dp = 0.dp,
    refractionHeight: Dp = 0.dp,
    refractionAmount: Dp = 0.dp,
    chromatic: Boolean = false,
    onDrawSurface: DrawScope.() -> Unit = {}
): Modifier = composed {
    val glassLayer = rememberGraphicsLayer()
    var myCoords by remember { mutableStateOf<LayoutCoordinates?>(null, neverEqualPolicy()) }
    val shaders = remember { arrayOfNulls<Any>(2) }

    this
        .graphicsLayer {
            clip = true
            this.shape = shape
        }
        .onGloballyPositioned { c -> if (c.isAttached) myCoords = c }
        .drawWithContent {
            val mine = myCoords
            val w = size.width
            val h = size.height
            if (mine != null && mine.isAttached && w >= 1f && h >= 1f) {
                val blurPx = blur.toPx()
                val refHeightPx = refractionHeight.toPx()
                val refAmountPx = refractionAmount.toPx()
                val pad =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurPx > 0f) {
                        ceil(blurPx * 2f).toInt()
                    } else {
                        0
                    }
                val radius = min(
                    shape.topStart.toPx(Size(w, h), this),
                    min(w, h) / 2f
                )

                glassLayer.renderEffect = GlassFx.build(
                    shaders = shaders,
                    widthPx = w,
                    heightPx = h,
                    padPx = pad.toFloat(),
                    radiusPx = radius,
                    blurPx = blurPx,
                    refractionHeightPx = refHeightPx,
                    refractionAmountPx = refAmountPx,
                    chromatic = chromatic
                )

                glassLayer.record(
                    this,
                    layoutDirection,
                    IntSize(w.roundToInt() + pad * 2, h.roundToInt() + pad * 2)
                ) {
                    translate(pad.toFloat(), pad.toFloat()) {
                        for (world in worlds) {
                            val src = world.coordinates
                            if (src != null && src.isAttached) {
                                val o = try {
                                    src.localPositionOf(mine, Offset.Zero)
                                } catch (_: Exception) {
                                    mine.positionInWindow() - src.positionInWindow()
                                }
                                translate(-o.x, -o.y) {
                                    drawLayer(world.layer)
                                }
                            }
                        }
                    }
                }
                glassLayer.topLeft = IntOffset(-pad, -pad)
                drawLayer(glassLayer)
            }
            onDrawSurface()
            drawContent()
        }
}

/** Thin light rim around a rounded rectangle, drawn just inside the edge. */
fun DrawScope.drawGlassRim(cornerRadiusPx: Float, brush: Brush, widthPx: Float) {
    val half = widthPx / 2f
    drawRoundRect(
        brush = brush,
        topLeft = Offset(half, half),
        size = Size(size.width - widthPx, size.height - widthPx),
        cornerRadius = CornerRadius((cornerRadiusPx - half).coerceAtLeast(0f)),
        style = Stroke(width = widthPx)
    )
}

// ---------------------------------------------------------------------------------------------
// RenderEffect plumbing (kept in one object so old Android versions never touch the new classes)
// ---------------------------------------------------------------------------------------------

private object GlassFx {

    fun build(
        shaders: Array<Any?>,
        widthPx: Float,
        heightPx: Float,
        padPx: Float,
        radiusPx: Float,
        blurPx: Float,
        refractionHeightPx: Float,
        refractionAmountPx: Float,
        chromatic: Boolean
    ): ComposeRenderEffect? {
        val sdk = Build.VERSION.SDK_INT
        if (sdk < Build.VERSION_CODES.S) return null

        var effect: AndroidRenderEffect? = null
        if (blurPx > 0f) {
            effect = blurEffect(blurPx)
        }
        if (sdk >= Build.VERSION_CODES.TIRAMISU && refractionHeightPx > 0f && refractionAmountPx > 0f) {
            effect = lensEffect(
                shaders, widthPx, heightPx, padPx, radiusPx,
                refractionHeightPx, refractionAmountPx, chromatic, effect
            )
        }
        return effect?.let { toCompose(it) }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun toCompose(effect: AndroidRenderEffect): ComposeRenderEffect =
        effect.asComposeRenderEffect()

    @RequiresApi(Build.VERSION_CODES.S)
    private fun blurEffect(radiusPx: Float): AndroidRenderEffect =
        AndroidRenderEffect.createBlurEffect(radiusPx, radiusPx, Shader.TileMode.CLAMP)

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun lensEffect(
        shaders: Array<Any?>,
        widthPx: Float,
        heightPx: Float,
        padPx: Float,
        radiusPx: Float,
        refractionHeightPx: Float,
        refractionAmountPx: Float,
        chromatic: Boolean,
        inner: AndroidRenderEffect?
    ): AndroidRenderEffect {
        val index = if (chromatic) 1 else 0
        val shader = (shaders[index] as? RuntimeShader)
            ?: RuntimeShader(if (chromatic) DispersionShader else RefractionShader).also {
                shaders[index] = it
            }
        shader.setFloatUniform("size", widthPx, heightPx)
        shader.setFloatUniform("offset", -padPx, -padPx)
        shader.setFloatUniform("cornerRadii", floatArrayOf(radiusPx, radiusPx, radiusPx, radiusPx))
        shader.setFloatUniform("refractionHeight", refractionHeightPx)
        shader.setFloatUniform("refractionAmount", -refractionAmountPx)
        shader.setFloatUniform("depthEffect", 0f)
        if (chromatic) {
            shader.setFloatUniform("chromaticAberration", 1f)
        }
        val effect = AndroidRenderEffect.createRuntimeShaderEffect(shader, "content")
        return if (inner != null) {
            AndroidRenderEffect.createChainEffect(effect, inner)
        } else {
            effect
        }
    }
}

private const val RoundedRectSdf = """
float radiusAt(float2 centeredCoord, float4 radii) {
    if (centeredCoord.x >= 0.0) {
        if (centeredCoord.y <= 0.0) return radii.y;
        else return radii.z;
    } else {
        if (centeredCoord.y <= 0.0) return radii.x;
        else return radii.w;
    }
}

float sdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    float outside = length(max(cornerCoord, 0.0)) - radius;
    float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);
    return outside + inside;
}

float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {
        return sign(coord) * normalize(max(cornerCoord, 0.0));
    } else {
        float gradX = step(cornerCoord.y, cornerCoord.x);
        return sign(coord) * float2(gradX, 1.0 - gradX);
    }
}
"""

private const val RefractionShader = """
uniform shader content;

uniform float2 size;
uniform float2 offset;
uniform float4 cornerRadii;
uniform float refractionHeight;
uniform float refractionAmount;
uniform float depthEffect;
$RoundedRectSdf
float circleMap(float x) {
    return 1.0 - sqrt(1.0 - x * x);
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = (coord + offset) - halfSize;
    float radius = radiusAt(centeredCoord, cornerRadii);

    float sd = sdRoundedRect(centeredCoord, halfSize, radius);
    if (-sd >= refractionHeight) {
        return content.eval(coord);
    }
    sd = min(sd, 0.0);

    float d = circleMap(1.0 - -sd / refractionHeight) * refractionAmount;
    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
    float2 grad = normalize(gradSdRoundedRect(centeredCoord, halfSize, gradRadius) + depthEffect * normalize(centeredCoord));

    float2 refractedCoord = coord + d * grad;
    return content.eval(refractedCoord);
}
"""

private const val DispersionShader = """
uniform shader content;

uniform float2 size;
uniform float2 offset;
uniform float4 cornerRadii;
uniform float refractionHeight;
uniform float refractionAmount;
uniform float depthEffect;
uniform float chromaticAberration;
$RoundedRectSdf
float circleMap(float x) {
    return 1.0 - sqrt(1.0 - x * x);
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = (coord + offset) - halfSize;
    float radius = radiusAt(centeredCoord, cornerRadii);

    float sd = sdRoundedRect(centeredCoord, halfSize, radius);
    if (-sd >= refractionHeight) {
        return content.eval(coord);
    }
    sd = min(sd, 0.0);

    float d = circleMap(1.0 - -sd / refractionHeight) * refractionAmount;
    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
    float2 grad = normalize(gradSdRoundedRect(centeredCoord, halfSize, gradRadius) + depthEffect * normalize(centeredCoord));

    float2 refractedCoord = coord + d * grad;
    float dispersionIntensity = chromaticAberration * ((centeredCoord.x * centeredCoord.y) / (halfSize.x * halfSize.y));
    float2 dispersedCoord = d * grad * dispersionIntensity;

    half4 color = half4(0.0);

    half4 red = content.eval(refractedCoord + dispersedCoord);
    color.r += red.r / 3.5;
    color.a += red.a / 7.0;

    half4 orange = content.eval(refractedCoord + dispersedCoord * (2.0 / 3.0));
    color.r += orange.r / 3.5;
    color.g += orange.g / 7.0;
    color.a += orange.a / 7.0;

    half4 yellow = content.eval(refractedCoord + dispersedCoord * (1.0 / 3.0));
    color.r += yellow.r / 3.5;
    color.g += yellow.g / 3.5;
    color.a += yellow.a / 7.0;

    half4 green = content.eval(refractedCoord);
    color.g += green.g / 3.5;
    color.a += green.a / 7.0;

    half4 cyan = content.eval(refractedCoord - dispersedCoord * (1.0 / 3.0));
    color.g += cyan.g / 3.5;
    color.b += cyan.b / 3.0;
    color.a += cyan.a / 7.0;

    half4 blue = content.eval(refractedCoord - dispersedCoord * (2.0 / 3.0));
    color.b += blue.b / 3.0;
    color.a += blue.a / 7.0;

    half4 purple = content.eval(refractedCoord - dispersedCoord);
    color.r += purple.r / 7.0;
    color.b += purple.b / 3.0;
    color.a += purple.a / 7.0;

    return color;
}
"""
