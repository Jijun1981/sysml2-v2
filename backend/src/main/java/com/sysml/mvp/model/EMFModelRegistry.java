package com.sysml.mvp.model;

import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.impl.EcoreFactoryImpl;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * EMF模型注册器
 * 负责注册本地SysML2子集元模型
 */
@Slf4j
@Component
public class EMFModelRegistry {
    
    private static final String NS_URI = "urn:your:sysml2";
    private static final String NS_PREFIX = "sysml";
    
    private EPackage sysmlPackage;
    
    @PostConstruct
    public void registerLocalEPackage() {
        log.info("注册本地SysML2 EPackage...");
        
        // 创建包
        sysmlPackage = EcoreFactory.eINSTANCE.createEPackage();
        sysmlPackage.setName("sysml");
        sysmlPackage.setNsPrefix(NS_PREFIX);
        sysmlPackage.setNsURI(NS_URI);
        
        // 创建元类
        EClass requirementDefinition = createRequirementDefinition();
        EClass requirementUsage = createRequirementUsage();
        EClass trace = createTrace();
        
        // 添加到包
        sysmlPackage.getEClassifiers().add(requirementDefinition);
        sysmlPackage.getEClassifiers().add(requirementUsage);
        sysmlPackage.getEClassifiers().add(trace);
        
        // 注册到全局Registry
        EPackage.Registry.INSTANCE.put(NS_URI, sysmlPackage);
        
        log.info("SysML2 EPackage注册完成: {}", NS_URI);
    }
    
    private EClass createRequirementDefinition() {
        EClass eClass = EcoreFactory.eINSTANCE.createEClass();
        eClass.setName("RequirementDefinition");
        
        // 添加属性
        addAttribute(eClass, "id", EcorePackage.Literals.ESTRING);
        addAttribute(eClass, "reqId", EcorePackage.Literals.ESTRING);
        addAttribute(eClass, "name", EcorePackage.Literals.ESTRING);
        addAttribute(eClass, "text", EcorePackage.Literals.ESTRING);
        addAttribute(eClass, "doc", EcorePackage.Literals.ESTRING);
        addAttribute(eClass, "tags", EcorePackage.Literals.ESTRING, true);
        addAttribute(eClass, "createdAt", EcorePackage.Literals.EDATE);
        addAttribute(eClass, "updatedAt", EcorePackage.Literals.EDATE);
        addAttribute(eClass, "_version", EcorePackage.Literals.ESTRING);
        
        return eClass;
    }
    
    private EClass createRequirementUsage() {
        EClass eClass = EcoreFactory.eINSTANCE.createEClass();
        eClass.setName("RequirementUsage");
        
        // 添加属性
        addAttribute(eClass, "id", EcorePackage.Literals.ESTRING);
        addAttribute(eClass, "of", EcorePackage.Literals.ESTRING);
        addAttribute(eClass, "name", EcorePackage.Literals.ESTRING);
        addAttribute(eClass, "text", EcorePackage.Literals.ESTRING);
        addAttribute(eClass, "status", EcorePackage.Literals.ESTRING);
        addAttribute(eClass, "tags", EcorePackage.Literals.ESTRING, true);
        addAttribute(eClass, "createdAt", EcorePackage.Literals.EDATE);
        addAttribute(eClass, "updatedAt", EcorePackage.Literals.EDATE);
        addAttribute(eClass, "_version", EcorePackage.Literals.ESTRING);
        
        return eClass;
    }
    
    private EClass createTrace() {
        EClass eClass = EcoreFactory.eINSTANCE.createEClass();
        eClass.setName("Trace");
        
        // 添加属性
        addAttribute(eClass, "id", EcorePackage.Literals.ESTRING);
        addAttribute(eClass, "fromId", EcorePackage.Literals.ESTRING);
        addAttribute(eClass, "toId", EcorePackage.Literals.ESTRING);
        addAttribute(eClass, "type", EcorePackage.Literals.ESTRING);
        addAttribute(eClass, "createdAt", EcorePackage.Literals.EDATE);
        
        return eClass;
    }
    
    private void addAttribute(EClass eClass, String name, EClassifier type) {
        addAttribute(eClass, name, type, false);
    }
    
    private void addAttribute(EClass eClass, String name, EClassifier type, boolean isMany) {
        EAttribute attr = EcoreFactory.eINSTANCE.createEAttribute();
        attr.setName(name);
        attr.setEType(type);
        if (isMany) {
            attr.setUpperBound(EStructuralFeature.UNBOUNDED_MULTIPLICITY);
        }
        eClass.getEStructuralFeatures().add(attr);
    }
    
    public EPackage getSysmlPackage() {
        return sysmlPackage;
    }
}