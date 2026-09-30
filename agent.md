# agent.md — Engineering Conventions for the Antigravity Agent

> Read this alongside `guide.md`. Split of responsibility:
> - `guide.md` = **what** to build (product spec, error-case table, session
>   resume/wrap-up protocol, status log).
> - `agent.md` (this file) = **how** to build it (stack, conventions, rules
>   the agent should follow on every task in this repo).

---

## Tech stack — ASSUMED, confirm or change before scaffolding

Nothing was locked in during planning, so this is a default, not a
decision. If it's wrong, correct this section first — everything below
depends on it.

- **Platform:** Native Android, Kotlin
- **UI:** Jetpack Compose (chosen over XML views — easier to hit the
  "cozy, dotted background" custom look from the spec without fighting
  a layout system)
- **Camera:** CameraX — supports silent single-frame JPEG capture without
  the shutter sound/animation that `MediaStore`/intent-based capture
  forces on you
- **Networking:** OkHttp (or Retrofit if the server side grows past a
  single endpoint) for the multipart JPEG POST
- **Vibration:** Android `Vibrator` / `VibratorManager` API for the 1s
  buzz on HTTP 422

## Repo conventions (fill in as scaffolding happens)
- Keep three concerns in separate packages: `camera/`, `network/`, `ui/`.
- All LED-state + vibration logic for a given HTTP response lives in
  **one place** (e.g. a `ResponseStatusHandler`), not scattered across
  UI callbacks — this is what makes it easy to add the extra error cases
  guide.md §2 calls for without hunting through the UI layer.
- Settings (server IP/port) persisted via `DataStore` (not raw
  `SharedPreferences`) — minor, but avoids a later migration.

## Hard rules — don't violate these even if it seems convenient
- **No shutter sound, no capture animation, no viewfinder freeze-frame
  flash.** The whole point is that this mimics a silent hardware
  trigger, not a camera app. If CameraX's default capture path adds any
  of that, suppress it — don't leave it "for now."
- **Keep Phone 1 and Phone 2 modular.** Phone 2 lives in `camera/` and `network/`.
  Phone 1 receiver logic lives in `receiver/` (`ReceiverHttpServer`, `GeminiVisionClient`, `TtsManager`).
  A single APK supports switching between both roles seamlessly.
- **Don't hardcode the OV3660 capture resolution.** Make it a tunable
  value (settings or a constants file) — real-hardware parity is a
  moving target until the actual sensor is in hand.
- When the final LED/vibration mapping for the extra error cases
  (timeout, no network, 5xx, malformed response) is implemented, **update
  the table in `guide.md` §2** with the final mapping — don't let it live
  only as code comments, or the next session starts blind.

## Files in this project
- `guide.md` — product spec + session protocol. Read first, always.
- `agent.md` — this file (engineering conventions & architecture).
- `.agent/skills/` — project-only skills (workspace scope).
- `app/src/main/java/com/antigravity/virtual32/`:
  - `camera/`:
    - `CameraCaptureManager.kt` — CameraX lifecycle controller, silent in-memory JPEG capture (no shutter sound, no flash, no capture freeze).
    - `SensorSpecs.kt` — OV3660 sensor specs, default 4:3 resolutions (UXGA 1600x1200 default, SVGA 800x600, VGA 640x480).
  - `network/`:
    - `NetworkResult.kt` — Sealed class hierarchy representing Success(200), ClientError(422), Timeout, NoNetwork, ServerError(5xx), MalformedResponse, and UnknownError.
    - `ImageUploader.kt` — OkHttp multipart/form-data POST (`image` field) with 5s connect and 10s read/write timeouts.
    - `ResponseStatusHandler.kt` — Single source of truth mapping network results to 6 distinct LED colors and Android `Vibrator`/`VibratorManager` haptic patterns.
  - `receiver/`:
    - `ReceiverHttpServer.kt` — Embedded coroutine-based HTTP server listening on port 5000/8080 at `/upload`.
    - `GeminiVisionClient.kt` — OkHttp client sending prompt + Base64 image to Google Gemini 1.5 Flash (`generateContent`).
    - `TtsManager.kt` — Android `TextToSpeech` manager streaming voice responses to Bluetooth earbuds / media audio.
  - `settings/`:
    - `AppSettings.kt` — Data class for server IP, port, resolution, quality, app mode, and Gemini API key.
    - `SettingsRepository.kt` — Persistent preferences via Jetpack DataStore Preferences.
  - `ui/`:
    - `components/`: `DottedBackground.kt`, `Ov3660Viewfinder.kt`, `TriggerButton.kt` (GPIO 1), `LedIndicator.kt` (GPIO 2).
    - `screens/`: `CameraScreen.kt` (Phone 2), `SettingsScreen.kt`, `ReceiverScreen.kt` (Phone 1), `WelcomeScreen.kt` (Opening Page / Role Wizard).
    - `theme/`: Design tokens, colors, typography.
  - `MainActivity.kt` — Main entry point, mode switcher (Camera Twin vs Earbud Brain), screen navigation.
- `app/src/test/java/com/antigravity/virtual32/network/ResponseStatusHandlerTest.kt` — Unit test suite verifying all 6 response feedback mappings.
- `app/src/test/java/com/antigravity/virtual32/network/OkHttpImageUploaderTest.kt` — MockWebServer tests verifying HTTP multipart POST and network error transitions.
- `app/src/test/java/com/antigravity/virtual32/camera/SensorSpecsTest.kt` — Unit test suite verifying OV3660 4:3 aspect ratios, resolution labels, and JPEG quality presets.
- `app/src/test/java/com/antigravity/virtual32/receiver/ReceiverHttpServerTest.kt` — Unit test suite verifying embedded HTTP server `/status` and `/upload` multipart handling.
- `app/src/test/java/com/antigravity/virtual32/receiver/GeminiVisionClientTest.kt` — Unit test suite verifying Gemini 1.5 Flash Vision client payload serialization, API key validation, and response parsing.
- `tools/`:
  - `mock_server.py` — Standalone Python HTTP mock server supporting dynamic mode switching (`cycle`, `200`, `422`, `500`, `malformed`, `timeout`), Web UI, and JPEG frame recording in `tools/captured_frames/`.
  - `test_mock_server.py` — Automated integration test suite verifying mock server responses.
  - `README.md` — Connection and test guide for testing Phone 2 via Wi-Fi/hotspot or ADB reverse port forwarding.

## Status & Progression
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
- **Phase 6 [NEXT]** — End-to-end dual-phone integration test on local Wi-Fi / hotspot.

