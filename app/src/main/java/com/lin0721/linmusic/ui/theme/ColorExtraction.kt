package com.lin0721.linmusic.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.drawable.toBitmap

data class ColorPalette(
    val dominant: Color,
    val secondary: Color
)

fun extractDominantColor(drawable: android.graphics.drawable.Drawable): Color {
    return extractColorPalette(drawable).dominant
}

fun extractColorPalette(drawable: android.graphics.drawable.Drawable): ColorPalette {
    return try {
        val bitmap = drawable.toBitmap()
        val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, 10, 10, true)
        val hsv = FloatArray(3)

        // 上半区 → 取鲜艳度最高的像素作为 dominant
        var bestScore1 = -1f
        var bestR1 = 0; var bestG1 = 0; var bestB1 = 0
        for (x in 0 until 10) {
            for (y in 0 until 5) {
                val c = scaled.getPixel(x, y)
                val r = android.graphics.Color.red(c)
                val g = android.graphics.Color.green(c)
                val b = android.graphics.Color.blue(c)
                android.graphics.Color.RGBToHSV(r, g, b, hsv)
                val score = hsv[1] * hsv[2]
                if (score > bestScore1) {
                    bestScore1 = score
                    bestR1 = r; bestG1 = g; bestB1 = b
                }
            }
        }

        // 下半区 → 取鲜艳度最高的像素作为 secondary
        var bestScore2 = -1f
        var bestR2 = 0; var bestG2 = 0; var bestB2 = 0
        for (x in 0 until 10) {
            for (y in 5 until 10) {
                val c = scaled.getPixel(x, y)
                val r = android.graphics.Color.red(c)
                val g = android.graphics.Color.green(c)
                val b = android.graphics.Color.blue(c)
                android.graphics.Color.RGBToHSV(r, g, b, hsv)
                val score = hsv[1] * hsv[2]
                if (score > bestScore2) {
                    bestScore2 = score
                    bestR2 = r; bestG2 = g; bestB2 = b
                }
            }
        }

        val dominant = adjustColor(bestR1, bestG1, bestB1, minSat = 0.5f, valRange = 0.2f..0.7f)
        val secondary = adjustColor(bestR2, bestG2, bestB2, minSat = 0.25f, valRange = 0.15f..0.35f)

        ColorPalette(dominant, secondary)
    } catch (e: Exception) {
        ColorPalette(Color(0xFF333333), Color(0xFF222222))
    }
}

private fun adjustColor(r: Int, g: Int, b: Int, minSat: Float, valRange: ClosedFloatingPointRange<Float>): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.RGBToHSV(r, g, b, hsv)
    hsv[1] = hsv[1].coerceAtLeast(minSat)
    hsv[2] = hsv[2].coerceIn(valRange.start, valRange.endInclusive)
    return Color(android.graphics.Color.HSVToColor(hsv))
}

object PaletteMemoryCache {
    private val cache = java.util.concurrent.ConcurrentHashMap<String, ColorPalette>()

    fun get(mediaId: String): ColorPalette? = cache[mediaId]

    fun put(mediaId: String, palette: ColorPalette) {
        cache[mediaId] = palette
    }
}
