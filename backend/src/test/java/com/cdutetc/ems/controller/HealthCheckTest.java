package com.cdutetc.ems.controller;

import com.cdutetc.ems.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 健康检查控制器测试
 */
class HealthCheckTest extends BaseIntegrationTest {

    @Test
    @DisplayName("健康检查 - 简单路径")
    void testSimpleHealthCheck() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/device-data/health"))
                .andExpect(status().isNotFound()); // 应该返回404，因为没有健康检查端点
    }

    @Test
    @DisplayName("检查Spring应用上下文加载")
    void testApplicationContextLoads() {
        // 这个测试只是为了验证Spring上下文能正确加载
        // 如果能运行到这里，说明测试框架配置正确
        assert mockMvc != null;
        assert objectMapper != null;
    }
}