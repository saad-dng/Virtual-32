package com.antigravity.virtual32.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.virtual32.ui.theme.HardwareDark
import com.antigravity.virtual32.ui.theme.HardwareSlate
import com.antigravity.virtual32.ui.theme.TriggerBase
import com.antigravity.virtual32.ui.theme.TriggerPressed

/**
 * Virtual trigger button standing in for physical GPIO 1 on the ESP32-S3-CAM rig.
 * Triggers a real, silent CameraX JPEG capture.
 */
@Composable
fun TriggerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1.0f,
        label = "trigger_press_scale"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Physical tactile button housing
        Box(
            modifier = Modifier
                .size(92.dp)
                .scale(scale)
                .shadow(elevation = if (isPressed) 2.dp else 6.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF5A5852), HardwareDark)
                    )
                )
                .border(3.dp, HardwareSlate, CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            // Inner plunger (copper / industrial orange)
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            colors = if (isPressed) {
                                listOf(TriggerPressed, Color(0xFF7E3200))
                            } else {
                                listOf(Color(0xFFE67E22), TriggerBase)
                            }
                        )
                    )
                    .border(2.dp, Color(0xFFFFB37A).copy(alpha = 0.6f), CircleShape)
            )
        }

        // Hardware pin identifier label
        Text(
            text = "GPIO 1 • TRIGGER",
            color = HardwareDark,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 1.sp
        )
    }
}
