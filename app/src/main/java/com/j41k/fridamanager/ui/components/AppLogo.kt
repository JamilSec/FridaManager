package com.j41k.fridamanager.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
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
import com.j41k.fridamanager.ui.theme.StatusOnlineHi
import com.j41k.fridamanager.ui.theme.SurfaceBase
import com.j41k.fridamanager.ui.theme.TextPrimary

/**
 * Marca de Frida Manager dibujada en Canvas (estática y nítida a cualquier tamaño).
 *
 * Composición: un anillo-indicador abierto (gradiente, con un hueco tipo instrumento
 * de medición) que rodea un monograma "F" geométrico, más un nodo de acento que
 * representa el punto de inyección. [activeAccent] cambia a la paleta verde cuando el
 * servicio está activo.
 */
@Composable
fun AppLogo(
    size: Dp = 32.dp,
    activeAccent: Boolean = false,
    modifier: Modifier = Modifier
) {
    val ringStart = if (activeAccent) StatusOnline else AccentPrimary
    val ringEnd = if (activeAccent) AccentCyan else AccentPrimaryHi
    val node = if (activeAccent) StatusOnlineHi else AccentCyan
    val glyphColor = if (activeAccent) StatusOnlineHi else TextPrimary

    Canvas(modifier = modifier.size(size)) {
        val side = this.size.minDimension
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val ringStroke = side * 0.085f
        val ringRadius = side / 2f - ringStroke / 2f
        val ringRect = Rect(
            offset = Offset(center.x - ringRadius, center.y - ringRadius),
            size = Size(ringRadius * 2f, ringRadius * 2f)
        )

        // Pista tenue del anillo completo (da cuerpo sin ruido).
        drawCircle(
            color = ringStart.copy(alpha = 0.14f),
            radius = ringRadius,
            center = center,
            style = Stroke(width = ringStroke)
        )

        // Anillo-indicador abierto (arco de ~300°) con gradiente de barrido.
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(ringStart, ringEnd, ringStart),
                center = center
            ),
            startAngle = 128f,
            sweepAngle = 304f,
            useCenter = false,
            topLeft = ringRect.topLeft,
            size = ringRect.size,
            style = Stroke(width = ringStroke, cap = StrokeCap.Round)
        )

        // Monograma "F" geométrico, centrado y de trazo uniforme.
        val stroke = side * 0.1f
        val gW = side * 0.3f
        val gH = side * 0.42f
        val left = center.x - gW / 2f + stroke * 0.1f
        val top = center.y - gH / 2f
        val f = Path().apply {
            // Asta vertical
            moveTo(left, top)
            lineTo(left, top + gH)
            // Brazo superior
            moveTo(left, top)
            lineTo(left + gW, top)
            // Brazo central (más corto)
            moveTo(left, top + gH * 0.46f)
            lineTo(left + gW * 0.72f, top + gH * 0.46f)
        }
        drawPath(f, color = glyphColor, style = Stroke(width = stroke, cap = StrokeCap.Round))

        // Nodo de acento (punto de inyección) sobre el anillo, abajo a la derecha.
        val nodeAngle = Math.toRadians(48.0)
        val nodeCenter = Offset(
            center.x + ringRadius * kotlin.math.cos(nodeAngle).toFloat(),
            center.y + ringRadius * kotlin.math.sin(nodeAngle).toFloat()
        )
        drawCircle(color = SurfaceBase, radius = stroke * 0.9f, center = nodeCenter)
        drawCircle(color = node, radius = stroke * 0.55f, center = nodeCenter)
    }
}
