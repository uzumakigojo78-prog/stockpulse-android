package app.stockpulse.ui

import app.stockpulse.ui.components.scaleToCanvas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartMathTest {
    @Test
    fun scalesMinToBottomMaxToTop() {
        val pts = scaleToCanvas(listOf(10.0, 20.0, 15.0), width = 100f, height = 50f)
        assertEquals(50f, pts[0].y, 1e-4f) // min -> bottom
        assertEquals(0f, pts[1].y, 1e-4f) // max -> top
        assertEquals(0f, pts[0].x, 1e-4f)
        assertEquals(100f, pts[2].x, 1e-4f)
    }

    @Test
    fun flatSeriesIsCenteredWithoutNaN() {
        val pts = scaleToCanvas(listOf(5.0, 5.0, 5.0), width = 90f, height = 40f)
        assertTrue(pts.all { it.y == 20f && it.x.isFinite() })
    }

    @Test
    fun singlePointDoesNotCrash() {
        val pts = scaleToCanvas(listOf(7.0), width = 90f, height = 40f)
        assertEquals(1, pts.size)
        assertTrue(pts[0].x.isFinite() && pts[0].y.isFinite())
    }

    @Test
    fun emptyInputGivesEmptyOutput() {
        assertEquals(emptyList<Any>(), scaleToCanvas(emptyList(), 10f, 10f))
    }
}
