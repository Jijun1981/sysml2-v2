package com.sysml.mvp;

import com.sysml.mvp.model.EMFModelRegistry;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD测试用例 - 基于REQ-B1-1完整Pilot元模型注册
 * 
 * 需求验收标准：
 * - 启动时加载完整的SysML.ecore文件，注册所有EClass到EPackage.Registry
 * - 使用运行时加载的Pilot标准命名空间（不硬编码日期）
 * - 包含完整继承层次（Element→NamedElement→Feature→Type→Classifier→Definition→OccurrenceDefinition→ConstraintDefinition→RequirementDefinition）
 * - EMF自动处理所有继承关系，子类可访问所有父类属性
 */
@SpringBootTest
public class PilotModelRegistryTest {
    
    @Autowired
    private EMFModelRegistry modelRegistry;
    
    @Test
    public void shouldLoadCompleteSysMLEcoreFile() {
        // REQ-B1-1: 启动时加载完整的SysML.ecore文件
        EPackage sysmlPackage = modelRegistry.getSysMLPackage();
        assertNotNull(sysmlPackage, "必须成功加载SysML.ecore文件");
        
        // 验证是完整的SysML元模型（不是简化版本）
        assertTrue(sysmlPackage.getEClassifiers().size() > 100, 
                   "完整SysML元模型应包含100+个EClass，实际数量: " + sysmlPackage.getEClassifiers().size());
    }
    
    @Test
    public void shouldRegisterAllEClassesToEPackageRegistry() {
        // REQ-B1-1: 注册所有EClass到EPackage.Registry
        String pilotNamespaceURI = modelRegistry.getSysMLNamespaceURI();
        
        // 验证已注册到全局Registry
        EPackage registeredPackage = EPackage.Registry.INSTANCE.getEPackage(pilotNamespaceURI);
        assertNotNull(registeredPackage, "SysML包必须注册到EPackage.Registry");
        
        EPackage localPackage = modelRegistry.getSysMLPackage();
        assertEquals(registeredPackage, localPackage, "Registry中的包必须与本地包相同");
    }
    
    @Test
    public void shouldUseRuntimePilotNamespaceURI() {
        // REQ-B1-1: 使用运行时加载的Pilot标准命名空间（不硬编码日期）
        String pilotNamespaceURI = modelRegistry.getSysMLNamespaceURI();
        
        assertNotNull(pilotNamespaceURI, "Pilot命名空间URI不能为空");
        assertTrue(pilotNamespaceURI.startsWith("https://www.omg.org/spec/SysML/"), 
                   "必须使用Pilot标准命名空间格式，实际: " + pilotNamespaceURI);
        
        // 验证不是硬编码的日期，而是从实际加载的.ecore文件中获取
        assertFalse(pilotNamespaceURI.equals("https://www.omg.org/spec/SysML/20250201"), 
                    "不应硬编码特定日期，应使用运行时加载的命名空间");
    }
    
    @Test
    public void shouldContainCompleteInheritanceHierarchy() {
        // REQ-B1-1: 包含完整继承层次（Element→...→RequirementDefinition）
        EPackage sysmlPackage = modelRegistry.getSysMLPackage();
        
        // 验证完整继承链中的每个类都存在
        EClass elementClass = (EClass) sysmlPackage.getEClassifier("Element");
        assertNotNull(elementClass, "必须包含Element类");
        
        EClass namedElementClass = (EClass) sysmlPackage.getEClassifier("NamedElement");
        assertNotNull(namedElementClass, "必须包含NamedElement类");
        
        EClass featureClass = (EClass) sysmlPackage.getEClassifier("Feature");
        assertNotNull(featureClass, "必须包含Feature类");
        
        EClass typeClass = (EClass) sysmlPackage.getEClassifier("Type");
        assertNotNull(typeClass, "必须包含Type类");
        
        EClass classifierClass = (EClass) sysmlPackage.getEClassifier("Classifier");
        assertNotNull(classifierClass, "必须包含Classifier类");
        
        EClass definitionClass = (EClass) sysmlPackage.getEClassifier("Definition");
        assertNotNull(definitionClass, "必须包含Definition类");
        
        EClass occurrenceDefinitionClass = (EClass) sysmlPackage.getEClassifier("OccurrenceDefinition");
        assertNotNull(occurrenceDefinitionClass, "必须包含OccurrenceDefinition类");
        
        EClass constraintDefinitionClass = (EClass) sysmlPackage.getEClassifier("ConstraintDefinition");
        assertNotNull(constraintDefinitionClass, "必须包含ConstraintDefinition类");
        
        EClass requirementDefinitionClass = (EClass) sysmlPackage.getEClassifier("RequirementDefinition");
        assertNotNull(requirementDefinitionClass, "必须包含RequirementDefinition类");
    }
    
    @Test
    public void shouldSupportEMFInheritanceAutoProcessing() {
        // REQ-B1-1: EMF自动处理所有继承关系，子类可访问所有父类属性
        EPackage sysmlPackage = modelRegistry.getSysMLPackage();
        EClass reqDefClass = (EClass) sysmlPackage.getEClassifier("RequirementDefinition");
        
        assertNotNull(reqDefClass, "RequirementDefinition类不能为空");
        
        // 验证完整继承链：EMF应自动处理继承关系
        boolean hasElement = false;
        boolean hasNamedElement = false;
        boolean hasDefinition = false;
        boolean hasConstraintDefinition = false;
        
        for (EClass superClass : reqDefClass.getEAllSuperTypes()) {
            String name = superClass.getName();
            if ("Element".equals(name)) hasElement = true;
            if ("NamedElement".equals(name)) hasNamedElement = true;
            if ("Definition".equals(name)) hasDefinition = true;
            if ("ConstraintDefinition".equals(name)) hasConstraintDefinition = true;
        }
        
        assertTrue(hasElement, "RequirementDefinition必须继承自Element");
        assertTrue(hasNamedElement, "RequirementDefinition必须继承自NamedElement");
        assertTrue(hasDefinition, "RequirementDefinition必须继承自Definition");
        assertTrue(hasConstraintDefinition, "RequirementDefinition必须继承自ConstraintDefinition");
    }
    
    @Test
    public void shouldAccessAllParentClassAttributes() {
        // REQ-B1-1: 子类可访问所有父类属性
        EPackage sysmlPackage = modelRegistry.getSysMLPackage();
        EClass reqDefClass = (EClass) sysmlPackage.getEClassifier("RequirementDefinition");
        
        // 验证可以通过EMF访问继承的核心属性
        EStructuralFeature elementIdFeature = reqDefClass.getEStructuralFeature("elementId");
        assertNotNull(elementIdFeature, "必须能访问从Element继承的elementId属性");
        
        EStructuralFeature declaredNameFeature = reqDefClass.getEStructuralFeature("declaredName");
        assertNotNull(declaredNameFeature, "必须能访问从NamedElement继承的declaredName属性");
        
        EStructuralFeature declaredShortNameFeature = reqDefClass.getEStructuralFeature("declaredShortName");
        assertNotNull(declaredShortNameFeature, "必须能访问从NamedElement继承的declaredShortName属性");
        
        // 验证所有继承的属性总数 > 自身定义的属性数
        int allAttributeCount = reqDefClass.getEAllAttributes().size();
        int ownAttributeCount = reqDefClass.getEAttributes().size();
        assertTrue(allAttributeCount > ownAttributeCount, 
                   "继承的属性总数必须大于自身属性数，继承属性: " + allAttributeCount + ", 自身属性: " + ownAttributeCount);
    }
}