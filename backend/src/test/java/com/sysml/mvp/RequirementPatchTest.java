package com.sysml.mvp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sysml.mvp.dto.RequirementDefinitionDTO;
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
 * Phase 3: PATCH部分更新测试 - RequirementDefinition
 * 测试REQ-C1-2的PATCH语义支持
 */
@SpringBootTest
@AutoConfigureMockMvc
public class RequirementPatchTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private String createdRequirementId;
    
    @BeforeEach
    void setUp() throws Exception {
        // 创建一个测试用的RequirementDefinition
        RequirementDefinitionDTO createDto = RequirementDefinitionDTO.builder()
            .reqId("REQ-PATCH-001")
            .name("Original Name")
            .text("Original Text")
            .type("definition")
            .build();
        
        MvcResult result = mockMvc.perform(post("/api/v1/requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createDto)))
            .andExpect(status().isCreated())
            .andReturn();
        
        RequirementDefinitionDTO created = objectMapper.readValue(
            result.getResponse().getContentAsString(), 
            RequirementDefinitionDTO.class
        );
        createdRequirementId = created.getId();
        
        assertNotNull(createdRequirementId, "需求必须创建成功");
    }
    
    @Test
    void shouldPatchUpdateDefinitionName() throws Exception {
        // REQ-C1-2: PATCH只更新name字段
        Map<String, Object> patchData = new HashMap<>();
        patchData.put("name", "Updated Name");
        
        // 执行PATCH
        mockMvc.perform(patch("/api/v1/requirements/{id}", createdRequirementId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchData)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated Name"))
            .andExpect(jsonPath("$.text").value("Original Text")) // 其他字段不变
            .andExpect(jsonPath("$.reqId").value("REQ-PATCH-001")); // 其他字段不变
    }
    
    @Test
    void shouldPatchUpdateDefinitionText() throws Exception {
        // REQ-C1-2: PATCH只更新text字段
        Map<String, Object> patchData = new HashMap<>();
        patchData.put("text", "Updated Text Content");
        
        // 执行PATCH
        mockMvc.perform(patch("/api/v1/requirements/{id}", createdRequirementId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchData)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.text").value("Updated Text Content"))
            .andExpect(jsonPath("$.name").value("Original Name")) // 其他字段不变
            .andExpect(jsonPath("$.reqId").value("REQ-PATCH-001")); // 其他字段不变
    }
    
    @Test
    void shouldNotChangeOtherFieldsWhenPatching() throws Exception {
        // REQ-C1-2: PATCH语义 - 未提供的字段保持不变
        Map<String, Object> patchData = new HashMap<>();
        patchData.put("name", "Only Name Changed");
        
        // 执行PATCH
        MvcResult result = mockMvc.perform(patch("/api/v1/requirements/{id}", createdRequirementId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchData)))
            .andExpect(status().isOk())
            .andReturn();
        
        RequirementDefinitionDTO updated = objectMapper.readValue(
            result.getResponse().getContentAsString(),
            RequirementDefinitionDTO.class
        );
        
        // 验证只有name改变，其他字段保持原值
        assertEquals("Only Name Changed", updated.getName());
        assertEquals("Original Text", updated.getText());
        assertEquals("REQ-PATCH-001", updated.getReqId());
        assertEquals("definition", updated.getType());
        assertEquals(createdRequirementId, updated.getId());
    }
    
    @Test
    void shouldHandleNullValuesInPatch() throws Exception {
        // 测试null值处理策略
        Map<String, Object> patchData = new HashMap<>();
        patchData.put("name", "New Name");
        patchData.put("text", null); // null值应该被忽略，不应清空字段
        
        // 执行PATCH
        mockMvc.perform(patch("/api/v1/requirements/{id}", createdRequirementId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchData)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("New Name"))
            .andExpect(jsonPath("$.text").value("Original Text")); // null不应清空原值
    }
    
    @Test
    void shouldReturnNotFoundForInvalidId() throws Exception {
        // 测试不存在的ID
        Map<String, Object> patchData = new HashMap<>();
        patchData.put("name", "Any Name");
        
        mockMvc.perform(patch("/api/v1/requirements/{id}", "invalid-id-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchData)))
            .andExpect(status().isNotFound());
    }
    
    @Test
    void shouldAllowPatchingMultipleFields() throws Exception {
        // REQ-C1-2: 支持同时更新多个字段
        Map<String, Object> patchData = new HashMap<>();
        patchData.put("name", "Multi Update Name");
        patchData.put("text", "Multi Update Text");
        
        // 执行PATCH
        mockMvc.perform(patch("/api/v1/requirements/{id}", createdRequirementId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchData)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Multi Update Name"))
            .andExpect(jsonPath("$.text").value("Multi Update Text"))
            .andExpect(jsonPath("$.reqId").value("REQ-PATCH-001")); // 未更新的字段保持不变
    }
    
    @Test
    void shouldIgnoreUnknownFieldsInPatch() throws Exception {
        // 测试未知字段应被忽略
        Map<String, Object> patchData = new HashMap<>();
        patchData.put("name", "Valid Name");
        patchData.put("unknownField", "Should be ignored");
        patchData.put("anotherUnknown", 123);
        
        // 执行PATCH - 不应因未知字段而失败
        mockMvc.perform(patch("/api/v1/requirements/{id}", createdRequirementId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchData)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Valid Name"))
            .andExpect(jsonPath("$.unknownField").doesNotExist());
    }
}