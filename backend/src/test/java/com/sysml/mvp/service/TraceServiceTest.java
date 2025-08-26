package com.sysml.mvp.service;

import com.sysml.mvp.dto.ElementDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * TraceService 测试用例
 * 
 * 需求对齐：
 * - REQ-B5-2: 追溯关系服务 - 业务验证后委托给UniversalElementService
 * - REQ-C3-1: 创建追溯关系 - type映射到具体EClass
 * - REQ-C3-2: 查询追溯关系 - 支持type和element过滤
 * - REQ-C3-3: 去重追溯关系 - 检测相同(source,target,type)的重复关系
 * - REQ-C3-4: 追溯语义约束 - 验证Satisfy/DeriveRequirement/Refine语义约束
 */
@DisplayName("TraceService测试 - REQ-B5-2")
public class TraceServiceTest {

    @Mock
    private UniversalElementService universalElementService;
    
    @Mock 
    private ValidationService validationService;
    
    private TraceService traceService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        traceService = new TraceService(universalElementService, validationService);
    }
    
    /**
     * 验收标准：REQ-C3-1
     * 创建Satisfy追溯关系时应映射到Satisfy EClass
     */
    @Test
    @DisplayName("REQ-C3-1: 创建Satisfy追溯关系映射到EClass")
    public void testCreateTrace_ShouldMapSatisfyTypeToEClass() {
        // Given: satisfy类型的追溯关系数据
        Map<String, Object> traceData = new HashMap<>();
        traceData.put("source", "part-001");
        traceData.put("target", "req-def-001");
        traceData.put("type", "satisfy");
        traceData.put("name", "Engine satisfies performance requirement");
        
        ElementDTO expectedResult = new ElementDTO();
        expectedResult.setElementId("satisfy-001");
        expectedResult.setEClass("Satisfy");
        expectedResult.setProperty("fromId", "part-001");
        expectedResult.setProperty("toId", "req-def-001");
        
        // When: 创建satisfy追溯关系
        when(validationService.validateTraceDuplication("part-001", "req-def-001", "satisfy")).thenReturn(true);
        when(validationService.validateTraceSemantics("part-001", "req-def-001", "satisfy")).thenReturn(true);
        when(universalElementService.createElement(eq("Satisfy"), any(Map.class))).thenReturn(expectedResult);
        
        ElementDTO result = traceService.createTrace(traceData);
        
        // Then: 应该成功创建Satisfy追溯关系
        assertNotNull(result);
        assertEquals("satisfy-001", result.getElementId());
        assertEquals("Satisfy", result.getEClass());
        assertEquals("part-001", result.getProperty("fromId"));
        assertEquals("req-def-001", result.getProperty("toId"));
        
        verify(validationService).validateTraceDuplication("part-001", "req-def-001", "satisfy");
        verify(validationService).validateTraceSemantics("part-001", "req-def-001", "satisfy");
        verify(universalElementService).createElement(eq("Satisfy"), any(Map.class));
    }
    
    /**
     * 验收标准：REQ-C3-1
     * 创建DeriveRequirement追溯关系时应映射到DeriveRequirement EClass
     */
    @Test
    @DisplayName("REQ-C3-1: 创建DeriveRequirement追溯关系映射")
    public void testCreateTrace_ShouldMapDeriveTypeToEClass() {
        // Given: derive类型的追溯关系数据
        Map<String, Object> traceData = new HashMap<>();
        traceData.put("source", "req-def-001");
        traceData.put("target", "req-def-002");
        traceData.put("type", "derive");
        
        ElementDTO expectedResult = new ElementDTO();
        expectedResult.setElementId("derive-001");
        expectedResult.setEClass("DeriveRequirement");
        
        // When: 创建derive追溯关系
        when(validationService.validateTraceDuplication("req-def-001", "req-def-002", "derive")).thenReturn(true);
        when(validationService.validateTraceSemantics("req-def-001", "req-def-002", "derive")).thenReturn(true);
        when(universalElementService.createElement(eq("DeriveRequirement"), any(Map.class))).thenReturn(expectedResult);
        
        ElementDTO result = traceService.createTrace(traceData);
        
        // Then: 应该成功创建DeriveRequirement追溯关系
        assertNotNull(result);
        assertEquals("derive-001", result.getElementId());
        assertEquals("DeriveRequirement", result.getEClass());
        
        verify(universalElementService).createElement(eq("DeriveRequirement"), any(Map.class));
    }
    
    /**
     * 验收标准：REQ-C3-1
     * 支持四种标准追溯类型的映射
     */
    @Test
    @DisplayName("REQ-C3-1: 支持四种追溯类型映射")
    public void testCreateTrace_ShouldSupportFourTraceTypes() {
        // Given: 四种追溯类型的映射规则
        String[][] typeMappings = {
            {"derive", "DeriveRequirement"},
            {"satisfy", "Satisfy"},
            {"refine", "Refine"},
            {"trace", "Trace"}
        };
        
        for (String[] mapping : typeMappings) {
            String type = mapping[0];
            String expectedEClass = mapping[1];
            
            Map<String, Object> traceData = new HashMap<>();
            traceData.put("source", "element-001");
            traceData.put("target", "element-002");
            traceData.put("type", type);
            
            ElementDTO expectedResult = new ElementDTO();
            expectedResult.setElementId(type + "-001");
            expectedResult.setEClass(expectedEClass);
            
            // When: 创建追溯关系
            when(validationService.validateTraceDuplication("element-001", "element-002", type)).thenReturn(true);
            when(validationService.validateTraceSemantics("element-001", "element-002", type)).thenReturn(true);
            when(universalElementService.createElement(eq(expectedEClass), any(Map.class))).thenReturn(expectedResult);
            
            ElementDTO result = traceService.createTrace(traceData);
            
            // Then: 应该正确映射到对应EClass
            assertNotNull(result, "Failed for type: " + type);
            assertEquals(expectedEClass, result.getEClass(), "Wrong EClass for type: " + type);
            
            verify(universalElementService).createElement(eq(expectedEClass), any(Map.class));
        }
    }
    
    /**
     * 验收标准：REQ-C3-3
     * 检测重复追溯关系时应抛出异常
     */
    @Test
    @DisplayName("REQ-C3-3: 检测重复追溯关系")
    public void testCreateTrace_ShouldThrowExceptionWhenDuplicate() {
        // Given: 重复的追溯关系
        Map<String, Object> traceData = new HashMap<>();
        traceData.put("source", "part-001");
        traceData.put("target", "req-def-001");
        traceData.put("type", "satisfy");
        
        // When: 去重验证失败
        when(validationService.validateTraceDuplication("part-001", "req-def-001", "satisfy")).thenReturn(false);
        
        // Then: 应该抛出IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> traceService.createTrace(traceData));
        
        assertTrue(exception.getMessage().contains("Duplicate trace relationship"));
        assertTrue(exception.getMessage().contains("part-001"));
        assertTrue(exception.getMessage().contains("req-def-001"));
        assertTrue(exception.getMessage().contains("satisfy"));
        
        verify(validationService).validateTraceDuplication("part-001", "req-def-001", "satisfy");
        verify(universalElementService, never()).createElement(anyString(), any(Map.class));
    }
    
    /**
     * 验收标准：REQ-C3-4
     * 语义约束验证失败时应抛出异常
     */
    @Test
    @DisplayName("REQ-C3-4: 语义约束验证失败")
    public void testCreateTrace_ShouldThrowExceptionWhenSemanticsInvalid() {
        // Given: 语义约束无效的追溯关系
        Map<String, Object> traceData = new HashMap<>();
        traceData.put("source", "invalid-element");
        traceData.put("target", "req-def-001");
        traceData.put("type", "satisfy");
        
        // When: 去重验证通过但语义验证失败
        when(validationService.validateTraceDuplication("invalid-element", "req-def-001", "satisfy")).thenReturn(true);
        when(validationService.validateTraceSemantics("invalid-element", "req-def-001", "satisfy")).thenReturn(false);
        
        // Then: 应该抛出IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> traceService.createTrace(traceData));
        
        assertTrue(exception.getMessage().contains("Invalid trace semantics"));
        assertTrue(exception.getMessage().contains("satisfy"));
        
        verify(validationService).validateTraceDuplication("invalid-element", "req-def-001", "satisfy");
        verify(validationService).validateTraceSemantics("invalid-element", "req-def-001", "satisfy");
        verify(universalElementService, never()).createElement(anyString(), any(Map.class));
    }
    
    /**
     * 验收标准：REQ-C3-1
     * 缺少必填字段时应抛出异常
     */
    @Test
    @DisplayName("REQ-C3-1: 必填字段验证")
    public void testCreateTrace_ShouldValidateRequiredFields() {
        // Given: 缺少source字段
        Map<String, Object> traceData = new HashMap<>();
        traceData.put("target", "req-def-001");
        traceData.put("type", "satisfy");
        
        // When & Then: 应该抛出IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> traceService.createTrace(traceData));
        
        assertTrue(exception.getMessage().contains("source is required"));
        
        verify(universalElementService, never()).createElement(anyString(), any(Map.class));
    }
    
    /**
     * 验收标准：REQ-C3-2
     * 查询所有追溯关系应委托给UniversalElementService
     */
    @Test
    @DisplayName("REQ-C3-2: 查询所有追溯关系")
    public void testGetAllTraces_ShouldDelegateToUniversalService() {
        // Given: 追溯关系列表
        ElementDTO satisfy = new ElementDTO();
        satisfy.setElementId("satisfy-001");
        satisfy.setEClass("Satisfy");
        
        ElementDTO derive = new ElementDTO();
        derive.setElementId("derive-001");
        derive.setEClass("DeriveRequirement");
        
        List<ElementDTO> allTraces = Arrays.asList(satisfy, derive);
        
        // When: 委托查询四种追溯类型
        when(universalElementService.queryElements("Satisfy")).thenReturn(Arrays.asList(satisfy));
        when(universalElementService.queryElements("DeriveRequirement")).thenReturn(Arrays.asList(derive));
        when(universalElementService.queryElements("Refine")).thenReturn(Arrays.asList());
        when(universalElementService.queryElements("Trace")).thenReturn(Arrays.asList());
        
        List<ElementDTO> result = traceService.getAllTraces();
        
        // Then: 应该返回所有追溯关系
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(t -> "satisfy-001".equals(t.getElementId())));
        assertTrue(result.stream().anyMatch(t -> "derive-001".equals(t.getElementId())));
        
        verify(universalElementService).queryElements("Satisfy");
        verify(universalElementService).queryElements("DeriveRequirement");
        verify(universalElementService).queryElements("Refine");
        verify(universalElementService).queryElements("Trace");
    }
    
    /**
     * 验收标准：REQ-C3-2
     * 按类型查询追溯关系支持过滤
     */
    @Test
    @DisplayName("REQ-C3-2: 按类型查询追溯关系")
    public void testGetTracesByType_ShouldSupportTypeFilter() {
        // Given: Satisfy类型的追溯关系
        ElementDTO satisfy1 = new ElementDTO();
        satisfy1.setElementId("satisfy-001");
        satisfy1.setEClass("Satisfy");
        
        ElementDTO satisfy2 = new ElementDTO();
        satisfy2.setElementId("satisfy-002");
        satisfy2.setEClass("Satisfy");
        
        List<ElementDTO> satisfyTraces = Arrays.asList(satisfy1, satisfy2);
        
        // When: 按类型查询
        when(universalElementService.queryElements("Satisfy")).thenReturn(satisfyTraces);
        
        List<ElementDTO> result = traceService.getTracesByType("satisfy");
        
        // Then: 应该返回指定类型的追溯关系
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("satisfy-001", result.get(0).getElementId());
        assertEquals("satisfy-002", result.get(1).getElementId());
        
        verify(universalElementService).queryElements("Satisfy");
    }
    
    /**
     * 验收标准：REQ-C3-2
     * 按元素查询追溯关系支持过滤
     */
    @Test
    @DisplayName("REQ-C3-2: 按元素查询追溯关系")
    public void testGetTracesByElement_ShouldSupportElementFilter() {
        // Given: 与特定元素相关的追溯关系
        ElementDTO satisfy = new ElementDTO();
        satisfy.setElementId("satisfy-001");
        satisfy.setEClass("Satisfy");
        satisfy.setProperty("fromId", "part-001");
        satisfy.setProperty("toId", "req-def-001");
        
        ElementDTO derive = new ElementDTO();
        derive.setElementId("derive-001");
        derive.setEClass("DeriveRequirement");
        derive.setProperty("fromId", "req-def-002");
        derive.setProperty("toId", "req-def-001");
        
        List<ElementDTO> allTraces = Arrays.asList(satisfy, derive);
        
        // When: 查询与req-def-001相关的追溯关系
        when(universalElementService.queryElements("Satisfy")).thenReturn(Arrays.asList(satisfy));
        when(universalElementService.queryElements("DeriveRequirement")).thenReturn(Arrays.asList(derive));
        when(universalElementService.queryElements("Refine")).thenReturn(Arrays.asList());
        when(universalElementService.queryElements("Trace")).thenReturn(Arrays.asList());
        
        List<ElementDTO> result = traceService.getTracesByElement("req-def-001");
        
        // Then: 应该返回相关的追溯关系
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(t -> "satisfy-001".equals(t.getElementId())));
        assertTrue(result.stream().anyMatch(t -> "derive-001".equals(t.getElementId())));
    }
    
    /**
     * 验收标准：REQ-B5-2
     * 删除追溯关系应委托给UniversalElementService
     */
    @Test
    @DisplayName("REQ-B5-2: 删除追溯关系")
    public void testDeleteTrace_ShouldDelegateToUniversalService() {
        // Given: 追溯关系ID和现有元素
        String elementId = "satisfy-001";
        ElementDTO existingTrace = new ElementDTO();
        existingTrace.setElementId(elementId);
        existingTrace.setEClass("Satisfy");
        
        // When: 元素存在且删除成功
        when(universalElementService.findElementById(elementId)).thenReturn(existingTrace);
        when(universalElementService.deleteElement(elementId)).thenReturn(true);
        
        // Then: 应该成功删除不抛异常
        assertDoesNotThrow(() -> traceService.deleteTrace(elementId));
        
        verify(universalElementService).deleteElement(elementId);
    }
    
    /**
     * 验收标准：REQ-B5-2
     * 根据ID查找追溯关系应委托给UniversalElementService
     */
    @Test
    @DisplayName("REQ-B5-2: 根据ID查找追溯关系")
    public void testGetTraceById_ShouldDelegateToUniversalService() {
        // Given: 追溯关系ID和预期结果
        String elementId = "satisfy-001";
        ElementDTO expectedTrace = new ElementDTO();
        expectedTrace.setElementId(elementId);
        expectedTrace.setEClass("Satisfy");
        
        // When: 委托查询
        when(universalElementService.findElementById(elementId)).thenReturn(expectedTrace);
        
        ElementDTO result = traceService.getTraceById(elementId);
        
        // Then: 应该返回指定追溯关系
        assertNotNull(result);
        assertEquals(elementId, result.getElementId());
        assertEquals("Satisfy", result.getEClass());
        
        verify(universalElementService).findElementById(elementId);
    }
    
    /**
     * 验收标准：REQ-C3-4
     * 获取追溯关系的语义验证信息
     */
    @Test
    @DisplayName("REQ-C3-4: 获取追溯关系语义验证信息")
    public void testGetTraceSemanticValidation_ShouldProvideValidationInfo() {
        // Given: 追溯关系语义验证查询
        String source = "part-001";
        String target = "req-def-001";
        String type = "satisfy";
        
        // When: 获取语义验证信息
        when(validationService.validateTraceSemantics(source, target, type)).thenReturn(true);
        when(validationService.getTraceSemanticValidationMessage(source, target, type))
                .thenReturn("Valid Satisfy relationship: PartUsage can satisfy RequirementDefinition");
        
        boolean isValid = traceService.validateTraceSemantics(source, target, type);
        String message = traceService.getTraceSemanticValidationMessage(source, target, type);
        
        // Then: 应该返回验证信息
        assertTrue(isValid);
        assertNotNull(message);
        assertTrue(message.contains("Valid Satisfy relationship"));
        
        verify(validationService).validateTraceSemantics(source, target, type);
        verify(validationService).getTraceSemanticValidationMessage(source, target, type);
    }
}