package com.antigravity.virtual32.ui.components

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.antigravity.virtual32.camera.Ov3660Resolution
import com.antigravity.virtual32.camera.SensorSpecs
import com.antigravity.virtual32.ui.theme.HardwareDark

/**
 * Camera viewfinder framed to mimic the physical OmniVision OV3660 sensor (4:3 ratio).
 */
@Composable
fun Ov3660Viewfinder(
    previewView: PreviewView,
    activeResolution: Ov3660Resolution,
    modifier: Modifier = Modifier
) {
    val cornerShape = RoundedCornerShape(12.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .aspectRatio(SensorSpecs.ASPECT_RATIO_FLOAT)
            .clip(cornerShape)
            .border(2.dp, HardwareDark, cornerShape)
            .background(Color.Black)
    ) {
        // Live CameraX viewfinder feed
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Hardware sensor overlay: framing crosshairs and corner brackets
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val strokeColor = Color.White.copy(alpha = 0.35f)
                    val strokeWidth = 2.dp.toPx()
                    val bracketLen = 20.dp.toPx()
                    val inset = 12.dp.toPx()

                    // Top-Left corner bracket
                    drawLine(strokeColor, Offset(inset, inset), Offset(inset + bracketLen, inset), strokeWidth)
                    drawLine(strokeColor, Offset(inset, inset), Offset(inset, inset + bracketLen), strokeWidth)

                    // Top-Right corner bracket
                    drawLine(strokeColor, Offset(size.width - inset, inset), Offset(size.width - inset - bracketLen, inset), strokeWidth)
                    drawLine(strokeColor, Offset(size.width - inset, inset), Offset(size.width - inset, inset + bracketLen), strokeWidth)

                    // Bottom-Left corner bracket
                    drawLine(strokeColor, Offset(inset, size.height - inset), Offset(inset + bracketLen, size.height - inset), strokeWidth)
                    drawLine(strokeColor, Offset(inset, size.height - inset), Offset(inset, size.height - inset - bracketLen), strokeWidth)

                    // Bottom-Right corner bracket
                    drawLine(strokeColor, Offset(size.width - inset, size.height - inset), Offset(size.width - inset - bracketLen, size.height - inset), strokeWidth)
                    drawLine(strokeColor, Offset(size.width - inset, size.height - inset), Offset(size.width - inset, size.height - inset - bracketLen), strokeWidth)

                    // Subtle center reticle
                    val cx = size.width / 2
                    val cy = size.height / 2
                    val reticleLen = 8.dp.toPx()
                    drawLine(strokeColor, Offset(cx - reticleLen, cy), Offset(cx + reticleLen, cy), 1.5.dp.toPx())
                    drawLine(strokeColor, Offset(cx, cy - reticleLen), Offset(cx, cy + reticleLen), 1.5.dp.toPx())
                }
        )

        // Sensor spec badge (top-left)
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                text = "OV3660 4:3 | ${activeResolution.width}x${activeResolution.height}",
                color = Color(0xFFE0E0E0),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Hardware twin FOV badge (bottom-right)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(
                text = "FOV ~66° | RAW/JPEG",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
