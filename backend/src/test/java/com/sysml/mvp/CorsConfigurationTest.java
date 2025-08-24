package com.sysml.mvp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CORS配置测试 - 验证跨域访问支持
 * 前端(localhost:3002) -> 后端(localhost:8080)
 */
@SpringBootTest
@AutoConfigureWebMvc
@SpringJUnitConfig
class CorsConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldAllowCorsFromFrontendOrigin() throws Exception {
        // 测试预检请求 (OPTIONS)
        mockMvc.perform(options("/api/v1/elements")
                .header("Origin", "http://localhost:3002")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "Content-Type,X-Project-Id"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3002"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PATCH,DELETE,OPTIONS"))
                .andExpect(header().string("Access-Control-Allow-Headers", "Content-Type,X-Project-Id"));
    }

    @Test
    void shouldAllowActualCorsRequest() throws Exception {
        // 测试实际的跨域GET请求
        mockMvc.perform(get("/api/v1/elements")
                .header("Origin", "http://localhost:3002")
                .header("Content-Type", "application/json")
                .header("X-Project-Id", "test-project"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3002"));
        // 注意：这里不检查status，因为可能有EMF相关错误，但CORS头应该正确设置
    }

    @Test
    void shouldSupportPostRequestWithCors() throws Exception {
        // 测试POST预检请求
        mockMvc.perform(options("/api/v1/elements")
                .header("Origin", "http://localhost:3002")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type,X-Project-Id"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3002"))
                .andExpect(header().string("Access-Control-Allow-Methods", "GET,POST,PATCH,DELETE,OPTIONS"));
    }

    @Test
    void shouldHandleCredentialsInCors() throws Exception {
        // 测试是否允许credentials
        mockMvc.perform(options("/api/v1/elements")
                .header("Origin", "http://localhost:3002")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
}