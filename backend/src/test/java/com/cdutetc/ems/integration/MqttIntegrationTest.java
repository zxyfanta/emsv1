package com.cdutetc.ems.integration;

import com.cdutetc.ems.BaseIntegrationTest;
import com.cdutetc.ems.entity.Company;
import com.cdutetc.ems.entity.Device;
import com.cdutetc.ems.entity.RadiationDeviceData;
import com.cdutetc.ems.entity.EnvironmentDeviceData;
import com.cdutetc.ems.entity.enums.DeviceType;
import com.cdutetc.ems.repository.CompanyRepository;
import com.cdutetc.ems.repository.RadiationDeviceDataRepository;
import com.cdutetc.ems.repository.EnvironmentDeviceDataRepository;
import com.cdutetc.ems.service.DeviceService;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MQTT集成测试
 * 测试MQTT消息接收、设备自动注册和数据存储功能
 */
@ActiveProfiles("test")
public class MqttIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private RadiationDeviceDataRepository radiationDeviceDataRepository;

    @Autowired
    private EnvironmentDeviceDataRepository environmentDeviceDataRepository;

    @Autowired
    private CompanyRepository companyRepository;

    private static final String MQTT_BROKER = "tcp://localhost:1883";
    private static final String TEST_CLIENT_ID = "test-mqtt-client";
    private static final int WAIT_MS = 3000; // 等待后端处理的时间

    private MqttClient mqttClient;
    private Long defaultCompanyId;

    @BeforeEach
    void setUp() throws Exception {
        // 获取默认公司ID
        Company defaultCompany = companyRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("测试环境中没有找到公司数据"));
        defaultCompanyId = defaultCompany.getId();

        // 初始化测试MQTT客户端
        try {
            mqttClient = new MqttClient(MQTT_BROKER, TEST_CLIENT_ID + "-" + System.currentTimeMillis(),
                                       new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(false);
            options.setConnectionTimeout(10);

            mqttClient.connect(options);
            assertTrue(mqttClient.isConnected(), "MQTT连接建立失败");
        } catch (MqttException e) {
            // 如果MQTT服务不可用，跳过测试
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "MQTT服务不可用，跳过集成测试");
        }
    }

    @Test
    @DisplayName("测试MQTT辐射设备数据接收和自动设备注册")
    void testMqttRadiationDataReceivingAndAutoDeviceRegistration() throws Exception {
        String testDeviceCode = "MQTT-RAD-001";
        String topic = "ems/device/" + testDeviceCode + "/data/radiation";
        String testData = createRadiationDeviceTestData();

        // 发送测试消息（不需要订阅回调，只需等待后端处理）
        MqttMessage message = new MqttMessage(testData.getBytes());
        message.setQos(1);
        mqttClient.publish(topic, message);

        // 等待后端MQTT监听器处理消息并保存数据
        Thread.sleep(WAIT_MS);

        // 验证设备自动注册
        Device device = deviceService.findByDeviceCode(testDeviceCode);
        assertNotNull(device, "设备应该被自动注册");
        assertEquals(testDeviceCode, device.getDeviceCode());
        assertEquals(DeviceType.RADIATION_MONITOR, device.getDeviceType());
        assertNotNull(device.getCompany(), "设备应该关联到公司");

        // 验证数据存储
        java.util.List<RadiationDeviceData> dataList = radiationDeviceDataRepository
                .findByDeviceCode(testDeviceCode);
        assertFalse(dataList.isEmpty(), "辐射设备数据应该被保存");
        RadiationDeviceData savedData = dataList.get(0);
        assertEquals(testDeviceCode, savedData.getDeviceCode());
        assertNotNull(savedData.getRawData());
        assertNotNull(savedData.getRecordTime());
    }

    @Test
    @DisplayName("测试MQTT环境设备数据接收和自动设备注册")
    void testMqttEnvironmentDataReceivingAndAutoDeviceRegistration() throws Exception {
        String testDeviceCode = "MQTT-ENV-001";
        String topic = "ems/device/" + testDeviceCode + "/data/environment";
        String testData = createEnvironmentDeviceTestData();

        // 发送测试消息
        MqttMessage message = new MqttMessage(testData.getBytes());
        message.setQos(1);
        mqttClient.publish(topic, message);

        // 等待后端MQTT监听器处理消息并保存数据
        Thread.sleep(WAIT_MS);

        // 验证设备自动注册
        Device device = deviceService.findByDeviceCode(testDeviceCode);
        assertNotNull(device, "设备应该被自动注册");
        assertEquals(testDeviceCode, device.getDeviceCode());
        assertEquals(DeviceType.ENVIRONMENT_STATION, device.getDeviceType());
        assertNotNull(device.getCompany(), "设备应该关联到公司");

        // 验证数据存储
        java.util.List<EnvironmentDeviceData> dataList = environmentDeviceDataRepository
                .findByDeviceCode(testDeviceCode);
        assertFalse(dataList.isEmpty(), "环境设备数据应该被保存");
        EnvironmentDeviceData savedData = dataList.get(0);
        assertEquals(testDeviceCode, savedData.getDeviceCode());
        assertNotNull(savedData.getRawData());
        assertNotNull(savedData.getRecordTime());
    }

    @Test
    @DisplayName("测试已存在设备的数据接收")
    void testMqttDataReceivingForExistingDevice() throws Exception {
        // 先创建一个设备
        String testDeviceCode = "MQTT-EXIST-001";
        Device existingDevice = new Device();
        existingDevice.setDeviceCode(testDeviceCode);
        existingDevice.setDeviceName("测试存在设备");
        existingDevice.setDeviceType(DeviceType.RADIATION_MONITOR);
        existingDevice.setManufacturer("测试厂商");
        existingDevice.setModel("测试型号");
        existingDevice.setLocation("测试位置");

        // 修复：使用动态获取的默认公司ID
        Device savedDevice = deviceService.createDevice(existingDevice, defaultCompanyId);
        assertNotNull(savedDevice.getId(), "已存在设备创建失败");

        String topic = "ems/device/" + testDeviceCode + "/data/radiation";
        String testData = createRadiationDeviceTestData();

        // 发送测试消息
        MqttMessage message = new MqttMessage(testData.getBytes());
        message.setQos(1);
        mqttClient.publish(topic, message);

        // 等待后端MQTT监听器处理消息并保存数据
        Thread.sleep(WAIT_MS);

        // 验证数据存储（不会创建新设备）
        Device device = deviceService.findByDeviceCode(testDeviceCode);
        assertNotNull(device, "设备应该存在");
        assertEquals(savedDevice.getId(), device.getId(), "应该是同一个设备实例");
        assertEquals(savedDevice.getDeviceName(), device.getDeviceName(), "设备名称不应改变");

        // 验证数据存储
        java.util.List<RadiationDeviceData> dataList = radiationDeviceDataRepository
                .findByDeviceCode(testDeviceCode);
        assertFalse(dataList.isEmpty(), "辐射设备数据应该被保存");
    }

    /**
     * 创建辐射设备测试数据
     */
    private String createRadiationDeviceTestData() {
        return "{\n" +
                "  \"src\": 1,\n" +
                "  \"msgtype\": 1,\n" +
                "  \"cpm\": 25.5,\n" +
                "  \"batvolt\": 3.8,\n" +
                "  \"time\": \"2025-12-23 10:30:00\",\n" +
                "  \"trigger\": 1,\n" +
                "  \"multi\": 1,\n" +
                "  \"way\": 1,\n" +
                "  \"bdsLongitude\": \"116.404\",\n" +
                "  \"bdsLatitude\": \"39.915\",\n" +
                "  \"bdsUtc\": \"2025-12-23 10:30:00\",\n" +
                "  \"lbsLongitude\": \"116.404\",\n" +
                "  \"lbsLatitude\": \"39.915\",\n" +
                "  \"lbsUseful\": 1\n" +
                "}";
    }

    /**
     * 创建环境设备测试数据
     */
    private String createEnvironmentDeviceTestData() {
        return "{\n" +
                "  \"src\": 1,\n" +
                "  \"cpm\": 15.2,\n" +
                "  \"temperature\": 25.6,\n" +
                "  \"wetness\": 68.5,\n" +
                "  \"windspeed\": 3.2,\n" +
                "  \"total\": 85.3,\n" +
                "  \"battery\": 4.1\n" +
                "}";
    }
}