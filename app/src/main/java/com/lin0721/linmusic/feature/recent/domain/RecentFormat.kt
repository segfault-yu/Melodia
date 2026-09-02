package com.lin0721.linmusic.feature.recent.domain

import java.util.Calendar
import java.util.Locale

// 播放时间转相对文案。超过一周退化成日期，避免出现「37 天前」这种没有参考价值的表述
fun formatPlayedAt(timeMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    if (timeMs <= 0) return ""
    val diff = nowMs - timeMs
    // 设备时钟回拨或服务端时间超前时按刚刚处理，不显示负数
    if (diff < 0) return "刚刚"

    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000
    return when {
        minutes < 1 -> "刚刚"
        minutes < 60 -> "$minutes 分钟前"
        hours < 24 -> "$hours 小时前"
        days < 7 -> "$days 天前"
        else -> formatPlayedDate(timeMs, nowMs)
    }
}

// 跨年才带年份，当年只显示月日
private fun formatPlayedDate(timeMs: Long, nowMs: Long): String {
    val target = Calendar.getInstance().apply { timeInMillis = timeMs }
    val now = Calendar.getInstance().apply { timeInMillis = nowMs }
    val month = target.get(Calendar.MONTH) + 1
    val day = target.get(Calendar.DAY_OF_MONTH)
    return if (target.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
        String.format(Locale.US, "%02d-%02d", month, day)
    } else {
        String.format(Locale.US, "%d-%02d-%02d", target.get(Calendar.YEAR), month, day)
    }
}
