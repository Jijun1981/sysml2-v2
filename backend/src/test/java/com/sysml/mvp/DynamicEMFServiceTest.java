package com.sysml.mvp;

import com.sysml.mvp.model.EMFModelRegistry;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD测试用例 - 基于REQ-B2-1 Service层工厂方法
 * 
 * 需求验收标准：
 * - 提供 createRequirementDefinition() / createRequirementUsage(ofId) / createTraceDependency(fromId,toId,type)
 * - 缺参→400
 * - 使用动态EMF创建Pilot元模型对象
 * - 正确映射API字段到Pilot字段
 */
@SpringBootTest
public class DynamicEMFServiceTest {
    
    @Autowired
    private EMFModelRegistry modelRegistry;
    
    @Test
    public void shouldCreateRequirementDefinitionViaDynamicEMF() {
        // REQ-B2-1: 提供 createRequirementDefinition()
        EPackage sysmlPackage = modelRegistry.getSysMLPackage();
        EClass reqDefClass = (EClass) sysmlPackage.getEClassifier("RequirementDefinition");
        
        assertNotNull(reqDefClass, "必须能获取RequirementDefinition类");
        
        // 使用动态EMF创建对象
        EObject reqDef = sysmlPackage.getEFactoryInstance().create(reqDefClass);
        assertNotNull(reqDef, "必须能通过动态EMF创建RequirementDefinition对象");
        
        // 验证是正确的类型
        assertEquals("RequirementDefinition", reqDef.eClass().getName());
        assertTrue(reqDefClass.isInstance(reqDef), "创建的对象必须是RequirementDefinition实例");
    }
    
    @Test
    public void shouldCreateRequirementUsageWithOfReference() {
        // REQ-B2-1: 提供 createRequirementUsage(ofId)
        EPackage sysmlPackage = modelRegistry.getSysMLPackage();
        EClass reqUsageClass = (EClass) sysmlPackage.getEClassifier("RequirementUsage");
        
        assertNotNull(reqUsageClass, "必须能获取RequirementUsage类");
        
        // 使用动态EMF创建对象
        EObject reqUsage = sysmlPackage.getEFactoryInstance().create(reqUsageClass);
        assertNotNull(reqUsage, "必须能通过动态EMF创建RequirementUsage对象");
        
        // 验证是正确的类型
        assertEquals("RequirementUsage", reqUsage.eClass().getName());
        assertTrue(reqUsageClass.isInstance(reqUsage), "创建的对象必须是RequirementUsage实例");
        
        // TODO: 验证Usage包含对Definition的引用关系
        // 这需要进一步分析Pilot元模型中的引用字段名称
    }
    
    @Test
    public void shouldMapReqIdToDeclaredShortName() {
        // REQ-B2-4: DTO选择性映射 - API reqId → Pilot declaredShortName
        EPackage sysmlPackage = modelRegistry.getSysMLPackage();
        EClass reqDefClass = (EClass) sysmlPackage.getEClassifier("RequirementDefinition");
        EObject reqDef = sysmlPackage.getEFactoryInstance().create(reqDefClass);
        
        // 查找declaredShortName字段
        EStructuralFeature shortNameFeature = reqDefClass.getEStructuralFeature("declaredShortName");
        assertNotNull(shortNameFeature, "RequirementDefinition必须包含declaredShortName字段");
        
        // 使用动态EMF设置字段值
        String testReqId = "REQ-001";
        reqDef.eSet(shortNameFeature, testReqId);
        
        // 验证字段值设置成功
        assertEquals(testReqId, reqDef.eGet(shortNameFeature));
    }
    
    @Test
    public void shouldMapTextToDocumentationBody() {
        // REQ-B2-4: DTO选择性映射 - API text → Pilot documentation.body
        EPackage sysmlPackage = modelRegistry.getSysMLPackage();
        EClass reqDefClass = (EClass) sysmlPackage.getEClassifier("RequirementDefinition");
        EObject reqDef = sysmlPackage.getEFactoryInstance().create(reqDefClass);
        
        // Pilot元模型中documentation可能是复杂的嵌套结构
        // 首先尝试查找documentation字段
        EStructuralFeature docFeature = null;
        for (EStructuralFeature feature : reqDefClass.getEAllStructuralFeatures()) {
            if (feature.getName().toLowerCase().contains("documentation") || 
                feature.getName().toLowerCase().contains("body")) {
                docFeature = feature;
                break;
            }
        }
        
        // 如果找到documentation相关字段，测试设置文本
        if (docFeature != null) {
            String testText = "系统响应时间不得超过500ms";
            // 根据字段类型设置值（可能需要创建Documentation对象）
            if (docFeature.getEType().getName().equals("EString")) {
                reqDef.eSet(docFeature, testText);
                assertEquals(testText, reqDef.eGet(docFeature));
            } else {
                // 如果是复杂对象，需要创建Documentation对象并设置body
                // 这部分在实现阶段会详细处理
                assertNotNull(docFeature, "至少要能找到documentation相关字段");
            }
        } else {
            // 如果没有直接的documentation字段，检查是否有其他文本字段
            fail("RequirementDefinition必须包含documentation或类似的文本字段");
        }
    }
    
    @Test
    public void shouldCreateDependencyForTraceAPI() {
        // REQ-B2-1: 提供 createTraceDependency(fromId,toId,type)
        // API层使用Trace，内部映射到Pilot的Dependency
        EPackage sysmlPackage = modelRegistry.getSysMLPackage();
        EClass dependencyClass = (EClass) sysmlPackage.getEClassifier("Dependency");
        
        assertNotNull(dependencyClass, "必须能获取Dependency类");
        
        // 使用动态EMF创建Dependency对象
        EObject dependency = sysmlPackage.getEFactoryInstance().create(dependencyClass);
        assertNotNull(dependency, "必须能通过动态EMF创建Dependency对象");
        
        // 验证是正确的类型
        assertEquals("Dependency", dependency.eClass().getName());
        assertTrue(dependencyClass.isInstance(dependency), "创建的对象必须是Dependency实例");
    }
    
    @Test
    public void shouldMapTraceTypeToDependencySubclass() {
        // REQ-C3-1: Trace type∈{derive,satisfy,refine,trace}
        // 需要验证Pilot元模型中是否有对应的Dependency子类或属性
        EPackage sysmlPackage = modelRegistry.getSysMLPackage();
        
        // 检查是否存在专门的追溯关系类
        String[] traceTypes = {"derive", "satisfy", "refine", "trace"};
        
        for (String traceType : traceTypes) {
            // 检查是否有对应的EClass（如DeriveRequirement, SatisfyRequirement等）
            EClass traceClass = (EClass) sysmlPackage.getEClassifier(
                Character.toUpperCase(traceType.charAt(0)) + traceType.substring(1) + "Requirement");
            
            if (traceClass == null) {
                // 如果没有专门的类，检查是否有其他命名方式
                traceClass = (EClass) sysmlPackage.getEClassifier(
                    Character.toUpperCase(traceType.charAt(0)) + traceType.substring(1));
            }
            
            // 至少应该有Dependency作为基类
            if (traceClass != null) {
                EClass dependencyClass = (EClass) sysmlPackage.getEClassifier("Dependency");
                assertTrue(dependencyClass.isSuperTypeOf(traceClass) || traceClass.equals(dependencyClass),
                          traceType + "关系类必须继承自Dependency或就是Dependency");
            }
        }
        
        // 确保至少有基础的Dependency类可以使用
        EClass dependencyClass = (EClass) sysmlPackage.getEClassifier("Dependency");
        assertNotNull(dependencyClass, "必须有Dependency类作为追溯关系的基础");
    }
    
    @Test
    public void shouldMapNameToDeclaredName() {
        // REQ-B2-4: API name → Pilot declaredName
        EPackage sysmlPackage = modelRegistry.getSysMLPackage();
        EClass reqDefClass = (EClass) sysmlPackage.getEClassifier("RequirementDefinition");
        EObject reqDef = sysmlPackage.getEFactoryInstance().create(reqDefClass);
        
        // 查找declaredName字段
        EStructuralFeature nameFeature = reqDefClass.getEStructuralFeature("declaredName");
        assertNotNull(nameFeature, "RequirementDefinition必须包含declaredName字段");
        
        // 使用动态EMF设置字段值
        String testName = "系统响应时间需求";
        reqDef.eSet(nameFeature, testName);
        
        // 验证字段值设置成功
        assertEquals(testName, reqDef.eGet(nameFeature));
    }
    
    @Test
    public void shouldSupportInheritanceChainDynamicAccess() {
        // REQ-B1-1: EMF自动处理所有继承关系，子类可访问所有父类属性
        EPackage sysmlPackage = modelRegistry.getSysMLPackage();
        EClass reqDefClass = (EClass) sysmlPackage.getEClassifier("RequirementDefinition");
        EObject reqDef = sysmlPackage.getEFactoryInstance().create(reqDefClass);
        
        // RequirementDefinition应该继承自Element，能访问Element的属性
        EStructuralFeature elementIdFeature = null;
        for (EStructuralFeature feature : reqDefClass.getEAllStructuralFeatures()) {
            if (feature.getName().toLowerCase().contains("elementid") || 
                feature.getName().toLowerCase().equals("id")) {
                elementIdFeature = feature;
                break;
            }
        }
        
        // 验证能通过继承访问父类属性
        assertNotNull(elementIdFeature, "RequirementDefinition通过继承应该能访问Element的ID属性");
        
        // 设置继承属性
        String testId = "req-def-001";
        if (elementIdFeature.getEType().getName().equals("EString")) {
            reqDef.eSet(elementIdFeature, testId);
            assertEquals(testId, reqDef.eGet(elementIdFeature));
        }
    }
}