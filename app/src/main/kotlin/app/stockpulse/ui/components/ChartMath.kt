package app.stockpulse.ui.components

import androidx.compose.ui.geometry.Offset

/** Maps values to canvas points: first value at x=0, last at x=width, min at the bottom, max at the top. */
fun scaleToCanvas(values: List<Double>, width: Float, height: Float): List<Offset> {
    if (values.isEmpty()) return emptyList()
    val min = values.min()
    val span = values.max() - min
    return values.mapIndexed { i, v ->
        val x = if (values.size == 1) 0f else width * i / (values.size - 1)
        val y = if (span == 0.0) height / 2f else height - ((v - min) / span).toFloat() * height
        Offset(x, y)
    }
}
