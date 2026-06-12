package com.kapa.ailedger.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
fun DogAvatar(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val paint = Paint().apply {
            color = Color(0xFF5B5BD6)
            strokeWidth = 2.5f
            style = PaintingStyle.Stroke
            strokeCap = StrokeCap.Round
        }
        // face
        drawCircle(
            color = Color(0xFFFFF3E0),
            radius = w * 0.42f,
            center = Offset(w / 2, h / 2 + h * 0.05f)
        )
        drawContext.canvas.drawCircle(
            Offset(w / 2, h / 2 + h * 0.05f),
            w * 0.42f, paint
        )
        // left ear
        val earPath = Path().apply {
            moveTo(w * 0.22f, h * 0.28f)
            cubicTo(w * 0.05f, h * 0.18f, w * 0.02f, h * 0.52f, w * 0.18f, h * 0.58f)
            cubicTo(w * 0.25f, h * 0.62f, w * 0.30f, h * 0.50f, w * 0.28f, h * 0.38f)
            close()
        }
        drawContext.canvas.drawPath(
            earPath,
            Paint().apply {
                color = Color(0xFFFFE0B2)
                style = PaintingStyle.Fill
            }
        )
        drawContext.canvas.drawPath(earPath, paint)
        // right ear
        val earPath2 = Path().apply {
            moveTo(w * 0.78f, h * 0.28f)
            cubicTo(w * 0.95f, h * 0.18f, w * 0.98f, h * 0.52f, w * 0.82f, h * 0.58f)
            cubicTo(w * 0.75f, h * 0.62f, w * 0.70f, h * 0.50f, w * 0.72f, h * 0.38f)
            close()
        }
        drawContext.canvas.drawPath(
            earPath2,
            Paint().apply {
                color = Color(0xFFFFE0B2)
                style = PaintingStyle.Fill
            }
        )
        drawContext.canvas.drawPath(earPath2, paint)
        // eyes
        drawCircle(Color(0xFF333333), w * 0.055f, Offset(w * 0.38f, h * 0.46f))
        drawCircle(Color(0xFF333333), w * 0.055f, Offset(w * 0.62f, h * 0.46f))
        // eye highlights
        drawCircle(Color.White, w * 0.022f, Offset(w * 0.395f, h * 0.445f))
        drawCircle(Color.White, w * 0.022f, Offset(w * 0.635f, h * 0.445f))
        // nose
        val nosePath = Path().apply {
            moveTo(w * 0.5f, h * 0.56f)
            lineTo(w * 0.44f, h * 0.525f)
            lineTo(w * 0.56f, h * 0.525f)
            close()
        }
        drawContext.canvas.drawPath(
            nosePath,
            Paint().apply { color = Color(0xFF795548); style = PaintingStyle.Fill }
        )
        // smile
        val smilePath = Path().apply {
            moveTo(w * 0.42f, h * 0.60f)
            cubicTo(w * 0.46f, h * 0.66f, w * 0.54f, h * 0.66f, w * 0.58f, h * 0.60f)
        }
        drawContext.canvas.drawPath(smilePath, paint)
        // blush
        drawCircle(Color(0x40FF8A80), w * 0.10f, Offset(w * 0.28f, h * 0.60f))
        drawCircle(Color(0x40FF8A80), w * 0.10f, Offset(w * 0.72f, h * 0.60f))
    }
}
