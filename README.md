# Virtual 32

Virtual 32 is a native Android companion application designed to act as a bridge between custom, low-power hardware (like an ESP32-S3) and cloud AI models (Gemini, Claude). It acts as the "brain" for a silent, camera-equipped physical study assistant. 

The app runs an embedded, battery-optimized HTTP server on a local hotspot, receives images of multiple-choice questions from the hardware, processes them through an LLM, and transmits the answers back to the device using a strictly defined LED blink protocol.

## Features

- **Embedded Local Server**: Runs a robust background Coroutine-based HTTP server that listens for incoming JPEGs on your phone's hotspot interface.
- **Silent Operation Protocol**: Designed for environments where audio isn't an option. Responses are translated into a sequence of blue and red LED blinks indicating processing states, answers (A-E), or hardware errors.
- **Built-in Hardware Simulator**: Don't have the ESP32 hardware yet? The app includes a "Simulator" tab powered by CameraX that mimics the exact behavior, constraints, and network timings of the physical hardware using a second Android device.
- **Provider Agnostic**: First-class support for both Gemini and Claude APIs, with automatic fallback handling and intelligent retry backoffs.
- **Robust Background Execution**: Engineered as a specialized foreground service (`START_STICKY`, custom wake/wifi locks) to survive aggressive OEM battery optimizations, Android Doze mode, and network disconnects.
- **Dashboard & History**: A fully-featured Jetpack Compose UI to monitor pipeline health, review past batches/answers, re-process images, and export history to CSV.

## How It Works

1. **Hardware Capture**: The ESP32 (or a phone running the Simulator) captures a photo of a question and sends a `multipart/form-data` POST request to the phone's hotspot IP on port `5000`.
2. **Pipeline Processing**: Virtual 32 receives the image, writes it to the local gallery, and queues it in a non-blocking `Channel` worker.
3. **AI Inference**: The image and prompt are sent to the configured AI provider.
4. **Signaling**: The hardware polls the `/next` endpoint. Virtual 32 responds with a parsed JSON answer, which the hardware translates into a physical LED blink sequence.

## Setup & Usage

### 1. Configure the Application
1. Install and open the app on your primary Android device (the "Brain").
2. Navigate to **Settings > AI & Prompt Settings**.
3. Enter your Gemini or Claude API Key. 

### 2. Start the Receiver
1. Turn on your Android device's **Mobile Hotspot**.
2. On the **Home** tab, toggle the Receiver to **START**. 
3. *Note:* You may be prompted to grant Notification permissions or Battery Optimization exemptions to ensure the server doesn't get killed in the background.

### 3. Connect the Hardware
**Option A: Real ESP32 Hardware**
Configure your ESP32 to connect to your phone's Hotspot SSID. It will automatically discover the phone's IP address. (Refer to `docs/ESP32_CONTRACT.md` if you are writing the firmware).

**Option B: Using the Simulator (Requires a 2nd Phone)**
1. Connect the 2nd phone to the 1st phone's Hotspot.
2. Open Virtual 32 on the 2nd phone and navigate to the **Simulator** tab.
3. Enter the IP address displayed on the 1st phone's Home screen into the Simulator settings and tap **Apply Settings**.
4. Press **BTN 1 (Capture)** on the simulator to take a photo. Watch the virtual LEDs respond.

## Hardware Firmware Contract

If you are building the physical ESP32 device, the exact network rules, debounce timings, JSON shapes, and LED flash durations are documented in the [ESP32 Contract](docs/ESP32_CONTRACT.md). 

A Python script is also provided in `tools/esp32_client_sim.py` to help test the API endpoints directly from your PC.

## Tech Stack

- **UI**: Jetpack Compose (Material 3)
- **Architecture**: MVVM, Clean Architecture, Kotlin Coroutines & StateFlow
- **Camera**: CameraX
- **Networking**: Custom Coroutine Socket Server (Receiver), OkHttp (Client/AI)
- **Local Storage**: Room Database (KSP), DataStore (Settings)

## License
MIT License. See `LICENSE` for more information.
