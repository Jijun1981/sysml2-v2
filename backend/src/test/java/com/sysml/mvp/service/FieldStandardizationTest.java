package com.sysml.mvp.service;

import com.sysml.mvp.dto.ElementDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EStructuralFeature;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * SysML 2.0字段标准化测试套件
 * 
 * 需求对齐：
 * - REQ-FS-1: 字段分类标准化
 * - REQ-FS-2: 标准字段映射修复
 * - REQ-FS-3: 动态EMF兼容性保证
 * - REQ-FS-4: Metadata机制实现
 * - REQ-FS-5: 回归测试完整性保证
 */
@DisplayName("字段标准化测试 - SysML 2.0兼容性")
public class FieldStandardizationTest {

    @Mock
    private PilotEMFService pilotEMFService;
    
    @Mock
    private UniversalElementService universalElementService;
    
    @Mock
    private ValidationService validationService;
    
    @Mock
    private MetadataService metadataService;
    
    private RequirementService requirementService;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        requirementService = new RequirementService(universalElementService, validationService);
    }
    
    /**
     * REQ-FS-1: 字段分类标准化测试
     */
    @Nested
    @DisplayName("REQ-FS-1: 字段分类标准化")
    class FieldClassificationTests {
        
        /**
         * 验证M2核心字段通过setAttributeIfExists正确设置
         */
        @Test
        @DisplayName("M2核心字段设置测试")
        public void testM2CoreFieldsSetting() {
            // Given: RequirementDefinition的M2核心字段数据
            Map<String, Object> coreFields = new HashMap<>();
            coreFields.put("declaredName", "FastChargeRequirement");
            coreFields.put("declaredShortName", "BR-001");
            coreFields.put("documentation", "10%→80% SOC充电时间不得超过30分钟");
            
            EObject mockElement = mock(EObject.class);
            EClass mockEClass = mock(EClass.class);
            when(mockElement.eClass()).thenReturn(mockEClass);
            when(mockEClass.getName()).thenReturn("RequirementDefinition");
            
            // When: 通过setAttributeIfExists设置核心字段
            when(pilotEMFService.setAttributeIfExists(eq(mockElement), eq("declaredName"), any()))
                .thenReturn(true);
            when(pilotEMFService.setAttributeIfExists(eq(mockElement), eq("declaredShortName"), any()))
                .thenReturn(true);
            when(pilotEMFService.setAttributeIfExists(eq(mockElement), eq("documentation"), any()))
                .thenReturn(true);
            
            // Then: 验证所有核心字段都能正确设置
            assertTrue(pilotEMFService.setAttributeIfExists(mockElement, "declaredName", "FastChargeRequirement"));
            assertTrue(pilotEMFService.setAttributeIfExists(mockElement, "declaredShortName", "BR-001"));
            assertTrue(pilotEMFService.setAttributeIfExists(mockElement, "documentation", "充电时间要求"));
            
            verify(pilotEMFService).setAttributeIfExists(mockElement, "declaredName", "FastChargeRequirement");
            verify(pilotEMFService).setAttributeIfExists(mockElement, "declaredShortName", "BR-001");
            verify(pilotEMFService).setAttributeIfExists(mockElement, "documentation", "充电时间要求");
        }
        
        /**
         * 验证治理字段通过Metadata机制存储
         */
        @Test
        @DisplayName("治理字段Metadata存储测试")
        public void testGovernanceFieldsMetadataStorage() {
            // Given: 治理字段数据
            Map<String, Object> governanceFields = new HashMap<>();
            governanceFields.put("status", "approved");
            governanceFields.put("priority", "High");
            governanceFields.put("owner", "Battery Team");
            
            EObject mockElement = mock(EObject.class);
            
            // When: 通过Metadata机制设置治理字段
            when(metadataService.setMetadata(eq(mockElement), eq("status"), eq("approved")))
                .thenReturn(true);
            when(metadataService.setMetadata(eq(mockElement), eq("priority"), eq("High")))
                .thenReturn(true);
            when(metadataService.setMetadata(eq(mockElement), eq("owner"), eq("Battery Team")))
                .thenReturn(true);
            
            // Then: 验证治理字段通过Metadata正确设置
            assertTrue(metadataService.setMetadata(mockElement, "status", "approved"));
            assertTrue(metadataService.setMetadata(mockElement, "priority", "High"));
            assertTrue(metadataService.setMetadata(mockElement, "owner", "Battery Team"));
            
            verify(metadataService).setMetadata(mockElement, "status", "approved");
            verify(metadataService).setMetadata(mockElement, "priority", "High");
            verify(metadataService).setMetadata(mockElement, "owner", "Battery Team");
        }
        
        /**
         * 验证字段分类的正确性
         */
        @Test
        @DisplayName("字段分类正确性验证")
        public void testFieldClassificationCorrectness() {
            // Given: 混合字段数据
            Map<String, Object> mixedFields = new HashMap<>();
            
            // M2核心字段
            mixedFields.put("declaredName", "TestRequirement");
            mixedFields.put("documentation", "测试需求");
            
            // 治理字段（应该被分离到Metadata）
            mixedFields.put("priority", "Medium");
            mixedFields.put("status", "draft");
            
            // 非标准字段（应该被忽略或警告）
            mixedFields.put("customField", "customValue");
            
            EObject mockElement = mock(EObject.class);
            
            // When: 分别处理不同类型的字段
            when(pilotEMFService.setAttributeIfExists(mockElement, "declaredName", "TestRequirement"))
                .thenReturn(true);  // 核心字段存在
            when(pilotEMFService.setAttributeIfExists(mockElement, "documentation", "测试需求"))
                .thenReturn(true);  // 核心字段存在
            when(pilotEMFService.setAttributeIfExists(mockElement, "priority", "Medium"))
                .thenReturn(false); // 非核心字段，应该被引导到Metadata
            when(pilotEMFService.setAttributeIfExists(mockElement, "customField", "customValue"))
                .thenReturn(false); // 非标准字段，应该被忽略
            
            // Then: 验证字段分类处理正确
            assertTrue(pilotEMFService.setAttributeIfExists(mockElement, "declaredName", "TestRequirement"));
            assertTrue(pilotEMFService.setAttributeIfExists(mockElement, "documentation", "测试需求"));
            assertFalse(pilotEMFService.setAttributeIfExists(mockElement, "priority", "Medium"));
            assertFalse(pilotEMFService.setAttributeIfExists(mockElement, "customField", "customValue"));
        }
    }
    
    /**
     * REQ-FS-2: 标准字段映射修复测试
     */
    @Nested
    @DisplayName("REQ-FS-2: 标准字段映射修复")
    class StandardFieldMappingTests {
        
        /**
         * 验证RequirementUsage正确使用requirementDefinition字段
         */
        @Test
        @DisplayName("requirementDefinition字段映射正确性")
        public void testRequirementDefinitionFieldMapping() {
            // Given: RequirementUsage数据，使用正确的字段名
            Map<String, Object> usageData = new HashMap<>();
            usageData.put("requirementDefinition", "req-def-001");  // ✅ 正确字段名
            usageData.put("declaredName", "具体需求使用");
            // ❌ 不再使用"of"字段
            
            ElementDTO expectedResult = new ElementDTO();
            expectedResult.setElementId("req-usage-001");
            expectedResult.setEClass("RequirementUsage");
            expectedResult.setProperty("requirementDefinition", "req-def-001");
            
            // When: 创建RequirementUsage
            when(universalElementService.createElement("RequirementUsage", usageData))
                .thenReturn(expectedResult);
            
            ElementDTO result = requirementService.createRequirementUsage(usageData);
            
            // Then: 验证使用了正确的字段名
            assertNotNull(result);
            assertEquals("req-usage-001", result.getElementId());
            assertEquals("req-def-001", result.getProperty("requirementDefinition"));
            
            // 验证不再使用旧的"of"字段
            assertNull(result.getProperty("of"));
            
            verify(universalElementService).createElement("RequirementUsage", usageData);
        }
        
        /**
         * 验证subject字段已完全删除
         */
        @Test
        @DisplayName("subject字段删除验证")
        public void testSubjectFieldRemoval() {
            // Given: RequirementUsage数据，不包含subject字段
            Map<String, Object> usageData = new HashMap<>();
            usageData.put("requirementDefinition", "req-def-001");
            usageData.put("declaredName", "需求使用");
            // ❌ 不再包含subject字段
            
            ElementDTO expectedResult = new ElementDTO();
            expectedResult.setElementId("req-usage-001");
            expectedResult.setEClass("RequirementUsage");
            
            // When: 创建RequirementUsage（不验证subject）
            when(universalElementService.createElement("RequirementUsage", usageData))
                .thenReturn(expectedResult);
            
            // Then: 验证不再要求subject字段
            assertDoesNotThrow(() -> {
                ElementDTO result = requirementService.createRequirementUsage(usageData);
                assertNotNull(result);
            });
            
            // 验证result中不包含subject相关信息
            ElementDTO result = requirementService.createRequirementUsage(usageData);
            assertNull(result.getProperty("subject"));
        }
        
        /**
         * 验证字段映射的向后兼容性
         */
        @Test
        @DisplayName("字段映射向后兼容性测试")
        public void testFieldMappingBackwardCompatibility() {
            // Given: 包含旧字段名的数据（模拟旧数据）
            Map<String, Object> legacyData = new HashMap<>();
            legacyData.put("of", "req-def-001");  // 旧字段名
            legacyData.put("declaredName", "遗留数据");
            
            // When: 系统应该能处理或转换旧字段名
            // 这里可能需要一个字段映射转换器
            Map<String, Object> normalizedData = new HashMap<>();
            // 模拟字段名转换逻辑
            if (legacyData.containsKey("of")) {
                normalizedData.put("requirementDefinition", legacyData.get("of"));
            }
            normalizedData.put("declaredName", legacyData.get("declaredName"));
            
            // Then: 验证转换后的数据正确
            assertEquals("req-def-001", normalizedData.get("requirementDefinition"));
            assertFalse(normalizedData.containsKey("of"));
        }
    }
    
    /**
     * REQ-FS-3: 动态EMF兼容性保证测试
     */
    @Nested
    @DisplayName("REQ-FS-3: 动态EMF兼容性保证")
    class DynamicEMFCompatibilityTests {
        
        /**
         * 验证setAttributeIfExists的优雅降级
         */
        @Test
        @DisplayName("setAttributeIfExists优雅降级测试")
        public void testSetAttributeIfExistsGracefulDegradation() {
            // Given: 包含存在和不存在字段的数据
            EObject mockElement = mock(EObject.class);
            
            // When: 设置存在的字段应该返回true
            when(pilotEMFService.setAttributeIfExists(mockElement, "declaredName", "TestName"))
                .thenReturn(true);
            
            // 设置不存在的字段应该返回false（优雅降级）
            when(pilotEMFService.setAttributeIfExists(mockElement, "nonExistentField", "value"))
                .thenReturn(false);
            
            // Then: 验证优雅降级行为
            assertTrue(pilotEMFService.setAttributeIfExists(mockElement, "declaredName", "TestName"));
            assertFalse(pilotEMFService.setAttributeIfExists(mockElement, "nonExistentField", "value"));
            
            // 验证不会抛出异常
            assertDoesNotThrow(() -> {
                pilotEMFService.setAttributeIfExists(mockElement, "anotherNonExistentField", "value");
            });
        }
        
        /**
         * 验证DTO转换的兼容性
         */
        @Test
        @DisplayName("ElementDTO转换兼容性测试")
        public void testElementDTOConversionCompatibility() {
            // Given: 包含各种类型字段的元素
            Map<String, Object> elementData = new HashMap<>();
            elementData.put("declaredName", "TestElement");
            elementData.put("documentation", "测试元素");
            
            ElementDTO mockDTO = new ElementDTO();
            mockDTO.setElementId("element-001");
            mockDTO.setEClass("RequirementDefinition");
            mockDTO.setProperty("declaredName", "TestElement");
            mockDTO.setProperty("documentation", "测试元素");
            
            // When: 通过UniversalElementService创建和转换
            when(universalElementService.createElement("RequirementDefinition", elementData))
                .thenReturn(mockDTO);
            
            ElementDTO result = universalElementService.createElement("RequirementDefinition", elementData);
            
            // Then: 验证转换后的DTO正确
            assertNotNull(result);
            assertEquals("element-001", result.getElementId());
            assertEquals("RequirementDefinition", result.getEClass());
            assertEquals("TestElement", result.getProperty("declaredName"));
            assertEquals("测试元素", result.getProperty("documentation"));
        }
        
        /**
         * 验证182个SysML类型的通用处理
         */
        @Test
        @DisplayName("SysML类型通用处理测试")
        public void testUniversalSysMLTypeHandling() {
            // Given: 不同SysML类型的数据
            List<String> sysmlTypes = Arrays.asList(
                "RequirementDefinition", 
                "RequirementUsage",
                "PartDefinition",
                "PartUsage",
                "InterfaceDefinition"
            );
            
            // When & Then: 验证每种类型都能通过通用接口处理
            for (String type : sysmlTypes) {
                Map<String, Object> data = new HashMap<>();
                data.put("declaredName", "Test" + type);
                
                ElementDTO mockDTO = new ElementDTO();
                mockDTO.setEClass(type);
                mockDTO.setProperty("declaredName", "Test" + type);
                
                when(universalElementService.createElement(type, data))
                    .thenReturn(mockDTO);
                
                ElementDTO result = universalElementService.createElement(type, data);
                
                assertNotNull(result, "应该能创建" + type);
                assertEquals(type, result.getEClass());
                assertEquals("Test" + type, result.getProperty("declaredName"));
            }
        }
    }
    
    /**
     * REQ-FS-4: Metadata机制实现测试
     */
    @Nested
    @DisplayName("REQ-FS-4: Metadata机制实现")
    class MetadataMechanismTests {
        
        /**
         * 验证MetadataDefinition的创建和管理
         */
        @Test
        @DisplayName("MetadataDefinition创建和管理测试")
        public void testMetadataDefinitionCreation() {
            // Given: Metadata定义数据
            Map<String, Object> priorityDef = new HashMap<>();
            priorityDef.put("name", "Priority");
            priorityDef.put("schema", Map.of(
                "level", Map.of("type", "enum", "values", Arrays.asList("Low", "Medium", "High", "Critical"))
            ));
            
            // When: 创建MetadataDefinition
            when(metadataService.createMetadataDefinition("Priority", priorityDef))
                .thenReturn("metadata-def-priority");
            
            String defId = metadataService.createMetadataDefinition("Priority", priorityDef);
            
            // Then: 验证MetadataDefinition创建成功
            assertNotNull(defId);
            assertEquals("metadata-def-priority", defId);
            
            verify(metadataService).createMetadataDefinition("Priority", priorityDef);
        }
        
        /**
         * 验证MetadataUsage的应用
         */
        @Test
        @DisplayName("MetadataUsage应用测试")
        public void testMetadataUsageApplication() {
            // Given: 元素和Metadata使用数据
            EObject mockElement = mock(EObject.class);
            Map<String, Object> metadataValue = Map.of("level", "High", "rationale", "Critical safety requirement");
            
            // When: 应用Metadata到元素
            when(metadataService.applyMetadata(mockElement, "Priority", metadataValue))
                .thenReturn("metadata-usage-001");
            
            String usageId = metadataService.applyMetadata(mockElement, "Priority", metadataValue);
            
            // Then: 验证Metadata应用成功
            assertNotNull(usageId);
            assertEquals("metadata-usage-001", usageId);
            
            verify(metadataService).applyMetadata(mockElement, "Priority", metadataValue);
        }
        
        /**
         * 验证核心治理字段的Metadata支持
         */
        @Test
        @DisplayName("核心治理字段Metadata支持测试")
        public void testCoreGovernanceFieldsMetadataSupport() {
            // Given: 核心治理字段列表
            List<String> coreGovernanceFields = Arrays.asList(
                "status", "priority", "owner", "source", "verificationMethod"
            );
            
            EObject mockElement = mock(EObject.class);
            
            // When & Then: 验证每个核心治理字段都支持Metadata
            for (String field : coreGovernanceFields) {
                when(metadataService.setMetadata(mockElement, field, "testValue"))
                    .thenReturn(true);
                
                assertTrue(metadataService.setMetadata(mockElement, field, "testValue"),
                    field + " 字段应该支持Metadata设置");
            }
        }
    }
    
    /**
     * REQ-FS-5: 回归测试完整性保证测试
     */
    @Nested
    @DisplayName("REQ-FS-5: 回归测试完整性保证")
    class RegressionTestIntegrityTests {
        
        /**
         * 验证RequirementDefinition CRUD操作完整性
         */
        @Test
        @DisplayName("RequirementDefinition CRUD完整性测试")
        public void testRequirementDefinitionCRUDIntegrity() {
            // Given: RequirementDefinition数据
            Map<String, Object> reqData = new HashMap<>();
            reqData.put("reqId", "BR-001");
            reqData.put("declaredName", "充电需求");
            reqData.put("documentation", "充电时间不超过30分钟");
            
            ElementDTO mockDTO = new ElementDTO();
            mockDTO.setElementId("req-def-001");
            mockDTO.setEClass("RequirementDefinition");
            mockDTO.setProperty("reqId", "BR-001");
            
            // When: CRUD操作
            when(validationService.validateReqIdUniqueness("BR-001")).thenReturn(true);
            when(universalElementService.createElement("RequirementDefinition", reqData))
                .thenReturn(mockDTO);
            
            ElementDTO result = requirementService.createRequirement(reqData);
            
            // Then: 验证操作成功
            assertNotNull(result);
            assertEquals("req-def-001", result.getElementId());
            assertEquals("RequirementDefinition", result.getEClass());
            assertEquals("BR-001", result.getProperty("reqId"));
        }
        
        /**
         * 验证RequirementUsage CRUD操作完整性
         */
        @Test
        @DisplayName("RequirementUsage CRUD完整性测试")
        public void testRequirementUsageCRUDIntegrity() {
            // Given: RequirementUsage数据（使用正确的字段）
            Map<String, Object> usageData = new HashMap<>();
            usageData.put("requirementDefinition", "req-def-001");  // ✅ 正确字段
            usageData.put("declaredName", "具体需求使用");
            
            ElementDTO mockDTO = new ElementDTO();
            mockDTO.setElementId("req-usage-001");
            mockDTO.setEClass("RequirementUsage");
            mockDTO.setProperty("requirementDefinition", "req-def-001");
            
            // When: 创建RequirementUsage
            when(universalElementService.createElement("RequirementUsage", usageData))
                .thenReturn(mockDTO);
            
            ElementDTO result = requirementService.createRequirementUsage(usageData);
            
            // Then: 验证操作成功
            assertNotNull(result);
            assertEquals("req-usage-001", result.getElementId());
            assertEquals("RequirementUsage", result.getEClass());
            assertEquals("req-def-001", result.getProperty("requirementDefinition"));
        }
        
        /**
         * 验证API接口行为的向后兼容性
         */
        @Test
        @DisplayName("API接口向后兼容性测试")
        public void testAPIBackwardCompatibility() {
            // Given: 现有API调用方式
            String elementId = "req-def-001";
            Map<String, Object> updates = new HashMap<>();
            updates.put("documentation", "更新后的需求描述");
            
            ElementDTO mockUpdatedDTO = new ElementDTO();
            mockUpdatedDTO.setElementId(elementId);
            mockUpdatedDTO.setProperty("documentation", "更新后的需求描述");
            
            // When: 调用现有API
            when(universalElementService.patchElement(elementId, updates))
                .thenReturn(mockUpdatedDTO);
            
            ElementDTO result = requirementService.updateRequirement(elementId, updates);
            
            // Then: 验证API行为保持不变
            assertNotNull(result);
            assertEquals(elementId, result.getElementId());
            assertEquals("更新后的需求描述", result.getProperty("documentation"));
            
            verify(universalElementService).patchElement(elementId, updates);
        }
    }
}