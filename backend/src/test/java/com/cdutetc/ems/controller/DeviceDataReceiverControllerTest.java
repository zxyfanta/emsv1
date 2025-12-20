package com.cdutetc.ems.controller;

import com.cdutetc.ems.BaseIntegrationTest;
import com.cdutetc.ems.entity.Company;
import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.entity.enums.CompanyStatus;
import com.cdutetc.ems.entity.enums.DeviceType;
import com.cdutetc.ems.repository.CompanyRepository;
import com.cdutetc.ems.repository.DeviceRepository;
import com.cdutetc.ems.util.TestDataBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 设备数据接收控制器集成测试
 */
class DeviceDataReceiverControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private Company testCompany;
    private Device radiationDevice;
    private Device environmentDevice;

    @BeforeEach
    void setUpTestData() {
        // 创建测试企业
        testCompany = TestDataBuilder.buildTestCompany();
        testCompany.setStatus(CompanyStatus.ACTIVE);
        testCompany = companyRepository.save(testCompany);

        // 创建测试辐射设备
        radiationDevice = TestDataBuilder.buildTestRadiationDevice(testCompany.getId());
        radiationDevice = deviceRepository.save(radiationDevice);

        // 创建测试环境设备
        environmentDevice = TestDataBuilder.buildTestEnvironmentDevice(testCompany.getId());
        environmentDevice = deviceRepository.save(environmentDevice);
    }

    @Test
    @DisplayName("接收辐射设备数据 - 成功")
    void testReceiveRadiationData_Success() throws Exception {
        var request = TestDataBuilder.buildRadiationDataRequest(radiationDevice.getDeviceCode());

        mockMvc.perform(post("/api/device-data/radiation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.message").value("数据接收成功"))
                .andExpect(jsonPath("$.data.deviceCode").value(radiationDevice.getDeviceCode()))
                .andExpect(jsonPath("$.data.deviceId").value(notNullValue()))
                .andExpect(jsonPath("$.data.receiveTime").value(notNullValue()));
    }

    @Test
    @DisplayName("接收辐射设备数据 - 设备不存在")
    void testReceiveRadiationData_DeviceNotFound() throws Exception {
        var request = TestDataBuilder.buildRadiationDataRequest("NONEXISTENT-DEVICE");

        mockMvc.perform(post("/api/device-data/radiation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("设备不存在: NONEXISTENT-DEVICE"));
    }

    @Test
    @DisplayName("接收辐射设备数据 - 数据验证失败")
    void testReceiveRadiationData_ValidationError() throws Exception {
        // 创建缺少必填字段的请求
        var request = TestDataBuilder.buildRadiationDataRequest(radiationDevice.getDeviceCode());
        request.setDeviceCode(null); // 设备编码为空

        mockMvc.perform(post("/api/device-data/radiation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("接收环境设备数据 - 成功")
    void testReceiveEnvironmentData_Success() throws Exception {
        var request = TestDataBuilder.buildEnvironmentDataRequest(environmentDevice.getDeviceCode());

        mockMvc.perform(post("/api/device-data/environment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.message").value("数据接收成功"))
                .andExpect(jsonPath("$.data.deviceCode").value(environmentDevice.getDeviceCode()))
                .andExpect(jsonPath("$.data.deviceId").value(notNullValue()))
                .andExpect(jsonPath("$.data.receiveTime").value(notNullValue()));
    }

    @Test
    @DisplayName("接收环境设备数据 - 设备不存在")
    void testReceiveEnvironmentData_DeviceNotFound() throws Exception {
        var request = TestDataBuilder.buildEnvironmentDataRequest("NONEXISTENT-DEVICE");

        mockMvc.perform(post("/api/device-data/environment")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("设备不存在: NONEXISTENT-DEVICE"));
    }

    @Test
    @DisplayName("批量接收辐射设备数据 - 成功")
    void testReceiveRadiationDataBatch_Success() throws Exception {
        var request1 = TestDataBuilder.buildRadiationDataRequest(radiationDevice.getDeviceCode());
        var request2 = TestDataBuilder.buildRadiationDataRequest(radiationDevice.getDeviceCode());
        request2.setCpm(15.5);

        var requests = java.util.List.of(request1, request2);

        mockMvc.perform(post("/api/device-data/radiation/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("批量接收环境设备数据 - 成功")
    void testReceiveEnvironmentDataBatch_Success() throws Exception {
        var request1 = TestDataBuilder.buildEnvironmentDataRequest(environmentDevice.getDeviceCode());
        var request2 = TestDataBuilder.buildEnvironmentDataRequest(environmentDevice.getDeviceCode());
        request2.setTemperature(26.0);

        var requests = java.util.List.of(request1, request2);

        mockMvc.perform(post("/api/device-data/environment/batch")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requests)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasSize(2)));
    }

    @Test
    @DisplayName("API访问不需要认证")
    void testApiAccessWithoutAuthentication() throws Exception {
        // 验证设备数据接收API不需要JWT认证
        var request = TestDataBuilder.buildRadiationDataRequest(radiationDevice.getDeviceCode());

        mockMvc.perform(post("/api/device-data/radiation")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()); // 应该成功访问，不需要认证
    }
}