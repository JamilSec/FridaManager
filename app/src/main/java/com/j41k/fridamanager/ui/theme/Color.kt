package com.j41k.fridamanager.ui.theme

import androidx.compose.ui.graphics.Color

// ─────────────────────────────────────────────────────────────
//  FRIDA MANAGER · PALETA
//  Inspirada en herramientas serias: Burp Suite, Ghidra, Frida.re
//  Contrastes verificados contra SurfaceLow (fondo de cards):
//  TextPrimary 15:1 · TextSecondary 7.6:1 · TextTertiary 5.3:1
// ─────────────────────────────────────────────────────────────

// ── Superficie / fondo (escala neutral azulada) ─────────────
val SurfaceBase     = Color(0xFF080B11)   // Fondo de la app
val SurfaceLow      = Color(0xFF0E131C)   // Card / barras
val SurfaceMid      = Color(0xFF161D2A)   // Elemento seleccionado / sheet
val SurfaceHigh     = Color(0xFF1F2838)   // Chips / placeholders
val SurfaceBorder   = Color(0xFF2A3448)   // Borde sutil entre superficies
val SurfaceDivider  = Color(0xFF202A3C)   // Divider interno

// ── Texto y neutros ────────────────────────────────────────
val TextPrimary     = Color(0xFFE6EAF2)   // Texto principal
val TextSecondary   = Color(0xFF9AA6BD)   // Texto secundario / labels
val TextTertiary    = Color(0xFF7D89A0)   // Hints y metadatos (cumple AA)
val TextDisabled    = Color(0xFF5F6B82)   // Solo para controles deshabilitados
val TextOnAccent    = Color(0xFF06080D)   // Texto sobre superficie acento

// ── Acentos brand (azul plataforma + cian datos) ───────────
val AccentPrimary   = Color(0xFF3B82F6)   // Azul Frida — acción principal
val AccentPrimaryHi = Color(0xFF60A5FA)   // Texto/iconos de acento sobre fondo oscuro
val AccentPrimaryLo = Color(0xFF1D4ED8)
val AccentCyan      = Color(0xFF22D3EE)   // Datos técnicos, acentos numéricos
val AccentCyanLo    = Color(0xFF0E7490)

// ── Estados semánticos ─────────────────────────────────────
val StatusOnline    = Color(0xFF10B981)   // Servicio activo
val StatusOnlineHi  = Color(0xFF34D399)
val StatusWarning   = Color(0xFFF59E0B)
val StatusCritical  = Color(0xFFEF4444)   // Error / detener
val StatusCriticalHi = Color(0xFFFB7185)
val StatusOffline   = Color(0xFF7D89A0)   // Apagado (mismo valor que TextTertiary)

// ── Niveles de log ─────────────────────────────────────────
val LogInfo         = Color(0xFF60A5FA)
val LogOk           = Color(0xFF34D399)
val LogWarn         = Color(0xFFFBBF24)
val LogError        = Color(0xFFF87171)
val LogTrace        = Color(0xFF94A3B8)
