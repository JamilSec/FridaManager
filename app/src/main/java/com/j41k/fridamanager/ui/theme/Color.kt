package com.j41k.fridamanager.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────
//  FRIDA MANAGER · PRO CYBER-SECURITY PALETTE
//  Inspirada en herramientas serias: Burp Suite, Ghidra, Frida.re
//  Todos los tokens están comentados con su uso semántico.
// ─────────────────────────────────────────────────────────────

// ── Superficie / fondo (escala neutral azulada) ─────────────
val SurfaceBase     = Color(0xFF080B11)   // Fondo absoluto (debajo de todo)
val SurfaceLow      = Color(0xFF0E131C)   // Card/panel principal
val SurfaceMid      = Color(0xFF161D2A)   // Card elevado / topbar
val SurfaceHigh     = Color(0xFF1F2838)   // Hover / focus / chips
val SurfaceBorder   = Color(0xFF2A3448)   // Borde sutil entre superficies
val SurfaceDivider  = Color(0xFF202A3C)   // Divider interno

// ── Texto y neutros ────────────────────────────────────────
val TextPrimary     = Color(0xFFE6EAF2)   // Texto principal
val TextSecondary   = Color(0xFF9AA6BD)   // Texto secundario / labels
val TextTertiary    = Color(0xFF5F6B82)   // Texto desactivado / hints
val TextOnAccent    = Color(0xFF06080D)   // Texto sobre superficie acento

// ── Acentos brand (azul plataforma + cian datos) ───────────
val AccentPrimary   = Color(0xFF3B82F6)   // Azul Frida — botón principal
val AccentPrimaryHi = Color(0xFF60A5FA)   // Hover / brillo
val AccentPrimaryLo = Color(0xFF1D4ED8)   // Pressed / fondo translúcido
val AccentCyan      = Color(0xFF22D3EE)   // Datos técnicos, acentos numéricos
val AccentCyanLo    = Color(0xFF0E7490)

// ── Estados semánticos ─────────────────────────────────────
val StatusOnline    = Color(0xFF10B981)   // Verde esmeralda — servicio activo
val StatusOnlineHi  = Color(0xFF34D399)
val StatusWarning   = Color(0xFFF59E0B)   // Ámbar — warning
val StatusCritical  = Color(0xFFEF4444)   // Rojo — error / detener
val StatusCriticalHi = Color(0xFFFB7185)
val StatusInfo      = AccentPrimary
val StatusOffline   = Color(0xFF64748B)   // Gris azul — apagado

// ── Niveles de log ─────────────────────────────────────────
val LogInfo         = Color(0xFF60A5FA)
val LogOk           = Color(0xFF34D399)
val LogWarn         = Color(0xFFFBBF24)
val LogError        = Color(0xFFF87171)
val LogTrace        = Color(0xFF94A3B8)

// ── Aliases legacy (mantener referencias antiguas vivas) ───
val DeepNaval       = SurfaceBase
val MidnightGlass   = SurfaceMid
val EmeraldNeon     = StatusOnline
val ElectricBlue    = AccentPrimary
val AlertRed        = StatusCritical
val SilverGray      = TextSecondary
val NeonCyan        = AccentCyan
val CharcoalDark    = SurfaceBase
val CharcoalLight   = SurfaceLow
val CobaltPro       = AccentPrimaryLo
val NeonGreenPro    = StatusOnline
val NeonRedPro      = StatusCritical
val NeonRedDeep     = Color(0xFFB71C1C)
val NeonBluePro     = AccentPrimaryHi
val CobaltDark      = SurfaceBase
val SlateDeep       = SurfaceLow
val ElectricPurple  = Color(0xFF7C3AED)
val NeonGreen       = StatusOnline
val NeonRed         = StatusCritical

// Material defaults (para que Theme no falle si los referencia)
val Purple80     = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80       = Color(0xFFEFB8C8)
val Purple40     = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40       = Color(0xFF7D5260)
