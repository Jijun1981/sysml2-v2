package com.sysml.mvp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sysml.mvp.dto.ElementDTO;
import com.sysml.mvp.dto.ValidationResultDTO;
import com.sysml.mvp.dto.ValidationViolationDTO;
import com.sysml.mvp.service.ValidationService;
import com.sysml.mvp.service.UniversalElementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ValidationController测试 - REQ-A3-1
 * 
 * 需求实现：
 * - REQ-A3-1: 验证API - 完整的REST API端点测试
 * - REQ-E1-1: MVP规则集 - 仅检测3条核心规则
 * - REQ-E1-2: 规则码固定枚举 - DUP_REQID, CYCLE_DERIVE_REFINE, BROKEN_REF
 * - REQ-E1-3: 验证结果API格式 - 返回ValidationResultDTO
 * - REQ-C1-1: reqId唯一性验证 - 支持reqId重复检测
 * - REQ-C3-3: 追溯关系去重检测 - 支持追溯关系重复验证
 * - REQ-C3-4: 追溯关系语义约束验证 - 支持语义约束检查
 * 
 * 测试覆盖：
 * 1. 静态验证接口 - 全量模型验证
 * 2. reqId唯一性验证接口
 * 3. 追溯关系重复性验证接口  
 * 4. 追溯关系语义约束验证接口
 * 5. 验证结果格式检查
 * 6. 错误场景处理
 */
@WebMvcTest(ValidationController.class)
@DisplayName("ValidationController测试 - REQ-A3-1")
public class ValidationControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ValidationService validationService;
    
    @MockBean
    private UniversalElementService universalElementService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private ValidationResultDTO sampleValidationResult;
    private List<ValidationViolationDTO> sampleViolations;
    private List<ElementDTO> sampleElements;
    
    @BeforeEach
    void setUp() {
        // 准备测试数据
        setupSampleViolations();
        setupSampleValidationResult();
        setupSampleElements();
    }
    
    private void setupSampleViolations() {
        ValidationViolationDTO violation1 = new ValidationViolationDTO();
        violation1.setRuleCode("DUP_REQID");
        violation1.setTargetId("req-002");
        violation1.setMessage("reqId duplicated: REQ-USER-001");
        violation1.setDetails("Found duplicate reqId 'REQ-USER-001' in 2 elements");
        
        ValidationViolationDTO violation2 = new ValidationViolationDTO();
        violation2.setRuleCode("BROKEN_REF");
        violation2.setTargetId("derive-001");
        violation2.setMessage("Reference to non-existent element");
        violation2.setDetails("Trace 'derive-001' references missing element 'req-999'");
        
        sampleViolations = Arrays.asList(violation1, violation2);
    }
    
    private void setupSampleValidationResult() {
        sampleValidationResult = new ValidationResultDTO();
        sampleValidationResult.setViolations(sampleViolations);
        sampleValidationResult.setValidatedAt("2025-08-25T12:00:00.000Z");
        sampleValidationResult.setElementCount(150);
        sampleValidationResult.setProcessingTimeMs(1500L);
        sampleValidationResult.setVersion("1.0");
    }
    
    private void setupSampleElements() {
        ElementDTO element1 = new ElementDTO();
        element1.setElementId("req-001");
        element1.setEClass("RequirementDefinition");
        
        ElementDTO element2 = new ElementDTO();
        element2.setElementId("trace-001");
        element2.setEClass("DeriveRequirement");
        
        sampleElements = Arrays.asList(element1, element2);
    }
    
    @Test
    @DisplayName("【REQ-E1-3】静态验证 - 全量模型验证成功")
    void testValidateStatic_Success() throws Exception {
        // 模拟服务调用
        when(universalElementService.getAllElements()).thenReturn(sampleElements);
        when(validationService.validateStatic(sampleElements)).thenReturn(sampleValidationResult);
        
        mockMvc.perform(post("/api/v1/validation/static")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.violations.length()").value(2))
                .andExpect(jsonPath("$.violations[0].ruleCode").value("DUP_REQID"))
                .andExpect(jsonPath("$.violations[1].ruleCode").value("BROKEN_REF"))
                .andExpect(jsonPath("$.validatedAt").value("2025-08-25T12:00:00.000Z"))
                .andExpect(jsonPath("$.elementCount").value(150))
                .andExpect(jsonPath("$.processingTimeMs").value(1500))
                .andExpect(jsonPath("$.version").value("1.0"));
    }
    
    @Test
    @DisplayName("【REQ-E1-3】静态验证 - 无违规场景")
    void testValidateStatic_NoViolations() throws Exception {
        // 准备无违规结果
        ValidationResultDTO cleanResult = new ValidationResultDTO();
        cleanResult.setViolations(Arrays.asList()); // 空违规列表
        cleanResult.setValidatedAt("2025-08-25T12:00:00.000Z");
        cleanResult.setElementCount(50);
        cleanResult.setProcessingTimeMs(800L);
        cleanResult.setVersion("1.0");
        
        when(universalElementService.getAllElements()).thenReturn(sampleElements);
        when(validationService.validateStatic(sampleElements)).thenReturn(cleanResult);
        
        mockMvc.perform(post("/api/v1/validation/static")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.violations.length()").value(0))
                .andExpect(jsonPath("$.elementCount").value(50))
                .andExpect(jsonPath("$.processingTimeMs").value(800));
    }
    
    @Test
    @DisplayName("【REQ-C1-1】reqId唯一性验证 - reqId可用")
    void testValidateReqIdUniqueness_Available() throws Exception {
        String reqId = "REQ-NEW-001";
        when(validationService.validateReqIdUniqueness(reqId)).thenReturn(true);
        
        mockMvc.perform(get("/api/v1/validation/reqId/{reqId}", reqId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reqId").value(reqId))
                .andExpect(jsonPath("$.isUnique").value(true))
                .andExpect(jsonPath("$.message").value("reqId is available"));
    }
    
    @Test
    @DisplayName("【REQ-C1-1】reqId唯一性验证 - reqId重复")
    void testValidateReqIdUniqueness_Duplicate() throws Exception {
        String reqId = "REQ-EXISTING-001";
        when(validationService.validateReqIdUniqueness(reqId)).thenReturn(false);
        
        mockMvc.perform(get("/api/v1/validation/reqId/{reqId}", reqId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reqId").value(reqId))
                .andExpect(jsonPath("$.isUnique").value(false))
                .andExpect(jsonPath("$.message").value("reqId already exists"));
    }
    
    @Test
    @DisplayName("【REQ-C3-3】追溯关系去重验证 - 无重复")
    void testValidateTraceDuplication_NoDuplicate() throws Exception {
        when(validationService.validateTraceDuplication("req-001", "req-002", "derive"))
                .thenReturn(true);
        
        mockMvc.perform(get("/api/v1/validation/trace/duplication")
                .param("source", "req-001")
                .param("target", "req-002")
                .param("type", "derive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("req-001"))
                .andExpect(jsonPath("$.target").value("req-002"))
                .andExpect(jsonPath("$.type").value("derive"))
                .andExpect(jsonPath("$.isDuplicate").value(false))
                .andExpect(jsonPath("$.message").value("Trace relationship is unique"));
    }
    
    @Test
    @DisplayName("【REQ-C3-3】追溯关系去重验证 - 存在重复")
    void testValidateTraceDuplication_Duplicate() throws Exception {
        when(validationService.validateTraceDuplication("req-001", "req-002", "derive"))
                .thenReturn(false);
        
        mockMvc.perform(get("/api/v1/validation/trace/duplication")
                .param("source", "req-001")
                .param("target", "req-002")
                .param("type", "derive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("req-001"))
                .andExpect(jsonPath("$.target").value("req-002"))
                .andExpect(jsonPath("$.type").value("derive"))
                .andExpect(jsonPath("$.isDuplicate").value(true))
                .andExpect(jsonPath("$.message").value("Trace relationship already exists"));
    }
    
    @Test
    @DisplayName("【REQ-C3-4】追溯关系语义验证 - 语义有效")
    void testValidateTraceSemantics_Valid() throws Exception {
        String validationMessage = "Valid derive relationship: RequirementDefinition can derive RequirementUsage";
        
        when(validationService.validateTraceSemantics("req-001", "req-002", "derive"))
                .thenReturn(true);
        when(validationService.getTraceSemanticValidationMessage("req-001", "req-002", "derive"))
                .thenReturn(validationMessage);
        
        mockMvc.perform(get("/api/v1/validation/trace/semantics")
                .param("source", "req-001")
                .param("target", "req-002")
                .param("type", "derive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("req-001"))
                .andExpect(jsonPath("$.target").value("req-002"))
                .andExpect(jsonPath("$.type").value("derive"))
                .andExpect(jsonPath("$.isValid").value(true))
                .andExpect(jsonPath("$.message").value(validationMessage));
    }
    
    @Test
    @DisplayName("【REQ-C3-4】追溯关系语义验证 - 语义无效")
    void testValidateTraceSemantics_Invalid() throws Exception {
        String validationMessage = "Invalid derive relationship: PartUsage cannot derive RequirementDefinition";
        
        when(validationService.validateTraceSemantics("part-001", "req-001", "derive"))
                .thenReturn(false);
        when(validationService.getTraceSemanticValidationMessage("part-001", "req-001", "derive"))
                .thenReturn(validationMessage);
        
        mockMvc.perform(get("/api/v1/validation/trace/semantics")
                .param("source", "part-001")
                .param("target", "req-001")
                .param("type", "derive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("part-001"))
                .andExpect(jsonPath("$.target").value("req-001"))
                .andExpect(jsonPath("$.type").value("derive"))
                .andExpect(jsonPath("$.isValid").value(false))
                .andExpect(jsonPath("$.message").value(validationMessage));
    }
    
    @Test
    @DisplayName("【REQ-C3-4】追溯关系语义验证 - 缺少参数")
    void testValidateTraceSemantics_MissingParameters() throws Exception {
        mockMvc.perform(get("/api/v1/validation/trace/semantics")
                .param("source", "req-001")
                .param("type", "derive"))
                // 缺少target参数
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").exists());
    }
    
    @Test
    @DisplayName("【REQ-A3-1】综合验证接口 - 组合多种验证")
    void testComprehensiveValidation() throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("reqId", "REQ-TEST-001");
        requestBody.put("source", "req-001");
        requestBody.put("target", "req-002");
        requestBody.put("type", "derive");
        
        // 模拟各项验证结果
        when(validationService.validateReqIdUniqueness("REQ-TEST-001")).thenReturn(true);
        when(validationService.validateTraceDuplication("req-001", "req-002", "derive")).thenReturn(true);
        when(validationService.validateTraceSemantics("req-001", "req-002", "derive")).thenReturn(true);
        when(validationService.getTraceSemanticValidationMessage("req-001", "req-002", "derive"))
                .thenReturn("Valid derive relationship");
        
        mockMvc.perform(post("/api/v1/validation/comprehensive")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reqIdValidation.isUnique").value(true))
                .andExpect(jsonPath("$.traceValidation.isDuplicate").value(false))
                .andExpect(jsonPath("$.traceValidation.isSemanticValid").value(true))
                .andExpect(jsonPath("$.overallValid").value(true));
    }
}