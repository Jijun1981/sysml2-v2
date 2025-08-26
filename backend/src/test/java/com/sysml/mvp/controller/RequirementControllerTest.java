package com.sysml.mvp.controller;

import com.sysml.mvp.dto.ElementDTO;
import com.sysml.mvp.dto.RequirementDTO;
import com.sysml.mvp.service.RequirementService;
import com.sysml.mvp.mapper.ElementMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * RequirementController 测试用例
 * 
 * 需求对齐：
 * - REQ-A1-1: 需求定义CRUD API - 完整的REST API端点
 * - REQ-C1-1: 创建需求定义 - POST /api/v1/requirements
 * - REQ-C1-2: 查询需求定义 - GET /api/v1/requirements
 * - REQ-C1-3: 更新需求定义 - PUT /api/v1/requirements/{id}
 * - REQ-C1-4: 参数化文本渲染 - POST /api/v1/requirements/{id}/render
 * - REQ-C2-1: 创建需求使用 - POST /api/v1/requirements/usages
 * - REQ-C2-2: 查询需求使用 - GET /api/v1/requirements/usages
 */
@WebMvcTest(RequirementController.class)
@DisplayName("RequirementController测试 - REQ-A1-1")
public class RequirementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RequirementService requirementService;

    @MockBean
    private ElementMapper elementMapper;

    /**
     * 验收标准：REQ-C1-1
     * POST /api/v1/requirements 应创建需求定义
     */
    @Test
    @DisplayName("REQ-C1-1: POST创建需求定义")
    public void testCreateRequirement_ShouldReturnCreatedRequirement() throws Exception {
        // Given: 创建需求定义的请求数据
        RequirementDTO requestDto = new RequirementDTO();
        requestDto.setReqId("EBS-L1-001");
        requestDto.setDeclaredName("电池系统需求");
        requestDto.setDocumentation("系统应监控电池状态");
        requestDto.setPriority("P0");
        
        ElementDTO createdElement = new ElementDTO();
        createdElement.setElementId("req-def-001");
        createdElement.setEClass("RequirementDefinition");
        createdElement.setProperty("reqId", "EBS-L1-001");
        
        RequirementDTO responseDto = new RequirementDTO();
        responseDto.setElementId("req-def-001");
        responseDto.setReqId("EBS-L1-001");
        responseDto.setDeclaredName("电池系统需求");
        responseDto.setDocumentation("系统应监控电池状态");
        responseDto.setPriority("P0");
        responseDto.setCreatedAt("2025-01-01T10:00:00.000Z");

        // When: 调用创建需求定义服务
        when(elementMapper.toElementData(requestDto)).thenReturn(createElementData(requestDto));
        when(requirementService.createRequirement(any())).thenReturn(createdElement);
        when(elementMapper.toRequirementDTO(createdElement)).thenReturn(responseDto);

        // Then: 应返回201 Created和创建的需求定义
        mockMvc.perform(post("/api/v1/requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.elementId").value("req-def-001"))
                .andExpect(jsonPath("$.reqId").value("EBS-L1-001"))
                .andExpect(jsonPath("$.declaredName").value("电池系统需求"))
                .andExpect(jsonPath("$.documentation").value("系统应监控电池状态"))
                .andExpect(jsonPath("$.priority").value("P0"))
                .andExpect(jsonPath("$.createdAt").exists());

        verify(elementMapper).toElementData(requestDto);
        verify(requirementService).createRequirement(any());
        verify(elementMapper).toRequirementDTO(createdElement);
    }

    /**
     * 验收标准：REQ-C1-1
     * reqId重复时应返回409 Conflict
     */
    @Test
    @DisplayName("REQ-C1-1: reqId重复返回409冲突")
    public void testCreateRequirement_ShouldReturn409WhenReqIdExists() throws Exception {
        // Given: 重复的reqId
        RequirementDTO requestDto = new RequirementDTO();
        requestDto.setReqId("EBS-L1-001");
        requestDto.setDeclaredName("重复需求");

        // When: reqId已存在，抛出IllegalArgumentException
        when(elementMapper.toElementData(requestDto)).thenReturn(createElementData(requestDto));
        when(requirementService.createRequirement(any()))
                .thenThrow(new IllegalArgumentException("reqId already exists: EBS-L1-001"));

        // Then: 应返回409 Conflict
        mockMvc.perform(post("/api/v1/requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("reqId already exists: EBS-L1-001"));

        verify(requirementService).createRequirement(any());
    }

    /**
     * 验收标准：REQ-C1-2
     * GET /api/v1/requirements 应返回所有需求定义
     */
    @Test
    @DisplayName("REQ-C1-2: GET查询所有需求定义")
    public void testGetRequirements_ShouldReturnAllRequirements() throws Exception {
        // Given: 系统中的需求定义
        ElementDTO req1 = new ElementDTO();
        req1.setElementId("req-def-001");
        req1.setEClass("RequirementDefinition");
        req1.setProperty("reqId", "EBS-L1-001");

        ElementDTO req2 = new ElementDTO();
        req2.setElementId("req-def-002");
        req2.setEClass("RequirementDefinition");
        req2.setProperty("reqId", "EBS-L1-002");

        List<ElementDTO> requirements = Arrays.asList(req1, req2);

        RequirementDTO dto1 = new RequirementDTO();
        dto1.setElementId("req-def-001");
        dto1.setReqId("EBS-L1-001");
        dto1.setDeclaredName("需求1");

        RequirementDTO dto2 = new RequirementDTO();
        dto2.setElementId("req-def-002");
        dto2.setReqId("EBS-L1-002");
        dto2.setDeclaredName("需求2");

        // When: 查询所有需求定义
        when(requirementService.getRequirements()).thenReturn(requirements);
        when(elementMapper.toRequirementDTO(req1)).thenReturn(dto1);
        when(elementMapper.toRequirementDTO(req2)).thenReturn(dto2);

        // Then: 应返回200 OK和需求定义列表
        mockMvc.perform(get("/api/v1/requirements"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].elementId").value("req-def-001"))
                .andExpect(jsonPath("$[0].reqId").value("EBS-L1-001"))
                .andExpect(jsonPath("$[1].elementId").value("req-def-002"))
                .andExpect(jsonPath("$[1].reqId").value("EBS-L1-002"));

        verify(requirementService).getRequirements();
        verify(elementMapper).toRequirementDTO(req1);
        verify(elementMapper).toRequirementDTO(req2);
    }

    /**
     * 验收标准：REQ-C1-2
     * GET /api/v1/requirements/{id} 应返回特定需求定义
     */
    @Test
    @DisplayName("REQ-C1-2: GET查询特定需求定义")
    public void testGetRequirementById_ShouldReturnSpecificRequirement() throws Exception {
        // Given: 特定的需求定义
        String requirementId = "req-def-001";
        ElementDTO requirement = new ElementDTO();
        requirement.setElementId(requirementId);
        requirement.setEClass("RequirementDefinition");
        requirement.setProperty("reqId", "EBS-L1-001");

        RequirementDTO responseDto = new RequirementDTO();
        responseDto.setElementId(requirementId);
        responseDto.setReqId("EBS-L1-001");
        responseDto.setDeclaredName("电池系统需求");

        // When: 查询特定需求定义
        when(requirementService.getRequirementById(requirementId)).thenReturn(requirement);
        when(elementMapper.toRequirementDTO(requirement)).thenReturn(responseDto);

        // Then: 应返回200 OK和需求定义
        mockMvc.perform(get("/api/v1/requirements/{id}", requirementId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.elementId").value(requirementId))
                .andExpect(jsonPath("$.reqId").value("EBS-L1-001"))
                .andExpect(jsonPath("$.declaredName").value("电池系统需求"));

        verify(requirementService).getRequirementById(requirementId);
        verify(elementMapper).toRequirementDTO(requirement);
    }

    /**
     * 验收标准：REQ-C1-2
     * 需求不存在时应返回404 Not Found
     */
    @Test
    @DisplayName("REQ-C1-2: 需求不存在返回404")
    public void testGetRequirementById_ShouldReturn404WhenNotFound() throws Exception {
        // Given: 不存在的需求ID
        String requirementId = "non-existent-req";

        // When: 需求不存在
        when(requirementService.getRequirementById(requirementId)).thenReturn(null);

        // Then: 应返回404 Not Found
        mockMvc.perform(get("/api/v1/requirements/{id}", requirementId))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Requirement not found: " + requirementId));

        verify(requirementService).getRequirementById(requirementId);
    }

    /**
     * 验收标准：REQ-C1-3
     * PUT /api/v1/requirements/{id} 应更新需求定义
     */
    @Test
    @DisplayName("REQ-C1-3: PUT更新需求定义")
    public void testUpdateRequirement_ShouldReturnUpdatedRequirement() throws Exception {
        // Given: 更新需求定义的数据
        String requirementId = "req-def-001";
        RequirementDTO updateDto = new RequirementDTO();
        updateDto.setDocumentation("更新后的需求描述");
        updateDto.setPriority("P1");
        updateDto.setStatus("active");

        ElementDTO updatedElement = new ElementDTO();
        updatedElement.setElementId(requirementId);
        updatedElement.setEClass("RequirementDefinition");
        updatedElement.setProperty("documentation", "更新后的需求描述");
        updatedElement.setProperty("priority", "P1");

        RequirementDTO responseDto = new RequirementDTO();
        responseDto.setElementId(requirementId);
        responseDto.setDocumentation("更新后的需求描述");
        responseDto.setPriority("P1");
        responseDto.setStatus("active");
        responseDto.setUpdatedAt("2025-01-01T10:30:00.000Z");

        // When: 更新需求定义
        when(elementMapper.toElementData(updateDto)).thenReturn(createElementData(updateDto));
        when(requirementService.updateRequirement(eq(requirementId), any())).thenReturn(updatedElement);
        when(elementMapper.toRequirementDTO(updatedElement)).thenReturn(responseDto);

        // Then: 应返回200 OK和更新后的需求定义
        mockMvc.perform(put("/api/v1/requirements/{id}", requirementId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.elementId").value(requirementId))
                .andExpect(jsonPath("$.documentation").value("更新后的需求描述"))
                .andExpect(jsonPath("$.priority").value("P1"))
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.updatedAt").exists());

        verify(requirementService).updateRequirement(eq(requirementId), any());
        verify(elementMapper).toRequirementDTO(updatedElement);
    }

    /**
     * 验收标准：REQ-C1-3
     * DELETE /api/v1/requirements/{id} 应删除需求定义
     */
    @Test
    @DisplayName("REQ-C1-3: DELETE删除需求定义")
    public void testDeleteRequirement_ShouldReturn204NoContent() throws Exception {
        // Given: 要删除的需求ID
        String requirementId = "req-def-001";

        // When: 删除需求定义
        when(requirementService.deleteRequirement(requirementId)).thenReturn(true);

        // Then: 应返回204 No Content
        mockMvc.perform(delete("/api/v1/requirements/{id}", requirementId))
                .andExpect(status().isNoContent());

        verify(requirementService).deleteRequirement(requirementId);
    }

    /**
     * 验收标准：REQ-C1-3
     * 删除被引用的需求时应返回409 Conflict
     */
    @Test
    @DisplayName("REQ-C1-3: 删除被引用需求返回409冲突")
    public void testDeleteRequirement_ShouldReturn409WhenReferenced() throws Exception {
        // Given: 被引用的需求ID
        String requirementId = "req-def-001";

        // When: 需求被引用，抛出IllegalStateException
        when(requirementService.deleteRequirement(requirementId))
                .thenThrow(new IllegalStateException("Cannot delete requirement req-def-001: referenced by 2 usages"));

        // Then: 应返回409 Conflict
        mockMvc.perform(delete("/api/v1/requirements/{id}", requirementId))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Cannot delete requirement req-def-001: referenced by 2 usages"));

        verify(requirementService).deleteRequirement(requirementId);
    }

    /**
     * 验收标准：REQ-C1-4
     * POST /api/v1/requirements/{id}/render 应渲染参数化文本
     */
    @Test
    @DisplayName("REQ-C1-4: POST渲染参数化文本")
    public void testRenderParametricText_ShouldReturnRenderedText() throws Exception {
        // Given: 参数化文本渲染请求
        String requirementId = "req-def-001";
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("subject", "Engine");
        parameters.put("performance", "100kW");
        parameters.put("window", "10min");

        String renderedText = "The Engine shall achieve 100kW within 10min.";

        // When: 渲染参数化文本
        when(requirementService.renderParametricText(requirementId, parameters)).thenReturn(renderedText);

        // Then: 应返回200 OK和渲染结果
        mockMvc.perform(post("/api/v1/requirements/{id}/render", requirementId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(parameters)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.renderedText").value(renderedText));

        verify(requirementService).renderParametricText(requirementId, parameters);
    }

    /**
     * 验收标准：REQ-C2-1
     * POST /api/v1/requirements/usages 应创建需求使用
     */
    @Test
    @DisplayName("REQ-C2-1: POST创建需求使用")
    public void testCreateRequirementUsage_ShouldReturnCreatedUsage() throws Exception {
        // Given: 创建需求使用的请求数据
        RequirementDTO requestDto = new RequirementDTO();
        requestDto.setDeclaredName("电池温度监控使用");
        // 设置subject和of属性通过properties
        Map<String, Object> properties = new HashMap<>();
        properties.put("subject", "part-001");
        properties.put("of", "req-def-001");

        ElementDTO createdElement = new ElementDTO();
        createdElement.setElementId("req-usage-001");
        createdElement.setEClass("RequirementUsage");
        createdElement.setProperty("subject", "part-001");
        createdElement.setProperty("of", "req-def-001");

        RequirementDTO responseDto = new RequirementDTO();
        responseDto.setElementId("req-usage-001");
        responseDto.setDeclaredName("电池温度监控使用");
        responseDto.setCreatedAt("2025-01-01T10:00:00.000Z");

        // When: 调用创建需求使用服务
        when(elementMapper.toElementData(requestDto)).thenReturn(properties);
        when(requirementService.createRequirementUsage(any())).thenReturn(createdElement);
        when(elementMapper.toRequirementDTO(createdElement)).thenReturn(responseDto);

        // Then: 应返回201 Created和创建的需求使用
        mockMvc.perform(post("/api/v1/requirements/usages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.elementId").value("req-usage-001"))
                .andExpect(jsonPath("$.declaredName").value("电池温度监控使用"))
                .andExpect(jsonPath("$.createdAt").exists());

        verify(requirementService).createRequirementUsage(any());
        verify(elementMapper).toRequirementDTO(createdElement);
    }

    /**
     * 验收标准：REQ-C2-2
     * GET /api/v1/requirements/usages 应返回所有需求使用
     */
    @Test
    @DisplayName("REQ-C2-2: GET查询所有需求使用")
    public void testGetRequirementUsages_ShouldReturnAllUsages() throws Exception {
        // Given: 系统中的需求使用
        ElementDTO usage1 = new ElementDTO();
        usage1.setElementId("req-usage-001");
        usage1.setEClass("RequirementUsage");
        usage1.setProperty("subject", "part-001");

        List<ElementDTO> usages = Arrays.asList(usage1);

        RequirementDTO dto1 = new RequirementDTO();
        dto1.setElementId("req-usage-001");
        dto1.setDeclaredName("需求使用1");

        // When: 查询所有需求使用
        when(requirementService.getRequirementUsages()).thenReturn(usages);
        when(elementMapper.toRequirementDTO(usage1)).thenReturn(dto1);

        // Then: 应返回200 OK和需求使用列表
        mockMvc.perform(get("/api/v1/requirements/usages"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].elementId").value("req-usage-001"))
                .andExpect(jsonPath("$[0].declaredName").value("需求使用1"));

        verify(requirementService).getRequirementUsages();
        verify(elementMapper).toRequirementDTO(usage1);
    }

    /**
     * 辅助方法：创建元素数据Map
     */
    private Map<String, Object> createElementData(RequirementDTO dto) {
        Map<String, Object> data = new HashMap<>();
        if (dto.getReqId() != null) data.put("reqId", dto.getReqId());
        if (dto.getDeclaredName() != null) data.put("declaredName", dto.getDeclaredName());
        if (dto.getDocumentation() != null) data.put("documentation", dto.getDocumentation());
        if (dto.getPriority() != null) data.put("priority", dto.getPriority());
        if (dto.getStatus() != null) data.put("status", dto.getStatus());
        return data;
    }
}