package com.sysml.mvp.service;

import com.sysml.mvp.dto.RequirementDefinitionDTO;
import com.sysml.mvp.dto.RequirementUsageDTO;
import com.sysml.mvp.model.EMFModelRegistry;
import com.sysml.mvp.repository.FileModelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.eclipse.emf.common.util.EList;

/**
 * 需求服务层
 * 实现REQ-C1-1, REQ-C1-2, REQ-C1-3, REQ-C2-1, REQ-C2-2
 * 
 * @deprecated 此类已被UniversalElementService替代。
 * 新代码应使用UniversalElementService和PilotEMFService。
 * 此类仅为兼容性保留，计划在下个版本删除。
 */
@Deprecated
@Slf4j
@Service
@RequiredArgsConstructor
public class RequirementService {
    
    private final FileModelRepository repository;
    private final EMFModelRegistry modelRegistry;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    private static final String PROJECT_ID = "default"; // MVP版本使用固定项目ID
    
    /**
     * REQ-C1-1 & REQ-C2-1: 创建需求（定义或用法）
     */
    public RequirementDefinitionDTO createRequirement(RequirementDefinitionDTO.CreateRequest request) {
        if ("definition".equals(request.getType())) {
            return createDefinition(request);
        } else if ("usage".equals(request.getType())) {
            return createUsage(request);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type必须为definition或usage");
        }
    }
    
    /**
     * REQ-C1-1: 创建需求定义
     */
    private RequirementDefinitionDTO createDefinition(RequirementDefinitionDTO.CreateRequest request) {
        log.info("创建需求定义: reqId={}, name={}", request.getReqId(), request.getName());
        
        // 验证类型
        if (!"definition".equals(request.getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type必须为definition");
        }
        
        // 验证必填字段
        validateRequiredFields(request.getReqId(), request.getName(), request.getText());
        
        // 检查reqId唯一性 (REQ-C1-3)
        validateReqIdUniqueness(request.getReqId());
        
        // 加载或创建资源
        Resource resource = repository.loadProject(PROJECT_ID);
        
        // 使用modelRegistry工厂方法创建对象（REQ-B2-1, REQ-B2-2）
        EObject definition = modelRegistry.createRequirementDefinition();
        
        // 设置业务属性
        setEObjectAttribute(definition, "reqId", request.getReqId());
        setEObjectAttribute(definition, "name", request.getName());
        setEObjectAttribute(definition, "text", request.getText());
        setEObjectAttribute(definition, "doc", request.getDoc() != null ? request.getDoc() : "");
        // 处理tags - EMF期望的是EList，需要特殊处理
        if (request.getTags() != null) {
            EStructuralFeature tagsFeature = definition.eClass().getEStructuralFeature("tags");
            if (tagsFeature != null && tagsFeature.isMany()) {
                @SuppressWarnings("unchecked")
                EList<String> tagsList = (EList<String>) definition.eGet(tagsFeature);
                tagsList.clear();
                tagsList.addAll(request.getTags());
            }
        }
        
        // 添加到资源
        resource.getContents().add(definition);
        
        // 保存
        repository.saveProject(PROJECT_ID, resource);
        
        String generatedId = (String) getEObjectAttribute(definition, "id");
        log.info("需求定义创建成功: id={}", generatedId);
        return convertToDTO(definition);
    }
    
    /**
     * REQ-C2-1: 创建需求用法
     */
    private RequirementDefinitionDTO createUsage(RequirementDefinitionDTO.CreateRequest request) {
        log.info("创建需求用法: of={}, name={}", request.getOf(), request.getName());
        
        // 验证of参数存在
        if (request.getOf() == null || request.getOf().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "创建usage时of参数不能为空");
        }
        
        // 验证引用的definition存在
        validateDefinitionExists(request.getOf());
        
        // 验证必填字段（usage的name和text可以为空）
        // 只验证基本约束
        
        // 加载或创建资源
        Resource resource = repository.loadProject(PROJECT_ID);
        
        // 使用modelRegistry工厂方法创建对象
        EObject usage = modelRegistry.createRequirementUsage(request.getOf());
        
        // 设置业务属性
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            setEObjectAttribute(usage, "name", request.getName());
        }
        if (request.getText() != null && !request.getText().trim().isEmpty()) {
            setEObjectAttribute(usage, "text", request.getText());
        }
        // 处理tags - EMF期望的是EList，需要特殊处理
        if (request.getTags() != null) {
            EStructuralFeature tagsFeature = usage.eClass().getEStructuralFeature("tags");
            if (tagsFeature != null && tagsFeature.isMany()) {
                @SuppressWarnings("unchecked")
                EList<String> tagsList = (EList<String>) usage.eGet(tagsFeature);
                tagsList.clear();
                tagsList.addAll(request.getTags());
            }
        }
        
        // 添加到资源
        resource.getContents().add(usage);
        
        // 保存
        repository.saveProject(PROJECT_ID, resource);
        
        String generatedId = (String) getEObjectAttribute(usage, "id");
        log.info("需求用法创建成功: id={}", generatedId);
        return convertUsageToDTO(usage);
    }
    
    /**
     * REQ-C1-2 & REQ-C2-2: 获取需求（定义或用法）
     */
    public RequirementDefinitionDTO getRequirement(String id) {
        log.debug("获取需求: id={}", id);
        
        EObject requirement = repository.findById(PROJECT_ID, id);
        if (requirement == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "需求不存在: " + id);
        }
        
        String className = requirement.eClass().getName();
        if ("RequirementDefinition".equals(className)) {
            return convertToDTO(requirement);
        } else if ("RequirementUsage".equals(className)) {
            return convertUsageToDTO(requirement);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "需求不存在: " + id);
        }
    }
    
    /**
     * REQ-C1-2: 获取需求定义
     */
    public RequirementDefinitionDTO getDefinition(String id) {
        log.debug("获取需求定义: id={}", id);
        
        EObject definition = repository.findById(PROJECT_ID, id);
        if (definition == null || !"RequirementDefinition".equals(definition.eClass().getName())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "需求定义不存在: " + id);
        }
        
        return convertToDTO(definition);
    }
    
    /**
     * REQ-C1-2 & REQ-C2-2: 更新需求（定义或用法）
     */
    public RequirementDefinitionDTO updateRequirement(String id, RequirementDefinitionDTO.UpdateRequest request) {
        log.info("更新需求: id={}", id);
        
        Resource resource = repository.loadProject(PROJECT_ID);
        EObject requirement = repository.findById(PROJECT_ID, id);
        
        if (requirement == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "需求不存在: " + id);
        }
        
        String className = requirement.eClass().getName();
        if ("RequirementDefinition".equals(className)) {
            return updateDefinition(id, request);
        } else if ("RequirementUsage".equals(className)) {
            return updateUsage(id, request);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "需求不存在: " + id);
        }
    }
    
    /**
     * REQ-C1-2: 更新需求定义
     */
    private RequirementDefinitionDTO updateDefinition(String id, RequirementDefinitionDTO.UpdateRequest request) {
        log.info("更新需求定义: id={}", id);
        
        Resource resource = repository.loadProject(PROJECT_ID);
        EObject definition = repository.findById(PROJECT_ID, id);
        
        if (definition == null || !"RequirementDefinition".equals(definition.eClass().getName())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "需求定义不存在: " + id);
        }
        
        // 如果更新reqId，检查唯一性 (REQ-C1-3)
        if (request.getReqId() != null) {
            String currentReqId = (String) getEObjectAttribute(definition, "reqId");
            if (!request.getReqId().equals(currentReqId)) {
                validateReqIdUniquenessForUpdate(request.getReqId(), id);
            }
            setEObjectAttribute(definition, "reqId", request.getReqId());
        }
        
        // 更新允许的字段
        if (request.getName() != null) {
            setEObjectAttribute(definition, "name", request.getName());
        }
        if (request.getText() != null) {
            setEObjectAttribute(definition, "text", request.getText());
        }
        if (request.getDoc() != null) {
            setEObjectAttribute(definition, "doc", request.getDoc());
        }
        if (request.getTags() != null) {
            EStructuralFeature tagsFeature = definition.eClass().getEStructuralFeature("tags");
            if (tagsFeature != null && tagsFeature.isMany()) {
                @SuppressWarnings("unchecked")
                EList<String> tagsList = (EList<String>) definition.eGet(tagsFeature);
                tagsList.clear();
                tagsList.addAll(request.getTags());
            }
        }
        
        // 更新时间戳
        setEObjectAttribute(definition, "updatedAt", Date.from(Instant.now()));
        
        // 保存
        repository.saveProject(PROJECT_ID, resource);
        
        log.info("需求定义更新成功: id={}", id);
        return convertToDTO(definition);
    }
    
    /**
     * REQ-C2-2: 更新需求用法
     */
    private RequirementDefinitionDTO updateUsage(String id, RequirementDefinitionDTO.UpdateRequest request) {
        log.info("更新需求用法: id={}", id);
        
        Resource resource = repository.loadProject(PROJECT_ID);
        EObject usage = repository.findById(PROJECT_ID, id);
        
        if (usage == null || !"RequirementUsage".equals(usage.eClass().getName())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "需求用法不存在: " + id);
        }
        
        // 更新允许的字段：name, text, status, tags
        if (request.getName() != null) {
            setEObjectAttribute(usage, "name", request.getName());
        }
        if (request.getText() != null) {
            setEObjectAttribute(usage, "text", request.getText());
        }
        if (request.getStatus() != null) {
            setEObjectAttribute(usage, "status", request.getStatus());
        }
        if (request.getTags() != null) {
            EStructuralFeature tagsFeature = usage.eClass().getEStructuralFeature("tags");
            if (tagsFeature != null && tagsFeature.isMany()) {
                @SuppressWarnings("unchecked")
                EList<String> tagsList = (EList<String>) usage.eGet(tagsFeature);
                tagsList.clear();
                tagsList.addAll(request.getTags());
            }
        }
        
        // 更新时间戳
        setEObjectAttribute(usage, "updatedAt", Date.from(Instant.now()));
        
        // 保存
        repository.saveProject(PROJECT_ID, resource);
        
        log.info("需求用法更新成功: id={}", id);
        return convertUsageToDTO(usage);
    }
    
    /**
     * REQ-C1-2 & REQ-C2-2: 删除需求（定义或用法）
     */
    public void deleteRequirement(String id) {
        log.info("删除需求: id={}", id);
        
        // 先获取资源
        Resource resource = repository.loadProject(PROJECT_ID);
        
        // 在同一个资源中查找对象
        EObject requirement = resource.getContents().stream()
            .filter(obj -> {
                var idFeature = obj.eClass().getEStructuralFeature("id");
                if (idFeature != null) {
                    Object value = obj.eGet(idFeature);
                    return id.equals(value);
                }
                return false;
            })
            .findFirst()
            .orElse(null);
        
        if (requirement == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "需求不存在: " + id);
        }
        
        String className = requirement.eClass().getName();
        if ("RequirementDefinition".equals(className)) {
            deleteDefinition(id);
        } else if ("RequirementUsage".equals(className)) {
            deleteUsage(id);
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "需求不存在: " + id);
        }
    }
    
    /**
     * REQ-C1-2: 删除需求定义
     */
    public void deleteDefinition(String id) {
        // TODO: 实现基于Pilot元模型的RequirementDefinition删除
        throw new UnsupportedOperationException("基于Pilot的RequirementDefinition删除功能待实现");
        
        /* 原实现暂时注释
        log.info("删除需求定义: id={}", id);
        
        Resource resource = repository.loadProject(PROJECT_ID);
        EObject definition = repository.findById(PROJECT_ID, id);
        
        if (definition == null || !"RequirementDefinition".equals(definition.eClass().getName())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "需求定义不存在: " + id);
        }
        
        // 检查是否被Usage引用
        List<EObject> usages = repository.findByType(PROJECT_ID, "RequirementUsage");
        boolean isReferenced = usages.stream()
                .anyMatch(usage -> id.equals(getEObjectAttribute(usage, "of")));
        
        if (isReferenced) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "需求定义被其他需求用法引用，无法删除");
        }
        
        // 从资源中移除
        boolean removed = resource.getContents().remove(definition);
        log.debug("从资源中移除Definition对象: removed={}, 剩余对象数={}", removed, resource.getContents().size());
        
        // 保存
        repository.saveProject(PROJECT_ID, resource);
        
        log.info("需求定义删除成功: id={}", id);
        */
    }
    
    /**
     * REQ-C2-2: 删除需求用法
     */
    private void deleteUsage(String id) {
        log.info("删除需求用法: id={}", id);
        
        // 先获取资源
        Resource resource = repository.loadProject(PROJECT_ID);
        
        // 在同一个资源中查找对象
        EObject usage = resource.getContents().stream()
            .filter(obj -> {
                var idFeature = obj.eClass().getEStructuralFeature("id");
                if (idFeature != null) {
                    Object value = obj.eGet(idFeature);
                    return id.equals(value);
                }
                return false;
            })
            .findFirst()
            .orElse(null);
        
        if (usage == null || !"RequirementUsage".equals(usage.eClass().getName())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "需求用法不存在: " + id);
        }
        
        // 检查是否存在Trace关系（REQ-C2-2）
        List<String> blockingTraceIds = findBlockingTraces(id);
        if (!blockingTraceIds.isEmpty()) {
            throw new UsageDeleteConflictException("需求用法被Trace引用，无法删除", blockingTraceIds);
        }
        
        // 从资源中移除
        boolean removed = resource.getContents().remove(usage);
        log.debug("从资源中移除用法对象: removed={}, 剩余对象数={}", removed, resource.getContents().size());
        
        // 保存
        repository.saveProject(PROJECT_ID, resource);
        
        log.info("需求用法删除成功: id={}", id);
    }
    
    /**
     * 获取所有需求定义列表
     */
    public List<RequirementDefinitionDTO> listDefinitions() {
        log.debug("获取需求定义列表");
        
        List<EObject> definitions = repository.findByType(PROJECT_ID, "RequirementDefinition");
        return definitions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有需求用法列表
     */
    public List<RequirementDefinitionDTO> listUsages() {
        log.debug("获取需求用法列表");
        
        List<EObject> usages = repository.findByType(PROJECT_ID, "RequirementUsage");
        return usages.stream()
                .map(this::convertUsageToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有需求列表（定义和用法）
     */
    public List<RequirementDefinitionDTO> listRequirements() {
        log.debug("获取所有需求列表");
        
        List<RequirementDefinitionDTO> allRequirements = new ArrayList<>();
        allRequirements.addAll(listDefinitions());
        allRequirements.addAll(listUsages());
        
        return allRequirements;
    }
    
    /**
     * 清除缓存 - 用于测试
     */
    public void clearCache(String projectId) {
        repository.clearCache(projectId);
    }
    
    // ViewController支持方法
    
    /**
     * 获取所有需求定义列表 - for TreeView
     */
    public List<RequirementDefinitionDTO> findAllDefinitions() {
        return listDefinitions();
    }
    
    /**
     * 获取所有需求使用列表 - for TreeView/GraphView  
     */
    public List<RequirementUsageDTO> findAllUsages() {
        List<EObject> usages = repository.findByType(PROJECT_ID, "RequirementUsage");
        return usages.stream()
                .map(this::convertUsageToUsageDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据Definition ID查找相关的Usage列表
     */
    public List<RequirementUsageDTO> findUsagesByDefinitionId(String definitionId) {
        List<EObject> usages = repository.findByType(PROJECT_ID, "RequirementUsage");
        return usages.stream()
                .filter(usage -> definitionId.equals(getEObjectAttribute(usage, "of")))
                .map(this::convertUsageToUsageDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 分页查询表格数据 - for TableView
     */
    public org.springframework.data.domain.Page<com.sysml.mvp.dto.TableRowDTO> findAllForTable(
            org.springframework.data.domain.Pageable pageable, String searchQuery) {
        // TODO: 实现真正的分页查询，目前先返回简单实现
        List<com.sysml.mvp.dto.TableRowDTO> allRows = new ArrayList<>();
        
        // 添加Definition行
        List<RequirementDefinitionDTO> definitions = listDefinitions();
        definitions.forEach(def -> {
            allRows.add(com.sysml.mvp.dto.TableRowDTO.builder()
                .id(def.getId())
                .reqId(def.getReqId())
                .name(def.getName())
                .type("definition")
                .status("definition")
                .tags(def.getTags())
                .text(def.getText())
                .of(null)
                .createdAt(def.getCreatedAt())
                .updatedAt(def.getUpdatedAt())
                .build());
        });
        
        // 添加Usage行
        List<RequirementUsageDTO> usages = findAllUsages();
        usages.forEach(usage -> {
            allRows.add(com.sysml.mvp.dto.TableRowDTO.builder()
                .id(usage.getId())
                .reqId(null)
                .name(usage.getName())
                .type("usage")
                .status(usage.getStatus())
                .tags(usage.getTags())
                .text(usage.getText())
                .of(usage.getOf())
                .createdAt(usage.getCreatedAt())
                .updatedAt(usage.getUpdatedAt())
                .build());
        });
        
        // 简单的内存分页实现
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allRows.size());
        List<com.sysml.mvp.dto.TableRowDTO> pageContent = start < allRows.size() ? 
            allRows.subList(start, end) : List.of();
        
        return new org.springframework.data.domain.PageImpl<>(
            pageContent, pageable, allRows.size());
    }
    
    /**
     * 转换Usage为UsageDTO的辅助方法
     */
    @SuppressWarnings("unchecked")
    private RequirementUsageDTO convertUsageToUsageDTO(EObject usage) {
        return RequirementUsageDTO.builder()
                .id((String) getEObjectAttribute(usage, "id"))
                .of((String) getEObjectAttribute(usage, "of"))
                .name((String) getEObjectAttribute(usage, "name"))
                .text((String) getEObjectAttribute(usage, "text"))
                .status((String) getEObjectAttribute(usage, "status"))
                .tags((List<String>) getEObjectAttribute(usage, "tags"))
                .version((String) getEObjectAttribute(usage, "_version"))
                .createdAt(convertToInstant((Date) getEObjectAttribute(usage, "createdAt")))
                .updatedAt(convertToInstant((Date) getEObjectAttribute(usage, "updatedAt")))
                .build();
    }
    
    // 私有辅助方法
    
    private void validateRequiredFields(String reqId, String name, String text) {
        if (reqId == null || reqId.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reqId不能为空");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name不能为空");
        }
        if (text == null || text.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "text不能为空");
        }
    }
    
    private void validateReqIdUniqueness(String reqId) {
        List<EObject> existing = repository.findByType(PROJECT_ID, "RequirementDefinition");
        List<String> conflictIds = existing.stream()
                .filter(obj -> reqId.equals(getEObjectAttribute(obj, "reqId")))
                .map(obj -> (String) getEObjectAttribute(obj, "id"))
                .collect(Collectors.toList());
        
        if (!conflictIds.isEmpty()) {
            throw new ReqIdConflictException("reqId already exists: " + reqId, conflictIds);
        }
    }
    
    private void validateReqIdUniquenessForUpdate(String reqId, String excludeId) {
        List<EObject> existing = repository.findByType(PROJECT_ID, "RequirementDefinition");
        List<String> conflictIds = existing.stream()
                .filter(obj -> !excludeId.equals(getEObjectAttribute(obj, "id")))
                .filter(obj -> reqId.equals(getEObjectAttribute(obj, "reqId")))
                .map(obj -> (String) getEObjectAttribute(obj, "id"))
                .collect(Collectors.toList());
        
        if (!conflictIds.isEmpty()) {
            throw new ReqIdConflictException("reqId already exists: " + reqId, conflictIds);
        }
    }
    
    // 自定义异常类
    public static class ReqIdConflictException extends RuntimeException {
        private final List<String> conflictIds;
        
        public ReqIdConflictException(String message, List<String> conflictIds) {
            super(message);
            this.conflictIds = conflictIds;
        }
        
        public List<String> getConflictIds() {
            return conflictIds;
        }
    }
    
    /**
     * 需求用法删除冲突异常 - REQ-C2-2
     */
    public static class UsageDeleteConflictException extends RuntimeException {
        private final List<String> blockingTraceIds;
        
        public UsageDeleteConflictException(String message, List<String> blockingTraceIds) {
            super(message);
            this.blockingTraceIds = blockingTraceIds;
        }
        
        public List<String> getBlockingTraceIds() {
            return blockingTraceIds;
        }
    }
    
    private EClass getDefinitionClass() {
        EPackage sysmlPackage = modelRegistry.getSysmlPackage();
        return (EClass) sysmlPackage.getEClassifier("RequirementDefinition");
    }
    
    private void setEObjectAttribute(EObject object, String attributeName, Object value) {
        EStructuralFeature feature = object.eClass().getEStructuralFeature(attributeName);
        if (feature != null) {
            object.eSet(feature, value);
        }
    }
    
    private Object getEObjectAttribute(EObject object, String attributeName) {
        EStructuralFeature feature = object.eClass().getEStructuralFeature(attributeName);
        return feature != null ? object.eGet(feature) : null;
    }
    
    @SuppressWarnings("unchecked")
    private RequirementDefinitionDTO convertToDTO(EObject definition) {
        // 处理tags字段 - 可能是EList或List
        Object tagsObj = getEObjectAttribute(definition, "tags");
        List<String> tags = null;
        if (tagsObj instanceof List) {
            tags = (List<String>) tagsObj;
        } else if (tagsObj instanceof EList) {
            tags = new ArrayList<>((EList<String>) tagsObj);
        } else if (tagsObj != null) {
            // 如果是其他类型，尝试转换为String再包装成List
            tags = Arrays.asList(tagsObj.toString());
        } else {
            tags = new ArrayList<>();
        }
        
        return RequirementDefinitionDTO.builder()
                .id((String) getEObjectAttribute(definition, "id"))
                .eClass("RequirementDefinition")
                .reqId((String) getEObjectAttribute(definition, "reqId"))
                .name((String) getEObjectAttribute(definition, "name"))
                .text((String) getEObjectAttribute(definition, "text"))
                .doc((String) getEObjectAttribute(definition, "doc"))
                .tags(tags)
                .version((String) getEObjectAttribute(definition, "_version"))
                .createdAt(convertToInstant((Date) getEObjectAttribute(definition, "createdAt")))
                .updatedAt(convertToInstant((Date) getEObjectAttribute(definition, "updatedAt")))
                .build();
    }
    
    /**
     * 将RequirementUsage转换为DTO
     */
    @SuppressWarnings("unchecked")
    private RequirementDefinitionDTO convertUsageToDTO(EObject usage) {
        // 处理tags字段 - 可能是EList或List
        Object tagsObj = getEObjectAttribute(usage, "tags");
        List<String> tags = null;
        if (tagsObj instanceof List) {
            tags = (List<String>) tagsObj;
        } else if (tagsObj instanceof EList) {
            tags = new ArrayList<>((EList<String>) tagsObj);
        } else if (tagsObj != null) {
            tags = Arrays.asList(tagsObj.toString());
        } else {
            tags = new ArrayList<>();
        }
        
        return RequirementDefinitionDTO.builder()
                .id((String) getEObjectAttribute(usage, "id"))
                .eClass("RequirementUsage")
                .of((String) getEObjectAttribute(usage, "of"))
                .name((String) getEObjectAttribute(usage, "name"))
                .text((String) getEObjectAttribute(usage, "text"))
                .status((String) getEObjectAttribute(usage, "status"))
                .tags(tags)
                .version((String) getEObjectAttribute(usage, "_version"))
                .createdAt(convertToInstant((Date) getEObjectAttribute(usage, "createdAt")))
                .updatedAt(convertToInstant((Date) getEObjectAttribute(usage, "updatedAt")))
                .build();
    }
    
    
    /**
     * 验证引用的definition是否存在
     */
    private void validateDefinitionExists(String defId) {
        EObject definition = repository.findById(PROJECT_ID, defId);
        if (definition == null || !"RequirementDefinition".equals(definition.eClass().getName())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "引用的需求定义不存在: " + defId);
        }
    }
    
    /**
     * 查找阻塞删除的Trace关系
     */
    private List<String> findBlockingTraces(String usageId) {
        List<EObject> traces = repository.findByType(PROJECT_ID, "Trace");
        return traces.stream()
                .filter(trace -> {
                    String fromId = (String) getEObjectAttribute(trace, "fromId");
                    String toId = (String) getEObjectAttribute(trace, "toId");
                    return usageId.equals(fromId) || usageId.equals(toId);
                })
                .map(trace -> (String) getEObjectAttribute(trace, "id"))
                .collect(Collectors.toList());
    }
    
    private Instant convertToInstant(Date date) {
        return date != null ? date.toInstant() : null;
    }
    
    // 为测试添加的公共方法
    
    /**
     * REQ-C1-2 & REQ-C2-2: PATCH部分更新需求
     * 使用PATCH语义，只更新提供的字段
     */
    public RequirementDefinitionDTO patchRequirement(String id, Map<String, Object> patchData) {
        log.info("PATCH更新需求: id={}, fields={}", id, patchData.keySet());
        
        // 查找需求对象
        EObject requirement = repository.findById(PROJECT_ID, id);
        if (requirement == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "需求不存在: " + id);
        }
        
        // 获取PilotEMFService
        PilotEMFService pilotService = applicationContext.getBean(PilotEMFService.class);
        
        // 将API字段映射到Pilot字段
        Map<String, Object> modelPatchData = new HashMap<>();
        for (Map.Entry<String, Object> entry : patchData.entrySet()) {
            String apiField = entry.getKey();
            Object value = entry.getValue();
            
            // API到Model的字段映射
            switch (apiField) {
                case "name":
                    modelPatchData.put("declaredName", value);
                    break;
                case "reqId":
                    modelPatchData.put("declaredShortName", value);
                    break;
                case "text":
                    // text需要特殊处理为documentation
                    if (value != null) {
                        modelPatchData.put("documentation", value);
                    }
                    break;
                case "status":
                    modelPatchData.put("status", value);
                    break;
                case "tags":
                    modelPatchData.put("tags", value);
                    break;
                // 忽略不应修改的字段
                case "id":
                case "type":
                case "of":
                case "eClass":
                case "createdAt":
                    log.trace("忽略只读字段: {}", apiField);
                    break;
                default:
                    // 未知字段直接传递（让mergeAttributes处理）
                    modelPatchData.put(apiField, value);
            }
        }
        
        // 使用PilotEMFService的mergeAttributes进行部分更新
        pilotService.mergeAttributes(requirement, modelPatchData);
        
        // 更新时间戳（不需要手动更新，mergeAttributes应该处理）
        // 如果需要强制更新时间戳，可以这样：
        // pilotService.setAttributeIfExists(requirement, "updatedAt", Date.from(Instant.now()));
        
        // 保存更新
        Resource resource = requirement.eResource();
        if (resource != null) {
            repository.saveProject(PROJECT_ID, resource);
        }
        
        // 转换为DTO返回
        return convertToDTO(requirement);
    }
    
    /**
     * 创建需求定义 - 基于Pilot元模型
     */
    public RequirementDefinitionDTO createDefinition(RequirementDefinitionDTO dto) {
        log.info("创建RequirementDefinition(Pilot): reqId={}, name={}", dto.getReqId(), dto.getName());
        
        // 输入验证
        if (dto.getReqId() == null || dto.getReqId().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "reqId不能为空");
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name不能为空");
        }
        if (dto.getText() == null || dto.getText().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "text不能为空");
        }
        
        // 使用PilotEMFService创建
        PilotEMFService pilotService = applicationContext.getBean(PilotEMFService.class);
        EObject reqDef = pilotService.createRequirementDefinition(dto.getReqId(), dto.getName(), dto.getText());
        
        // 转换为DTO返回
        RequirementDefinitionDTO result = new RequirementDefinitionDTO();
        result.setId((String) pilotService.getAttributeValue(reqDef, "elementId"));
        result.setEClass("RequirementDefinition");
        result.setType("definition");
        result.setReqId(dto.getReqId());
        result.setName(dto.getName());
        result.setText(dto.getText());
        result.setCreatedAt(Instant.now());
        result.setUpdatedAt(Instant.now());
        
        log.info("成功创建RequirementDefinition: id={}", result.getId());
        return result;
    }
    
    /**
     * 更新需求定义 - 用于测试
     */
    public RequirementDefinitionDTO updateDefinition(String id, RequirementDefinitionDTO dto) {
        // TODO: 实现基于Pilot元模型的RequirementDefinition更新
        throw new UnsupportedOperationException("基于Pilot的RequirementDefinition更新功能待实现");
    }
    
    
    /**
     * 根据ID获取需求定义 - 用于测试
     */
    public RequirementDefinitionDTO getDefinitionById(String id) {
        // TODO: 实现基于Pilot元模型的RequirementDefinition查询
        throw new UnsupportedOperationException("基于Pilot的RequirementDefinition查询功能待实现");
    }
}