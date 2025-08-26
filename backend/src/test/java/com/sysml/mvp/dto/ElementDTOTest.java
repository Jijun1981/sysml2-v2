package com.sysml.mvp.dto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ElementDTO 测试用例
 * 
 * 需求对齐：
 * - REQ-D0-1: 通用元素数据API - ElementDTO包含eClass字段标识类型
 * - REQ-B2-4: DTO选择性映射 - 支持动态属性
 */
@DisplayName("ElementDTO测试 - REQ-D0-1")
public class ElementDTOTest {

    /**
     * 验收标准：REQ-D0-1
     * 每个ElementDTO包含eClass字段标识类型
     */
    @Test
    @DisplayName("REQ-D0-1: ElementDTO必须包含eClass字段")
    public void testElementDTO_ShouldHaveEClassField() {
        // Given: 创建一个ElementDTO
        ElementDTO dto = new ElementDTO();
        
        // When: 设置eClass字段
        dto.setEClass("RequirementDefinition");
        
        // Then: 能够获取eClass字段
        assertEquals("RequirementDefinition", dto.getEClass());
    }

    /**
     * 验收标准：REQ-D0-1
     * ElementDTO应该能标识任意SysML类型
     */
    @Test
    @DisplayName("REQ-D0-1: 支持任意SysML类型标识")
    public void testElementDTO_ShouldSupportAnySysMLType() {
        // Given: 测试多种SysML类型
        String[] sysmlTypes = {
            "RequirementDefinition",
            "RequirementUsage",
            "PartDefinition",
            "PartUsage",
            "InterfaceDefinition",
            "Connection",
            "Dependency"
        };
        
        // When & Then: 每种类型都应该能被正确设置和获取
        for (String type : sysmlTypes) {
            ElementDTO dto = new ElementDTO();
            dto.setEClass(type);
            assertEquals(type, dto.getEClass(), 
                "应该支持" + type + "类型");
        }
    }

    /**
     * 验收标准：REQ-D0-1
     * ElementDTO应该包含elementId字段作为唯一标识
     */
    @Test
    @DisplayName("REQ-D0-1: ElementDTO必须包含elementId字段")
    public void testElementDTO_ShouldHaveElementId() {
        // Given: 创建一个ElementDTO
        ElementDTO dto = new ElementDTO();
        
        // When: 设置elementId
        String testId = "req-def-12345";
        dto.setElementId(testId);
        
        // Then: 能够获取elementId
        assertEquals(testId, dto.getElementId());
    }

    /**
     * 验收标准：REQ-B2-4
     * DTO支持动态属性映射（选择性映射）
     */
    @Test
    @DisplayName("REQ-B2-4: 支持动态属性Map")
    public void testElementDTO_ShouldSupportDynamicProperties() {
        // Given: 创建一个ElementDTO
        ElementDTO dto = new ElementDTO();
        
        // When: 设置动态属性
        dto.setProperty("declaredName", "性能需求");
        dto.setProperty("declaredShortName", "REQ-001");
        dto.setProperty("documentation", "系统应在3秒内响应");
        dto.setProperty("priority", "P0");
        
        // Then: 能够获取所有属性
        assertEquals("性能需求", dto.getProperty("declaredName"));
        assertEquals("REQ-001", dto.getProperty("declaredShortName"));
        assertEquals("系统应在3秒内响应", dto.getProperty("documentation"));
        assertEquals("P0", dto.getProperty("priority"));
    }

    /**
     * 验收标准：REQ-B2-4
     * 获取所有动态属性
     */
    @Test
    @DisplayName("REQ-B2-4: 获取所有动态属性Map")
    public void testElementDTO_ShouldReturnAllProperties() {
        // Given: 创建一个ElementDTO并设置多个属性
        ElementDTO dto = new ElementDTO();
        dto.setProperty("name", "测试需求");
        dto.setProperty("status", "active");
        dto.setProperty("version", "1.0");
        
        // When: 获取所有属性
        Map<String, Object> properties = dto.getProperties();
        
        // Then: 应该包含所有设置的属性
        assertNotNull(properties);
        assertEquals(3, properties.size());
        assertEquals("测试需求", properties.get("name"));
        assertEquals("active", properties.get("status"));
        assertEquals("1.0", properties.get("version"));
    }

    /**
     * 验收标准：REQ-B2-4
     * 支持不同数据类型的属性
     */
    @Test
    @DisplayName("REQ-B2-4: 支持不同数据类型的动态属性")
    public void testElementDTO_ShouldSupportVariousDataTypes() {
        // Given: 创建一个ElementDTO
        ElementDTO dto = new ElementDTO();
        
        // When: 设置不同类型的属性
        dto.setProperty("name", "测试");               // String
        dto.setProperty("count", 42);                  // Integer
        dto.setProperty("ratio", 3.14);                // Double
        dto.setProperty("isActive", true);             // Boolean
        dto.setProperty("tags", new String[]{"tag1", "tag2"}); // Array
        
        // Then: 能够正确获取各种类型
        assertEquals("测试", dto.getProperty("name"));
        assertEquals(42, dto.getProperty("count"));
        assertEquals(3.14, dto.getProperty("ratio"));
        assertEquals(true, dto.getProperty("isActive"));
        assertNotNull(dto.getProperty("tags"));
    }

    /**
     * 验收标准：通用约定
     * 未识别字段应保留（round-trip不丢失）
     */
    @Test
    @DisplayName("通用约定: 未识别字段应保留")
    public void testElementDTO_ShouldPreserveUnknownFields() {
        // Given: 创建一个ElementDTO
        ElementDTO dto = new ElementDTO();
        
        // When: 设置一些"未知"字段（模拟从JSON反序列化）
        dto.setProperty("unknownField1", "value1");
        dto.setProperty("customExtension", "customValue");
        dto.setProperty("_internal", "internalData");
        
        // Then: 这些字段应该被保留
        assertEquals("value1", dto.getProperty("unknownField1"));
        assertEquals("customValue", dto.getProperty("customExtension"));
        assertEquals("internalData", dto.getProperty("_internal"));
    }

    /**
     * 验收标准：REQ-D0-1
     * 测试完整的RequirementDefinition DTO
     */
    @Test
    @DisplayName("REQ-D0-1: 完整的RequirementDefinition示例")
    public void testElementDTO_RequirementDefinitionExample() {
        // Given: 创建一个RequirementDefinition的ElementDTO
        ElementDTO dto = new ElementDTO();
        
        // When: 设置RequirementDefinition的所有字段
        dto.setEClass("RequirementDefinition");
        dto.setElementId("req-def-001");
        dto.setProperty("declaredName", "电池温度监控需求");
        dto.setProperty("declaredShortName", "EBS-L2-BMS-001");
        dto.setProperty("documentation", "电池管理系统应实时监控电池温度");
        dto.setProperty("priority", "P0");
        dto.setProperty("verificationMethod", "test");
        dto.setProperty("createdAt", "2025-01-01T10:00:00.000Z");
        dto.setProperty("updatedAt", "2025-01-01T10:00:00.000Z");
        
        // Then: 验证所有字段
        assertEquals("RequirementDefinition", dto.getEClass());
        assertEquals("req-def-001", dto.getElementId());
        assertEquals("电池温度监控需求", dto.getProperty("declaredName"));
        assertEquals("EBS-L2-BMS-001", dto.getProperty("declaredShortName"));
        assertTrue(dto.getProperties().containsKey("documentation"));
        assertTrue(dto.getProperties().containsKey("priority"));
        assertTrue(dto.getProperties().containsKey("verificationMethod"));
    }

    /**
     * 验收标准：REQ-D0-2
     * 测试Dependency（追溯关系）DTO
     */
    @Test
    @DisplayName("REQ-D0-2: Dependency类型的ElementDTO")
    public void testElementDTO_DependencyExample() {
        // Given: 创建一个Dependency的ElementDTO
        ElementDTO dto = new ElementDTO();
        
        // When: 设置Dependency的字段
        dto.setEClass("Dependency");
        dto.setElementId("trace-001");
        dto.setProperty("sourceId", "req-usage-001");
        dto.setProperty("targetId", "req-def-001");
        dto.setProperty("type", "satisfy");
        
        // Then: 验证Dependency字段
        assertEquals("Dependency", dto.getEClass());
        assertEquals("trace-001", dto.getElementId());
        assertEquals("req-usage-001", dto.getProperty("sourceId"));
        assertEquals("req-def-001", dto.getProperty("targetId"));
        assertEquals("satisfy", dto.getProperty("type"));
    }
}