package com.sysml.mvp.service;

import com.sysml.mvp.model.EMFModelRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.ecore.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Pilot元模型动态EMF服务
 * 实现REQ-B2-1: Service层工厂方法
 * 
 * 提供基于完整Pilot元模型的对象创建和操作方法
 * 使用动态EMF (eGet/eSet) 访问所有字段
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PilotEMFService {
    
    private final EMFModelRegistry modelRegistry;
    
    /**
     * REQ-B2-1: 创建RequirementDefinition
     * 使用Pilot元模型的RequirementDefinition类
     */
    public EObject createRequirementDefinition(String reqId, String name, String text) {
        log.debug("创建RequirementDefinition: reqId={}, name={}", reqId, name);
        
        EPackage sysmlPackage = modelRegistry.getSysMLPackage();
        EClass reqDefClass = (EClass) sysmlPackage.getEClassifier("RequirementDefinition");
        
        if (reqDefClass == null) {
            throw new IllegalStateException("无法找到RequirementDefinition类");
        }
        
        // 使用动态EMF创建对象
        EObject reqDef = sysmlPackage.getEFactoryInstance().create(reqDefClass);
        
        // 生成唯一ID
        String id = "req-def-" + UUID.randomUUID().toString();
        setAttributeIfExists(reqDef, "elementId", id);
        
        // 映射API字段到Pilot字段
        // reqId → declaredShortName
        setAttributeIfExists(reqDef, "declaredShortName", reqId);
        
        // name → declaredName
        setAttributeIfExists(reqDef, "declaredName", name);
        
        // text → documentation (这可能需要创建Documentation对象)
        // 暂时尝试直接设置，如果字段结构复杂再处理
        setDocumentation(reqDef, text);
        
        // 设置时间戳
        Date now = Date.from(Instant.now());
        setAttributeIfExists(reqDef, "createdAt", now);
        setAttributeIfExists(reqDef, "updatedAt", now);
        
        log.debug("成功创建RequirementDefinition: id={}", id);
        return reqDef;
    }
    
    /**
     * REQ-B2-1: 创建RequirementUsage
     * 使用Pilot元模型的RequirementUsage类
     * 
     * @param ofId 关联的RequirementDefinition的ID
     */
    public EObject createRequirementUsage(String ofId, String name, String text, String status) {
        log.debug("创建RequirementUsage: of={}, name={}", ofId, name);
        
        EPackage sysmlPackage = modelRegistry.getSysMLPackage();
        EClass reqUsageClass = (EClass) sysmlPackage.getEClassifier("RequirementUsage");
        
        if (reqUsageClass == null) {
            throw new IllegalStateException("无法找到RequirementUsage类");
        }
        
        // 使用动态EMF创建对象
        EObject reqUsage = sysmlPackage.getEFactoryInstance().create(reqUsageClass);
        
        // 生成唯一ID
        String id = "req-usage-" + UUID.randomUUID().toString();
        setAttributeIfExists(reqUsage, "elementId", id);
        
        // 设置基本属性
        setAttributeIfExists(reqUsage, "declaredName", name);
        setDocumentation(reqUsage, text);
        
        // 设置Usage特有的status字段（如果存在）
        setAttributeIfExists(reqUsage, "status", status);
        
        // 设置对Definition的引用
        // 这需要分析Pilot中Usage如何引用Definition
        // 可能通过typing, featuring或其他机制
        setUsageDefinitionReference(reqUsage, ofId);
        
        // 设置时间戳
        Date now = Date.from(Instant.now());
        setAttributeIfExists(reqUsage, "createdAt", now);
        setAttributeIfExists(reqUsage, "updatedAt", now);
        
        log.debug("成功创建RequirementUsage: id={}", id);
        return reqUsage;
    }
    
    /**
     * REQ-B2-1: 创建Trace (内部映射到Dependency)
     * API层使用Trace概念，内部创建Pilot的Dependency对象
     * 
     * @param fromId 源需求ID
     * @param toId 目标需求ID
     * @param type 追溯类型 (derive/satisfy/refine/trace)
     */
    public EObject createTraceDependency(String fromId, String toId, String type) {
        log.debug("创建Trace(Dependency): from={}, to={}, type={}", fromId, toId, type);
        
        EPackage sysmlPackage = modelRegistry.getSysMLPackage();
        
        // 根据type选择合适的类
        EClass dependencyClass = selectDependencyClass(sysmlPackage, type);
        
        if (dependencyClass == null) {
            throw new IllegalStateException("无法找到Dependency类");
        }
        
        // 使用动态EMF创建对象
        EObject dependency = sysmlPackage.getEFactoryInstance().create(dependencyClass);
        
        // 生成唯一ID
        String id = "trace-" + UUID.randomUUID().toString();
        setAttributeIfExists(dependency, "elementId", id);
        
        // 设置source和target引用
        // 需要分析Pilot中Dependency的结构
        setDependencyReferences(dependency, fromId, toId);
        
        // 如果使用基础Dependency类，设置type属性
        if ("Dependency".equals(dependencyClass.getName())) {
            setAttributeIfExists(dependency, "type", type);
            setAttributeIfExists(dependency, "stereotype", type);
        }
        
        // 设置时间戳
        Date now = Date.from(Instant.now());
        setAttributeIfExists(dependency, "createdAt", now);
        
        log.debug("成功创建Trace(Dependency): id={}", id);
        return dependency;
    }
    
    /**
     * 根据trace type选择合适的Dependency类
     */
    private EClass selectDependencyClass(EPackage sysmlPackage, String type) {
        // 首先尝试找专门的追溯类
        String[] possibleClassNames = {
            "SatisfyRequirementUsage",  // satisfy可能有专门的类
            "DeriveRequirement",         // derive
            "RefineRequirement",         // refine
            "Dependency"                 // 默认基类
        };
        
        if ("satisfy".equals(type)) {
            EClass satisfyClass = (EClass) sysmlPackage.getEClassifier("SatisfyRequirementUsage");
            if (satisfyClass != null) return satisfyClass;
        }
        
        // 如果没有专门的类，使用基础Dependency
        return (EClass) sysmlPackage.getEClassifier("Dependency");
    }
    
    /**
     * 设置Documentation字段
     * Pilot中documentation可能是复杂结构
     */
    private void setDocumentation(EObject eObject, String text) {
        // 首先尝试直接设置body或text字段
        if (!setAttributeIfExists(eObject, "body", text) && 
            !setAttributeIfExists(eObject, "text", text)) {
            
            // 如果没有直接字段，查找documentation相关字段
            EClass eClass = eObject.eClass();
            EStructuralFeature docFeature = eClass.getEStructuralFeature("documentation");
            
            if (docFeature instanceof EReference) {
                EReference docRef = (EReference) docFeature;
                EPackage sysmlPackage = modelRegistry.getSysMLPackage();
                EClass docClass = (EClass) sysmlPackage.getEClassifier("Documentation");
                
                if (docClass != null) {
                    EObject doc = sysmlPackage.getEFactoryInstance().create(docClass);
                    setAttributeIfExists(doc, "body", text);
                    
                    // 检查是否是多值引用（List）
                    if (docRef.isMany()) {
                        // documentation是一个List
                        @SuppressWarnings("unchecked")
                        List<EObject> docList = (List<EObject>) eObject.eGet(docFeature);
                        docList.add(doc);
                        log.debug("添加Documentation对象到列表");
                    } else {
                        // documentation是单个对象
                        eObject.eSet(docFeature, doc);
                        log.debug("设置单个Documentation对象");
                    }
                }
            }
        }
    }
    
    /**
     * 设置Usage对Definition的引用
     */
    private void setUsageDefinitionReference(EObject usage, String definitionId) {
        // Pilot中Usage可能通过多种方式引用Definition
        // 1. 直接的definition引用
        // 2. 通过typing机制
        // 3. 通过featuring机制
        
        EClass usageClass = usage.eClass();
        
        // 尝试各种可能的引用字段
        String[] possibleRefFields = {
            "definition",
            "type",
            "typing",
            "typedBy",
            "of"
        };
        
        for (String fieldName : possibleRefFields) {
            EStructuralFeature refFeature = usageClass.getEStructuralFeature(fieldName);
            if (refFeature instanceof EReference) {
                // TODO: 这里需要查找并设置实际的Definition对象引用
                // 暂时只记录ID
                log.debug("找到Usage->Definition引用字段: {}", fieldName);
                break;
            }
        }
        
        // 作为备选，至少在某个字段记录definitionId
        setAttributeIfExists(usage, "of", definitionId);
    }
    
    /**
     * 设置Dependency的source和target引用
     */
    private void setDependencyReferences(EObject dependency, String fromId, String toId) {
        EClass depClass = dependency.eClass();
        
        // 查找source相关字段
        String[] sourceFields = {"source", "client", "from"};
        String[] targetFields = {"target", "supplier", "to"};
        
        EStructuralFeature sourceFeature = null;
        EStructuralFeature targetFeature = null;
        
        for (String fieldName : sourceFields) {
            sourceFeature = depClass.getEStructuralFeature(fieldName);
            if (sourceFeature != null) break;
        }
        
        for (String fieldName : targetFields) {
            targetFeature = depClass.getEStructuralFeature(fieldName);
            if (targetFeature != null) break;
        }
        
        // TODO: 设置实际的对象引用而不只是ID
        // 暂时记录ID用于后续查找
        if (sourceFeature != null) {
            log.debug("找到source字段: {}", sourceFeature.getName());
        }
        if (targetFeature != null) {
            log.debug("找到target字段: {}", targetFeature.getName());
        }
        
        // 备选：至少记录ID
        setAttributeIfExists(dependency, "fromId", fromId);
        setAttributeIfExists(dependency, "toId", toId);
    }
    
    /**
     * 安全地设置属性（如果存在）
     * 
     * @return true if attribute was set, false otherwise
     */
    public boolean setAttributeIfExists(EObject eObject, String attributeName, Object value) {
        EClass eClass = eObject.eClass();
        EStructuralFeature feature = eClass.getEStructuralFeature(attributeName);
        
        if (feature != null && feature instanceof EAttribute) {
            try {
                eObject.eSet(feature, value);
                log.trace("设置属性 {}.{} = {}", eClass.getName(), attributeName, value);
                return true;
            } catch (Exception e) {
                log.debug("无法设置属性 {}.{}: {}", eClass.getName(), attributeName, e.getMessage());
            }
        }
        
        // 如果直接字段不存在，尝试从所有继承的字段中查找
        for (EStructuralFeature inheritedFeature : eClass.getEAllStructuralFeatures()) {
            if (attributeName.equals(inheritedFeature.getName()) && inheritedFeature instanceof EAttribute) {
                try {
                    eObject.eSet(inheritedFeature, value);
                    log.trace("设置继承属性 {}.{} = {}", eClass.getName(), attributeName, value);
                    return true;
                } catch (Exception e) {
                    log.debug("无法设置继承属性 {}.{}: {}", eClass.getName(), attributeName, e.getMessage());
                }
            }
        }
        
        return false;
    }
    
    /**
     * 安全地获取属性值
     */
    public Object getAttributeValue(EObject eObject, String attributeName) {
        EClass eClass = eObject.eClass();
        EStructuralFeature feature = eClass.getEStructuralFeature(attributeName);
        
        if (feature != null) {
            return eObject.eGet(feature);
        }
        
        // 尝试从继承的字段中获取
        for (EStructuralFeature inheritedFeature : eClass.getEAllStructuralFeatures()) {
            if (attributeName.equals(inheritedFeature.getName())) {
                return eObject.eGet(inheritedFeature);
            }
        }
        
        return null;
    }
    
    /**
     * 获取对象的eClass名称
     */
    public String getEClassName(EObject eObject) {
        return eObject.eClass().getName();
    }
    
    /**
     * Phase 4: 通用元素创建方法 - REQ-B5-1的核心实现
     * 可以创建任意SysML元素类型，无需为每个类型编写专门代码
     * 
     * @param eClassName SysML元素类型名称（如PartUsage, InterfaceDefinition等）
     * @param attributes 元素属性Map
     * @return 创建的EObject
     * @throws IllegalArgumentException 如果eClassName不存在
     */
    public EObject createElement(String eClassName, Map<String, Object> attributes) {
        log.debug("通用创建元素: eClass={}, attributes={}", eClassName, attributes.keySet());
        
        EPackage sysmlPackage = modelRegistry.getSysMLPackage();
        EClass eClass = (EClass) sysmlPackage.getEClassifier(eClassName);
        
        if (eClass == null) {
            throw new IllegalArgumentException("未知的eClass类型: " + eClassName);
        }
        
        // 使用动态EMF创建对象
        EObject element = sysmlPackage.getEFactoryInstance().create(eClass);
        
        // 生成唯一ID（如果没有提供）
        if (!attributes.containsKey("elementId")) {
            String id = eClassName.toLowerCase() + "-" + UUID.randomUUID().toString();
            setAttributeIfExists(element, "elementId", id);
        }
        
        // 设置所有提供的属性
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            String attrName = entry.getKey();
            Object value = entry.getValue();
            
            // 特殊处理documentation字段
            if ("documentation".equals(attrName) && value instanceof String) {
                setDocumentation(element, (String) value);
            } else {
                // 尝试设置属性
                boolean set = setAttributeIfExists(element, attrName, value);
                if (!set) {
                    log.trace("属性不存在或无法设置: {}.{}", eClassName, attrName);
                }
            }
        }
        
        // 设置时间戳（如果元素支持）
        Date now = Date.from(Instant.now());
        setAttributeIfExists(element, "createdAt", now);
        setAttributeIfExists(element, "updatedAt", now);
        
        log.debug("成功创建{}: elementId={}", eClassName, 
            getAttributeValue(element, "elementId"));
        
        return element;
    }
    
    /**
     * Phase 3: PATCH支持 - 合并部分属性到EObject
     * 只更新提供的属性，保留其他属性原值
     * 
     * @param eObject 要更新的对象
     * @param patchAttributes 要更新的属性Map（只包含要修改的字段）
     */
    public void mergeAttributes(EObject eObject, Map<String, Object> patchAttributes) {
        if (eObject == null || patchAttributes == null || patchAttributes.isEmpty()) {
            return;
        }
        
        log.debug("合并属性到{}: {}", eObject.eClass().getName(), patchAttributes.keySet());
        
        for (Map.Entry<String, Object> entry : patchAttributes.entrySet()) {
            String attributeName = entry.getKey();
            Object value = entry.getValue();
            
            // 跳过null值（null表示不更新该字段）
            if (value == null) {
                log.trace("跳过null值字段: {}", attributeName);
                continue;
            }
            
            // 跳过只读字段
            if (isReadOnlyField(attributeName)) {
                log.trace("跳过只读字段: {}", attributeName);
                continue;
            }
            
            // 特殊处理documentation字段
            if ("documentation".equals(attributeName) && value instanceof String) {
                setDocumentation(eObject, (String) value);
                continue;
            }
            
            // 尝试设置属性
            boolean set = setAttributeIfExists(eObject, attributeName, value);
            if (!set) {
                log.trace("字段不存在或无法设置: {}", attributeName);
            }
        }
    }
    
    /**
     * 判断字段是否为只读
     * 某些字段不应通过PATCH修改
     */
    private boolean isReadOnlyField(String fieldName) {
        return Set.of(
            "elementId",    // ID不应被修改
            "id",           // ID不应被修改
            "eClass",       // 类型不应被修改
            "createdAt",    // 创建时间不应被修改
            "type"          // 对象类型不应被修改
        ).contains(fieldName);
    }
}