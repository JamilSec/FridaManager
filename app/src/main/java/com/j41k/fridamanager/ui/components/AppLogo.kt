package com.j41k.fridamanager.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.j41k.fridamanager.ui.theme.AccentCyan
import com.j41k.fridamanager.ui.theme.AccentPrimary
import com.j41k.fridamanager.ui.theme.AccentPrimaryHi
import com.j41k.fridamanager.ui.theme.StatusOnline
import com.j41k.fridamanager.ui.theme.TextPrimary

/**
 * Logo de Frida Manager dibujado en Canvas (estático).
 * - Anillo exterior con gradiente (instrumentación).
 * - Marcas de cuadrante (análisis 360°).
 * - Glifo "F" en el centro.
 * [activeAccent] cambia a la paleta verde cuando el servicio está activo.
 */
@Composable
fun AppLogo(
    size: Dp = 32.dp,
    activeAccent: Boolean = false,
    modifier: Modifier = Modifier
) {
    val ringStart = if (activeAccent) StatusOnline else AccentPrimary
    val ringEnd = if (activeAccent) AccentCyan else AccentPrimaryHi
    val glyphColor = if (activeAccent) StatusOnline else TextPrimary

    Canvas(modifier = modifier.size(size)) {
        val side = this.size.minDimension
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val ringStroke = side * 0.10f
        val ringRadius = side / 2f - ringStroke / 2f

        drawCircle(color = ringStart.copy(alpha = 0.10f), radius = side / 2f, center = center)

        drawCircle(
            brush = Brush.sweepGradient(listOf(ringStart, ringEnd, ringStart), center = center),
            radius = ringRadius,
            center = center,
            style = Stroke(width = ringStroke, cap = StrokeCap.Round)
        )

        val markLen = side * 0.07f
        for (i in 0 until 4) {
            val rad = Math.toRadians(i * 90.0 + 45.0).toFloat()
            val outerR = ringRadius + ringStroke / 2f + markLen * 0.4f
            val innerR = outerR - markLen
            drawLine(
                color = ringEnd.copy(alpha = 0.55f),
                start = Offset(center.x + outerR * kotlin.math.cos(rad), center.y + outerR * kotlin.math.sin(rad)),
                end = Offset(center.x + innerR * kotlin.math.cos(rad), center.y + innerR * kotlin.math.sin(rad)),
                strokeWidth = side * 0.025f,
                cap = StrokeCap.Round
            )
        }

        val glyphStroke = side * 0.085f
        val gW = side * 0.34f
        val gH = side * 0.42f
        val gx = center.x - gW / 2f
        val gy = center.y - gH / 2f
        val path = Path().apply {
            moveTo(gx + glyphStroke / 2f, gy + gH)
            lineTo(gx + glyphStroke / 2f, gy)
            lineTo(gx + gW, gy)
            moveTo(gx + glyphStroke / 2f, gy + gH * 0.48f)
            lineTo(gx + gW * 0.78f, gy + gH * 0.48f)
        }
        drawPath(path = path, color = glyphColor, style = Stroke(width = glyphStroke, cap = StrokeCap.Round))
    }
}
