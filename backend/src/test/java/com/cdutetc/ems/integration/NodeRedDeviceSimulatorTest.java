package com.cdutetc.ems.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Node-RED设备模拟器集成测试
 * 测试通过Node-RED发送设备数据的完整流程
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Transactional
public class NodeRedDeviceSimulatorTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String baseUrl;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";
        jwtToken = obtainJwtToken();
    }

    /**
     * 获取JWT认证Token
     */
    private String obtainJwtToken() {
        String loginUrl = baseUrl + "/auth/login";

        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("username", "admin");
        loginRequest.put("password", "admin123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(loginRequest, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(loginUrl, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return (String) response.getBody().get("token");
            }
        } catch (Exception e) {
            // 登录失败，使用免认证的设备数据API
            System.out.println("JWT认证失败，将使用免认证API进行测试");
        }

        return null;
    }

    @Test
    @DisplayName("测试Node-RED辐射设备数据接收 - 已注册设备")
    void testNodeRedRadiationDeviceRegistered() {
        // 模拟Node-RED发送的辐射设备数据（RAD-001已注册设备）
        Map<String, Object> deviceData = new HashMap<>();
        deviceData.put("BDS", Map.of(
            "longitude", "11607.4321",
            "latitude", "3998.7654",
            "useful", 1,
            "UTC", "2025/12/22 14:30:00"
        ));
        deviceData.put("CPM", 35);
        deviceData.put("Batvolt", 4000);
        deviceData.put("signal", 4);
        deviceData.put("temperature", 22.5);
        deviceData.put("time", "2025/12/22 14:30:00");

        String url = baseUrl + "/device-data/radiation";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (jwtToken != null) {
            headers.setBearerAuth(jwtToken);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(deviceData, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        // 设备数据API应该成功接收（免认证）
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
        assertEquals("设备数据接收成功", response.getBody().get("message"));
    }

    @Test
    @DisplayName("测试Node-RED环境设备数据接收 - 已注册设备")
    void testNodeRedEnvironmentDeviceRegistered() {
        // 模拟Node-RED发送的环境设备数据（ENV-001已注册设备）
        Map<String, Object> deviceData = new HashMap<>();
        deviceData.put("src", 1);
        deviceData.put("CPM", 8);
        deviceData.put("temperature", 25.6);
        deviceData.put("wetness", 68.5);
        deviceData.put("windspeed", 3.2);
        deviceData.put("total", 85.3);
        deviceData.put("battery", 11.8);

        String url = baseUrl + "/device-data/environment";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (jwtToken != null) {
            headers.setBearerAuth(jwtToken);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(deviceData, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        // 设备数据API应该成功接收（免认证）
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
    }

    @Test
    @DisplayName("测试Node-RED辐射设备数据接收 - 未注册设备")
    void testNodeRedRadiationDeviceUnregistered() {
        // 模拟Node-RED发送的未注册辐射设备数据（RAD-999未注册设备）
        Map<String, Object> deviceData = new HashMap<>();
        deviceData.put("BDS", Map.of(
            "longitude", "11699.9999",
            "latitude", "3999.9999",
            "useful", 1,
            "UTC", "2025/12/22 14:30:00"
        ));
        deviceData.put("CPM", 75);
        deviceData.put("Batvolt", 3750);
        deviceData.put("signal", 3);
        deviceData.put("temperature", 18.7);
        deviceData.put("time", "2025/12/22 14:30:00");

        String url = baseUrl + "/device-data/radiation";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(deviceData, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        // 设备数据API应该接收数据，但设备标记为未注册
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @DisplayName("测试批量Node-RED设备数据接收")
    void testNodeRedBatchDeviceData() {
        // 发送多个设备的数据，模拟Node-RED批量发送场景
        Map<String, Object> radData = Map.of(
            "deviceId", "RAD-001",
            "deviceType", "RADIATION",
            "data", Map.of(
                "BDS", Map.of("longitude", "11607.4321", "latitude", "3998.7654", "useful", 1),
                "CPM", 42,
                "Batvolt", 3950,
                "time", "2025/12/22 14:35:00"
            )
        );

        Map<String, Object> envData = Map.of(
            "deviceId", "ENV-001",
            "deviceType", "ENVIRONMENT",
            "data", Map.of(
                "src", 1,
                "temperature", 26.8,
                "wetness", 72.3,
                "windspeed", 2.9,
                "total", 91.2,
                "battery", 12.1
            )
        );

        // 逐个发送设备数据
        String radUrl = baseUrl + "/device-data/radiation";
        String envUrl = baseUrl + "/device-data/environment";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        @SuppressWarnings("unchecked")
        HttpEntity<Map<String, Object>> radEntity = new HttpEntity<>((Map<String, Object>) radData.get("data"), headers);
        @SuppressWarnings("unchecked")
        HttpEntity<Map<String, Object>> envEntity = new HttpEntity<>((Map<String, Object>) envData.get("data"), headers);

        ResponseEntity<Map> radResponse = restTemplate.postForEntity(radUrl, radEntity, Map.class);
        ResponseEntity<Map> envResponse = restTemplate.postForEntity(envUrl, envEntity, Map.class);

        // 所有数据都应该成功接收
        assertEquals(HttpStatus.OK, radResponse.getStatusCode());
        assertEquals(HttpStatus.OK, envResponse.getStatusCode());
    }

    @Test
    @DisplayName("测试Node-RED设备数据格式验证")
    void testNodeRedDeviceDataValidation() {
        // 测试无效数据格式的处理
        Map<String, Object> invalidData = new HashMap<>();
        invalidData.put("invalid_field", "invalid_value");
        // 缺少必需字段

        String url = baseUrl + "/device-data/radiation";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(invalidData, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        // 应该返回错误状态
        assertTrue(response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError());
    }
}