#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Detailed Handshake Analysis for New Server
Test different handshake strategies
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


def build_data_packet():
    """Build standard HJ/T212 data packet"""
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


def test_handshake_strategy(host, port, strategy_name, handshake_func):
    """Test a specific handshake strategy"""
    print(f"\n{'='*70}")
    print(f"[Strategy] {strategy_name}")
    print(f"{'='*70}")

    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(10)
        sock.connect((host, port))

        print(f"[1] Connected to {host}:{port}")

        # Receive initial message
        sock.settimeout(2)
        initial_msg = sock.recv(1024)

        if not initial_msg:
            print(f"[Error] No initial message from server")
            sock.close()
            return False

        print(f"[2] Server initial message:")
        print(f"    Hex: {initial_msg.hex()}")
        print(f"    Length: {len(initial_msg)} bytes")

        # Parse initial message structure
        if len(initial_msg) >= 9:
            print(f"\n[Structure Analysis]")
            print(f"    Bytes 0-1 (Magic):   {initial_msg[0:2].hex()} = '{initial_msg[0:2].decode('ascii', errors='ignore')}'")
            print(f"    Byte  2   (Status):  0x{initial_msg[2]:02X} = {initial_msg[2]} (decimal)")
            print(f"    Byte  3   (Type):    0x{initial_msg[3]:02X} = {initial_msg[3]} (decimal)")
            print(f"    Byte  4   (SubType): 0x{initial_msg[4]:02X} = {initial_msg[4]} (decimal)")
            print(f"    Bytes 5-8 (Data/Padding): {initial_msg[5:9].hex()}")

        # Execute handshake strategy
        print(f"\n[3] Executing handshake...")
        handshake_response = handshake_func(initial_msg)

        if handshake_response:
            print(f"    Sending: {handshake_response.hex()}")
            sock.sendall(handshake_response)

            # Wait for handshake response
            sock.settimeout(2)
            try:
                handshake_ack = sock.recv(1024)
                if handshake_ack:
                    print(f"    Server ACK: {handshake_ack.hex()}")

                    if len(handshake_ack) >= 9:
                        print(f"\n[Handshake ACK Structure]")
                        print(f"    Bytes 0-1 (Magic):   {handshake_ack[0:2].hex()}")
                        print(f"    Byte  2   (Status):  0x{handshake_ack[2]:02X} = {handshake_ack[2]}")

                        # Check if handshake was successful
                        if handshake_ack[2] == 0x01:
                            print(f"    [SUCCESS] Handshake accepted (status=0x01)")
                        elif handshake_ack[2] == 0x00:
                            print(f"    [INFO] Handshake status=0x00")
                        else:
                            print(f"    [WARNING] Unknown status: 0x{handshake_ack[2]:02X}")
            except socket.timeout:
                print(f"    [Timeout] No handshake ACK")

        # Send data packet
        print(f"\n[4] Sending data packet...")
        packet = build_data_packet()
        sock.sendall(packet.encode('ascii'))
        print(f"    Sent: {len(packet)} bytes")

        # Receive response
        print(f"\n[5] Waiting for data response...")
        sock.settimeout(5)
        response = sock.recv(1024)

        if response:
            print(f"    Response: {response.hex()}")
            print(f"    Length: {len(response)} bytes")

            if len(response) >= 9:
                status_byte = response[2]
                print(f"\n[Response Status]")
                print(f"    Status byte: 0x{status_byte:02X} = {status_byte} (decimal)")

                # Interpret status
                if status_byte == 0x8D:
                    print(f"    [Result] Status 0x8D - Possible error or rejection")
                elif status_byte == 0x01:
                    print(f"    [Result] Status 0x01 - Success")
                elif status_byte == 0x00:
                    print(f"    [Result] Status 0x00 - Neutral/Unknown")
                else:
                    print(f"    [Result] Unknown status code")

        sock.close()
        return True

    except Exception as e:
        print(f"\n[Error] {e}")
        import traceback
        traceback.print_exc()
        return False


def main():
    """Test different handshake strategies"""
    HOST = "221.214.62.118"
    PORT = 20050

    print("\n" + "="*70)
    print("Handshake Strategy Analysis")
    print("="*70)
    print(f"Server: {HOST}:{PORT}")
    print("="*70)

    # Strategy 1: Simple CM + CRLF
    def strategy_1(initial_msg):
        """Simple 'CM\\r\\n' response"""
        return b"CM\r\n"

    # Strategy 2: Echo back the exact initial message
    def strategy_2(initial_msg):
        """Echo server's message back"""
        return initial_msg

    # Strategy 3: CM with status byte 0x01
    def strategy_3(initial_msg):
        """Construct CM message with status=0x01"""
        # CM + 0x01 + 0x02 + 0x02 + padding
        return bytes([0x43, 0x4D, 0x01, 0x02, 0x02, 0x00, 0x00, 0x00, 0x00])

    # Strategy 4: No handshake (send data immediately)
    def strategy_4(initial_msg):
        """Don't respond to handshake, send data immediately"""
        return None

    # Strategy 5: CM with CRLF but include full binary frame
    def strategy_5(initial_msg):
        """Send CM binary frame with CRLF"""
        frame = bytes([0x43, 0x4D, 0x01, 0x02, 0x02, 0x00, 0x00, 0x00, 0x00])
        return frame + b"\r\n"

    # Test all strategies
    strategies = [
        ("Strategy 1: Simple 'CM\\r\\n'", strategy_1),
        ("Strategy 2: Echo initial message", strategy_2),
        ("Strategy 3: Binary CM frame (status=0x01)", strategy_3),
        ("Strategy 4: No handshake", strategy_4),
        ("Strategy 5: Binary CM + CRLF", strategy_5),
    ]

    for name, func in strategies:
        test_handshake_strategy(HOST, PORT, name, func)
        time.sleep(1)  # Wait between tests

    print("\n" + "="*70)
    print("Analysis completed")
    print("="*70)

    print("""
Summary of TCP Handshake:

Standard TCP Programming:
1. TCP 3-way handshake is handled by OS/network stack (invisible to application)
2. Application-layer handshake is PROTOCOL-SPECIFIC
3. There is NO "standard" application handshake for TCP

For This Server:
1. Server sends 9-byte binary frame after TCP connection
2. Structure: CM (2B) + Status (1B) + Type (1B) + SubType (1B) + Padding (4B)
3. Client must respond with similar frame
4. Current best response: Simple 'CM\\r\\n' (4 bytes)

Note: This is NOT standard HJ/T212-2005 protocol!
This appears to be a vendor-specific binary protocol layer.
    """)


if __name__ == "__main__":
    main()
