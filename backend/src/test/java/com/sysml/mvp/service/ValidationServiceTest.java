package com.sysml.mvp.service;

import com.sysml.mvp.dto.ElementDTO;
import com.sysml.mvp.dto.ValidationResultDTO;
import com.sysml.mvp.dto.ValidationViolationDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ValidationService 测试用例
 * 
 * 需求对齐：
 * - REQ-B5-4: 验证服务 - 内部工具层验证功能
 * - REQ-E1-1: MVP规则集 - 仅检测3条核心规则
 * - REQ-E1-2: 规则码固定枚举 - DUP_REQID, CYCLE_DERIVE_REFINE, BROKEN_REF
 * - REQ-E1-3: 验证结果API格式 - 返回ValidationResultDTO
 * - REQ-C1-1: reqId唯一性验证
 * - REQ-C3-3: 追溯关系去重检测
 * - REQ-C3-4: 追溯关系语义约束验证
 */
@DisplayName("ValidationService测试 - REQ-B5-4")
public class ValidationServiceTest {

    @Mock
    private UniversalElementService universalElementService;
    
    private ValidationService validationService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validationService = new ValidationService(universalElementService);
    }
    
    /**
     * 验收标准：REQ-C1-1
     * 验证reqId唯一性 - 无重复时返回true
     */
    @Test
    @DisplayName("REQ-C1-1: reqId唯一性验证通过")
    public void testValidateReqIdUniqueness_ShouldReturnTrueWhenUnique() {
        // Given: 系统中不存在该reqId的需求
        String reqId = "EBS-L1-001";
        
        ElementDTO req1 = new ElementDTO();
        req1.setElementId("req-def-001");
        req1.setEClass("RequirementDefinition");
        req1.setProperty("reqId", "EBS-L1-002");
        
        List<ElementDTO> existingRequirements = Arrays.asList(req1);
        
        // When: 验证reqId唯一性
        when(universalElementService.queryElements("RequirementDefinition")).thenReturn(existingRequirements);
        
        boolean result = validationService.validateReqIdUniqueness(reqId);
        
        // Then: 应该返回true（唯一）
        assertTrue(result);
        
        verify(universalElementService).queryElements("RequirementDefinition");
    }
    
    /**
     * 验收标准：REQ-C1-1
     * 验证reqId唯一性 - 存在重复时返回false
     */
    @Test
    @DisplayName("REQ-C1-1: reqId唯一性验证失败")
    public void testValidateReqIdUniqueness_ShouldReturnFalseWhenDuplicate() {
        // Given: 系统中存在相同reqId的需求
        String reqId = "EBS-L1-001";
        
        ElementDTO req1 = new ElementDTO();
        req1.setElementId("req-def-001");
        req1.setEClass("RequirementDefinition");
        req1.setProperty("reqId", "EBS-L1-001"); // 重复的reqId
        
        List<ElementDTO> existingRequirements = Arrays.asList(req1);
        
        // When: 验证reqId唯一性
        when(universalElementService.queryElements("RequirementDefinition")).thenReturn(existingRequirements);
        
        boolean result = validationService.validateReqIdUniqueness(reqId);
        
        // Then: 应该返回false（重复）
        assertFalse(result);
        
        verify(universalElementService).queryElements("RequirementDefinition");
    }
    
    /**
     * 验收标准：REQ-C3-3
     * 验证追溯关系去重 - 无重复时返回true
     */
    @Test
    @DisplayName("REQ-C3-3: 追溯关系去重验证通过")
    public void testValidateTraceDuplication_ShouldReturnTrueWhenUnique() {
        // Given: 系统中不存在相同的追溯关系
        String source = "part-001";
        String target = "req-def-001";
        String type = "satisfy";
        
        ElementDTO satisfy1 = new ElementDTO();
        satisfy1.setElementId("satisfy-001");
        satisfy1.setEClass("Satisfy");
        satisfy1.setProperty("fromId", "part-002");  // 不同的source
        satisfy1.setProperty("toId", "req-def-001");
        
        List<ElementDTO> existingSatisfies = Arrays.asList(satisfy1);
        
        // When: 验证追溯关系去重
        when(universalElementService.queryElements("Satisfy")).thenReturn(existingSatisfies);
        
        boolean result = validationService.validateTraceDuplication(source, target, type);
        
        // Then: 应该返回true（不重复）
        assertTrue(result);
        
        verify(universalElementService).queryElements("Satisfy");
    }
    
    /**
     * 验收标准：REQ-C3-3
     * 验证追溯关系去重 - 存在重复时返回false
     */
    @Test
    @DisplayName("REQ-C3-3: 追溯关系去重验证失败")
    public void testValidateTraceDuplication_ShouldReturnFalseWhenDuplicate() {
        // Given: 系统中存在相同的追溯关系
        String source = "part-001";
        String target = "req-def-001";
        String type = "satisfy";
        
        ElementDTO satisfy1 = new ElementDTO();
        satisfy1.setElementId("satisfy-001");
        satisfy1.setEClass("Satisfy");
        satisfy1.setProperty("fromId", "part-001"); // 相同的source
        satisfy1.setProperty("toId", "req-def-001"); // 相同的target
        
        List<ElementDTO> existingSatisfies = Arrays.asList(satisfy1);
        
        // When: 验证追溯关系去重
        when(universalElementService.queryElements("Satisfy")).thenReturn(existingSatisfies);
        
        boolean result = validationService.validateTraceDuplication(source, target, type);
        
        // Then: 应该返回false（重复）
        assertFalse(result);
        
        verify(universalElementService).queryElements("Satisfy");
    }
    
    /**
     * 验收标准：REQ-C3-4
     * 验证Satisfy语义约束 - 有效组合时返回true
     */
    @Test
    @DisplayName("REQ-C3-4: Satisfy语义约束验证通过")
    public void testValidateTraceSemantics_ShouldReturnTrueForValidSatisfy() {
        // Given: 有效的Satisfy语义约束组合
        String source = "part-001";  // PartUsage
        String target = "req-def-001"; // RequirementDefinition
        String type = "satisfy";
        
        ElementDTO sourceElement = new ElementDTO();
        sourceElement.setElementId(source);
        sourceElement.setEClass("PartUsage");
        
        ElementDTO targetElement = new ElementDTO();
        targetElement.setElementId(target);
        targetElement.setEClass("RequirementDefinition");
        
        // When: 验证Satisfy语义约束
        when(universalElementService.findElementById(source)).thenReturn(sourceElement);
        when(universalElementService.findElementById(target)).thenReturn(targetElement);
        
        boolean result = validationService.validateTraceSemantics(source, target, type);
        
        // Then: 应该返回true（语义有效）
        assertTrue(result);
        
        verify(universalElementService).findElementById(source);
        verify(universalElementService).findElementById(target);
    }
    
    /**
     * 验收标准：REQ-C3-4
     * 验证Satisfy语义约束 - 无效组合时返回false
     */
    @Test
    @DisplayName("REQ-C3-4: Satisfy语义约束验证失败")
    public void testValidateTraceSemantics_ShouldReturnFalseForInvalidSatisfy() {
        // Given: 无效的Satisfy语义约束组合
        String source = "req-def-001";  // RequirementDefinition (应该是PartUsage)
        String target = "req-def-002"; // RequirementDefinition
        String type = "satisfy";
        
        ElementDTO sourceElement = new ElementDTO();
        sourceElement.setElementId(source);
        sourceElement.setEClass("RequirementDefinition"); // 错误的source类型
        
        ElementDTO targetElement = new ElementDTO();
        targetElement.setElementId(target);
        targetElement.setEClass("RequirementDefinition");
        
        // When: 验证Satisfy语义约束
        when(universalElementService.findElementById(source)).thenReturn(sourceElement);
        when(universalElementService.findElementById(target)).thenReturn(targetElement);
        
        boolean result = validationService.validateTraceSemantics(source, target, type);
        
        // Then: 应该返回false（语义无效）
        assertFalse(result);
        
        verify(universalElementService).findElementById(source);
        verify(universalElementService).findElementById(target);
    }
    
    /**
     * 验收标准：REQ-C3-4
     * 验证DeriveRequirement语义约束 - 有效组合时返回true
     */
    @Test
    @DisplayName("REQ-C3-4: DeriveRequirement语义约束验证通过")
    public void testValidateTraceSemantics_ShouldReturnTrueForValidDerive() {
        // Given: 有效的DeriveRequirement语义约束组合
        String source = "req-def-001";  // RequirementDefinition
        String target = "req-usage-001"; // RequirementUsage
        String type = "derive";
        
        ElementDTO sourceElement = new ElementDTO();
        sourceElement.setElementId(source);
        sourceElement.setEClass("RequirementDefinition");
        
        ElementDTO targetElement = new ElementDTO();
        targetElement.setElementId(target);
        targetElement.setEClass("RequirementUsage");
        
        // When: 验证DeriveRequirement语义约束
        when(universalElementService.findElementById(source)).thenReturn(sourceElement);
        when(universalElementService.findElementById(target)).thenReturn(targetElement);
        
        boolean result = validationService.validateTraceSemantics(source, target, type);
        
        // Then: 应该返回true（语义有效）
        assertTrue(result);
        
        verify(universalElementService).findElementById(source);
        verify(universalElementService).findElementById(target);
    }
    
    /**
     * 验收标准：REQ-C3-4
     * 获取语义验证消息 - Satisfy类型
     */
    @Test
    @DisplayName("REQ-C3-4: 获取Satisfy语义验证消息")
    public void testGetTraceSemanticValidationMessage_ShouldReturnSatisfyMessage() {
        // Given: Satisfy类型的语义验证
        String source = "part-001";
        String target = "req-def-001";
        String type = "satisfy";
        
        ElementDTO sourceElement = new ElementDTO();
        sourceElement.setElementId(source);
        sourceElement.setEClass("PartUsage");
        
        ElementDTO targetElement = new ElementDTO();
        targetElement.setElementId(target);
        targetElement.setEClass("RequirementDefinition");
        
        // When: 获取语义验证消息
        when(universalElementService.findElementById(source)).thenReturn(sourceElement);
        when(universalElementService.findElementById(target)).thenReturn(targetElement);
        
        String message = validationService.getTraceSemanticValidationMessage(source, target, type);
        
        // Then: 应该返回详细的验证消息
        assertNotNull(message);
        assertTrue(message.contains("satisfy"));
        assertTrue(message.contains("PartUsage"));
        assertTrue(message.contains("RequirementDefinition"));
        
        verify(universalElementService, times(2)).findElementById(source);
        verify(universalElementService, times(2)).findElementById(target);
    }
    
    /**
     * 验收标准：REQ-E1-3
     * 静态验证应返回ValidationResultDTO格式
     */
    @Test
    @DisplayName("REQ-E1-3: 静态验证返回ValidationResultDTO格式")
    public void testValidateStatic_ShouldReturnValidationResultDTO() {
        // Given: 包含违规的元素列表
        ElementDTO req1 = new ElementDTO();
        req1.setElementId("req-def-001");
        req1.setEClass("RequirementDefinition");
        req1.setProperty("reqId", "EBS-L1-001");
        
        ElementDTO req2 = new ElementDTO();
        req2.setElementId("req-def-002");
        req2.setEClass("RequirementDefinition");
        req2.setProperty("reqId", "EBS-L1-001"); // 重复的reqId
        
        List<ElementDTO> allElements = Arrays.asList(req1, req2);
        
        // When: 执行静态验证
        when(universalElementService.queryElements("RequirementDefinition")).thenReturn(Arrays.asList(req1, req2));
        when(universalElementService.queryElements("Satisfy")).thenReturn(new ArrayList<>());
        when(universalElementService.queryElements("DeriveRequirement")).thenReturn(new ArrayList<>());
        when(universalElementService.queryElements("Refine")).thenReturn(new ArrayList<>());
        when(universalElementService.queryElements("Trace")).thenReturn(new ArrayList<>());
        
        ValidationResultDTO result = validationService.validateStatic(allElements);
        
        // Then: 应该返回正确格式的ValidationResultDTO
        assertNotNull(result);
        assertNotNull(result.getViolations());
        assertTrue(result.hasViolations());
        assertEquals(1, result.getViolationCount()); // DUP_REQID违规
        
        // 验证违规信息
        ValidationViolationDTO violation = result.getViolations().get(0);
        assertEquals("DUP_REQID", violation.getRuleCode());
        assertTrue(violation.getMessage().contains("EBS-L1-001"));
        
        // 验证元数据
        assertNotNull(result.getValidatedAt());
        assertNotNull(result.getElementCount());
        assertNotNull(result.getProcessingTimeMs());
        assertNotNull(result.getVersion());
    }
    
    /**
     * 验收标准：REQ-E1-1
     * MVP规则集应检测3条核心规则
     */
    @Test
    @DisplayName("REQ-E1-1: MVP规则集检测3条核心规则")
    public void testValidateStatic_ShouldDetectThreeCoreRules() {
        // Given: 包含三种违规的元素
        // 1. DUP_REQID违规
        ElementDTO req1 = new ElementDTO();
        req1.setElementId("req-def-001");
        req1.setEClass("RequirementDefinition");
        req1.setProperty("reqId", "EBS-L1-001");
        
        ElementDTO req2 = new ElementDTO();
        req2.setElementId("req-def-002");
        req2.setEClass("RequirementDefinition");
        req2.setProperty("reqId", "EBS-L1-001"); // 重复
        
        // 2. CYCLE_DERIVE_REFINE违规
        ElementDTO derive1 = new ElementDTO();
        derive1.setElementId("derive-001");
        derive1.setEClass("DeriveRequirement");
        derive1.setProperty("fromId", "req-def-001");
        derive1.setProperty("toId", "req-def-002");
        
        ElementDTO derive2 = new ElementDTO();
        derive2.setElementId("derive-002");
        derive2.setEClass("DeriveRequirement");
        derive2.setProperty("fromId", "req-def-002");
        derive2.setProperty("toId", "req-def-001"); // 循环
        
        // 3. BROKEN_REF违规
        ElementDTO satisfy1 = new ElementDTO();
        satisfy1.setElementId("satisfy-001");
        satisfy1.setEClass("Satisfy");
        satisfy1.setProperty("fromId", "part-001");
        satisfy1.setProperty("toId", "non-existent-req"); // 悬挂引用
        
        List<ElementDTO> allElements = Arrays.asList(req1, req2);
        
        // When: 执行静态验证
        when(universalElementService.queryElements("RequirementDefinition")).thenReturn(Arrays.asList(req1, req2));
        when(universalElementService.queryElements("Satisfy")).thenReturn(Arrays.asList(satisfy1));
        when(universalElementService.queryElements("DeriveRequirement")).thenReturn(Arrays.asList(derive1, derive2));
        when(universalElementService.queryElements("Refine")).thenReturn(new ArrayList<>());
        when(universalElementService.queryElements("Trace")).thenReturn(new ArrayList<>());
        
        ValidationResultDTO result = validationService.validateStatic(allElements);
        
        // Then: 应该检测到三种违规
        assertNotNull(result);
        assertTrue(result.hasViolations());
        assertTrue(result.getViolationCount() >= 3);
        
        // 验证包含三种规则码
        List<String> ruleCodes = result.getViolations().stream()
            .map(ValidationViolationDTO::getRuleCode)
            .distinct()
            .toList();
        
        assertTrue(ruleCodes.contains("DUP_REQID"));
        assertTrue(ruleCodes.contains("CYCLE_DERIVE_REFINE"));
        assertTrue(ruleCodes.contains("BROKEN_REF"));
    }
    
    /**
     * 验收标准：REQ-E1-2
     * 规则码应为固定枚举
     */
    @Test
    @DisplayName("REQ-E1-2: 规则码固定枚举验证")
    public void testValidateStatic_ShouldUseFixedRuleCodes() {
        // Given: 包含违规的元素
        ElementDTO req1 = new ElementDTO();
        req1.setElementId("req-def-001");
        req1.setEClass("RequirementDefinition");
        req1.setProperty("reqId", "EBS-L1-001");
        
        ElementDTO req2 = new ElementDTO();
        req2.setElementId("req-def-002");
        req2.setEClass("RequirementDefinition");
        req2.setProperty("reqId", "EBS-L1-001"); // 重复
        
        List<ElementDTO> allElements = Arrays.asList(req1, req2);
        
        // When: 执行静态验证
        when(universalElementService.queryElements("RequirementDefinition")).thenReturn(Arrays.asList(req1, req2));
        when(universalElementService.queryElements("Satisfy")).thenReturn(new ArrayList<>());
        when(universalElementService.queryElements("DeriveRequirement")).thenReturn(new ArrayList<>());
        when(universalElementService.queryElements("Refine")).thenReturn(new ArrayList<>());
        when(universalElementService.queryElements("Trace")).thenReturn(new ArrayList<>());
        
        ValidationResultDTO result = validationService.validateStatic(allElements);
        
        // Then: 规则码应该是固定枚举值
        assertNotNull(result);
        if (result.hasViolations()) {
            for (ValidationViolationDTO violation : result.getViolations()) {
                String ruleCode = violation.getRuleCode();
                assertTrue(
                    "DUP_REQID".equals(ruleCode) || 
                    "CYCLE_DERIVE_REFINE".equals(ruleCode) || 
                    "BROKEN_REF".equals(ruleCode),
                    "Invalid rule code: " + ruleCode
                );
            }
        }
    }
    
    /**
     * 验收标准：REQ-E1-3
     * 验证结果应包含性能指标
     */
    @Test
    @DisplayName("REQ-E1-3: 验证结果包含性能指标")
    public void testValidateStatic_ShouldIncludePerformanceMetrics() {
        // Given: 少量元素（性能测试）
        List<ElementDTO> allElements = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            ElementDTO req = new ElementDTO();
            req.setElementId("req-def-" + String.format("%03d", i));
            req.setEClass("RequirementDefinition");
            req.setProperty("reqId", "EBS-L1-" + String.format("%03d", i));
            allElements.add(req);
        }
        
        // When: 执行静态验证
        when(universalElementService.queryElements("RequirementDefinition")).thenReturn(allElements);
        when(universalElementService.queryElements("Satisfy")).thenReturn(new ArrayList<>());
        when(universalElementService.queryElements("DeriveRequirement")).thenReturn(new ArrayList<>());
        when(universalElementService.queryElements("Refine")).thenReturn(new ArrayList<>());
        when(universalElementService.queryElements("Trace")).thenReturn(new ArrayList<>());
        
        long startTime = System.currentTimeMillis();
        ValidationResultDTO result = validationService.validateStatic(allElements);
        long endTime = System.currentTimeMillis();
        
        // Then: 应该包含性能指标
        assertNotNull(result);
        assertNotNull(result.getElementCount());
        assertNotNull(result.getProcessingTimeMs());
        assertEquals(100, result.getElementCount().intValue());
        assertTrue(result.getProcessingTimeMs() > 0);
        assertTrue(result.getProcessingTimeMs() < (endTime - startTime + 100)); // 合理范围
    }
}