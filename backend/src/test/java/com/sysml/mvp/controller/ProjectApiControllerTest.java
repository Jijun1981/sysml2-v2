package com.sysml.mvp.controller;

import com.sysml.mvp.dto.RequirementDefinitionDTO;
import com.sysml.mvp.dto.TraceDTO;
import com.sysml.mvp.service.RequirementService;
import com.sysml.mvp.service.TraceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 项目API测试 - TDD实现
 * REQ-D0-1: 项目数据API
 * REQ-D0-2: 追溯数据API
 */
@WebMvcTest(ApiController.class)
@DisplayName("REQ-D0: 前端数据初始化API测试")
public class ProjectApiControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private RequirementService requirementService;
    
    @MockBean
    private TraceService traceService;
    
    private RequirementDefinitionDTO sampleDefinition;
    private RequirementDefinitionDTO sampleUsage;
    private TraceDTO sampleTrace;
    
    @BeforeEach
    void setUp() {
        // 准备测试数据 - Definition
        sampleDefinition = RequirementDefinitionDTO.builder()
            .id("R-001")
            .eClass("RequirementDefinition")  // 必须包含eClass字段
            .reqId("REQ-001")
            .name("功能需求")
            .text("系统应该提供用户登录功能")
            .tags(Arrays.asList("critical"))
            .createdAt(Instant.now())
            .build();
            
        // 准备测试数据 - Usage
        sampleUsage = RequirementDefinitionDTO.builder()
            .id("U-001")
            .eClass("RequirementUsage")  // 必须包含eClass字段
            .of("R-001")
            .name("Web登录实现")
            .text("基于REST API的登录实现")
            .status("draft")
            .build();
            
        // 准备测试数据 - Trace
        sampleTrace = TraceDTO.builder()
            .id("T-001")
            .fromId("R-001")
            .toId("R-002")
            .type("derive")
            .createdAt(Instant.now())
            .build();
    }
    
    /**
     * REQ-D0-1: 项目数据API
     * AC: GET /api/v1/projects/{projectId}/requirements 返回 {projectId, content:[需求列表]}
     * content包含所有RequirementDefinition和RequirementUsage，每个对象必须包含eClass字段
     */
    @Test
    @DisplayName("REQ-D0-1: GET /api/v1/projects/{projectId}/requirements 返回项目需求数据")
    void testGetProjectRequirements() throws Exception {
        // Given: 模拟服务返回数据
        List<RequirementDefinitionDTO> allRequirements = Arrays.asList(
            sampleDefinition,
            sampleUsage
        );
        when(requirementService.listRequirements()).thenReturn(allRequirements);
        
        // When & Then: 调用API并验证
        mockMvc.perform(get("/api/v1/projects/default/requirements"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.projectId").value("default"))
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content", hasSize(2)))
            // 验证Definition
            .andExpect(jsonPath("$.content[0].id").value("R-001"))
            .andExpect(jsonPath("$.content[0].eClass").value("RequirementDefinition"))
            .andExpect(jsonPath("$.content[0].reqId").value("REQ-001"))
            .andExpect(jsonPath("$.content[0].name").value("功能需求"))
            // 验证Usage
            .andExpect(jsonPath("$.content[1].id").value("U-001"))
            .andExpect(jsonPath("$.content[1].eClass").value("RequirementUsage"))
            .andExpect(jsonPath("$.content[1].of").value("R-001"))
            .andExpect(jsonPath("$.content[1].name").value("Web登录实现"));
        
        verify(requirementService).listRequirements();
    }
    
    /**
     * REQ-D0-2: 追溯数据API
     * AC: GET /api/v1/projects/{projectId}/traces 返回 {projectId, traces:[追溯列表]}
     */
    @Test
    @DisplayName("REQ-D0-2: GET /api/v1/projects/{projectId}/traces 返回项目追溯数据")
    void testGetProjectTraces() throws Exception {
        // Given: 模拟服务返回数据
        List<TraceDTO> allTraces = Arrays.asList(sampleTrace);
        when(traceService.findAllTraces()).thenReturn(allTraces);
        
        // When & Then: 调用API并验证
        mockMvc.perform(get("/api/v1/projects/default/traces"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.projectId").value("default"))
            .andExpect(jsonPath("$.traces").isArray())
            .andExpect(jsonPath("$.traces", hasSize(1)))
            .andExpect(jsonPath("$.traces[0].id").value("T-001"))
            .andExpect(jsonPath("$.traces[0].fromId").value("R-001"))
            .andExpect(jsonPath("$.traces[0].toId").value("R-002"))
            .andExpect(jsonPath("$.traces[0].type").value("derive"));
        
        verify(traceService).findAllTraces();
    }
    
    /**
     * REQ-D0-1: 验证eClass字段存在性
     * AC: 每个对象必须包含eClass字段
     */
    @Test
    @DisplayName("REQ-D0-1: 验证返回的每个需求对象都包含eClass字段")
    void testRequirementsContainEClassField() throws Exception {
        // Given: 准备包含多个需求的列表
        RequirementDefinitionDTO def2 = RequirementDefinitionDTO.builder()
            .id("R-002")
            .eClass("RequirementDefinition")
            .reqId("REQ-002")
            .name("性能需求")
            .text("系统响应时间不超过500ms")
            .build();
            
        List<RequirementDefinitionDTO> requirements = Arrays.asList(
            sampleDefinition,
            sampleUsage,
            def2
        );
        when(requirementService.listRequirements()).thenReturn(requirements);
        
        // When & Then: 验证所有对象都有eClass字段
        mockMvc.perform(get("/api/v1/projects/test-project/requirements"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.projectId").value("test-project"))
            .andExpect(jsonPath("$.content[0].eClass").exists())
            .andExpect(jsonPath("$.content[1].eClass").exists())
            .andExpect(jsonPath("$.content[2].eClass").exists())
            .andExpect(jsonPath("$.content[*].eClass").isNotEmpty());
    }
    
    /**
     * REQ-D0-1 & REQ-D0-2: 空数据场景
     * AC: 没有数据时返回空数组
     */
    @Test
    @DisplayName("REQ-D0-1/D0-2: 没有数据时返回空数组")
    void testEmptyDataScenario() throws Exception {
        // Given: 没有数据
        when(requirementService.listRequirements()).thenReturn(Arrays.asList());
        when(traceService.listAllTraces()).thenReturn(Arrays.asList());
        
        // When & Then: 需求API返回空数组
        mockMvc.perform(get("/api/v1/projects/empty-project/requirements"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.projectId").value("empty-project"))
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content", hasSize(0)));
        
        // When & Then: 追溯API返回空数组
        mockMvc.perform(get("/api/v1/projects/empty-project/traces"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.projectId").value("empty-project"))
            .andExpect(jsonPath("$.traces").isArray())
            .andExpect(jsonPath("$.traces", hasSize(0)));
    }
}