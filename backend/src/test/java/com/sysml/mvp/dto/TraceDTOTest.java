package com.sysml.mvp.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TraceDTO 测试用例
 * 
 * 需求对齐：
 * - REQ-C3-1: 创建追溯关系 - TraceDTO用于API层简化表示
 * - REQ-C3-2: 查询追溯关系 - 返回TraceDTO格式
 * - REQ-C3-4: 追溯语义约束 - type字段映射到具体EClass
 */
@DisplayName("TraceDTO测试 - REQ-C3-1")
public class TraceDTOTest {

    /**
     * 验收标准：REQ-C3-1
     * TraceDTO应该包含追溯关系的核心字段
     */
    @Test
    @DisplayName("REQ-C3-1: TraceDTO包含核心追溯字段")
    public void testTraceDTO_ShouldHaveCoreTraceFields() {
        // Given: 创建TraceDTO
        TraceDTO dto = new TraceDTO();
        
        // When: 设置核心字段
        dto.setElementId("trace-001");
        dto.setSource("req-usage-001");
        dto.setTarget("req-def-001");
        dto.setType("satisfy");
        
        // Then: 能够获取所有核心字段
        assertEquals("trace-001", dto.getElementId());
        assertEquals("req-usage-001", dto.getSource());
        assertEquals("req-def-001", dto.getTarget());
        assertEquals("satisfy", dto.getType());
    }

    /**
     * 验收标准：REQ-C3-2
     * TraceDTO应该支持四种标准追溯类型
     */
    @Test
    @DisplayName("REQ-C3-2: 支持专门追溯类型")
    public void testTraceDTO_ShouldSupportFourTraceTypes() {
        // Given: 四种标准追溯类型
        String[] traceTypes = {"derive", "satisfy", "refine", "trace"};
        
        // When & Then: 每种类型都应该被正确支持
        for (String type : traceTypes) {
            TraceDTO dto = new TraceDTO();
            dto.setType(type);
            assertEquals(type, dto.getType(), "应该支持追溯类型: " + type);
        }
    }

    /**
     * 验收标准：REQ-C3-1
     * TraceDTO应该正确映射source和target字段
     */
    @Test
    @DisplayName("REQ-C3-1: source/target字段映射")
    public void testTraceDTO_ShouldMapSourceAndTargetFields() {
        // Given: 创建TraceDTO
        TraceDTO dto = new TraceDTO();
        
        // When: 设置source和target（API层概念）
        dto.setSource("req-usage-001");    // API层：fromId
        dto.setTarget("req-def-001");      // API层：toId
        
        // Then: 应该正确存储和获取
        assertEquals("req-usage-001", dto.getSource());
        assertEquals("req-def-001", dto.getTarget());
    }

    /**
     * 验收标准：REQ-C3-4
     * TraceDTO应该支持语义约束相关的字段
     */
    @Test
    @DisplayName("REQ-C3-4: 追溯语义约束字段")
    public void testTraceDTO_ShouldSupportSemanticConstraintFields() {
        // Given: 创建TraceDTO
        TraceDTO dto = new TraceDTO();
        
        // When: 设置语义约束相关字段
        dto.setType("satisfy");
        dto.setSourceType("RequirementUsage");
        dto.setTargetType("RequirementDefinition");
        dto.setValid(true);
        dto.setValidationMessage("Valid satisfy relationship");
        
        // Then: 语义约束字段应该正确存储
        assertEquals("satisfy", dto.getType());
        assertEquals("RequirementUsage", dto.getSourceType());
        assertEquals("RequirementDefinition", dto.getTargetType());
        assertTrue(dto.getValid());
        assertEquals("Valid satisfy relationship", dto.getValidationMessage());
    }

    /**
     * 验收标准：REQ-C3-1
     * TraceDTO应该包含追溯关系的描述信息
     */
    @Test
    @DisplayName("REQ-C3-1: 追溯关系描述信息")
    public void testTraceDTO_ShouldSupportDescriptiveFields() {
        // Given: 创建TraceDTO
        TraceDTO dto = new TraceDTO();
        
        // When: 设置描述字段
        dto.setElementId("trace-001");
        dto.setName("Battery Temperature Monitoring Trace");
        dto.setDescription("This trace shows how the temperature monitoring requirement is satisfied by sensor implementation");
        dto.setRationale("Required for safety compliance");
        
        // Then: 描述信息应该正确存储
        assertEquals("trace-001", dto.getElementId());
        assertEquals("Battery Temperature Monitoring Trace", dto.getName());
        assertNotNull(dto.getDescription());
        assertEquals("Required for safety compliance", dto.getRationale());
    }

    /**
     * 验收标准：REQ-C3-3
     * TraceDTO应该支持依赖去重相关的字段
     */
    @Test
    @DisplayName("REQ-C3-3: 依赖去重支持")
    public void testTraceDTO_ShouldSupportDuplicateDetection() {
        // Given: 创建两个相同的追溯关系
        TraceDTO dto1 = new TraceDTO();
        dto1.setSource("req-usage-001");
        dto1.setTarget("req-def-001");
        dto1.setType("satisfy");
        
        TraceDTO dto2 = new TraceDTO();
        dto2.setSource("req-usage-001");
        dto2.setTarget("req-def-001");
        dto2.setType("satisfy");
        
        // When: 检查是否为重复关系
        boolean isDuplicate = dto1.getSource().equals(dto2.getSource()) &&
                             dto1.getTarget().equals(dto2.getTarget()) &&
                             dto1.getType().equals(dto2.getType());
        
        // Then: 应该能检测到重复
        assertTrue(isDuplicate, "应该能检测相同(source,target,type)的重复关系");
    }

    /**
     * 验收标准：REQ-C3-2
     * TraceDTO应该包含查询响应所需的所有字段
     */
    @Test
    @DisplayName("REQ-C3-2: 查询追溯返回完整信息")
    public void testTraceDTO_ShouldContainQueryResponseFields() {
        // Given: 创建完整的TraceDTO
        TraceDTO dto = new TraceDTO();
        
        // When: 设置查询响应字段
        dto.setElementId("trace-001");
        dto.setSource("req-usage-001");
        dto.setTarget("req-def-001");
        dto.setType("satisfy");
        dto.setName("Performance Requirement Satisfaction");
        dto.setDescription("Traces performance requirement to implementation");
        dto.setSourceType("RequirementUsage");
        dto.setTargetType("RequirementDefinition");
        dto.setValid(true);
        dto.setCreatedAt("2025-01-01T10:00:00.000Z");
        dto.setUpdatedAt("2025-01-01T10:00:00.000Z");
        
        // Then: 所有查询字段都应该可获取
        assertNotNull(dto.getElementId());
        assertNotNull(dto.getSource());
        assertNotNull(dto.getTarget());
        assertNotNull(dto.getType());
        assertNotNull(dto.getName());
        assertNotNull(dto.getDescription());
        assertNotNull(dto.getSourceType());
        assertNotNull(dto.getTargetType());
        assertTrue(dto.getValid());
        assertNotNull(dto.getCreatedAt());
        assertNotNull(dto.getUpdatedAt());
    }

    /**
     * 验收标准：REQ-C3-4
     * TraceDTO应该提供语义验证的便捷方法
     */
    @Test
    @DisplayName("REQ-C3-4: 语义验证便捷方法")
    public void testTraceDTO_ShouldProvideSemanticValidationMethods() {
        // Given: 创建TraceDTO
        TraceDTO dto = new TraceDTO();
        dto.setType("satisfy");
        dto.setSourceType("RequirementUsage");
        dto.setTargetType("RequirementDefinition");
        
        // When & Then: 测试语义验证便捷方法
        assertTrue(dto.isSatisfyType(), "应该识别satisfy类型");
        assertFalse(dto.isDeriveType(), "不应该识别为derive类型");
        assertFalse(dto.isRefineType(), "不应该识别为refine类型");
        assertFalse(dto.isTraceType(), "不应该识别为trace类型");
    }

    /**
     * 验收标准：REQ-C3-1
     * TraceDTO应该正确处理null值
     */
    @Test
    @DisplayName("REQ-C3-1: 处理null值和可选字段")
    public void testTraceDTO_ShouldHandleNullValues() {
        // Given: 创建TraceDTO
        TraceDTO dto = new TraceDTO();
        
        // When: 只设置必填字段
        dto.setElementId("trace-001");
        dto.setSource("req-usage-001");
        dto.setTarget("req-def-001");
        dto.setType("satisfy");
        
        // Then: 必填字段应该有值，可选字段为null
        assertNotNull(dto.getElementId());
        assertNotNull(dto.getSource());
        assertNotNull(dto.getTarget());
        assertNotNull(dto.getType());
        
        // 可选字段默认为null
        assertNull(dto.getName());
        assertNull(dto.getDescription());
        assertNull(dto.getRationale());
        assertNull(dto.getSourceType());
        assertNull(dto.getTargetType());
    }

    /**
     * 验收标准：REQ-C3-1
     * TraceDTO应该支持时间戳字段
     */
    @Test
    @DisplayName("REQ-C3-1: 时间戳字段ISO-8601格式")
    public void testTraceDTO_ShouldSupportTimestampFields() {
        // Given: 创建TraceDTO
        TraceDTO dto = new TraceDTO();
        
        // When: 设置ISO-8601时间戳
        String createdAt = "2025-01-01T10:00:00.000Z";
        String updatedAt = "2025-01-02T15:30:45.123Z";
        dto.setCreatedAt(createdAt);
        dto.setUpdatedAt(updatedAt);
        
        // Then: 时间戳应该正确存储
        assertEquals(createdAt, dto.getCreatedAt());
        assertEquals(updatedAt, dto.getUpdatedAt());
    }

    /**
     * 验收标准：REQ-C3-4
     * TraceDTO应该支持不同追溯类型的语义验证
     */
    @Test
    @DisplayName("REQ-C3-4: 不同追溯类型的语义验证")
    public void testTraceDTO_ShouldValidateDifferentTraceTypeSemantics() {
        // Given: 测试不同的追溯类型组合
        
        // Satisfy: source∈{PartUsage,ActionUsage}, target∈{RequirementUsage,RequirementDefinition}
        TraceDTO satisfyTrace = new TraceDTO();
        satisfyTrace.setType("satisfy");
        satisfyTrace.setSourceType("PartUsage");
        satisfyTrace.setTargetType("RequirementDefinition");
        
        // DeriveRequirement: source/target∈{RequirementDefinition,RequirementUsage}
        TraceDTO deriveTrace = new TraceDTO();
        deriveTrace.setType("derive");
        deriveTrace.setSourceType("RequirementDefinition");
        deriveTrace.setTargetType("RequirementUsage");
        
        // When & Then: 验证类型识别
        assertTrue(satisfyTrace.isSatisfyType());
        assertEquals("PartUsage", satisfyTrace.getSourceType());
        assertEquals("RequirementDefinition", satisfyTrace.getTargetType());
        
        assertTrue(deriveTrace.isDeriveType());
        assertEquals("RequirementDefinition", deriveTrace.getSourceType());
        assertEquals("RequirementUsage", deriveTrace.getTargetType());
    }
}