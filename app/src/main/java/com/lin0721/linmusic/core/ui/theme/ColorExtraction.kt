package com.lin0721.linmusic.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import com.lin0721.linmusic.core.log.AppLogger

private const val TAG = "ColorExtraction"

// 全屏播放器背景色系统的唯一契约：base 用于 Hero/歌词沉浸背景/顶栏，textHighlight 用于当前播放歌词行高亮
data class PlayerBackdropPalette(
    val base: Color,
    val textHighlight: Color
)

// candidate swatch 饱和度均低于此值时，视为黑白/灰阶封面，不编造色相
private const val MEANINGFUL_SATURATION = 0.08f

// 缓存未命中或取色异常时的兜底色板，迷你播放器与全屏播放器共用以保证配色一致
val FallbackBackdropPalette = PlayerBackdropPalette(FallbackBase, lerp(FallbackBase, Color.White, 0.85f))

fun extractBaseColor(drawable: android.graphics.drawable.Drawable): Color {
    return extractBackdropPalette(drawable).base
}

fun extractBackdropPalette(drawable: android.graphics.drawable.Drawable): PlayerBackdropPalette {
    return try {
        val bitmap = drawable.toBitmap()
        val palette = Palette.from(bitmap).generate()
        val base = pickBaseColor(palette)
        PlayerBackdropPalette(base, lerp(base, Color.White, 0.85f))
    } catch (e: Exception) {
        AppLogger.d(TAG, "取色失败，使用默认深灰色板", e)
        FallbackBackdropPalette
    }
}

// base 相对 Hero 目标背景 #121212 的最低明度，低于这个值基本读不出颜色差异
private const val MIN_LIGHTNESS_AGAINST_BACKDROP = 0.3f

// base 明度上限——封面本身很浅色（比如大片留白插画）时，取色若原样跟着浅，
// 会把封面区上叠加的白色文字/图标（如"播放自"标题、更多按钮）糊得看不清
private const val MAX_LIGHTNESS_FOR_LEGIBILITY = 0.55f

// candidate swatch 在整张封面采样像素里的占比低于此值，视为边角细节（比如一根红丝带），不代表整张封面基调
private const val MIN_POPULATION_SHARE = 0.08f

private fun Palette.Swatch?.takeIfRepresentative(totalPopulation: Int): Palette.Swatch? =
    this?.takeIf { totalPopulation > 0 && it.population >= totalPopulation * MIN_POPULATION_SHARE }

// Vibrant → Muted → Dominant 降级链。DarkMuted 的 target 定义就是"暗+低饱和"，
// 套在中等亮度封面上会显脏；Vibrant 是 Palette 里专门用来代表"这张图最有代表性的颜色"的 target，
// 更贴近封面本身的观感，暗化交给 Hero/歌词区各自的渐变与压暗处理，不指望 base 本身就是暗色。
// Vibrant/Muted 还要求占比够高才采纳，否则大片留白封面里一小块高饱和细节会抢走整个背景色
private fun pickBaseColor(palette: Palette): Color {
    val totalPopulation = palette.swatches.sumOf { it.population }
    val vibrant = palette.vibrantSwatch.takeIfRepresentative(totalPopulation)
    val muted = palette.mutedSwatch.takeIfRepresentative(totalPopulation)
    val dominant = palette.dominantSwatch

    val candidates = listOfNotNull(vibrant, muted, dominant)
    if (candidates.isEmpty()) return FallbackBase

    val maxSaturation = candidates.maxOf { it.hsl[1] }
    if (maxSaturation < MEANINGFUL_SATURATION) {
        val value = candidates.first().hsl[2].coerceIn(0.15f, 0.35f)
        return Color(android.graphics.Color.HSVToColor(floatArrayOf(0f, 0f, value)))
    }

    val chosen = vibrant ?: muted ?: dominant
    val chosenColor = chosen?.let { Color(it.rgb) } ?: return FallbackBase
    val lightness = chosen.hsl[2]

    // 没有 Vibrant 候选、退到 Muted/Dominant 时才可能偏暗——Vibrant 的 target 亮度下限本身就有 0.3，
    // 天然不会踩到这条兜底。跟 #121212 背景对比度不够时直接提亮，而不是原样收下
    val isFallenBackToMutedOrDominant = vibrant == null
    return when {
        lightness > MAX_LIGHTNESS_FOR_LEGIBILITY -> chosenColor.darken(0.25f)
        isFallenBackToMutedOrDominant && lightness < MIN_LIGHTNESS_AGAINST_BACKDROP -> chosenColor.lighten(0.25f)
        else -> chosenColor
    }
}

object PaletteMemoryCache {
    private val cache = android.util.LruCache<String, PlayerBackdropPalette>(150)

    fun get(mediaId: String): PlayerBackdropPalette? = cache.get(mediaId)

    fun put(mediaId: String, palette: PlayerBackdropPalette) {
        cache.put(mediaId, palette)
    }
}
