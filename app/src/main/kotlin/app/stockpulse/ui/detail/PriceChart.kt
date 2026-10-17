package app.stockpulse.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.stockpulse.ui.components.linePath
import app.stockpulse.ui.components.scaleToCanvas

@Composable
fun PriceChart(values: List<Double>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val pts = scaleToCanvas(values, size.width, size.height * 0.9f)
            .map { Offset(it.x, it.y + size.height * 0.05f) }
        if (pts.size < 2) return@Canvas
        val line = linePath(pts)
        val fill = linePath(pts).apply {
            lineTo(pts.last().x, size.height)
            lineTo(pts.first().x, size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent)))
        drawPath(line, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
