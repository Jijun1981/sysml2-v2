package com.sysml.mvp.mapper;

import com.sysml.mvp.dto.ElementDTO;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.common.util.BasicEList;
import org.eclipse.emf.common.util.EList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * ElementMapper 测试用例
 * 
 * 需求对齐：
 * - REQ-B2-4: DTO选择性映射 - 支持EMF对象与DTO互相转换
 * - REQ-D0-1: 通用元素数据API - 保持eClass字段的准确性
 * - 通用约定: 未识别字段应保留（round-trip不丢失）
 */
@DisplayName("ElementMapper测试 - REQ-B2-4")
public class ElementMapperTest {

    private ElementMapper elementMapper;
    
    @Mock
    private EObject mockEObject;
    
    @Mock
    private EClass mockEClass;
    
    @Mock
    private EStructuralFeature mockFeature1;
    
    @Mock
    private EStructuralFeature mockFeature2;
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        elementMapper = new ElementMapper();
    }

    /**
     * 验收标准：REQ-B2-4
     * ElementMapper应该能将EMF对象转换为DTO
     */
    @Test
    @DisplayName("REQ-B2-4: EMF对象转换为DTO")
    public void testToDTO_ShouldConvertEMFObjectToDTO() {
        // Given: 准备EMF对象
        when(mockEObject.eClass()).thenReturn(mockEClass);
        when(mockEClass.getName()).thenReturn("RequirementDefinition");
        
        // 准备属性
        EList<EStructuralFeature> features = new BasicEList<>();
        features.add(mockFeature1);
        features.add(mockFeature2);
        when(mockEClass.getEAllStructuralFeatures()).thenReturn(features);
        
        when(mockFeature1.getName()).thenReturn("elementId");
        when(mockFeature1.isDerived()).thenReturn(false);
        when(mockFeature1.isTransient()).thenReturn(false);
        when(mockEObject.eGet(mockFeature1)).thenReturn("req-def-001");
        
        when(mockFeature2.getName()).thenReturn("declaredName");
        when(mockFeature2.isDerived()).thenReturn(false);
        when(mockFeature2.isTransient()).thenReturn(false);
        when(mockEObject.eGet(mockFeature2)).thenReturn("测试需求");
        
        // When: 转换为DTO
        ElementDTO result = elementMapper.toDTO(mockEObject);
        
        // Then: 验证转换结果
        assertNotNull(result);
        assertEquals("RequirementDefinition", result.getEClass());
        assertEquals("req-def-001", result.getElementId());
        assertEquals("测试需求", result.getProperty("declaredName"));
    }

    /**
     * 验收标准：REQ-B2-4
     * 转换时应该跳过派生属性和瞬态属性
     */
    @Test
    @DisplayName("REQ-B2-4: 跳过派生属性和瞬态属性")
    public void testToDTO_ShouldSkipDerivedAndTransientAttributes() {
        // Given: 准备包含派生属性的EMF对象
        when(mockEObject.eClass()).thenReturn(mockEClass);
        when(mockEClass.getName()).thenReturn("RequirementDefinition");
        
        EList<EStructuralFeature> features = new BasicEList<>();
        features.add(mockFeature1);
        features.add(mockFeature2);
        when(mockEClass.getEAllStructuralFeatures()).thenReturn(features);
        
        // 第一个特征是正常属性
        when(mockFeature1.getName()).thenReturn("elementId");
        when(mockFeature1.isDerived()).thenReturn(false);
        when(mockFeature1.isTransient()).thenReturn(false);
        when(mockEObject.eGet(mockFeature1)).thenReturn("req-def-001");
        
        // 第二个特征是派生属性
        when(mockFeature2.getName()).thenReturn("derivedAttribute");
        when(mockFeature2.isDerived()).thenReturn(true);
        when(mockFeature2.isTransient()).thenReturn(false);
        
        // When: 转换为DTO
        ElementDTO result = elementMapper.toDTO(mockEObject);
        
        // Then: 派生属性不应该包含在结果中
        assertNotNull(result);
        assertEquals("req-def-001", result.getElementId());
        assertNull(result.getProperty("derivedAttribute"));
        
        // 不应该调用派生属性的eGet
        verify(mockEObject, never()).eGet(mockFeature2);
    }

    /**
     * 验收标准：REQ-D0-1
     * 支持任意SysML元素类型的转换
     */
    @Test
    @DisplayName("REQ-D0-1: 支持任意SysML类型转换")
    public void testToDTO_ShouldSupportAnySysMLType() {
        // Given: 测试多种SysML类型
        String[] sysmlTypes = {
            "RequirementUsage",
            "PartDefinition",
            "InterfaceDefinition",
            "Connection",
            "Dependency"
        };
        
        for (String type : sysmlTypes) {
            // Given
            when(mockEObject.eClass()).thenReturn(mockEClass);
            when(mockEClass.getName()).thenReturn(type);
            
            EList<EStructuralFeature> features = new BasicEList<>();
            features.add(mockFeature1);
            when(mockEClass.getEAllStructuralFeatures()).thenReturn(features);
            
            when(mockFeature1.getName()).thenReturn("elementId");
            when(mockFeature1.isDerived()).thenReturn(false);
            when(mockFeature1.isTransient()).thenReturn(false);
            when(mockEObject.eGet(mockFeature1)).thenReturn(type.toLowerCase() + "-001");
            
            // When
            ElementDTO result = elementMapper.toDTO(mockEObject);
            
            // Then
            assertNotNull(result, "应该支持" + type + "类型");
            assertEquals(type, result.getEClass());
            assertEquals(type.toLowerCase() + "-001", result.getElementId());
        }
    }

    /**
     * 验收标准：REQ-B2-4
     * ElementMapper应该能将DTO转换为属性Map
     */
    @Test
    @DisplayName("REQ-B2-4: DTO转换为属性Map")
    public void testToMap_ShouldConvertDTOToMap() {
        // Given: 准备ElementDTO
        ElementDTO dto = new ElementDTO();
        dto.setEClass("RequirementDefinition");
        dto.setElementId("req-def-001");
        dto.setProperty("declaredName", "测试需求");
        dto.setProperty("declaredShortName", "REQ-001");
        dto.setProperty("documentation", "这是一个测试需求");
        dto.setProperty("priority", "P0");
        
        // When: 转换为Map
        Map<String, Object> result = elementMapper.toMap(dto);
        
        // Then: 验证转换结果
        assertNotNull(result);
        assertEquals("req-def-001", result.get("elementId"));
        assertEquals("测试需求", result.get("declaredName"));
        assertEquals("REQ-001", result.get("declaredShortName"));
        assertEquals("这是一个测试需求", result.get("documentation"));
        assertEquals("P0", result.get("priority"));
        
        // eClass不应该包含在Map中（它是元数据，不是属性）
        assertFalse(result.containsKey("eClass"));
    }

    /**
     * 验收标准：通用约定
     * 未识别字段应保留（round-trip不丢失）
     */
    @Test
    @DisplayName("通用约定: round-trip转换不丢失数据")
    public void testRoundTrip_ShouldPreserveAllData() {
        // Given: 原始EMF对象包含多种属性类型
        when(mockEObject.eClass()).thenReturn(mockEClass);
        when(mockEClass.getName()).thenReturn("RequirementDefinition");
        
        EList<EStructuralFeature> features = new BasicEList<>();
        EStructuralFeature feature1 = mock(EStructuralFeature.class);
        EStructuralFeature feature2 = mock(EStructuralFeature.class);
        EStructuralFeature feature3 = mock(EStructuralFeature.class);
        EStructuralFeature feature4 = mock(EStructuralFeature.class);
        features.add(feature1);
        features.add(feature2);
        features.add(feature3);
        features.add(feature4);
        when(mockEClass.getEAllStructuralFeatures()).thenReturn(features);
        
        // 设置各种类型的属性
        when(feature1.getName()).thenReturn("elementId");
        when(feature1.isDerived()).thenReturn(false);
        when(feature1.isTransient()).thenReturn(false);
        when(mockEObject.eGet(feature1)).thenReturn("req-def-001");
        
        when(feature2.getName()).thenReturn("declaredName");
        when(feature2.isDerived()).thenReturn(false);
        when(feature2.isTransient()).thenReturn(false);
        when(mockEObject.eGet(feature2)).thenReturn("测试需求");
        
        when(feature3.getName()).thenReturn("priority");
        when(feature3.isDerived()).thenReturn(false);
        when(feature3.isTransient()).thenReturn(false);
        when(mockEObject.eGet(feature3)).thenReturn("P0");
        
        when(feature4.getName()).thenReturn("customField");
        when(feature4.isDerived()).thenReturn(false);
        when(feature4.isTransient()).thenReturn(false);
        when(mockEObject.eGet(feature4)).thenReturn("customValue");
        
        // When: EMF -> DTO -> Map round-trip
        ElementDTO dto = elementMapper.toDTO(mockEObject);
        Map<String, Object> map = elementMapper.toMap(dto);
        
        // Then: 所有数据都应该保留
        assertEquals("req-def-001", map.get("elementId"));
        assertEquals("测试需求", map.get("declaredName"));
        assertEquals("P0", map.get("priority"));
        assertEquals("customValue", map.get("customField"));
        
        // 验证DTO也保留了所有数据
        assertEquals("RequirementDefinition", dto.getEClass());
        assertEquals("req-def-001", dto.getElementId());
        assertEquals("测试需求", dto.getProperty("declaredName"));
        assertEquals("P0", dto.getProperty("priority"));
        assertEquals("customValue", dto.getProperty("customField"));
    }

    /**
     * 验收标准：REQ-B2-4
     * 处理null值和空值
     */
    @Test
    @DisplayName("REQ-B2-4: 处理null值和空值")
    public void testToDTO_ShouldHandleNullAndEmptyValues() {
        // Given: EMF对象有null值和空值
        when(mockEObject.eClass()).thenReturn(mockEClass);
        when(mockEClass.getName()).thenReturn("RequirementDefinition");
        
        EList<EStructuralFeature> features = new BasicEList<>();
        features.add(mockFeature1);
        features.add(mockFeature2);
        when(mockEClass.getEAllStructuralFeatures()).thenReturn(features);
        
        when(mockFeature1.getName()).thenReturn("elementId");
        when(mockFeature1.isDerived()).thenReturn(false);
        when(mockFeature1.isTransient()).thenReturn(false);
        when(mockEObject.eGet(mockFeature1)).thenReturn("req-def-001");
        
        when(mockFeature2.getName()).thenReturn("nullAttribute");
        when(mockFeature2.isDerived()).thenReturn(false);
        when(mockFeature2.isTransient()).thenReturn(false);
        when(mockEObject.eGet(mockFeature2)).thenReturn(null);
        
        // When: 转换为DTO
        ElementDTO result = elementMapper.toDTO(mockEObject);
        
        // Then: null值不应该包含在properties中
        assertNotNull(result);
        assertEquals("req-def-001", result.getElementId());
        assertFalse(result.getProperties().containsKey("nullAttribute"));
    }

    /**
     * 验收标准：REQ-B2-4
     * toMap处理空DTO
     */
    @Test
    @DisplayName("REQ-B2-4: toMap处理空DTO")
    public void testToMap_ShouldHandleEmptyDTO() {
        // Given: 空DTO
        ElementDTO dto = new ElementDTO();
        
        // When: 转换为Map
        Map<String, Object> result = elementMapper.toMap(dto);
        
        // Then: 应该返回空Map，不应该抛异常
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    /**
     * 验收标准：REQ-B2-4
     * toDTO处理null输入
     */
    @Test
    @DisplayName("REQ-B2-4: toDTO处理null输入")
    public void testToDTO_ShouldHandleNullInput() {
        // When: 输入null
        ElementDTO result = elementMapper.toDTO(null);
        
        // Then: 应该返回null
        assertNull(result);
    }

    /**
     * 验收标准：REQ-B2-4
     * toMap处理null输入
     */
    @Test
    @DisplayName("REQ-B2-4: toMap处理null输入")
    public void testToMap_ShouldHandleNullInput() {
        // When: 输入null
        Map<String, Object> result = elementMapper.toMap(null);
        
        // Then: 应该返回空Map
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}