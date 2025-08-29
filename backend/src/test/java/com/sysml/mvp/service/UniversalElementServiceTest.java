package com.sysml.mvp.service;

import com.sysml.mvp.dto.ElementDTO;
import com.sysml.mvp.model.EMFModelRegistry;
import com.sysml.mvp.repository.FileModelRepository;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UniversalElementService 测试用例
 * 
 * 需求对齐：
 * - REQ-B5-3: 内部EMF工具层 - 提供通用元素操作能力
 * - REQ-D0-1: 通用元素数据API - 支持任意SysML类型
 * - REQ-B2-4: DTO选择性映射 - 支持PATCH更新
 * - REQ-B2-1: 创建API - 支持动态创建任意类型
 */
@DisplayName("UniversalElementService测试 - REQ-B5-3")
public class UniversalElementServiceTest {

    private UniversalElementService universalElementService;
    
    @Mock
    private PilotEMFService pilotEMFService;
    
    @Mock
    private FileModelRepository fileModelRepository;
    
    @Mock
    private EMFModelRegistry emfModelRegistry;
    
    @Mock
    private ReferenceResolverService referenceResolverService;
    
    @Mock
    private Resource mockResource;
    
    @Mock
    private EObject mockEObject;
    
    @Mock
    private EClass mockEClass;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        universalElementService = new UniversalElementService(
            pilotEMFService, 
            fileModelRepository, 
            emfModelRegistry,
            referenceResolverService
        );
    }

    /**
     * 验收标准：REQ-B5-3
     * UniversalElementService应该能创建任意SysML元素类型
     */
    @Test
    @DisplayName("REQ-B5-3: 创建任意SysML元素类型")
    public void testCreateElement_ShouldCreateAnySysMLType() {
        // Given: 准备创建RequirementDefinition
        String eClassName = "RequirementDefinition";
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("declaredName", "性能需求");
        attributes.put("elementId", "req-def-001");
        
        // 不需要mock getEClass，因为createElement直接处理
        // when(emfModelRegistry.getEClass(eClassName)).thenReturn(mockEClass);
        when(pilotEMFService.createElement(any(String.class), any(Map.class))).thenReturn(mockEObject);
        when(fileModelRepository.loadProject("default")).thenReturn(mockResource);
        EList<EObject> contents = new BasicEList<>();
        when(mockResource.getContents()).thenReturn(contents);
        when(mockEObject.eClass()).thenReturn(mockEClass);
        when(mockEClass.getName()).thenReturn(eClassName);
        EList<EStructuralFeature> features = new BasicEList<>();
        when(mockEClass.getEAllStructuralFeatures()).thenReturn(features);
        when(pilotEMFService.getAttributeValue(mockEObject, "elementId")).thenReturn("req-def-001");
        
        // When: 创建元素
        ElementDTO result = universalElementService.createElement(eClassName, attributes);
        
        // Then: 验证创建成功
        assertNotNull(result);
        assertEquals(eClassName, result.getEClass());
        assertEquals("req-def-001", result.getElementId());
        // 验证PilotEMFService的createElement被正确调用
        verify(pilotEMFService).createElement(eq(eClassName), eq(attributes));
        // 验证保存操作
        verify(fileModelRepository).saveProject(eq("default"), eq(mockResource));
    }

    /**
     * 验收标准：REQ-B5-3
     * 支持多种SysML类型的创建
     */
    @Test
    @DisplayName("REQ-B5-3: 支持创建多种SysML类型")
    public void testCreateElement_ShouldSupportVariousSysMLTypes() {
        // Given: 测试多种类型
        String[] sysmlTypes = {
            "RequirementUsage",
            "PartDefinition", 
            "InterfaceDefinition",
            "Connection",
            "Dependency"
        };
        
        for (String type : sysmlTypes) {
            // Given
            Map<String, Object> attrs = new HashMap<>();
            attrs.put("elementId", type.toLowerCase() + "-001");
            
            // 不需要mock getEClass，因为createElement直接处理
        // when(emfModelRegistry.getEClass(type)).thenReturn(mockEClass);
            when(pilotEMFService.createElement(any(String.class), any(Map.class))).thenReturn(mockEObject);
            when(fileModelRepository.loadProject("default")).thenReturn(mockResource);
            EList<EObject> emptyContents = new BasicEList<>();
            when(mockResource.getContents()).thenReturn(emptyContents);
            when(mockEObject.eClass()).thenReturn(mockEClass);
            when(mockEClass.getName()).thenReturn(type);
            EList<EStructuralFeature> features = new BasicEList<>();
            when(mockEClass.getEAllStructuralFeatures()).thenReturn(features);
            
            // When
            ElementDTO result = universalElementService.createElement(type, attrs);
            
            // Then
            assertNotNull(result, "应该支持创建" + type);
            assertEquals(type, result.getEClass());
        }
    }

    /**
     * 验收标准：REQ-D0-1
     * 查询所有元素
     */
    @Test
    @DisplayName("REQ-D0-1: 查询所有元素")
    public void testQueryElements_ShouldReturnAllElements() {
        // Given: 准备模拟数据
        EObject obj1 = mock(EObject.class);
        EObject obj2 = mock(EObject.class);
        EClass eClass1 = mock(EClass.class);
        EClass eClass2 = mock(EClass.class);
        
        when(eClass1.getName()).thenReturn("RequirementDefinition");
        when(eClass2.getName()).thenReturn("RequirementUsage");
        when(obj1.eClass()).thenReturn(eClass1);
        when(obj2.eClass()).thenReturn(eClass2);
        EList<EStructuralFeature> features1 = new BasicEList<>();
        EList<EStructuralFeature> features2 = new BasicEList<>();
        when(eClass1.getEAllStructuralFeatures()).thenReturn(features1);
        when(eClass2.getEAllStructuralFeatures()).thenReturn(features2);
        
        when(fileModelRepository.loadProject("default")).thenReturn(mockResource);
        EList<EObject> contents = new BasicEList<>();
        contents.add(obj1);
        contents.add(obj2);
        when(mockResource.getContents()).thenReturn(contents);
        when(pilotEMFService.getAttributeValue(obj1, "elementId")).thenReturn("req-def-001");
        when(pilotEMFService.getAttributeValue(obj2, "elementId")).thenReturn("req-usage-001");
        
        // When: 查询所有元素
        List<ElementDTO> results = universalElementService.queryElements(null);
        
        // Then: 验证返回所有元素
        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("RequirementDefinition", results.get(0).getEClass());
        assertEquals("RequirementUsage", results.get(1).getEClass());
    }

    /**
     * 验收标准：REQ-D0-2
     * 按类型查询元素
     */
    @Test
    @DisplayName("REQ-D0-2: 按类型查询元素")
    public void testQueryElements_ShouldFilterByType() {
        // Given: 准备混合类型数据
        EObject reqDef = mock(EObject.class);
        EObject reqUsage = mock(EObject.class);
        EObject partDef = mock(EObject.class);
        EClass reqDefClass = mock(EClass.class);
        EClass reqUsageClass = mock(EClass.class);
        EClass partDefClass = mock(EClass.class);
        
        when(reqDefClass.getName()).thenReturn("RequirementDefinition");
        when(reqUsageClass.getName()).thenReturn("RequirementUsage");
        when(partDefClass.getName()).thenReturn("PartDefinition");
        when(reqDef.eClass()).thenReturn(reqDefClass);
        when(reqUsage.eClass()).thenReturn(reqUsageClass);
        when(partDef.eClass()).thenReturn(partDefClass);
        EList<EStructuralFeature> reqDefFeatures = new BasicEList<>();
        EList<EStructuralFeature> reqUsageFeatures = new BasicEList<>();
        EList<EStructuralFeature> partDefFeatures = new BasicEList<>();
        when(reqDefClass.getEAllStructuralFeatures()).thenReturn(reqDefFeatures);
        when(reqUsageClass.getEAllStructuralFeatures()).thenReturn(reqUsageFeatures);
        when(partDefClass.getEAllStructuralFeatures()).thenReturn(partDefFeatures);
        
        when(fileModelRepository.loadProject("default")).thenReturn(mockResource);
        EList<EObject> contents = new BasicEList<>();
        contents.add(reqDef);
        contents.add(reqUsage);
        contents.add(partDef);
        when(mockResource.getContents()).thenReturn(contents);
        when(pilotEMFService.getAttributeValue(reqDef, "elementId")).thenReturn("req-def-001");
        when(pilotEMFService.getAttributeValue(reqUsage, "elementId")).thenReturn("req-usage-001");
        when(pilotEMFService.getAttributeValue(partDef, "elementId")).thenReturn("part-def-001");
        
        // When: 查询特定类型
        List<ElementDTO> results = universalElementService.queryElements("RequirementDefinition");
        
        // Then: 只返回匹配类型
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("RequirementDefinition", results.get(0).getEClass());
        assertEquals("req-def-001", results.get(0).getElementId());
    }

    /**
     * 验收标准：REQ-B2-4
     * PATCH更新部分属性
     */
    @Test
    @DisplayName("REQ-B2-4: PATCH更新元素属性")
    public void testPatchElement_ShouldUpdatePartialAttributes() {
        // Given: 准备现有元素
        String elementId = "req-def-001";
        Map<String, Object> updates = new HashMap<>();
        updates.put("declaredName", "更新后的需求");
        updates.put("priority", "P0");
        
        when(fileModelRepository.loadProject("default")).thenReturn(mockResource);
        EList<EObject> contents = new BasicEList<>();
        contents.add(mockEObject);
        when(mockResource.getContents()).thenReturn(contents);
        when(pilotEMFService.getAttributeValue(mockEObject, "elementId")).thenReturn(elementId);
        when(mockEObject.eClass()).thenReturn(mockEClass);
        when(mockEClass.getName()).thenReturn("RequirementDefinition");
        EList<EStructuralFeature> features = new BasicEList<>();
        when(mockEClass.getEAllStructuralFeatures()).thenReturn(features);
        
        // When: 执行PATCH更新
        ElementDTO result = universalElementService.patchElement(elementId, updates);
        
        // Then: 验证更新调用
        verify(pilotEMFService).setAttributeIfExists(eq(mockEObject), eq("declaredName"), eq("更新后的需求"));
        verify(pilotEMFService).setAttributeIfExists(eq(mockEObject), eq("priority"), eq("P0"));
        verify(fileModelRepository).saveProject(eq("default"), eq(mockResource));
        assertNotNull(result);
        assertEquals("RequirementDefinition", result.getEClass());
    }

    /**
     * 验收标准：REQ-B2-4
     * PATCH不应该影响未指定的属性
     */
    @Test
    @DisplayName("REQ-B2-4: PATCH只更新指定属性")
    public void testPatchElement_ShouldNotAffectOtherAttributes() {
        // Given: 元素有多个属性，但只更新一个
        String elementId = "req-def-001";
        Map<String, Object> updates = new HashMap<>();
        updates.put("priority", "P1"); // 只更新priority
        
        when(fileModelRepository.loadProject("default")).thenReturn(mockResource);
        EList<EObject> contents = new BasicEList<>();
        contents.add(mockEObject);
        when(mockResource.getContents()).thenReturn(contents);
        when(pilotEMFService.getAttributeValue(mockEObject, "elementId")).thenReturn(elementId);
        when(mockEObject.eClass()).thenReturn(mockEClass);
        when(mockEClass.getName()).thenReturn("RequirementDefinition");
        EList<EStructuralFeature> features = new BasicEList<>();
        when(mockEClass.getEAllStructuralFeatures()).thenReturn(features);
        
        // When: 执行PATCH
        universalElementService.patchElement(elementId, updates);
        
        // Then: 只应该更新priority，不应该触碰其他属性
        verify(pilotEMFService).setAttributeIfExists(eq(mockEObject), eq("priority"), eq("P1"));
        verify(pilotEMFService, never()).setAttributeIfExists(eq(mockEObject), eq("declaredName"), any());
        verify(pilotEMFService, never()).setAttributeIfExists(eq(mockEObject), eq("documentation"), any());
    }

    /**
     * 验收标准：REQ-B5-3
     * 删除元素
     */
    @Test
    @DisplayName("REQ-B5-3: 删除元素")
    public void testDeleteElement_ShouldRemoveElement() {
        // Given: 准备要删除的元素
        String elementId = "req-def-001";
        
        when(fileModelRepository.loadProject("default")).thenReturn(mockResource);
        EList<EObject> contents = new BasicEList<>();
        contents.add(mockEObject);
        when(mockResource.getContents()).thenReturn(contents);
        when(pilotEMFService.getAttributeValue(mockEObject, "elementId")).thenReturn(elementId);
        
        // When: 删除元素
        boolean result = universalElementService.deleteElement(elementId);
        
        // Then: 验证删除操作
        assertTrue(result);
        verify(fileModelRepository).saveProject(eq("default"), eq(mockResource));
    }

    /**
     * 验收标准：REQ-B5-3
     * 删除不存在的元素应返回false
     */
    @Test
    @DisplayName("REQ-B5-3: 删除不存在的元素返回false")
    public void testDeleteElement_ShouldReturnFalseForNonExistent() {
        // Given: 元素不存在
        String elementId = "non-existent";
        
        when(fileModelRepository.loadProject("default")).thenReturn(mockResource);
        EList<EObject> contents = new BasicEList<>();
        contents.add(mockEObject);
        when(mockResource.getContents()).thenReturn(contents);
        when(pilotEMFService.getAttributeValue(mockEObject, "elementId")).thenReturn("other-id");
        
        // When: 尝试删除
        boolean result = universalElementService.deleteElement(elementId);
        
        // Then: 返回false
        assertFalse(result);
        verify(fileModelRepository, never()).saveProject(any(), any());
    }

    /**
     * 验收标准：REQ-B2-1
     * 创建元素时验证必填字段
     */
    @Test
    @DisplayName("REQ-B2-1: 创建时elementId必填")
    public void testCreateElement_ShouldRequireElementId() {
        // Given: 缺少elementId
        String eClassName = "RequirementDefinition";
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("declaredName", "测试需求");
        // 没有elementId
        
        // When & Then: 应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            universalElementService.createElement(eClassName, attributes);
        }, "缺少elementId应该抛出异常");
    }

    /**
     * 验收标准：REQ-B5-3
     * 根据ID查找元素
     */
    @Test
    @DisplayName("REQ-B5-3: 根据ID查找元素")
    public void testFindElementById_ShouldReturnElement() {
        // Given: 准备元素
        String elementId = "req-def-001";
        
        when(fileModelRepository.loadProject("default")).thenReturn(mockResource);
        EList<EObject> contents = new BasicEList<>();
        contents.add(mockEObject);
        when(mockResource.getContents()).thenReturn(contents);
        when(pilotEMFService.getAttributeValue(mockEObject, "elementId")).thenReturn(elementId);
        when(mockEObject.eClass()).thenReturn(mockEClass);
        when(mockEClass.getName()).thenReturn("RequirementDefinition");
        EList<EStructuralFeature> features = new BasicEList<>();
        when(mockEClass.getEAllStructuralFeatures()).thenReturn(features);
        
        // When: 查找元素
        ElementDTO result = universalElementService.findElementById(elementId);
        
        // Then: 返回正确元素
        assertNotNull(result);
        assertEquals("RequirementDefinition", result.getEClass());
        assertEquals(elementId, result.getElementId());
    }

    /**
     * 验收标准：REQ-D0-1
     * ElementDTO应包含所有EMF属性
     */
    @Test
    @DisplayName("REQ-D0-1: 转换时保留所有EMF属性")
    public void testToDTO_ShouldPreserveAllAttributes() {
        // Given: EMF对象有多个属性
        when(mockEObject.eClass()).thenReturn(mockEClass);
        when(mockEClass.getName()).thenReturn("RequirementDefinition");
        
        // Mock EStructuralFeature列表
        EList<EStructuralFeature> features = new BasicEList<>();
        EStructuralFeature elementIdFeature = mock(EStructuralFeature.class);
        EStructuralFeature nameFeature = mock(EStructuralFeature.class);
        EStructuralFeature shortNameFeature = mock(EStructuralFeature.class);
        EStructuralFeature docFeature = mock(EStructuralFeature.class);
        EStructuralFeature priorityFeature = mock(EStructuralFeature.class);
        
        features.add(elementIdFeature);
        features.add(nameFeature);
        features.add(shortNameFeature);
        features.add(docFeature);
        features.add(priorityFeature);
        
        when(mockEClass.getEAllStructuralFeatures()).thenReturn(features);
        
        // Mock每个Feature的属性
        when(elementIdFeature.getName()).thenReturn("elementId");
        when(elementIdFeature.isDerived()).thenReturn(false);
        when(elementIdFeature.isTransient()).thenReturn(false);
        when(pilotEMFService.getAttributeValue(mockEObject, "elementId")).thenReturn("req-def-001");
        
        when(nameFeature.getName()).thenReturn("declaredName");
        when(nameFeature.isDerived()).thenReturn(false);
        when(nameFeature.isTransient()).thenReturn(false);
        when(pilotEMFService.getAttributeValue(mockEObject, "declaredName")).thenReturn("测试需求");
        
        when(shortNameFeature.getName()).thenReturn("declaredShortName");
        when(shortNameFeature.isDerived()).thenReturn(false);
        when(shortNameFeature.isTransient()).thenReturn(false);
        when(pilotEMFService.getAttributeValue(mockEObject, "declaredShortName")).thenReturn("REQ-001");
        
        when(docFeature.getName()).thenReturn("documentation");
        when(docFeature.isDerived()).thenReturn(false);
        when(docFeature.isTransient()).thenReturn(false);
        when(pilotEMFService.getAttributeValue(mockEObject, "documentation")).thenReturn("需求描述");
        
        when(priorityFeature.getName()).thenReturn("priority");
        when(priorityFeature.isDerived()).thenReturn(false);
        when(priorityFeature.isTransient()).thenReturn(false);
        when(pilotEMFService.getAttributeValue(mockEObject, "priority")).thenReturn("P0");
        
        // When: 转换为DTO
        ElementDTO dto = universalElementService.toDTO(mockEObject);
        
        // Then: 所有属性都应该保留
        assertNotNull(dto);
        assertEquals("RequirementDefinition", dto.getEClass());
        assertEquals("req-def-001", dto.getElementId());
        assertEquals("测试需求", dto.getProperty("declaredName"));
        assertEquals("REQ-001", dto.getProperty("declaredShortName"));
        assertEquals("需求描述", dto.getProperty("documentation"));
        assertEquals("P0", dto.getProperty("priority"));
    }
}