package com.kapa.ailedger.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

@Composable
fun EmptyLedgerIllustration() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(160.dp)) {
            val w = size.width
            val h = size.height
            // piggy bank
            drawCircle(Color(0xFFFFCDD2), w * 0.22f, Offset(w * 0.65f, h * 0.58f))
            // coin slot
            drawRoundRect(
                color = Color(0xFFEF9A9A),
                topLeft = Offset(w * 0.57f, h * 0.36f),
                size = Size(w * 0.16f, h * 0.04f),
                cornerRadius = CornerRadius(4f)
            )
            // dog body
            drawRoundRect(
                color = Color(0xFFFFF3E0),
                topLeft = Offset(w * 0.18f, h * 0.52f),
                size = Size(w * 0.28f, h * 0.22f),
                cornerRadius = CornerRadius(w * 0.08f)
            )
            // dog head
            drawCircle(Color(0xFFFFF3E0), w * 0.14f, Offset(w * 0.32f, h * 0.44f))
            // dog eyes
            drawCircle(Color(0xFF333333), w * 0.025f, Offset(w * 0.28f, h * 0.42f))
            drawCircle(Color(0xFF333333), w * 0.025f, Offset(w * 0.36f, h * 0.42f))
            // dog ears
            drawOval(Color(0xFFFFE0B2), Offset(w * 0.16f, h * 0.38f), Size(w * 0.08f, h * 0.14f))
            drawOval(Color(0xFFFFE0B2), Offset(w * 0.40f, h * 0.38f), Size(w * 0.08f, h * 0.14f))
            // ground line
            drawLine(Color(0xFFE0E0E0), Offset(w * 0.08f, h * 0.78f), Offset(w * 0.92f, h * 0.78f), 2f)
            // grass
            listOf(0.12f, 0.48f, 0.82f).forEach { x ->
                drawLine(Color(0xFFA5D6A7), Offset(w * x, h * 0.78f), Offset(w * x, h * 0.70f), 3f)
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "还没有账单",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            "点右下角 ＋ 记第一笔",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun EmptyDebtIllustration() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(140.dp)) {
            val w = size.width
            val h = size.height
            // dog head
            drawCircle(Color(0xFFFFF3E0), w * 0.16f, Offset(w * 0.5f, h * 0.38f))
            // eyes
            drawCircle(Color(0xFF333333), w * 0.028f, Offset(w * 0.44f, h * 0.36f))
            drawCircle(Color(0xFF333333), w * 0.028f, Offset(w * 0.56f, h * 0.36f))
            // smile
            val smilePath = Path().apply {
                moveTo(w * 0.43f, h * 0.42f)
                cubicTo(w * 0.47f, h * 0.47f, w * 0.53f, h * 0.47f, w * 0.57f, h * 0.42f)
            }
            drawContext.canvas.drawPath(
                smilePath,
                Paint().apply {
                    color = Color(0xFF795548)
                    strokeWidth = 2f
                    style = PaintingStyle.Stroke
                    strokeCap = StrokeCap.Round
                }
            )
            // ears
            drawOval(Color(0xFFFFE0B2), Offset(w * 0.30f, h * 0.28f), Size(w * 0.08f, h * 0.14f))
            drawOval(Color(0xFFFFE0B2), Offset(w * 0.62f, h * 0.28f), Size(w * 0.08f, h * 0.14f))
            // handshake badge
            drawRoundRect(
                Color(0xFFBBDEFB),
                Offset(w * 0.25f, h * 0.62f),
                Size(w * 0.5f, h * 0.14f),
                CornerRadius(w * 0.06f)
            )
            // handshake line
            drawLine(Color(0xFF90CAF9), Offset(w * 0.5f, h * 0.62f), Offset(w * 0.5f, h * 0.76f), 2f)
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "暂无借还款记录",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            "点右下角 ＋ 新增",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
