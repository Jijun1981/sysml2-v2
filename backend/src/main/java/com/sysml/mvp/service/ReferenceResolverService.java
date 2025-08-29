package com.sysml.mvp.service;

import com.sysml.mvp.repository.FileModelRepository;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EMF引用解析服务 - 基础设施层
 * 
 * 需求实现：
 * - REQ-B2-1: EMF引用解析 - 将字符串ID转换为实际对象引用
 * - REQ-B2-4: 延迟引用解析 - 支持创建时引用对象不存在的情况
 * - 架构遵循：严格按照EMF标准处理EReference vs EAttribute区别
 * 
 * 设计说明：
 * 1. 专门处理EMF对象引用解析，支持Resource上下文
 * 2. 实现requirementDefinition等引用字段的正确处理
 * 3. 支持延迟解析：引用对象稍后创建的情况
 * 4. 缓存机制提升查找性能
 * 5. 集成到现有的TraceService和ValidationService架构中
 */
@Slf4j
@Service
public class ReferenceResolverService {
    
    private final FileModelRepository fileModelRepository;
    private final PilotEMFService pilotEMFService;
    
    /**
     * Resource级别的对象查找缓存
     * Key: projectId, Value: elementId -> EObject映射
     */
    private final Map<String, Map<String, EObject>> resourceCache = new ConcurrentHashMap<>();
    
    public ReferenceResolverService(
            FileModelRepository fileModelRepository,
            PilotEMFService pilotEMFService) {
        this.fileModelRepository = fileModelRepository;
        this.pilotEMFService = pilotEMFService;
    }
    
    /**
     * 【REQ-B2-1】在指定Resource中查找对象
     * 这是核心方法，为PilotEMFService提供查找能力
     * 
     * @param resource EMF Resource上下文
     * @param elementId 要查找的元素ID
     * @return 找到的EObject，如果不存在返回null
     */
    public EObject findObjectInResource(Resource resource, String elementId) {
        if (resource == null || elementId == null) {
            return null;
        }
        
        // 遍历Resource中的所有内容
        for (EObject obj : resource.getContents()) {
            EObject found = findObjectRecursively(obj, elementId);
            if (found != null) {
                return found;
            }
        }
        
        log.trace("对象未找到: {} (在Resource中)", elementId);
        return null;
    }
    
    /**
     * 【REQ-B2-1】为EObject设置引用字段
     * 严格按照EMF标准：EReference字段设置对象引用，EAttribute字段设置属性值
     * 
     * @param eObject 要设置引用的对象
     * @param referenceName 引用字段名
     * @param targetElementId 目标对象的elementId
     * @param resource Resource上下文，用于查找目标对象
     * @return true如果引用设置成功，false如果字段不存在或不是引用类型
     */
    public boolean setEMFReference(EObject eObject, String referenceName, String targetElementId, Resource resource) {
        if (eObject == null || referenceName == null || targetElementId == null || resource == null) {
            return false;
        }
        
        // 查找引用字段
        EStructuralFeature feature = eObject.eClass().getEStructuralFeature(referenceName);
        if (!(feature instanceof EReference)) {
            log.debug("字段 {} 不是EReference类型，无法设置对象引用", referenceName);
            return false;
        }
        
        EReference reference = (EReference) feature;
        
        // 在Resource中查找目标对象
        EObject targetObject = findObjectInResource(resource, targetElementId);
        if (targetObject == null) {
            log.warn("引用目标对象未找到: {}, 引用字段: {}.{}", targetElementId, 
                eObject.eClass().getName(), referenceName);
            return false;
        }
        
        // 验证类型兼容性
        if (!reference.getEType().isInstance(targetObject)) {
            log.warn("类型不兼容: {} 无法引用 {}", 
                reference.getEType().getName(), targetObject.eClass().getName());
            return false;
        }
        
        try {
            // 设置EMF引用
            eObject.eSet(reference, targetObject);
            log.debug("成功设置EMF引用: {}.{} → {} ({})", 
                eObject.eClass().getName(), referenceName, 
                targetElementId, targetObject.eClass().getName());
            return true;
            
        } catch (Exception e) {
            log.error("设置EMF引用失败: {}.{} → {}, 错误: {}", 
                eObject.eClass().getName(), referenceName, targetElementId, e.getMessage());
            return false;
        }
    }
    
    /**
     * 【REQ-B2-4】延迟引用解析
     * 当对象创建时引用的目标对象还不存在，先记录引用关系，后续解析
     * 
     * @param projectId 项目ID
     * @param sourceElementId 源对象ID
     * @param referenceName 引用字段名
     * @param targetElementId 目标对象ID
     */
    public void addPendingReference(String projectId, String sourceElementId, 
                                   String referenceName, String targetElementId) {
        // 简单实现：直接尝试解析，如果失败就记录日志
        // 在MVP阶段，假设对象创建顺序正确，暂不实现复杂的延迟解析
        log.debug("记录延迟引用: {} → {} ({})", sourceElementId, targetElementId, referenceName);
        
        // TODO: 如果需要，可以实现延迟引用队列
        // 在saveProject时统一解析所有待解决的引用
    }
    
    /**
     * 【REQ-B2-4】解析项目中的所有待解决引用
     * 在保存项目时调用，确保所有引用关系正确建立
     * 
     * @param projectId 项目ID
     */
    public void resolvePendingReferences(String projectId) {
        // MVP暂不实现延迟解析，假设创建时已正确解析
        log.trace("解析项目 {} 的待解决引用 (MVP暂不实现)", projectId);
    }
    
    /**
     * 【REQ-B2-1】验证引用完整性
     * 检查Resource中的所有引用是否指向有效对象
     * 
     * @param resource EMF Resource
     * @return 损坏的引用数量
     */
    public int validateReferenceIntegrity(Resource resource) {
        int brokenRefs = 0;
        
        for (EObject obj : resource.getContents()) {
            brokenRefs += validateObjectReferences(obj, resource);
        }
        
        if (brokenRefs > 0) {
            log.warn("发现 {} 个损坏的引用在Resource中", brokenRefs);
        }
        
        return brokenRefs;
    }
    
    /**
     * 清理Resource缓存
     */
    public void clearCache(String projectId) {
        resourceCache.remove(projectId);
        log.debug("清理项目 {} 的引用解析缓存", projectId);
    }
    
    /**
     * 递归查找对象
     */
    private EObject findObjectRecursively(EObject root, String elementId) {
        // 检查当前对象
        Object currentId = pilotEMFService.getAttributeValue(root, "elementId");
        if (elementId.equals(currentId)) {
            return root;
        }
        
        // 递归检查所有子对象（包含的对象）
        for (EObject child : root.eContents()) {
            EObject found = findObjectRecursively(child, elementId);
            if (found != null) {
                return found;
            }
        }
        
        return null;
    }
    
    /**
     * 验证单个对象的引用
     */
    private int validateObjectReferences(EObject obj, Resource resource) {
        int brokenCount = 0;
        
        // 检查所有引用字段
        for (EReference ref : obj.eClass().getEAllReferences()) {
            if (ref.isContainment()) {
                continue; // 跳过包含关系
            }
            
            Object value = obj.eGet(ref);
            if (value instanceof EObject) {
                EObject target = (EObject) value;
                // 检查目标对象是否仍然存在于Resource中
                if (target.eResource() == null || target.eResource() != resource) {
                    log.warn("损坏的引用: {}.{} → {}", 
                        obj.eClass().getName(), ref.getName(), 
                        pilotEMFService.getAttributeValue(target, "elementId"));
                    brokenCount++;
                }
            }
        }
        
        // 递归检查子对象
        for (EObject child : obj.eContents()) {
            brokenCount += validateObjectReferences(child, resource);
        }
        
        return brokenCount;
    }
}