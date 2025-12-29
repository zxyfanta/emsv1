#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Shandong Protocol Quick Test Script
For fast server connection and basic functionality validation
"""

import socket
import time
from datetime import datetime


class ShandongProtocolTester:
    """Shandong Protocol Quick Tester"""

    def __init__(self, host, port):
        self.host = host
        self.port = port

    def test_tcp_connection(self):
        """Test TCP connection"""
        print(f"\n{'='*50}")
        print(f"[Test] TCP Connection: {self.host}:{self.port}")
        print(f"{'='*50}")

        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.settimeout(5)
            start_time = time.time()
            sock.connect((self.host, self.port))
            elapsed = (time.time() - start_time) * 1000

            print(f"[OK] Connection successful!")
            print(f"[Info] Latency: {elapsed:.2f}ms")

            # Try to receive initial message (if any)
            try:
                sock.settimeout(1)
                data = sock.recv(1024)
                if data:
                    print(f"[Receive] Server message: {data.decode('ascii', errors='ignore')}")
            except:
                print("[Info] No initial message from server")

            sock.close()
            return True

        except socket.timeout:
            print(f"[Error] Connection timeout (5 seconds)")
            return False
        except ConnectionRefusedError:
            print(f"[Error] Connection refused - Server may not be running")
            return False
        except Exception as e:
            print(f"[Error] Connection failed: {e}")
            return False

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
        # Device information (SIM card number = Device MN)
        device_mn = "865229085145869"  # SIM card number = Device MN (15 digits)
        password = "123456"

        # Generate request number (QN)
        # Format: YYYYMMDDHHMMSSZZZZS (4-digit random number + flag bit)
        import random
        qn_base = datetime.now().strftime("%Y%m%d%H%M%S")
        qn_random = random.randint(0, 9999)
        qn = f"{qn_base}{qn_random:04d}1"

        # Build data segment - using real device information
        data_time = datetime.now().strftime("%Y%m%d%H%M%S")
        cp_content = (
            f"MN={device_mn};"
            f"Ma=002162;"  # Detector number: 2162 (padded to 6 digits)
            f"Rno=DE25IR006722;"  # Source code (12 digits, CAN contain letters)
            f"Xtype=02;"  # Source type: 02=Class II
            f"LastAct=5.530E012;"  # Original activity: 5.53E+12
            f"NowAct=3.270E012;"  # Current activity: 3.27E+12
            f"SourceTime=20250703;"  # Production date: 2025-07-03
            f"DataTime={data_time};"  # Data time (YYYYMMDDHHMMSS)
            f"LONG=12102.1465;"  # GPS longitude: 121.035775 deg = 121 deg 02.1465 min
            f"LAT=3740.5073;"  # GPS latitude: 37.675122 deg = 37 deg 40.5073 min
            f"Xvalue=1000000.000;"  # Dose rate
            f"Thres=100000.000;"  # Threshold
            f"AlertType=01;"  # Alert type: 01=Source loss (adjustable)
            f"BattChar=1000.0;"  # Battery level
            f"Sig=1"  # GPS status: 1=Valid
        )

        # Build complete data packet (according to protocol document and Java implementation)
        # Include QN field
        data_segment = f"QN={qn};ST=61;CN=3051;PW={password};CP=&&{cp_content}&&"

        # Calculate data segment length (4-digit decimal)
        data_length = f"{len(data_segment):04d}"

        # Calculate CRC (checksum of data segment)
        crc = self.calculate_crc16(data_segment)

        # Add header, data segment length, data segment, CRC and tail
        return f"##{data_length}{data_segment}{crc}\r\n", {
            'qn': qn,
            'st': '61',
            'cn': '3051',
            'password': password,
            'device_mn': device_mn,
            'data_time': data_time,
            'data_length': data_length,
            'crc': crc,
            'cp_content': cp_content
        }

    def print_data_details(self, details):
        """Print detailed explanation of data packet"""
        print(f"\n{'='*50}")
        print(f"[Detail] Data Field Description")
        print(f"{'='*50}")

        # Communication packet structure
        print(f"\n[Packet Structure]")
        print(f"  Header          : ##  (Fixed identifier)")
        print(f"  Data length     : {details['data_length']}  (Character count of data segment)")
        print(f"  CRC checksum    : {details['crc']}  (Data segment checksum)")
        print(f"  Tail            : <CR><LF>  (Carriage return + Line feed)")

        # Data segment fields
        print(f"\n[Data Segment Fields]")

        print(f"\n  1. Request Number (QN)")
        print(f"     Value: {details['qn']}")
        print(f"     Description: Timestamp to seconds + 4-digit random + flag")
        print(f"     Format: YYYYMMDDHHMMSSZZZZS")

        print(f"\n  2. System Type (ST)")
        print(f"     Value: {details['st']}")
        print(f"     Description: Field device number (Radiation source monitoring device)")

        print(f"\n  3. Command Number (CN)")
        print(f"     Value: {details['cn']}")
        print(f"     Description: Real-time data upload command")

        print(f"\n  4. Password (PW)")
        print(f"     Value: {details['password']}")
        print(f"     Description: Device access password, 6 digits")

        # Data content (CP field)
        print(f"\n[Data Content Fields (CP)]")
        cp = details['cp_content']

        # Parse and display each field
        fields = cp.split(';')
        field_descriptions = {
            'MN': ('Device Number', '14 digits, unique identifier for field monitoring device'),
            'Ma': ('Detector Number', '6 digits, identifier for detection equipment paired with source'),
            'Rno': ('Source Code', '12 digits, unique identifier for single radiation source'),
            'Xtype': ('Source Type', '2 digits: 01=Class I, 02=Class II, 03=Class III, 04=Class IV, 05=Class V'),
            'LastAct': ('Original Activity', 'Scientific notation, e.g.: 2.700E004'),
            'NowAct': ('Current Activity', 'Scientific notation, e.g.: 1.300E004'),
            'SourceTime': ('Production Date', '8 digits, format: YYYYMMDD'),
            'DataTime': ('Data Time', '14 digits, format: YYYYMMDDHHMMSS'),
            'LONG': ('GPS Longitude', 'Degree-minute format, e.g.: 11225.1333'),
            'LAT': ('GPS Latitude', 'Degree-minute format, e.g.: 3705.4300'),
            'Xvalue': ('Dose Rate', 'Floating point, unit: nGy/h'),
            'Thres': ('Threshold', 'Floating point, alarm threshold'),
            'AlertType': ('Alert Type', '2 digits: 01=Source loss, 02=Count blocked, 03=Undervoltage, 04=Low count, 05=Communication fault'),
            'BattChar': ('Battery Level', 'Floating point, unit: V'),
            'Sig': ('GPS Flag', '1 digit: 1=Valid')
        }

        for i, field in enumerate(fields, 1):
            if '=' in field:
                key, value = field.split('=', 1)
                if key in field_descriptions:
                    name, desc = field_descriptions[key]
                    print(f"\n  {i+4}. {name} ({key})")
                    print(f"     Value: {value}")
                    print(f"     Description: {desc}")

    def test_data_send(self):
        """Test data sending"""
        print(f"\n{'='*50}")
        print(f"[Test] Data Sending")
        print(f"{'='*50}")

        try:
            sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            sock.settimeout(10)
            sock.connect((self.host, self.port))

            # IMPORTANT: Clear the initial "CM" message from server
            # But DO NOT respond to it - just ignore and send data directly
            try:
                sock.settimeout(1)
                initial_msg = sock.recv(1024)
                if initial_msg:
                    print(f"[Info] Server sent initial message: {initial_msg.hex()}")
                    print(f"[Note] Ignoring initial message (no handshake needed)")
            except socket.timeout:
                print(f"[Info] No initial message from server")

            # Build data packet
            packet, details = self.build_data_packet()

            # Print detailed field description
            self.print_data_details(details)

            # Print complete data packet
            print(f"\n{'='*50}")
            print(f"[Complete Data Packet]")
            print(f"{'='*50}")
            print(f"{packet.strip()}")
            print(f"{'='*50}")

            # Send data directly (no handshake response)
            sock.sendall(packet.encode('ascii'))
            print(f"[OK] Data sent successfully ({len(packet)} bytes)")

            # Receive response
            print(f"\n[Waiting] Server response...")
            sock.settimeout(5)

            response_data = sock.recv(1024)

            if response_data:
                print(f"\n[Raw Data] Complete server response:")
                print(f"{'='*50}")
                print(f"Byte length: {len(response_data)} bytes")
                print(f"\nHexadecimal:")
                print(f"{' '.join(f'{b:02X}' for b in response_data)}")

                # Analyze binary response structure
                if len(response_data) >= 9 and response_data[0:2] == b'CM':
                    print(f"\n[Binary Response Analysis]")
                    print(f"  Magic: {response_data[0:2].hex()} (CM)")
                    print(f"  Status: 0x{response_data[2]:02X} ({response_data[2]} decimal)")

                    if response_data[2] == 0x01:
                        print(f"  [SUCCESS] Status 0x01 = Data upload ACCEPTED!")
                    elif response_data[2] == 0x8D:
                        print(f"  [FAILURE] Status 0x8D = Data upload REJECTED")
                    else:
                        print(f"  [UNKNOWN] Status 0x{response_data[2]:02X}")
                else:
                    # Try to decode as text
                    print(f"\nDecimal:")
                    print(f"{' '.join(f'{b:03d}' for b in response_data)}")
                    print(f"\nASCII decode:")
                    try:
                        ascii_str = response_data.decode('ascii', errors='replace')
                        print(f"{ascii_str}")
                    except:
                        print("(Cannot decode as ASCII)")

                    # Try UTF-8 decode
                    try:
                        response = response_data.decode('utf-8').strip()
                        print(f"\n[Parse] Decoded response:")
                        print(f"{response}")

                        if "ST=91" in response:
                            print(f"\n[Result] SUCCESS (ST=91)")
                        elif "ST=92" in response:
                            print(f"\n[Result] FAILURE (ST=92)")
                    except:
                        pass

                print(f"{'='*50}")
                return True

            else:
                print(f"\n[Error] No response received")
                return False

            sock.close()

        except Exception as e:
            print(f"\n[Error] Data sending failed: {e}")
            import traceback
            traceback.print_exc()
            return False

    def run_all_tests(self):
        """Run all tests"""
        print("\n" + "="*50)
        print("[Test] Shandong Protocol Quick Test")
        print("="*50)
        print(f"[Config] Server: {self.host}:{self.port}")

        # Test 1: TCP connection
        result1 = self.test_tcp_connection()

        if result1:
            # Test 2: Data sending
            result2 = self.test_data_send()

            # Summary
            print(f"\n{'='*50}")
            print(f"[Result] Test Results")
            print(f"{'='*50}")
            print(f"TCP Connection: {'[OK] PASS' if result1 else '[FAIL] FAIL'}")
            print(f"Data Sending: {'[OK] PASS' if result2 else '[FAIL] FAIL'}")

            if result1 and result2:
                print(f"\n[Success] Server is working normally!")
            else:
                print(f"\n[Failure] Server has issues, please check configuration")
        else:
            print(f"\n[Error] Cannot connect to server, please check:")
            print(f"   1. Network connection is normal")
            print(f"   2. Server address and port are correct")
            print(f"   3. Server is running")


def main():
    """Main function"""
    # Configuration parameters
    HOST = "221.214.62.118"
    PORT = 20050

    # Create tester and run tests
    tester = ShandongProtocolTester(HOST, PORT)
    tester.run_all_tests()

    print(f"\n[Hint]")
    print(f"   - Server address can be modified in config file")
    print(f"   - Current SIM card number (Device MN): 865229085145869")
    print(f"   - Source code (Rno): DE25IR006722 (CAN contain letters)")
    print(f"   - Access password: 123456")


if __name__ == "__main__":
    main()
