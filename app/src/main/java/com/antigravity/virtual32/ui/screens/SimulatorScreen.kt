package com.antigravity.virtual32.ui.screens

import android.graphics.ImageFormat
import android.util.Size
import androidx.activity.compose.BackHandler
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antigravity.virtual32.settings.SettingsRepository
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulatorScreen(
    modifier: Modifier = Modifier,
    viewModel: SimulatorViewModel = viewModel(),
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)
    
    val lamps by viewModel.lamps.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()
    val photosInSession by viewModel.photosInSession.collectAsState()
    
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    var holdProgress by remember { mutableStateOf(0f) }
    var isHolding by remember { mutableStateOf(false) }
    var holdJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val longPressDetector = remember {
        com.antigravity.virtual32.simulator.LongPressDetector(
            thresholdMs = com.antigravity.virtual32.simulator.BlinkPatterns.LONG_PRESS_MS,
            onShortPress = {
                val ic = imageCapture ?: return@LongPressDetector
                ic.takePicture(cameraExecutor, object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                        val buffer = imageProxy.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining())
                        buffer.get(bytes)
                        imageProxy.close()
                        viewModel.onButton1ShortPress(bytes)
                    }
                    override fun onError(exception: ImageCaptureException) {
                        exception.printStackTrace()
                    }
                })
            },
            onLongPress = {
                viewModel.onButton1LongPress()
            }
        )
    }

    val settingsRepo = remember { SettingsRepository(context) }
    val settings by settingsRepo.settingsFlow.collectAsState(initial = null)

    var host by remember(settings) { mutableStateOf(settings?.simHost ?: "192.168.4.1") }
    var port by remember(settings) { mutableStateOf((settings?.simPort ?: 5000).toString()) }
    var loopback by remember(settings) { mutableStateOf(settings?.simLoopback ?: false) }
    var resolution by remember(settings) { mutableStateOf(settings?.simResolution ?: "UXGA") }
    var jpegQuality by remember(settings) { mutableStateOf((settings?.simJpegQuality ?: 80).toFloat()) }
    var saveGallery by remember(settings) { mutableStateOf(settings?.saveToGallery ?: true) }

    var resExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = { Text("Simulator (ESP32 Twin)", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        )

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Viewfinder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .background(Color.Black)
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            
                            val targetSize = when (resolution) {
                                "VGA" -> Size(640, 480)
                                "SVGA" -> Size(800, 600)
                                "XGA" -> Size(1024, 768)
                                "SXGA" -> Size(1280, 1024)
                                "UXGA" -> Size(1600, 1200)
                                else -> Size(1600, 1200)
                            }
    
                            val ic = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                .setTargetResolution(targetSize)
                                .build()
                            imageCapture = ic
    
                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, ic)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
    
                // Session status badge overlay (top-start)
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(16.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    Text(
                        text = "Photos in session: $photosInSession",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                // Lamps overlay
                Row(
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier.size(24.dp).clip(CircleShape).background(if (lamps.blueOn) Color.Blue else Color.DarkGray)
                    )
                    Box(
                        modifier = Modifier.size(24.dp).clip(CircleShape).background(if (lamps.redOn) Color.Red else Color.DarkGray)
                    )
                }
    
                // Buttons overlay
                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Button 1: Short = Capture/Add, Long = Finish with visual hold indicator
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown()
                                    longPressDetector.onDown(down.position.x, down.position.y)
                                    isHolding = true
                                    holdProgress = 0f
                                    holdJob?.cancel()
                                    holdJob = scope.launch {
                                        val start = System.currentTimeMillis()
                                        while (isActive) {
                                            val elapsed = System.currentTimeMillis() - start
                                            holdProgress = (elapsed.toFloat() / com.antigravity.virtual32.simulator.BlinkPatterns.LONG_PRESS_MS).coerceIn(0f, 1f)
                                            if (elapsed >= com.antigravity.virtual32.simulator.BlinkPatterns.LONG_PRESS_MS) break
                                            delay(16L)
                                        }
                                    }

                                    var pointerUp = false
                                    while (!pointerUp) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull() ?: break
                                        if (change.pressed) {
                                            if (longPressDetector.onMove(change.position.x, change.position.y)) {
                                                isHolding = false
                                                holdProgress = 0f
                                                holdJob?.cancel()
                                            }
                                        } else {
                                            pointerUp = true
                                            holdJob?.cancel()
                                            longPressDetector.onUp(change.position.x, change.position.y)
                                            isHolding = false
                                            holdProgress = 0f
                                        }
                                    }
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isHolding && holdProgress > 0f) {
                            CircularProgressIndicator(
                                progress = { holdProgress },
                                modifier = Modifier.matchParentSize(),
                                color = MaterialTheme.colorScheme.inversePrimary,
                                strokeWidth = 3.dp
                            )
                        }
                        Text(
                            text = if (isHolding && holdProgress >= 1f) "FINISH..." else "BTN 1 (Add/Hold)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
    
                    Button(
                        onClick = { viewModel.onButton2() },
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("BTN 2 (Next/Rep)")
                    }
                }
            }
    
            // Settings
            Column(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            Text("Simulator Network", fontWeight = FontWeight.SemiBold)
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = loopback, onCheckedChange = { loopback = it })
                Text("Loopback (this phone)")
            }

            if (!loopback) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Host IP") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            OutlinedTextField(
                value = port,
                onValueChange = { port = it },
                label = { Text("Port") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = { viewModel.testConnection() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (isConnected) "Test connection (OK)" else "Test connection")
            }

            Text("Camera Settings", fontWeight = FontWeight.SemiBold)
            
            ExposedDropdownMenuBox(
                expanded = resExpanded,
                onExpandedChange = { resExpanded = !resExpanded }
            ) {
                OutlinedTextField(
                    value = resolution,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Resolution") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = resExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = resExpanded,
                    onDismissRequest = { resExpanded = false }
                ) {
                    listOf("VGA", "SVGA", "XGA", "SXGA", "UXGA").forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                resolution = selectionOption
                                resExpanded = false
                            }
                        )
                    }
                }
            }

            Text("JPEG Quality: ${jpegQuality.toInt()}")
            Slider(value = jpegQuality, onValueChange = { jpegQuality = it }, valueRange = 10f..100f)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = saveGallery, onCheckedChange = { saveGallery = it })
                Text("Save captures to gallery")
            }
            
            Button(
                onClick = { 
                    viewModel.updateSettings(host, port.toIntOrNull() ?: 5000, loopback, resolution, jpegQuality.toInt(), saveGallery)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply Settings (rebinds camera)")
            }
            }
        }
    }
}
