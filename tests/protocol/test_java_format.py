#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Test Java-format packet (without data length field)
According to HJT212ProtocolService.java implementation
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


def build_packet_java_format():
    """Build packet in Java format (WITHOUT data length field)"""
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

    # Java format: ## + data_segment + CRC + \r\n
    # NO data length field!
    data_segment = f"QN={qn};ST=61;CN=3051;PW={password};CP=&&{cp_content}&&"
    crc = calculate_crc16(data_segment)

    return f"##{data_segment}{crc}\r\n", data_segment


def build_packet_standard_format():
    """Build packet in standard format (WITH data length field)"""
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

    # Standard format: ## + length + data_segment + CRC + \r\n
    data_segment = f"QN={qn};ST=61;CN=3051;PW={password};CP=&&{cp_content}&&"
    data_length = f"{len(data_segment):04d}"
    crc = calculate_crc16(data_segment)

    return f"##{data_length}{data_segment}{crc}\r\n", data_segment


def test_format(format_name, build_packet_func, host, port):
    """Test a specific packet format"""
    print(f"\n{'='*60}")
    print(f"[Test] {format_name}")
    print(f"{'='*60}")

    try:
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(10)
        sock.connect((host, port))

        # Receive and respond to initial message
        sock.settimeout(2)
        initial_msg = sock.recv(1024)

        if initial_msg and b"CM" in initial_msg:
            print(f"[Handshake] Server sent: {initial_msg.hex()}")
            # Respond with simple CM
            sock.sendall(b"CM\r\n")
            print(f"[Handshake] Responded with: CM\\r\\n")

            # Wait for handshake response
            try:
                handshake_resp = sock.recv(1024)
                print(f"[Handshake] Server replied: {handshake_resp.hex()}")
            except socket.timeout:
                print(f"[Handshake] No response (timeout)")

        # Build and send packet
        packet, data_segment = build_packet_func()

        print(f"\n[Packet] Format: {format_name}")
        print(f"  Total length: {len(packet)} bytes")
        print(f"  Data segment: {data_segment[:60]}...")
        print(f"  Full packet (first 100 chars): {packet[:100]}...")

        sock.sendall(packet.encode('ascii'))
        print(f"[Send] Packet sent successfully")

        # Receive response
        sock.settimeout(5)
        response_data = sock.recv(1024)

        if response_data:
            print(f"\n[Response] Server response:")
            print(f"  Length: {len(response_data)} bytes")
            print(f"  Hex: {response_data.hex()}")

            # Try to decode
            try:
                ascii_response = response_data.decode('ascii', errors='ignore').strip()
                print(f"  ASCII: {ascii_response}")

                if "ST=91" in ascii_response:
                    print(f"\n  [SUCCESS] Data upload ACCEPTED (ST=91)")
                    return True
                elif "ST=92" in ascii_response:
                    print(f"\n  [FAILURE] Data upload REJECTED (ST=92)")
                    return False
            except:
                pass

            # If binary, analyze
            if b"ST=" not in response_data:
                print(f"  Binary response - analyzing structure:")
                chunks = [response_data[i:i+8] for i in range(0, len(response_data), 8)]
                for i, chunk in enumerate(chunks):
                    hex_str = ' '.join(f'{b:02X}' for b in chunk)
                    print(f"    Chunk {i+1}: {hex_str}")

        sock.close()
        return False

    except Exception as e:
        print(f"\n[Error] Test failed: {e}")
        import traceback
        traceback.print_exc()
        return False


def main():
    """Main function"""
    HOST = "221.214.62.118"
    PORT = 20050

    print("\n" + "="*60)
    print("Packet Format Comparison Test")
    print("="*60)
    print(f"Server: {HOST}:{PORT}")
    print(f"Device MN (SIM): 865229085145869")
    print("="*60)

    # Test 1: Java format (without length)
    result1 = test_format(
        "Java Format (NO length field)",
        build_packet_java_format,
        HOST,
        PORT
    )

    # Test 2: Standard format (with length)
    result2 = test_format(
        "Standard Format (WITH length field)",
        build_packet_standard_format,
        HOST,
        PORT
    )

    # Summary
    print(f"\n\n{'='*60}")
    print(f"[Summary] Test Results")
    print(f"{'='*60}")
    print(f"Java Format (no length):    {'ACCEPTED' if result1 else 'REJECTED/BINARY'}")
    print(f"Standard Format (length):   {'ACCEPTED' if result2 else 'REJECTED/BINARY'}")

    if not result1 and not result2:
        print(f"\n[Note] Server returns binary response for both formats")
        print(f"      This server may not be standard HJ/T212 protocol")


if __name__ == "__main__":
    main()
