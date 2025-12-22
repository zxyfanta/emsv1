package com.cdutetc.ems;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 基础集成测试类
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    protected ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * 将对象转换为JSON字符串
     */
    protected String asJsonString(final Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 创建测试用的JWT token
     */
    protected String createTestToken(String username, String role) {
        // 这里应该使用实际的JWT工具生成token
        // 为了测试目的，可以使用一个固定的token或者模拟JWT服务
        return "Bearer test-jwt-token-for-" + username + "-with-role-" + role;
    }

    /**
     * 获取管理员权限的token
     */
    protected String getAdminToken() {
        return createTestToken("admin", "ADMIN");
    }

    /**
     * 获取普通用户权限的token
     */
    protected String getUserToken() {
        return createTestToken("user", "USER");
    }

    /**
     * JSON媒体类型常量
     */
    protected static final MediaType APPLICATION_JSON_UTF8 = MediaType.APPLICATION_JSON;

    /**
     * API路径前缀
     */
    protected static final String API_PREFIX = "/api";
}