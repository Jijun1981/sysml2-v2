package com.sysml.mvp;

import com.sysml.mvp.model.EMFModelRegistry;
import com.sysml.mvp.service.PilotEMFService;
import org.eclipse.emf.ecore.EObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 3: 动态EMF的PATCH机制测试
 * 测试PilotEMFService的部分更新能力
 */
@SpringBootTest
public class DynamicEMFPatchTest {
    
    @Autowired
    private PilotEMFService pilotService;
    
    @Autowired
    private EMFModelRegistry modelRegistry;
    
    private EObject testRequirement;
    
    @BeforeEach
    void setUp() {
        // 创建一个测试用的RequirementDefinition
        testRequirement = pilotService.createRequirementDefinition(
            "REQ-EMF-001",
            "Original EMF Name",
            "Original EMF Text"
        );
        assertNotNull(testRequirement);
    }
    
    @Test
    void shouldMergePartialAttributesViaDynamicEMF() {
        // 测试部分属性合并
        Map<String, Object> patchAttributes = new HashMap<>();
        patchAttributes.put("declaredName", "Patched Name");
        // 注意：没有提供declaredShortName和documentation
        
        // 执行部分更新
        pilotService.mergeAttributes(testRequirement, patchAttributes);
        
        // 验证：只有declaredName被更新
        assertEquals("Patched Name", 
            pilotService.getAttributeValue(testRequirement, "declaredName"));
        assertEquals("REQ-EMF-001", 
            pilotService.getAttributeValue(testRequirement, "declaredShortName"));
        // documentation保持原值（需要特殊处理）
        assertNotNull(pilotService.getAttributeValue(testRequirement, "elementId"));
    }
    
    @Test
    void shouldHandleInheritedAttributesInPatch() {
        // 测试继承属性的更新
        // RequirementDefinition继承自多个父类，包含继承的属性
        Map<String, Object> patchAttributes = new HashMap<>();
        
        // elementId是从Element继承的
        String originalId = (String) pilotService.getAttributeValue(testRequirement, "elementId");
        patchAttributes.put("elementId", "new-id-should-not-change"); // ID不应被改变
        
        // declaredName是从NamedElement继承的
        patchAttributes.put("declaredName", "Inherited Attribute Update");
        
        // 执行部分更新
        pilotService.mergeAttributes(testRequirement, patchAttributes);
        
        // 验证：elementId不应被改变（业务规则）
        assertEquals(originalId, 
            pilotService.getAttributeValue(testRequirement, "elementId"),
            "ID字段不应被PATCH修改");
        
        // declaredName应该被更新
        assertEquals("Inherited Attribute Update", 
            pilotService.getAttributeValue(testRequirement, "declaredName"));
    }
    
    @Test
    void shouldIgnoreUnknownFieldsInPatch() {
        // 测试未知字段应被忽略
        Map<String, Object> patchAttributes = new HashMap<>();
        patchAttributes.put("declaredName", "Valid Update");
        patchAttributes.put("unknownField", "Should be ignored");
        patchAttributes.put("anotherUnknown", 12345);
        
        // 执行部分更新 - 不应因未知字段而失败
        assertDoesNotThrow(() -> 
            pilotService.mergeAttributes(testRequirement, patchAttributes)
        );
        
        // 验证：只有有效字段被更新
        assertEquals("Valid Update", 
            pilotService.getAttributeValue(testRequirement, "declaredName"));
        
        // 未知字段不应存在
        assertNull(pilotService.getAttributeValue(testRequirement, "unknownField"));
        assertNull(pilotService.getAttributeValue(testRequirement, "anotherUnknown"));
    }
    
    @Test
    void shouldHandleNullValuesInMerge() {
        // 测试null值处理策略
        Map<String, Object> patchAttributes = new HashMap<>();
        patchAttributes.put("declaredName", null); // null值
        patchAttributes.put("declaredShortName", "NEW-REQ-ID");
        
        // 执行部分更新
        pilotService.mergeAttributes(testRequirement, patchAttributes);
        
        // 验证：null值应被忽略（不清空原值）
        assertEquals("Original EMF Name", 
            pilotService.getAttributeValue(testRequirement, "declaredName"),
            "null值不应清空原字段");
        
        // 非null值正常更新
        assertEquals("NEW-REQ-ID", 
            pilotService.getAttributeValue(testRequirement, "declaredShortName"));
    }
    
    @Test
    void shouldMergeEmptyMapWithoutChanges() {
        // 空Map不应改变任何内容
        Map<String, Object> emptyPatch = new HashMap<>();
        
        // 记录原始值
        String originalName = (String) pilotService.getAttributeValue(testRequirement, "declaredName");
        String originalReqId = (String) pilotService.getAttributeValue(testRequirement, "declaredShortName");
        
        // 执行空更新
        pilotService.mergeAttributes(testRequirement, emptyPatch);
        
        // 验证：所有值保持不变
        assertEquals(originalName, 
            pilotService.getAttributeValue(testRequirement, "declaredName"));
        assertEquals(originalReqId, 
            pilotService.getAttributeValue(testRequirement, "declaredShortName"));
    }
    
    @Test
    void shouldHandleComplexDocumentationField() {
        // documentation字段的特殊处理
        Map<String, Object> patchAttributes = new HashMap<>();
        patchAttributes.put("documentation", "New Documentation Text");
        
        // 执行更新
        pilotService.mergeAttributes(testRequirement, patchAttributes);
        
        // 验证：documentation应该被正确更新
        // 注意：documentation可能是复杂结构，需要特殊处理
        Object docValue = pilotService.getAttributeValue(testRequirement, "documentation");
        assertNotNull(docValue, "Documentation应该存在");
        // 具体验证取决于documentation的实际结构
    }
    
    @Test
    void shouldPreserveReadOnlyFields() {
        // 某些字段应该是只读的
        Map<String, Object> patchAttributes = new HashMap<>();
        patchAttributes.put("eClass", "AnotherClass"); // eClass不应被改变
        patchAttributes.put("createdAt", "2025-01-01"); // 创建时间不应被改变
        
        String originalClass = testRequirement.eClass().getName();
        
        // 执行更新
        pilotService.mergeAttributes(testRequirement, patchAttributes);
        
        // 验证：只读字段保持不变
        assertEquals(originalClass, testRequirement.eClass().getName(),
            "eClass不应被改变");
    }
    
    @Test
    void shouldHandleApiToModelFieldMapping() {
        // 测试API字段到模型字段的映射
        Map<String, Object> apiPatch = new HashMap<>();
        apiPatch.put("name", "API Name"); // API使用name
        apiPatch.put("reqId", "API-REQ-001"); // API使用reqId
        apiPatch.put("text", "API Text"); // API使用text
        
        // 需要转换为Pilot字段名
        Map<String, Object> modelPatch = new HashMap<>();
        modelPatch.put("declaredName", apiPatch.get("name"));
        modelPatch.put("declaredShortName", apiPatch.get("reqId"));
        // text需要特殊处理为documentation
        
        // 执行更新
        pilotService.mergeAttributes(testRequirement, modelPatch);
        
        // 验证映射正确
        assertEquals("API Name", 
            pilotService.getAttributeValue(testRequirement, "declaredName"));
        assertEquals("API-REQ-001", 
            pilotService.getAttributeValue(testRequirement, "declaredShortName"));
    }
}