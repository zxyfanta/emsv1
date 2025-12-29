#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Shandong Protocol Test with Handshake
Test responding to server's CM message
"""

import socket
import time
from datetime import datetime


class ShandongHandshakeTester:
    """Shandong Protocol Tester with Handshake Support"""

    def __init__(self, host, port):
        self.host = host
        self.port = port

    def calculate_crc16(self, data):
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

    def build_data_packet(self):
        """Build test data packet"""
        device_mn = "865229085145869"  # SIM card number = Device MN
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
        crc = self.calculate_crc16(data_segment)

        return f"##{data_length}{data_segment}{crc}\r\n", data_segment

    def test_with_handshake(self):
        """Test with handshake - respond to server's CM message"""
        print(f"\n{'='*60}")
        print(f"[Test] Shandong Protocol with Handshake")
        print(f"{'='*60}")
        print(f"[Config] Server: {self.host}:{self.port}")

        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.settimeout(10)
            print(f"\n[Step 1] Connecting to server...")
            sock.connect((self.host, self.port))
            print(f"[OK] Connected")

            # Step 1: Receive initial message
            print(f"\n[Step 2] Waiting for server initial message...")
            sock.settimeout(2)
            initial_msg = sock.recv(1024)

            print(f"[Receive] Initial message from server:")
            print(f"  Length: {len(initial_msg)} bytes")
            print(f"  Hex: {' '.join(f'{b:02X}' for b in initial_msg)}")

            if len(initial_msg) >= 2:
                ascii_part = initial_msg[:2].decode('ascii', errors='ignore')
                print(f"  ASCII: {ascii_part}")

                # If server sends "CM", try to respond
                if ascii_part == "CM":
                    print(f"\n[Step 3] Server sent 'CM', attempting handshake response...")

                    # Try different handshake responses
                    handshake_attempts = [
                        b"CM\r\n",  # Simple CM response
                        b"CM\x00\x00\x00\x00\x00\x00",  # CM + 6 null bytes
                        b"\x43\x4D\x01\x02\x02\x00\x00\x00\x00",  # Binary CM response
                    ]

                    for i, response in enumerate(handshake_attempts, 1):
                        print(f"\n  [Attempt {i}] Sending: {response.hex()}")
                        sock.sendall(response)

                        # Wait a bit
                        time.sleep(0.5)

                        # Try to receive response to handshake
                        try:
                            handshake_response = sock.recv(1024)
                            if handshake_response:
                                print(f"  [Response] Server replied: {handshake_response.hex()}")
                                break
                        except socket.timeout:
                            print(f"  [Timeout] No response to handshake attempt {i}")
                            continue

            # Step 2: Send data packet
            print(f"\n[Step 4] Sending data packet...")
            packet, data_segment = self.build_data_packet()

            print(f"  Data packet length: {len(packet)} bytes")
            print(f"  Data segment: {data_segment[:50]}...")

            sock.sendall(packet.encode('ascii'))
            print(f"[OK] Data packet sent")

            # Step 3: Receive response
            print(f"\n[Step 5] Waiting for data response...")
            sock.settimeout(5)

            response_data = b""
            while True:
                try:
                    chunk = sock.recv(1024)
                    if not chunk:
                        break
                    response_data += chunk
                    if b"\r\n" in response_data or len(response_data) > 30:
                        break
                except socket.timeout:
                    if response_data:
                        break
                    raise

            if response_data:
                print(f"\n[Response] Server response:")
                print(f"  Length: {len(response_data)} bytes")
                print(f"  Hex: {' '.join(f'{b:02X}' for b in response_data)}")

                # Try to decode as ASCII
                try:
                    ascii_response = response_data.decode('ascii', errors='ignore').strip()
                    print(f"  ASCII: {ascii_response}")

                    # Check for standard protocol responses
                    if "ST=91" in ascii_response:
                        print(f"\n[SUCCESS] Data upload accepted (ST=91)")
                        return True
                    elif "ST=92" in ascii_response:
                        print(f"\n[FAILURE] Data upload rejected (ST=92)")
                        return False
                except:
                    pass

                # If binary response, analyze structure
                print(f"\n[Analysis] Binary response structure:")
                for i in range(0, len(response_data), 8):
                    chunk = response_data[i:i+8]
                    chunk_hex = ' '.join(f'{b:02X}' for b in chunk)
                    try:
                        chunk_ascii = ''.join(chr(b) if 32 <= b < 127 else '.' for b in chunk)
                    except:
                        chunk_ascii = '???'
                    print(f"  Bytes {i:2d}-{i+len(chunk)-1:2d}: {chunk_hex:<20s} | {chunk_ascii}")

            sock.close()
            return True

        except Exception as e:
            print(f"\n[Error] Test failed: {e}")
            import traceback
            traceback.print_exc()
            return False

    def test_raw_tcp(self):
        """Test with raw TCP - send data immediately without waiting"""
        print(f"\n{'='*60}")
        print(f"[Test] Raw TCP - Send Immediately")
        print(f"{'='*60}")

        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.settimeout(10)
            print(f"\n[Connecting] to {self.host}:{self.port}...")
            sock.connect((self.host, self.port))
            print(f"[OK] Connected")

            # Send data immediately without receiving initial message
            print(f"\n[Sending] Data packet immediately...")
            packet, data_segment = self.build_data_packet()

            sock.sendall(packet.encode('ascii'))
            print(f"[OK] Sent {len(packet)} bytes")

            # Receive response
            print(f"\n[Receiving] Response...")
            sock.settimeout(5)
            response_data = sock.recv(1024)

            if response_data:
                print(f"  Length: {len(response_data)} bytes")
                print(f"  Hex: {' '.join(f'{b:02X}' for b in response_data)}")
                print(f"  ASCII: {response_data.decode('ascii', errors='ignore')}")

            sock.close()
            return True

        except Exception as e:
            print(f"\n[Error] {e}")
            return False


def main():
    """Main function"""
    HOST = "221.214.62.118"
    PORT = 20050

    tester = ShandongHandshakeTester(HOST, PORT)

    print("\n" + "="*60)
    print("Shandong Protocol Handshake Test")
    print("="*60)
    print(f"Server: {HOST}:{PORT}")
    print(f"Device MN (SIM): 865229085145869")
    print(f"Source Code: DE25IR006722")
    print("="*60)

    # Test 1: With handshake
    print("\n\n### TEST 1: With Handshake Response ###")
    tester.test_with_handshake()

    # Test 2: Raw TCP
    print("\n\n### TEST 2: Raw TCP (Send Immediately) ###")
    tester.test_raw_tcp()

    print("\n\n" + "="*60)
    print("Test completed")
    print("="*60)


if __name__ == "__main__":
    main()
