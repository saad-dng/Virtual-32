package com.antigravity.virtual32.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.antigravity.virtual32.data.NextResult
import com.antigravity.virtual32.receiver.pipeline.PipelineState
import com.antigravity.virtual32.receiver.pipeline.SessionUiState
import com.antigravity.virtual32.receiver.service.BackgroundHealth
import com.antigravity.virtual32.receiver.service.ReceiverService
import com.antigravity.virtual32.receiver.service.ReceiverState
import com.antigravity.virtual32.ui.components.DottedBackgroundBox
import com.antigravity.virtual32.util.OemUtils
import java.io.File
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val health by ReceiverState.health.collectAsState()
    val pipelineState by ReceiverState.pipelineState.collectAsState()
    val lastResult by ReceiverState.lastResult.collectAsState()
    val logs by ReceiverState.logs.collectAsState()
    val sessionState by viewModel.sessionUiState.collectAsState()
    
    var showChecklist by remember { mutableStateOf(false) }
    var fullScreenImage by remember { mutableStateOf<String?>(null) }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        uris.forEach { uri ->
            val intent = Intent(context, ReceiverService::class.java).apply {
                action = ReceiverService.ACTION_PROCESS_GALLERY
                putExtra(ReceiverService.EXTRA_URI, uri.toString())
            }
            context.startService(intent)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DottedBackgroundBox(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StatusCard(health = health, onChecklistClick = { showChecklist = true })
            }
            item {
                SessionCard(
                    sessionState = sessionState,
                    onImageTap = { fullScreenImage = it },
                    onDeletePhoto = { viewModel.deletePhoto(it) },
                    onMoveEarlier = { viewModel.moveEarlier(it) },
                    onMoveLater = { viewModel.moveLater(it) },
                    onAnalyzeNow = { viewModel.analyzeNow() },
                    onCancelSession = { viewModel.cancelSession() }
                )
            }
            item {
                PipelineCard(state = pipelineState, onImageTap = { fullScreenImage = it })
            }
            item {
                NowSignallingCard(lastResult = lastResult)
            }
            item {
                ManualControlsCard(
                    onNext = { viewModel.next() },
                    onRepeat = { viewModel.repeat() },
                    onReset = { viewModel.reset() }
                )
            }
            item {
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery")
                    Spacer(Modifier.width(8.dp))
                    Text("Process from Gallery")
                }
            }
            item {
                BackgroundHealthCard(health = health, onChecklistClick = { showChecklist = true })
            }
            item {
                MiniLogCard(logs = logs)
            }
        }
    }

    if (showChecklist) {
        SetupChecklistDialog(
            health = health,
            onDismiss = { showChecklist = false },
            onStartAnyway = {
                showChecklist = false
                startReceiver(context)
            }
        )
    }

    if (fullScreenImage != null) {
        Dialog(onDismissRequest = { fullScreenImage = null }) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f))
                .clickable { fullScreenImage = null }) {
                AsyncImage(
                    model = File(fullScreenImage!!),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Composable
fun StatusCard(health: BackgroundHealth, onChecklistClick: () -> Unit) {
    val context = LocalContext.current
    val isRunning = health.isServiceRunning
    val ipString = if (health.currentIp != null) "${health.currentIp}:${health.serverPort}" else "Not Bound"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Receiver Status", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = ipString,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("IP", ipString))
                }) {
                    Icon(Icons.Default.ContentCopy, "Copy IP")
                }
                IconButton(onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "http://$ipString")
                    }
                    context.startActivity(Intent.createChooser(intent, "Share IP"))
                }) {
                    Icon(Icons.Default.Share, "Share IP")
                }
            }
            
            val espStatus = when {
                health.lastEspSeenMs <= 0 -> "Never seen"
                System.currentTimeMillis() - health.lastEspSeenMs < 25000 -> "Connected (< 25s ago)"
                else -> "Idle (${(System.currentTimeMillis() - health.lastEspSeenMs)/1000}s ago)"
            }
            Text("ESP32: $espStatus", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    if (isRunning) {
                        context.stopService(Intent(context, ReceiverService::class.java))
                    } else {
                        if (!health.hasNotificationPermission || !health.hasBatteryExemption) {
                            onChecklistClick()
                        } else {
                            val serviceIntent = Intent(context, ReceiverService::class.java)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent)
                            } else {
                                context.startService(serviceIntent)
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (isRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            ) {
                Text(if (isRunning) "STOP" else "START", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SessionCard(
    sessionState: SessionUiState,
    onImageTap: (String) -> Unit,
    onDeletePhoto: (Int) -> Unit,
    onMoveEarlier: (Int) -> Unit,
    onMoveLater: (Int) -> Unit,
    onAnalyzeNow: () -> Unit,
    onCancelSession: () -> Unit
) {
    if (!sessionState.open && !sessionState.isAnalyzing) return

    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Open Session (${if (sessionState.isAnalyzing) sessionState.analyzingPhotoCount else sessionState.photos.size} photos)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (sessionState.isAnalyzing) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                        Text("Analysing", color = Color.White)
                    }
                } else if (sessionState.autoSubmitRemainingSec >= 0) {
                    Text(
                        text = "Auto-submits in ${sessionState.autoSubmitRemainingSec}s",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (sessionState.isAnalyzing) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Analysing ${sessionState.analyzingPhotoCount} photos... (${sessionState.analyzingElapsedSec}s)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ordered thumbnail strip 1..N (tap = fullscreen)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(sessionState.photos) { index, photo ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(84.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onImageTap(photo.cachePath) }
                        ) {
                            AsyncImage(
                                model = File(photo.cachePath),
                                contentDescription = "Photo ${index + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(4.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Each photo has Delete and Move earlier/later actions (only while session is open and not analyzing)
                        if (!sessionState.isAnalyzing) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onMoveEarlier(index) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowBack,
                                        contentDescription = "Move earlier",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeletePhoto(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete photo",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onMoveLater(index) },
                                    enabled = index < sessionState.photos.size - 1,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Move later",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons: "Analyze now" and "Cancel session"
            if (!sessionState.isAnalyzing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancelSession,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Cancel session")
                    }
                    Button(
                        onClick = onAnalyzeNow,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Analyze now")
                    }
                }
            }
        }
    }
}

@Composable
fun PipelineCard(state: PipelineState?, onImageTap: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Photo Pipeline", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            if (state == null) {
                Text("Not initialized.", style = MaterialTheme.typography.bodyMedium)
                return@Column
            }
            
            Text("Queue length: ${state.queueLength}", style = MaterialTheme.typography.bodyMedium)
            
            if (state.isAnalyzing) {
                Text("Current state: ${state.currentStatus}", color = MaterialTheme.colorScheme.primary)
            } else {
                Text("Idle (Last Latency: ${state.lastLatencyMs}ms)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (state.currentStatus?.contains("retired", ignoreCase = true) == true) {
                Spacer(modifier = Modifier.height(6.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = state.currentStatus ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = state.lastPhotoPath != null,
                enter = fadeIn(animationSpec = tween(500)),
                exit = fadeOut(animationSpec = tween(500))
            ) {
                if (state.lastPhotoPath != null) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = File(state.lastPhotoPath),
                            contentDescription = "Current photo",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onImageTap(state.lastPhotoPath) },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NowSignallingCard(lastResult: NextResult?) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).animateContentSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Now Signalling", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (lastResult == null) {
                Text("No data yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else if (!lastResult.ok) {
                Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp)).padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(lastResult.reason ?: "Unknown error", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                }
            } else if (lastResult.end == true) {
                Box(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(8.dp)).padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("CYCLE COMPLETE", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                }
            } else {
                Text("Q${lastResult.q} of ${lastResult.of} → ${lastResult.choice}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val blinks = lastResult.blinks ?: 0
                    for (i in 0 until 5) { // max 5 blinks for A-E
                        Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(if (i < blinks) Color.Blue else Color.LightGray))
                    }
                }
            }
        }
    }
}

@Composable
fun ManualControlsCard(onNext: () -> Unit, onRepeat: () -> Unit, onReset: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = onReset, modifier = Modifier.weight(1f).height(48.dp)) { Text("Reset") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onRepeat, modifier = Modifier.weight(1f).height(48.dp)) { Text("Repeat") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = onNext, modifier = Modifier.weight(1f).height(48.dp)) { Text("Next") }
        }
    }
}

@Composable
fun MiniLogCard(logs: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).animateContentSize()) {
            Text("Recent Server Logs", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            val recent = logs.takeLast(5)
            if (recent.isEmpty()) {
                Text("No logs yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                recent.forEach { line ->
                    Text(line, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun BackgroundHealthCard(health: BackgroundHealth, onChecklistClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Background Health", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onChecklistClick, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Info, contentDescription = "Setup Checklist", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            HealthRow("Service Running", health.isServiceRunning)
            HealthRow("Server Responding", health.isServerResponding)
            HealthRow("Network Available", health.isNetworkUp)
            HealthRow("Wi-Fi Lock Held", health.hasWifiLock)
            HealthRow("Battery Exemption", health.hasBatteryExemption)
            HealthRow("Notification Perm", health.hasNotificationPermission)
        }
    }
}

@Composable
fun HealthRow(label: String, isOk: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(if (isOk) Color(0xFF4CAF50) else Color(0xFFF44336))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun startReceiver(context: Context) {
    val serviceIntent = Intent(context, ReceiverService::class.java)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(serviceIntent)
    } else {
        context.startService(serviceIntent)
    }
}

private fun checkPermissions(context: Context) {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    val hasBattery = pm.isIgnoringBatteryOptimizations(context.packageName)
    
    val hasNotif = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
    
    ReceiverState.update { 
        it.copy(
            hasBatteryExemption = hasBattery,
            hasNotificationPermission = hasNotif
        )
    }
}

@Composable
fun SetupChecklistDialog(health: BackgroundHealth, onDismiss: () -> Unit, onStartAnyway: () -> Unit) {
    val context = LocalContext.current
    val manufacturer = Build.MANUFACTURER.lowercase()
    
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        ReceiverState.update { it.copy(hasNotificationPermission = isGranted) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
            ) {
                Text("Setup Checklist", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                ChecklistItem(
                    title = "Notification Permission",
                    desc = "Required for foreground service.",
                    isDone = health.hasNotificationPermission,
                    onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !health.hasNotificationPermission) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
                
                ChecklistItem(
                    title = "Battery Optimization Exemption",
                    desc = "Prevents Android from killing the server.",
                    isDone = health.hasBatteryExemption,
                    onClick = {
                        if (!health.hasBatteryExemption) {
                            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                data = Uri.parse("package:${context.packageName}")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                )
                
                val oemGuidance = OemUtils.getOemGuidance(manufacturer)
                if (oemGuidance != null) {
                    ChecklistItem(
                        title = "OEM Background Settings",
                        desc = "Your device ($manufacturer) requires manual background allowances. $oemGuidance",
                        isDone = false,
                        onClick = {
                            OemUtils.openOemSettings(context, manufacturer)
                        }
                    )
                }
                
                ChecklistItem(
                    title = "Hotspot Reminder",
                    desc = "Please ensure your phone's hotspot is turned on so the ESP32 can connect.",
                    isDone = health.isNetworkUp,
                    onClick = {}
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = onStartAnyway, modifier = Modifier.height(48.dp)) {
                        Text("Start Anyway")
                    }
                }
            }
        }
    }
}

@Composable
fun ChecklistItem(title: String, desc: String, isDone: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
            .defaultMinSize(minHeight = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isDone) Icons.Default.CheckCircle else Icons.Default.Warning,
            contentDescription = null,
            tint = if (isDone) Color(0xFF4CAF50) else Color(0xFFFF9800)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
