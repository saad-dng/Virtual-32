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

# §1 Project Overview
Virtual 32 is the phone-side app for a university project: an ESP32-S3 Sense device takes a photo of multiple-choice questions when Button 1 is pressed, sends it to this app over the phone's hotspot, the app sends it to a vision AI, stores the answers, and the ESP32 shows them ONLY via LEDs (no display, no sound). Button 2 cycles through the answers one by one; a double-click repeats the current one. A red LED signals problems. Test material: practice/sample MCQ sheets only.
The ESP32 firmware is built outside Antigravity. This app must follow the contract in §3 exactly so the firmware can be written against it.

# §2 Modes (one APK)
- Receiver (default): embedded HTTP server + AI pipeline + answer store + dashboard. Runs as a foreground service.
- Simulator: the phone pretends to be the ESP32 (camera, virtual Button 1/2, virtual blue/red LEDs) using the same protocol and blink patterns. Used for testing before hardware arrives; can target this phone (loopback) or a second phone.
Navigation: bottom bar = Home | Answers | Simulator | Settings.

# §3 Protocol contract (source of truth; mirror in docs/ESP32_CONTRACT.md)
Base URL: http://<phone-ip>:5000 (fallback port 8080). All replies are JSON with header "Connection: close".
- GET /ping (alias /status) -> {"ok":true,"app":"virtual32","answers":N,"cursor":i,"busy":bool}. Any call updates "ESP last seen". The ESP pings about every 10 s.
- POST /upload -> body is multipart field "image" OR raw Content-Type image/jpeg. Validate JPEG magic bytes (FF D8) and size 2 KB–8 MB (else 400 {"status":"error","reason":"bad_image"} / 413). Blocks until the AI result is ready (max 45 s), then 200:
  {"status":"ok","count":N,"batch":id} | {"status":"unclear","reason":"..."} | {"status":"error","reason":"ai_failed|no_key|no_internet|timeout|paused"}.
- GET /next -> advances the cycle. 200 {"ok":true,"q":7,"of":20,"choice":"C","blinks":3}. After the LAST answer, the next call returns {"ok":true,"end":true,"of":20} (cycle complete) and the following call wraps to the first answer. No answers stored: {"ok":false,"reason":"empty"}.
- GET /repeat -> same shape as /next for the CURRENT answer without advancing. If nothing is current: {"ok":false,"reason":"empty"}.
- GET /reset -> cursor back to the start, {"ok":true}.
- choice -> blinks: A=1, B=2, C=3, D=4, E=5.
- New batch rules: Replace mode = new photo replaces the active list and resets the cycle. Append mode = new answers are added to the end of the active list (same question number replaced by the newer answer), cursor unchanged.
- Every photo is processed the same way, forever (continuous mode). Photos arriving while one is processing are queued FIFO.

# §4 Blink language (identical in Simulator, app docs and future firmware)
Blue LED: answer = N blinks (250 ms on / 250 ms off) | processing = slow pulse (500/500) until a reply | ready (upload ok) = solid 1000 ms.
Red LED: photo unclear = 1 long (1200 ms) | cycle complete = 2 medium (500 on / 300 off) | no answers yet = 1 short (150) + 1 long (800), 200 ms gap | server unreachable (ESP-side) = 3 fast (120/120) | server/AI error = 5 fast (120/120).
A new button press interrupts any pattern in progress. Double-click window = 350 ms. All timings live in one constants object (BlinkPatterns.kt).

# §5 Features
Core: continuous processing queue; answer list in the app (with cursor mirror, manual edit, low-confidence flag); every photo saved to the gallery (Pictures/Virtual32); editable AI prompt with presets and a locked JSON output contract; Gemini + Claude providers with fallback; runs reliably in the background.
Extras: process photos from the gallery (test without ESP); reprocess a batch with a new prompt; history + export; diagnostics + self-test; usage stats; settings backup/restore; Quick Settings tile; pause-AI switch.

# §6 Failure & backup matrix
| Failure | Behaviour |
|---|---|
| Primary AI fails / rate-limited | Retry 2x with backoff, then fallback provider if configured |
| No internet | Photo stays queued (gallery copy already saved), auto-retry when network returns; /upload replies error/no_internet |
| ESP32 dies or is absent | Next / Repeat / Reset available on the phone; answers persist |
| App/process killed | Foreground service restarts (sticky); answers + cursor restored from the database |
| Hotspot/Wi-Fi changes | Server rebinds, notification and Home show the new IP |
| Garbage AI output | Tolerant parser; one retry; otherwise status error |

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

# Roadmap
Legacy foundation (done): 1 Scaffolding · 2 Networking · 3 Standalone test pass · 4 Polish · 5 Old receiver/earbuds (now being replaced).
6 Cleanup & foundation · 7 Protocol server · 8 AI pipeline & queue · 9 Storage & gallery · 10 Background reliability · 11 Dashboard & Answers · 12 AI & Prompt settings · 13 Simulator v2 · 14 Power features · 15 Integration hardening & docs · 16 ESP32 firmware (outside Antigravity; uses docs/ESP32_CONTRACT.md).
Current phase: 7.

---

## Status Log
*(most recent entry first — append, don't rewrite)*

- **2026-09-30** — Persistence & Gallery (Phase 9):
  - **Done:** Implemented Room database with `Batch`, `AnswerEntity`, and `CycleState` tables. Replaced the in-memory AnswerStore with `RoomAnswerStore`, fully supporting persistence of the cursor, `REPLACE`, and `APPEND` modes. Implemented `GalleryWriter` to save photos to MediaStore before AI calls (handling Scoped Storage API 29+ correctly) and `PhotoCache` for internal fast-access copies. Added robust batch retention (keeping the latest 100). All Room functions backed by Robolectric in-memory tests and custom filename generation unit tests.
  - **Next:** Phase 10 (Receiver Service & background reliability).

- **2026-09-30** — AI Pipeline & Queue (Phase 8):
  - **Done:** Implemented `VisionProvider` (with `GeminiProvider` and `ClaudeProvider`), `AiResponseParser`, and `PromptBuilder` enforcing the locked JSON schema contract. Implemented `PhotoPipelineImpl` as a robust Coroutine Channel queue with retries, 429 backoff, fallback provider support, network awareness, and `ImageResizer`. Wired `PhotoPipelineImpl` to `ReceiverHttpServer` inside `HomeScreen.kt` for acceptance testing. Updated `AnswerStore` to support REPLACE and APPEND modes. Added MockWebServer unit test suites for providers and parsing (15+ tests).
  - **Next:** Phase 9 (Storage & gallery).

- **2026-09-30** — Protocol Server implementation (Phase 7):
  - **Done:** Completely rewrote `ReceiverHttpServer` to precisely implement the §3 protocol contract (handling `/upload`, `/ping`, `/next`, `/repeat`, `/reset` with exact JSON structures). Added IP discovery prioritizing Wi-Fi/Hotspot. Created robust HTTP/1.1 parsing handling 8MB body caps, chunked encoding, and timeouts. Added `PhotoPipeline` and `AnswerStore` interfaces with fake/in-memory implementations.
  - **Decisions:** Put the acceptance test Start Server button directly on `HomeScreen.kt`. HTTP parsing does not rely on third-party HTTP libraries to remain lightweight and embedded.
  - **Next:** Phase 8 (AI pipeline & queue).

- **2026-09-30** — Docs migrated to the MCQ Blinker direction:
  - **Next:** Phase 7.

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