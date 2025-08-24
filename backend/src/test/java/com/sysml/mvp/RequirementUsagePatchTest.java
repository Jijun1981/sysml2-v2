package com.sysml.mvp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sysml.mvp.dto.RequirementDefinitionDTO;
import com.sysml.mvp.dto.RequirementUsageDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 3: PATCH部分更新测试 - RequirementUsage
 * 测试REQ-C2-2的PATCH语义支持
 */
@SpringBootTest
@AutoConfigureMockMvc
public class RequirementUsagePatchTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private String createdUsageId;
    private String definitionId;
    
    @BeforeEach
    void setUp() throws Exception {
        // 先创建一个Definition
        RequirementDefinitionDTO defDto = RequirementDefinitionDTO.builder()
            .reqId("REQ-DEF-PATCH")
            .name("Test Definition")
            .text("Definition for PATCH test")
            .type("definition")
            .build();
        
        MvcResult defResult = mockMvc.perform(post("/api/v1/requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(defDto)))
            .andExpect(status().isCreated())
            .andReturn();
        
        RequirementDefinitionDTO createdDef = objectMapper.readValue(
            defResult.getResponse().getContentAsString(),
            RequirementDefinitionDTO.class
        );
        definitionId = createdDef.getId();
        
        // 创建一个测试用的RequirementUsage
        RequirementUsageDTO usageDto = RequirementUsageDTO.builder()
            .name("Original Usage Name")
            .text("Original Usage Text")
            .status("draft")
            .of(definitionId)
            .type("usage")
            .build();
        
        MvcResult usageResult = mockMvc.perform(post("/api/v1/requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usageDto)))
            .andExpect(status().isCreated())
            .andReturn();
        
        RequirementUsageDTO createdUsage = objectMapper.readValue(
            usageResult.getResponse().getContentAsString(),
            RequirementUsageDTO.class
        );
        createdUsageId = createdUsage.getId();
        
        assertNotNull(createdUsageId, "Usage必须创建成功");
        assertNotNull(definitionId, "Definition必须创建成功");
    }
    
    @Test
    void shouldPatchUpdateUsageName() throws Exception {
        // REQ-C2-2: PATCH只更新name字段
        Map<String, Object> patchData = new HashMap<>();
        patchData.put("name", "Updated Usage Name");
        
        // 执行PATCH
        mockMvc.perform(patch("/api/v1/requirements/{id}", createdUsageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchData)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated Usage Name"))
            .andExpect(jsonPath("$.text").value("Original Usage Text")) // 其他字段不变
            .andExpect(jsonPath("$.status").value("draft")) // 其他字段不变
            .andExpect(jsonPath("$.of").value(definitionId)); // of引用不变
    }
    
    @Test
    void shouldPatchUpdateUsageStatus() throws Exception {
        // REQ-C2-2: PATCH只更新status字段
        Map<String, Object> patchData = new HashMap<>();
        patchData.put("status", "approved");
        
        // 执行PATCH
        mockMvc.perform(patch("/api/v1/requirements/{id}", createdUsageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchData)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("approved"))
            .andExpect(jsonPath("$.name").value("Original Usage Name")) // 其他字段不变
            .andExpect(jsonPath("$.text").value("Original Usage Text")) // 其他字段不变
            .andExpect(jsonPath("$.of").value(definitionId)); // of引用不变
    }
    
    @Test
    void shouldPreserveOfReferenceWhenPatching() throws Exception {
        // REQ-C2-2: PATCH时保留of引用（即使请求中没有提供）
        Map<String, Object> patchData = new HashMap<>();
        patchData.put("name", "New Name");
        patchData.put("text", "New Text");
        patchData.put("status", "active");
        // 注意：没有提供of字段
        
        // 执行PATCH
        MvcResult result = mockMvc.perform(patch("/api/v1/requirements/{id}", createdUsageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchData)))
            .andExpect(status().isOk())
            .andReturn();
        
        RequirementUsageDTO updated = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            RequirementUsageDTO.class
        );
        
        // 验证of引用保持不变
        assertEquals(definitionId, updated.getOf(), "of引用必须保持不变");
        assertEquals("New Name", updated.getName());
        assertEquals("New Text", updated.getText());
        assertEquals("active", updated.getStatus());
    }
    
    @Test
    void shouldNotAllowChangingOfReference() throws Exception {
        // 业务规则：不允许通过PATCH修改of引用
        Map<String, Object> patchData = new HashMap<>();
        patchData.put("of", "another-definition-id");
        
        // 执行PATCH - of字段应被忽略
        mockMvc.perform(patch("/api/v1/requirements/{id}", createdUsageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchData)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.of").value(definitionId)); // of保持原值
    }
    
    @Test
    void shouldHandleMultipleFieldsUpdate() throws Exception {
        // REQ-C2-2: 同时更新多个字段
        Map<String, Object> patchData = new HashMap<>();
        patchData.put("name", "Multi Update Name");
        patchData.put("text", "Multi Update Text");
        patchData.put("status", "completed");
        
        // 执行PATCH
        mockMvc.perform(patch("/api/v1/requirements/{id}", createdUsageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchData)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Multi Update Name"))
            .andExpect(jsonPath("$.text").value("Multi Update Text"))
            .andExpect(jsonPath("$.status").value("completed"))
            .andExpect(jsonPath("$.of").value(definitionId)); // of保持不变
    }
    
    @Test
    void shouldHandleEmptyPatchRequest() throws Exception {
        // 空的PATCH请求应该成功但不改变任何内容
        Map<String, Object> patchData = new HashMap<>();
        
        // 执行PATCH
        mockMvc.perform(patch("/api/v1/requirements/{id}", createdUsageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchData)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Original Usage Name"))
            .andExpect(jsonPath("$.text").value("Original Usage Text"))
            .andExpect(jsonPath("$.status").value("draft"))
            .andExpect(jsonPath("$.of").value(definitionId));
    }
    
    @Test
    void shouldReturnNotFoundForInvalidUsageId() throws Exception {
        // 测试不存在的Usage ID
        Map<String, Object> patchData = new HashMap<>();
        patchData.put("name", "Any Name");
        
        mockMvc.perform(patch("/api/v1/requirements/{id}", "invalid-usage-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchData)))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void shouldIgnoreTypeFieldInPatch() throws Exception {
        // type字段不应被修改（Usage永远是usage类型）
        Map<String, Object> patchData = new HashMap<>();
        patchData.put("type", "definition"); // 尝试改变类型
        patchData.put("name", "Name Update");
        
        // 执行PATCH
        mockMvc.perform(patch("/api/v1/requirements/{id}", createdUsageId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchData)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.type").value("usage")) // type保持为usage
            .andExpect(jsonPath("$.name").value("Name Update"));
    }
}