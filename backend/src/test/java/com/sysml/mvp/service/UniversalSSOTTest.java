package com.sysml.mvp.service;

import com.sysml.mvp.model.EMFModelRegistry;
import com.sysml.mvp.repository.FileModelRepository;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD测试：REQ-A1-1 数据源唯一（SSOT）
 * 验证通用接口能够访问和管理所有元素类型
 */
@SpringBootTest
@DisplayName("REQ-A1-1: SSOT通用接口测试")
public class UniversalSSOTTest {
    
    @Autowired
    private UniversalElementService service;
    
    @Autowired
    private FileModelRepository repository;
    
    private final String TEST_PROJECT = "test-ssot";
    
    @BeforeEach
    void setUp() {
        // 清理测试项目
        repository.clearCache(TEST_PROJECT);
    }
    
    @Test
    @DisplayName("验证通用接口可以创建和查询RequirementDefinition")
    void testUniversalAPI_RequirementDefinition() {
        // Given: 准备RequirementDefinition数据
        Map<String, Object> reqDef = new HashMap<>();
        reqDef.put("eClass", "RequirementDefinition");
        reqDef.put("declaredName", "电池温度监控需求");
        reqDef.put("declaredShortName", "REQ-BAT-001");
        reqDef.put("text", "系统应监控电池温度");
        
        // When: 通过通用接口创建
        Map<String, Object> created = service.createElement(TEST_PROJECT, reqDef);
        
        // Then: 验证创建成功
        assertNotNull(created);
        assertEquals("RequirementDefinition", created.get("eClass"));
        assertNotNull(created.get("elementId"));
        assertEquals("电池温度监控需求", created.get("declaredName"));
        
        // And: 验证可以查询到
        List<Map<String, Object>> results = service.queryElements(
            TEST_PROJECT, "RequirementDefinition", new HashMap<>()
        );
        assertEquals(1, results.size());
        assertEquals(created.get("elementId"), results.get(0).get("elementId"));
    }
    
    @Test
    @DisplayName("验证通用接口可以创建和查询RequirementUsage")
    void testUniversalAPI_RequirementUsage() {
        // Given: 先创建Definition
        Map<String, Object> reqDef = new HashMap<>();
        reqDef.put("eClass", "RequirementDefinition");
        reqDef.put("declaredName", "温度监控模板");
        Map<String, Object> def = service.createElement(TEST_PROJECT, reqDef);
        String defId = (String) def.get("elementId");
        
        // When: 创建Usage引用Definition
        Map<String, Object> reqUsage = new HashMap<>();
        reqUsage.put("eClass", "RequirementUsage");
        reqUsage.put("declaredName", "BMS温度监控实例");
        reqUsage.put("of", defId);  // 引用Definition
        reqUsage.put("status", "active");
        
        Map<String, Object> usage = service.createElement(TEST_PROJECT, reqUsage);
        
        // Then: 验证Usage创建成功
        assertNotNull(usage);
        assertEquals("RequirementUsage", usage.get("eClass"));
        assertEquals(defId, usage.get("of"));
        
        // And: 验证可以查询到
        List<Map<String, Object>> usages = service.queryElements(
            TEST_PROJECT, "RequirementUsage", new HashMap<>()
        );
        assertEquals(1, usages.size());
    }
    
    @Test
    @DisplayName("验证通用接口可以创建和查询PartUsage")
    void testUniversalAPI_PartUsage() {
        // Given: 准备PartUsage数据
        Map<String, Object> partUsage = new HashMap<>();
        partUsage.put("eClass", "PartUsage");
        partUsage.put("declaredName", "电池管理系统");
        partUsage.put("declaredShortName", "BMS");
        
        // When: 通过通用接口创建
        Map<String, Object> created = service.createElement(TEST_PROJECT, partUsage);
        
        // Then: 验证创建成功
        assertNotNull(created);
        assertEquals("PartUsage", created.get("eClass"));
        assertEquals("电池管理系统", created.get("declaredName"));
        
        // And: 验证可以查询到
        List<Map<String, Object>> results = service.queryElements(
            TEST_PROJECT, "PartUsage", new HashMap<>()
        );
        assertEquals(1, results.size());
    }
    
    @Test
    @DisplayName("验证通用接口可以创建追溯关系（Satisfy）")
    void testUniversalAPI_TraceRelations() {
        // Given: 创建源和目标
        Map<String, Object> source = service.createElement(TEST_PROJECT, Map.of(
            "eClass", "RequirementUsage",
            "declaredName", "实现需求"
        ));
        Map<String, Object> target = service.createElement(TEST_PROJECT, Map.of(
            "eClass", "RequirementUsage",
            "declaredName", "原始需求"
        ));
        
        // When: 创建Satisfy关系
        Map<String, Object> satisfy = new HashMap<>();
        satisfy.put("eClass", "Satisfy");
        satisfy.put("source", source.get("elementId"));
        satisfy.put("target", target.get("elementId"));
        
        Map<String, Object> trace = service.createElement(TEST_PROJECT, satisfy);
        
        // Then: 验证关系创建成功
        assertNotNull(trace);
        assertEquals("Satisfy", trace.get("eClass"));
        assertEquals(source.get("elementId"), trace.get("source"));
        assertEquals(target.get("elementId"), trace.get("target"));
    }
    
    @Test
    @DisplayName("验证通用接口支持所有查询不限类型")
    void testUniversalAPI_QueryAll() {
        // Given: 创建多种类型的元素
        service.createElement(TEST_PROJECT, Map.of(
            "eClass", "RequirementDefinition",
            "declaredName", "需求定义1"
        ));
        service.createElement(TEST_PROJECT, Map.of(
            "eClass", "RequirementUsage",
            "declaredName", "需求使用1"
        ));
        service.createElement(TEST_PROJECT, Map.of(
            "eClass", "PartUsage",
            "declaredName", "部件使用1"
        ));
        
        // When: 查询所有元素（不指定类型）
        List<Map<String, Object>> all = service.queryElements(
            TEST_PROJECT, null, new HashMap<>()
        );
        
        // Then: 验证返回所有元素
        assertEquals(3, all.size());
        
        // And: 验证包含所有类型
        Set<String> eClasses = new HashSet<>();
        all.forEach(el -> eClasses.add((String) el.get("eClass")));
        assertTrue(eClasses.contains("RequirementDefinition"));
        assertTrue(eClasses.contains("RequirementUsage"));
        assertTrue(eClasses.contains("PartUsage"));
    }
    
    @Test
    @DisplayName("验证Demo数据可以通过通用接口访问")
    void testUniversalAPI_AccessDemoData() {
        // Given: 加载默认项目（包含demo数据）
        String DEFAULT_PROJECT = "default";
        
        // When: 查询所有元素
        List<Map<String, Object>> all = service.queryElements(
            DEFAULT_PROJECT, null, new HashMap<>()
        );
        
        // Then: 验证能访问到数据
        assertNotNull(all);
        assertTrue(all.size() > 0, "应该能访问到demo数据");
        
        // And: 验证数据包含必要字段
        for (Map<String, Object> element : all) {
            assertNotNull(element.get("eClass"), "每个元素都应该有eClass");
            assertNotNull(element.get("elementId"), "每个元素都应该有elementId");
        }
        
        // And: 打印数据统计
        Map<String, Integer> stats = new HashMap<>();
        all.forEach(el -> {
            String eClass = (String) el.get("eClass");
            stats.put(eClass, stats.getOrDefault(eClass, 0) + 1);
        });
        System.out.println("Demo数据统计:");
        stats.forEach((k, v) -> System.out.println("  " + k + ": " + v));
    }
    
    @Test
    @DisplayName("REQ-A1-1: 验证SSOT - 更新后所有视图看到同步变更")
    void testSSOT_UpdateSynchronization() {
        // Given: 创建一个元素
        Map<String, Object> element = service.createElement(TEST_PROJECT, Map.of(
            "eClass", "RequirementDefinition",
            "declaredName", "原始名称"
        ));
        String elementId = (String) element.get("elementId");
        
        // When: 更新元素
        Map<String, Object> updates = Map.of(
            "declaredName", "更新后的名称",
            "status", "updated"
        );
        Map<String, Object> updated = service.updateElement(TEST_PROJECT, elementId, updates);
        
        // Then: 验证更新成功
        assertEquals("更新后的名称", updated.get("declaredName"));
        assertEquals("updated", updated.get("status"));
        
        // And: 验证查询返回更新后的数据（模拟其他视图）
        Map<String, Object> queried = service.getElementById(TEST_PROJECT, elementId);
        assertEquals("更新后的名称", queried.get("declaredName"));
        assertEquals("updated", queried.get("status"));
        
        // And: 验证列表查询也看到更新
        List<Map<String, Object>> list = service.queryElements(
            TEST_PROJECT, "RequirementDefinition", new HashMap<>()
        );
        assertEquals(1, list.size());
        assertEquals("更新后的名称", list.get(0).get("declaredName"));
    }
}