package com.antigravity.virtual32.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SignalWifiConnectedNoInternet4
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.virtual32.camera.JpegQualityPreset
import com.antigravity.virtual32.camera.Ov3660Resolution
import com.antigravity.virtual32.camera.SensorSpecs
import com.antigravity.virtual32.network.ImageUploader
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
 * Settings screen for configuring Phone 1 destination, OV3660 capture resolution,
 * and JPEG compression quality presets.
 * All settings are persisted reactively via Jetpack Preferences DataStore.
 */
@Composable
fun SettingsScreen(
    currentSettings: AppSettings,
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit,
    uploader: ImageUploader? = null,
    onOpenWelcome: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var serverIp by remember(currentSettings) { mutableStateOf(currentSettings.serverIp) }
    var serverPort by remember(currentSettings) { mutableStateOf(currentSettings.serverPort.toString()) }
    var selectedResolution by remember(currentSettings) { mutableStateOf(currentSettings.resolution) }
    var selectedQuality by remember(currentSettings) { mutableIntStateOf(currentSettings.jpegQuality) }
    var selectedAppMode by remember(currentSettings) { mutableStateOf(currentSettings.appMode) }
    var savedFeedback by remember { mutableStateOf(false) }

    var isTestingConnection by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var totalDragX by remember { mutableFloatStateOf(0f) }

    // Hardware back gesture support
    BackHandler {
        onNavigateBack()
    }

    DottedBackgroundBox(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (totalDragX > 80f) {
                                onNavigateBack()
                            }
                            totalDragX = 0f
                        },
                        onDragCancel = { totalDragX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            totalDragX += dragAmount
                        }
                    )
                }
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top navigation header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = HardwareDark
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = "SETTINGS & HARDWARE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = HardwareDark
                    )
                    Text(
                        text = "${SensorSpecs.SENSOR_NAME} • ${SensorSpecs.HARDWARE_PLATFORM}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = HardwareSlate
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Active Operating Mode Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CozySurface, RoundedCornerShape(12.dp))
                    .border(1.dp, HardwareOutline, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "ACTIVE OPERATING MODE",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = HardwareDark
                    )

                    Text(
                        text = "Choose whether this device acts as the Camera Rig (Phone 2) or the Earbud Brain (Phone 1).",
                        fontSize = 12.sp,
                        color = HardwareSlate
                    )

                    com.antigravity.virtual32.settings.AppMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedAppMode = mode
                                    savedFeedback = false
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedAppMode == mode,
                                onClick = {
                                    selectedAppMode = mode
                                    savedFeedback = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = TriggerBase)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = mode.label,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = if (selectedAppMode == mode) FontWeight.Bold else FontWeight.Normal,
                                color = HardwareDark
                            )
                        }
                    }

                    if (onOpenWelcome != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = onOpenWelcome,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEFECE4)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "LAUNCH DEVICE SETUP WIZARD & LOGO",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = HardwareDark
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Phone 1 Termux Server Destination card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CozySurface, RoundedCornerShape(12.dp))
                    .border(1.dp, HardwareOutline, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "PHONE 1 SERVER ENDPOINT",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = HardwareDark
                    )

                    Text(
                        text = "Configured destination for multipart JPEG POST over local hotspot.",
                        fontSize = 12.sp,
                        color = HardwareSlate
                    )

                    OutlinedTextField(
                        value = serverIp,
                        onValueChange = {
                            serverIp = it
                            savedFeedback = false
                        },
                        label = { Text("Server IP Address", fontFamily = FontFamily.Monospace) },
                        placeholder = { Text("192.168.43.1") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HardwareDark,
                            unfocusedBorderColor = HardwareOutline
                        )
                    )

                    OutlinedTextField(
                        value = serverPort,
                        onValueChange = {
                            serverPort = it
                            savedFeedback = false
                        },
                        label = { Text("Server Port", fontFamily = FontFamily.Monospace) },
                        placeholder = { Text("5000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HardwareDark,
                            unfocusedBorderColor = HardwareOutline
                        )
                    )

                    // Test Connection Button & Diagnostic Readout
                    Button(
                        onClick = {
                            isTestingConnection = true
                            testResult = null
                            coroutineScope.launch {
                                val portNum = serverPort.toIntOrNull() ?: 5000
                                val result = uploader?.testConnection(serverIp, portNum)
                                    ?: Pair(false, "Network uploader not initialized.")
                                isTestingConnection = false
                                testResult = result
                            }
                        },
                        enabled = !isTestingConnection && serverIp.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = HardwareDark),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isTestingConnection) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("TESTING CONNECTION...", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.WifiTethering,
                                contentDescription = "Test Connection",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("TEST CONNECTION TO PHONE 1", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }

                    testResult?.let { (success, message) ->
                        val bgColor = if (success) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        val borderColor = if (success) Color(0xFF4CAF50) else Color(0xFFE57373)
                        val textColor = if (success) Color(0xFF2E7D32) else Color(0xFFC62828)
                        val icon = if (success) Icons.Default.Wifi else Icons.Default.SignalWifiConnectedNoInternet4

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bgColor, RoundedCornerShape(8.dp))
                                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = textColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = message,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // OV3660 Sensor Resolution Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CozySurface, RoundedCornerShape(12.dp))
                    .border(1.dp, HardwareOutline, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "OV3660 RESOLUTION PRESET",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = HardwareDark
                    )

                    Text(
                        text = "Match real ESP32-S3-CAM streaming speed vs detail constraints.",
                        fontSize = 12.sp,
                        color = HardwareSlate
                    )

                    Ov3660Resolution.entries.forEach { res ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedResolution = res
                                    savedFeedback = false
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedResolution == res,
                                onClick = {
                                    selectedResolution = res
                                    savedFeedback = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = TriggerBase)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = res.label,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        fontWeight = if (selectedResolution == res) FontWeight.Bold else FontWeight.Normal,
                                        color = HardwareDark
                                    )
                                    Text(
                                        text = res.typicalPayloadKb,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TriggerBase
                                    )
                                }
                                Text(
                                    text = res.description,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = HardwareSlate
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // JPEG Compression Quality Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CozySurface, RoundedCornerShape(12.dp))
                    .border(1.dp, HardwareOutline, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "JPEG COMPRESSION QUALITY",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = HardwareDark
                    )

                    Text(
                        text = "Tunable compression factor matching ESP32 DMA buffer constraints.",
                        fontSize = 12.sp,
                        color = HardwareSlate
                    )

                    JpegQualityPreset.entries.forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedQuality = preset.qualityPercentage
                                    savedFeedback = false
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedQuality == preset.qualityPercentage,
                                onClick = {
                                    selectedQuality = preset.qualityPercentage
                                    savedFeedback = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = TriggerBase)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = preset.label,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedQuality == preset.qualityPercentage) FontWeight.Bold else FontWeight.Normal,
                                    color = HardwareDark
                                )
                                Text(
                                    text = preset.description,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = HardwareSlate
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save Button
            Button(
                onClick = {
                    val portInt = serverPort.toIntOrNull() ?: AppSettings.DEFAULT_SERVER_PORT
                    coroutineScope.launch {
                        settingsRepository.updateSettings(
                            serverIp = serverIp,
                            serverPort = portInt,
                            resolution = selectedResolution,
                            jpegQuality = selectedQuality
                        )
                        settingsRepository.updateAppMode(selectedAppMode)
                        savedFeedback = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HardwareDark,
                    contentColor = Color.White
                )
            ) {
                if (savedFeedback) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = "Saved")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "SAVED TO DATASTORE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                } else {
                    Text(text = "SAVE SETTINGS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
