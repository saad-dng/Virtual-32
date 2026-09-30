# 📱 Dual-Phone Smart Camera & Earbuds Setup Guide
### A Simple, Step-by-Step Guide (No Tech Knowledge Required!)

Welcome! This system turns two phones into an intelligent wearable device:
* **Phone 2 (The Eyes):** Acts like a mini spy camera. When you tap the big button, it takes a silent photo without making any noise or flash and sends it to Phone 1.
* **Phone 1 (The Brain & Voice):** Receives the photo, asks AI (Google Gemini) what is in front of the camera, and whispers the answer directly into your **Bluetooth earbuds**.

---

## 🛠️ What Was Fixed in This Update:
1. **Connectivity Issue Solved:** Enabled Android Local Network & Cleartext Traffic permissions and bound the receiver server to all interfaces (`0.0.0.0`). Phone 2 can now reach Phone 1 reliably.
2. **Built-in Connection Tester:** Inside Settings on Phone 2, there is now a **"TEST CONNECTION TO PHONE 1"** button so you can instantly verify whether the phones can communicate before taking any photo!
3. **Screen Layout & Status Bar Insets Fixed:** All headers, buttons, and camera views now cleanly sit below your phone's status bar, clock, battery icons, and camera notch (`statusBarsPadding()`).
4. **Full Back & Swipe Gestures Added:**
   * **Android System Back Gesture / Back Button:** Swiping from the phone's edge or pressing back works everywhere (returns from Settings to the main screen, and returns from Phone 1 to Phone 2).
   * **Horizontal Swipe to Switch:** Swipe left or right on the screen to switch between **Phone 2 (Camera)** and **Phone 1 (Brain)**!
   * **Top Mode Tab Bar:** An interactive switcher at the very top lets you switch modes with a single tap.

---

## 📋 What You Need Before You Start
1. **Both Android phones** with the updated **Virtual 32 app** installed.
2. **One pair of Bluetooth earbuds** (paired to Phone 1).
3. **An internet connection** (either your home Wi-Fi or Phone 1's Personal Hotspot).
4. **A free Google Gemini API Key** (explained in Step 2).

---

## ⚡ STEP 1: Connect Both Phones Together

Both phones must be on the **same Wi-Fi network**.

### Option A: At home with Wi-Fi (Easiest)
* Connect **Phone 1** to your home Wi-Fi.
* Connect **Phone 2** to the **exact same** home Wi-Fi.

### Option B: Outside (Using Mobile Hotspot)
1. On **Phone 1**, go to Android **Settings** ➔ **Network & internet** ➔ **Hotspot & tethering** ➔ Turn **ON Personal Hotspot**.
2. On **Phone 2**, turn on Wi-Fi and connect to Phone 1's hotspot.

---

## 🔑 STEP 2: Get a Free Gemini AI Key (One-time, 2 minutes)

The AI needs a key so it knows it has permission to answer your camera. It is completely free from Google.

1. On your phone or computer, open your browser and go to:
   👉 **https://aistudio.google.com/apikey**
2. Sign in with your standard Google (Gmail) account.
3. Click the blue button that says **"Create API key"**.
4. Choose **"Create API key in new project"**.
5. Google will show you a long string of letters and numbers (starting with `AIzaSy...`).
6. Click **Copy** to copy this key.

---

## 🎧 STEP 3: Setup Phone 1 ("The Brain & Earbuds")

1. Put in your **Bluetooth earbuds** and make sure they are connected to **Phone 1**.
2. Open the **Virtual 32** app on **Phone 1** (look for the tactile cream & obsidian camera icon).
3. On the opening **Virtual 32 Welcome Screen**, tap:
   👉 **[ 🎧 PHONE 1: THE BRAIN ]**
   *(If you're already in the app, you can also just tap the top `Phone 1 (Brain)` tab)*.
4. Paste your **Gemini API Key** from Step 2 into the **Gemini API Key** box.
5. In the **EMBEDDED HTTP RECEIVER** box:
   * Look at the line **"IP to enter on Phone 2"** (e.g. `192.168.1.45` or `192.168.43.1`).
   * Tap the **"COPY"** button or write down those numbers!
   * Ensure the server button says **STOP** (which means it is currently **ONLINE** in green).

> Phone 1 is now ready and listening! You can put Phone 1 in your pocket.

---

## 📷 STEP 4: Setup Phone 2 ("The Camera")

1. Open the **Virtual 32** app on **Phone 2**.
2. On the opening **Virtual 32 Welcome Screen**, tap:
   👉 **[ 📷 PHONE 2: THE EYES ]**
   *(If you're already in the app, tap the top `Phone 2 (Camera)` tab)*.
3. Tap the **⚙️ Settings (Gear icon)** in the top right corner.
4. In Settings:
   * **Server IP Address**: Type in or paste the IP address numbers you got from Phone 1 (e.g. `192.168.1.45` or `192.168.43.1`).
   * **Server Port**: Make sure this is set to **`5000`**.
   * **TEST CONNECTION:** Tap the **"TEST CONNECTION TO PHONE 1"** button!
     * It will test the link in real-time.
     * When it turns green and says *"Connected! Phone 1 Receiver is online and ready"*, your phones are 100% connected!
   * Tap the **Save** button.
5. Swipe from the left edge of your screen or tap the back arrow to return to the camera.

---

## 🚀 STEP 5: Test the Camera!

1. Point **Phone 2** at anything (a bottle, a book, your shoes, or an object).
2. Tap the big **⚪ TRIGGER (GPIO 1)** button at the bottom of Phone 2.
3. Notice:
   * Phone 2 takes the photo completely silently (zero shutter sound, zero flash).
   * The small light on Phone 2 turns **Blue (Sending...)** and then **Solid Green (Success)**.
4. **Listen to your earbuds connected to Phone 1!**
   * The AI will describe the scene directly into your ear!
   * On Phone 1's screen, you will also see the photo and the text description.

---

## 💡 What Do the Lights on Phone 2 Mean?

| Light Color | Meaning | What to do |
| :--- | :--- | :--- |
| **⚫ Gray** | Idle & Ready | Ready to take a picture. |
| **🔵 Blue** | Sending... | The photo is being sent over Wi-Fi right now. |
| **🟢 Solid Green** | Success! | Photo arrived, AI analyzed it, and speech was sent to earbuds. |
| **🟠 Orange** | Needs API Key | Phone 1 received the picture, but you need to paste your Gemini API key on Phone 1. |
| **🔴 Red** | Cannot Connect | Check that both phones are on the same Wi-Fi and test connection in Settings. |
| **🟡 Yellow** | Server Error | Temporary hiccup. Try pressing the trigger again. |
| **🟣 Purple** | Timeout | Connection dropped or took too long. |

*(Tip: You can tap on the light at any time to reset it back to gray!)*
