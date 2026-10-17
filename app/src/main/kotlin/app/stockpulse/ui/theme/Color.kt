package app.stockpulse.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun gainColor(): Color = if (isSystemInDarkTheme()) Color(0xFF7FDB9B) else Color(0xFF1B7F45)

@Composable
fun lossColor(): Color = MaterialTheme.colorScheme.error

@Composable
fun changeColor(change: Double): Color = if (change >= 0) gainColor() else lossColor()
