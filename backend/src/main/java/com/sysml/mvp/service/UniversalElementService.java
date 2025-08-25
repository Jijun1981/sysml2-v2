package com.sysml.mvp.service;

import com.sysml.mvp.model.EMFModelRegistry;
import com.sysml.mvp.repository.FileModelRepository;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.common.util.EList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 通用元素服务 - 完全基于Map，避免DTO转换问题
 * 实现REQ-B5-1到REQ-B5-4
 * 
 * 核心设计：直接返回Map<String, Object>，避免EMF类型转换错误
 */
@Service
public class UniversalElementService {
    
    @Autowired
    private FileModelRepository repository;
    
    @Autowired
    private EMFModelRegistry modelRegistry;
    
    /**
     * REQ-B5-1: 创建任意类型的元素
     * 
     * @param projectId 项目ID
     * @param request 包含eClass和其他属性的Map
     * @return 创建的元素Map
     */
    public Map<String, Object> createElement(String projectId, Map<String, Object> request) {
        String eClassName = (String) request.get("eClass");
        if (eClassName == null || eClassName.isEmpty()) {
            throw new IllegalArgumentException("eClass is required");
        }
        
        // 获取EClass
        EPackage ePackage = modelRegistry.getSysMLPackage();
        EClass eClass = (EClass) ePackage.getEClassifier(eClassName);
        if (eClass == null) {
            throw new IllegalArgumentException("Unknown eClass: " + eClassName);
        }
        
        // 创建实例
        EObject element = ePackage.getEFactoryInstance().create(eClass);
        
        // 设置属性
        for (Map.Entry<String, Object> entry : request.entrySet()) {
            String key = entry.getKey();
            if ("eClass".equals(key)) continue;
            
            EStructuralFeature feature = eClass.getEStructuralFeature(key);
            if (feature != null && !feature.isDerived() && feature.isChangeable()) {
                try {
                    element.eSet(feature, entry.getValue());
                } catch (Exception e) {
                    // 忽略设置失败的属性
                    System.err.println("Failed to set " + key + ": " + e.getMessage());
                }
            }
        }
        
        // 生成elementId
        String elementId = eClassName.toLowerCase() + "-" + UUID.randomUUID().toString();
        EStructuralFeature idFeature = eClass.getEStructuralFeature("elementId");
        if (idFeature != null) {
            element.eSet(idFeature, elementId);
        }
        
        // 保存到资源
        Resource resource = repository.loadProject(projectId);
        resource.getContents().add(element);
        repository.saveProject(projectId, resource);
        
        // 转换为Map
        return eObjectToMap(element);
    }
    
    /**
     * REQ-B5-2: 查询元素
     * 
     * @param projectId 项目ID
     * @param type 元素类型（可选）
     * @param params 查询参数
     * @return 元素列表
     */
    public List<Map<String, Object>> queryElements(String projectId, String type, Map<String, Object> params) {
        Resource resource = repository.loadProject(projectId);
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 遍历所有内容
        for (EObject obj : resource.getContents()) {
            addElementsToResult(obj, type, result);
        }
        
        // 分页处理（简化版）
        int page = (int) params.getOrDefault("page", 0);
        int size = (int) params.getOrDefault("size", 50);
        int start = page * size;
        int end = Math.min(start + size, result.size());
        
        if (start < result.size()) {
            return result.subList(start, end);
        }
        
        return result;
    }
    
    /**
     * 递归添加元素到结果集
     */
    private void addElementsToResult(EObject obj, String type, List<Map<String, Object>> result) {
        // 如果指定了类型，过滤
        if (type == null || type.isEmpty() || obj.eClass().getName().equals(type)) {
            result.add(eObjectToMapWithHierarchy(obj));
        }
        
        // 不再递归处理子元素 - 层次结构现在由eObjectToMapWithHierarchy处理
        // 这样可以保持父子包含关系
    }
    
    /**
     * 将EObject转换为Map，保持层次结构（内嵌子对象）
     */
    private Map<String, Object> eObjectToMapWithHierarchy(EObject obj) {
        Map<String, Object> map = new HashMap<>();
        
        // 添加eClass信息
        map.put("eClass", obj.eClass().getName());
        map.put("eclass", obj.eClass().getName()); // 兼容性
        
        // 遍历所有属性
        for (EStructuralFeature feature : obj.eClass().getEAllStructuralFeatures()) {
            try {
                if (!feature.isDerived() && obj.eIsSet(feature)) {
                    String name = feature.getName();
                    Object value = obj.eGet(feature);
                    
                    // 处理不同类型的值
                    if (value == null) {
                        map.put(name, null);
                    } else if (value instanceof EList) {
                        // EMF List类型 - 对于包含特性(containment)，内嵌完整对象
                        EList<?> eList = (EList<?>) value;
                        List<Object> list = new ArrayList<>();
                        for (Object item : eList) {
                            if (item instanceof EObject) {
                                EObject childObj = (EObject) item;
                                // 对于包含关系（如ownedFeature），递归内嵌完整对象
                                if (feature instanceof org.eclipse.emf.ecore.EReference && 
                                    ((org.eclipse.emf.ecore.EReference) feature).isContainment()) {
                                    list.add(eObjectToMapWithHierarchy(childObj));
                                } else {
                                    // 对于引用关系，使用$ref
                                    list.add(Map.of("$ref", getObjectId(childObj)));
                                }
                            } else {
                                list.add(item);
                            }
                        }
                        map.put(name, list);
                    } else if (value instanceof EObject) {
                        EObject childObj = (EObject) value;
                        // 对于包含关系，内嵌完整对象；对于引用关系，使用ID
                        if (feature instanceof org.eclipse.emf.ecore.EReference && 
                            ((org.eclipse.emf.ecore.EReference) feature).isContainment()) {
                            map.put(name, eObjectToMapWithHierarchy(childObj));
                        } else {
                            map.put(name, getObjectId(childObj));
                        }
                    } else if (value instanceof Enum) {
                        // 枚举类型
                        map.put(name, value.toString());
                    } else {
                        // 基本类型
                        map.put(name, value);
                    }
                }
            } catch (Exception e) {
                // 忽略无法访问的属性
                System.err.println("Failed to get feature " + feature.getName() + ": " + e.getMessage());
            }
        }
        
        return map;
    }
    
    /**
     * 获取单个元素
     */
    public Map<String, Object> getElementById(String projectId, String elementId) {
        Resource resource = repository.loadProject(projectId);
        
        for (EObject obj : resource.getContents()) {
            Map<String, Object> element = findElementById(obj, elementId);
            if (element != null) {
                return element;
            }
        }
        
        return null;
    }
    
    /**
     * 递归查找元素
     */
    private Map<String, Object> findElementById(EObject obj, String elementId) {
        EStructuralFeature idFeature = obj.eClass().getEStructuralFeature("elementId");
        if (idFeature != null) {
            Object id = obj.eGet(idFeature);
            if (elementId.equals(id)) {
                return eObjectToMap(obj);
            }
        }
        
        for (EObject child : obj.eContents()) {
            Map<String, Object> found = findElementById(child, elementId);
            if (found != null) {
                return found;
            }
        }
        
        return null;
    }
    
    /**
     * REQ-B5-3: 更新元素
     */
    public Map<String, Object> updateElement(String projectId, String elementId, Map<String, Object> updates) {
        Resource resource = repository.loadProject(projectId);
        EObject element = findEObjectById(resource, elementId);
        
        if (element == null) {
            return null;
        }
        
        // 更新属性
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String key = entry.getKey();
            EStructuralFeature feature = element.eClass().getEStructuralFeature(key);
            if (feature != null && !feature.isDerived() && feature.isChangeable()) {
                try {
                    element.eSet(feature, entry.getValue());
                } catch (Exception e) {
                    System.err.println("Failed to update " + key + ": " + e.getMessage());
                }
            }
        }
        
        // 保存
        repository.saveProject(projectId, resource);
        
        return eObjectToMap(element);
    }
    
    /**
     * 删除元素
     */
    public boolean deleteElement(String projectId, String elementId) {
        Resource resource = repository.loadProject(projectId);
        EObject element = findEObjectById(resource, elementId);
        
        if (element == null) {
            return false;
        }
        
        // 从容器中移除
        if (element.eContainer() != null) {
            Object container = element.eContainer().eGet(element.eContainingFeature());
            if (container instanceof EList) {
                ((EList<?>) container).remove(element);
            }
        } else {
            resource.getContents().remove(element);
        }
        
        // 保存
        repository.saveProject(projectId, resource);
        return true;
    }
    
    /**
     * 查找EObject
     */
    private EObject findEObjectById(Resource resource, String elementId) {
        for (EObject obj : resource.getContents()) {
            EObject found = findEObjectByIdRecursive(obj, elementId);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
    
    private EObject findEObjectByIdRecursive(EObject obj, String elementId) {
        EStructuralFeature idFeature = obj.eClass().getEStructuralFeature("elementId");
        if (idFeature != null) {
            Object id = obj.eGet(idFeature);
            if (elementId.equals(id)) {
                return obj;
            }
        }
        
        for (EObject child : obj.eContents()) {
            EObject found = findEObjectByIdRecursive(child, elementId);
            if (found != null) {
                return found;
            }
        }
        
        return null;
    }
    
    /**
     * 核心转换方法：EObject到Map
     * 处理EMF List类型，避免ClassCastException
     */
    private Map<String, Object> eObjectToMap(EObject obj) {
        Map<String, Object> map = new HashMap<>();
        
        // 添加eClass信息
        map.put("eClass", obj.eClass().getName());
        map.put("eclass", obj.eClass().getName()); // 兼容性
        
        // 遍历所有属性 - 使用更安全的方式
        for (EStructuralFeature feature : obj.eClass().getEAllStructuralFeatures()) {
            try {
                if (!feature.isDerived() && obj.eIsSet(feature)) {
                    String name = feature.getName();
                    Object value = obj.eGet(feature);
                    
                    // 处理不同类型的值
                    if (value == null) {
                        map.put(name, null);
                    } else if (value instanceof EList) {
                        // EMF List类型 - 转换为普通List
                        EList<?> eList = (EList<?>) value;
                        List<Object> list = new ArrayList<>();
                        for (Object item : eList) {
                            if (item instanceof EObject) {
                                // 递归转换，但要避免循环引用
                                list.add(Map.of("$ref", getObjectId((EObject) item)));
                            } else {
                                list.add(item);
                            }
                        }
                        map.put(name, list);
                    } else if (value instanceof EObject) {
                        // 引用类型 - 只返回ID
                        map.put(name, getObjectId((EObject) value));
                    } else if (value instanceof Enum) {
                        // 枚举类型
                        map.put(name, value.toString());
                    } else {
                        // 基本类型
                        map.put(name, value);
                    }
                }
            } catch (Exception e) {
                // 忽略无法访问的属性
                System.err.println("Failed to get feature " + feature.getName() + ": " + e.getMessage());
            }
        }
        
        return map;
    }
    
    /**
     * 获取对象ID
     */
    private String getObjectId(EObject obj) {
        if (obj == null) return null;
        
        EStructuralFeature idFeature = obj.eClass().getEStructuralFeature("elementId");
        if (idFeature != null) {
            Object id = obj.eGet(idFeature);
            if (id != null) return id.toString();
        }
        
        // 如果没有elementId，使用URI fragment
        Resource resource = obj.eResource();
        if (resource != null && resource.getURI() != null) {
            return resource.getURIFragment(obj);
        }
        
        // 最后的备选：使用hashCode
        return "obj-" + Integer.toHexString(obj.hashCode());
    }
}