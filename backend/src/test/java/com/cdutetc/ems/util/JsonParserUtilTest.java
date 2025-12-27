package com.cdutetc.ems.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JsonParserUtil单元测试
 *
 * @author EMS Team
 */
class JsonParserUtilTest {

    private ObjectMapper objectMapper;
    private JsonNode testNode;

    @BeforeEach
    void setUp() throws Exception {
        objectMapper = new ObjectMapper();
        // 创建测试JSON: {"value":123,"text":"hello","flag":true,"number":45.6}
        String testJson = "{\"value\":123,\"text\":\"hello\",\"flag\":true,\"number\":45.6,\"nested\":{\"key\":\"value\"}}";
        testNode = objectMapper.readTree(testJson);
    }

    // ==================== 正常情况测试 ====================

    @Test
    void testParseInt_Success() {
        Optional<Integer> result = JsonParserUtil.parseInt(testNode, "value");

        assertTrue(result.isPresent());
        assertEquals(123, result.get());
    }

    @Test
    void testParseLong_Success() throws Exception {
        String json = "{\"bigint\":9223372036854775807}";
        JsonNode node = objectMapper.readTree(json);

        Optional<Long> result = JsonParserUtil.parseLong(node, "bigint");

        assertTrue(result.isPresent());
        assertEquals(9223372036854775807L, result.get());
    }

    @Test
    void testParseDouble_Success() {
        Optional<Double> result = JsonParserUtil.parseDouble(testNode, "number");

        assertTrue(result.isPresent());
        assertEquals(45.6, result.get(), 0.001);
    }

    @Test
    void testParseString_Success() {
        Optional<String> result = JsonParserUtil.parseString(testNode, "text");

        assertTrue(result.isPresent());
        assertEquals("hello", result.get());
    }

    @Test
    void testParseBoolean_Success() {
        Optional<Boolean> result = JsonParserUtil.parseBoolean(testNode, "flag");

        assertTrue(result.isPresent());
        assertTrue(result.get());
    }

    @Test
    void testParseObject_Success() {
        Optional<JsonNode> result = JsonParserUtil.parseObject(testNode, "nested");

        assertTrue(result.isPresent());
        assertTrue(result.get().has("key"));
        assertEquals("value", result.get().get("key").asText());
    }

    // ==================== 字段缺失测试 ====================

    @Test
    void testParseInt_MissingField() {
        Optional<Integer> result = JsonParserUtil.parseInt(testNode, "nonexistent");

        assertFalse(result.isPresent());
    }

    @Test
    void testParseString_MissingField() {
        Optional<String> result = JsonParserUtil.parseString(testNode, "nonexistent");

        assertFalse(result.isPresent());
    }

    @Test
    void testParseObject_MissingField() {
        Optional<JsonNode> result = JsonParserUtil.parseObject(testNode, "nonexistent");

        assertFalse(result.isPresent());
    }

    // ==================== 类型转换测试 ====================

    @Test
    void testParseInt_FromString() throws Exception {
        String json = "{\"value\":\"456\"}";
        JsonNode node = objectMapper.readTree(json);

        Optional<Integer> result = JsonParserUtil.parseInt(node, "value");

        assertTrue(result.isPresent());
        assertEquals(456, result.get());
    }

    @Test
    void testParseDouble_FromString() throws Exception {
        String json = "{\"value\":\"78.9\"}";
        JsonNode node = objectMapper.readTree(json);

        Optional<Double> result = JsonParserUtil.parseDouble(node, "value");

        assertTrue(result.isPresent());
        assertEquals(78.9, result.get(), 0.001);
    }

    @Test
    void testParseDouble_FromInt() throws Exception {
        String json = "{\"value\":100}";
        JsonNode node = objectMapper.readTree(json);

        Optional<Double> result = JsonParserUtil.parseDouble(node, "value");

        assertTrue(result.isPresent());
        assertEquals(100.0, result.get(), 0.001);
    }

    @Test
    void testParseBoolean_FromString() throws Exception {
        String json = "{\"flag1\":\"true\",\"flag2\":\"false\",\"flag3\":\"1\",\"flag4\":\"0\"}";
        JsonNode node = objectMapper.readTree(json);

        assertTrue(JsonParserUtil.parseBoolean(node, "flag1").get());
        assertFalse(JsonParserUtil.parseBoolean(node, "flag2").get());
        assertTrue(JsonParserUtil.parseBoolean(node, "flag3").get());
        assertFalse(JsonParserUtil.parseBoolean(node, "flag4").get());
    }

    @Test
    void testParseBoolean_FromInt() throws Exception {
        String json = "{\"flag1\":1,\"flag2\":0}";
        JsonNode node = objectMapper.readTree(json);

        assertTrue(JsonParserUtil.parseBoolean(node, "flag1").get());
        assertFalse(JsonParserUtil.parseBoolean(node, "flag2").get());
    }

    // ==================== 类型错误测试 ====================

    @Test
    void testParseInt_InvalidString() throws Exception {
        String json = "{\"value\":\"not_a_number\"}";
        JsonNode node = objectMapper.readTree(json);

        Optional<Integer> result = JsonParserUtil.parseInt(node, "value");

        assertFalse(result.isPresent());
    }

    @Test
    void testParseDouble_InvalidString() throws Exception {
        String json = "{\"value\":\"not_a_double\"}";
        JsonNode node = objectMapper.readTree(json);

        Optional<Double> result = JsonParserUtil.parseDouble(node, "value");

        assertFalse(result.isPresent());
    }

    // ==================== null值测试 ====================

    @Test
    void testParseInt_NullValue() throws Exception {
        String json = "{\"value\":null}";
        JsonNode node = objectMapper.readTree(json);

        Optional<Integer> result = JsonParserUtil.parseInt(node, "value");

        assertFalse(result.isPresent());
    }

    @Test
    void testParseString_NullValue() throws Exception {
        String json = "{\"value\":null}";
        JsonNode node = objectMapper.readTree(json);

        Optional<String> result = JsonParserUtil.parseString(node, "value");

        assertFalse(result.isPresent());
    }

    // ==================== 带默认值的解析测试 ====================

    @Test
    void testParseIntOrDefault_WithDefaultValue() {
        Integer result = JsonParserUtil.parseIntOrDefault(testNode, "nonexistent", 999);

        assertEquals(999, result);
    }

    @Test
    void testParseIntOrDefault_WithValue() {
        Integer result = JsonParserUtil.parseIntOrDefault(testNode, "value", 999);

        assertEquals(123, result);
    }

    @Test
    void testParseStringOrDefault_WithDefaultValue() {
        String result = JsonParserUtil.parseStringOrDefault(testNode, "nonexistent", "default");

        assertEquals("default", result);
    }

    @Test
    void testParseDoubleOrDefault_WithDefaultValue() {
        Double result = JsonParserUtil.parseDoubleOrDefault(testNode, "nonexistent", 99.9);

        assertEquals(99.9, result, 0.001);
    }

    // ==================== 辐射设备数据格式测试 ====================

    @Test
    void testParseRadiationDeviceData() throws Exception {
        String radiationJson = "{\"src\":1,\"msgtype\":1,\"CPM\":123.5,\"Batvolt\":3989,\"time\":\"2025/01/15 14:30:45\",\"trigger\":1,\"multi\":1,\"way\":1}";
        JsonNode node = objectMapper.readTree(radiationJson);

        Optional<Integer> src = JsonParserUtil.parseInt(node, "src");
        Optional<Double> cpm = JsonParserUtil.parseDouble(node, "CPM");
        Optional<Double> batvolt = JsonParserUtil.parseDouble(node, "Batvolt");
        Optional<String> time = JsonParserUtil.parseString(node, "time");

        assertTrue(src.isPresent());
        assertEquals(1, src.get());
        assertTrue(cpm.isPresent());
        assertEquals(123.5, cpm.get(), 0.001);
        assertTrue(batvolt.isPresent());
        assertEquals(3989.0, batvolt.get(), 0.001);
        assertTrue(time.isPresent());
        assertEquals("2025/01/15 14:30:45", time.get());
    }

    @Test
    void testParseRadiationDataWithGPS() throws Exception {
        String json = "{\"BDS\":{\"longitude\":\"103°59'\",\"latitude\":\"30°33'\",\"UTC\":\"14:30:45\",\"useful\":1}}";
        JsonNode node = objectMapper.readTree(json);

        Optional<JsonNode> bds = JsonParserUtil.parseObject(node, "BDS");

        assertTrue(bds.isPresent());
        Optional<String> longitude = JsonParserUtil.parseString(bds.get(), "longitude");
        Optional<String> latitude = JsonParserUtil.parseString(bds.get(), "latitude");
        Optional<Integer> useful = JsonParserUtil.parseInt(bds.get(), "useful");

        assertTrue(longitude.isPresent());
        assertEquals("103°59'", longitude.get());
        assertTrue(latitude.isPresent());
        assertEquals("30°33'", latitude.get());
        assertTrue(useful.isPresent());
        assertEquals(1, useful.get());
    }

    // ==================== 环境设备数据格式测试 ====================

    @Test
    void testParseEnvironmentDeviceData() throws Exception {
        String envJson = "{\"src\":1,\"CPM\":4,\"temperature\":10.5,\"wetness\":95.0,\"windspeed\":0.2,\"total\":144.1,\"battery\":11.9}";
        JsonNode node = objectMapper.readTree(envJson);

        Optional<Integer> src = JsonParserUtil.parseInt(node, "src");
        Optional<Double> cpm = JsonParserUtil.parseDouble(node, "CPM");
        Optional<Double> temperature = JsonParserUtil.parseDouble(node, "temperature");
        Optional<Double> wetness = JsonParserUtil.parseDouble(node, "wetness");
        Optional<Double> windspeed = JsonParserUtil.parseDouble(node, "windspeed");
        Optional<Double> total = JsonParserUtil.parseDouble(node, "total");
        Optional<Double> battery = JsonParserUtil.parseDouble(node, "battery");

        assertTrue(src.isPresent() && cpm.isPresent() && temperature.isPresent() &&
                   wetness.isPresent() && windspeed.isPresent() && total.isPresent() && battery.isPresent());

        assertEquals(1, src.get());
        assertEquals(4.0, cpm.get(), 0.001);
        assertEquals(10.5, temperature.get(), 0.001);
        assertEquals(95.0, wetness.get(), 0.001);
        assertEquals(0.2, windspeed.get(), 0.001);
        assertEquals(144.1, total.get(), 0.001);
        assertEquals(11.9, battery.get(), 0.001);
    }

    // ==================== 解析统计测试 ====================

    @Test
    void testParsingStats_Success() {
        String json = "{\"src\":1,\"CPM\":100,\"Batvolt\":3989}";
        JsonNode node;
        try {
            node = objectMapper.readTree(json);
        } catch (Exception e) {
            fail("Failed to parse test JSON");
            return;
        }

        JsonParserUtil.ParsingStats stats = JsonParserUtil.parseWithStats(helper -> {
            helper.parse(node, "src", n -> n.asInt());
            helper.parse(node, "CPM", n -> n.asDouble());
            helper.parse(node, "Batvolt", n -> n.asDouble());
        });

        assertEquals(3, stats.getTotalFields());
        assertEquals(3, stats.getSuccessFields());
        assertEquals(0, stats.getFailedFields());
        assertTrue(stats.isSuccess());
        assertEquals(100.0, stats.getSuccessRate(), 0.001);
    }

    @Test
    void testParsingStats_WithFailures() {
        String json = "{\"src\":1,\"CPM\":100}";
        JsonNode node;
        try {
            node = objectMapper.readTree(json);
        } catch (Exception e) {
            fail("Failed to parse test JSON");
            return;
        }

        JsonParserUtil.ParsingStats stats = JsonParserUtil.parseWithStats(helper -> {
            helper.parse(node, "src", n -> n.asInt());
            helper.parse(node, "CPM", n -> n.asDouble());
            helper.parse(node, "Batvolt", n -> n.asDouble()); // 字段不存在
        });

        assertEquals(3, stats.getTotalFields());
        assertEquals(2, stats.getSuccessFields());
        assertEquals(1, stats.getFailedFields());
        assertFalse(stats.isSuccess());
        assertEquals(66.67, stats.getSuccessRate(), 0.01);
        assertTrue(stats.getFailedFieldNames().contains("Batvolt"));
    }
}
