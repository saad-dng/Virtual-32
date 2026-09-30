import argparse
import json
import time
import urllib.request
import urllib.error
from urllib.error import URLError, HTTPError
from urllib.request import Request
from email.message import EmailMessage

BASE_URL = "http://127.0.0.1:5000"

def parse_args():
    parser = argparse.ArgumentParser(description="Virtual 32 ESP32 Client Simulator")
    parser.add_argument("--host", default="127.0.0.1", help="Phone IP address")
    parser.add_argument("--port", default="5000", help="Phone Server Port")
    
    subparsers = parser.add_subparsers(dest="command", required=True)
    
    subparsers.add_parser("ping", help="Ping the server")
    
    parser_photo = subparsers.add_parser("photo", help="Upload a photo")
    parser_photo.add_argument("file", help="Path to photo JPEG")
    
    subparsers.add_parser("next", help="Advance to next answer")
    subparsers.add_parser("repeat", help="Repeat current answer")
    
    subparsers.add_parser("cycle", help="Loop /next until end")
    
    parser_soak = subparsers.add_parser("soak", help="Soak test with N photos")
    parser_soak.add_argument("--photos", type=int, default=10, help="Number of photos to send")
    parser_soak.add_argument("--interval", type=int, default=30, help="Seconds between photos")
    parser_soak.add_argument("--file", default="test.jpg", help="Path to photo JPEG")
    
    subparsers.add_parser("chaos", help="Randomly send commands to test robustness")
    
    return parser.parse_args()

def do_request(url, method="GET", data=None, headers=None, timeout=5.0, retries=0):
    if headers is None:
        headers = {}
    headers["Connection"] = "close"
    
    backoff = 1.0
    for attempt in range(retries + 1):
        try:
            req = Request(url, data=data, headers=headers, method=method)
            with urllib.request.urlopen(req, timeout=timeout) as response:
                body = response.read().decode("utf-8")
                return response.status, json.loads(body)
        except HTTPError as e:
            try:
                body = e.read().decode("utf-8")
                return e.code, json.loads(body)
            except:
                return e.code, {"status":"error", "reason":str(e)}
        except URLError as e:
            if attempt < retries:
                time.sleep(backoff)
                backoff *= 2
                continue
            return 0, {"status":"error", "reason":"timeout" if isinstance(e.reason, socket.timeout) else "unreachable"}
        except Exception as e:
            if attempt < retries:
                time.sleep(backoff)
                backoff *= 2
                continue
            return 0, {"status":"error", "reason":str(e)}
    return 0, {"status":"error", "reason":"max_retries"}

def print_blink(cmd, data):
    # Determine the blink pattern exactly as ESP would
    pattern = ""
    if cmd == "ping":
        if "ok" in data and data["ok"]:
            pattern = "Blue: solid 1s"
        else:
            pattern = "Red: 3 fast (unreachable)"
            
    elif cmd == "photo":
        status = data.get("status")
        if status == "ok":
            pattern = "Blue: solid 1000ms"
        elif status == "unclear":
            pattern = "Red: 1 long (1200ms)"
        else:
            pattern = "Red: 5 fast (error)"
            
    elif cmd in ["next", "repeat"]:
        ok = data.get("ok", False)
        if ok:
            if data.get("end"):
                pattern = "Red: 2 medium (cycle complete)"
            else:
                blinks = data.get("blinks", 0)
                pattern = f"Blue: {blinks} blinks (250/250)"
        else:
            reason = data.get("reason")
            if reason == "empty":
                pattern = "Red: 1 short (150ms) + 1 long (800ms) (empty)"
            else:
                pattern = "Red: 3 fast (network error)"

    print(f"ESP Pattern -> {pattern}")

def cmd_ping(base_url):
    print("Sending GET /ping...")
    status, data = do_request(f"{base_url}/ping", timeout=5.0, retries=0)
    print(f"Reply: {status} {data}")
    print_blink("ping", data)
    return status, data

def cmd_photo(base_url, filepath):
    print(f"Sending POST /upload with {filepath}...")
    try:
        with open(filepath, "rb") as f:
            img_data = f.read()
    except FileNotFoundError:
        # Create a dummy image if testing
        print("File not found, generating dummy 2KB JPEG bytes...")
        img_data = b'\xFF\xD8\xFF' + b'\x00' * 2048
        
    boundary = "----Esp32Boundary"
    body = (
        f"--{boundary}\r\n"
        f"Content-Disposition: form-data; name=\"image\"; filename=\"photo.jpg\"\r\n"
        f"Content-Type: image/jpeg\r\n\r\n"
    ).encode('utf-8') + img_data + f"\r\n--{boundary}--\r\n".encode('utf-8')
    
    headers = {
        "Content-Type": f"multipart/form-data; boundary={boundary}"
    }
    
    status, data = do_request(f"{base_url}/upload", method="POST", data=body, headers=headers, timeout=60.0, retries=3)
    print(f"Reply: {status} {data}")
    print_blink("photo", data)
    return status, data

def cmd_next(base_url):
    print("Sending GET /next...")
    status, data = do_request(f"{base_url}/next", timeout=5.0, retries=0)
    print(f"Reply: {status} {data}")
    print_blink("next", data)
    return status, data

def cmd_repeat(base_url):
    print("Sending GET /repeat...")
    status, data = do_request(f"{base_url}/repeat", timeout=5.0, retries=0)
    print(f"Reply: {status} {data}")
    print_blink("repeat", data)
    return status, data

def cmd_cycle(base_url):
    print("Starting cycle...")
    while True:
        status, data = cmd_next(base_url)
        if data.get("end") or not data.get("ok"):
            break
        time.sleep(1.5)

def cmd_soak(base_url, n_photos, interval, file):
    print(f"Starting soak test: {n_photos} photos every {interval}s")
    for i in range(1, n_photos + 1):
        print(f"--- Soak Photo {i}/{n_photos} ---")
        cmd_photo(base_url, file)
        if i < n_photos:
            print(f"Waiting {interval}s...")
            time.sleep(interval)

def cmd_chaos(base_url):
    import random
    print("Starting chaos test...")
    cmds = [
        lambda: cmd_ping(base_url),
        lambda: cmd_next(base_url),
        lambda: cmd_repeat(base_url),
        lambda: do_request(f"{base_url}/reset", timeout=5.0)
    ]
    for i in range(50):
        c = random.choice(cmds)
        c()
        time.sleep(random.uniform(0.1, 2.0))

if __name__ == "__main__":
    import socket
    args = parse_args()
    base = f"http://{args.host}:{args.port}"
    
    if args.command == "ping":
        cmd_ping(base)
    elif args.command == "photo":
        cmd_photo(base, args.file)
    elif args.command == "next":
        cmd_next(base)
    elif args.command == "repeat":
        cmd_repeat(base)
    elif args.command == "cycle":
        cmd_cycle(base)
    elif args.command == "soak":
        cmd_soak(base, args.photos, args.interval, args.file)
    elif args.command == "chaos":
        cmd_chaos(base)
