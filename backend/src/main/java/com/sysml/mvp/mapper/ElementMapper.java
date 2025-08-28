package com.sysml.mvp.mapper;

import com.sysml.mvp.dto.ElementDTO;
import com.sysml.mvp.dto.RequirementDTO;
import com.sysml.mvp.dto.TraceDTO;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 元素映射工具
 * 
 * 需求实现：
 * - REQ-B2-4: DTO选择性映射 - 支持EMF对象与DTO互相转换
 * - REQ-D0-1: 通用元素数据API - 保持eClass字段的准确性
 * - 通用约定: 未识别字段应保留（round-trip不丢失）
 * 
 * 设计说明：
 * 1. toDTO()：将EMF对象转换为ElementDTO，保留所有有效属性
 * 2. toMap()：将ElementDTO转换为属性Map，用于更新操作
 * 3. 跳过派生属性和瞬态属性，避免不必要的数据污染
 * 4. 处理null值，确保转换过程的健壮性
 */
@Component
public class ElementMapper {
    
    /**
     * 【REQ-B2-4】将EMF对象转换为ElementDTO
     * 保留所有非派生、非瞬态的属性到properties Map
     * @param eObject EMF对象
     * @return ElementDTO，如果输入为null返回null
     */
    public ElementDTO toDTO(EObject eObject) {
        if (eObject == null) {
            return null;
        }
        
        ElementDTO dto = new ElementDTO();
        
        // 设置eClass
        dto.setEClass(eObject.eClass().getName());
        
        // 遍历所有属性特征
        for (EStructuralFeature feature : eObject.eClass().getEAllStructuralFeatures()) {
            // 跳过派生和瞬态属性
            if (feature.isDerived() || feature.isTransient()) {
                continue;
            }
            
            String name = feature.getName();
            Object value = eObject.eGet(feature);
            
            // 跳过null值
            if (value == null) {
                continue;
            }
            
            // elementId作为顶级字段单独处理
            if ("elementId".equals(name)) {
                dto.setElementId(value.toString());
            } else {
                // 其他属性存储在properties Map中
                dto.setProperty(name, value);
            }
        }
        
        return dto;
    }
    
    /**
     * 【REQ-B2-4】将ElementDTO转换为属性Map
     * 用于PATCH更新操作或持久化
     * @param dto ElementDTO对象
     * @return 属性Map，不包含eClass（元数据）
     */
    public Map<String, Object> toMap(ElementDTO dto) {
        Map<String, Object> map = new HashMap<>();
        
        if (dto == null) {
            return map; // 返回空Map
        }
        
        // 添加elementId
        if (dto.getElementId() != null) {
            map.put("elementId", dto.getElementId());
        }
        
        // 添加所有properties
        if (dto.getProperties() != null) {
            map.putAll(dto.getProperties());
        }
        
        // 注意：不包含eClass，因为它是元数据而非属性
        
        return map;
    }
    
    /**
     * 【REQ-B2-4】便捷方法：将多个EMF对象转换为DTO列表
     * @param eObjects EMF对象列表
     * @return ElementDTO列表
     */
    public java.util.List<ElementDTO> toDTOList(java.util.List<EObject> eObjects) {
        if (eObjects == null) {
            return new java.util.ArrayList<>();
        }
        
        return eObjects.stream()
            .map(this::toDTO)
            .filter(java.util.Objects::nonNull)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 【REQ-B2-4】便捷方法：合并两个属性Map
     * 用于PATCH操作时合并现有属性和更新属性
     * @param existing 现有属性Map
     * @param updates 更新属性Map
     * @return 合并后的Map
     */
    public Map<String, Object> mergeAttributes(Map<String, Object> existing, Map<String, Object> updates) {
        Map<String, Object> merged = new HashMap<>();
        
        if (existing != null) {
            merged.putAll(existing);
        }
        
        if (updates != null) {
            merged.putAll(updates); // 更新覆盖现有
        }
        
        return merged;
    }
    
    /**
     * 【REQ-B2-4】将ElementDTO转换为RequirementDTO
     * 专门用于需求相关API的响应
     * @param elementDto ElementDTO对象
     * @return RequirementDTO，如果输入为null返回null
     */
    public RequirementDTO toRequirementDTO(ElementDTO elementDto) {
        if (elementDto == null) {
            return null;
        }
        
        RequirementDTO reqDto = new RequirementDTO();
        
        // 基本字段
        reqDto.setElementId(elementDto.getElementId());
        
        // 从properties Map中提取需求特定字段
        if (elementDto.getProperties() != null) {
            Map<String, Object> props = elementDto.getProperties();
            
            // 业务标识字段
            reqDto.setReqId(getStringProperty(props, "reqId"));
            reqDto.setDeclaredName(getStringProperty(props, "declaredName"));
            reqDto.setDeclaredShortName(getStringProperty(props, "declaredShortName"));
            
            // 文档字段
            reqDto.setDocumentation(getStringProperty(props, "documentation"));
            reqDto.setRenderedText(getStringProperty(props, "renderedText"));
            
            // 需求工程字段
            reqDto.setStatus(getStringProperty(props, "status"));
            reqDto.setPriority(getStringProperty(props, "priority"));
            reqDto.setVerificationMethod(getStringProperty(props, "verificationMethod"));
            reqDto.setCategory(getStringProperty(props, "category"));
            reqDto.setSource(getStringProperty(props, "source"));
            reqDto.setRiskLevel(getStringProperty(props, "riskLevel"));
            
            // 【REQ-C2-1】RequirementUsage特有字段
            reqDto.setRequirementDefinition(getStringProperty(props, "requirementDefinition"));
            
            // 时间戳字段
            reqDto.setCreatedAt(getStringProperty(props, "createdAt"));
            reqDto.setUpdatedAt(getStringProperty(props, "updatedAt"));
        }
        
        return reqDto;
    }
    
    /**
     * 【REQ-B2-4】将RequirementDTO转换为元素数据Map
     * 用于创建和更新需求定义/使用的操作
     * @param requirementDto RequirementDTO对象
     * @return 元素数据Map
     */
    public Map<String, Object> toElementData(RequirementDTO requirementDto) {
        Map<String, Object> data = new HashMap<>();
        
        if (requirementDto == null) {
            return data;
        }
        
        // 必填字段：elementId
        if (requirementDto.getElementId() != null) {
            data.put("elementId", requirementDto.getElementId());
        }
        
        // 业务标识字段
        if (requirementDto.getReqId() != null) {
            data.put("reqId", requirementDto.getReqId());
        }
        if (requirementDto.getDeclaredName() != null) {
            data.put("declaredName", requirementDto.getDeclaredName());
        }
        if (requirementDto.getDeclaredShortName() != null) {
            data.put("declaredShortName", requirementDto.getDeclaredShortName());
        }
        
        // 文档字段
        if (requirementDto.getDocumentation() != null) {
            data.put("documentation", requirementDto.getDocumentation());
        }
        if (requirementDto.getRenderedText() != null) {
            data.put("renderedText", requirementDto.getRenderedText());
        }
        
        // 需求工程字段
        if (requirementDto.getStatus() != null) {
            data.put("status", requirementDto.getStatus());
        }
        if (requirementDto.getPriority() != null) {
            data.put("priority", requirementDto.getPriority());
        }
        if (requirementDto.getVerificationMethod() != null) {
            data.put("verificationMethod", requirementDto.getVerificationMethod());
        }
        if (requirementDto.getCategory() != null) {
            data.put("category", requirementDto.getCategory());
        }
        if (requirementDto.getSource() != null) {
            data.put("source", requirementDto.getSource());
        }
        if (requirementDto.getRiskLevel() != null) {
            data.put("riskLevel", requirementDto.getRiskLevel());
        }
        
        // 【REQ-C2-1】RequirementUsage特有字段
        if (requirementDto.getRequirementDefinition() != null) {
            data.put("requirementDefinition", requirementDto.getRequirementDefinition());
        }
        
        return data;
    }
    
    /**
     * 【REQ-A2-1】将ElementDTO转换为TraceDTO
     * 专门用于追溯关系相关API的响应
     * @param elementDto ElementDTO对象
     * @return TraceDTO，如果输入为null返回null
     */
    public TraceDTO toTraceDTO(ElementDTO elementDto) {
        if (elementDto == null) {
            return null;
        }
        
        TraceDTO traceDto = new TraceDTO();
        
        // 基本字段
        traceDto.setElementId(elementDto.getElementId());
        
        // 从properties Map中提取追溯关系特定字段
        if (elementDto.getProperties() != null) {
            Map<String, Object> props = elementDto.getProperties();
            
            // 追溯关系核心字段
            traceDto.setSource(getStringProperty(props, "fromId"));
            traceDto.setTarget(getStringProperty(props, "toId"));
            traceDto.setType(getStringProperty(props, "type"));
            
            // 追溯关系描述字段
            traceDto.setName(getStringProperty(props, "name"));
            traceDto.setDescription(getStringProperty(props, "description"));
            traceDto.setRationale(getStringProperty(props, "rationale"));
            
            // 语义约束验证字段
            traceDto.setSourceType(getStringProperty(props, "sourceType"));
            traceDto.setTargetType(getStringProperty(props, "targetType"));
            
            // 验证状态字段
            Object validObj = props.get("valid");
            if (validObj instanceof Boolean) {
                traceDto.setValid((Boolean) validObj);
            }
            traceDto.setValidationMessage(getStringProperty(props, "validationMessage"));
            
            // 时间戳字段
            traceDto.setCreatedAt(getStringProperty(props, "createdAt"));
            traceDto.setUpdatedAt(getStringProperty(props, "updatedAt"));
        }
        
        return traceDto;
    }
    
    /**
     * 【REQ-A2-1】将TraceDTO转换为元素数据Map
     * 用于创建和更新追溯关系的操作
     * @param traceDto TraceDTO对象
     * @return 元素数据Map
     */
    public Map<String, Object> toElementData(TraceDTO traceDto) {
        Map<String, Object> data = new HashMap<>();
        
        if (traceDto == null) {
            return data;
        }
        
        // 必填字段：elementId
        if (traceDto.getElementId() != null) {
            data.put("elementId", traceDto.getElementId());
        }
        
        // 追溯关系核心字段
        if (traceDto.getSource() != null) {
            data.put("fromId", traceDto.getSource());
        }
        if (traceDto.getTarget() != null) {
            data.put("toId", traceDto.getTarget());
        }
        if (traceDto.getType() != null) {
            data.put("type", traceDto.getType());
        }
        
        // 追溯关系描述字段
        if (traceDto.getName() != null) {
            data.put("name", traceDto.getName());
        }
        if (traceDto.getDescription() != null) {
            data.put("description", traceDto.getDescription());
        }
        if (traceDto.getRationale() != null) {
            data.put("rationale", traceDto.getRationale());
        }
        
        return data;
    }
    
    /**
     * 【REQ-A2-1】便捷方法：将多个ElementDTO转换为TraceDTO列表
     * @param elementDtos ElementDTO列表
     * @return TraceDTO列表
     */
    public java.util.List<TraceDTO> toTraceDTOList(java.util.List<ElementDTO> elementDtos) {
        if (elementDtos == null) {
            return new java.util.ArrayList<>();
        }
        
        return elementDtos.stream()
            .map(this::toTraceDTO)
            .filter(java.util.Objects::nonNull)
            .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 辅助方法：从属性Map中安全地获取字符串值
     * @param properties 属性Map
     * @param key 属性键
     * @return 字符串值，如果不存在或不是字符串则返回null
     */
    private String getStringProperty(Map<String, Object> properties, String key) {
        Object value = properties.get(key);
        return value != null ? value.toString() : null;
    }
}