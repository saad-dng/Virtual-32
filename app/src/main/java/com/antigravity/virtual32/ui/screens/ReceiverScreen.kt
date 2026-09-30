package com.antigravity.virtual32.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.virtual32.receiver.ReceiverHttpServer
import com.antigravity.virtual32.receiver.TtsManager
import com.antigravity.virtual32.settings.AppMode
import com.antigravity.virtual32.settings.AppSettings
import com.antigravity.virtual32.settings.SettingsRepository
import com.antigravity.virtual32.ui.components.DottedBackgroundBox
import com.antigravity.virtual32.ui.theme.CozySurface
import com.antigravity.virtual32.ui.theme.HardwareDark
import com.antigravity.virtual32.ui.theme.HardwareOutline
import com.antigravity.virtual32.ui.theme.HardwareSlate
import com.antigravity.virtual32.ui.theme.TriggerBase
import kotlinx.coroutines.launch

/**
 * Main dashboard for Phone 1 (Earbud Brain).
 * Hosts the embedded HTTP server, displays Gemini Vision analysis, and streams voice to earbuds.
 */
@Composable
fun ReceiverScreen(
    currentSettings: AppSettings,
    settingsRepository: SettingsRepository,
    ttsManager: TtsManager,
    isServerRunning: Boolean,
    onToggleServer: () -> Unit,
    lastFrameBytes: ByteArray?,
    lastAnalysis: String,
    onSwitchToCameraMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    var availableInterfaces by remember { mutableStateOf(ReceiverHttpServer.getAvailableInterfaces(context)) }
    var localIp by remember { mutableStateOf(ReceiverHttpServer.getLocalIpAddress(context)) }

    var apiKeyInput by remember(currentSettings) { mutableStateOf(currentSettings.geminiApiKey) }
    var promptInput by remember(currentSettings) { mutableStateOf(currentSettings.geminiPrompt) }
    var showApiKey by remember { mutableStateOf(false) }
    var keySavedFeedback by remember { mutableStateOf(false) }
    var ipCopiedFeedback by remember { mutableStateOf(false) }
    var totalDragX by remember { mutableFloatStateOf(0f) }

    // System Back Gesture: smoothly return to Camera Twin mode
    BackHandler {
        onSwitchToCameraMode()
    }

    val lastBitmap = remember(lastFrameBytes) {
        lastFrameBytes?.let {
            BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap()
        }
    }

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
                            // Swipe right to switch to Camera Twin
                            if (totalDragX > 80f) {
                                onSwitchToCameraMode()
                            }
                            totalDragX = 0f
                        },
                        onDragCancel = { totalDragX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            totalDragX += dragAmount
                        }
                    )
                }
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Mode Switcher Tab Bar
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
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSwitchToCameraMode() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = HardwareSlate, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Phone 2 (Camera)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = HardwareSlate)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(HardwareDark, RoundedCornerShape(8.dp))
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Headphones, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Phone 1 (Brain)", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VIRTUAL 32 • EARBUD BRAIN",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = HardwareDark
                    )
                    Text(
                        text = "Receiver • Gemini Vision • TTS Audio Hub",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = HardwareSlate
                    )
                }

                IconButton(onClick = {
                    availableInterfaces = ReceiverHttpServer.getAvailableInterfaces(context)
                    localIp = ReceiverHttpServer.getLocalIpAddress(context)
                }) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh IP", tint = HardwareDark)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Receiver Server Status Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CozySurface, RoundedCornerShape(12.dp))
                    .border(1.dp, HardwareOutline, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EMBEDDED HTTP RECEIVER",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = HardwareDark
                        )

                        Text(
                            text = if (isServerRunning) "● ONLINE" else "○ STOPPED",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (isServerRunning) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                        )
                    }

                    // Main IP and Endpoint display with copy button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "IP to enter on Phone 2:",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = HardwareSlate
                                    )
                                    Text(
                                        text = localIp,
                                        fontSize = 16.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = HardwareDark
                                    )
                                }

                                Row {
                                    Button(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(localIp))
                                            ipCopiedFeedback = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = HardwareDark),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy IP", modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (ipCopiedFeedback) "COPIED!" else "COPY", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Full Endpoint: http://$localIp:${currentSettings.receiverPort}/upload",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = HardwareSlate
                            )
                        }
                    }

                    // Detected Network Interfaces list
                    if (availableInterfaces.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Detected Network Interfaces:",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = HardwareSlate
                            )
                            availableInterfaces.take(3).forEach { detail ->
                                val badgeColor = when (detail.type) {
                                    "Wi-Fi" -> Color(0xFF1976D2)
                                    "Mobile Hotspot" -> Color(0xFF388E3C)
                                    else -> Color(0xFF757575)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${detail.name} (${detail.ip})",
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = HardwareDark
                                    )
                                    Text(
                                        text = detail.type,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = "Earbuds",
                                tint = Color(0xFF1E88E5),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Earbuds Audio Route: Active",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = HardwareSlate
                            )
                        }

                        Button(
                            onClick = onToggleServer,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isServerRunning) Color(0xFFC62828) else Color(0xFF2E7D32)
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isServerRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isServerRunning) "STOP" else "START",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Last Received Frame & Gemini Scene Description
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CozySurface, RoundedCornerShape(12.dp))
                    .border(1.dp, HardwareOutline, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "LATEST CAPTURE & VISION ANALYSIS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = HardwareDark
                    )

                    if (lastBitmap != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                bitmap = lastBitmap,
                                contentDescription = "Received Frame",
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, HardwareDark, RoundedCornerShape(8.dp))
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "JPEG RECEIVED • ${lastFrameBytes?.size?.div(1024) ?: 0} KB",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HardwareDark
                                )
                                Text(
                                    text = "Sent to Gemini 1.5 Flash Vision",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = HardwareSlate
                                )
                            }
                        }
                    }

                    // Analysis speech text box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                            .border(1.dp, HardwareOutline, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = lastAnalysis.ifBlank { "Waiting for Phone 2 trigger tap... Point Phone 2 at an object and press GPIO 1." },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = if (lastAnalysis.isBlank()) HardwareSlate else HardwareDark
                        )
                    }

                    if (lastAnalysis.isNotBlank()) {
                        Button(
                            onClick = { ttsManager.speak(lastAnalysis) },
                            colors = ButtonDefaults.buttonColors(containerColor = TriggerBase),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Replay", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("REPLAY IN EARBUDS", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Gemini API Key & Vision Prompt Configuration
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CozySurface, RoundedCornerShape(12.dp))
                    .border(1.dp, HardwareOutline, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "GEMINI 1.5 FLASH CONFIGURATION",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = HardwareDark
                    )

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            keySavedFeedback = false
                        },
                        label = { Text("Gemini API Key (AI Studio)", fontFamily = FontFamily.Monospace) },
                        placeholder = { Text("AIzaSy...") },
                        singleLine = true,
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HardwareDark,
                            unfocusedBorderColor = HardwareOutline
                        )
                    )

                    OutlinedTextField(
                        value = promptInput,
                        onValueChange = {
                            promptInput = it
                            keySavedFeedback = false
                        },
                        label = { Text("Vision Assistant Prompt", fontFamily = FontFamily.Monospace) },
                        minLines = 2,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HardwareDark,
                            unfocusedBorderColor = HardwareOutline
                        )
                    )

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                settingsRepository.updateGeminiConfig(
                                    apiKey = apiKeyInput,
                                    prompt = promptInput,
                                    receiverPort = currentSettings.receiverPort
                                )
                                keySavedFeedback = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HardwareDark)
                    ) {
                        if (keySavedFeedback) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Saved")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("KEY & PROMPT SAVED", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        } else {
                            Text("SAVE GEMINI CONFIG", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
