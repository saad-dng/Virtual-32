package com.antigravity.virtual32.ui.theme

import androidx.compose.ui.graphics.Color

// Cozy aesthetic palette
val CozyBackground = Color(0xFFF7F4EE)
val CozyDotColor = Color(0xFFD4C8B8).copy(alpha = 0.55f)
val CozySurface = Color(0xFFFFFFFF)
val CozySurfaceSubtle = Color(0xFFEFECE4)

// Hardware instrument palette
val HardwareDark = Color(0xFF22211F)
val HardwareSlate = Color(0xFF4A4843)
val HardwareOutline = Color(0xFFD8D3C8)

// Indicator states (GPIO 2 stand-in)
val LedIdle = Color(0xFF9E9C96)
val LedRedError = Color(0xFFD32F2F)
val LedGreenOk = Color(0xFF388E3C)
val LedAmberTimeout = Color(0xFFFFA000)

// Trigger button (GPIO 1 stand-in)
val TriggerBase = Color(0xFFD35400)
val TriggerPressed = Color(0xFFA04000)
val TriggerHousing = Color(0xFF3D3B37)
