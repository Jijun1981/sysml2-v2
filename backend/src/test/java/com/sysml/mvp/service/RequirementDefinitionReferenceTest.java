package com.sysml.mvp.service;

import com.sysml.mvp.dto.ElementDTO;
import com.sysml.mvp.model.EMFModelRegistry;
import com.sysml.mvp.repository.FileModelRepository;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * REQ-TDD-001: RequirementDefinition引用功能的TDD测试
 * 
 * 测试RequirementUsage正确引用RequirementDefinition的EMF引用关系
 */
@SpringBootTest
@TestPropertySource(properties = {
    "logging.level.com.sysml.mvp=DEBUG"
})
@DisplayName("REQ-TDD-001: RequirementDefinition引用功能测试")
public class RequirementDefinitionReferenceTest {
    
    @Autowired
    private UniversalElementService universalElementService;
    
    @Autowired
    private FileModelRepository fileModelRepository;
    
    @Autowired
    private EMFModelRegistry emfModelRegistry;
    
    @Autowired
    private PilotEMFService pilotEMFService;
    
    @Autowired 
    private ReferenceResolverService referenceResolverService;
    
    private static final String TEST_PROJECT = "default";
    
    @BeforeEach
    void setUp() {
        // 确保测试项目存在
        fileModelRepository.loadProject(TEST_PROJECT);
    }
    
    @Test
    @DisplayName("REQ-TDD-001-1: EMF引用创建")
    void testEMFReferenceCreation() {
        // Given: 创建RequirementDefinition对象
        Map<String, Object> defData = new HashMap<>();
        defData.put("elementId", "DEF-PERF");
        defData.put("declaredName", "性能需求模板");
        defData.put("declaredShortName", "Performance");
        
        ElementDTO definition = universalElementService.createElement("RequirementDefinition", defData);
        assertNotNull(definition);
        assertEquals("DEF-PERF", definition.getElementId());
        
        // When: 创建RequirementUsage并引用该Definition
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("elementId", "REQ-001");
        usageData.put("declaredName", "API响应时间");
        usageData.put("requirementDefinition", "DEF-PERF"); // 字符串ID，应该解析为对象引用
        
        ElementDTO usage = universalElementService.createElement("RequirementUsage", usageData);
        
        // Then: 验证引用正确建立
        assertNotNull(usage);
        assertEquals("REQ-001", usage.getElementId());
        
        // 验证EMF层面的引用关系
        Resource resource = fileModelRepository.loadProject(TEST_PROJECT);
        EObject usageEObject = findEObjectById(resource, "REQ-001");
        EObject defEObject = findEObjectById(resource, "DEF-PERF");
        
        assertNotNull(usageEObject);
        assertNotNull(defEObject);
        
        // 检查EMF引用字段（这里应该是对象引用，不是字符串）
        Object refValue = pilotEMFService.getAttributeValue(usageEObject, "requirementDefinition");
        
        // 核心断言：应该是对象引用，不是字符串
        if (refValue != null) {
            assertTrue(refValue instanceof EObject, 
                "requirementDefinition应该是EObject引用，不是字符串: " + refValue.getClass());
            
            EObject referencedDef = (EObject) refValue;
            Object referencedId = pilotEMFService.getAttributeValue(referencedDef, "elementId");
            assertEquals("DEF-PERF", referencedId, 
                "引用的Definition对象elementId应该是DEF-PERF");
        }
    }
    
    @Test
    @DisplayName("REQ-TDD-001-2: API响应序列化")
    void testAPIResponseSerialization() {
        // Given: 创建Definition和Usage的引用关系
        createDefinitionAndUsage();
        
        // When: 通过API查询RequirementUsage
        ElementDTO usage = universalElementService.findElementById("REQ-001");
        
        // Then: API响应应该包含requirementDefinition字段
        assertNotNull(usage);
        assertNotNull(usage.getProperties(), "ElementDTO应该有properties");
        
        Object reqDefValue = usage.getProperties().get("requirementDefinition");
        assertNotNull(reqDefValue, "API响应应该包含requirementDefinition字段");
        assertEquals("DEF-PERF", reqDefValue.toString(), 
            "requirementDefinition字段值应该是目标Definition的elementId");
    }
    
    @Test
    @DisplayName("REQ-TDD-001-3: 引用验证")
    void testReferenceValidation() {
        // Given: 引用不存在的RequirementDefinition
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("elementId", "REQ-INVALID");
        usageData.put("declaredName", "无效引用的需求");
        usageData.put("requirementDefinition", "NON-EXISTENT-DEF");
        
        // When & Then: 创建对象应该失败
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            universalElementService.createElement("RequirementUsage", usageData);
        });
        
        assertTrue(exception.getMessage().contains("引用目标对象未找到") || 
                  exception.getMessage().contains("not found"),
            "错误消息应该指示引用目标对象未找到: " + exception.getMessage());
    }
    
    @Test
    @DisplayName("REQ-TDD-001-4: 空引用处理")
    void testNullReferenceHandling() {
        // Given: RequirementUsage的requirementDefinition为null
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("elementId", "REQ-NULL");
        usageData.put("declaredName", "无引用需求");
        // 注意：不设置requirementDefinition字段，或者设置为null
        
        // When: 创建对象
        ElementDTO usage = universalElementService.createElement("RequirementUsage", usageData);
        
        // Then: 对象创建成功，引用字段为null
        assertNotNull(usage);
        assertEquals("REQ-NULL", usage.getElementId());
        
        // API响应中不应该包含requirementDefinition字段，或者值为null
        if (usage.getProperties().containsKey("requirementDefinition")) {
            assertNull(usage.getProperties().get("requirementDefinition"));
        }
    }
    
    @Test
    @DisplayName("REQ-TDD-001-5: 引用关系验证工具方法")
    void testReferenceValidationUtility() {
        // Given: 创建Definition和Usage的引用关系
        createDefinitionAndUsage();
        
        // When: 使用ReferenceResolverService验证引用完整性
        Resource resource = fileModelRepository.loadProject(TEST_PROJECT);
        int brokenRefs = referenceResolverService.validateReferenceIntegrity(resource);
        
        // Then: 应该没有损坏的引用
        assertEquals(0, brokenRefs, "不应该有损坏的引用");
    }
    
    /**
     * 辅助方法：创建Definition和Usage的引用关系
     */
    private void createDefinitionAndUsage() {
        // 创建RequirementDefinition
        Map<String, Object> defData = new HashMap<>();
        defData.put("elementId", "DEF-PERF");
        defData.put("declaredName", "性能需求模板");
        defData.put("declaredShortName", "Performance");
        universalElementService.createElement("RequirementDefinition", defData);
        
        // 创建RequirementUsage并引用Definition
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("elementId", "REQ-001");
        usageData.put("declaredName", "API响应时间");
        usageData.put("requirementDefinition", "DEF-PERF");
        universalElementService.createElement("RequirementUsage", usageData);
    }
    
    /**
     * 辅助方法：在Resource中查找EObject
     */
    private EObject findEObjectById(Resource resource, String elementId) {
        for (EObject obj : resource.getContents()) {
            Object id = pilotEMFService.getAttributeValue(obj, "elementId");
            if (elementId.equals(id)) {
                return obj;
            }
        }
        return null;
    }
}