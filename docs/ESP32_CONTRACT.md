# ESP32-S3 Firmware Contract

This document dictates the exact communication protocol between the ESP32 hardware and the Virtual 32 companion app. The firmware **must** implement this exactly as specified to ensure flawless operation.

## 1. Network & Endpoints
The phone app acts as an HTTP/1.1 server running on the phone's Hotspot interface. 
- **Base URL:** `http://<phone-ip>:5000`
- **Fallback Port:** `8080` (attempt this if `5000` refuses connection)
- **Header Requirement:** All requests **must** include `Connection: close` (do not rely on Keep-Alive).

### 1.1 Endpoints and JSON Shapes

#### `GET /ping`
- **Purpose:** Heartbeat to let the app know the ESP32 is alive, and to check system status.
- **Timeout:** 5 seconds
- **ESP Retry:** Do not block. Just retry on the next interval.
- **Interval:** Send roughly every 10 seconds.
- **Response (200 OK):**
  ```json
  {"ok":true,"app":"virtual32","answers":20,"cursor":0,"busy":false}
  ```

#### `POST /upload`
- **Purpose:** Send a captured JPEG to the phone for AI processing.
- **Timeout:** 60 seconds (AI processing can be slow).
- **ESP Retry:** 3 retries with exponential backoff if the server replies with a network error or times out.
- **Body:** `multipart/form-data` with field name `image` or raw `image/jpeg`.
- **JPEG constraints:** Minimum 2 KB, Maximum 8 MB. Must start with magic bytes `FF D8`.
- **Success Response (200 OK):**
  ```json
  {"status":"ok","count":20,"batch":1234}
  ```
- **Unclear Image Response (200 OK):**
  ```json
  {"status":"unclear","reason":"Blurry image"}
  ```
- **Error Response (200 OK or 400/500):**
  ```json
  {"status":"error","reason":"ai_failed"} // reasons: ai_failed, no_key, no_internet, timeout, paused, bad_image
  ```

#### `GET /next`
- **Purpose:** Advance to the next answer in the cycle. Triggered by Button 2 single-click.
- **Timeout:** 5 seconds
- **Response - Answer (200 OK):**
  ```json
  {"ok":true,"q":1,"of":20,"choice":"A","blinks":1}
  ```
- **Response - Cycle Complete (200 OK):**
  ```json
  {"ok":true,"end":true,"of":20}
  ```
- **Response - Empty / No Answers (200 OK):**
  ```json
  {"ok":false,"reason":"empty"}
  ```

#### `GET /repeat`
- **Purpose:** Repeat the current answer without advancing. Triggered by Button 2 double-click.
- **Timeout:** 5 seconds
- **Response:** Identical shape to `/next`, but returns the *current* cursor position instead of advancing.

#### `GET /reset`
- **Purpose:** Reset the cursor back to the start.
- **Response (200 OK):**
  ```json
  {"ok":true}
  ```

---

## 2. LED Blink Language

The ESP32 communicates exclusively via two LEDs (Blue and Red). There is no display and no audio.

| State | LED | Pattern | Timings (ms) |
|---|---|---|---|
| Processing /upload | Blue | Slow Pulse | 500 On / 500 Off |
| Upload OK | Blue | Solid | 1000 On |
| Answer A | Blue | 1 Blink | 250 On / 250 Off |
| Answer B | Blue | 2 Blinks | 250 On / 250 Off |
| Answer C | Blue | 3 Blinks | 250 On / 250 Off |
| Answer D | Blue | 4 Blinks | 250 On / 250 Off |
| Answer E | Blue | 5 Blinks | 250 On / 250 Off |
| Photo Unclear | Red | 1 Long | 1200 On |
| Cycle Complete (End) | Red | 2 Medium | 500 On / 300 Off |
| No Answers Yet (Empty) | Red | 1 Short, 1 Long | 150 On, 200 Off, 800 On |
| Server Unreachable | Red | 3 Fast | 120 On / 120 Off |
| Server/AI Error | Red | 5 Fast | 120 On / 120 Off |

**Crucial LED Rule:** A new button press **must** instantly interrupt any blink pattern currently in progress and start the new sequence.

---

## 3. Hardware Interactions

### Button 1 (Shutter)
- **Action:** Captures photo and issues `POST /upload`.
- **Debounce Advice:** 30 ms hardware/software debounce.

### Button 2 (Navigation)
- **Action:** 
  - Single Click -> `GET /next`
  - Double Click -> `GET /repeat`
- **Double-Click Window:** 350 ms.
- **Debounce Advice:** 30 ms.

---

## 4. Example Raw HTTP Request

```http
POST /upload HTTP/1.1
Host: 192.168.43.1:5000
Connection: close
Content-Type: multipart/form-data; boundary=----Esp32Boundary
Content-Length: [Length]

------Esp32Boundary
Content-Disposition: form-data; name="image"; filename="photo.jpg"
Content-Type: image/jpeg

[RAW JPEG BYTES FF D8 ... FF D9]
------Esp32Boundary--
```
