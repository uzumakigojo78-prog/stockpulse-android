package app.stockpulse.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import app.stockpulse.R

/** The launcher icon (neon green rising graph) drawn inside the app. */
@Composable
fun AppLogo(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(percent = 28))
            .background(colorResource(R.color.ic_launcher_background)),
    ) {
        Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = "StockPulse logo")
    }
}
