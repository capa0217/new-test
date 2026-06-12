package com.kapa.ailedger.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val IosBlue = Color(0xFF007AFF)
val IosGreen = Color(0xFF34C759)
val IosRed = Color(0xFFFF3B30)
val TextPrimary = Color(0xFF1C1C1E)
val TextSecondary = Color(0xFF8E8E93)
val CardBg = Color(0xB3FFFFFF)
val CardBorder = Color(0x20000000)
val NavBg = Color(0xE6FFFFFF)
val BubbleUser = Color(0x1A007AFF)
val BubbleAi = Color(0xFFF0F0F5)

val IosBg = Color(0xFFF2F2F7)
val TrackGray = Color(0x1A000000)

val ExpenseRed = IosRed
val IncomeGreen = IosGreen

private val LightColors = lightColorScheme(
    primary = IosBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0x1A007AFF),
    onPrimaryContainer = IosBlue,
    secondary = TextSecondary,
    secondaryContainer = Color(0xFFF0F0F5),
    onSecondaryContainer = TextPrimary,
    tertiary = IosGreen,
    background = Color.Transparent,
    surface = CardBg,
    surfaceVariant = Color(0xFFF2F2F7),
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    outlineVariant = Color(0x1A000000),
    error = IosRed,
    errorContainer = Color(0x1AFF3B30),
    onError = Color.White,
    onErrorContainer = IosRed,
    inverseSurface = TextPrimary,
    inverseOnSurface = Color.White,
    inversePrimary = Color(0xFF4DA3FF)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp)
)

@Composable
fun IosBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IosBg),
        contentAlignment = androidx.compose.ui.Alignment.TopCenter
    ) {
        content()
    }
}

@Composable
fun AiLedgerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, shapes = AppShapes) {
        IosBackground {
            content()
        }
    }
}
