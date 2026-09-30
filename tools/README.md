# Phone 1 Mock Server & Test Suite (Phase 3)

This directory contains the standalone mock server and test runner used to exercise all 6 response and error paths on Phone 2 without requiring Phone 1 or live Gemini API infrastructure.

## Files
- `mock_server.py`: Interactive Python HTTP server simulating Phone 1's `/upload` endpoint.
- `test_mock_server.py`: Automated integration test verifying mock server response modes.
- `captured_frames/`: Auto-created directory where incoming JPEGs from Phone 2 are saved.

## Quick Start

### 1. Start Mock Server on your PC
```bash
python tools/mock_server.py --port 8080 --mode cycle
```

Available modes:
- `cycle` (default): Cycles automatically on each trigger: 200 -> 422 -> 500 -> malformed -> timeout.
- `200`: Always returns HTTP 200 OK (Phone 2 LED stays Gray, no vibration).
- `422`: Always returns HTTP 422 Validation Error (Phone 2 LED turns Red, 1s continuous buzz).
- `500`: Always returns HTTP 500 Server Error (Phone 2 LED turns Purple, 1 long + 1 short buzz).
- `malformed`: Returns HTTP 418 unexpected body (Phone 2 LED turns Orange, 1 medium pulse).
- `timeout`: Delays 12s before responding (triggers Phone 2 10s timeout: LED turns Amber, 2 short buzzes).

### 2. Connect Phone 2
- **Wi-Fi / Hotspot**: Ensure Phone 2 is on the same local network or Wi-Fi hotspot as your PC. Open Phone 2 Settings and enter your PC's local IP (e.g. `192.168.1.50`) and port `8080`.
- **ADB Reverse (USB)**: If connected via USB, run:
  ```bash
  adb reverse tcp:8080 tcp:8080
  ```
  Then in Phone 2 Settings, set server IP to `127.0.0.1` and port `8080`.

### 3. Dynamic Mode Switching
You can switch modes without restarting the server:
- Open `http://localhost:8080/` in your browser and click any mode button.
- Or send a GET request: `curl http://localhost:8080/mode?set=422`
