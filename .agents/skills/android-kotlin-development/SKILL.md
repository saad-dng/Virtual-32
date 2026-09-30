---
name: android-kotlin-development
description: Expert guidance on modern Android development with Kotlin, Clean Architecture, Hilt dependency injection, Coroutines, StateFlow, and DataStore settings persistence. Use when structuring Android packages, implementing ViewModels, managing lifecycles, or setting up project architecture.
metadata:
  author: Anti-Gravity Engineering
  version: "1.0"
  keywords:
    - android
    - kotlin
    - architecture
    - coroutines
    - hilt
    - datastore
    - vibrator
---

# Android Kotlin Development & Architecture

Guidance for building production-grade Android applications with Kotlin, focusing on clean separation of concerns, reactive state management, and modern hardware-software integration.

## Architectural Principles

### 1. Package Separation
Separate concerns cleanly into distinct, dedicated packages:
- `camera/`: CameraX lifecycle binding, capture use cases, sensor configuration.
- `network/`: HTTP clients, multipart requests, response status mapping.
- `ui/`: Compose screens, custom styling, view models, UI state models.
- `data/` or `settings/`: DataStore persistence for configuration (server IP/port, resolution).

Dependencies strictly flow from UI -> ViewModel -> Use Case / Repository -> Hardware / Network layer.

### 2. State & Concurrency
- Use Kotlin **Coroutines** and **StateFlow** for state delivery to UI.
- All repository and network methods must be main-safe `suspend` functions using appropriate dispatchers (`Dispatchers.IO` for disk/network).
- Use `viewModelScope` to launch coroutines that produce UI state, avoiding memory leaks on configuration changes.

```kotlin
class AppViewModel(
    private val captureRepository: CaptureRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
}
```

### 3. Settings Persistence with Jetpack DataStore
Per project conventions, do **not** use raw `SharedPreferences`. Use **Preferences DataStore**:
- Define keys using `stringPreferencesKey`, `intPreferencesKey`.
- Expose values as `Flow<T>`.
- Provide atomic updates via `dataStore.edit { preferences -> ... }`.

```kotlin
val SERVER_IP_KEY = stringPreferencesKey("server_ip")
val SERVER_PORT_KEY = intPreferencesKey("server_port")
val CAPTURE_RESOLUTION_KEY = stringPreferencesKey("capture_resolution")
```

### 4. Vibration Feedback API
For hardware status feedback (e.g., 1-second vibration on HTTP 422 error), use the modern `VibratorManager` on Android 12+ (API 31+) with backward compatibility to `Vibrator`:

```kotlin
fun triggerVibration(context: Context, durationMillis: Long) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val vibrator = vibratorManager.defaultVibrator
        vibrator.vibrate(VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE))
    }
}
```

### 5. Code Quality Checklist
- [ ] No UI logic in data or network layers.
- [ ] Centralize hardware status handling (LED / vibration) in a dedicated handler.
- [ ] Ensure all background operations use structured concurrency with Coroutines.
- [ ] Unit test repositories with fakes rather than heavy mocks.
