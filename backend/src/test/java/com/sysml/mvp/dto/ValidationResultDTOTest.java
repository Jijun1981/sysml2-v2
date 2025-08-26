package com.sysml.mvp.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ValidationResultDTO 测试用例
 * 
 * 需求对齐：
 * - REQ-E1-3: 接口返回 - POST /api/v1/validate/static返回ValidationResultDTO格式
 * - REQ-E1-2: 规则码固定枚举 - DUP_REQID, CYCLE_DERIVE_REFINE, BROKEN_REF
 * - REQ-E1-1: MVP规则集 - 仅检测3条核心规则
 */
@DisplayName("ValidationResultDTO测试 - REQ-E1-3")
public class ValidationResultDTOTest {

    /**
     * 验收标准：REQ-E1-3
     * ValidationResultDTO应该包含violations列表
     */
    @Test
    @DisplayName("REQ-E1-3: ValidationResultDTO包含violations字段")
    public void testValidationResultDTO_ShouldHaveViolationsList() {
        // Given: 创建ValidationResultDTO
        ValidationResultDTO dto = new ValidationResultDTO();
        
        // When: 设置violations
        ValidationViolationDTO violation = new ValidationViolationDTO();
        violation.setRuleCode("DUP_REQID");
        violation.setTargetId("req-def-001");
        violation.setMessage("reqId duplicated: EBS-L1-001");
        
        dto.setViolations(Arrays.asList(violation));
        
        // Then: 能够获取violations列表
        assertNotNull(dto.getViolations());
        assertEquals(1, dto.getViolations().size());
        assertEquals("DUP_REQID", dto.getViolations().get(0).getRuleCode());
    }

    /**
     * 验收标准：REQ-E1-2
     * ValidationViolationDTO应该包含固定的规则码枚举
     */
    @Test
    @DisplayName("REQ-E1-2: 规则码固定枚举")
    public void testValidationViolationDTO_ShouldSupportFixedRuleCodes() {
        // Given: 三种固定的规则码
        String[] ruleCodes = {"DUP_REQID", "CYCLE_DERIVE_REFINE", "BROKEN_REF"};
        
        // When & Then: 每种规则码都应该被支持
        for (String ruleCode : ruleCodes) {
            ValidationViolationDTO violation = new ValidationViolationDTO();
            violation.setRuleCode(ruleCode);
            assertEquals(ruleCode, violation.getRuleCode(), "应该支持规则码: " + ruleCode);
        }
    }

    /**
     * 验收标准：REQ-E1-2
     * ValidationViolationDTO应该包含完整的违规信息
     */
    @Test
    @DisplayName("REQ-E1-2: 违规信息完整字段")
    public void testValidationViolationDTO_ShouldHaveCompleteViolationInfo() {
        // Given: 创建ValidationViolationDTO
        ValidationViolationDTO dto = new ValidationViolationDTO();
        
        // When: 设置完整字段
        dto.setRuleCode("DUP_REQID");
        dto.setTargetId("req-def-001");
        dto.setMessage("reqId duplicated: EBS-L1-001");
        dto.setDetails("Found duplicate reqId 'EBS-L1-001' in elements: req-def-001, req-def-002");
        
        // Then: 所有字段都应该可获取
        assertEquals("DUP_REQID", dto.getRuleCode());
        assertEquals("req-def-001", dto.getTargetId());
        assertEquals("reqId duplicated: EBS-L1-001", dto.getMessage());
        assertEquals("Found duplicate reqId 'EBS-L1-001' in elements: req-def-001, req-def-002", dto.getDetails());
    }

    /**
     * 验收标准：REQ-E1-1
     * ValidationResultDTO应该支持DUP_REQID规则
     */
    @Test
    @DisplayName("REQ-E1-1: DUP_REQID规则支持")
    public void testValidationResultDTO_ShouldSupportDupReqIdRule() {
        // Given: 创建DUP_REQID违规
        ValidationViolationDTO violation = new ValidationViolationDTO();
        violation.setRuleCode("DUP_REQID");
        violation.setTargetId("req-def-002");
        violation.setMessage("reqId duplicated: EBS-L1-001");
        violation.setDetails("Duplicate reqId found in req-def-001");
        
        ValidationResultDTO result = new ValidationResultDTO();
        result.setViolations(Arrays.asList(violation));
        
        // When & Then: DUP_REQID规则应该被正确处理
        assertEquals(1, result.getViolations().size());
        ValidationViolationDTO dupViolation = result.getViolations().get(0);
        assertEquals("DUP_REQID", dupViolation.getRuleCode());
        assertTrue(dupViolation.getMessage().contains("duplicated"));
    }

    /**
     * 验收标准：REQ-E1-1
     * ValidationResultDTO应该支持CYCLE_DERIVE_REFINE规则
     */
    @Test
    @DisplayName("REQ-E1-1: CYCLE_DERIVE_REFINE规则支持")
    public void testValidationResultDTO_ShouldSupportCycleDeriveRefineRule() {
        // Given: 创建CYCLE_DERIVE_REFINE违规
        ValidationViolationDTO violation = new ValidationViolationDTO();
        violation.setRuleCode("CYCLE_DERIVE_REFINE");
        violation.setTargetId("req-def-001");
        violation.setMessage("Circular dependency detected in derive/refine chain");
        violation.setDetails("Cycle: req-def-001 -> req-def-002 -> req-def-003 -> req-def-001");
        
        ValidationResultDTO result = new ValidationResultDTO();
        result.setViolations(Arrays.asList(violation));
        
        // When & Then: CYCLE_DERIVE_REFINE规则应该被正确处理
        assertEquals(1, result.getViolations().size());
        ValidationViolationDTO cycleViolation = result.getViolations().get(0);
        assertEquals("CYCLE_DERIVE_REFINE", cycleViolation.getRuleCode());
        assertTrue(cycleViolation.getMessage().contains("Circular dependency"));
    }

    /**
     * 验收标准：REQ-E1-1
     * ValidationResultDTO应该支持BROKEN_REF规则
     */
    @Test
    @DisplayName("REQ-E1-1: BROKEN_REF规则支持")
    public void testValidationResultDTO_ShouldSupportBrokenRefRule() {
        // Given: 创建BROKEN_REF违规
        ValidationViolationDTO violation = new ValidationViolationDTO();
        violation.setRuleCode("BROKEN_REF");
        violation.setTargetId("trace-001");
        violation.setMessage("Reference to non-existent element");
        violation.setDetails("Trace 'trace-001' references missing element 'req-def-999'");
        
        ValidationResultDTO result = new ValidationResultDTO();
        result.setViolations(Arrays.asList(violation));
        
        // When & Then: BROKEN_REF规则应该被正确处理
        assertEquals(1, result.getViolations().size());
        ValidationViolationDTO brokenRefViolation = result.getViolations().get(0);
        assertEquals("BROKEN_REF", brokenRefViolation.getRuleCode());
        assertTrue(brokenRefViolation.getMessage().contains("non-existent"));
    }

    /**
     * 验收标准：REQ-E1-3
     * ValidationResultDTO应该支持无违规的情况
     */
    @Test
    @DisplayName("REQ-E1-3: 无违规情况处理")
    public void testValidationResultDTO_ShouldHandleNoViolations() {
        // Given: 创建无违规的ValidationResultDTO
        ValidationResultDTO dto = new ValidationResultDTO();
        dto.setViolations(Collections.emptyList());
        
        // When: 检查是否有违规
        boolean hasViolations = dto.hasViolations();
        
        // Then: 应该正确识别无违规状态
        assertFalse(hasViolations);
        assertNotNull(dto.getViolations());
        assertTrue(dto.getViolations().isEmpty());
    }

    /**
     * 验收标准：REQ-E1-3
     * ValidationResultDTO应该支持多个违规
     */
    @Test
    @DisplayName("REQ-E1-3: 多个违规处理")
    public void testValidationResultDTO_ShouldHandleMultipleViolations() {
        // Given: 创建多个违规
        ValidationViolationDTO violation1 = new ValidationViolationDTO();
        violation1.setRuleCode("DUP_REQID");
        violation1.setTargetId("req-def-001");
        violation1.setMessage("reqId duplicated: EBS-L1-001");
        
        ValidationViolationDTO violation2 = new ValidationViolationDTO();
        violation2.setRuleCode("BROKEN_REF");
        violation2.setTargetId("trace-001");
        violation2.setMessage("Reference to non-existent element");
        
        ValidationResultDTO result = new ValidationResultDTO();
        result.setViolations(Arrays.asList(violation1, violation2));
        
        // When & Then: 应该支持多个违规
        assertTrue(result.hasViolations());
        assertEquals(2, result.getViolations().size());
        assertEquals(2, result.getViolationCount());
        
        // 验证每个违规的规则码
        List<String> ruleCodes = Arrays.asList(
            result.getViolations().get(0).getRuleCode(),
            result.getViolations().get(1).getRuleCode()
        );
        assertTrue(ruleCodes.contains("DUP_REQID"));
        assertTrue(ruleCodes.contains("BROKEN_REF"));
    }

    /**
     * 验收标准：REQ-E1-3
     * ValidationResultDTO应该包含验证元数据
     */
    @Test
    @DisplayName("REQ-E1-3: 验证元数据字段")
    public void testValidationResultDTO_ShouldContainValidationMetadata() {
        // Given: 创建ValidationResultDTO
        ValidationResultDTO dto = new ValidationResultDTO();
        
        // When: 设置验证元数据
        dto.setValidatedAt("2025-01-01T10:00:00.000Z");
        dto.setElementCount(150);
        dto.setProcessingTimeMs(850L);
        dto.setVersion("1.0");
        
        // Then: 元数据应该正确存储
        assertEquals("2025-01-01T10:00:00.000Z", dto.getValidatedAt());
        assertEquals(150, dto.getElementCount());
        assertEquals(850L, dto.getProcessingTimeMs());
        assertEquals("1.0", dto.getVersion());
    }

    /**
     * 验收标准：REQ-E1-3
     * ValidationResultDTO应该支持性能要求（≤500元素<2s）
     */
    @Test
    @DisplayName("REQ-E1-3: 性能要求支持")
    public void testValidationResultDTO_ShouldSupportPerformanceRequirements() {
        // Given: 创建符合性能要求的验证结果
        ValidationResultDTO dto = new ValidationResultDTO();
        dto.setElementCount(500); // 边界值
        dto.setProcessingTimeMs(1800L); // <2s
        
        // When & Then: 性能指标应该在要求范围内
        assertTrue(dto.getElementCount() <= 500, "元素数量应该≤500");
        assertTrue(dto.getProcessingTimeMs() < 2000, "处理时间应该<2s");
    }

    /**
     * 验收标准：REQ-E1-2
     * ValidationViolationDTO应该提供规则码验证便捷方法
     */
    @Test
    @DisplayName("REQ-E1-2: 规则码验证便捷方法")
    public void testValidationViolationDTO_ShouldProvideRuleCodeValidationMethods() {
        // Given: 创建不同规则码的违规
        ValidationViolationDTO dupViolation = new ValidationViolationDTO();
        dupViolation.setRuleCode("DUP_REQID");
        
        ValidationViolationDTO cycleViolation = new ValidationViolationDTO();
        cycleViolation.setRuleCode("CYCLE_DERIVE_REFINE");
        
        ValidationViolationDTO brokenRefViolation = new ValidationViolationDTO();
        brokenRefViolation.setRuleCode("BROKEN_REF");
        
        // When & Then: 便捷方法应该正确识别规则码类型
        assertTrue(dupViolation.isDuplicateReqId());
        assertFalse(dupViolation.isCyclicDependency());
        assertFalse(dupViolation.isBrokenReference());
        
        assertFalse(cycleViolation.isDuplicateReqId());
        assertTrue(cycleViolation.isCyclicDependency());
        assertFalse(cycleViolation.isBrokenReference());
        
        assertFalse(brokenRefViolation.isDuplicateReqId());
        assertFalse(brokenRefViolation.isCyclicDependency());
        assertTrue(brokenRefViolation.isBrokenReference());
    }

    /**
     * 验收标准：REQ-E1-3
     * ValidationResultDTO应该支持按规则码分组
     */
    @Test
    @DisplayName("REQ-E1-3: 按规则码分组统计")
    public void testValidationResultDTO_ShouldSupportGroupingByRuleCode() {
        // Given: 创建包含不同规则码的违规列表
        ValidationViolationDTO dup1 = new ValidationViolationDTO();
        dup1.setRuleCode("DUP_REQID");
        
        ValidationViolationDTO dup2 = new ValidationViolationDTO();
        dup2.setRuleCode("DUP_REQID");
        
        ValidationViolationDTO broken = new ValidationViolationDTO();
        broken.setRuleCode("BROKEN_REF");
        
        ValidationResultDTO result = new ValidationResultDTO();
        result.setViolations(Arrays.asList(dup1, dup2, broken));
        
        // When & Then: 应该支持按规则码统计
        assertEquals(3, result.getViolationCount());
        assertEquals(2, result.getViolationCountByRuleCode("DUP_REQID"));
        assertEquals(1, result.getViolationCountByRuleCode("BROKEN_REF"));
        assertEquals(0, result.getViolationCountByRuleCode("CYCLE_DERIVE_REFINE"));
    }
}