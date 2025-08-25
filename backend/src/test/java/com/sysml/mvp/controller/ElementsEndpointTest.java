package com.sysml.mvp.controller;

import com.sysml.mvp.command.ImportDemoDataCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TDD测试：验证前端期望的 /api/v1/elements 端点
 * 
 * 需求：
 * 1. GET /api/v1/elements - 返回default项目的所有元素
 * 2. 前端无需传projectId参数（默认使用default项目）
 * 3. 返回层次结构数据，包括RequirementDefinition和其包含的RequirementUsage
 */
@SpringBootTest
@AutoConfigureWebMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ElementsEndpointTest {

    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ImportDemoDataCommand importCommand;
    
    @BeforeEach
    public void setUp() throws Exception {
        // 导入demo数据，确保有测试数据
        importCommand.run("import-demo");
    }
    
    @Test
    public void testGET_elements_withoutProjectId_shouldReturnDefaultProject() throws Exception {
        // Given: demo数据已导入
        
        // When: 前端请求GET /api/v1/elements（不带projectId）
        // Then: 应该返回default项目的所有元素
        mockMvc.perform(get("/api/v1/elements"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(8)) // 应该只返回8个根级元素（RequirementDefinition）
                .andExpect(jsonPath("$[0].eClass").value("RequirementDefinition"))
                .andExpect(jsonPath("$[0].elementId").exists())
                .andExpect(jsonPath("$[0].declaredName").exists())
                .andExpect(jsonPath("$[0].declaredShortName").exists());
    }
    
    @Test 
    public void testGET_elements_shouldIncludeHierarchicalStructure() throws Exception {
        // Given: demo数据已导入，有层次结构
        
        // When & Then: 返回的数据应包含层次结构
        mockMvc.perform(get("/api/v1/elements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ownedFeature").isArray())
                .andExpect(jsonPath("$[0].ownedFeature[0].eClass").value("RequirementUsage"))
                .andExpect(jsonPath("$[0].ownedFeature[0].elementId").exists());
    }
    
    @Test
    public void testGET_elements_shouldShowMeaningfulNames() throws Exception {
        // Given: demo数据已导入，text字段应映射为declaredName
        
        // When & Then: 应该显示有意义的需求内容而非通用名称
        mockMvc.perform(get("/api/v1/elements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].declaredName").value(org.hamcrest.Matchers.containsString("用户")))
                .andExpect(jsonPath("$[0].declaredShortName").value("DEMO-REQ-001"));
    }
    
    @Test
    public void testGET_elements_backwardCompatibilityWithProjectIdParam() throws Exception {
        // Given: demo数据已导入
        
        // When: 使用原有的projectId参数方式
        // Then: 应该仍然工作（向后兼容）
        mockMvc.perform(get("/api/v1/elements").param("projectId", "default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}