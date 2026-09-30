package com.antigravity.virtual32

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.antigravity.virtual32.camera.CameraCaptureManager
import com.antigravity.virtual32.network.ImageUploader
import com.antigravity.virtual32.network.OkHttpImageUploader
import com.antigravity.virtual32.network.ResponseStatusHandler
import com.antigravity.virtual32.receiver.GeminiResult
import com.antigravity.virtual32.receiver.GeminiVisionClient
import com.antigravity.virtual32.receiver.ReceiverHttpServer
import com.antigravity.virtual32.receiver.TtsManager
import com.antigravity.virtual32.settings.AppMode
import com.antigravity.virtual32.settings.AppSettings
import com.antigravity.virtual32.settings.SettingsRepository
import com.antigravity.virtual32.ui.screens.CameraScreen
import com.antigravity.virtual32.ui.screens.ReceiverScreen
import com.antigravity.virtual32.ui.screens.SettingsScreen
import com.antigravity.virtual32.ui.theme.HardwareDark
import com.antigravity.virtual32.ui.theme.Virtual32Theme
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.antigravity.virtual32.ui.screens.WelcomeScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var cameraManager: CameraCaptureManager
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var responseHandler: ResponseStatusHandler
    private lateinit var ttsManager: TtsManager
    private val uploader: ImageUploader = OkHttpImageUploader()
    private val geminiClient: GeminiVisionClient = GeminiVisionClient()

    private var hasCameraPermission by mutableStateOf(false)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasCameraPermission = isGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraManager = CameraCaptureManager(this)
        settingsRepository = SettingsRepository(this)
        responseHandler = ResponseStatusHandler(this)
        ttsManager = TtsManager(this)

        checkCameraPermission()

        setContent {
            Virtual32Theme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppRoot(
                        hasCameraPermission = hasCameraPermission,
                        onRequestCameraPermission = { checkCameraPermission() },
                        cameraManager = cameraManager,
                        settingsRepository = settingsRepository,
                        uploader = uploader,
                        responseHandler = responseHandler,
                        ttsManager = ttsManager,
                        geminiClient = geminiClient
                    )
                }
            }
        }
    }

    private fun checkCameraPermission() {
        val permission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            hasCameraPermission = true
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.shutdown()
        ttsManager.shutdown()
    }
}

enum class Screen {
    WELCOME,
    MAIN,
    SETTINGS
}

@Composable
fun AppRoot(
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    cameraManager: CameraCaptureManager,
    settingsRepository: SettingsRepository,
    uploader: ImageUploader,
    responseHandler: ResponseStatusHandler,
    ttsManager: TtsManager,
    geminiClient: GeminiVisionClient
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val currentSettings by settingsRepository.settingsFlow.collectAsStateWithLifecycle(
        initialValue = AppSettings()
    )

    var currentScreen by remember { mutableStateOf(Screen.MAIN) }
    var lastReceivedFrame by remember { mutableStateOf<ByteArray?>(null) }
    var lastAnalysisText by remember { mutableStateOf("") }

    // Embedded receiver server for Phone 1 mode
    var isServerManualRunning by remember { mutableStateOf(true) }
    val receiverServer = remember(currentSettings.receiverPort) {
        ReceiverHttpServer(
            port = currentSettings.receiverPort,
            context = context
        ) { jpegBytes, clientIp ->
            lastReceivedFrame = jpegBytes
            if (currentSettings.geminiApiKey.isNotBlank()) {
                val result = geminiClient.analyzeImage(
                    jpegBytes = jpegBytes,
                    apiKey = currentSettings.geminiApiKey,
                    prompt = currentSettings.geminiPrompt
                )
                when (result) {
                    is GeminiResult.Success -> {
                        lastAnalysisText = result.text
                        ttsManager.speak(result.text)
                        result.text
                    }
                    is GeminiResult.Error -> {
                        val errMsg = "Gemini Error: ${result.message}"
                        lastAnalysisText = errMsg
                        ttsManager.speak(errMsg)
                        errMsg
                    }
                }
            } else {
                val notice = "Frame received (${jpegBytes.size / 1024} KB). Configure Gemini API key on Phone 1 to hear visual scene analysis."
                lastAnalysisText = notice
                ttsManager.speak("Frame received from Phone 2. Please enter Gemini API key on Phone 1.")
                notice
            }
        }
    }

    // Auto-start/stop receiver HTTP server when in Phone 1 (RECEIVER_BRAIN) mode
    LaunchedEffect(currentSettings.appMode, isServerManualRunning) {
        if (currentSettings.appMode == AppMode.RECEIVER_BRAIN && isServerManualRunning) {
            receiverServer.start()
        } else {
            receiverServer.stop()
        }
    }

    DisposableEffect(receiverServer) {
        onDispose {
            receiverServer.stop()
        }
    }

    // Remember a single PreviewView for the camera session
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Rebind camera only when in Camera Twin mode and permission granted
    LaunchedEffect(currentSettings.appMode, currentSettings.resolution, currentSettings.jpegQuality, hasCameraPermission) {
        if (currentSettings.appMode == AppMode.CAMERA_TWIN && hasCameraPermission) {
            cameraManager.startCamera(
                lifecycleOwner = lifecycleOwner,
                previewView = previewView,
                resolution = currentSettings.resolution,
                jpegQuality = currentSettings.jpegQuality,
                onError = { /* Error handled in manager */ }
            )
        }
    }

    var initialLaunchCheck by remember { mutableStateOf(false) }
    LaunchedEffect(currentSettings.hasCompletedWelcome) {
        if (!initialLaunchCheck) {
            if (!currentSettings.hasCompletedWelcome) {
                currentScreen = Screen.WELCOME
            }
            initialLaunchCheck = true
        }
    }

    // Back gesture handling: navigate back to main screen from settings or welcome
    BackHandler(enabled = currentScreen == Screen.SETTINGS || (currentScreen == Screen.WELCOME && currentSettings.hasCompletedWelcome)) {
        currentScreen = Screen.MAIN
    }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn(animationSpec = tween(320)) togetherWith fadeOut(animationSpec = tween(260))
        },
        label = "AppScreenTransition"
    ) { screen ->
        when (screen) {
            Screen.WELCOME -> {
                WelcomeScreen(
                    initialMode = currentSettings.appMode,
                    onConfirmRole = { chosenMode ->
                        coroutineScope.launch {
                            settingsRepository.updateAppMode(chosenMode)
                            settingsRepository.setCompletedWelcome(true)
                        }
                        currentScreen = Screen.MAIN
                    }
                )
            }
            Screen.SETTINGS -> {
                SettingsScreen(
                    currentSettings = currentSettings,
                    settingsRepository = settingsRepository,
                    uploader = uploader,
                    onNavigateBack = { currentScreen = Screen.MAIN },
                    onOpenWelcome = { currentScreen = Screen.WELCOME }
                )
            }
            Screen.MAIN -> {
                AnimatedContent(
                    targetState = currentSettings.appMode,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(240))
                    },
                    label = "ModeTransition"
                ) { mode ->
                    when (mode) {
                        AppMode.CAMERA_TWIN -> {
                            if (hasCameraPermission) {
                                CameraScreen(
                                    previewView = previewView,
                                    cameraManager = cameraManager,
                                    uploader = uploader,
                                    responseHandler = responseHandler,
                                    settings = currentSettings,
                                    onNavigateToSettings = { currentScreen = Screen.SETTINGS },
                                    onSwitchToReceiverMode = {
                                        coroutineScope.launch {
                                            settingsRepository.updateAppMode(AppMode.RECEIVER_BRAIN)
                                        }
                                    }
                                )
                            } else {
                                PermissionRequiredScreen(onRequestPermission = onRequestCameraPermission)
                            }
                        }
                        AppMode.RECEIVER_BRAIN -> {
                            val serverState by receiverServer.isRunning.collectAsState()
                            ReceiverScreen(
                                currentSettings = currentSettings,
                                settingsRepository = settingsRepository,
                                ttsManager = ttsManager,
                                isServerRunning = serverState,
                                onToggleServer = {
                                    isServerManualRunning = !isServerManualRunning
                                },
                                lastFrameBytes = lastReceivedFrame,
                                lastAnalysis = lastAnalysisText,
                                onSwitchToCameraMode = {
                                    coroutineScope.launch {
                                        settingsRepository.updateAppMode(AppMode.CAMERA_TWIN)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PermissionRequiredScreen(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(containerColor = HardwareDark)
        ) {
            Text("Grant Camera Permission for Virtual 32")
        }
    }
}
