package com.sysml.mvp.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sysml.mvp.dto.RequirementDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 简化集成测试 - 验证基本联调功能
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebMvc
@DisplayName("简化集成测试")
public class SimpleIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @DisplayName("【联调】创建需求并验证持久化")
    void testBasicCreateAndRead() throws Exception {
        // 创建需求
        RequirementDTO requirement = new RequirementDTO();
        requirement.setReqId("REQ-SIMPLE-001");
        requirement.setDeclaredName("简单集成测试");
        requirement.setDocumentation("测试基本的创建和读取功能");
        
        MvcResult createResult = mockMvc.perform(post("/api/v1/requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requirement)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reqId").value("REQ-SIMPLE-001"))
                .andReturn();
        
        // 获取创建的ID
        String response = createResult.getResponse().getContentAsString();
        RequirementDTO created = objectMapper.readValue(response, RequirementDTO.class);
        String elementId = created.getElementId();
        
        assertNotNull(elementId, "元素ID不应为空");
        
        // 验证可以读取
        mockMvc.perform(get("/api/v1/requirements/" + elementId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reqId").value("REQ-SIMPLE-001"))
                .andExpect(jsonPath("$.declaredName").value("简单集成测试"));
        
        System.out.println("✅ 基本创建和读取测试通过");
        System.out.println("   创建的元素ID: " + elementId);
    }
    
    @Test
    @DisplayName("【联调】查询所有需求")
    void testListAllRequirements() throws Exception {
        mockMvc.perform(get("/api/v1/requirements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        
        System.out.println("✅ 查询所有需求测试通过");
    }
    
    @Test
    @DisplayName("【联调】验证API基本功能")
    void testValidationAPI() throws Exception {
        // 测试reqId唯一性验证
        mockMvc.perform(get("/api/v1/validation/reqId/TEST-UNIQUE-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reqId").value("TEST-UNIQUE-001"))
                .andExpect(jsonPath("$.isUnique").exists());
        
        // 测试静态验证
        mockMvc.perform(post("/api/v1/validation/static"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.elementCount").isNumber())
                .andExpect(jsonPath("$.version").value("1.0"));
        
        System.out.println("✅ 验证API基本功能测试通过");
    }
}