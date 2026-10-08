package com.j41k.fridamanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.j41k.fridamanager.BuildConfig
import com.j41k.fridamanager.ui.theme.*

@Composable
fun CreditsSheetContent(onOpenGithub: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.xl)
            .padding(bottom = Spacing.xl)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AppLogo(size = 40.dp)
            Spacer(Modifier.width(Spacing.lg))
            Column {
                Text(
                    "Frida Manager",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    "v${BuildConfig.VERSION_NAME} · por J41K",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextTertiary
                )
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        Text("Cómo empezar", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
        Spacer(Modifier.height(Spacing.xs))

        ManualStep(1, "Concede root", "La app necesita acceso root para gestionar archivos en /data/local/tmp.")
        ManualStep(2, "Consigue el binario", "Descárgalo desde la pestaña Descargas o súbelo manualmente con adb push.")
        ManualStep(3, "Revisa la red", "En Ajustes puedes cambiar el puerto (27042 por defecto) y la dirección de escucha.")
        ManualStep(4, "Inicia el servicio", "Selecciona el binario y pulsa Iniciar servicio. Se aplica chmod 755 automáticamente.")

        Spacer(Modifier.height(Spacing.lg))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Shapes.control)
                .background(SurfaceHigh.copy(alpha = 0.5f))
                .padding(Spacing.lg),
            verticalAlignment = Alignment.Top
        ) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = AccentCyan, modifier = Modifier.size(Sizes.iconMd))
            Spacer(Modifier.width(Spacing.md))
            Text(
                text = "El servidor se ejecuta como servicio en primer plano para que Android no lo cierre cuando la app pasa a segundo plano.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }

        Spacer(Modifier.height(Spacing.xl))

        OutlinedButton(
            onClick = onOpenGithub,
            modifier = Modifier
                .fillMaxWidth()
                .height(Sizes.touchTarget),
            shape = Shapes.control,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
        ) {
            Text("Frida en GitHub", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.width(Spacing.sm))
            Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ManualStep(number: Int, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.md),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(AccentPrimary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = AccentPrimaryHi
            )
        }
        Spacer(Modifier.width(Spacing.lg))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Spacer(Modifier.height(Spacing.xxs))
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}
