package com.j41k.fridamanager.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.j41k.fridamanager.ui.theme.AccentCyan
import com.j41k.fridamanager.ui.theme.AccentPrimary
import com.j41k.fridamanager.ui.theme.AccentPrimaryHi
import com.j41k.fridamanager.ui.theme.StatusOnline

/**
 * Logo de Frida Manager dibujado en Canvas.
 * - Anillo exterior con gradiente (representa la inyección/instrumentación).
 * - Marcas de cuadrante (cuatro segmentos: análisis 360°).
 * - Glifo "F" estilizado en el centro (Frida).
 * Si [animate] es true, el anillo gira lentamente — útil cuando el servicio está activo.
 */
@Composable
fun AppLogo(
    size: Dp = 32.dp,
    activeAccent: Boolean = false,
    animate: Boolean = false,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "logo")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (animate) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "logo_rot"
    )

    val ringStart = if (activeAccent) StatusOnline else AccentPrimary
    val ringEnd = if (activeAccent) AccentCyan else AccentPrimaryHi

    Canvas(modifier = modifier.size(size)) {
        val side = this.size.minDimension
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val ringStroke = side * 0.10f
        val ringRadius = side / 2f - ringStroke / 2f

        // Halo exterior tenue
        drawCircle(
            color = ringStart.copy(alpha = 0.10f),
            radius = side / 2f,
            center = center
        )

        // Anillo principal (gradiente sweep)
        rotate(degrees = rotation, pivot = center) {
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(ringStart, ringEnd, ringStart),
                    center = center
                ),
                radius = ringRadius,
                center = center,
                style = Stroke(width = ringStroke, cap = StrokeCap.Round)
            )
        }

        // 4 marcas de cuadrante
        val markLen = side * 0.07f
        for (i in 0 until 4) {
            val angle = i * 90.0 + 45.0
            val rad = Math.toRadians(angle).toFloat()
            val outerR = ringRadius + ringStroke / 2f + markLen * 0.4f
            val innerR = outerR - markLen
            val sx = center.x + outerR * kotlin.math.cos(rad)
            val sy = center.y + outerR * kotlin.math.sin(rad)
            val ex = center.x + innerR * kotlin.math.cos(rad)
            val ey = center.y + innerR * kotlin.math.sin(rad)
            drawLine(
                color = ringEnd.copy(alpha = 0.55f),
                start = Offset(sx, sy),
                end = Offset(ex, ey),
                strokeWidth = side * 0.025f,
                cap = StrokeCap.Round
            )
        }

        // Glifo "F" estilizado dentro del anillo
        val glyphStroke = side * 0.085f
        val gW = side * 0.34f
        val gH = side * 0.42f
        val gx = center.x - gW / 2f
        val gy = center.y - gH / 2f

        val path = Path().apply {
            // Vertical
            moveTo(gx + glyphStroke / 2f, gy + gH)
            lineTo(gx + glyphStroke / 2f, gy)
            // Top horizontal
            lineTo(gx + gW, gy)
            // Middle horizontal (más corto)
            moveTo(gx + glyphStroke / 2f, gy + gH * 0.48f)
            lineTo(gx + gW * 0.78f, gy + gH * 0.48f)
        }
        drawPath(
            path = path,
            color = if (activeAccent) StatusOnline else Color(0xFFE6EAF2),
            style = Stroke(width = glyphStroke, cap = StrokeCap.Round)
        )
    }
}

/** Sparkline real basado en una serie de valores (heartbeat del polling). */
@Composable
fun Sparkline(
    values: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    fillAlpha: Float = 0.18f
) {
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas
        val w = this.size.width
        val h = this.size.height
        val step = w / (values.size - 1).coerceAtLeast(1)

        val linePath = Path()
        val fillPath = Path()
        values.forEachIndexed { i, v ->
            val x = i * step
            val y = h - (v.coerceIn(0f, 1f) * h * 0.9f) - h * 0.05f
            if (i == 0) {
                linePath.moveTo(x, y)
                fillPath.moveTo(x, h)
                fillPath.lineTo(x, y)
            } else {
                linePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(w, h)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = fillAlpha), Color.Transparent)
            )
        )
        drawPath(
            path = linePath,
            color = color,
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/** Punto estilo grid en blueprint para fondos sutiles. */
@Composable
fun BlueprintGrid(
    modifier: Modifier = Modifier,
    spacing: Dp = 28.dp,
    dotColor: Color = Color.White.copy(alpha = 0.04f)
) {
    Canvas(modifier = modifier) {
        val s = spacing.toPx()
        val r = 0.8.dp.toPx()
        var x = 0f
        while (x <= this.size.width) {
            var y = 0f
            while (y <= this.size.height) {
                drawCircle(color = dotColor, radius = r, center = Offset(x, y))
                y += s
            }
            x += s
        }
    }
}
