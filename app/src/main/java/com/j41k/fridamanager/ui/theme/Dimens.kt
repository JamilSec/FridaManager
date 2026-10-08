package com.j41k.fridamanager.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/** Escala de espaciado (múltiplos de 4dp). Usar siempre estos valores. */
object Spacing {
    val xxs = 2.dp
    val xs  = 4.dp
    val sm  = 8.dp
    val md  = 12.dp
    val lg  = 16.dp
    val xl  = 24.dp
    val xxl = 32.dp

    /** Margen horizontal estándar de pantalla. */
    val screen = lg
}

/** Radios de esquina. Tres niveles: chips, controles, contenedores. */
object Radius {
    val sm = 6.dp    // chips / badges
    val md = 12.dp   // botones, inputs, filas seleccionables
    val lg = 16.dp   // cards y contenedores
}

object Shapes {
    val chip      = RoundedCornerShape(Radius.sm)
    val control   = RoundedCornerShape(Radius.md)
    val container = RoundedCornerShape(Radius.lg)
}

/** Alturas de controles. 48dp es el mínimo táctil de Material / 44pt de HIG. */
object Sizes {
    val touchTarget   = 48.dp
    val buttonHeight  = 52.dp
    val iconSm        = 16.dp
    val iconMd        = 20.dp
    val iconLg        = 24.dp
}
