package com.sysml.mvp.service;

import com.sysml.mvp.dto.ElementDTO;
import com.sysml.mvp.model.EMFModelRegistry;
import com.sysml.mvp.repository.FileModelRepository;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 通用元素服务
 * 实现REQ-B5-1, REQ-B5-2, REQ-B5-3
 * 
 * 通过动态EMF实现零代码扩展
 */
@Service
public class UniversalElementService {
    
    @Autowired
    private PilotEMFService pilotService;
    
    @Autowired
    private FileModelRepository repository;
    
    @Autowired
    private EMFModelRegistry modelRegistry;
    
    // 默认项目ID（用于MVP）
    private static final String DEFAULT_PROJECT_ID = "default";
    
    /**
     * REQ-B5-1: 创建任意类型的元素
     */
    public ElementDTO createElement(String eClassName, Map<String, Object> attributes) {
        // 使用PilotEMFService的工厂方法
        EObject element = pilotService.createElement(eClassName, attributes);
        
        // 保存到资源
        Resource resource = repository.loadProject(DEFAULT_PROJECT_ID);
        resource.getContents().add(element);
        repository.saveProject(DEFAULT_PROJECT_ID, resource);
        
        // 转换为DTO
        return convertToDTO(element);
    }
    
    /**
     * REQ-B5-2: 按类型查询元素（返回List）
     */
    public List<ElementDTO> queryElements(String type) {
        Resource resource = repository.loadProject(DEFAULT_PROJECT_ID);
        
        List<ElementDTO> result = new ArrayList<>();
        
        // 遍历所有内容（包括嵌套）
        Iterator<EObject> iter = resource.getAllContents();
        while (iter.hasNext()) {
            EObject obj = iter.next();
            
            // 如果指定了类型，过滤
            if (type == null || type.isEmpty() || obj.eClass().getName().equals(type)) {
                result.add(convertToDTO(obj));
            }
        }
        
        return result;
    }
    
    /**
     * REQ-B5-2: 按类型查询元素（支持分页）
     */
    public Page<ElementDTO> queryElements(String type, Pageable pageable) {
        List<ElementDTO> allElements = queryElements(type);
        
        // 手动分页
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allElements.size());
        
        List<ElementDTO> pageContent = allElements.subList(start, end);
        
        return new PageImpl<>(pageContent, pageable, allElements.size());
    }
    
    /**
     * REQ-B5-3: PATCH更新元素
     */
    public ElementDTO patchElement(String elementId, Map<String, Object> updates) {
        EObject element = findElementById(elementId);
        if (element == null) {
            return null;
        }
        
        // 使用PilotEMFService的合并方法
        pilotService.mergeAttributes(element, updates);
        
        // 保存
        Resource resource = repository.loadProject(DEFAULT_PROJECT_ID);
        repository.saveProject(DEFAULT_PROJECT_ID, resource);
        
        return convertToDTO(element);
    }
    
    /**
     * 根据ID获取元素
     */
    public ElementDTO getElementById(String elementId) {
        EObject element = findElementById(elementId);
        return element != null ? convertToDTO(element) : null;
    }
    
    /**
     * 删除元素
     */
    public boolean deleteElement(String elementId) {
        EObject element = findElementById(elementId);
        if (element == null) {
            return false;
        }
        
        // 从容器中移除
        if (element.eContainer() != null) {
            EcoreUtil.remove(element);
        } else {
            // 如果是根元素，从资源中移除
            Resource resource = element.eResource();
            if (resource != null) {
                resource.getContents().remove(element);
            }
        }
        
        Resource resource = repository.loadProject(DEFAULT_PROJECT_ID);
        repository.saveProject(DEFAULT_PROJECT_ID, resource);
        return true;
    }
    
    /**
     * 根据elementId查找元素
     */
    private EObject findElementById(String elementId) {
        Resource resource = repository.loadProject(DEFAULT_PROJECT_ID);
        
        Iterator<EObject> iter = resource.getAllContents();
        while (iter.hasNext()) {
            EObject obj = iter.next();
            Object idObj = pilotService.getAttributeValue(obj, "elementId");
            String id = idObj != null ? idObj.toString() : null;
            if (elementId.equals(id)) {
                return obj;
            }
        }
        
        // 也检查根元素
        for (EObject root : resource.getContents()) {
            Object idObj = pilotService.getAttributeValue(root, "elementId");
            String id = idObj != null ? idObj.toString() : null;
            if (elementId.equals(id)) {
                return root;
            }
        }
        
        return null;
    }
    
    /**
     * 将EObject转换为通用DTO
     */
    private ElementDTO convertToDTO(EObject eObject) {
        ElementDTO dto = new ElementDTO();
        
        // 设置eClass
        dto.setEClass(eObject.eClass().getName());
        
        // 设置elementId
        Object idObj = pilotService.getAttributeValue(eObject, "elementId");
        String elementId = idObj != null ? idObj.toString() : null;
        dto.setElementId(elementId);
        
        // 复制所有属性
        for (EAttribute attr : eObject.eClass().getEAllAttributes()) {
            String name = attr.getName();
            Object value = eObject.eGet(attr);
            
            // 跳过null值和elementId（已经设置）
            if (value != null && !"elementId".equals(name)) {
                // 处理Enumerator类型（EMF枚举）
                if (value instanceof org.eclipse.emf.common.util.Enumerator) {
                    // 转换为字符串避免序列化问题
                    dto.setProperty(name, ((org.eclipse.emf.common.util.Enumerator) value).getLiteral());
                } 
                // 处理List类型的属性
                else if (value instanceof List) {
                    List<?> list = (List<?>) value;
                    if (!list.isEmpty()) {
                        // 检查列表中是否有Enumerator
                        List<Object> processedList = new ArrayList<>();
                        for (Object item : list) {
                            if (item instanceof org.eclipse.emf.common.util.Enumerator) {
                                processedList.add(((org.eclipse.emf.common.util.Enumerator) item).getLiteral());
                            } else {
                                processedList.add(item);
                            }
                        }
                        dto.setProperty(name, processedList);
                    }
                } else {
                    dto.setProperty(name, value);
                }
            }
        }
        
        // 复制简单引用（只包含ID，不展开）
        for (EReference ref : eObject.eClass().getEAllReferences()) {
            if (!ref.isContainment() && !ref.isMany()) {
                EObject target = (EObject) eObject.eGet(ref);
                if (target != null) {
                    Object targetIdObj = pilotService.getAttributeValue(target, "elementId");
                    String targetId = targetIdObj != null ? targetIdObj.toString() : null;
                    if (targetId != null) {
                        dto.setProperty(ref.getName() + "Id", targetId);
                    }
                }
            }
        }
        
        return dto;
    }
}