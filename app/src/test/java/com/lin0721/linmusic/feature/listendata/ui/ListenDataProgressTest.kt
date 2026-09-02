package com.lin0721.linmusic.feature.listendata.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// 入场动效的进度换算。渲染没法在单测里验证，但错开区间的边界值容易算错，这里锁住。
class ListenDataProgressTest {

    @Test
    fun `单项时进度直接透传`() {
        assertEquals(0f, itemProgress(0f, 0, 1))
        assertEquals(0.5f, itemProgress(0.5f, 0, 1))
        assertEquals(1f, itemProgress(1f, 0, 1))
    }

    @Test
    fun `总进度归零时所有项都未起步`() {
        repeat(7) { index ->
            assertEquals("第 $index 项", 0f, itemProgress(0f, index, 7))
        }
    }

    @Test
    fun `总进度走满时所有项都到位`() {
        repeat(7) { index ->
            assertEquals("第 $index 项", 1f, itemProgress(1f, index, 7))
        }
    }

    @Test
    fun `首项在错开区间结束前即完成生长`() {
        // 首项 start=0，span=0.6，故 total=0.6 时它正好走完
        assertEquals(1f, itemProgress(0.6f, 0, 7))
    }

    @Test
    fun `末项在错开区间结束时才起步`() {
        // 末项 start=0.4，此刻进度为 0，之后才开始生长
        assertEquals(0f, itemProgress(0.4f, 6, 7))
        assertTrue(itemProgress(0.5f, 6, 7) > 0f)
    }

    @Test
    fun `同一时刻靠前的项进度不落后于靠后的项`() {
        val total = 0.55f
        val values = (0 until 7).map { itemProgress(total, it, 7) }
        values.zipWithNext { front, back ->
            assertTrue("靠前项应不落后：$front vs $back", front >= back)
        }
    }

    @Test
    fun `进度超界时被夹紧在合法区间`() {
        assertEquals(0f, itemProgress(-1f, 3, 7))
        assertEquals(1f, itemProgress(2f, 3, 7))
    }

    @Test
    fun `数值随进度向下取整递增`() {
        assertEquals(0, 464.byProgress(0f))
        assertEquals(232, 464.byProgress(0.5f))
        assertEquals(464, 464.byProgress(1f))
        // 未走完时不得提前显示终值
        assertTrue(464.byProgress(0.999f) < 464)
    }

    @Test
    fun `零值在任何进度下都是零`() {
        assertEquals(0, 0.byProgress(0.5f))
        assertEquals(0, 0.byProgress(1f))
    }
}
