package com.sysml.mvp.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RequirementDTO 测试用例
 * 
 * 需求对齐：
 * - REQ-B2-4: DTO选择性映射 - 需求特定字段，只包含需要的字段
 * - REQ-C1-1: 创建需求定义 - 包含reqId、declaredName等核心字段
 * - REQ-C1-2: 查询需求定义 - 返回RequirementDTO格式
 * - REQ-C1-4: 参数化文本渲染 - 支持renderedText字段
 */
@DisplayName("RequirementDTO测试 - REQ-B2-4")
public class RequirementDTOTest {

    /**
     * 验收标准：REQ-B2-4
     * RequirementDTO应该包含需求特定的核心字段
     */
    @Test
    @DisplayName("REQ-B2-4: RequirementDTO包含核心需求字段")
    public void testRequirementDTO_ShouldHaveCoreRequirementFields() {
        // Given: 创建RequirementDTO
        RequirementDTO dto = new RequirementDTO();
        
        // When: 设置核心字段
        dto.setElementId("req-def-001");
        dto.setReqId("EBS-L2-BMS-001");
        dto.setDeclaredName("电池温度监控需求");
        dto.setDeclaredShortName("REQ-001");
        dto.setDocumentation("系统应实时监控电池温度");
        
        // Then: 能够获取所有核心字段
        assertEquals("req-def-001", dto.getElementId());
        assertEquals("EBS-L2-BMS-001", dto.getReqId());
        assertEquals("电池温度监控需求", dto.getDeclaredName());
        assertEquals("REQ-001", dto.getDeclaredShortName());
        assertEquals("系统应实时监控电池温度", dto.getDocumentation());
    }

    /**
     * 验收标准：REQ-C1-1
     * RequirementDTO应该支持reqId作为唯一业务标识
     */
    @Test
    @DisplayName("REQ-C1-1: reqId作为唯一业务标识")
    public void testRequirementDTO_ShouldSupportReqIdAsBusinessKey() {
        // Given: 创建RequirementDTO
        RequirementDTO dto = new RequirementDTO();
        
        // When: 设置reqId
        String reqId = "EBS-L1-001";
        dto.setReqId(reqId);
        
        // Then: reqId应该正确存储和获取
        assertEquals(reqId, dto.getReqId());
        assertNotNull(dto.getReqId());
    }

    /**
     * 验收标准：REQ-C1-2
     * RequirementDTO应该包含查询需求时返回的所有字段
     */
    @Test
    @DisplayName("REQ-C1-2: 查询需求返回完整信息")
    public void testRequirementDTO_ShouldContainQueryResponseFields() {
        // Given: 创建完整的RequirementDTO
        RequirementDTO dto = new RequirementDTO();
        
        // When: 设置查询响应字段
        dto.setElementId("req-def-001");
        dto.setReqId("EBS-L2-BMS-001");
        dto.setDeclaredName("电池温度监控需求");
        dto.setDeclaredShortName("REQ-001");
        dto.setDocumentation("电池管理系统应实时监控电池温度，温度范围-40°C至+85°C");
        dto.setStatus("active");
        dto.setPriority("P0");
        dto.setVerificationMethod("test");
        dto.setCreatedAt("2025-01-01T10:00:00.000Z");
        dto.setUpdatedAt("2025-01-01T10:00:00.000Z");
        
        // Then: 所有字段都应该可获取
        assertNotNull(dto.getElementId());
        assertNotNull(dto.getReqId());
        assertNotNull(dto.getDeclaredName());
        assertNotNull(dto.getDocumentation());
        assertNotNull(dto.getStatus());
        assertNotNull(dto.getPriority());
        assertNotNull(dto.getVerificationMethod());
        assertNotNull(dto.getCreatedAt());
        assertNotNull(dto.getUpdatedAt());
    }

    /**
     * 验收标准：REQ-C1-4
     * RequirementDTO应该支持参数化文本渲染
     */
    @Test
    @DisplayName("REQ-C1-4: 支持参数化文本渲染")
    public void testRequirementDTO_ShouldSupportParametricText() {
        // Given: 创建RequirementDTO
        RequirementDTO dto = new RequirementDTO();
        
        // When: 设置模板文本和渲染文本
        dto.setDocumentation("The ${subject} shall achieve ${performance} within ${window}.");
        dto.setRenderedText("The Engine shall achieve 100kW within 10min.");
        
        // Then: 两种文本都应该可获取
        assertEquals("The ${subject} shall achieve ${performance} within ${window}.", dto.getDocumentation());
        assertEquals("The Engine shall achieve 100kW within 10min.", dto.getRenderedText());
    }

    /**
     * 验收标准：REQ-B2-4
     * RequirementDTO应该支持领域特定的业务属性
     */
    @Test
    @DisplayName("REQ-B2-4: 支持需求领域特定属性")
    public void testRequirementDTO_ShouldSupportDomainSpecificFields() {
        // Given: 创建RequirementDTO
        RequirementDTO dto = new RequirementDTO();
        
        // When: 设置需求领域特定属性
        dto.setPriority("P0");
        dto.setVerificationMethod("test");
        dto.setStatus("active");
        dto.setCategory("functional");
        dto.setSource("stakeholder");
        dto.setRiskLevel("high");
        
        // Then: 领域属性都应该可获取
        assertEquals("P0", dto.getPriority());
        assertEquals("test", dto.getVerificationMethod());
        assertEquals("active", dto.getStatus());
        assertEquals("functional", dto.getCategory());
        assertEquals("stakeholder", dto.getSource());
        assertEquals("high", dto.getRiskLevel());
    }

    /**
     * 验收标准：REQ-B2-4
     * RequirementDTO应该正确处理null值
     */
    @Test
    @DisplayName("REQ-B2-4: 处理null值和可选字段")
    public void testRequirementDTO_ShouldHandleNullValues() {
        // Given: 创建RequirementDTO
        RequirementDTO dto = new RequirementDTO();
        
        // When: 只设置必填字段
        dto.setElementId("req-def-001");
        dto.setReqId("EBS-L1-001");
        dto.setDeclaredName("基础需求");
        
        // Then: 可选字段应该为null，必填字段应该有值
        assertNotNull(dto.getElementId());
        assertNotNull(dto.getReqId());
        assertNotNull(dto.getDeclaredName());
        
        // 可选字段默认为null
        assertNull(dto.getDocumentation());
        assertNull(dto.getPriority());
        assertNull(dto.getVerificationMethod());
        assertNull(dto.getRenderedText());
    }

    /**
     * 验收标准：REQ-C1-1
     * RequirementDTO应该支持reqId的层次编码格式
     */
    @Test
    @DisplayName("REQ-C1-1: reqId层次编码格式")
    public void testRequirementDTO_ShouldSupportHierarchicalReqId() {
        // Given: 测试不同层次的reqId格式
        String[] reqIds = {
            "EBS-L1-001",           // L1系统级
            "EBS-L2-BMS-001",       // L2子系统级
            "EBS-L3-SENSOR-001"     // L3组件级
        };
        
        // When & Then: 每种格式都应该被正确存储
        for (String reqId : reqIds) {
            RequirementDTO dto = new RequirementDTO();
            dto.setReqId(reqId);
            assertEquals(reqId, dto.getReqId(), "应该支持reqId格式: " + reqId);
        }
    }

    /**
     * 验收标准：REQ-B2-4
     * RequirementDTO应该支持时间戳字段（ISO-8601 UTC格式）
     */
    @Test
    @DisplayName("REQ-B2-4: 时间戳字段ISO-8601格式")
    public void testRequirementDTO_ShouldSupportTimestampFields() {
        // Given: 创建RequirementDTO
        RequirementDTO dto = new RequirementDTO();
        
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
     * 验收标准：REQ-B2-4
     * RequirementDTO应该与ElementDTO配合使用
     */
    @Test
    @DisplayName("REQ-B2-4: 与ElementDTO的关系")
    public void testRequirementDTO_ShouldWorkWithElementDTO() {
        // Given: RequirementDTO专门用于需求特定字段
        RequirementDTO reqDto = new RequirementDTO();
        reqDto.setElementId("req-def-001");
        reqDto.setReqId("EBS-L1-001");
        reqDto.setDeclaredName("基础需求");
        
        // When: 验证与ElementDTO的区别
        // RequirementDTO专注于需求特定字段，不包含通用的properties Map
        
        // Then: RequirementDTO应该有明确的需求字段
        assertNotNull(reqDto.getReqId()); // RequirementDTO特有
        assertNotNull(reqDto.getElementId()); // 与ElementDTO共同字段
        assertNotNull(reqDto.getDeclaredName()); // 需求特定字段
    }
}