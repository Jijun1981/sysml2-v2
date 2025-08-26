package com.sysml.mvp.service;

import com.sysml.mvp.dto.ElementDTO;
import com.sysml.mvp.model.EMFModelRegistry;
import com.sysml.mvp.repository.FileModelRepository;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 通用元素服务（内部工具层）
 * 
 * 需求实现：
 * - REQ-B5-3: 内部EMF工具层 - 提供通用元素操作能力，/api/v1/elements仅支持读
 * - REQ-D0-1: 通用元素数据API - 支持查询任意SysML类型
 * - REQ-B2-4: DTO选择性映射 - 支持PATCH部分更新
 * - REQ-B2-1: 创建API - 支持动态创建任意类型
 * 
 * 设计说明：
 * 1. 这是内部服务层，不直接暴露为Controller
 * 2. 领域服务（RequirementService等）委托给此服务执行通用操作
 * 3. 所有EMF操作最终委托给PilotEMFService
 * 4. 支持182个SysML EClass类型的通用CRUD
 */
@Service
public class UniversalElementService {
    
    private final PilotEMFService pilotEMFService;
    private final FileModelRepository fileModelRepository;
    private final EMFModelRegistry emfModelRegistry;
    
    public UniversalElementService(
            PilotEMFService pilotEMFService,
            FileModelRepository fileModelRepository,
            EMFModelRegistry emfModelRegistry) {
        this.pilotEMFService = pilotEMFService;
        this.fileModelRepository = fileModelRepository;
        this.emfModelRegistry = emfModelRegistry;
    }
    
    /**
     * 【REQ-B2-1】创建任意类型的SysML元素
     * @param eClassName SysML类型名称（如RequirementDefinition）
     * @param attributes 元素属性Map
     * @return 创建的元素DTO
     * @throws IllegalArgumentException 如果缺少elementId
     */
    public ElementDTO createElement(String eClassName, Map<String, Object> attributes) {
        // 【REQ-B2-1】验证必填字段
        if (!attributes.containsKey("elementId")) {
            throw new IllegalArgumentException("elementId is required");
        }
        
        // 使用PilotEMFService创建元素（它内部会处理EClass查找）
        EObject eObject = pilotEMFService.createElement(eClassName, attributes);
        
        // 添加到Resource（假设默认项目ID为"default"）
        String projectId = "default";
        Resource resource = fileModelRepository.loadProject(projectId);
        if (resource != null) {
            resource.getContents().add(eObject);
        } else {
            throw new IllegalStateException("Project not found: " + projectId);
        }
        
        // 保存
        fileModelRepository.saveProject(projectId, resource);
        
        // 转换为DTO返回
        return toDTO(eObject);
    }
    
    /**
     * 【REQ-D0-1】查询元素
     * @param type 可选的类型过滤器（null表示查询所有）
     * @return 元素列表
     */
    public List<ElementDTO> queryElements(String type) {
        String projectId = "default";
        Resource resource = fileModelRepository.loadProject(projectId);
        if (resource == null) {
            return new ArrayList<>();
        }
        List<EObject> contents = resource.getContents();
        
        return contents.stream()
            .filter(obj -> type == null || obj.eClass().getName().equals(type))
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
    
    /**
     * 【REQ-E1-3】获取所有元素
     * 用于静态验证，返回模型中所有元素
     * @return 所有元素列表
     */
    public List<ElementDTO> getAllElements() {
        return queryElements(null);
    }
    
    /**
     * 【REQ-B2-4】PATCH更新元素部分属性
     * @param elementId 元素ID
     * @param updates 要更新的属性Map
     * @return 更新后的元素DTO
     */
    public ElementDTO patchElement(String elementId, Map<String, Object> updates) {
        EObject eObject = findEObjectById(elementId);
        if (eObject == null) {
            return null;
        }
        
        // 只更新指定的属性
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            pilotEMFService.setAttributeIfExists(eObject, entry.getKey(), entry.getValue());
        }
        
        // 保存
        String projectId = "default";
        Resource resource = fileModelRepository.loadProject(projectId);
        fileModelRepository.saveProject(projectId, resource);
        
        return toDTO(eObject);
    }
    
    /**
     * 【REQ-B5-3】删除元素
     * @param elementId 元素ID
     * @return 是否删除成功
     */
    public boolean deleteElement(String elementId) {
        EObject eObject = findEObjectById(elementId);
        if (eObject == null) {
            return false;
        }
        
        String projectId = "default";
        Resource resource = fileModelRepository.loadProject(projectId);
        if (resource != null) {
            resource.getContents().remove(eObject);
            fileModelRepository.saveProject(projectId, resource);
            return true;
        }
        return false;
    }
    
    /**
     * 【REQ-B5-3】根据ID查找元素
     * @param elementId 元素ID
     * @return 元素DTO，如果不存在返回null
     */
    public ElementDTO findElementById(String elementId) {
        EObject eObject = findEObjectById(elementId);
        return eObject != null ? toDTO(eObject) : null;
    }
    
    /**
     * 【REQ-D0-1】将EMF对象转换为DTO
     * 保留所有属性到properties Map中
     * @param eObject EMF对象
     * @return ElementDTO
     */
    public ElementDTO toDTO(EObject eObject) {
        if (eObject == null) {
            return null;
        }
        
        ElementDTO dto = new ElementDTO();
        
        // 设置eClass
        dto.setEClass(eObject.eClass().getName());
        
        // 设置elementId
        Object elementId = pilotEMFService.getAttributeValue(eObject, "elementId");
        if (elementId != null) {
            dto.setElementId(elementId.toString());
        }
        
        // 获取所有属性并设置到properties
        Map<String, Object> allAttributes = getAllAttributes(eObject);
        for (Map.Entry<String, Object> entry : allAttributes.entrySet()) {
            // elementId已经单独设置，不需要再加入properties
            if (!"elementId".equals(entry.getKey())) {
                dto.setProperty(entry.getKey(), entry.getValue());
            }
        }
        
        return dto;
    }
    
    /**
     * 内部方法：根据ID查找EMF对象
     */
    private EObject findEObjectById(String elementId) {
        String projectId = "default";
        Resource resource = fileModelRepository.loadProject(projectId);
        if (resource == null) {
            return null;
        }
        List<EObject> contents = resource.getContents();
        
        for (EObject obj : contents) {
            Object id = pilotEMFService.getAttributeValue(obj, "elementId");
            if (elementId.equals(id)) {
                return obj;
            }
        }
        return null;
    }
    
    /**
     * 内部方法：获取EMF对象的所有属性
     * 兼容PilotEMFService可能没有getAllAttributes方法的情况
     */
    private Map<String, Object> getAllAttributes(EObject eObject) {
        Map<String, Object> attributes = new HashMap<>();
        
        // 遍历所有属性特征
        for (EStructuralFeature feature : eObject.eClass().getEAllStructuralFeatures()) {
            if (feature.isDerived() || feature.isTransient()) {
                continue; // 跳过派生和瞬态属性
            }
            
            String name = feature.getName();
            Object value = pilotEMFService.getAttributeValue(eObject, name);
            
            // 只添加可序列化的值，过滤掉EMF内部对象
            if (value != null && isSerializableValue(value)) {
                attributes.put(name, value);
            }
        }
        
        return attributes;
    }
    
    /**
     * 检查值是否可以被Jackson安全序列化
     * 过滤掉EMF内部对象如Enumerator等
     */
    private boolean isSerializableValue(Object value) {
        // 过滤EMF内部类型
        String className = value.getClass().getName();
        if (className.contains("eclipse.emf") && 
            (className.contains("EEnumLiteralImpl") || 
             className.contains("Enumerator") ||
             className.contains("EList") ||
             className.contains("EObject"))) {
            return false;
        }
        
        // 只保留基本类型和集合类型
        return value instanceof String || 
               value instanceof Number ||
               value instanceof Boolean ||
               value instanceof Collection ||
               value instanceof Map;
    }
}