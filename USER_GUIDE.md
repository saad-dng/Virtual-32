# Virtual 32 - User Guide

Welcome to Virtual 32. This app bridges the physical hardware device (Phone 2 or ESP32) with a powerful cloud AI (Phone 1/The Brain) to act as a silent study assistant. 

## 6-Step Setup

1. **Enable Phone Hotspot**: On Phone 1, turn on your Mobile Hotspot or connect both devices to the same local Wi-Fi.
2. **Open Virtual 32**: Launch the Virtual 32 app on Phone 1.
3. **Configure AI Keys**: Go to Settings -> AI & Prompt Settings. Add your Gemini or Claude API key.
4. **Start the Receiver**: On the Home tab, ensure the Receiver switch is toggled **ON**. A persistent background notification will appear indicating it is listening.
5. **Connect Phone 2 / Hardware**: 
   - If using a second phone as the hardware: Open Virtual 32 -> Simulator tab. Enter the IP Address shown on Phone 1's Home screen into the Simulator settings.
   - If using ESP32: It should automatically connect to your hotspot.
6. **Capture**: Press the capture button (Button 1) on the hardware to take a picture of your multiple-choice questions.

## LED Cheat-Sheet

The hardware communicates entirely through its Blue and Red LEDs.

### Blue LED (Operations & Answers)
- **Photo Added**: 1 Short flash (100ms)
- **Processing**: Slow pulse (500ms on / 500ms off)
- **Ready/Success**: Solid light for 1 second
- **Answer A**: 1 Blink (250ms on / 250ms off)
- **Answer B**: 2 Blinks
- **Answer C**: 3 Blinks
- **Answer D**: 4 Blinks
- **Answer E**: 5 Blinks

### Red LED (Errors & Status)
- **Unclear Photo**: 1 Long pulse (1.2 seconds)
- **Cycle Complete**: 2 Medium pulses
- **No Answers Queued**: 1 Short pulse + 1 Long pulse
- **Unreachable**: 3 Fast flashes
- **Server/AI Error**: 5 Fast flashes

## Taking a Multi-Photo Paper

When capturing multi-page exams or long question sheets spanning multiple photos:
1. **Camera Position**: Keep the camera parallel to the page to minimize distortion and perspective glare.
2. **Consecutive Overlap**: Overlap about 20% between consecutive photos so boundary questions are not clipped or obscured.
3. **Capture Order**: Shoot first to last in sequential order. Each short press of Button 1 adds the photo to the current session (confirmed by a quick 100 ms blue flash).
4. **Finish Session**: When all pages are captured, long-press Button 1 (hold for 1.5 seconds until the blue LED begins to slowly pulse). Virtual 32 will freeze the session and analyze all photos as a unified batch.
5. **Auto-Submit Option**: If configured under Settings -> Sessions, the session can also auto-submit if no new photo arrives after the configured timeout (e.g., 20s).

## Troubleshooting

| Problem | Cause | Solution |
|---|---|---|
| **Red LED flashes 3 times (fast)** | Hardware cannot reach Phone 1. | Verify both devices are on the same Hotspot/Wi-Fi. Check the IP address matches. |
| **Red LED flashes 5 times (fast)** | AI Failure or Missing API Key. | Go to Settings -> AI & Prompts and "Test Key". Check your internet connection on Phone 1. |
| **Red LED long pulse (1.2s)** | The AI couldn't read the photo. | Ensure the document is well-lit, fully visible, and free of extreme angles. |
| **"Q missing" or unreadable warnings** | Page gap, skipped question, or blurry section across photos. | Virtual 32 detects gaps (e.g., "Q9 missing"). Retake the affected photo (e.g., photo 2) and send all photos again, ensuring proper overlap and focus. |
| **App is killed in background** | Strict OEM battery optimizations. | Go to Android Settings -> Apps -> Virtual 32 -> Battery -> Unrestricted. |
| **"Port in use" error on Home** | Another app is blocking port 5000. | Tap "Stop" and "Start" to reset, or restart the phone. |
