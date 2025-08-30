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
     * 调试方法：打印RequirementDefinition和RequirementUsage的所有字段
     * 用于验证SysML元模型中的标准字段
     */
    public void debugMetamodel() {
        EPackage sysmlPackage = modelRegistry.getSysMLPackage();
        
        // 打印RequirementDefinition的所有属性
        EClass reqDefClass = (EClass) sysmlPackage.getEClassifier("RequirementDefinition");
        if (reqDefClass != null) {
            log.info("=== RequirementDefinition 所有属性 ===");
            
            // 自身属性
            log.info("自身属性 ({}个):", reqDefClass.getEAttributes().size());
            for (EAttribute attr : reqDefClass.getEAttributes()) {
                log.info("  - {}: {} ({})", attr.getName(), attr.getEType().getName(), 
                    attr.isRequired() ? "必填" : "可选");
            }
            
            // 所有结构特征（包括引用）
            log.info("所有结构特征 ({}个):", reqDefClass.getEAllStructuralFeatures().size());
            for (EStructuralFeature feature : reqDefClass.getEAllStructuralFeatures()) {
                String type = feature instanceof EAttribute ? "属性" : "引用";
                EClass definingClass = feature.getEContainingClass();
                log.info("  - {} [{}]: {} (来自 {})", feature.getName(), type, 
                    feature.getEType().getName(), definingClass.getName());
            }
            
            // 继承层次
            log.info("继承层次:");
            for (EClass superType : reqDefClass.getEAllSuperTypes()) {
                log.info("  <- {}", superType.getName());
            }
        }
        
        // 打印RequirementUsage的所有属性
        EClass reqUsageClass = (EClass) sysmlPackage.getEClassifier("RequirementUsage");
        if (reqUsageClass != null) {
            log.info("=== RequirementUsage 所有属性 ===");
            
            // 自身属性
            log.info("自身属性 ({}个):", reqUsageClass.getEAttributes().size());
            for (EAttribute attr : reqUsageClass.getEAttributes()) {
                log.info("  - {}: {} ({})", attr.getName(), attr.getEType().getName(),
                    attr.isRequired() ? "必填" : "可选");
            }
            
            // 所有结构特征
            log.info("所有结构特征 ({}个):", reqUsageClass.getEAllStructuralFeatures().size());
            for (EStructuralFeature feature : reqUsageClass.getEAllStructuralFeatures()) {
                String type = feature instanceof EAttribute ? "属性" : "引用";
                EClass definingClass = feature.getEContainingClass();
                log.info("  - {} [{}]: {} (来自 {})", feature.getName(), type,
                    feature.getEType().getName(), definingClass.getName());
            }
            
            // 继承层次
            log.info("继承层次:");
            for (EClass superType : reqUsageClass.getEAllSuperTypes()) {
                log.info("  <- {}", superType.getName());
            }
        }
    }
    
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
        // REQ-TEXT-SIMPLE-001-1: 使用简化方案
        // reqId → declaredShortName (简短名称)
        setAttributeIfExists(reqDef, "declaredShortName", reqId);
        setAttributeIfExists(reqDef, "reqId", reqId);  // 同时设置reqId字段
        
        // name/text → declaredName (描述文本)
        if (text != null && !text.isEmpty()) {
            setAttributeIfExists(reqDef, "declaredName", text);
        } else if (name != null && !name.isEmpty()) {
            setAttributeIfExists(reqDef, "declaredName", name);
        }
        
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
        // REQ-TEXT-SIMPLE-001-1: 使用简化方案
        setAttributeIfExists(reqUsage, "declaredShortName", name);  // name作为简短名称
        if (text != null && !text.isEmpty()) {
            setAttributeIfExists(reqUsage, "declaredName", text);  // text作为描述
        }
        
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
     * REQ-TEXT-SIMPLE-001-3: 不再使用Documentation对象，改用declaredName
     * @deprecated 使用declaredName字段替代
     */
    @Deprecated
    private void setDocumentation(EObject eObject, String text) {
        log.debug("setDocumentation: 开始处理文档字段，目标类型: {}, 文本: {}", eObject.eClass().getName(), text);
        
        // 检查text字段类型
        EClass eClass = eObject.eClass();
        EStructuralFeature textFeature = eClass.getEStructuralFeature("text");
        if (textFeature != null) {
            log.debug("发现text字段: 类型={}, 多值={}, EClass={}", 
                textFeature.getEType().getName(), 
                textFeature.isMany(), 
                textFeature.getEType().getInstanceClassName());
        }
        
        // 首先尝试直接设置body或text字段
        if (!setAttributeIfExists(eObject, "body", text) && 
            !setAttributeIfExists(eObject, "text", text)) {
            
            // 如果没有直接字段，查找documentation相关字段
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
            "requirementDefinition",  // SysML 2.0标准字段，优先使用
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
                // 处理多值字段
                if (feature.isMany()) {
                    @SuppressWarnings("unchecked")
                    List<Object> list = (List<Object>) eObject.eGet(feature);
                    list.clear(); // 清除现有值
                    
                    if (value instanceof String) {
                        list.add(value); // 将字符串添加到列表
                        log.debug("设置多值属性 {}.{} = [{}]", eClass.getName(), attributeName, value);
                    } else if (value instanceof List) {
                        list.addAll((List<?>) value);
                        log.debug("设置多值属性 {}.{} = {}", eClass.getName(), attributeName, value);
                    }
                } else {
                    // 单值字段直接设置
                    eObject.eSet(feature, value);
                    log.trace("设置属性 {}.{} = {}", eClass.getName(), attributeName, value);
                }
                return true;
            } catch (Exception e) {
                log.debug("无法设置属性 {}.{}: {}", eClass.getName(), attributeName, e.getMessage());
            }
        }
        
        // 如果直接字段不存在，尝试从所有继承的字段中查找
        for (EStructuralFeature inheritedFeature : eClass.getEAllStructuralFeatures()) {
            if (attributeName.equals(inheritedFeature.getName()) && inheritedFeature instanceof EAttribute) {
                try {
                    // 处理多值字段
                    if (inheritedFeature.isMany()) {
                        @SuppressWarnings("unchecked")
                        List<Object> list = (List<Object>) eObject.eGet(inheritedFeature);
                        list.clear(); // 清除现有值
                        
                        if (value instanceof String) {
                            list.add(value); // 将字符串添加到列表
                            log.debug("设置继承多值属性 {}.{} = [{}]", eClass.getName(), attributeName, value);
                        } else if (value instanceof List) {
                            list.addAll((List<?>) value);
                            log.debug("设置继承多值属性 {}.{} = {}", eClass.getName(), attributeName, value);
                        }
                    } else {
                        // 单值字段直接设置
                        eObject.eSet(inheritedFeature, value);
                        log.trace("设置继承属性 {}.{} = {}", eClass.getName(), attributeName, value);
                    }
                    return true;
                } catch (Exception e) {
                    log.debug("无法设置继承属性 {}.{}: {}", eClass.getName(), attributeName, e.getMessage());
                }
            }
        }
        
        return false;
    }
    
    /**
     * 【REQ-TDD-001-1】设置EMF引用字段（将ID字符串转换为实际引用）
     */
    public boolean setReferenceIfExists(EObject eObject, String referenceName, String targetId) {
        log.debug("尝试设置引用: {}.{} -> {}", eObject.eClass().getName(), referenceName, targetId);
        
        EClass eClass = eObject.eClass();
        EStructuralFeature feature = eClass.getEStructuralFeature(referenceName);
        log.debug("查找到特征: {} (类型: {})", feature, 
            feature != null ? feature.getClass().getSimpleName() : "null");
        
        if (feature != null && feature instanceof EReference) {
            EReference reference = (EReference) feature;
            log.debug("确认是EReference类型，目标类型: {}", reference.getEType().getName());
            
            try {
                // 查找目标对象
                EObject targetObject = findObjectById(targetId);
                if (targetObject != null) {
                    log.debug("找到目标对象: {} (类型: {})", targetId, targetObject.eClass().getName());
                    eObject.eSet(feature, targetObject);
                    log.info("成功设置EMF引用: {}.{} -> {} ({})", 
                        eClass.getName(), referenceName, targetId, targetObject.eClass().getName());
                    return true;
                } else {
                    // 如果找不到目标对象
                    log.warn("引用目标对象未找到: {} (ObjectFinder是否设置: {}), 字段: {}.{}", 
                        targetId, objectFinder != null, eClass.getName(), referenceName);
                    return false;
                }
            } catch (Exception e) {
                log.error("设置引用失败 {}.{}: {}", eClass.getName(), referenceName, e.getMessage(), e);
            }
        } else {
            log.warn("特征 {} 不是EReference类型，而是: {}", referenceName, 
                feature != null ? feature.getClass().getSimpleName() : "null");
        }
        
        // 如果直接字段不存在，尝试从所有继承的字段中查找
        for (EStructuralFeature inheritedFeature : eClass.getEAllStructuralFeatures()) {
            if (referenceName.equals(inheritedFeature.getName()) && inheritedFeature instanceof EReference) {
                log.debug("从继承特征中找到引用: {}", inheritedFeature.getName());
                try {
                    EObject targetObject = findObjectById(targetId);
                    if (targetObject != null) {
                        eObject.eSet(inheritedFeature, targetObject);
                        log.info("成功设置继承引用 {}.{} -> {} (对象引用)", 
                            eClass.getName(), referenceName, targetId);
                        return true;
                    } else {
                        log.warn("继承引用的目标对象未找到: {}", targetId);
                    }
                } catch (Exception e) {
                    log.error("无法设置继承引用 {}.{}: {}", eClass.getName(), referenceName, e.getMessage(), e);
                }
            }
        }
        
        return false;
    }
    
    
    /**
     * 根据elementId查找对象
     * 这个方法需要在创建时提供Resource上下文
     * 在当前架构中，对象查找应该由UniversalElementService提供
     */
    private EObject findObjectById(String elementId) {
        // 在当前设计中，PilotEMFService不直接访问Resource
        // 这个查找操作应该在UniversalElementService层面处理
        // 返回null，让UniversalElementService通过setObjectFinder提供查找能力
        log.trace("查找对象ID: {} (需要Resource上下文，等待外部提供)", elementId);
        return objectFinder != null ? objectFinder.findById(elementId) : null;
    }
    
    /**
     * 对象查找器接口
     * 由上层Service（UniversalElementService）实现并注入
     */
    @FunctionalInterface
    public interface ObjectFinder {
        EObject findById(String elementId);
    }
    
    private ObjectFinder objectFinder;
    
    /**
     * 设置对象查找器
     * 由UniversalElementService在操作时注入
     */
    public void setObjectFinder(ObjectFinder finder) {
        this.objectFinder = finder;
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
            
            log.debug("处理属性: {} = {} (类型: {})", attrName, value, 
                value != null ? value.getClass().getSimpleName() : "null");
            
            // REQ-TEXT-SIMPLE-001-3: documentation映射到declaredName
            if ("documentation".equals(attrName) && value instanceof String) {
                setAttributeIfExists(element, "declaredName", (String) value);
            } else if ("requirementDefinition".equals(attrName) && value instanceof String) {
                // 【REQ-TDD-001-1】特殊处理requirementDefinition EMF引用字段
                log.info("检测到requirementDefinition字段，开始设置EMF引用: {}", value);
                log.info("当前ObjectFinder是否设置: {}", objectFinder != null);
                boolean refSet = setReferenceIfExists(element, attrName, (String) value);
                if (!refSet) {
                    // 如果无法设置为EMF引用，这是一个错误（应该严格按EMF标准）
                    log.error("无法为RequirementUsage设置requirementDefinition引用: {}", value);
                    throw new IllegalArgumentException("引用目标对象未找到: " + value);
                }
                // 验证引用是否真的设置成功
                EStructuralFeature refFeature = element.eClass().getEStructuralFeature("requirementDefinition");
                Object refValue = refFeature != null ? element.eGet(refFeature) : null;
                log.info("成功设置requirementDefinition引用: {} -> {} (类型: {})", 
                    value, refValue, refValue != null ? refValue.getClass().getName() : "null");
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
            
            // REQ-TEXT-SIMPLE-001-3: documentation映射到declaredName
            if ("documentation".equals(attributeName) && value instanceof String) {
                setAttributeIfExists(eObject, "declaredName", (String) value);
                continue;
            }
            
            // 【REQ-TDD-001-1】特殊处理requirementDefinition EMF引用字段
            if ("requirementDefinition".equals(attributeName) && value instanceof String) {
                log.info("检测到requirementDefinition字段，开始设置EMF引用: {}", value);
                log.info("当前ObjectFinder是否设置: {}", objectFinder != null);
                boolean refSet = setReferenceIfExists(eObject, attributeName, (String) value);
                if (!refSet) {
                    // 如果无法设置为EMF引用，这是一个错误（应该严格按EMF标准）
                    log.error("无法为RequirementUsage设置requirementDefinition引用: {}", value);
                    throw new IllegalArgumentException("引用目标对象未找到: " + value);
                }
                // 验证引用是否真的设置成功
                EStructuralFeature refFeature = eObject.eClass().getEStructuralFeature("requirementDefinition");
                Object refValue = refFeature != null ? eObject.eGet(refFeature) : null;
                log.info("成功设置requirementDefinition引用: {} -> {} (类型: {})", 
                    value, refValue, refValue != null ? refValue.getClass().getName() : "null");
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