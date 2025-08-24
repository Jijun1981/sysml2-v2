package com.sysml.mvp;

import com.sysml.mvp.model.EMFModelRegistry;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.emf.ecore.EPackage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 理解SysML 2.0中Definition vs Usage的概念测试
 */
@SpringBootTest
public class UnderstandDefinitionUsageTest {
    
    @Autowired
    private EMFModelRegistry modelRegistry;
    
    @Test
    public void exploreRequirementRelatedClasses() {
        EPackage sysmlPackage = modelRegistry.getSysMLPackage();
        
        System.out.println("=== 所有与Requirement相关的类 ===");
        sysmlPackage.getEClassifiers().stream()
            .filter(classifier -> classifier.getName().contains("Requirement"))
            .forEach(classifier -> {
                System.out.println("类名: " + classifier.getName());
                if (classifier instanceof EClass) {
                    EClass eClass = (EClass) classifier;
                    List<String> superTypes = eClass.getEAllSuperTypes().stream()
                        .map(superType -> superType.getName())
                        .toList();
                    System.out.println("  父类: " + superTypes);
                }
                System.out.println();
            });
        
        System.out.println("=== 所有与Definition相关的类 ===");
        sysmlPackage.getEClassifiers().stream()
            .filter(classifier -> classifier.getName().contains("Definition"))
            .forEach(classifier -> {
                System.out.println("类名: " + classifier.getName());
            });
        
        System.out.println("=== 所有与Usage相关的类 ===");
        sysmlPackage.getEClassifiers().stream()
            .filter(classifier -> classifier.getName().contains("Usage"))
            .forEach(classifier -> {
                System.out.println("类名: " + classifier.getName());
            });
        
        System.out.println("=== 核心继承层次分析 ===");
        EClass reqDefClass = (EClass) sysmlPackage.getEClassifier("RequirementDefinition");
        EClass reqUsageClass = (EClass) sysmlPackage.getEClassifier("RequirementUsage");
        
        if (reqDefClass != null) {
            System.out.println("RequirementDefinition 直接父类: " + 
                reqDefClass.getESuperTypes().stream().map(t -> t.getName()).toList());
        }
        
        if (reqUsageClass != null) {
            System.out.println("RequirementUsage 直接父类: " + 
                reqUsageClass.getESuperTypes().stream().map(t -> t.getName()).toList());
        }
    }
}