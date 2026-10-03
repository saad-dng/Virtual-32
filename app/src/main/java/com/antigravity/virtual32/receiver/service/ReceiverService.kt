package com.antigravity.virtual32.receiver.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.antigravity.virtual32.MainActivity
import com.antigravity.virtual32.R
import com.antigravity.virtual32.data.AppDatabase
import com.antigravity.virtual32.data.RoomAnswerStore
import com.antigravity.virtual32.receiver.pipeline.PhotoPipelineImpl
import com.antigravity.virtual32.receiver.server.LogBuffer
import com.antigravity.virtual32.receiver.server.ReceiverHttpServer
import com.antigravity.virtual32.settings.SettingsRepository
import com.antigravity.virtual32.settings.PromptRepository
import com.antigravity.virtual32.util.GalleryWriter
import com.antigravity.virtual32.util.IpDiscovery
import com.antigravity.virtual32.util.PhotoCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class ReceiverService : Service() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    
    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    
    private lateinit var answerStore: RoomAnswerStore
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var promptRepo: PromptRepository
    private lateinit var pipeline: PhotoPipelineImpl
    private lateinit var galleryWriter: GalleryWriter
    private lateinit var photoCache: PhotoCache
    private lateinit var sessionManager: com.antigravity.virtual32.receiver.pipeline.SessionManager
    private var server: ReceiverHttpServer? = null
    private val logBuffer = LogBuffer()
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
        
    private val CHANNEL_ID = "receiver_channel"
    private val NOTIFICATION_ID = 32
    
    private var watchdogJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val dependenciesReady = CompletableDeferred<Unit>()

    override fun onCreate() {
        super.onCreate()
        
        ReceiverState.update { it.copy(isServiceRunning = true) }
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification(), if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)

        acquireLocks()
        startNetworkListener()
        startWatchdog()
        
        // Initial setup and bind asynchronously on Dispatchers.IO
        scope.launch(Dispatchers.IO) {
            setupDependencies()
            dependenciesReady.complete(Unit)

            val port = settingsRepo.getSettings().serverPort
            ReceiverState.update { it.copy(serverPort = port) }
            val ip = IpDiscovery.getBestIpAddress()
            ReceiverState.update { it.copy(currentIp = ip) }
            startServer(port)
            
            // Run self test on startup
            val runner = SelfTestRunner(this@ReceiverService)
            val results = runner.runTests(port, wakeLock, wifiLock)
            val failed = results.filter { !it.passed }
            if (failed.isNotEmpty()) {
                logBuffer.addAppLog("E", "SelfTest", "Failed: ${failed.joinToString { it.name }}")
            } else {
                logBuffer.addAppLog("I", "SelfTest", "All tests passed")
            }

            scope.launch {
                pipeline.state.collect { state ->
                    ReceiverState.updatePipeline(state)
                }
            }
            
            scope.launch {
                // Keep notification updated with answers count
                answerStore.activeAnswers().collect { list ->
                    ReceiverState.update { it.copy(answersCount = list.size) }
                    updateNotification()
                }
            }

            scope.launch {
                sessionManager.uiState.collect {
                    updateNotification()
                }
            }
        }
        
        scope.launch {
            while (isActive) {
                delay(5000)
                val lastTime = logBuffer.lastActivityMs
                ReceiverState.update { it.copy(lastEspSeenMs = lastTime) }
                ReceiverState.updateLogs(logBuffer.getLogs())
                updateNotification()
            }
        }
    }

    private suspend fun setupDependencies() = withContext(Dispatchers.IO) {
        val db = AppDatabase.initDatabase(this@ReceiverService)
        answerStore = RoomAnswerStore(db.answerDao())
        settingsRepo = SettingsRepository(this@ReceiverService)
        promptRepo = PromptRepository(this@ReceiverService)
        galleryWriter = GalleryWriter(this@ReceiverService)
        photoCache = PhotoCache(this@ReceiverService)

        pipeline = PhotoPipelineImpl(
            context = this@ReceiverService,
            settingsRepo = settingsRepo,
            promptRepo = promptRepo,
            answerStore = answerStore,
            okHttpClient = okHttpClient,
            galleryWriter = galleryWriter,
            photoCache = photoCache,
            isNetworkAvailable = { ReceiverState.health.value.isNetworkUp }
        )

        sessionManager = com.antigravity.virtual32.receiver.pipeline.SessionManager.getInstance(this@ReceiverService).apply {
            setOnAutoSubmit { photos ->
                val jpegs = photos.map { java.io.File(it.cachePath).readBytes() }
                val paths = photos.map { it.cachePath }
                val uris = photos.mapNotNull { it.galleryUri }
                pipeline.processPhotos(jpegs, "ESP", paths, uris)
            }
        }
    }

    private fun acquireLocks() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Virtual32::ReceiverWakeLock").apply {
                acquire()
            }
            
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val lockType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                WifiManager.WIFI_MODE_FULL_LOW_LATENCY
            } else {
                WifiManager.WIFI_MODE_FULL_HIGH_PERF
            }
            wifiLock = wifiManager.createWifiLock(lockType, "Virtual32::ReceiverWifiLock").apply {
                acquire()
            }
            ReceiverState.update { it.copy(hasWifiLock = true) }
        } catch (e: Exception) {
            e.printStackTrace()
            ReceiverState.update { it.copy(hasWifiLock = false) }
        }
    }

    private fun releaseLocks() {
        try {
            wakeLock?.takeIf { it.isHeld }?.release()
            wifiLock?.takeIf { it.isHeld }?.release()
            ReceiverState.update { it.copy(hasWifiLock = false) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startServer(port: Int) {
        server?.stop()
        server = ReceiverHttpServer(
            port = port,
            pipeline = pipeline,
            answerStore = answerStore,
            logBuffer = logBuffer,
            sessionManager = sessionManager,
            galleryWriter = galleryWriter,
            photoCache = photoCache,
            settingsRepo = settingsRepo
        ).apply {
            start()
        }
    }

    private fun startWatchdog() {
        watchdogJob = scope.launch {
            while (isActive) {
                delay(10000)
                try {
                    val req = Request.Builder().url("http://127.0.0.1:${ReceiverState.health.value.serverPort}/ping").get().build()
                    val response = okHttpClient.newCall(req).execute()
                    val success = response.isSuccessful
                    response.close()
                    ReceiverState.update { it.copy(isServerResponding = success) }
                    
                    if (!success) {
                        startServer(ReceiverState.health.value.serverPort)
                    }
                } catch (e: Exception) {
                    ReceiverState.update { it.copy(isServerResponding = false) }
                    // Rebind
                    startServer(ReceiverState.health.value.serverPort)
                }
                
                // Also update logs periodically
                ReceiverState.updateLogs(logBuffer.getLogs())
                
                updateNotification()
            }
        }
    }

    private fun startNetworkListener() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                ReceiverState.update { it.copy(isNetworkUp = true) }
                recomputeIp()
            }

            override fun onLost(network: Network) {
                ReceiverState.update { it.copy(isNetworkUp = false) }
                recomputeIp()
            }
        }
        cm.registerNetworkCallback(request, networkCallback!!)
        
        // 5s polling for hotspot/IP changes that ConnectivityManager might miss
        scope.launch {
            while (isActive) {
                delay(5000)
                recomputeIp()
            }
        }
    }
    
    private fun recomputeIp() {
        val ip = IpDiscovery.getBestIpAddress()
        if (ip != ReceiverState.health.value.currentIp) {
            ReceiverState.update { it.copy(currentIp = ip) }
            startServer(ReceiverState.health.value.serverPort)
            updateNotification()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Receiver Service", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Keeps the Virtual 32 receiver server alive"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val state = ReceiverState.health.value
        val ip = state.currentIp ?: "No IP"
        val port = state.serverPort
        val answers = state.answersCount
        val espTime = if (state.lastEspSeenMs > 0) "${(System.currentTimeMillis() - state.lastEspSeenMs) / 1000}s ago" else "Never"
        val waitingPhotos = if (::sessionManager.isInitialized) sessionManager.uiState.value.photos.size else 0
        val waitingPrefix = if (waitingPhotos > 0) "$waitingPhotos photos waiting - " else ""
        
        val contentText = "${waitingPrefix}Running - $ip:$port - $answers answers - ESP last seen $espTime"
        
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val nextIntent = PendingIntent.getBroadcast(this, 1, Intent(this, NotificationActionReceiver::class.java).apply { action = "NEXT" }, PendingIntent.FLAG_IMMUTABLE)
        val repeatIntent = PendingIntent.getBroadcast(this, 2, Intent(this, NotificationActionReceiver::class.java).apply { action = "REPEAT" }, PendingIntent.FLAG_IMMUTABLE)
        val stopIntent = PendingIntent.getBroadcast(this, 3, Intent(this, NotificationActionReceiver::class.java).apply { action = "STOP" }, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Virtual 32 Receiver")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentIntent(pendingIntent)
            .addAction(0, "Next", nextIntent)
            .addAction(0, "Repeat", repeatIntent)
            .addAction(0, "Stop", stopIntent)
            .setOngoing(true)
            .build()
    }
    
    private fun updateNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_PROCESS_GALLERY) {
            val uriStr = intent.getStringExtra(EXTRA_URI)
            if (uriStr != null) {
                scope.launch {
                    try {
                        dependenciesReady.await()
                        val uri = android.net.Uri.parse(uriStr)
                        contentResolver.openInputStream(uri)?.use { stream ->
                            val bytes = stream.readBytes()
                            pipeline.processPhoto(bytes, "GALLERY")
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } else if (intent?.action == ACTION_REPROCESS) {
            val photoPaths = intent.getStringArrayListExtra(EXTRA_PHOTO_PATHS)
                ?: intent.getStringExtra(EXTRA_PHOTO_PATH)?.let { arrayListOf(it) }
            val batchId = intent.getLongExtra(EXTRA_BATCH_ID, -1L)
            if (!photoPaths.isNullOrEmpty() && batchId != -1L) {
                scope.launch {
                    try {
                        dependenciesReady.await()
                        val db = AppDatabase.getDatabase(this@ReceiverService)
                        db.answerDao().markBatchesSuperseded(listOf(batchId))
                        val jpegs = photoPaths.map { java.io.File(it).readBytes() }
                        pipeline.processPhotos(jpegs, "GALLERY")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        ReceiverState.update { it.copy(isServiceRunning = false, isServerResponding = false, hasWifiLock = false) }
        scope.cancel()
        server?.stop()
        releaseLocks()
        networkCallback?.let {
            val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            cm.unregisterNetworkCallback(it)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    
    companion object {
        private const val CHANNEL_ID = "ReceiverServiceChannel"
        private const val NOTIFICATION_ID = 1
        
        const val ACTION_PROCESS_GALLERY = "com.antigravity.virtual32.PROCESS_GALLERY"
        const val ACTION_REPROCESS = "com.antigravity.virtual32.REPROCESS"
        const val EXTRA_URI = "extra_uri"
        const val EXTRA_PHOTO_PATH = "extra_photo_path"
        const val EXTRA_PHOTO_PATHS = "extra_photo_paths"
        const val EXTRA_BATCH_ID = "extra_batch_id"
    }
}
