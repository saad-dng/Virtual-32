#!/usr/bin/env python3
"""
Anti-Gravity Digital Twin (Phone 2) - Standalone Mock Server
Exercises all 6 response and error paths defined in guide.md §2:
- HTTP 200: LED Gray (#9E9C96) | Vibration: None
- HTTP 422: LED Red (#D32F2F) | Vibration: 1s continuous buzz
- HTTP 500: LED Purple (#8E24AA) | Vibration: 1 long + 1 short buzz
- Timeout: LED Amber (#FFB300) | Vibration: 2 short pulses
- Malformed: LED Orange (#FB8C00) | Vibration: 1 medium pulse
- Disconnect / Unreachable: simulated by stopping the server or wrong IP

Usage:
    python tools/mock_server.py [--port 8080] [--mode cycle|200|422|500|malformed|timeout]
"""

import argparse
import cgi
import http.server
import json
import os
import socketserver
import sys
import time
from datetime import datetime
from pathlib import Path
from urllib.parse import parse_qs, urlparse

CAPTURE_DIR = Path(__file__).parent / "captured_frames"
CAPTURE_DIR.mkdir(parents=True, exist_ok=True)

# ANSI colors for console output
RESET = "\033[0m"
BOLD = "\033[1m"
GRAY = "\033[90m"
RED = "\033[91m"
GREEN = "\033[92m"
YELLOW = "\033[93m"
BLUE = "\033[94m"
PURPLE = "\033[95m"
CYAN = "\033[96m"

MODES = ["cycle", "200", "422", "500", "malformed", "timeout"]
CURRENT_MODE = "cycle"
CYCLE_SEQUENCE = ["200", "422", "500", "malformed", "timeout"]
cycle_index = 0

FEEDBACK_MAP = {
    "200": {
        "status": 200,
        "body": '{"status":"ok","message":"Scene processed successfully by Gemini twin"}',
        "content_type": "application/json",
        "led": f"{GRAY}GRAY (#9E9C96) [Idle]{RESET}",
        "haptic": "None",
    },
    "422": {
        "status": 422,
        "body": '{"status":"error","message":"Validation failed: image contains severe motion blur or occluded lens"}',
        "content_type": "application/json",
        "led": f"{RED}RED (#D32F2F) [Error]{RESET}",
        "haptic": "1 second continuous buzz",
    },
    "500": {
        "status": 500,
        "body": '{"status":"server_error","message":"Internal Server Error: Gemini pipeline exception"}',
        "content_type": "application/json",
        "led": f"{PURPLE}PURPLE (#8E24AA) [Server Error]{RESET}",
        "haptic": "1 long + 1 short buzz (500ms, 150ms gap, 200ms)",
    },
    "malformed": {
        "status": 418,
        "body": '<html><body><h1>418 I\'m a teapot</h1><p>Unexpected body</p></body></html>',
        "content_type": "text/html",
        "led": f"{YELLOW}ORANGE (#FB8C00) [Malformed]{RESET}",
        "haptic": "1 medium pulse (400ms)",
    },
    "timeout": {
        "delay": 12.0,  # Phone 2 has a 10s read timeout
        "status": 200,
        "body": '{"status":"delayed_ok"}',
        "content_type": "application/json",
        "led": f"{YELLOW}AMBER (#FFB300) [Timeout]{RESET}",
        "haptic": "2 short pulses (200ms, 100ms gap, 200ms)",
    },
}


class MockServerHandler(http.server.BaseHTTPRequestHandler):

    def log_message(self, format, *args):
        # Suppress default noisy stderr access logs
        return

    def do_GET(self):
        parsed = urlparse(self.path)
        query = parse_qs(parsed.query)

        global CURRENT_MODE, cycle_index

        if parsed.path == "/mode":
            new_mode = query.get("set", [None])[0]
            if new_mode in MODES:
                CURRENT_MODE = new_mode
                cycle_index = 0
                response_data = {"status": "ok", "mode": CURRENT_MODE}
            else:
                response_data = {"status": "error", "message": f"Invalid mode. Choose from {MODES}"}

            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps(response_data, indent=2).encode())
            print(f"{CYAN}-> Mode switched to: {BOLD}{CURRENT_MODE}{RESET}")
            return

        if parsed.path in ("/", "/status"):
            html = f"""<!DOCTYPE html>
<html>
<head>
<title>Anti-Gravity Mock Server</title>
<style>
body {{ font-family: monospace; padding: 2rem; background: #121212; color: #ececec; }}
h1 {{ color: #4fc3f7; }}
.btn {{ display: inline-block; padding: 0.5rem 1rem; margin: 0.25rem; background: #263238; color: #ececec; border: 1px solid #455a64; border-radius: 4px; text-decoration: none; }}
.btn:hover {{ background: #37474f; }}
.active {{ border-color: #4fc3f7; font-weight: bold; background: #00838f; }}
.box {{ background: #1e1e1e; padding: 1rem; border-radius: 6px; margin: 1rem 0; border: 1px solid #333; }}
</style>
</head>
<body>
<h1>Anti-Gravity Mock Server (Phone 1 Twin)</h1>
<p>Current Active Mode: <strong>{CURRENT_MODE.upper()}</strong></p>
<div class="box">
<h3>Switch Response Mode:</h3>
<a class="btn {'active' if CURRENT_MODE == 'cycle' else ''}" href="/mode?set=cycle">Cycle (200 → 422 → 500 → Malformed → Timeout)</a>
<a class="btn {'active' if CURRENT_MODE == '200' else ''}" href="/mode?set=200">200 OK (Gray LED)</a>
<a class="btn {'active' if CURRENT_MODE == '422' else ''}" href="/mode?set=422">422 Validation (Red LED)</a>
<a class="btn {'active' if CURRENT_MODE == '500' else ''}" href="/mode?set=500">500 Server Error (Purple LED)</a>
<a class="btn {'active' if CURRENT_MODE == 'malformed' else ''}" href="/mode?set=malformed">Malformed / Unexpected (Orange LED)</a>
<a class="btn {'active' if CURRENT_MODE == 'timeout' else ''}" href="/mode?set=timeout">Timeout (Amber LED)</a>
</div>
<p>Endpoint for Phone 2: <code>POST /upload</code> (multipart/form-data with field 'image')</p>
<p>Captured frames directory: <code>{CAPTURE_DIR.resolve()}</code></p>
</body>
</html>"""
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()
            self.wfile.write(html.encode())
            return

        self.send_response(404)
        self.end_headers()
        self.wfile.write(b"Not Found")

    def do_POST(self):
        parsed = urlparse(self.path)
        if parsed.path != "/upload":
            self.send_response(404)
            self.end_headers()
            self.wfile.write(b"Not Found")
            return

        global CURRENT_MODE, cycle_index

        # Determine effective mode
        if CURRENT_MODE == "cycle":
            target_mode = CYCLE_SEQUENCE[cycle_index % len(CYCLE_SEQUENCE)]
            cycle_index += 1
        else:
            target_mode = CURRENT_MODE

        feedback = FEEDBACK_MAP[target_mode]

        # Extract headers and content
        content_type = self.headers.get("Content-Type", "")
        content_length = int(self.headers.get("Content-Length", 0))
        timestamp_str = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

        print(f"\n{BOLD}{'='*60}{RESET}")
        print(f"{CYAN}[{timestamp_str}] Incoming POST /upload from {self.client_address[0]}:{self.client_address[1]}{RESET}")
        print(f"Content-Length: {content_length} bytes")

        # Read body and save file
        post_data = self.rfile.read(content_length)
        saved_file = None
        if "boundary=" in content_type:
            boundary = content_type.split("boundary=")[-1].strip().encode()
            # Simple multipart extraction for frame
            parts = post_data.split(b"--" + boundary)
            for part in parts:
                if b'filename="' in part or b'name="image"' in part:
                    header_and_body = part.split(b"\r\n\r\n", 1)
                    if len(header_and_body) == 2:
                        image_bytes = header_and_body[1].rstrip(b"\r\n--")
                        frame_filename = f"frame_{datetime.now().strftime('%Y%m%d_%H%M%S')}.jpg"
                        frame_path = CAPTURE_DIR / frame_filename
                        frame_path.write_bytes(image_bytes)
                        saved_file = frame_path
                        print(f"Captured Frame Saved: {frame_path.name} ({len(image_bytes)} bytes)")
                        break

        # Simulate timeout if requested
        if target_mode == "timeout":
            print(f"{YELLOW}⏳ Simulating Server Timeout: Holding connection for {feedback['delay']}s...{RESET}")
            time.sleep(feedback["delay"])

        # Send response
        self.send_response(feedback["status"])
        self.send_header("Content-Type", feedback["content_type"])
        self.send_header("Content-Length", str(len(feedback["body"])))
        self.end_headers()
        self.wfile.write(feedback["body"].encode())

        print(f"{BOLD}Simulated Response:{RESET} HTTP {feedback['status']}")
        print(f"{BOLD}Expected Phone 2 LED:{RESET} {feedback['led']}")
        print(f"{BOLD}Expected Vibration:{RESET} {feedback['haptic']}")
        if CURRENT_MODE == "cycle":
            next_mode = CYCLE_SEQUENCE[cycle_index % len(CYCLE_SEQUENCE)]
            print(f"{GRAY}Next cycle mode will be: {next_mode.upper()}{RESET}")
        print(f"{BOLD}{'='*60}{RESET}")


def run_server(port: int = 8080, mode: str = "cycle"):
    global CURRENT_MODE
    CURRENT_MODE = mode

    handler = MockServerHandler
    # Allow socket reuse so restarts are immediate
    socketserver.TCPServer.allow_reuse_address = True
    with socketserver.TCPServer(("0.0.0.0", port), handler) as httpd:
        print(f"{GREEN}{BOLD}Anti-Gravity Standalone Mock Server started on port {port}{RESET}")
        print(f"Active Mode: {BOLD}{CURRENT_MODE.upper()}{RESET}")
        print(f"Web Dashboard / Mode Switcher: {CYAN}http://localhost:{port}/{RESET}")
        print(f"Upload Endpoint: {CYAN}http://localhost:{port}/upload{RESET}")
        print(f"Captured JPEGs Directory: {CAPTURE_DIR.resolve()}\n")
        try:
            httpd.serve_forever()
        except KeyboardInterrupt:
            print("\nShutting down mock server.")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Anti-Gravity Phone 1 Mock Server")
    parser.add_argument("--port", type=int, default=8080, help="Port to listen on (default: 8080)")
    parser.add_argument("--mode", choices=MODES, default="cycle", help="Initial response mode (default: cycle)")
    args = parser.parse_args()
    run_server(port=args.port, mode=args.mode)
