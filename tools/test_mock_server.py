#!/usr/bin/env python3
"""
Automated test suite for tools/mock_server.py
Verifies that all mock server modes and multipart uploads function properly.
"""

import subprocess
import sys
import time
import urllib.request
import urllib.error
import http.client
import json

SERVER_PORT = 8089

def send_multipart_upload(port: int, dummy_filename: str = "test_frame.jpg", dummy_bytes: bytes = b"\xff\xd8\xff\xd9"):
    boundary = "----WebKitFormBoundaryVirtual32TestBoundary"
    body = bytearray()
    body.extend(f"--{boundary}\r\n".encode())
    body.extend(f'Content-Disposition: form-data; name="image"; filename="{dummy_filename}"\r\n'.encode())
    body.extend(b"Content-Type: image/jpeg\r\n\r\n")
    body.extend(dummy_bytes)
    body.extend(f"\r\n--{boundary}--\r\n".encode())

    conn = http.client.HTTPConnection("127.0.0.1", port, timeout=5)
    headers = {
        "Content-Type": f"multipart/form-data; boundary={boundary}",
        "Content-Length": str(len(body)),
    }
    conn.request("POST", "/upload", body=body, headers=headers)
    response = conn.getresponse()
    response_data = response.read().decode(errors="replace")
    conn.close()
    return response.status, response_data

def set_server_mode(port: int, mode: str):
    url = f"http://127.0.0.1:{port}/mode?set={mode}"
    req = urllib.request.urlopen(url, timeout=3)
    data = json.loads(req.read().decode())
    return data

def main():
    print(f"Starting mock server on test port {SERVER_PORT}...")
    server_process = subprocess.Popen(
        [sys.executable, "tools/mock_server.py", "--port", str(SERVER_PORT), "--mode", "200"],
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        text=True,
    )

    try:
        # Give server time to bind
        time.sleep(1.0)

        # 1. Test GET /status
        print("1. Testing GET /status...")
        req = urllib.request.urlopen(f"http://127.0.0.1:{SERVER_PORT}/status", timeout=3)
        assert req.status == 200, f"Expected 200, got {req.status}"
        assert "Anti-Gravity Mock Server" in req.read().decode()
        print("   [OK] GET /status OK")

        # 2. Test 200 Mode
        print("2. Testing POST /upload in 200 Mode...")
        status, body = send_multipart_upload(SERVER_PORT)
        assert status == 200, f"Expected 200, got {status}"
        assert '"status":"ok"' in body
        print(f"   [OK] 200 Mode OK (status={status})")

        # 3. Test 422 Mode
        print("3. Switching to 422 Mode and testing POST /upload...")
        res = set_server_mode(SERVER_PORT, "422")
        assert res["mode"] == "422"
        status, body = send_multipart_upload(SERVER_PORT)
        assert status == 422, f"Expected 422, got {status}"
        assert "Validation failed" in body
        print(f"   [OK] 422 Mode OK (status={status})")

        # 4. Test 500 Mode
        print("4. Switching to 500 Mode and testing POST /upload...")
        set_server_mode(SERVER_PORT, "500")
        status, body = send_multipart_upload(SERVER_PORT)
        assert status == 500, f"Expected 500, got {status}"
        assert "Internal Server Error" in body
        print(f"   [OK] 500 Mode OK (status={status})")

        # 5. Test Malformed Mode
        print("5. Switching to Malformed Mode and testing POST /upload...")
        set_server_mode(SERVER_PORT, "malformed")
        status, body = send_multipart_upload(SERVER_PORT)
        assert status == 418, f"Expected 418, got {status}"
        assert "teapot" in body
        print(f"   [OK] Malformed Mode OK (status={status})")

        # 6. Test Cycle Mode
        print("6. Switching to Cycle Mode...")
        set_server_mode(SERVER_PORT, "cycle")
        # Cycle 1: 200
        s1, _ = send_multipart_upload(SERVER_PORT)
        assert s1 == 200, f"Expected 200 in cycle 1, got {s1}"
        # Cycle 2: 422
        s2, _ = send_multipart_upload(SERVER_PORT)
        assert s2 == 422, f"Expected 422 in cycle 2, got {s2}"
        # Cycle 3: 500
        s3, _ = send_multipart_upload(SERVER_PORT)
        assert s3 == 500, f"Expected 500 in cycle 3, got {s3}"
        # Cycle 4: Malformed
        s4, _ = send_multipart_upload(SERVER_PORT)
        assert s4 == 418, f"Expected 418 in cycle 4, got {s4}"
        print("   [OK] Cycle Mode sequence (200 -> 422 -> 500 -> 418) verified!")

        print("\nAll standalone mock server integration tests PASSED successfully!")
    finally:
        server_process.terminate()
        server_process.wait()

if __name__ == "__main__":
    main()
