package com.antigravity.virtual32.ui.screens

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.virtual32.camera.CameraCaptureManager
import com.antigravity.virtual32.network.FeedbackState
import com.antigravity.virtual32.network.ImageUploader
import com.antigravity.virtual32.network.ResponseStatusHandler
import com.antigravity.virtual32.settings.AppSettings
import com.antigravity.virtual32.ui.components.DottedBackgroundBox
import com.antigravity.virtual32.ui.components.LedIndicator
import com.antigravity.virtual32.ui.components.Ov3660Viewfinder
import com.antigravity.virtual32.ui.components.TriggerButton
import com.antigravity.virtual32.ui.theme.CozySurface
import com.antigravity.virtual32.ui.theme.HardwareDark
import com.antigravity.virtual32.ui.theme.HardwareOutline
import com.antigravity.virtual32.ui.theme.HardwareSlate
import com.antigravity.virtual32.ui.theme.LedIdle
import kotlinx.coroutines.launch

/**
 * Main Camera capture screen mimicking the ESP32-S3 + OV3660 hardware rig.
 * Wires GPIO 1 trigger to silent capture -> OkHttp multipart upload -> GPIO 2 LED/vibration feedback.
 * Responsive across varied device dimensions, aspect ratios, and orientations.
 */
@Composable
fun CameraScreen(
    previewView: PreviewView,
    cameraManager: CameraCaptureManager,
    uploader: ImageUploader,
    responseHandler: ResponseStatusHandler,
    settings: AppSettings,
    onNavigateToSettings: () -> Unit,
    onSwitchToReceiverMode: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var lastCaptureInfo by remember { mutableStateOf("Ready • Tap GPIO 1 to trigger") }
    var isCapturing by remember { mutableStateOf(false) }
    var feedbackState by remember { mutableStateOf(FeedbackState.IDLE) }
    val scrollState = rememberScrollState()
    var totalDragX by remember { mutableFloatStateOf(0f) }

    DottedBackgroundBox(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            // Swiping left (drag to left < -80f) switches to Earbud Brain
                            if (totalDragX < -80f && onSwitchToReceiverMode != null) {
                                onSwitchToReceiverMode()
                            }
                            totalDragX = 0f
                        },
                        onDragCancel = { totalDragX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            totalDragX += dragAmount
                        }
                    )
                }
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode Switcher Tab Bar
            if (onSwitchToReceiverMode != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFE8ECEF), RoundedCornerShape(10.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(HardwareDark, RoundedCornerShape(8.dp))
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Phone 2 (Camera)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onSwitchToReceiverMode() }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Headphones, contentDescription = null, tint = HardwareSlate, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Phone 1 (Brain)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = HardwareSlate)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VIRTUAL 32",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = HardwareDark,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Phone 2 • ESP32-S3 Camera Twin",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = HardwareSlate
                    )
                }

                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = HardwareDark
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // OV3660 Viewfinder with 4:3 physical sensor framing
            Ov3660Viewfinder(
                previewView = previewView,
                activeResolution = settings.resolution
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Hardware Telemetry & Configuration Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(CozySurface, RoundedCornerShape(10.dp))
                    .border(1.dp, HardwareOutline, RoundedCornerShape(10.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TARGET: ${settings.serverIp}:${settings.serverPort}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HardwareDark
                        )
                        Text(
                            text = if (isCapturing) "POSTING..." else "Q: ${settings.jpegQuality}% • SILENT",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCapturing) Color(0xFFE65100) else Color(0xFF2E7D32)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "SENSOR: ${settings.resolution.name} (${settings.resolution.width}x${settings.resolution.height})",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = HardwareDark
                        )
                        Text(
                            text = "EST: ${settings.resolution.typicalPayloadKb}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = HardwareSlate
                        )
                    }

                    Text(
                        text = "LAST: $lastCaptureInfo",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = HardwareSlate
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Hardware Controls Section: LED Indicator (GPIO 2) & Trigger Button (GPIO 1)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Virtual LED indicator standing in for GPIO 2 (driven by centralized ResponseStatusHandler)
                // Tap to reset to IDLE if desired
                LedIndicator(
                    ledColor = feedbackState.ledColor,
                    stateLabel = feedbackState.statusLabel,
                    onClick = {
                        if (feedbackState.ledColor != LedIdle) {
                            feedbackState = FeedbackState.IDLE
                            lastCaptureInfo = "LED manually reset to IDLE"
                        }
                    }
                )

                // Virtual hardware trigger button standing in for GPIO 1
                TriggerButton(
                    enabled = !isCapturing,
                    onClick = {
                        isCapturing = true
                        lastCaptureInfo = "GPIO 1 triggered • Capturing frame..."

                        // Silent single-frame JPEG capture (no shutter sound, no capture animation, no flash)
                        cameraManager.captureFrameSilently(
                            onSuccess = { jpegBytes, width, height ->
                                val kbSize = String.format("%.1f", jpegBytes.size / 1024.0)
                                lastCaptureInfo = "CAPTURED: $kbSize KB (${width}x${height}) • POSTing..."

                                // Asynchronously POST multipart JPEG to Phone 1's Termux server
                                coroutineScope.launch {
                                    val netResult = uploader.uploadFrame(
                                        serverIp = settings.serverIp,
                                        serverPort = settings.serverPort,
                                        jpegBytes = jpegBytes
                                    )
                                    // Process response exclusively through centralized ResponseStatusHandler
                                    val newFeedback = responseHandler.processResult(netResult)
                                    feedbackState = newFeedback
                                    lastCaptureInfo = "$kbSize KB -> ${newFeedback.description}"
                                    isCapturing = false
                                }
                            },
                            onError = { err ->
                                isCapturing = false
                                lastCaptureInfo = "CAPTURE ERROR: ${err.message}"
                            }
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
