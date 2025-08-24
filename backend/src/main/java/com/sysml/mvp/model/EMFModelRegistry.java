package com.sysml.mvp.model;

import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.common.util.URI;
import org.eclipse.sirius.emfjson.resource.JsonResourceFactoryImpl;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * EMF模型注册器
 * 负责注册官方SysML2元模型
 */
@Slf4j
@Component
public class EMFModelRegistry {
    
    // 使用本地SysML命名空间 - 按照架构文档
    private static final String NS_URI = "urn:your:sysml2";
    private static final String NS_PREFIX = "sysml";
    
    private EPackage sysmlPackage;
    
    @PostConstruct
    public void registerSysMLEPackage() {
        log.info("注册官方SysML2 EPackage...");
        
        // 注册JsonResourceFactoryImpl - 使用sirius-emfjson
        JsonResourceFactoryImpl factory = new JsonResourceFactoryImpl();
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("json", factory);
        log.info("JSON资源工厂注册完成");
        
        // 创建简化的SysML包用于演示
        sysmlPackage = EcoreFactory.eINSTANCE.createEPackage();
        sysmlPackage.setName("sysml");
        sysmlPackage.setNsPrefix(NS_PREFIX);
        sysmlPackage.setNsURI(NS_URI);
        
        // 创建基本的需求类型
        EClass requirementDefinition = createRequirementDefinitionEClass();
        EClass requirementUsage = createRequirementUsageEClass();
        
        // 添加到包
        sysmlPackage.getEClassifiers().add(requirementDefinition);
        sysmlPackage.getEClassifiers().add(requirementUsage);
        
        // 注册到全局Registry（多种URI形式）
        EPackage.Registry.INSTANCE.put(NS_URI, sysmlPackage);
        EPackage.Registry.INSTANCE.put("", sysmlPackage);  // 支持fragment-only引用
        
        // 注册URN协议支持
        Resource.Factory.Registry.INSTANCE.getProtocolToFactoryMap().put("urn", factory);
        
        log.info("SysML2 EPackage注册完成: {}", NS_URI);
    }
    
    private EClass createRequirementDefinitionEClass() {
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
    
    private EClass createRequirementUsageEClass() {
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
    
    // 工厂方法 - 创建具体对象 (REQ-B2-1, REQ-B2-2)
    public EObject createRequirementDefinition() {
        EClass eClass = (EClass) sysmlPackage.getEClassifier("RequirementDefinition");
        EObject obj = sysmlPackage.getEFactoryInstance().create(eClass);
        
        // 设置默认值 (REQ-B2-2)
        obj.eSet(eClass.getEStructuralFeature("id"), generateId("R-"));
        obj.eSet(eClass.getEStructuralFeature("_version"), "1.0");
        obj.eSet(eClass.getEStructuralFeature("createdAt"), new java.util.Date());
        obj.eSet(eClass.getEStructuralFeature("updatedAt"), new java.util.Date());
        
        return obj;
    }
    
    public EObject createRequirementUsage(String ofId) {
        EClass eClass = (EClass) sysmlPackage.getEClassifier("RequirementUsage");
        EObject obj = sysmlPackage.getEFactoryInstance().create(eClass);
        
        // 设置默认值 (REQ-B2-2)
        obj.eSet(eClass.getEStructuralFeature("id"), generateId("U-"));
        obj.eSet(eClass.getEStructuralFeature("status"), "draft");
        obj.eSet(eClass.getEStructuralFeature("_version"), "1.0");
        obj.eSet(eClass.getEStructuralFeature("of"), ofId);
        obj.eSet(eClass.getEStructuralFeature("createdAt"), new java.util.Date());
        obj.eSet(eClass.getEStructuralFeature("updatedAt"), new java.util.Date());
        
        return obj;
    }
    
    public EObject createTrace(String fromId, String toId, String type) {
        // 创建Trace类 (REQ-C3-1)
        if (sysmlPackage.getEClassifier("Trace") == null) {
            EClass traceClass = EcoreFactory.eINSTANCE.createEClass();
            traceClass.setName("Trace");
            addAttribute(traceClass, "id", EcorePackage.Literals.ESTRING);
            addAttribute(traceClass, "fromId", EcorePackage.Literals.ESTRING);
            addAttribute(traceClass, "toId", EcorePackage.Literals.ESTRING);
            addAttribute(traceClass, "type", EcorePackage.Literals.ESTRING);
            addAttribute(traceClass, "createdAt", EcorePackage.Literals.EDATE);
            sysmlPackage.getEClassifiers().add(traceClass);
        }
        
        EClass traceClass = (EClass) sysmlPackage.getEClassifier("Trace");
        EObject obj = sysmlPackage.getEFactoryInstance().create(traceClass);
        obj.eSet(traceClass.getEStructuralFeature("id"), generateId("T-"));
        obj.eSet(traceClass.getEStructuralFeature("fromId"), fromId);
        obj.eSet(traceClass.getEStructuralFeature("toId"), toId);
        obj.eSet(traceClass.getEStructuralFeature("type"), type);
        obj.eSet(traceClass.getEStructuralFeature("createdAt"), new java.util.Date());
        
        return obj;
    }
    
    // REQ-B2-3: ID稳定 - 使用UUID替代时间戳
    private String generateId(String prefix) {
        return prefix + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}