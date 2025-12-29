#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Final robust test - ignore initial CM and send data immediately
"""

import socket
import time
from datetime import datetime


def calculate_crc16(data):
    """Calculate CRC16 checksum"""
    crc = 0xFFFF
    bytes_data = data.encode('ascii')

    for byte in bytes_data:
        crc ^= (byte & 0xFF)
        for _ in range(8):
            if (crc & 0x0001) != 0:
                crc >>= 1
                crc ^= 0xA001
            else:
                crc >>= 1

    return f"{crc:04X}"


def build_packet():
    """Build HJ/T212 data packet"""
    device_mn = "865229085145869"
    password = "123456"

    import random
    qn_base = datetime.now().strftime("%Y%m%d%H%M%S")
    qn_random = random.randint(0, 9999)
    qn = f"{qn_base}{qn_random:04d}1"

    data_time = datetime.now().strftime("%Y%m%d%H%M%S")
    cp_content = (
        f"MN={device_mn};"
        f"Ma=002162;"
        f"Rno=DE25IR006722;"
        f"Xtype=02;"
        f"LastAct=5.530E012;"
        f"NowAct=3.270E012;"
        f"SourceTime=20250703;"
        f"DataTime={data_time};"
        f"LONG=12102.1465;"
        f"LAT=3740.5073;"
        f"Xvalue=1000000.000;"
        f"Thres=100000.000;"
        f"AlertType=01;"
        f"BattChar=1000.0;"
        f"Sig=1"
    )

    data_segment = f"QN={qn};ST=61;CN=3051;PW={password};CP=&&{cp_content}&&"
    data_length = f"{len(data_segment):04d}"
    crc = calculate_crc16(data_segment)

    return f"##{data_length}{data_segment}{crc}\r\n"


def test_no_handshake():
    """Test: Connect and immediately send data (ignore CM)"""
    print("\n" + "="*70)
    print("TEST: Ignore Initial CM, Send Data Immediately")
    print("="*70)

    HOST = "221.214.62.118"
    PORT = 20050

    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(10)

        print(f"\n[1] Connecting to {HOST}:{PORT}...")
        sock.connect((HOST, PORT))
        print(f"    Connected")

        # Immediately set to non-blocking to read initial message
        # but don't respond to it
        print(f"\n[2] Checking for initial message...")
        sock.settimeout(0.5)
        try:
            initial_msg = sock.recv(1024)
            if initial_msg:
                print(f"    Received: {initial_msg.hex()}")
                print(f"    Action: IGNORING (no handshake response)")
        except socket.timeout:
            print(f"    No initial message (timeout)")

        # Send data packet immediately
        print(f"\n[3] Sending data packet...")
        packet = build_packet()
        print(f"    Packet length: {len(packet)} bytes")
        print(f"    Data segment (first 60 chars): {packet[6:66]}...")

        sock.sendall(packet.encode('ascii'))
        print(f"    Sent successfully")

        # Wait for response
        print(f"\n[4] Waiting for response...")
        sock.settimeout(3)

        response = sock.recv(1024)

        if response:
            print(f"    Received: {response.hex()}")
            print(f"    Length: {len(response)} bytes")

            if len(response) >= 9:
                status = response[2]
                print(f"\n[Analysis]")
                print(f"  Magic: {response[0:2].hex()}")
                print(f"  Status: 0x{status:02X} ({status})")

                if status == 0x01:
                    print(f"  [SUCCESS] Data upload ACCEPTED!")
                    return True
                elif status == 0x8D:
                    print(f"  [FAILED] Data upload REJECTED")
                    return False
                elif status == 0x03:
                    print(f"  [WARNING] Server returned initial status (0x03)")
                    print(f"            This means server didn't process data packet")
                    return False
                else:
                    print(f"  [UNKNOWN] Status code: 0x{status:02X}")
                    return False
        else:
            print(f"    No response")
            return False

        sock.close()

    except Exception as e:
        print(f"\n[Error] {e}")
        import traceback
        traceback.print_exc()
        return False


def test_with_delay():
    """Test: Add delay after connection before sending"""
    print("\n" + "="*70)
    print("TEST: Wait 1 Second After Connection, Then Send Data")
    print("="*70)

    HOST = "221.214.62.118"
    PORT = 20050

    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(10)

        print(f"\n[1] Connecting to {HOST}:{PORT}...")
        sock.connect((HOST, PORT))
        print(f"    Connected")

        print(f"\n[2] Waiting 1 second...")
        time.sleep(1)

        # Clear any pending data
        print(f"\n[3] Clearing receive buffer...")
        sock.settimeout(0.5)
        try:
            while True:
                msg = sock.recv(1024)
                if not msg:
                    break
                print(f"    Cleared: {msg.hex()}")
        except socket.timeout:
            print(f"    Buffer cleared")

        # Send data
        print(f"\n[4] Sending data packet...")
        packet = build_packet()
        sock.sendall(packet.encode('ascii'))
        print(f"    Sent {len(packet)} bytes")

        # Receive response
        print(f"\n[5] Waiting for response...")
        sock.settimeout(3)
        response = sock.recv(1024)

        if response:
            print(f"    Received: {response.hex()}")

            if len(response) >= 9:
                status = response[2]
                print(f"\n[Analysis] Status: 0x{status:02X}")

                if status == 0x01:
                    print(f"  [SUCCESS]!")
                    return True
                elif status == 0x8D:
                    print(f"  [FAILED]")
                    return False
                elif status == 0x03:
                    print(f"  [WARNING] Initial status - data not processed")
                    return False

        sock.close()
        return False

    except Exception as e:
        print(f"\n[Error] {e}")
        import traceback
        traceback.print_exc()
        return False


def main():
    """Run tests"""
    print("\n" + "="*70)
    print("Final Robust Handshake Test")
    print("="*70)
    print(f"Device MN: 865229085145869")
    print(f"Source Code: DE25IR006722")
    print("="*70)

    # Test 1
    result1 = test_no_handshake()
    time.sleep(2)

    # Test 2
    result2 = test_with_delay()

    # Summary
    print("\n" + "="*70)
    print("SUMMARY")
    print("="*70)
    print(f"Test 1 (No delay, ignore CM):     {'PASS' if result1 else 'FAIL'}")
    print(f"Test 2 (Delay 1s, clear buffer): {'PASS' if result2 else 'FAIL'}")

    if result1 or result2:
        print("\n[SUCCESS] Found working strategy!")
    else:
        print("\n[INFO] Both tests failed - server may have other issues")


if __name__ == "__main__":
    main()
