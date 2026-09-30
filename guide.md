# Anti-Gravity Build — Agent Guide

> **Read this file first, every session.** It is the single source of truth for
> this project. Do not re-derive the plan from scratch or ask the user to
> re-explain it — everything needed to resume work is below.

---

## 0. Session Protocol (read this part first)

**On session start / when the user says "resume", "continue", or opens this
project:**
1. Read the `## Status Log` section at the bottom — that's the current state.
2. Pick up at the "Next" item of the most recent entry.
3. Don't re-summarize the whole spec back to the user — just say what you're
   about to do and do it. Keep replies short; token budget matters.

**On "wrap up for today":**
1. Stop new feature work.
2. Add a new entry at the **top** of the Status Log with: date, what was
   completed this session, any decisions made or changed, and a concrete
   "Next" line.
3. Do **not** rewrite earlier log entries — append only, most recent first.
4. Confirm to the user in one line that the log is updated. Nothing else.

This file is the only thing that needs to persist context between sessions —
don't duplicate the spec elsewhere or ask the user to repeat it.

---

## 1. Project Overview

**Anti-Gravity** is a unified dual-mode Android app providing the complete end-to-end pipeline on two phones before any real soldering happens:

- **Phone 2 Mode (ESP32-S3 Camera Twin):** Simulates the ESP32-S3-CAM + OV3660 hardware rig: camera framing, silent trigger, GPIO 2 status LED, and multipart image upload.
- **Phone 1 Mode (Receiver & Earbud Brain):** Acts as the receiver hub: runs an embedded local HTTP server (`/upload` on port 5000/8080), calls the Google Gemini Vision API to analyze incoming images, and streams spoken answers via Android Text-to-Speech (TTS) directly into Bluetooth earbuds.

A single APK installs on both devices, with an instant toggle between **Camera Twin (Phone 2)** and **Earbud Brain (Phone 1)**.

---

## 2. Phone 2 App Spec

### UI
- Camera viewfinder — mimics the real OV3660 sensor's framing/behavior (see
  §3 for sensor specs to match).
- Virtual trigger button → stands in for **GPIO 1** on real hardware.
- Virtual LED status indicator → stands in for **GPIO 2** on real hardware.
- Settings screen: server IP (and port) for Phone 1's Termux server.
- Visual style: cozy, dotted background, per the reference screenshot the
  user shared during planning.

### Core workflow
1. User taps the trigger.
2. App **silently** captures a JPEG frame — no shutter sound, no capture
   animation (this must feel like a silent hardware trigger, not a camera
   app photo).
3. App POSTs the JPEG to Phone 1's Termux server at the configured IP, over
   the local hotspot/LAN.
4. Response handling (LED = GPIO 2 stand-in):
   | Response | LED state | Hex Color | Vibration |
   |---|---|---|---|
   | HTTP 200 (Success) | Gray (idle) | `#9E9C96` | None |
   | HTTP 422 (Validation) | Red | `#D32F2F` | 1 second continuous buzz (`longArrayOf(0, 1000)`) |
   | Timeout / Unreachable | Amber | `#FFB300` | 2 short pulses (`longArrayOf(0, 200, 100, 200)`) |
   | No Network / Hotspot Down | Blue | `#1E88E5` | 3 rapid pulses (`longArrayOf(0, 100, 100, 100, 100, 100)`) |
   | HTTP 5xx (Server Error) | Purple | `#8E24AA` | 1 long + 1 short buzz (`longArrayOf(0, 500, 150, 200)`) |
   | Malformed / Unexpected Body | Orange | `#FB8C00` | 1 medium pulse (`longArrayOf(0, 400)`) |

### Finalized Error Cases & Mappings
The extra error handling cases beyond 200/422 have been finalized and verified in `ResponseStatusHandler.kt`:
- **Request timeout / server unreachable:** Amber LED (`#FFB300`), 2 short buzzes.
- **No network / hotspot disconnected:** Blue LED (`#1E88E5`), 3 rapid pulses.
- **HTTP 5xx server failure:** Purple LED (`#8E24AA`), 1 long + 1 short buzz.
- **Malformed or unexpected response body:** Orange LED (`#FB8C00`), 1 medium pulse.
Each status is handled through `ResponseStatusHandler` and verified with unit tests (`ResponseStatusHandlerTest`).

### Image capture quality — match real OV3660 sensor
Real OV3660 hardware spec (for parity, not necessarily to run at max):
- Max resolution: 2048×1536 (3MP, "QXGA")
- Output format: JPEG (also supports raw/YUV/RGB565, but JPEG is what
  this pipeline uses)
- Diagonal field of view: ~65–68°
Default the app's capture to a resolution/quality that a real ESP32-S3-CAM
would realistically stream at (not full 3MP — that sensor is typically run
down-scaled for speed), and make this tunable so it can be matched exactly
once real hardware is in hand.

### Upload approach
Multipart/form-data POST of the JPEG to the configured server IP — this is
the standard, least-surprising approach and matches what a Flask server on
the other end will expect with minimal glue code.

---

## 3. Architecture & Unified Scope
- **Phone 2 (Digital Twin Camera):** Built natively with CameraX, silent capture, OV3660 framing, GPIO 1 trigger, GPIO 2 status LED, OkHttp multipart POST.
- **Phone 1 (Earbud Brain / Receiver):** Built natively into this same app with an embedded HTTP server listening on `/upload`, Google Gemini Vision API client, and Android TextToSpeech streaming to connected Bluetooth earbuds.
- **Out of scope:** Dedicated Termux scripts (replaced by native Android embedded receiver).

---

## 4. Antigravity IDE Workflow (how this project gets built)
Standard workflow for this user's app builds, applied here:
1. Plan the app idea with Claude first (done — this file is the output).
2. In the project folder, open in Google Antigravity (Gemini-based agentic
   IDE).
3. Import relevant skills files.
4. `agent.md` — a separate engineering-conventions file for the coding
   agent (tech stack, coding rules, file layout). `guide.md` (this file)
   stays the product-spec + session-protocol file; `agent.md` stays the
   "how to write code here" file. Read both at session start.
5. Build a prompt library: a set of prompts covering the build end-to-end,
   from sketch to final polish.
Keep all of this efficient — avoid burning tokens re-explaining the spec;
that's what this file is for.

---

## Roadmap
*(where any given task sits in the bigger picture — update phase status as
each completes, don't delete finished phases, they're useful history)*

1. **Phone 2 scaffolding** [DONE] — package structure (`camera/`, `network/`, `settings/`, `ui/`), camera capture screen with OV3660 framing, settings screen with DataStore persistence, silent JPEG capture via CameraX.
2. **Phone 2 networking** [DONE] — wired trigger to real OkHttp multipart POST; implemented centralized `ResponseStatusHandler` with finalized LED and vibration feedback for all 6 response states; verified via unit tests (`ResponseStatusHandlerTest`) and APK assemble.
3. **Standalone test pass** [DONE] — built standalone local mock server (`tools/mock_server.py`) supporting dynamic mode switching (`cycle`, `200`, `422`, `500`, `malformed`, `timeout`) and saving captured frames; automated Python test runner (`tools/test_mock_server.py`); MockWebServer unit tests in Android (`OkHttpImageUploaderTest`).
4. **Polish & real-world parity** [DONE] — tuned OV3660 capture resolution presets with hardware DMA payload estimates, added tunable JPEG compression quality (`JpegQualityPreset`), animated diode pulse/glow on active LED indicator, hardware telemetry strip on main camera view, multi-device layout responsiveness, verified 15 unit tests and clean APK assembly.
5. **Phone 1 Receiver & Earbud Brain** [DONE] — embedded HTTP server (`/upload` on port 5000/8080), Gemini 1.5 Flash Vision API integration, Android Text-to-Speech (TTS) routed to Bluetooth earbuds, and live reception dashboard.
6. **End-to-end integration test** — both phones live on one hotspot,
   full loop, deliberately break things to confirm error paths hold up
   outside the mock.
7. **Real hardware** — port trigger/capture/upload logic to actual
   ESP32-S3 + OV3660 firmware; GPIO 1/2 become real pins.

**Current phase: 6 (End-to-end integration test).**

---

## Status Log
*(most recent entry first — append, don't rewrite)*

- **2026-09-26** — App Identity, Theme-Matched Logo & Welcome Wizard (Phase 6 Polish):
  - **App Name Unified:** Formally set app name to **Virtual 32** in `strings.xml`, `AndroidManifest.xml`, screen headers, and guides.
  - **Custom Hardware-Matched Logo:** Generated custom brand icon matching the in-app hardware theme (warm cream `#F7F4EE`, obsidian `#22211F`, dial with '32', terracotta accent `#D35400`, emerald status diode `#388E3C`).
  - **Complete Launcher Icon Set:** Produced high-res PNG launcher and round launcher icons across `mipmap-mdpi`, `mipmap-hdpi`, `mipmap-xhdpi`, `mipmap-xxhdpi`, and `mipmap-xxxhdpi`.
  - **Opening Page & Role Wizard (`WelcomeScreen.kt`):** Created animated onboarding screen featuring glowing/pulsing app logo, tactile role cards ("Phone 2: The Eyes" vs "Phone 1: The Brain"), quick setup hints, and smooth slide/fade entrance animations.
  - **Settings Integration:** Added "Launch Device Setup Wizard & Logo" button to `SettingsScreen` to revisit the wizard anytime.
  - **Build Verification:** All 21 JVM unit tests passing (`BUILD SUCCESSFUL`), APK cleanly assembled at `app/build/outputs/apk/debug/app-debug.apk`.

- **2026-09-26** — Connectivity, Layout & Gestures Overhaul (Phase 6 Polish):
  - **Connectivity Root Cause Resolved:** Fixed strict Android 9+ cleartext blocking by enabling `android:usesCleartextTraffic="true"` and creating `@xml/network_security_config`. Added missing network state & Wi-Fi permissions (`ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_MULTICAST_STATE`, `WAKE_LOCK`).
  - **Socket & IP Prioritization:** Configured `ReceiverHttpServer` to bind explicitly to `0.0.0.0:$port` and prioritized Wi-Fi (`wlan`) and hotspot (`ap`) IPv4 addresses over mobile data/carrier NAT IPs (`rmnet`). Added background partial wake lock and Wi-Fi lock to keep sockets active when screen is off.
  - **Live Connection Tester:** Added an instant "Test Connection to Phone 1" button in `SettingsScreen` using `/status` GET ping with real-time green/red diagnostic readout.
  - **Status Bar & Notch Insets:** Applied `.statusBarsPadding()`, `.navigationBarsPadding()`, and `.imePadding()` across `CameraScreen`, `ReceiverScreen`, and `SettingsScreen` — completely eliminating overlap with the phone's information header (status bar clock/battery/cutout).
  - **Complete Gesture Implementation:** Added system back gesture support (`BackHandler`), edge swipe-to-back on `SettingsScreen`, and horizontal swipe gesture + interactive segmented top tab bar to switch between Phone 2 (Camera) and Phone 1 (Brain).
  - Updated user guideline at `C:\Projects\99-guideline\README.md` and `c:\Projects\Virtual 32\USER_GUIDE.md`. All unit tests verified passing (`BUILD SUCCESSFUL`).

- **2026-09-26** — Completed Phase 5 (Phone 1 Receiver & Earbud Brain):
  - Unified app architecture into a single installable APK operating in two distinct roles: Phone 2 (Camera Twin) and Phone 1 (Earbud Brain / Receiver), seamlessly switchable via the UI and persisted via Jetpack DataStore.
  - Implemented embedded coroutine HTTP server (`ReceiverHttpServer`) running on `Dispatchers.IO` with auto-discovered local Wi-Fi IP and resilient multipart JPEG payload extraction on `/upload`.
  - Built `GeminiVisionClient` communicating directly with Google Gemini 1.5 Flash Vision API (`generateContent`) for sub-second visual scene descriptions.
  - Integrated Android `TextToSpeech` engine (`TtsManager`) with audio attributes configured for media streaming (`USAGE_MEDIA`, `CONTENT_TYPE_SPEECH`) directly to connected Bluetooth earbuds.
  - Built `ReceiverScreen` featuring live server toggle, dynamic IP endpoint display, API key and prompt configuration, real-time incoming image preview, visual analysis text readout, and manual replay audio trigger.
  - Resolved `org.json` JVM mocking issues and verified all 21 unit tests pass (`BUILD SUCCESSFUL`).
  - Assembled verified production debug APK at `app/build/outputs/apk/debug/app-debug.apk` (20.9 MB).
  - **Next:** Phase 6 — End-to-end dual-phone integration test: connect Phone 1 and Phone 2 to the same local Wi-Fi or mobile hotspot, initiate captures from Phone 2, and verify that the JPEG arrives at Phone 1, is analyzed by Gemini Vision, and is spoken through Bluetooth earbuds while Phone 2's LED pulses green.

- **2026-09-26** — Completed Phase 4 (Polish & real-world parity):
  - Added tunable `JpegQualityPreset` (Fast 65%, Balanced 80% default, Fine 92%) matching real ESP32-S3-CAM DMA buffer constraints.
  - Updated `SettingsScreen` with JPEG compression selector and estimated payload badges (`~35 KB` to `~350 KB`).
  - Added breathing glow/pulse animation to `LedIndicator` when active, with click-to-reset support.
  - Polished `CameraScreen` telemetry with sensor specs, estimated payload, JPEG quality, and responsive layout for different aspect ratios and orientations.
  - Added `SensorSpecsTest` test suite. All 15 unit tests passing (`BUILD SUCCESSFUL`) and debug APK assembled cleanly.
  - **Next:** Phone 2 build is fully polished and tested against the mock server. Next milestone is Phase 5 (Phone 1 Termux/Flask server in a separate build) followed by Phase 6 (End-to-end dual phone test).

- **2026-09-26** — Completed Phase 3 (Standalone test pass):
  - Created standalone mock server in `tools/mock_server.py` supporting all 6 response and error states (`cycle`, `200`, `422`, `500`, `malformed`, `timeout`), with interactive web dashboard at `http://localhost:8080/` and disk saving for captured frames in `tools/captured_frames/`.
  - Created automated test script `tools/test_mock_server.py` validating multipart parsing and all response codes.
  - Added MockWebServer test dependency and unit test suite `OkHttpImageUploaderTest.kt` verifying that `OkHttpImageUploader` handles 200, 422, 500, malformed codes, and socket timeouts properly.
  - Verified 11/11 JVM unit tests pass and debug APK builds cleanly.
  - Created `tools/README.md` with instructions for local hotspot, Wi-Fi, and ADB reverse port forwarding.
  - **Next:** Phase 4 — Polish & real-world parity: tune capture resolution/quality toward realistic ESP32-S3-CAM defaults, multi-device sanity check, UI polish.

- **2026-09-26** — Completed Phase 1 (Scaffolding & silent capture) and Phase 2 (Networking & response mapping):
  - Built Clean Architecture package layout (`camera/`, `network/`, `settings/`, `ui/`) with Jetpack Compose and DataStore settings persistence.
  - Implemented CameraX silent capture matching OV3660 framing without shutter sound, flash, or capture animations.
  - Built OkHttp multipart image uploader (`ImageUploader`) with configurable timeouts (5s connect, 10s read/write).
  - Implemented centralized `ResponseStatusHandler` mapping HTTP 200, 422, timeouts, no-network, 5xx, and malformed responses to 6 distinct LED color and haptic vibration patterns.
  - Added unit test suite `ResponseStatusHandlerTest` (all 6 tests passing) and verified debug APK assembly.
  - **Next:** Phase 3 — Standalone test pass: build a lightweight local mock server to exercise all 6 response/error states against the app before Phone 1 exists.

- **2026-09-26** — Planning completed in Claude: full app spec, workflow,
  error-handling approach, and OV3660 capture-quality target defined. This
  guide.md created as the persistent resume/wrap-up file.
  **Next:** open project in Google Antigravity, import skills, decide
  whether `agent.md` is separate from this file or merges with it, then
  begin scaffolding the Phone 2 app (camera capture + settings screen
  first, upload/LED logic second).