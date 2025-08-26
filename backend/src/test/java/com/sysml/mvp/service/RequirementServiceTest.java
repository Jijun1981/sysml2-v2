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
 * RequirementService 测试用例
 * 
 * 需求对齐：
 * - REQ-B5-1: 需求领域服务 - 业务验证后委托给UniversalElementService
 * - REQ-C1-1: reqId唯一性验证 - 创建需求定义时验证reqId唯一性（409冲突）
 * - REQ-C1-3: 更新需求定义
 * - REQ-C2-1: 创建需求使用
 * - REQ-C2-3: 约束对象必填 - RequirementUsage必须有subject
 * - REQ-C2-4: 删除前检查被引用保护 - RequirementDefinition被Usage引用时不能删除
 */
@DisplayName("RequirementService测试 - REQ-B5-1")
public class RequirementServiceTest {

    @Mock
    private UniversalElementService universalElementService;
    
    @Mock 
    private ValidationService validationService;
    
    private RequirementService requirementService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        requirementService = new RequirementService(universalElementService, validationService);
    }
    
    /**
     * 验收标准：REQ-C1-1
     * 创建RequirementDefinition时应验证reqId唯一性
     */
    @Test
    @DisplayName("REQ-C1-1: 创建需求定义验证reqId唯一性")
    public void testCreateRequirement_ShouldValidateReqIdUniqueness() {
        // Given: 准备需求数据
        Map<String, Object> reqData = new HashMap<>();
        reqData.put("reqId", "EBS-L1-001");
        reqData.put("declaredName", "电池系统需求");
        reqData.put("documentation", "系统应监控电池状态");
        
        ElementDTO expectedResult = new ElementDTO();
        expectedResult.setElementId("req-def-001");
        expectedResult.setEClass("RequirementDefinition");
        expectedResult.setProperty("reqId", "EBS-L1-001");
        
        // When: reqId唯一性验证通过
        when(validationService.validateReqIdUniqueness("EBS-L1-001")).thenReturn(true);
        when(universalElementService.createElement("RequirementDefinition", reqData)).thenReturn(expectedResult);
        
        ElementDTO result = requirementService.createRequirement(reqData);
        
        // Then: 应该成功创建需求
        assertNotNull(result);
        assertEquals("req-def-001", result.getElementId());
        assertEquals("RequirementDefinition", result.getEClass());
        assertEquals("EBS-L1-001", result.getProperty("reqId"));
        
        verify(validationService).validateReqIdUniqueness("EBS-L1-001");
        verify(universalElementService).createElement("RequirementDefinition", reqData);
    }
    
    /**
     * 验收标准：REQ-C1-1
     * 当reqId重复时应抛出IllegalArgumentException
     */
    @Test
    @DisplayName("REQ-C1-1: reqId重复时抛出异常")
    public void testCreateRequirement_ShouldThrowExceptionWhenReqIdExists() {
        // Given: 重复的reqId
        Map<String, Object> reqData = new HashMap<>();
        reqData.put("reqId", "EBS-L1-001");
        reqData.put("declaredName", "重复需求");
        
        // When: reqId唯一性验证失败
        when(validationService.validateReqIdUniqueness("EBS-L1-001")).thenReturn(false);
        
        // Then: 应该抛出IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> requirementService.createRequirement(reqData));
        
        assertTrue(exception.getMessage().contains("reqId already exists"));
        assertTrue(exception.getMessage().contains("EBS-L1-001"));
        
        verify(validationService).validateReqIdUniqueness("EBS-L1-001");
        verify(universalElementService, never()).createElement(anyString(), any());
    }
    
    /**
     * 验收标准：REQ-C1-1
     * 缺少reqId时应抛出IllegalArgumentException
     */
    @Test
    @DisplayName("REQ-C1-1: reqId必填验证")
    public void testCreateRequirement_ShouldThrowExceptionWhenReqIdMissing() {
        // Given: 缺少reqId的需求数据
        Map<String, Object> reqData = new HashMap<>();
        reqData.put("declaredName", "缺少reqId的需求");
        
        // When & Then: 应该抛出IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> requirementService.createRequirement(reqData));
        
        assertTrue(exception.getMessage().contains("reqId is required"));
        
        verify(validationService, never()).validateReqIdUniqueness(anyString());
        verify(universalElementService, never()).createElement(anyString(), any());
    }
    
    /**
     * 验收标准：REQ-C1-3
     * 更新RequirementDefinition支持部分更新
     */
    @Test
    @DisplayName("REQ-C1-3: 更新需求定义支持PATCH语义")
    public void testUpdateRequirement_ShouldSupportPartialUpdate() {
        // Given: 部分更新数据
        String elementId = "req-def-001";
        Map<String, Object> updates = new HashMap<>();
        updates.put("documentation", "更新后的需求描述");
        updates.put("priority", "P0");
        
        ElementDTO expectedResult = new ElementDTO();
        expectedResult.setElementId(elementId);
        expectedResult.setEClass("RequirementDefinition");
        expectedResult.setProperty("documentation", "更新后的需求描述");
        expectedResult.setProperty("priority", "P0");
        
        // When: 委托给UniversalElementService更新
        when(universalElementService.patchElement(elementId, updates)).thenReturn(expectedResult);
        
        ElementDTO result = requirementService.updateRequirement(elementId, updates);
        
        // Then: 应该返回更新后的需求
        assertNotNull(result);
        assertEquals(elementId, result.getElementId());
        assertEquals("更新后的需求描述", result.getProperty("documentation"));
        assertEquals("P0", result.getProperty("priority"));
        
        verify(universalElementService).patchElement(elementId, updates);
    }
    
    /**
     * 验收标准：REQ-C2-1
     * 创建RequirementUsage应验证约束对象必填
     */
    @Test
    @DisplayName("REQ-C2-1: 创建需求使用验证约束对象")
    public void testCreateRequirementUsage_ShouldValidateSubjectRequired() {
        // Given: 包含subject的需求使用数据
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("subject", "part-001");
        usageData.put("of", "req-def-001");
        usageData.put("declaredName", "电池温度监控使用");
        
        ElementDTO expectedResult = new ElementDTO();
        expectedResult.setElementId("req-usage-001");
        expectedResult.setEClass("RequirementUsage");
        expectedResult.setProperty("subject", "part-001");
        expectedResult.setProperty("of", "req-def-001");
        
        // When: 创建需求使用
        when(universalElementService.createElement("RequirementUsage", usageData)).thenReturn(expectedResult);
        
        ElementDTO result = requirementService.createRequirementUsage(usageData);
        
        // Then: 应该成功创建需求使用
        assertNotNull(result);
        assertEquals("req-usage-001", result.getElementId());
        assertEquals("RequirementUsage", result.getEClass());
        assertEquals("part-001", result.getProperty("subject"));
        
        verify(universalElementService).createElement("RequirementUsage", usageData);
    }
    
    /**
     * 验收标准：REQ-C2-3
     * RequirementUsage缺少subject时应抛出异常
     */
    @Test
    @DisplayName("REQ-C2-3: 约束对象必填验证")
    public void testCreateRequirementUsage_ShouldThrowExceptionWhenSubjectMissing() {
        // Given: 缺少subject的需求使用数据
        Map<String, Object> usageData = new HashMap<>();
        usageData.put("of", "req-def-001");
        usageData.put("declaredName", "缺少subject的使用");
        
        // When & Then: 应该抛出IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> requirementService.createRequirementUsage(usageData));
        
        assertTrue(exception.getMessage().contains("subject is required"));
        
        verify(universalElementService, never()).createElement(anyString(), any());
    }
    
    /**
     * 验收标准：REQ-C2-4
     * 删除被引用的RequirementDefinition时应检查保护
     */
    @Test
    @DisplayName("REQ-C2-4: 删除前检查被引用保护")
    public void testDeleteRequirement_ShouldCheckReferenceProtection() {
        // Given: 被RequirementUsage引用的RequirementDefinition
        String elementId = "req-def-001";
        
        ElementDTO referencingUsage = new ElementDTO();
        referencingUsage.setElementId("req-usage-001");
        referencingUsage.setEClass("RequirementUsage");
        referencingUsage.setProperty("of", elementId);
        
        List<ElementDTO> allUsages = Arrays.asList(referencingUsage);
        
        // When: 查询到引用该需求定义的使用
        when(universalElementService.queryElements("RequirementUsage")).thenReturn(allUsages);
        
        // Then: 应该抛出IllegalStateException
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            () -> requirementService.deleteRequirement(elementId));
        
        assertTrue(exception.getMessage().contains("Cannot delete requirement"));
        assertTrue(exception.getMessage().contains("referenced by 1 usages"));
        
        verify(universalElementService).queryElements("RequirementUsage");
        verify(universalElementService, never()).deleteElement(anyString());
    }
    
    /**
     * 验收标准：REQ-C2-4
     * 删除未被引用的RequirementDefinition应该成功
     */
    @Test
    @DisplayName("REQ-C2-4: 删除未被引用的需求定义")
    public void testDeleteRequirement_ShouldSucceedWhenNotReferenced() {
        // Given: 未被引用的RequirementDefinition
        String elementId = "req-def-001";
        
        ElementDTO otherUsage = new ElementDTO();
        otherUsage.setElementId("req-usage-002");
        otherUsage.setEClass("RequirementUsage");
        otherUsage.setProperty("of", "req-def-002"); // 引用其他需求定义
        
        List<ElementDTO> allUsages = Arrays.asList(otherUsage);
        
        // When: 没有找到引用该需求定义的使用
        when(universalElementService.queryElements("RequirementUsage")).thenReturn(allUsages);
        when(universalElementService.deleteElement(elementId)).thenReturn(true);
        
        boolean result = requirementService.deleteRequirement(elementId);
        
        // Then: 应该成功删除
        assertTrue(result);
        
        verify(universalElementService).queryElements("RequirementUsage");
        verify(universalElementService).deleteElement(elementId);
    }
    
    /**
     * 验收标准：REQ-B5-1
     * 查询所有RequirementDefinition应委托给UniversalElementService
     */
    @Test
    @DisplayName("REQ-B5-1: 查询所有需求定义")
    public void testGetRequirements_ShouldDelegateToUniversalService() {
        // Given: 需求定义列表
        ElementDTO req1 = new ElementDTO();
        req1.setElementId("req-def-001");
        req1.setEClass("RequirementDefinition");
        
        ElementDTO req2 = new ElementDTO();
        req2.setElementId("req-def-002");
        req2.setEClass("RequirementDefinition");
        
        List<ElementDTO> expectedRequirements = Arrays.asList(req1, req2);
        
        // When: 委托查询
        when(universalElementService.queryElements("RequirementDefinition")).thenReturn(expectedRequirements);
        
        List<ElementDTO> result = requirementService.getRequirements();
        
        // Then: 应该返回所有需求定义
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("req-def-001", result.get(0).getElementId());
        assertEquals("req-def-002", result.get(1).getElementId());
        
        verify(universalElementService).queryElements("RequirementDefinition");
    }
    
    /**
     * 验收标准：REQ-B5-1
     * 查询所有RequirementUsage应委托给UniversalElementService
     */
    @Test
    @DisplayName("REQ-B5-1: 查询所有需求使用")
    public void testGetRequirementUsages_ShouldDelegateToUniversalService() {
        // Given: 需求使用列表
        ElementDTO usage1 = new ElementDTO();
        usage1.setElementId("req-usage-001");
        usage1.setEClass("RequirementUsage");
        
        List<ElementDTO> expectedUsages = Arrays.asList(usage1);
        
        // When: 委托查询
        when(universalElementService.queryElements("RequirementUsage")).thenReturn(expectedUsages);
        
        List<ElementDTO> result = requirementService.getRequirementUsages();
        
        // Then: 应该返回所有需求使用
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("req-usage-001", result.get(0).getElementId());
        
        verify(universalElementService).queryElements("RequirementUsage");
    }
    
    /**
     * 验收标准：REQ-B5-1
     * 根据ID查找需求应委托给UniversalElementService
     */
    @Test
    @DisplayName("REQ-B5-1: 根据ID查找需求")
    public void testGetRequirementById_ShouldDelegateToUniversalService() {
        // Given: 需求ID和预期结果
        String elementId = "req-def-001";
        ElementDTO expectedRequirement = new ElementDTO();
        expectedRequirement.setElementId(elementId);
        expectedRequirement.setEClass("RequirementDefinition");
        
        // When: 委托查询
        when(universalElementService.findElementById(elementId)).thenReturn(expectedRequirement);
        
        ElementDTO result = requirementService.getRequirementById(elementId);
        
        // Then: 应该返回指定需求
        assertNotNull(result);
        assertEquals(elementId, result.getElementId());
        assertEquals("RequirementDefinition", result.getEClass());
        
        verify(universalElementService).findElementById(elementId);
    }
    
    /**
     * 验收标准：REQ-C1-4
     * 参数化文本渲染应替换${placeholder}占位符
     */
    @Test
    @DisplayName("REQ-C1-4: 参数化文本渲染")
    public void testRenderParametricText_ShouldReplacePlaceholders() {
        // Given: 包含占位符的需求
        String requirementId = "req-def-001";
        ElementDTO requirement = new ElementDTO();
        requirement.setElementId(requirementId);
        requirement.setEClass("RequirementDefinition");
        requirement.setProperty("documentation", "The ${subject} shall achieve ${performance} within ${window}.");
        
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("subject", "Engine");
        parameters.put("performance", "100kW");
        parameters.put("window", "10min");
        
        // When: 渲染参数化文本
        when(universalElementService.findElementById(requirementId)).thenReturn(requirement);
        
        String result = requirementService.renderParametricText(requirementId, parameters);
        
        // Then: 应该正确替换占位符
        assertEquals("The Engine shall achieve 100kW within 10min.", result);
        
        verify(universalElementService).findElementById(requirementId);
    }
    
    /**
     * 验收标准：REQ-C1-4
     * 需求不存在时渲染参数化文本应抛出异常
     */
    @Test
    @DisplayName("REQ-C1-4: 需求不存在时抛出异常")
    public void testRenderParametricText_ShouldThrowExceptionWhenRequirementNotFound() {
        // Given: 不存在的需求ID
        String requirementId = "non-existent-req";
        Map<String, Object> parameters = new HashMap<>();
        
        // When: 需求不存在
        when(universalElementService.findElementById(requirementId)).thenReturn(null);
        
        // Then: 应该抛出IllegalArgumentException
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> requirementService.renderParametricText(requirementId, parameters));
        
        assertTrue(exception.getMessage().contains("Requirement not found"));
        assertTrue(exception.getMessage().contains(requirementId));
        
        verify(universalElementService).findElementById(requirementId);
    }
}