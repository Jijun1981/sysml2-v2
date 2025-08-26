package com.sysml.mvp.service;

import com.sysml.mvp.dto.ElementDTO;
import com.sysml.mvp.dto.ValidationResultDTO;
import com.sysml.mvp.dto.ValidationViolationDTO;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 验证服务
 * 
 * 需求实现：
 * - REQ-B5-4: 验证服务 - 内部工具层验证功能
 * - REQ-E1-1: MVP规则集 - 仅检测3条核心规则
 * - REQ-E1-2: 规则码固定枚举 - DUP_REQID, CYCLE_DERIVE_REFINE, BROKEN_REF
 * - REQ-E1-3: 验证结果API格式 - 返回ValidationResultDTO
 * - REQ-C1-1: reqId唯一性验证
 * - REQ-C3-3: 追溯关系去重检测
 * - REQ-C3-4: 追溯关系语义约束验证
 * 
 * 设计说明：
 * 1. 实现3条核心验证规则的静态检查
 * 2. 提供reqId唯一性和追溯关系验证支持
 * 3. 返回标准化的ValidationResultDTO格式
 * 4. 支持性能要求：≤500元素<2s处理时间
 */
@Service
public class ValidationService {
    
    private final UniversalElementService universalElementService;
    
    /**
     * 追溯类型到EClass的映射
     */
    private static final Map<String, String> TYPE_TO_ECLASS_MAPPING = new HashMap<>();
    static {
        TYPE_TO_ECLASS_MAPPING.put("derive", "DeriveRequirement");
        TYPE_TO_ECLASS_MAPPING.put("satisfy", "Satisfy");
        TYPE_TO_ECLASS_MAPPING.put("refine", "Refine");
        TYPE_TO_ECLASS_MAPPING.put("trace", "Trace");
    }
    
    public ValidationService(UniversalElementService universalElementService) {
        this.universalElementService = universalElementService;
    }
    
    /**
     * 【REQ-C1-1】验证reqId唯一性
     * @param reqId 需求ID
     * @return true如果reqId唯一，false如果重复
     */
    public boolean validateReqIdUniqueness(String reqId) {
        List<ElementDTO> requirements = universalElementService.queryElements("RequirementDefinition");
        
        return requirements.stream()
            .noneMatch(req -> reqId.equals(req.getProperty("reqId")));
    }
    
    /**
     * 【REQ-C3-3】验证追溯关系去重
     * @param source 源端元素ID
     * @param target 目标端元素ID
     * @param type 追溯类型
     * @return true如果不重复，false如果重复
     */
    public boolean validateTraceDuplication(String source, String target, String type) {
        String eClass = TYPE_TO_ECLASS_MAPPING.get(type);
        if (eClass == null) {
            return false;
        }
        
        List<ElementDTO> traces = universalElementService.queryElements(eClass);
        
        return traces.stream()
            .noneMatch(trace -> 
                source.equals(trace.getProperty("fromId")) && 
                target.equals(trace.getProperty("toId")));
    }
    
    /**
     * 【REQ-C3-4】验证追溯关系语义约束
     * @param source 源端元素ID
     * @param target 目标端元素ID  
     * @param type 追溯类型
     * @return true如果语义有效，false如果无效
     */
    public boolean validateTraceSemantics(String source, String target, String type) {
        ElementDTO sourceElement = universalElementService.findElementById(source);
        ElementDTO targetElement = universalElementService.findElementById(target);
        
        if (sourceElement == null || targetElement == null) {
            return false;
        }
        
        String sourceType = sourceElement.getEClass();
        String targetType = targetElement.getEClass();
        
        return switch (type) {
            case "satisfy" -> isValidSatisfySemantics(sourceType, targetType);
            case "derive" -> isValidDeriveSemantics(sourceType, targetType);
            case "refine" -> isValidRefineSemantics(sourceType, targetType);
            case "trace" -> true; // 通用追溯关系无特殊约束
            default -> false;
        };
    }
    
    /**
     * 【REQ-C3-4】获取追溯关系语义验证消息
     * @param source 源端元素ID
     * @param target 目标端元素ID
     * @param type 追溯类型
     * @return 验证消息
     */
    public String getTraceSemanticValidationMessage(String source, String target, String type) {
        ElementDTO sourceElement = universalElementService.findElementById(source);
        ElementDTO targetElement = universalElementService.findElementById(target);
        
        if (sourceElement == null || targetElement == null) {
            return String.format("Invalid %s relationship: element not found", type);
        }
        
        String sourceType = sourceElement.getEClass();
        String targetType = targetElement.getEClass();
        
        boolean isValid = validateTraceSemantics(source, target, type);
        
        if (isValid) {
            return String.format("Valid %s relationship: %s can %s %s", 
                type, sourceType, type, targetType);
        } else {
            return String.format("Invalid %s relationship: %s cannot %s %s", 
                type, sourceType, type, targetType);
        }
    }
    
    /**
     * 【REQ-E1-3】静态验证
     * 执行所有3条核心规则的验证
     * @param elements 要验证的元素列表
     * @return 验证结果DTO
     */
    public ValidationResultDTO validateStatic(List<ElementDTO> elements) {
        long startTime = System.currentTimeMillis();
        List<ValidationViolationDTO> violations = new ArrayList<>();
        
        // 【REQ-E1-1】检测3条核心规则
        violations.addAll(validateDuplicateReqId());
        violations.addAll(validateCyclicDependencies());
        violations.addAll(validateBrokenReferences());
        
        long endTime = System.currentTimeMillis();
        
        // 【REQ-E1-3】构建ValidationResultDTO
        ValidationResultDTO result = new ValidationResultDTO();
        result.setViolations(violations);
        result.setValidatedAt(Instant.now().toString());
        result.setElementCount(elements.size());
        result.setProcessingTimeMs(endTime - startTime);
        result.setVersion("1.0");
        
        return result;
    }
    
    /**
     * 【REQ-E1-1】检测DUP_REQID违规
     */
    private List<ValidationViolationDTO> validateDuplicateReqId() {
        List<ValidationViolationDTO> violations = new ArrayList<>();
        List<ElementDTO> requirements = universalElementService.queryElements("RequirementDefinition");
        
        Map<String, List<ElementDTO>> reqIdGroups = requirements.stream()
            .filter(req -> req.getProperty("reqId") != null)
            .collect(Collectors.groupingBy(req -> req.getProperty("reqId").toString()));
        
        for (Map.Entry<String, List<ElementDTO>> entry : reqIdGroups.entrySet()) {
            if (entry.getValue().size() > 1) {
                String reqId = entry.getKey();
                List<ElementDTO> duplicates = entry.getValue();
                
                // 为除第一个外的所有重复项创建违规
                for (int i = 1; i < duplicates.size(); i++) {
                    ElementDTO duplicate = duplicates.get(i);
                    ValidationViolationDTO violation = new ValidationViolationDTO();
                    violation.setRuleCode("DUP_REQID");
                    violation.setTargetId(duplicate.getElementId());
                    violation.setMessage("reqId duplicated: " + reqId);
                    violation.setDetails(String.format("Found duplicate reqId '%s' in %d elements", 
                        reqId, duplicates.size()));
                    violations.add(violation);
                }
            }
        }
        
        return violations;
    }
    
    /**
     * 【REQ-E1-1】检测CYCLE_DERIVE_REFINE违规
     */
    private List<ValidationViolationDTO> validateCyclicDependencies() {
        List<ValidationViolationDTO> violations = new ArrayList<>();
        
        // 获取所有派生和细化关系
        List<ElementDTO> derives = universalElementService.queryElements("DeriveRequirement");
        List<ElementDTO> refines = universalElementService.queryElements("Refine");
        
        List<ElementDTO> allDependencies = new ArrayList<>();
        allDependencies.addAll(derives);
        allDependencies.addAll(refines);
        
        // 构建依赖图
        Map<String, Set<String>> dependencyGraph = new HashMap<>();
        for (ElementDTO dep : allDependencies) {
            String from = (String) dep.getProperty("fromId");
            String to = (String) dep.getProperty("toId");
            if (from != null && to != null) {
                dependencyGraph.computeIfAbsent(from, k -> new HashSet<>()).add(to);
            }
        }
        
        // 检测循环依赖
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        
        for (String node : dependencyGraph.keySet()) {
            if (hasCycle(node, dependencyGraph, visited, recursionStack)) {
                ValidationViolationDTO violation = new ValidationViolationDTO();
                violation.setRuleCode("CYCLE_DERIVE_REFINE");
                violation.setTargetId(node);
                violation.setMessage("Circular dependency detected in derive/refine chain");
                violation.setDetails("Cycle detected starting from: " + node);
                violations.add(violation);
                break; // 只报告一个循环即可
            }
        }
        
        return violations;
    }
    
    /**
     * 【REQ-E1-1】检测BROKEN_REF违规
     */
    private List<ValidationViolationDTO> validateBrokenReferences() {
        List<ValidationViolationDTO> violations = new ArrayList<>();
        
        // 获取所有元素ID
        Set<String> allElementIds = getAllElementIds();
        
        // 检查所有追溯关系的引用
        List<String> traceTypes = Arrays.asList("Satisfy", "DeriveRequirement", "Refine", "Trace");
        
        for (String traceType : traceTypes) {
            List<ElementDTO> traces = universalElementService.queryElements(traceType);
            
            for (ElementDTO trace : traces) {
                String fromId = (String) trace.getProperty("fromId");
                String toId = (String) trace.getProperty("toId");
                
                if (fromId != null && !allElementIds.contains(fromId)) {
                    ValidationViolationDTO violation = new ValidationViolationDTO();
                    violation.setRuleCode("BROKEN_REF");
                    violation.setTargetId(trace.getElementId());
                    violation.setMessage("Reference to non-existent element");
                    violation.setDetails(String.format("Trace '%s' references missing element '%s'", 
                        trace.getElementId(), fromId));
                    violations.add(violation);
                }
                
                if (toId != null && !allElementIds.contains(toId)) {
                    ValidationViolationDTO violation = new ValidationViolationDTO();
                    violation.setRuleCode("BROKEN_REF");
                    violation.setTargetId(trace.getElementId());
                    violation.setMessage("Reference to non-existent element");
                    violation.setDetails(String.format("Trace '%s' references missing element '%s'", 
                        trace.getElementId(), toId));
                    violations.add(violation);
                }
            }
        }
        
        return violations;
    }
    
    /**
     * 验证Satisfy语义约束
     * Satisfy: source∈{PartUsage,ActionUsage}, target∈{RequirementUsage,RequirementDefinition}
     */
    private boolean isValidSatisfySemantics(String sourceType, String targetType) {
        boolean validSource = "PartUsage".equals(sourceType) || 
                             "ActionUsage".equals(sourceType);
        
        boolean validTarget = "RequirementUsage".equals(targetType) || 
                             "RequirementDefinition".equals(targetType);
        
        return validSource && validTarget;
    }
    
    /**
     * 验证DeriveRequirement语义约束
     * DeriveRequirement: source/target∈{RequirementDefinition,RequirementUsage}
     */
    private boolean isValidDeriveSemantics(String sourceType, String targetType) {
        boolean validSource = "RequirementDefinition".equals(sourceType) || 
                             "RequirementUsage".equals(sourceType);
        
        boolean validTarget = "RequirementDefinition".equals(targetType) || 
                             "RequirementUsage".equals(targetType);
        
        return validSource && validTarget;
    }
    
    /**
     * 验证Refine语义约束
     * Refine: source/target∈{RequirementDefinition,RequirementUsage}
     */
    private boolean isValidRefineSemantics(String sourceType, String targetType) {
        return isValidDeriveSemantics(sourceType, targetType); // 与Derive相同的约束
    }
    
    /**
     * 深度优先搜索检测循环
     */
    private boolean hasCycle(String node, Map<String, Set<String>> graph, 
                           Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(node)) {
            return true; // 发现循环
        }
        
        if (visited.contains(node)) {
            return false; // 已访问过的节点
        }
        
        visited.add(node);
        recursionStack.add(node);
        
        Set<String> neighbors = graph.get(node);
        if (neighbors != null) {
            for (String neighbor : neighbors) {
                if (hasCycle(neighbor, graph, visited, recursionStack)) {
                    return true;
                }
            }
        }
        
        recursionStack.remove(node);
        return false;
    }
    
    /**
     * 获取所有元素ID
     */
    private Set<String> getAllElementIds() {
        Set<String> allIds = new HashSet<>();
        
        // 查询所有主要元素类型
        List<String> elementTypes = Arrays.asList(
            "RequirementDefinition", "RequirementUsage", 
            "PartUsage", "ActionUsage",
            "Satisfy", "DeriveRequirement", "Refine", "Trace"
        );
        
        for (String type : elementTypes) {
            List<ElementDTO> elements = universalElementService.queryElements(type);
            for (ElementDTO element : elements) {
                if (element.getElementId() != null) {
                    allIds.add(element.getElementId());
                }
            }
        }
        
        return allIds;
    }
}