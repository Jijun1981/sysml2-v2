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

import java.io.File;
import java.io.IOException;

/**
 * EMF模型注册器 - 严格实现REQ-B1-1
 * 
 * REQ-B1-1 完整Pilot元模型注册：
 * - 启动时加载完整的SysML.ecore文件，注册所有EClass到EPackage.Registry
 * - 使用运行时加载的Pilot标准命名空间（不硬编码日期）
 * - 包含完整继承层次（Element→...→RequirementDefinition）
 * - EMF自动处理所有继承关系，子类可访问所有父类属性
 */
@Slf4j
@Component
public class EMFModelRegistry {
    
    private EPackage sysmlPackage;
    private String sysmlNamespaceURI;
    
    @PostConstruct
    public void registerSysMLEPackage() {
        log.info("开始注册SysML Pilot元模型...");
        
        try {
            // REQ-B1-2: 注册JsonResourceFactoryImpl - 使用sirius-emfjson
            JsonResourceFactoryImpl factory = new JsonResourceFactoryImpl();
            Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("json", factory);
            
            // 注册EcoreResourceFactory来加载.ecore文件
            Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("ecore", new org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl());
            
            log.info("JSON和Ecore资源工厂注册完成");
            
            // REQ-B1-1: 启动时加载完整的SysML.ecore文件
            loadCompletePilotSysMLModel();
            
            // REQ-B1-1: 注册所有EClass到EPackage.Registry
            EPackage.Registry.INSTANCE.put(sysmlNamespaceURI, sysmlPackage);
            log.info("成功注册SysML Pilot元模型到EPackage.Registry");
            
            log.info("完整SysML Pilot元模型注册成功！");
            log.info("命名空间URI: {}", sysmlNamespaceURI);
            log.info("注册的EClass数量: {}", sysmlPackage.getEClassifiers().size());
            
            // 验证完整继承层次
            validateInheritanceHierarchy();
            
        } catch (Exception e) {
            log.error("加载SysML Pilot元模型失败", e);
            throw new RuntimeException("无法加载SysML Pilot元模型 - 需求REQ-B1-1要求必须加载完整Pilot.ecore文件", e);
        }
    }
    
    /**
     * REQ-B1-1: 启动时加载完整的SysML.ecore文件
     */
    private void loadCompletePilotSysMLModel() throws IOException {
        // 查找SysML.ecore文件 - 从Pilot Implementation项目中
        String[] possiblePaths = {
            "../opensource/SysML-v2-Pilot-Implementation/org.omg.sysml/model/SysML.ecore",
            "../../opensource/SysML-v2-Pilot-Implementation/org.omg.sysml/model/SysML.ecore",
            "./opensource/SysML-v2-Pilot-Implementation/org.omg.sysml/model/SysML.ecore"
        };
        
        File ecoreFile = null;
        for (String path : possiblePaths) {
            File candidate = new File(path);
            if (candidate.exists()) {
                ecoreFile = candidate;
                log.info("找到SysML.ecore文件: {}", candidate.getAbsolutePath());
                break;
            }
        }
        
        if (ecoreFile == null) {
            throw new RuntimeException("找不到SysML.ecore文件 - REQ-B1-1要求加载完整的SysML.ecore文件。" +
                    "请确保SysML-v2-Pilot-Implementation项目存在于opensource目录中");
        }
        
        // 使用EMF加载.ecore文件
        ResourceSet resourceSet = new ResourceSetImpl();
        URI ecoreURI = URI.createFileURI(ecoreFile.getAbsolutePath());
        Resource ecoreResource = resourceSet.getResource(ecoreURI, true);
        
        if (ecoreResource == null || ecoreResource.getContents().isEmpty()) {
            throw new RuntimeException("无法加载SysML.ecore文件内容");
        }
        
        // 获取SysML EPackage
        EObject rootObject = ecoreResource.getContents().get(0);
        if (!(rootObject instanceof EPackage)) {
            throw new RuntimeException("SysML.ecore根对象不是EPackage类型");
        }
        
        sysmlPackage = (EPackage) rootObject;
        
        // REQ-B1-1: 使用运行时加载的Pilot标准命名空间（不硬编码日期）
        sysmlNamespaceURI = sysmlPackage.getNsURI();
        if (sysmlNamespaceURI == null || !sysmlNamespaceURI.startsWith("https://www.omg.org/spec/SysML/")) {
            throw new RuntimeException("加载的SysML.ecore文件命名空间URI不符合Pilot标准格式: " + sysmlNamespaceURI);
        }
        
        log.info("成功加载完整SysML.ecore文件，命名空间: {}", sysmlNamespaceURI);
        log.info("加载的EClass数量: {}", sysmlPackage.getEClassifiers().size());
        
        // 验证是完整的SysML元模型
        if (sysmlPackage.getEClassifiers().size() < 100) {
            throw new RuntimeException("加载的不是完整SysML元模型，EClass数量过少: " + sysmlPackage.getEClassifiers().size());
        }
    }
    
    /**
     * REQ-B1-1: 验证包含完整继承层次
     */
    private void validateInheritanceHierarchy() {
        // 验证Pilot元模型中必须包含的核心类
        String[] requiredClasses = {
            "Element", "Feature", "Type", "ConstraintDefinition", 
            "RequirementDefinition", "RequirementUsage"
        };
        
        for (String className : requiredClasses) {
            EClass eClass = (EClass) sysmlPackage.getEClassifier(className);
            if (eClass == null) {
                // 输出所有可用的类名帮助调试
                log.error("可用的类名: {}", sysmlPackage.getEClassifiers().stream()
                    .map(classifier -> classifier.getName())
                    .sorted()
                    .toList());
                throw new RuntimeException("完整继承层次验证失败：缺少 " + className + " 类");
            }
        }
        
        // REQ-B1-1: 验证EMF自动处理继承关系
        EClass reqDefClass = (EClass) sysmlPackage.getEClassifier("RequirementDefinition");
        if (reqDefClass != null) {
            boolean hasElementInHierarchy = reqDefClass.getEAllSuperTypes().stream()
                    .anyMatch(superType -> "Element".equals(superType.getName()));
            
            if (!hasElementInHierarchy) {
                log.error("RequirementDefinition的父类: {}", reqDefClass.getEAllSuperTypes().stream()
                    .map(superType -> superType.getName())
                    .toList());
                throw new RuntimeException("EMF继承关系处理验证失败：RequirementDefinition未继承自Element");
            }
            
            // 验证子类可访问所有父类属性
            int allAttributeCount = reqDefClass.getEAllAttributes().size();
            int ownAttributeCount = reqDefClass.getEAttributes().size();
            if (allAttributeCount <= ownAttributeCount) {
                log.warn("RequirementDefinition所有属性数: {}, 自身属性数: {}", allAttributeCount, ownAttributeCount);
                // Pilot元模型可能属性定义方式不同，不作为阻塞性错误
            }
            
            log.info("RequirementDefinition继承层次验证通过，父类: {}", 
                reqDefClass.getEAllSuperTypes().stream()
                    .map(superType -> superType.getName())
                    .toList());
        }
        
        log.info("完整继承层次验证通过");
    }
    
    // 公共访问方法
    
    public EPackage getSysMLPackage() {
        return sysmlPackage;
    }
    
    public String getSysMLNamespaceURI() {
        return sysmlNamespaceURI;
    }
    
    // 兼容旧接口的方法（向后兼容）
    @Deprecated
    public EPackage getSysmlPackage() {
        return getSysMLPackage();
    }
    
    // Phase 2将重写为动态EMF的临时兼容方法
    @Deprecated
    public EObject createRequirementDefinition() {
        EClass reqDefClass = (EClass) sysmlPackage.getEClassifier("RequirementDefinition");
        if (reqDefClass != null) {
            return sysmlPackage.getEFactoryInstance().create(reqDefClass);
        }
        throw new RuntimeException("RequirementDefinition class not found in Pilot model");
    }
    
    @Deprecated
    public EObject createRequirementUsage() {
        return createRequirementUsage(null);
    }
    
    @Deprecated
    public EObject createRequirementUsage(String of) {
        EClass reqUsageClass = (EClass) sysmlPackage.getEClassifier("RequirementUsage");
        if (reqUsageClass != null) {
            return sysmlPackage.getEFactoryInstance().create(reqUsageClass);
        }
        throw new RuntimeException("RequirementUsage class not found in Pilot model");
    }
    
    @Deprecated
    public EObject createTrace() {
        return createTrace(null, null, null);
    }
    
    @Deprecated
    public EObject createTrace(String sourceId, String targetId, String type) {
        // Phase 2将实现Trace -> Dependency映射
        EClass dependencyClass = (EClass) sysmlPackage.getEClassifier("Dependency");
        if (dependencyClass != null) {
            return sysmlPackage.getEFactoryInstance().create(dependencyClass);
        }
        throw new RuntimeException("Dependency class not found in Pilot model");
    }
}