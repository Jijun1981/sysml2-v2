package com.sysml.mvp.service;

import com.sysml.mvp.dto.ElementDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 追溯关系服务
 * 
 * 需求实现：
 * - REQ-B5-2: 追溯关系服务 - 业务验证后委托给UniversalElementService
 * - REQ-C3-1: 创建追溯关系 - type映射到具体EClass
 * - REQ-C3-2: 查询追溯关系 - 支持type和element过滤
 * - REQ-C3-3: 去重追溯关系 - 检测相同(source,target,type)的重复关系
 * - REQ-C3-4: 追溯语义约束 - 验证Satisfy/DeriveRequirement/Refine语义约束
 * 
 * 设计说明：
 * 1. API层使用简化的type字符串，映射到具体的EMF EClass
 * 2. 实现去重验证和语义约束验证
 * 3. 支持多种查询过滤方式
 * 4. 所有CRUD操作委托给UniversalElementService执行
 */
@Service
public class TraceService {
    
    private final UniversalElementService universalElementService;
    private final ValidationService validationService;
    
    /**
     * API层type到EMF EClass的映射
     */
    private static final Map<String, String> TYPE_TO_ECLASS_MAPPING = new HashMap<>();
    static {
        // 在SysML 2.0中，所有追溯关系都使用Dependency类，通过type属性区分具体类型
        TYPE_TO_ECLASS_MAPPING.put("derive", "Dependency");
        TYPE_TO_ECLASS_MAPPING.put("satisfy", "Dependency");
        TYPE_TO_ECLASS_MAPPING.put("refine", "Dependency");
        TYPE_TO_ECLASS_MAPPING.put("trace", "Dependency");
    }
    
    public TraceService(
            UniversalElementService universalElementService,
            ValidationService validationService) {
        this.universalElementService = universalElementService;
        this.validationService = validationService;
    }
    
    /**
     * 【REQ-C3-1】创建追溯关系
     * API层type映射到具体EClass，验证去重和语义约束
     * @param traceData 追溯关系数据，必须包含source、target、type
     * @return 创建的追溯关系DTO
     * @throws IllegalArgumentException 如果字段缺失、重复或语义无效
     */
    public ElementDTO createTrace(Map<String, Object> traceData) {
        // 【REQ-C3-1】验证必填字段
        validateRequiredFields(traceData);
        
        String source = traceData.get("fromId").toString();
        String target = traceData.get("toId").toString();
        String type = traceData.get("type").toString();
        
        // 【REQ-C3-3】验证去重
        if (!validationService.validateTraceDuplication(source, target, type)) {
            throw new IllegalArgumentException(
                String.format("Duplicate trace relationship found: %s -> %s (%s)", 
                    source, target, type));
        }
        
        // 【REQ-C3-4】验证语义约束
        if (!validationService.validateTraceSemantics(source, target, type)) {
            throw new IllegalArgumentException(
                String.format("Invalid trace semantics for type: %s", type));
        }
        
        // 【REQ-C3-1】映射API层type到EMF EClass
        String eClass = TYPE_TO_ECLASS_MAPPING.get(type);
        if (eClass == null) {
            throw new IllegalArgumentException("Unsupported trace type: " + type);
        }
        
        // 转换API层字段到EMF层字段
        Map<String, Object> emfData = convertToEmfData(traceData);
        
        // 委托给UniversalElementService创建
        return universalElementService.createElement(eClass, emfData);
    }
    
    /**
     * 【REQ-C3-2】查询所有追溯关系
     * 聚合四种追溯类型的查询结果
     * @return 所有追溯关系列表
     */
    public List<ElementDTO> getAllTraces() {
        List<ElementDTO> allTraces = new ArrayList<>();
        
        // 查询四种追溯类型
        for (String eClass : TYPE_TO_ECLASS_MAPPING.values()) {
            List<ElementDTO> traces = universalElementService.queryElements(eClass);
            allTraces.addAll(traces);
        }
        
        return allTraces;
    }
    
    /**
     * 【REQ-C3-2】按类型查询追溯关系
     * @param type API层type (derive/satisfy/refine/trace)
     * @return 指定类型的追溯关系列表
     */
    public List<ElementDTO> getTracesByType(String type) {
        String eClass = TYPE_TO_ECLASS_MAPPING.get(type);
        if (eClass == null) {
            throw new IllegalArgumentException("Unsupported trace type: " + type);
        }
        
        return universalElementService.queryElements(eClass);
    }
    
    /**
     * 【REQ-C3-2】按元素查询追溯关系
     * 查询与指定元素相关的所有追溯关系（作为source或target）
     * @param elementId 元素ID
     * @return 相关的追溯关系列表
     */
    public List<ElementDTO> getTracesByElement(String elementId) {
        List<ElementDTO> allTraces = getAllTraces();
        
        return allTraces.stream()
            .filter(trace -> isTraceRelatedToElement(trace, elementId))
            .collect(Collectors.toList());
    }
    
    /**
     * 【REQ-C3-2】按源端查询追溯关系
     * @param fromId 源端元素ID
     * @return 以指定元素为源端的追溯关系列表
     */
    public List<ElementDTO> getTracesByFromId(String fromId) {
        List<ElementDTO> allTraces = getAllTraces();
        
        return allTraces.stream()
            .filter(trace -> fromId.equals(trace.getProperty("fromId")))
            .collect(Collectors.toList());
    }
    
    /**
     * 【REQ-C3-2】按目标端查询追溯关系
     * @param toId 目标端元素ID
     * @return 以指定元素为目标端的追溯关系列表
     */
    public List<ElementDTO> getTracesByToId(String toId) {
        List<ElementDTO> allTraces = getAllTraces();
        
        return allTraces.stream()
            .filter(trace -> toId.equals(trace.getProperty("toId")))
            .collect(Collectors.toList());
    }
    
    /**
     * 【REQ-B5-2】删除追溯关系
     * @param elementId 追溯关系ID
     * @throws IllegalArgumentException 如果追溯关系不存在
     */
    public void deleteTrace(String elementId) {
        // 先检查元素是否存在
        ElementDTO existingTrace = universalElementService.findElementById(elementId);
        if (existingTrace == null) {
            throw new IllegalArgumentException("Trace not found: " + elementId);
        }
        
        boolean deleted = universalElementService.deleteElement(elementId);
        if (!deleted) {
            throw new IllegalArgumentException("Failed to delete trace: " + elementId);
        }
    }
    
    /**
     * 【REQ-B5-2】根据ID查找追溯关系
     * @param elementId 追溯关系ID
     * @return 追溯关系DTO，如果不存在返回null
     */
    public ElementDTO getTraceById(String elementId) {
        return universalElementService.findElementById(elementId);
    }
    
    /**
     * 【REQ-C3-4】验证追溯关系语义
     * @param source 源端元素ID
     * @param target 目标端元素ID
     * @param type 追溯类型
     * @return true如果语义有效
     */
    public boolean validateTraceSemantics(String source, String target, String type) {
        return validationService.validateTraceSemantics(source, target, type);
    }
    
    /**
     * 【REQ-C3-4】获取追溯关系语义验证消息
     * @param source 源端元素ID
     * @param target 目标端元素ID
     * @param type 追溯类型
     * @return 验证消息
     */
    public String getTraceSemanticValidationMessage(String source, String target, String type) {
        return validationService.getTraceSemanticValidationMessage(source, target, type);
    }
    
    /**
     * 验证追溯关系必填字段
     */
    private void validateRequiredFields(Map<String, Object> traceData) {
        if (!traceData.containsKey("fromId") || traceData.get("fromId") == null) {
            throw new IllegalArgumentException("fromId is required for trace relationship");
        }
        
        if (!traceData.containsKey("toId") || traceData.get("toId") == null) {
            throw new IllegalArgumentException("toId is required for trace relationship");
        }
        
        if (!traceData.containsKey("type") || traceData.get("type") == null) {
            throw new IllegalArgumentException("type is required for trace relationship");
        }
    }
    
    /**
     * 转换API层数据到EMF层数据
     * 移除API层的type字段，EMF层通过EClass表示类型
     */
    private Map<String, Object> convertToEmfData(Map<String, Object> traceData) {
        Map<String, Object> emfData = new HashMap<>(traceData);
        
        // 移除API层的type字段，EMF层通过EClass表示类型
        emfData.remove("type");
        
        return emfData;
    }
    
    /**
     * 检查追溯关系是否与指定元素相关
     */
    private boolean isTraceRelatedToElement(ElementDTO trace, String elementId) {
        Object fromId = trace.getProperty("fromId");
        Object toId = trace.getProperty("toId");
        
        return elementId.equals(fromId) || elementId.equals(toId);
    }
}