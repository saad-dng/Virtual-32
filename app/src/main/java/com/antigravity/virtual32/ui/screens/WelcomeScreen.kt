package com.antigravity.virtual32.ui.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.virtual32.R
import com.antigravity.virtual32.settings.AppMode
import com.antigravity.virtual32.ui.components.DottedBackgroundBox
import com.antigravity.virtual32.ui.theme.CozySurface
import com.antigravity.virtual32.ui.theme.HardwareDark
import com.antigravity.virtual32.ui.theme.HardwareOutline
import com.antigravity.virtual32.ui.theme.HardwareSlate
import com.antigravity.virtual32.ui.theme.TriggerBase

/**
 * Opening Page & Welcome screen for Virtual 32.
 * Presents the app logo, brand identity, and intuitive device role selection
 * (Phone 2 Camera Twin vs Phone 1 Earbud Brain) with fluid animations.
 */
@Composable
fun WelcomeScreen(
    initialMode: AppMode,
    onConfirmRole: (AppMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf(initialMode) }
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    // Breathing glow animation for logo
    val infiniteTransition = rememberInfiniteTransition(label = "logoPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Load logo bitmap from resources safely
    val logoBitmap = remember {
        runCatching {
            context.resources.openRawResource(R.drawable.app_logo).use { stream ->
                BitmapFactory.decodeStream(stream)?.asImageBitmap()
            }
        }.getOrNull()
    }

    DottedBackgroundBox(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { -40 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Animated App Logo with subtle halo
                    Box(
                        modifier = Modifier
                            .size(118.dp)
                            .scale(pulseScale)
                            .background(Color(0xFFE2DDD2).copy(alpha = 0.4f), RoundedCornerShape(28.dp))
                            .border(1.5.dp, HardwareOutline, RoundedCornerShape(28.dp))
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (logoBitmap != null) {
                            Image(
                                bitmap = logoBitmap,
                                contentDescription = "Virtual 32 Logo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(22.dp))
                            )
                        } else {
                            // High-tech fallback icon dial
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(HardwareDark, RoundedCornerShape(22.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "32",
                                    color = Color.White,
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "VIRTUAL 32",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = HardwareDark,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Tactile Hardware Digital Twin & AI Audio Assistant",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = HardwareSlate,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(800, delayMillis = 200)) + slideInVertically(tween(800, delayMillis = 200)) { 50 }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "SELECT THIS PHONE'S ROLE:",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = HardwareSlate,
                        letterSpacing = 1.sp
                    )

                    // Card 1: Phone 2 (Camera Twin)
                    RoleSelectionCard(
                        title = "Phone 2 • The Eyes (Camera Rig)",
                        subtitle = "ESP32-S3 + OV3660 Twin",
                        description = "Silent stealth viewfinder, GPIO 1 trigger button, GPIO 2 status diode, and high-speed multipart JPEG upload over Wi-Fi/Hotspot.",
                        icon = Icons.Default.CameraAlt,
                        badgeText = "CAMERA RIG",
                        isSelected = selectedMode == AppMode.CAMERA_TWIN,
                        onClick = { selectedMode = AppMode.CAMERA_TWIN }
                    )

                    // Card 2: Phone 1 (Earbud Brain)
                    RoleSelectionCard(
                        title = "Phone 1 • The Brain (Audio Assistant)",
                        subtitle = "Receiver • Gemini Vision • TTS",
                        description = "Hosts embedded local receiver server on port 5000. Translates incoming photos via Gemini 1.5 Flash Vision into speech directly inside your Bluetooth earbuds.",
                        icon = Icons.Default.Headphones,
                        badgeText = "EARBUD BRAIN",
                        isSelected = selectedMode == AppMode.RECEIVER_BRAIN,
                        onClick = { selectedMode = AppMode.RECEIVER_BRAIN }
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(1000, delayMillis = 350))
            ) {
                Button(
                    onClick = { onConfirmRole(selectedMode) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HardwareDark
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "LAUNCH " + (if (selectedMode == AppMode.CAMERA_TWIN) "CAMERA TWIN" else "EARBUD BRAIN"),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            letterSpacing = 1.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Continue",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "You can switch roles anytime using the top bar or inside Settings.",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = HardwareSlate.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RoleSelectionCard(
    title: String,
    subtitle: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    badgeText: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) HardwareDark else HardwareOutline
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val backgroundColor = if (isSelected) Color(0xFFF0EDE4) else CozySurface

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.015f else 1.0f,
        animationSpec = tween(200),
        label = "cardScale"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(backgroundColor, RoundedCornerShape(14.dp))
            .border(borderWidth, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isSelected) HardwareDark else Color(0xFFEAE5DA),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else HardwareDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = title,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = HardwareDark
                        )
                        Text(
                            text = subtitle,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = HardwareSlate
                        )
                    }
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = HardwareDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = description,
                fontSize = 11.sp,
                color = HardwareSlate,
                lineHeight = 16.sp
            )
        }
    }
}
