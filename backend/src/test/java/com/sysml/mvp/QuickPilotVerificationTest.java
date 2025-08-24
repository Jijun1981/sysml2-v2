package com.sysml.mvp;

import com.sysml.mvp.model.EMFModelRegistry;
import com.sysml.mvp.service.PilotEMFService;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 快速验证Pilot元模型集成
 * 用于快速测试Phase 2实现是否正常工作
 */
@SpringBootTest
public class QuickPilotVerificationTest {
    
    @Autowired
    private EMFModelRegistry modelRegistry;
    
    @Autowired
    private PilotEMFService pilotEMFService;
    
    @Test
    public void quickVerifyPilotIntegration() {
        // 1. 验证Pilot元模型加载
        EPackage sysmlPackage = modelRegistry.getSysMLPackage();
        assertNotNull(sysmlPackage, "Pilot元模型必须成功加载");
        assertEquals("https://www.omg.org/spec/SysML/20250201", 
                    sysmlPackage.getNsURI(), 
                    "必须是Pilot标准命名空间");
        
        // 2. 验证能获取RequirementDefinition类
        EClass reqDefClass = (EClass) sysmlPackage.getEClassifier("RequirementDefinition");
        assertNotNull(reqDefClass, "必须能获取RequirementDefinition类");
        
        // 3. 验证能创建RequirementDefinition对象
        EObject reqDef = pilotEMFService.createRequirementDefinition(
            "REQ-TEST-001",
            "测试需求",
            "这是一个测试需求的描述"
        );
        assertNotNull(reqDef, "必须能创建RequirementDefinition对象");
        assertEquals("RequirementDefinition", reqDef.eClass().getName());
        
        // 4. 验证字段映射
        Object shortName = pilotEMFService.getAttributeValue(reqDef, "declaredShortName");
        assertEquals("REQ-TEST-001", shortName, "reqId应该映射到declaredShortName");
        
        Object name = pilotEMFService.getAttributeValue(reqDef, "declaredName");
        assertEquals("测试需求", name, "name应该映射到declaredName");
        
        // 5. 验证能创建RequirementUsage
        EObject reqUsage = pilotEMFService.createRequirementUsage(
            "def-123",
            "测试Usage",
            "Usage描述",
            "active"
        );
        assertNotNull(reqUsage, "必须能创建RequirementUsage对象");
        assertEquals("RequirementUsage", reqUsage.eClass().getName());
        
        // 6. 验证能创建Dependency(Trace)
        EObject dependency = pilotEMFService.createTraceDependency(
            "req-001",
            "req-002",
            "derive"
        );
        assertNotNull(dependency, "必须能创建Dependency对象");
        assertTrue(dependency.eClass().getName().contains("Dependency") ||
                  dependency.eClass().getName().contains("Satisfy"),
                  "创建的对象必须是Dependency或相关类型");
        
        System.out.println("✅ Phase 2 Pilot集成验证通过！");
        System.out.println("  - Pilot元模型加载: ✓");
        System.out.println("  - RequirementDefinition创建: ✓");
        System.out.println("  - RequirementUsage创建: ✓");
        System.out.println("  - Dependency(Trace)创建: ✓");
        System.out.println("  - 字段映射(reqId→declaredShortName): ✓");
        System.out.println("  - 字段映射(name→declaredName): ✓");
    }
}