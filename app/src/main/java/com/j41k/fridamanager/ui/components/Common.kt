package com.j41k.fridamanager.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.j41k.fridamanager.ui.theme.*

/** Contenedor base de la app: superficie plana con borde de 1dp, sin sombras. */
fun Modifier.panel(borderColor: androidx.compose.ui.graphics.Color = SurfaceBorder): Modifier =
    this
        .clip(Shapes.container)
        .background(SurfaceLow)
        .border(1.dp, borderColor, Shapes.container)

/**
 * Encabezado de sección. Acción opcional como IconButton de 48dp.
 */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    actionIcon: ImageVector? = null,
    actionDescription: String? = null,
    isLoading: Boolean = false,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.touchTarget),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                modifier = Modifier.semantics { heading() }
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }

        if (isLoading) {
            Box(Modifier.size(Sizes.touchTarget), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Sizes.iconMd),
                    strokeWidth = 2.dp,
                    color = AccentPrimaryHi
                )
            }
        } else if (actionIcon != null && onAction != null) {
            IconButton(onClick = onAction) {
                Icon(
                    actionIcon,
                    contentDescription = actionDescription,
                    tint = TextSecondary,
                    modifier = Modifier.size(Sizes.iconMd)
                )
            }
        }
    }
}

/**
 * Estado vacío / de error con acción opcional. Siempre ofrece una salida al usuario.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .panel()
            .padding(horizontal = Spacing.xl, vertical = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = TextTertiary,
            modifier = Modifier.size(32.dp)
        )
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(Spacing.lg))
            FilledTonalButton(onClick = onAction, shape = Shapes.control) {
                if (actionIcon != null) {
                    Icon(actionIcon, contentDescription = null, modifier = Modifier.size(Sizes.iconMd))
                    Spacer(Modifier.width(Spacing.sm))
                }
                Text(actionLabel, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Pulso compartido para todos los bloques skeleton de una misma pantalla. */
@Composable
fun rememberSkeletonAlpha(): State<Float> =
    rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.45f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )

/** Bloque placeholder. El alpha se lee en la fase de dibujo (sin recomposición por frame). */
@Composable
fun SkeletonBlock(
    alpha: State<Float>,
    width: Dp? = null,
    height: Dp = 14.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .clip(Shapes.chip)
            .drawBehind { drawRect(SurfaceHigh.copy(alpha = alpha.value)) }
    )
}
