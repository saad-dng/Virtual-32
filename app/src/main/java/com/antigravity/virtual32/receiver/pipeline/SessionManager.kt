package com.antigravity.virtual32.receiver.pipeline

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class SessionPhotoEntry(
    val cachePath: String,
    val galleryUri: String?,
    val arrivalTimeMs: Long
)

data class SessionInfo(
    val open: Boolean,
    val pages: Int,
    val idleMs: Long
)

data class SessionUiState(
    val open: Boolean = false,
    val photos: List<SessionPhotoEntry> = emptyList(),
    val isAnalyzing: Boolean = false,
    val analyzingPhotoCount: Int = 0,
    val analyzingElapsedSec: Long = 0L,
    val autoSubmitRemainingSec: Int = -1
)

sealed class AddPhotoResult {
    data class Success(val pages: Int) : AddPhotoResult()
    data class SessionFull(val max: Int) : AddPhotoResult()
}

class SessionManager(
    private val context: Context,
    private val timeSource: () -> Long = { System.currentTimeMillis() },
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    private var onAutoSubmit: (suspend (List<SessionPhotoEntry>) -> Unit)? = null
) {
    private val mutex = Mutex()
    private val openPhotos = mutableListOf<SessionPhotoEntry>()
    private var autoSubmitJob: Job? = null
    private var countdownJob: Job? = null
    private var elapsedJob: Job? = null
    private var currentAutoSubmitSec: Int = 0

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    private val sessionFile: File by lazy {
        File(context.filesDir, "open_session.json")
    }

    private val json = Json { ignoreUnknownKeys = true }

    init {
        coroutineScope.launch(Dispatchers.IO) {
            mutex.withLock {
                loadPersistedSession()
                updateUiStateLocked()
            }
        }
    }

    fun setOnAutoSubmit(callback: suspend (List<SessionPhotoEntry>) -> Unit) {
        this.onAutoSubmit = callback
    }

    private fun loadPersistedSession() {
        try {
            if (sessionFile.exists()) {
                val text = sessionFile.readText()
                if (text.isNotBlank()) {
                    val list = json.decodeFromString<List<SessionPhotoEntry>>(text)
                    val existing = list.filter { File(it.cachePath).exists() }
                    openPhotos.addAll(existing)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveLocked() {
        try {
            val text = json.encodeToString(openPhotos)
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    sessionFile.writeText(text)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        updateUiStateLocked()
    }

    private fun updateUiStateLocked() {
        val current = _uiState.value
        _uiState.value = current.copy(
            open = openPhotos.isNotEmpty(),
            photos = ArrayList(openPhotos)
        )
    }

    suspend fun addPhoto(
        cachePath: String,
        galleryUri: String?,
        maxPages: Int,
        autoSubmitSec: Int = 0
    ): AddPhotoResult = mutex.withLock {
        if (openPhotos.size + 1 > maxPages) {
            return@withLock AddPhotoResult.SessionFull(maxPages)
        }

        val now = timeSource()
        openPhotos.add(SessionPhotoEntry(cachePath, galleryUri, now))
        saveLocked()
        scheduleAutoSubmitLocked(autoSubmitSec)
        return@withLock AddPhotoResult.Success(openPhotos.size)
    }

    suspend fun deletePhoto(index: Int): Boolean = mutex.withLock {
        if (index in openPhotos.indices) {
            openPhotos.removeAt(index)
            saveLocked()
            if (openPhotos.isEmpty()) {
                stopAutoSubmitLocked()
            } else {
                scheduleAutoSubmitLocked(currentAutoSubmitSec)
            }
            return@withLock true
        }
        return@withLock false
    }

    suspend fun movePhoto(fromIndex: Int, toIndex: Int): Boolean = mutex.withLock {
        if (fromIndex in openPhotos.indices && toIndex in openPhotos.indices && fromIndex != toIndex) {
            val item = openPhotos.removeAt(fromIndex)
            openPhotos.add(toIndex, item)
            saveLocked()
            return@withLock true
        }
        return@withLock false
    }

    suspend fun moveEarlier(index: Int): Boolean = movePhoto(index, index - 1)

    suspend fun moveLater(index: Int): Boolean = movePhoto(index, index + 1)

    suspend fun freezeSession(): List<SessionPhotoEntry> = mutex.withLock {
        stopAutoSubmitLocked()
        if (openPhotos.isEmpty()) return@withLock emptyList()

        val frozen = ArrayList(openPhotos)
        openPhotos.clear()
        saveLocked()
        return@withLock frozen
    }

    suspend fun cancelSession() = mutex.withLock {
        stopAutoSubmitLocked()
        openPhotos.clear()
        saveLocked()
    }

    suspend fun analyzeNow(): Boolean {
        val photos = freezeSession()
        if (photos.isEmpty()) return false

        startAnalyzing(photos.size)
        coroutineScope.launch {
            try {
                onAutoSubmit?.invoke(photos)
            } finally {
                stopAnalyzing()
            }
        }
        return true
    }

    fun startAnalyzing(photoCount: Int) {
        elapsedJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isAnalyzing = true,
            analyzingPhotoCount = photoCount,
            analyzingElapsedSec = 0L
        )
        elapsedJob = coroutineScope.launch {
            val startTime = timeSource()
            while (isActive) {
                delay(1000L)
                val elapsed = (timeSource() - startTime) / 1000L
                _uiState.value = _uiState.value.copy(analyzingElapsedSec = elapsed)
            }
        }
    }

    fun stopAnalyzing() {
        elapsedJob?.cancel()
        elapsedJob = null
        _uiState.value = _uiState.value.copy(
            isAnalyzing = false,
            analyzingPhotoCount = 0,
            analyzingElapsedSec = 0L
        )
    }

    suspend fun getSessionInfo(): SessionInfo = mutex.withLock {
        val isOpen = openPhotos.isNotEmpty()
        val pages = openPhotos.size
        val idleMs = if (isOpen) {
            maxOf(0L, timeSource() - openPhotos.last().arrivalTimeMs)
        } else {
            0L
        }
        return@withLock SessionInfo(open = isOpen, pages = pages, idleMs = idleMs)
    }

    fun getSessionInfoBlocking(): SessionInfo {
        return if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper() || mutex.isLocked) {
            val state = _uiState.value
            SessionInfo(open = state.open, pages = state.photos.size, idleMs = 0L)
        } else {
            runBlocking(Dispatchers.IO) {
                getSessionInfo()
            }
        }
    }

    fun getAutoSubmitRemainingSec(nowMs: Long = timeSource(), autoSubmitSec: Int = currentAutoSubmitSec): Int {
        if (autoSubmitSec <= 0 || openPhotos.isEmpty()) return -1
        val elapsedMs = nowMs - openPhotos.last().arrivalTimeMs
        val remainingMs = (autoSubmitSec * 1000L) - elapsedMs
        return maxOf(0, (remainingMs / 1000L).toInt())
    }

    private fun stopAutoSubmitLocked() {
        autoSubmitJob?.cancel()
        autoSubmitJob = null
        countdownJob?.cancel()
        countdownJob = null
        _uiState.value = _uiState.value.copy(autoSubmitRemainingSec = -1)
    }

    private fun scheduleAutoSubmitLocked(autoSubmitSec: Int) {
        currentAutoSubmitSec = autoSubmitSec
        stopAutoSubmitLocked()
        if (autoSubmitSec <= 0 || openPhotos.isEmpty()) return

        _uiState.value = _uiState.value.copy(autoSubmitRemainingSec = autoSubmitSec)

        countdownJob = coroutineScope.launch {
            while (isActive) {
                delay(1000L)
                val remaining = getAutoSubmitRemainingSec(timeSource(), autoSubmitSec)
                _uiState.value = _uiState.value.copy(autoSubmitRemainingSec = remaining)
                if (remaining <= 0) break
            }
        }

        autoSubmitJob = coroutineScope.launch {
            delay(autoSubmitSec * 1000L)
            triggerAutoSubmit()
        }
    }

    suspend fun triggerAutoSubmit() {
        val photos = freezeSession()
        if (photos.isNotEmpty()) {
            startAnalyzing(photos.size)
            try {
                onAutoSubmit?.invoke(photos)
            } finally {
                stopAnalyzing()
            }
        }
    }

    suspend fun checkAutoSubmit(autoSubmitSec: Int, nowMs: Long = timeSource()): Boolean {
        val shouldSubmit = mutex.withLock {
            if (autoSubmitSec <= 0 || openPhotos.isEmpty()) {
                false
            } else {
                nowMs - openPhotos.last().arrivalTimeMs >= autoSubmitSec * 1000L
            }
        }
        if (shouldSubmit) {
            triggerAutoSubmit()
            return true
        }
        return false
    }

    companion object {
        @Volatile
        private var INSTANCE: SessionManager? = null

        fun getInstance(context: Context): SessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun setInstanceForTesting(manager: SessionManager?) {
            INSTANCE = manager
        }
    }
}
