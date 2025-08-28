package com.sysml.mvp.service;

import com.sysml.mvp.dto.ElementDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 需求领域服务
 * 
 * 需求实现：
 * - REQ-B5-1: 需求领域服务 - 业务验证后委托给UniversalElementService
 * - REQ-C1-1: reqId唯一性验证 - 创建需求定义时验证reqId唯一性（409冲突）
 * - REQ-C1-3: 更新需求定义
 * - REQ-C2-1: 创建需求使用
 * - REQ-C2-4: 删除前检查被引用保护 - RequirementDefinition被Usage引用时不能删除
 * 
 * 设计说明：
 * 1. 专注于需求领域的业务验证逻辑
 * 2. 所有CRUD操作委托给UniversalElementService执行
 * 3. 实现reqId唯一性、约束对象必填、删除保护等业务规则
 * 4. 不直接操作EMF，保持领域服务的纯业务性
 */
@Service
public class RequirementService {
    
    private final UniversalElementService universalElementService;
    private final ValidationService validationService;
    
    public RequirementService(
            UniversalElementService universalElementService,
            ValidationService validationService) {
        this.universalElementService = universalElementService;
        this.validationService = validationService;
    }
    
    /**
     * 【REQ-C1-1】创建RequirementDefinition
     * 验证reqId唯一性，然后委托给UniversalElementService
     * @param reqData 需求数据，必须包含reqId
     * @return 创建的需求DTO
     * @throws IllegalArgumentException 如果reqId重复或缺失
     */
    public ElementDTO createRequirement(Map<String, Object> reqData) {
        // 【REQ-C1-1】验证reqId必填
        if (!reqData.containsKey("reqId") || reqData.get("reqId") == null) {
            throw new IllegalArgumentException("reqId is required for RequirementDefinition");
        }
        
        String reqId = reqData.get("reqId").toString();
        
        // 【REQ-C1-1】验证reqId唯一性
        if (!validationService.validateReqIdUniqueness(reqId)) {
            throw new IllegalArgumentException("reqId already exists: " + reqId);
        }
        
        // 委托给UniversalElementService创建
        return universalElementService.createElement("RequirementDefinition", reqData);
    }
    
    /**
     * 【REQ-C1-3】更新RequirementDefinition
     * 支持部分更新（PATCH语义）
     * @param elementId 需求ID
     * @param updates 要更新的属性
     * @return 更新后的需求DTO
     */
    public ElementDTO updateRequirement(String elementId, Map<String, Object> updates) {
        // 委托给UniversalElementService更新
        return universalElementService.patchElement(elementId, updates);
    }
    
    /**
     * 【REQ-C2-1】创建RequirementUsage
     * 验证必填字段后委托给UniversalElementService创建
     * @param usageData 需求使用数据
     * @return 创建的需求使用DTO
     */
    public ElementDTO createRequirementUsage(Map<String, Object> usageData) {
        // 验证requirementDefinition字段必填 - RequirementUsage必须关联到RequirementDefinition
        if (!usageData.containsKey("requirementDefinition") || usageData.get("requirementDefinition") == null) {
            throw new IllegalArgumentException("requirementDefinition is required for RequirementUsage");
        }
        
        // 委托给UniversalElementService创建
        return universalElementService.createElement("RequirementUsage", usageData);
    }
    
    /**
     * 【REQ-C2-4】删除RequirementDefinition
     * 检查是否被RequirementUsage引用，如有引用则阻止删除
     * @param elementId 需求ID
     * @return 是否删除成功
     * @throws IllegalStateException 如果被引用
     */
    public boolean deleteRequirement(String elementId) {
        // 【REQ-C2-4】检查被引用保护
        List<ElementDTO> usages = universalElementService.queryElements("RequirementUsage");
        List<ElementDTO> referencingUsages = usages.stream()
            .filter(usage -> elementId.equals(usage.getProperty("requirementDefinition")))
            .collect(Collectors.toList());
        
        if (!referencingUsages.isEmpty()) {
            throw new IllegalStateException(
                String.format("Cannot delete requirement %s: referenced by %d usages", 
                    elementId, referencingUsages.size()));
        }
        
        // 委托给UniversalElementService删除
        return universalElementService.deleteElement(elementId);
    }
    
    /**
     * 【REQ-B5-1】查询所有RequirementDefinition
     * @return 需求定义列表
     */
    public List<ElementDTO> getRequirements() {
        return universalElementService.queryElements("RequirementDefinition");
    }
    
    /**
     * 【REQ-B5-1】查询所有RequirementUsage
     * @return 需求使用列表
     */
    public List<ElementDTO> getRequirementUsages() {
        return universalElementService.queryElements("RequirementUsage");
    }
    
    /**
     * 【REQ-B5-1】根据ID查找需求
     * @param elementId 需求ID
     * @return 需求DTO，如果不存在返回null
     */
    public ElementDTO getRequirementById(String elementId) {
        return universalElementService.findElementById(elementId);
    }
    
    /**
     * 【REQ-C1-4】参数化文本渲染
     * 渲染需求文本中的${placeholder}占位符
     * @param requirementId 需求ID
     * @param parameters 参数Map
     * @return 渲染后的文本
     */
    public String renderParametricText(String requirementId, Map<String, Object> parameters) {
        // 查找需求
        ElementDTO requirement = getRequirementById(requirementId);
        if (requirement == null) {
            throw new IllegalArgumentException("Requirement not found: " + requirementId);
        }
        
        String text = (String) requirement.getProperty("documentation");
        if (text == null) {
            return "";
        }
        
        // 简单的占位符替换实现
        String result = text;
        if (parameters != null) {
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                String placeholder = "${" + entry.getKey() + "}";
                String value = entry.getValue() != null ? entry.getValue().toString() : "";
                result = result.replace(placeholder, value);
            }
        }
        
        return result;
    }
}