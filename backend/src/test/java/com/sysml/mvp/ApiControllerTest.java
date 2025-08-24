package com.sysml.mvp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sysml.mvp.controller.ApiController;
import com.sysml.mvp.dto.RequirementDefinitionDTO;
import com.sysml.mvp.dto.TraceDTO;
import com.sysml.mvp.service.RequirementService;
import com.sysml.mvp.service.TraceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ApiController测试类 - TDD实现
 * 测试前端需要的/api/v1/projects/{projectId}/*路径格式
 */
@WebMvcTest(ApiController.class)
public class ApiControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private RequirementService requirementService;
    
    @MockBean
    private TraceService traceService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private RequirementDefinitionDTO sampleDefinition;
    private RequirementDefinitionDTO sampleUsage;
    private TraceDTO sampleTrace;
    
    @BeforeEach
    void setUp() {
        // 准备测试数据
        sampleDefinition = RequirementDefinitionDTO.builder()
            .id("R-001")
            .eClass("RequirementDefinition")
            .reqId("REQ-001")
            .name("测试需求")
            .text("需求描述")
            .tags(Arrays.asList("tag1"))
            .createdAt(Instant.now())
            .build();
            
        sampleUsage = RequirementDefinitionDTO.builder()
            .id("U-001")
            .eClass("RequirementUsage")
            .of("R-001")
            .name("测试用法")
            .text("用法描述")
            .status("draft")
            .build();
            
        sampleTrace = TraceDTO.builder()
            .id("T-001")
            .fromId("R-001")
            .toId("R-002")
            .type("derive")
            .createdAt(Instant.now())
            .build();
    }
    
    /**
     * REQ-A2-1: 健康检查
     * AC: GET /health 响应 {"status":"UP"...}
     */
    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.timestamp").exists());
    }
    
    /**
     * REQ-C1-2 & REQ-C2-2: 获取所有需求（包括定义和用法）
     * AC: GET /api/v1/projects/{projectId}/requirements 返回所有需求
     */
    @Test
    void testGetProjectRequirements() throws Exception {
        List<RequirementDefinitionDTO> requirements = Arrays.asList(sampleDefinition, sampleUsage);
        when(requirementService.listRequirements()).thenReturn(requirements);
        
        mockMvc.perform(get("/api/v1/projects/default/requirements"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(2)))
            .andExpect(jsonPath("$.content[0].id").value("R-001"))
            .andExpect(jsonPath("$.content[1].id").value("U-001"))
            .andExpect(jsonPath("$.projectId").value("default"));
        
        verify(requirementService).listRequirements();
    }
    
    /**
     * REQ-C1-1: 创建需求定义
     * AC: POST /api/v1/projects/{projectId}/requirements 创建新需求
     */
    @Test
    void testCreateRequirement() throws Exception {
        RequirementDefinitionDTO.CreateRequest request = new RequirementDefinitionDTO.CreateRequest();
        request.setType("definition");
        request.setReqId("REQ-002");
        request.setName("新需求");
        request.setText("需求描述");
        
        when(requirementService.createRequirement(any())).thenReturn(sampleDefinition);
        
        mockMvc.perform(post("/api/v1/projects/default/requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("R-001"))
            .andExpect(jsonPath("$.reqId").value("REQ-001"));
        
        verify(requirementService).createRequirement(any());
    }
    
    /**
     * REQ-C1-2: 更新需求
     * AC: PUT /api/v1/projects/{projectId}/requirements/{id} 更新需求
     */
    @Test
    void testUpdateRequirement() throws Exception {
        RequirementDefinitionDTO.UpdateRequest request = new RequirementDefinitionDTO.UpdateRequest();
        request.setName("更新后的名称");
        request.setText("更新后的描述");
        
        RequirementDefinitionDTO updated = RequirementDefinitionDTO.builder()
            .id("R-001")
            .reqId("REQ-001")
            .name("更新后的名称")
            .text("更新后的描述")
            .build();
            
        when(requirementService.updateRequirement(eq("R-001"), any())).thenReturn(updated);
        
        mockMvc.perform(put("/api/v1/projects/default/requirements/R-001")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("更新后的名称"));
        
        verify(requirementService).updateRequirement(eq("R-001"), any());
    }
    
    /**
     * REQ-C1-2: 删除需求
     * AC: DELETE /api/v1/projects/{projectId}/requirements/{id} 删除需求
     */
    @Test
    void testDeleteRequirement() throws Exception {
        doNothing().when(requirementService).deleteRequirement("R-001");
        
        mockMvc.perform(delete("/api/v1/projects/default/requirements/R-001"))
            .andExpect(status().isNoContent());
        
        verify(requirementService).deleteRequirement("R-001");
    }
    
    /**
     * REQ-C3-2: 获取所有追溯关系
     * AC: GET /api/v1/projects/{projectId}/traces 返回所有追溯
     */
    @Test
    void testGetProjectTraces() throws Exception {
        List<TraceDTO> traces = Arrays.asList(sampleTrace);
        when(traceService.findAllTraces()).thenReturn(traces);
        
        mockMvc.perform(get("/api/v1/projects/default/traces"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.traces", hasSize(1)))
            .andExpect(jsonPath("$.traces[0].id").value("T-001"))
            .andExpect(jsonPath("$.traces[0].type").value("derive"))
            .andExpect(jsonPath("$.projectId").value("default"));
        
        verify(traceService).findAllTraces();
    }
    
    /**
     * REQ-C3-1: 创建追溯关系
     * AC: POST /api/v1/projects/{projectId}/requirements/{fromId}/traces 创建追溯
     */
    @Test
    void testCreateTrace() throws Exception {
        TraceDTO.CreateRequest request = new TraceDTO.CreateRequest("R-002", "derive");
        
        when(traceService.createTrace(eq("R-001"), any(TraceDTO.CreateRequest.class))).thenReturn(sampleTrace);
        
        mockMvc.perform(post("/api/v1/projects/default/requirements/R-001/traces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("T-001"))
            .andExpect(jsonPath("$.fromId").value("R-001"))
            .andExpect(jsonPath("$.toId").value("R-002"));
        
        verify(traceService).createTrace(eq("R-001"), any(TraceDTO.CreateRequest.class));
    }
    
    /**
     * REQ-C3-4: 删除追溯关系
     * AC: DELETE /api/v1/projects/{projectId}/traces/{traceId} 删除追溯
     */
    @Test
    void testDeleteTrace() throws Exception {
        doNothing().when(traceService).deleteTrace("T-001");
        
        mockMvc.perform(delete("/api/v1/projects/default/traces/T-001"))
            .andExpect(status().isNoContent());
        
        verify(traceService).deleteTrace("T-001");
    }
    
    
    /**
     * 测试空数据响应
     * AC: 列表类接口空集合返回200 + []（不使用204）
     */
    @Test
    void testEmptyListReturns200() throws Exception {
        when(requirementService.listRequirements()).thenReturn(Collections.emptyList());
        
        mockMvc.perform(get("/api/v1/projects/default/requirements"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(0)));
    }
}