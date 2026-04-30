package com.j41k.fridamanager.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.j41k.fridamanager.ui.theme.*

/**
 * TopBar profesional. Compone:
 *   [logo] FRIDA MANAGER · v1.2.0       [LIVE]   [info] [config]
 */
@Composable
fun ProTopBar(
    versionLabel: String,
    isLive: Boolean,
    onInfoClick: () -> Unit,
    @Suppress("UNUSED_PARAMETER") onSettingsClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "topbar")
    val pulse by transition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "topbar_pulse"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppLogo(size = 30.dp, activeAccent = isLive, animate = isLive)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Black)) {
                        append("FRIDA ")
                    }
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Light, color = TextPrimary.copy(alpha = 0.85f))) {
                        append("MANAGER")
                    }
                },
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                letterSpacing = 1.2.sp,
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = versionLabel,
                    style = MonoCaption,
                    color = TextTertiary,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(3.dp)
                        .clip(CircleShape)
                        .background(TextTertiary.copy(alpha = 0.5f))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Pro Toolkit",
                    style = MaterialTheme.typography.labelSmall,
                    color = AccentCyan.copy(alpha = 0.85f),
                    fontSize = 10.sp
                )
            }
        }

        // Badge LIVE/OFFLINE
//        Row(
//            modifier = Modifier
//                .clip(RoundedCornerShape(12.dp))
//                .background(
//                    if (isLive) StatusOnline.copy(alpha = 0.10f)
//                    else SurfaceHigh.copy(alpha = 0.6f)
//                )
//                .border(
//                    width = 1.dp,
//                    color = if (isLive) StatusOnline.copy(alpha = 0.45f)
//                            else SurfaceBorder,
//                    shape = RoundedCornerShape(12.dp)
//                )
//                .padding(horizontal = 8.dp, vertical = 4.dp),
//            verticalAlignment = Alignment.CenterVertically
//        ) {
//            Box(
//                modifier = Modifier
//                    .size(5.dp)
//                    .clip(CircleShape)
//                    .background(
//                        if (isLive) StatusOnline.copy(alpha = pulse)
//                        else StatusOffline.copy(alpha = 0.65f)
//                    )
//            )
//            Spacer(modifier = Modifier.width(6.dp))
//            Text(
//                text = if (isLive) "LIVE" else "OFFLINE",
//                style = MaterialTheme.typography.labelMedium,
//                color = if (isLive) StatusOnlineHi else TextSecondary,
//                fontSize = 10.sp,
//                letterSpacing = 1.2.sp
//            )
//        }

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(
            onClick = onInfoClick,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceMid.copy(alpha = 0.6f))
                .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = "Información",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }


    // Hairline gradiente bajo el header
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        SurfaceBorder,
                        SurfaceBorder,
                        Color.Transparent
                    )
                )
            )
    )
}
