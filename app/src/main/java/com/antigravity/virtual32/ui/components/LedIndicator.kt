package com.antigravity.virtual32.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.virtual32.ui.theme.HardwareDark
import com.antigravity.virtual32.ui.theme.HardwareSlate
import com.antigravity.virtual32.ui.theme.LedIdle

/**
 * Virtual hardware status LED standing in for GPIO 2 on the ESP32-S3-CAM rig.
 * Features realistic metallic bezel, diode lens, and animated glow pulse when active.
 */
@Composable
fun LedIndicator(
    modifier: Modifier = Modifier,
    ledColor: Color = LedIdle,
    stateLabel: String = "IDLE (GRAY)",
    onClick: (() -> Unit)? = null
) {
    val isActive = ledColor != LedIdle

    // Infinite breathing glow animation when in an active non-idle state
    val infiniteTransition = rememberInfiniteTransition(label = "led_glow_transition")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "led_glow_alpha"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750),
            repeatMode = RepeatMode.Reverse
        ),
        label = "led_glow_scale"
    )

    Row(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Ambient blooming glow behind the bezel when illuminated
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .scale(glowScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    ledColor.copy(alpha = 0.45f * glowAlpha),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // Metallic LED bezel
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF8C8A84), HardwareDark)
                        )
                    )
                    .border(1.5.dp, HardwareSlate, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Diffused LED diode lens
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    ledColor.copy(alpha = if (isActive) (0.85f * glowAlpha).coerceIn(0.6f, 1f) else 0.85f),
                                    ledColor.copy(alpha = if (isActive) (0.55f * glowAlpha).coerceIn(0.3f, 0.8f) else 0.5f)
                                )
                            )
                        )
                        .border(1.dp, Color.White.copy(alpha = if (isActive) 0.6f else 0.35f), CircleShape)
                )
            }
        }

        // Hardware pin identifier & status readout
        Column {
            Text(
                text = "GPIO 2 • STATUS LED",
                color = HardwareDark,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )
            Text(
                text = stateLabel,
                color = if (isActive) ledColor else HardwareSlate,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                fontSize = 10.sp
            )
        }
    }
}
