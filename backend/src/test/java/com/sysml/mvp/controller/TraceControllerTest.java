package com.sysml.mvp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sysml.mvp.dto.ElementDTO;
import com.sysml.mvp.dto.TraceDTO;
import com.sysml.mvp.service.TraceService;
import com.sysml.mvp.service.ValidationService;
import com.sysml.mvp.mapper.ElementMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TraceController测试 - REQ-A2-1
 * 
 * 需求实现：
 * - REQ-A2-1: 追溯关系CRUD API - 完整的REST API端点测试
 * - REQ-C3-1: 创建追溯关系 - POST /api/v1/traces
 * - REQ-C3-2: 查询追溯关系 - GET /api/v1/traces
 * - REQ-C3-3: 追溯关系去重检测 - 409 Conflict响应
 * - REQ-C3-4: 追溯关系语义约束验证 - 400 Bad Request响应
 * - REQ-C3-5: 删除追溯关系 - DELETE /api/v1/traces/{id}
 * 
 * 测试覆盖：
 * 1. 创建追溯关系成功场景
 * 2. 创建追溯关系重复冲突
 * 3. 创建追溯关系语义约束违反
 * 4. 查询所有追溯关系
 * 5. 根据类型查询追溯关系
 * 6. 根据源端查询追溯关系
 * 7. 根据目标端查询追溯关系
 * 8. 删除追溯关系成功
 * 9. 删除不存在的追溯关系
 */
@WebMvcTest(TraceController.class)
@DisplayName("TraceController测试 - REQ-A2-1")
public class TraceControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private TraceService traceService;
    
    @MockBean
    private ValidationService validationService;
    
    @MockBean
    private ElementMapper elementMapper;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private TraceDTO sampleTraceDTO;
    private ElementDTO sampleElementDTO;
    private Map<String, Object> sampleElementData;
    
    @BeforeEach
    void setUp() {
        // 准备测试数据
        sampleTraceDTO = new TraceDTO();
        sampleTraceDTO.setSource("req-001");
        sampleTraceDTO.setTarget("req-002");
        sampleTraceDTO.setType("derive");
        
        sampleElementDTO = new ElementDTO();
        sampleElementDTO.setElementId("trace-001");
        sampleElementDTO.setEClass("DeriveRequirement");
        sampleElementDTO.setProperty("fromId", "req-001");
        sampleElementDTO.setProperty("toId", "req-002");
        sampleElementDTO.setProperty("type", "derive");
        
        sampleElementData = new HashMap<>();
        sampleElementData.put("fromId", "req-001");
        sampleElementData.put("toId", "req-002");
        sampleElementData.put("type", "derive");
    }
    
    @Test
    @DisplayName("【REQ-C3-1】创建追溯关系 - 成功场景")
    void testCreateTrace_Success() throws Exception {
        // 模拟服务调用
        when(validationService.validateTraceDuplication("req-001", "req-002", "derive"))
            .thenReturn(true);
        when(validationService.validateTraceSemantics("req-001", "req-002", "derive"))
            .thenReturn(true);
        when(elementMapper.toElementData(any(TraceDTO.class)))
            .thenReturn(sampleElementData);
        when(traceService.createTrace(sampleElementData))
            .thenReturn(sampleElementDTO);
        when(elementMapper.toTraceDTO(sampleElementDTO))
            .thenReturn(sampleTraceDTO);
        
        mockMvc.perform(post("/api/v1/traces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleTraceDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("req-001"))
                .andExpect(jsonPath("$.target").value("req-002"))
                .andExpect(jsonPath("$.type").value("derive"));
    }
    
    @Test
    @DisplayName("【REQ-C3-3】创建追溯关系 - 重复冲突")
    void testCreateTrace_Duplicate() throws Exception {
        // 模拟重复检测失败
        when(validationService.validateTraceDuplication("req-001", "req-002", "derive"))
            .thenReturn(false);
        
        mockMvc.perform(post("/api/v1/traces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleTraceDTO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    @DisplayName("【REQ-C3-4】创建追溯关系 - 语义约束违反")
    void testCreateTrace_SemanticViolation() throws Exception {
        // 模拟语义验证失败
        when(validationService.validateTraceDuplication("req-001", "req-002", "derive"))
            .thenReturn(true);
        when(validationService.validateTraceSemantics("req-001", "req-002", "derive"))
            .thenReturn(false);
        when(validationService.getTraceSemanticValidationMessage("req-001", "req-002", "derive"))
            .thenReturn("Invalid derive relationship: PartUsage cannot derive RequirementDefinition");
        
        mockMvc.perform(post("/api/v1/traces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sampleTraceDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid derive relationship: PartUsage cannot derive RequirementDefinition"));
    }
    
    @Test
    @DisplayName("【REQ-C3-2】查询所有追溯关系")
    void testGetAllTraces() throws Exception {
        // 准备测试数据
        TraceDTO trace1 = new TraceDTO();
        trace1.setSource("req-001");
        trace1.setTarget("req-002");
        trace1.setType("derive");
        
        TraceDTO trace2 = new TraceDTO();
        trace2.setSource("req-002");
        trace2.setTarget("part-001");
        trace2.setType("satisfy");
        
        List<ElementDTO> elementList = Arrays.asList(sampleElementDTO);
        List<TraceDTO> traceDTOList = Arrays.asList(trace1, trace2);
        
        when(traceService.getAllTraces()).thenReturn(elementList);
        when(elementMapper.toTraceDTOList(elementList)).thenReturn(traceDTOList);
        
        mockMvc.perform(get("/api/v1/traces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("derive"))
                .andExpect(jsonPath("$[1].type").value("satisfy"));
    }
    
    @Test
    @DisplayName("【REQ-C3-2】根据类型查询追溯关系")
    void testGetTracesByType() throws Exception {
        List<ElementDTO> elementList = Arrays.asList(sampleElementDTO);
        List<TraceDTO> traceDTOList = Arrays.asList(sampleTraceDTO);
        
        when(traceService.getTracesByType("derive")).thenReturn(elementList);
        when(elementMapper.toTraceDTOList(elementList)).thenReturn(traceDTOList);
        
        mockMvc.perform(get("/api/v1/traces")
                .param("type", "derive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("derive"));
    }
    
    @Test
    @DisplayName("【REQ-C3-2】根据源端查询追溯关系")
    void testGetTracesByFromId() throws Exception {
        List<ElementDTO> elementList = Arrays.asList(sampleElementDTO);
        List<TraceDTO> traceDTOList = Arrays.asList(sampleTraceDTO);
        
        when(traceService.getTracesByFromId("req-001")).thenReturn(elementList);
        when(elementMapper.toTraceDTOList(elementList)).thenReturn(traceDTOList);
        
        mockMvc.perform(get("/api/v1/traces")
                .param("fromId", "req-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].source").value("req-001"));
    }
    
    @Test
    @DisplayName("【REQ-C3-2】根据目标端查询追溯关系")
    void testGetTracesByToId() throws Exception {
        List<ElementDTO> elementList = Arrays.asList(sampleElementDTO);
        List<TraceDTO> traceDTOList = Arrays.asList(sampleTraceDTO);
        
        when(traceService.getTracesByToId("req-002")).thenReturn(elementList);
        when(elementMapper.toTraceDTOList(elementList)).thenReturn(traceDTOList);
        
        mockMvc.perform(get("/api/v1/traces")
                .param("toId", "req-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].target").value("req-002"));
    }
    
    @Test
    @DisplayName("【REQ-C3-5】删除追溯关系 - 成功")
    void testDeleteTrace_Success() throws Exception {
        mockMvc.perform(delete("/api/v1/traces/trace-001"))
                .andExpect(status().isNoContent());
    }
    
    @Test
    @DisplayName("【REQ-C3-5】删除追溯关系 - 不存在")
    void testDeleteTrace_NotFound() throws Exception {
        doThrow(new IllegalArgumentException("Trace not found: trace-999"))
                .when(traceService).deleteTrace("trace-999");
        
        mockMvc.perform(delete("/api/v1/traces/trace-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Trace not found: trace-999"));
    }
}