# agent.md — Engineering Conventions for the Antigravity Agent

> Read this alongside `guide.md`. Split of responsibility:
>
> - `guide.md` = **what** to build (product spec, error-case table, session
>   resume/wrap-up protocol, status log).
> - `agent.md` (this file) = **how** to build it (stack, conventions, rules
>   the agent should follow on every task in this repo).

---

## Tech stack (locked for this direction)

- Native Android, Kotlin, Jetpack Compose, Material 3, cozy dotted theme (keep).
- CameraX (Simulator only), OkHttp (AI calls + simulator upload), kotlinx-serialization-json (replaces org.json), Coroutines/Flow, Room (KSP) for answers/history, DataStore for settings.
- Embedded HTTP server: keep our own coroutine socket server in receiver/server/ (no extra web framework).
- Foreground service type: specialUse (NOT dataSync — Android 15 caps dataSync at ~6 h).
- minSdk stays as is; target latest stable SDK.

## Package layout (app/src/main/java/com/antigravity/virtual32/)

- receiver/server/ (ReceiverHttpServer, routes, ServerState, LogBuffer)
- receiver/ai/ (VisionProvider, GeminiProvider, ClaudeProvider, PromptBuilder, AiResponseParser)
- receiver/pipeline/ (PhotoPipeline queue/worker, AnswerMode)
- receiver/service/ (ReceiverService, watchdog, locks, notification, QS tile)
- data/ (Room DB, entities, DAOs, AnswerStore, GalleryWriter, PhotoCache)
- simulator/ (SimulatorScreen, BlinkPatterns, BlinkEngine, DoubleTapDetector, LongPressDetector, SimClient)
- settings/ (AppSettings, SettingsRepository, backup)
- ui/ (components, screens: Home, Answers, History, Simulator, Settings, PromptEditor, Diagnostics, SetupChecklist; theme)
- util/ (IpDiscovery, Oem helpers, TimeSource)
- tools/ (esp32_client_sim.py) and docs/ (ESP32_CONTRACT.md)

## Hard rules

- No TTS, no Bluetooth audio, no sound of any kind anywhere in the app.
- The protocol in guide.md §3 and blink language in §4 are a contract. Any change must update guide.md, docs/ESP32_CONTRACT.md and BlinkPatterns.kt in the same task.
- Blink timings exist only in BlinkPatterns.kt. No magic numbers elsewhere.
- Never block the server thread on an AI call; the pipeline is a queue with one worker.
- All AI output passes through AiResponseParser; nothing else parses model text.
- Save every incoming photo to the gallery BEFORE calling the AI.
- Never log or display full API keys (show last 4 chars only).
- The service must not depend on the Activity being alive; UI only observes shared StateFlows.
- No decorative-only UI. Every element must show state or do something.
- Silent capture in the Simulator (no shutter sound/animation).

## Definition of Done (applies to EVERY task)

1. Add/update unit tests for new logic; run the unit test task; all must pass.
2. Assemble the debug APK successfully.
3. Update guide.md: mark the phase in the Roadmap and append a Status Log entry at the TOP (date, done, decisions, Next).
4. Update the "Files in this project" inventory below to match the real repo (add new files, remove deleted ones).
5. If a requirement conflicts with reality (API changed, library missing), stop and ask ONE short question instead of guessing.
6. Final reply: max 5 lines: what changed, what to test manually. No spec recap.

## Files in this project

- `guide.md` — Product spec, protocol contract, session protocol, and status log.
- `agent.md` — Engineering conventions, architecture rules, and file inventory.
- `USER_GUIDE.md` — Setup instructions and LED cheat sheet.
- `build.gradle.kts` (root and app) — KSP, Room, and serialization configured.
- `app/src/main/AndroidManifest.xml` — Declares permissions, Foreground Service (specialUse), and Quick Settings Tile.
- `app/src/main/java/com/antigravity/virtual32/`:
  - `Virtual32App.kt` — Application class with StrictMode and startup timestamp tracking.
  - `MainActivity.kt` — App entry point, time-to-first-frame telemetry, and bottom navigation.
  - `data/` — Room DB (`AppDatabase`), DAOs (`AnswerDao`), `AnswerStore`, `RoomAnswerStore`, `GalleryWriter`, `PhotoCache`, `Entities.kt`.
  - `receiver/`:
    - `ai/` — `VisionProvider`, `GeminiProvider`, `ClaudeProvider`, `AiResponseParser`, `PromptBuilder`, `PromptRepository`.
    - `pipeline/` — `PhotoPipelineImpl.kt` (Queue worker), `SessionManager.kt` (Multi-photo session state), `AnswerMode.kt`.
    - `server/` — `ReceiverHttpServer.kt` (Coroutine HTTP server), `LogBuffer.kt`.
    - `service/` — `ReceiverService.kt` (Watchdog, locks), `ReceiverState.kt`, `SelfTestRunner.kt`, `Virtual32TileService.kt`.
  - `settings/` — `AppSettings.kt`, `SettingsRepository.kt`.
  - `simulator/` — `BlinkPatterns.kt`, `BlinkEngine.kt`, `DoubleTapDetector.kt`, `LongPressDetector.kt`, `SimClient.kt`.
  - `ui/`:
    - `components/` — `DottedBackgroundBox.kt`.
    - `screens/` — `HomeScreen`, `HomeViewModel`, `AnswersScreen`, `AnswersViewModel`, `SimulatorScreen`, `SimulatorViewModel`, `SettingsScreen`, `SettingsViewModel`, `AiSettingsScreen`, `DiagnosticsScreen`, `HistoryScreen`, `HistoryViewModel`.
    - `theme/` — `Color.kt`, `Theme.kt`, `Type.kt`.
  - `util/` — `IpDiscovery.kt`, `ImageResizer.kt`, `ThumbnailCache.kt` (Background downscaling and memory cache).
- `app/src/test/java/com/antigravity/virtual32/`:
  - `receiver/` — Tests for `ReceiverHttpServer`, `SessionFlowSocketTest`, `PhotoPipeline`, `VisionProviders`, `MultiImageProviderRequestShapeTest`, `AiResponseParserTest`.
  - `simulator/` — Tests for `BlinkPatterns`, `DoubleTapDetector`, `LongPressDetectorTest`.
  - `ui/` — `HomeViewModelSessionTest`, `AnswersViewModelWarningsTest`, `StartupRegressionTest`.
  - `settings/` — `SessionsSettingsTest`.
  - `data/` — Tests for `RoomAnswerStore`, `GalleryWriter`, `MigrationTest`.
- `tools/esp32_client_sim.py` — Python client simulator for firmware dev testing (supports single upload and multi-photo sessions).
- `docs/ESP32_CONTRACT.md` — Complete HTTP and Blink language contract for firmware.
- `docs/anr-trace.txt` — Main-thread crash and ANR trace logs.

---

## Legacy phases (done)

- **Phase 1 [COMPLETED]** — Scaffolding & silent capture:
  - Clean Architecture packages (`camera/`, `network/`, `settings/`, `ui/`).
  - CameraX silent single-frame JPEG capture matching OV3660 framing (zero shutter sound, zero capture flash).
  - Persisted settings (server IP, port, resolution) via DataStore.
  - Cozy dotted aesthetic, custom trigger button (GPIO 1), and LED indicator (GPIO 2).
- **Phase 2 [COMPLETED]** — Networking & ResponseStatusHandler:
  - Wired trigger to real OkHttp multipart POST sending JPEG payload to `/upload`.
  - Implemented centralized `ResponseStatusHandler` with 6 distinct LED colors and vibration patterns.
  - Unit tests in `ResponseStatusHandlerTest` verified and passing (`BUILD SUCCESSFUL`).
  - Production debug APK assembled and verified at `app/build/outputs/apk/debug/app-debug.apk`.
- **Phase 3 [COMPLETED]** — Standalone test pass:
  - Built standalone mock server in `tools/mock_server.py` with dynamic mode switching (`cycle`, `200`, `422`, `500`, `malformed`, `timeout`) and frame recording.
  - Built automated Python test suite `tools/test_mock_server.py` verifying all response conditions.
  - Added OkHttp MockWebServer tests in `OkHttpImageUploaderTest.kt` (all 11 unit tests passing).
- **Phase 4 [COMPLETED]** — Polish & real-world parity:
  - Real-world ESP32-S3-CAM DMA buffer constraints modeled: added `JpegQualityPreset` (Fast 65%, Balanced 80% default, Fine 92%).
  - Added resolution transfer payload badges (`~35 KB` to `~350 KB`) in SettingsScreen.
  - Added high-intensity diode breathing glow animation on active `LedIndicator` with manual tap-to-reset.
  - Polished `CameraScreen` hardware telemetry strip and multi-device responsive layout.
  - Added `SensorSpecsTest.kt` (all 15 unit tests across 3 suites passing; `BUILD SUCCESSFUL`).
- **Phase 5 [COMPLETED]** — Phone 1 Receiver & Earbud Brain:
  - Built native embedded coroutine HTTP server (`ReceiverHttpServer`) on `Dispatchers.IO` listening on `/upload` with local IP auto-discovery.
  - Built `GeminiVisionClient` integration with Google Gemini 1.5 Flash Vision API (`generateContent`) using configurable prompts and API key.
  - Built `TtsManager` with Android `TextToSpeech` engine configured with `USAGE_MEDIA` and `CONTENT_TYPE_SPEECH` to stream speech directly into Bluetooth earbuds.
  - Built `ReceiverScreen` dashboard with live server toggle, endpoint address indicator, Gemini prompt/key setup, received JPEG frame preview, scene description readout, and "Replay in Earbuds" button.
  - Updated `MainActivity` and `SettingsScreen` with instant operating mode switching between Phone 2 (Camera Twin) and Phone 1 (Earbud Brain).
  - Created unit tests in `ReceiverHttpServerTest.kt` and `GeminiVisionClientTest.kt`. All 21 JVM unit tests passing (`BUILD SUCCESSFUL`), and debug APK assembled cleanly.
