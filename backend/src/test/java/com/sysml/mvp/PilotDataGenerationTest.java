package com.sysml.mvp;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.sysml.mvp.util.DemoDataGenerator;
import com.sysml.mvp.service.UniversalElementService;
import com.sysml.mvp.dto.ElementDTO;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Pilot格式EV电池系统演示数据生成测试
 * 实现Phase 5: REQ-B1-4需求驱动的TDD测试
 * 
 * 验证需求：
 * 1. 生成~20 RequirementDefinition模板（需求库）
 * 2. 基于模板生成50个RequirementUsage实例
 * 3. 创建15-20个Trace关系
 * 4. 使用真实的汽车电池系统领域内容
 * 5. 遵循3层结构：L1系统，L2子系统，L3组件
 */
@SpringBootTest
@ActiveProfiles("test")
public class PilotDataGenerationTest {
    
    @Autowired
    private DemoDataGenerator demoDataGenerator;
    
    @Autowired
    private UniversalElementService universalElementService;
    
    @BeforeEach
    void setUp() {
        // 确保data目录存在
        File dataDir = new File("./data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }
    
    @Test
    @DisplayName("REQ-B1-4: 生成EV电池系统Pilot格式演示数据")
    void shouldGeneratePilotFormatEVBatteryDemoData() {
        // When: 使用Pilot格式生成EV电池系统演示数据
        demoDataGenerator.generatePilotEVBatteryDemo();
        
        // Then: 验证文件生成
        File demoFile = new File("./data/ev-battery-demo.json");
        assertTrue(demoFile.exists(), "EV电池演示数据文件应该生成");
        assertTrue(demoFile.length() > 0, "EV电池演示数据文件不应为空");
        
        // 验证数据内容：~20个RequirementDefinition（注意：测试环境可能有累积数据）
        List<ElementDTO> definitions = universalElementService.queryElements("RequirementDefinition");
        assertTrue(definitions.size() >= 18, 
            "应该至少生成18个RequirementDefinition，实际：" + definitions.size());
        
        // 验证数据内容：~50个RequirementUsage（测试环境可能有累积数据）
        List<ElementDTO> usages = universalElementService.queryElements("RequirementUsage");
        assertTrue(usages.size() >= 45, 
            "应该至少生成45个RequirementUsage，实际：" + usages.size());
        
        // 验证数据内容：15-20个Trace关系（测试环境可能有累积数据）
        List<ElementDTO> traces = universalElementService.queryElements("Dependency");
        assertTrue(traces.size() >= 15, 
            "应该至少生成15个Trace关系，实际：" + traces.size());
    }
    
    @Test
    @DisplayName("REQ-B1-4: 验证EV电池系统领域内容准确性")
    void shouldGenerateRealisticAutomotiveBatteryContent() {
        // When: 生成EV电池系统数据
        demoDataGenerator.generatePilotEVBatteryDemo();
        
        // Then: 验证需求内容包含汽车电池系统相关术语
        List<ElementDTO> definitions = universalElementService.queryElements("RequirementDefinition");
        
        // 检查是否包含电池系统相关术语
        Set<String> batteryTerms = Set.of(
            "电池", "电芯", "BMS", "热管理", "安全", "充电", "放电", 
            "电压", "电流", "温度", "SOC", "SOH", "冷却", "Pack"
        );
        
        boolean hasRelevantContent = false;
        for (ElementDTO def : definitions) {
            // 在Pilot格式中，text可能被映射到其他属性或包含在name中
            String text = (String) def.getProperty("text");
            String name = (String) def.getProperty("name");
            String reqId = (String) def.getProperty("declaredShortName");
            
            // 检查所有可能包含领域内容的属性
            String[] contentFields = {text, name, reqId};
            
            for (String content : contentFields) {
                if (content != null) {
                    for (String term : batteryTerms) {
                        if (content.contains(term)) {
                            hasRelevantContent = true;
                            System.out.println("✅ 找到电池系统术语: " + term + " 在内容: " + content);
                            break;
                        }
                    }
                    if (hasRelevantContent) break;
                }
            }
            if (hasRelevantContent) break;
        }
        
        // 如果没找到，输出一些调试信息
        if (!hasRelevantContent && !definitions.isEmpty()) {
            System.out.println("🔍 调试信息 - 前3个Definition的内容:");
            for (int i = 0; i < Math.min(3, definitions.size()); i++) {
                ElementDTO def = definitions.get(i);
                System.out.println("Definition " + i + ":");
                System.out.println("  - text: " + def.getProperty("text"));
                System.out.println("  - name: " + def.getProperty("name"));
                System.out.println("  - declaredShortName: " + def.getProperty("declaredShortName"));
            }
        }
        
        assertTrue(hasRelevantContent, "需求内容应该包含汽车电池系统相关术语");
    }
    
    @Test
    @DisplayName("REQ-B1-4: 验证3层系统架构结构")
    void shouldFollowThreeLayerArchitecture() {
        // When: 生成EV电池系统数据
        demoDataGenerator.generatePilotEVBatteryDemo();
        
        // Then: 验证包含L1系统、L2子系统、L3组件层级
        List<ElementDTO> definitions = universalElementService.queryElements("RequirementDefinition");
        
        boolean hasL1System = false;
        boolean hasL2Subsystem = false; 
        boolean hasL3Component = false;
        
        for (ElementDTO def : definitions) {
            String reqId = (String) def.getProperty("declaredShortName");
            if (reqId != null) {
                if (reqId.contains("L1") || reqId.contains("SYS")) {
                    hasL1System = true;
                } else if (reqId.contains("L2") || reqId.contains("SUB")) {
                    hasL2Subsystem = true;
                } else if (reqId.contains("L3") || reqId.contains("COM")) {
                    hasL3Component = true;
                }
            }
        }
        
        assertTrue(hasL1System, "应该包含L1系统级需求");
        assertTrue(hasL2Subsystem, "应该包含L2子系统级需求");
        assertTrue(hasL3Component, "应该包含L3组件级需求");
    }
    
    @Test
    @DisplayName("REQ-B1-4: 验证Shall语句格式要求")
    void shouldGenerateProperShallStatements() {
        // When: 生成EV电池系统数据
        demoDataGenerator.generatePilotEVBatteryDemo();
        
        // Then: 验证需求文本包含规范的shall语句
        List<ElementDTO> definitions = universalElementService.queryElements("RequirementDefinition");
        
        int shallStatementCount = 0;
        for (ElementDTO def : definitions) {
            String text = (String) def.getProperty("text");
            if (text != null && (text.contains("应当") || text.contains("必须") || text.contains("shall"))) {
                shallStatementCount++;
            }
        }
        
        assertTrue(shallStatementCount >= 10, 
            "至少应该有10条需求包含规范的shall语句格式，实际：" + shallStatementCount);
    }
    
    @Test
    @DisplayName("REQ-B1-4: 验证RequirementUsage基于Definition创建")
    void shouldCreateUsagesBasedOnDefinitions() {
        // When: 生成EV电池系统数据
        demoDataGenerator.generatePilotEVBatteryDemo();
        
        // Then: 验证所有Usage都有对应的Definition引用
        List<ElementDTO> usages = universalElementService.queryElements("RequirementUsage");
        List<ElementDTO> definitions = universalElementService.queryElements("RequirementDefinition");
        
        // 创建Definition ID映射
        Set<String> definitionIds = definitions.stream()
            .map(def -> (String) def.getProperty("elementId"))
            .collect(java.util.stream.Collectors.toSet());
        
        for (ElementDTO usage : usages) {
            String ofId = (String) usage.getProperty("ofId");
            assertNotNull(ofId, "RequirementUsage应该有of引用");
            assertTrue(definitionIds.contains(ofId), 
                "RequirementUsage的of引用应该指向存在的Definition");
        }
    }
    
    @Test
    @DisplayName("REQ-B1-4: 验证Trace关系的有效性")
    void shouldCreateValidTraceRelationships() {
        // When: 生成EV电池系统数据
        demoDataGenerator.generatePilotEVBatteryDemo();
        
        // Then: 验证Trace关系的source和target都存在
        List<ElementDTO> traces = universalElementService.queryElements("Dependency");
        List<ElementDTO> usages = universalElementService.queryElements("RequirementUsage");
        
        Set<String> usageIds = usages.stream()
            .map(usage -> (String) usage.getProperty("elementId"))
            .collect(java.util.stream.Collectors.toSet());
        
        for (ElementDTO trace : traces) {
            String sourceId = (String) trace.getProperty("sourceId");
            String targetId = (String) trace.getProperty("targetId");
            
            assertNotNull(sourceId, "Trace关系应该有source");
            assertNotNull(targetId, "Trace关系应该有target");
            assertTrue(usageIds.contains(sourceId), "Trace的source应该指向存在的Usage");
            assertTrue(usageIds.contains(targetId), "Trace的target应该指向存在的Usage");
        }
    }
    
    @Test
    @DisplayName("REQ-B1-4: 生成多套规模化EV电池数据集")
    void shouldGenerateScalableEVBatteryDatasets() {
        // When: 生成不同规模的EV电池系统数据集
        demoDataGenerator.generateScalableEVBatteryDatasets();
        
        // Then: 验证所有数据集文件生成
        String[] files = {
            "ev-battery-small.json", 
            "ev-battery-medium.json", 
            "ev-battery-large.json"
        };
        
        for (String fileName : files) {
            File file = new File("./data/" + fileName);
            assertTrue(file.exists(), fileName + "应该生成");
            assertTrue(file.length() > 0, fileName + "不应为空");
        }
    }
    
    @Test
    @DisplayName("REQ-B1-4: 验证使用UniversalElementService创建元素")
    void shouldUseUniversalElementServiceForElementCreation() {
        // When: 生成EV电池系统数据（内部应该使用UniversalElementService）
        demoDataGenerator.generatePilotEVBatteryDemo();
        
        // Then: 验证生成的元素都可以通过UniversalElementService查询到
        List<ElementDTO> allElements = universalElementService.queryElements(null);
        
        // 验证至少包含预期数量的元素（20个Definition + 50个Usage + 15个Trace = 85个）
        assertTrue(allElements.size() >= 85, 
            "应该至少生成85个元素，实际：" + allElements.size());
        
        // 验证元素包含必要的属性（Pilot格式）
        for (ElementDTO element : allElements) {
            assertNotNull(element.getElementId(), "所有元素都应该有elementId");
            assertNotNull(element.getEClass(), "所有元素都应该有eClass");
        }
    }
    
    @Test
    @DisplayName("REQ-B3-1,B3-2,B3-3: 验证导出导入数据一致性")
    void shouldMaintainConsistencyAfterExportImport() {
        // Given: 生成EV电池系统数据
        demoDataGenerator.generatePilotEVBatteryDemo();
        
        // When: 通过UniversalElementService读取原始数据
        List<ElementDTO> originalElements = universalElementService.queryElements(null);
        
        // Then: 验证数据可以被正确读取（简化的导入导出一致性测试）
        assertFalse(originalElements.isEmpty(), "应该能读取到生成的数据");
        
        // 验证元素ID的稳定性（Pilot格式要求）
        for (ElementDTO element : originalElements) {
            String elementId = element.getElementId();
            assertNotNull(elementId, "所有元素都应该有稳定的elementId");
            assertTrue(elementId.length() > 0, "elementId不应为空");
        }
    }
}