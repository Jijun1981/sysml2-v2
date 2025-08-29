package com.sysml.mvp.service;

import com.sysml.mvp.dto.ElementDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.eclipse.emf.ecore.EObject;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Dependency字段映射测试用例
 * 测试需求: REQ-FIELD-002 - Dependency字段标准化
 * 
 * 验收标准:
 * - TraceService创建Dependency时使用client/supplier字段
 * - PilotEMFService.setDependencyReferences()正确设置标准字段
 * - 数据生成脚本使用标准字段名
 * - 前端API调用适配新字段名
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("REQ-FIELD-002: Dependency字段标准化测试")
public class DependencyFieldMappingTest {

    @Mock
    private UniversalElementService universalElementService;
    
    @Mock
    private ValidationService validationService;
    
    @Mock
    private PilotEMFService pilotEMFService;
    
    private TraceService traceService;

    @BeforeEach
    public void setUp() {
        traceService = new TraceService(universalElementService, validationService);
    }

    /**
     * 测试点: client/supplier字段设置
     * 验收标准: TraceService创建Dependency时使用client/supplier字段
     */
    @Test
    @DisplayName("TraceService应该使用client/supplier字段创建Dependency")
    public void testTraceService_ShouldUseClientSupplierFields() {
        // Given: 创建追溯关系的请求数据
        Map<String, Object> traceData = new HashMap<>();
        traceData.put("fromId", "REQ-SOURCE");
        traceData.put("toId", "REQ-TARGET");
        traceData.put("type", "derive");
        
        // 模拟返回的ElementDTO
        ElementDTO expectedResult = new ElementDTO();
        expectedResult.setElementId("DERIVE-001");
        expectedResult.setEClass("DeriveRequirement");
        
        // 验证传给UniversalElementService的数据应该包含client/supplier字段
        when(universalElementService.createElement(eq("DeriveRequirement"), argThat(data -> {
            Map<String, Object> elementData = (Map<String, Object>) data;
            // 验证使用了标准字段名
            return elementData.containsKey("client") && 
                   elementData.containsKey("supplier") &&
                   "REQ-SOURCE".equals(elementData.get("client")) &&
                   "REQ-TARGET".equals(elementData.get("supplier")) &&
                   // 验证没有使用旧字段名
                   !elementData.containsKey("fromId") &&
                   !elementData.containsKey("toId");
        }))).thenReturn(expectedResult);
        
        when(validationService.validateTraceDuplication("REQ-SOURCE", "REQ-TARGET", "derive")).thenReturn(true);
        when(validationService.validateTraceSemantics("REQ-SOURCE", "REQ-TARGET", "derive")).thenReturn(true);
        
        // When: 创建追溯关系
        ElementDTO result = traceService.createTrace(traceData);
        
        // Then: 验证结果和调用
        assertNotNull(result);
        assertEquals("DERIVE-001", result.getElementId());
        assertEquals("DeriveRequirement", result.getEClass());
        
        // 验证使用了标准字段名调用UniversalElementService
        verify(universalElementService).createElement(eq("DeriveRequirement"), argThat(data -> {
            Map<String, Object> elementData = (Map<String, Object>) data;
            return elementData.containsKey("client") && 
                   elementData.containsKey("supplier") &&
                   "REQ-SOURCE".equals(elementData.get("client")) &&
                   "REQ-TARGET".equals(elementData.get("supplier"));
        }));
    }

    /**
     * 测试点: fromId/toId兼容性处理
     * 验收标准: 能够处理旧字段名并转换为标准字段
     */
    @Test
    @DisplayName("TraceService应该兼容旧的fromId/toId字段名")
    public void testTraceService_ShouldCompatibleWithOldFieldNames() {
        // Given: 使用旧字段名的请求数据
        Map<String, Object> traceDataWithOldFields = new HashMap<>();
        traceDataWithOldFields.put("fromId", "REQ-A");
        traceDataWithOldFields.put("toId", "REQ-B");
        traceDataWithOldFields.put("type", "satisfy");
        
        ElementDTO expectedResult = new ElementDTO();
        expectedResult.setElementId("SATISFY-001");
        expectedResult.setEClass("Satisfy");
        
        when(universalElementService.createElement(eq("Satisfy"), any())).thenReturn(expectedResult);
        when(validationService.validateTraceDuplication("REQ-A", "REQ-B", "satisfy")).thenReturn(true);
        when(validationService.validateTraceSemantics("REQ-A", "REQ-B", "satisfy")).thenReturn(true);
        
        // When: 创建追溯关系（使用旧字段名）
        ElementDTO result = traceService.createTrace(traceDataWithOldFields);
        
        // Then: 验证能够正常处理并转换为标准字段
        assertNotNull(result);
        assertEquals("SATISFY-001", result.getElementId());
        
        // 验证内部使用了标准字段名
        verify(universalElementService).createElement(eq("Satisfy"), argThat(data -> {
            Map<String, Object> elementData = (Map<String, Object>) data;
            // 应该转换为标准字段名
            return elementData.containsKey("client") && 
                   elementData.containsKey("supplier") &&
                   "REQ-A".equals(elementData.get("client")) &&
                   "REQ-B".equals(elementData.get("supplier"));
        }));
    }

    /**
     * 测试点: 字段标准化验证
     * 验收标准: 所有Dependency类型都使用client/supplier字段
     */
    @Test
    @DisplayName("所有Dependency类型都应该使用client/supplier字段")
    public void testAllDependencyTypes_ShouldUseClientSupplierFields() {
        // Given: 测试所有支持的依赖关系类型
        String[][] testCases = {
            {"derive", "DeriveRequirement"},
            {"satisfy", "Satisfy"},
            {"refine", "Refine"},
            {"trace", "Trace"}
        };
        
        for (String[] testCase : testCases) {
            String type = testCase[0];
            String expectedEClass = testCase[1];
            
            Map<String, Object> traceData = new HashMap<>();
            traceData.put("fromId", "REQ-FROM-" + type.toUpperCase());
            traceData.put("toId", "REQ-TO-" + type.toUpperCase());
            traceData.put("type", type);
            
            ElementDTO mockResult = new ElementDTO();
            mockResult.setElementId(type.toUpperCase() + "-001");
            mockResult.setEClass(expectedEClass);
            
            when(universalElementService.createElement(eq(expectedEClass), any())).thenReturn(mockResult);
            when(validationService.validateTraceDuplication(anyString(), anyString(), eq(type))).thenReturn(true);
            when(validationService.validateTraceSemantics(anyString(), anyString(), eq(type))).thenReturn(true);
            
            // When: 创建此类型的追溯关系
            ElementDTO result = traceService.createTrace(traceData);
            
            // Then: 验证使用了标准字段
            assertNotNull(result, "类型 " + type + " 应该能够创建");
            assertEquals(expectedEClass, result.getEClass(), "EClass应该正确映射");
            
            // 验证使用了client/supplier字段调用
            verify(universalElementService).createElement(eq(expectedEClass), argThat(data -> {
                Map<String, Object> elementData = (Map<String, Object>) data;
                return elementData.containsKey("client") && 
                       elementData.containsKey("supplier") &&
                       ("REQ-FROM-" + type.toUpperCase()).equals(elementData.get("client")) &&
                       ("REQ-TO-" + type.toUpperCase()).equals(elementData.get("supplier"));
            }));
        }
    }

    /**
     * 测试点: PilotEMFService字段设置
     * 验收标准: TraceService间接验证client/supplier字段正确传递
     */
    @Test
    @DisplayName("TraceService应该正确传递client/supplier字段到UniversalElementService")
    public void testTraceService_ShouldPassClientSupplierFieldsCorrectly() {
        // Given: 创建包含client/supplier字段的追溯关系数据
        String clientId = "REQ-CLIENT";
        String supplierId = "REQ-SUPPLIER";
        
        Map<String, Object> traceData = new HashMap<>();
        traceData.put("client", clientId);
        traceData.put("supplier", supplierId);
        traceData.put("type", "derive");
        
        ElementDTO mockResult = new ElementDTO();
        mockResult.setElementId("DERIVE-001");
        mockResult.setEClass("DeriveRequirement");
        
        when(universalElementService.createElement(eq("DeriveRequirement"), any())).thenReturn(mockResult);
        when(validationService.validateTraceDuplication(clientId, supplierId, "derive")).thenReturn(true);
        when(validationService.validateTraceSemantics(clientId, supplierId, "derive")).thenReturn(true);
        
        // When: 创建追溯关系
        ElementDTO result = traceService.createTrace(traceData);
        
        // Then: 验证调用了正确的字段设置
        assertNotNull(result);
        assertEquals("DERIVE-001", result.getElementId());
        
        verify(universalElementService).createElement(eq("DeriveRequirement"), argThat(data -> {
            Map<String, Object> elementData = (Map<String, Object>) data;
            return elementData.containsKey("client") && 
                   elementData.containsKey("supplier") &&
                   clientId.equals(elementData.get("client")) &&
                   supplierId.equals(elementData.get("supplier"));
        }));
    }

    /**
     * 测试点: 数据验证
     * 验收标准: 验证client/supplier字段必须存在
     */
    @Test
    @DisplayName("应该验证client/supplier字段必须存在")
    public void testValidation_ShouldRequireClientSupplierFields() {
        // Given: 缺少必需字段的数据
        Map<String, Object> incompleteData = new HashMap<>();
        incompleteData.put("type", "derive");
        // 缺少fromId/toId或client/supplier字段
        
        // When & Then: 应该抛出异常
        assertThrows(IllegalArgumentException.class, () -> {
            traceService.createTrace(incompleteData);
        }, "缺少client/supplier字段时应该抛出异常");
    }
}