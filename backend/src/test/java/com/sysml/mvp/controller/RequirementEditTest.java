package com.sysml.mvp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sysml.mvp.service.RequirementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 端到端编辑功能测试
 * 验证完整的编辑数据链：创建 -> 编辑 -> 查询验证
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class RequirementEditTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCompleteEditFlow() throws Exception {
        // Step 1: 创建一个RequirementDefinition
        Map<String, Object> definition = new HashMap<>();
        definition.put("elementId", "TEST-DEF-001");
        definition.put("reqId", "REQ-DEF-001");
        definition.put("declaredName", "测试需求定义");
        definition.put("documentation", "原始描述");

        MvcResult createDefResult = mockMvc.perform(post("/api/v1/requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(definition)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.elementId").value("TEST-DEF-001"))
                .andExpect(jsonPath("$.declaredName").value("测试需求定义"))
                .andReturn();

        // Step 2: 创建一个RequirementUsage
        Map<String, Object> usage = new HashMap<>();
        usage.put("elementId", "TEST-USAGE-001");
        usage.put("declaredName", "测试需求使用");
        usage.put("requirementDefinition", "TEST-DEF-001");
        usage.put("documentation", "原始使用描述");

        MvcResult createUsageResult = mockMvc.perform(post("/api/v1/requirements/usages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(usage)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.elementId").value("TEST-USAGE-001"))
                .andExpect(jsonPath("$.declaredName").value("测试需求使用"))
                .andExpect(jsonPath("$.requirementDefinition").value("TEST-DEF-001"))
                .andReturn();

        // Step 3: 使用PATCH编辑RequirementUsage
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("declaredName", "更新后的需求使用");
        updateData.put("documentation", "更新后的描述");
        updateData.put("requirementDefinition", null); // 清空关联

        MvcResult patchResult = mockMvc.perform(patch("/api/v1/requirements/usages/TEST-USAGE-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elementId").value("TEST-USAGE-001"))
                .andExpect(jsonPath("$.declaredName").value("更新后的需求使用"))
                .andReturn();

        // Step 4: 通过GET验证更新是否持久化
        mockMvc.perform(get("/api/v1/requirements/usages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.elementId=='TEST-USAGE-001')].declaredName").value("更新后的需求使用"));

        // Step 5: 通过/elements/advanced验证数据是否同步
        mockMvc.perform(get("/api/v1/elements/advanced")
                .param("page", "0")
                .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.elementId=='TEST-USAGE-001')].properties.declaredName").value("更新后的需求使用"));

        // Step 6: 再次编辑，恢复关联
        Map<String, Object> restoreData = new HashMap<>();
        restoreData.put("requirementDefinition", "TEST-DEF-001");

        mockMvc.perform(patch("/api/v1/requirements/usages/TEST-USAGE-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(restoreData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requirementDefinition").value("TEST-DEF-001"));

        // Step 7: 最终验证
        mockMvc.perform(get("/api/v1/elements/advanced")
                .param("page", "0")
                .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.elementId=='TEST-USAGE-001')].properties.requirementDefinition").value("TEST-DEF-001"));
    }

    @Test
    public void testEditDefinition() throws Exception {
        // 创建Definition
        Map<String, Object> definition = new HashMap<>();
        definition.put("elementId", "TEST-DEF-002");
        definition.put("reqId", "REQ-DEF-002");
        definition.put("declaredName", "原始定义");

        mockMvc.perform(post("/api/v1/requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(definition)))
                .andExpect(status().isCreated());

        // 使用PATCH编辑
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("declaredName", "更新后的定义");
        updateData.put("declaredShortName", "短名称");

        mockMvc.perform(patch("/api/v1/requirements/TEST-DEF-002")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.declaredName").value("更新后的定义"))
                .andExpect(jsonPath("$.declaredShortName").value("短名称"));

        // 验证持久化
        mockMvc.perform(get("/api/v1/requirements/TEST-DEF-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.declaredName").value("更新后的定义"));
    }
}