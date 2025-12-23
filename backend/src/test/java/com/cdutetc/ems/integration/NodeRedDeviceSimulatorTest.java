package com.cdutetc.ems.integration;

import com.cdutetc.ems.entity.Company;
import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.entity.enums.DeviceType;
import com.cdutetc.ems.repository.CompanyRepository;
import com.cdutetc.ems.repository.DeviceRepository;
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
import org.springframework.test.context.TestPropertySource;
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
@TestPropertySource(properties = "ems.data.initialize=true")  // 为此测试启用数据初始化
@Transactional
public class NodeRedDeviceSimulatorTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private CompanyRepository companyRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private String baseUrl;
    private String jwtToken;
    private Long defaultCompanyId;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";
        jwtToken = obtainJwtToken();

        // 获取默认公司ID
        Company defaultCompany = companyRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("测试环境中没有找到公司数据"));
        defaultCompanyId = defaultCompany.getId();

        // 确保测试设备存在
        ensureTestDeviceExists("RAD001", DeviceType.RADIATION_MONITOR, "辐射监测仪001");
        ensureTestDeviceExists("ENV001", DeviceType.ENVIRONMENT_STATION, "环境监测站001");
    }

    /**
     * 确保测试设备存在
     */
    private void ensureTestDeviceExists(String deviceCode, DeviceType deviceType, String deviceName) {
        if (!deviceRepository.findByDeviceCode(deviceCode).isPresent()) {
            Company company = companyRepository.findById(defaultCompanyId)
                    .orElseThrow(() -> new IllegalStateException("默认公司不存在"));
            Device device = new Device();
            device.setDeviceCode(deviceCode);
            device.setDeviceName(deviceName);
            device.setDeviceType(deviceType);
            device.setCompany(company);
            deviceRepository.save(device);
        }
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
        // 模拟Node-RED发送的辐射设备数据（RAD001已注册设备）
        Map<String, Object> deviceData = new HashMap<>();
        deviceData.put("deviceCode", "RAD001");
        deviceData.put("rawData", "{\"device\":\"radiation_sensor\"}");
        deviceData.put("src", 1);
        deviceData.put("msgtype", 1);
        deviceData.put("cpm", 35.0);
        deviceData.put("batvolt", 4.0);
        deviceData.put("time", "2025/12/22 14:30:00");
        deviceData.put("trigger", 1);
        deviceData.put("multi", 1);
        deviceData.put("way", 1);
        deviceData.put("bdsLongitude", "116.074321");
        deviceData.put("bdsLatitude", "39.987654");
        deviceData.put("bdsUtc", "2025/12/22 14:30:00");
        deviceData.put("lbsLongitude", "116.074321");
        deviceData.put("lbsLatitude", "39.987654");
        deviceData.put("lbsUseful", 1);

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
        // 验证响应成功即可，不需要检查具体消息文本
        assertEquals(200, response.getBody().get("status"));
    }

    @Test
    @DisplayName("测试Node-RED环境设备数据接收 - 已注册设备")
    void testNodeRedEnvironmentDeviceRegistered() {
        // 模拟Node-RED发送的环境设备数据（ENV001已注册设备）
        Map<String, Object> deviceData = new HashMap<>();
        deviceData.put("deviceCode", "ENV001");
        deviceData.put("rawData", "{\"device\":\"environment_sensor\"}");
        deviceData.put("src", 1);
        deviceData.put("cpm", 8.0);
        deviceData.put("temperature", 25.6);
        deviceData.put("wetness", 68.5);
        deviceData.put("windspeed", 3.2);
        deviceData.put("total", 85.3);
        deviceData.put("battery", 4.1);

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
        deviceData.put("deviceCode", "RAD-999");
        deviceData.put("rawData", "{\"device\":\"radiation_sensor\"}");
        deviceData.put("src", 1);
        deviceData.put("msgtype", 1);
        deviceData.put("cpm", 75.0);
        deviceData.put("batvolt", 3.75);
        deviceData.put("time", "2025/12/22 14:30:00");
        deviceData.put("trigger", 1);
        deviceData.put("multi", 1);
        deviceData.put("way", 1);
        deviceData.put("bdsLongitude", "116.999999");
        deviceData.put("bdsLatitude", "39.999999");
        deviceData.put("bdsUtc", "2025/12/22 14:30:00");
        deviceData.put("lbsLongitude", "116.999999");
        deviceData.put("lbsLatitude", "39.999999");
        deviceData.put("lbsUseful", 1);

        String url = baseUrl + "/device-data/radiation";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(deviceData, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

        // 设备不存在时应该返回400错误
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody().containsKey("message"));
    }

    @Test
    @DisplayName("测试批量Node-RED设备数据接收")
    void testNodeRedBatchDeviceData() {
        // 发送多个设备的数据，模拟Node-RED批量发送场景
        Map<String, Object> radData = new HashMap<>();
        radData.put("deviceCode", "RAD001");
        radData.put("rawData", "{\"device\":\"radiation_sensor\"}");
        radData.put("src", 1);
        radData.put("msgtype", 1);
        radData.put("cpm", 42.0);
        radData.put("batvolt", 3.95);
        radData.put("time", "2025/12/22 14:35:00");
        radData.put("trigger", 1);
        radData.put("multi", 1);
        radData.put("way", 1);
        radData.put("bdsLongitude", "116.074321");
        radData.put("bdsLatitude", "39.987654");
        radData.put("bdsUtc", "2025/12/22 14:35:00");
        radData.put("lbsLongitude", "116.074321");
        radData.put("lbsLatitude", "39.987654");
        radData.put("lbsUseful", 1);

        Map<String, Object> envData = new HashMap<>();
        envData.put("deviceCode", "ENV001");
        envData.put("rawData", "{\"device\":\"environment_sensor\"}");
        envData.put("src", 1);
        envData.put("cpm", 15.0);
        envData.put("temperature", 26.8);
        envData.put("wetness", 72.3);
        envData.put("windspeed", 2.9);
        envData.put("total", 91.2);
        envData.put("battery", 4.2);

        // 逐个发送设备数据
        String radUrl = baseUrl + "/device-data/radiation";
        String envUrl = baseUrl + "/device-data/environment";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 修复：直接传递数据对象，而不是调用.get("data")
        HttpEntity<Map<String, Object>> radEntity = new HttpEntity<>(radData, headers);
        HttpEntity<Map<String, Object>> envEntity = new HttpEntity<>(envData, headers);

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