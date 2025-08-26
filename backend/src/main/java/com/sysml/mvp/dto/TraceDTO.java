package com.sysml.mvp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 追溯关系数据传输对象
 * 
 * 需求实现：
 * - REQ-C3-1: 创建追溯关系 - API层简化表示，type映射到具体EClass
 * - REQ-C3-2: 查询追溯关系 - 返回TraceDTO格式，支持type和element过滤
 * - REQ-C3-4: 追溯语义约束 - 支持语义验证字段
 * 
 * API层与Service层映射说明：
 * - API层使用简单的type字符串（derive/satisfy/refine/trace）
 * - Service层映射到具体的EClass实例（DeriveRequirement/Satisfy/Refine/Trace）
 * - source/target对应Service层的fromId/toId
 * 
 * 设计说明：
 * 1. 用于追溯关系相关API的数据传输，简化复杂的EMF Dependency结构
 * 2. type字段提供API层的简单接口，隐藏EMF的复杂继承体系
 * 3. 支持语义约束验证，确保追溯关系的正确性
 * 4. 包含描述信息，便于理解追溯关系的业务含义
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TraceDTO {
    
    /**
     * 追溯关系唯一标识符
     * 对应EMF elementId属性
     * 格式：trace-{uuid}
     */
    private String elementId;
    
    /**
     * 【REQ-C3-1】追溯关系源端
     * API层概念，对应Service层的fromId
     * 指向追溯关系的起始元素ID
     */
    private String source;
    
    /**
     * 【REQ-C3-1】追溯关系目标端  
     * API层概念，对应Service层的toId
     * 指向追溯关系的目标元素ID
     */
    private String target;
    
    /**
     * 【REQ-C3-2】追溯类型
     * API层简化表示，支持四种类型：
     * - derive: 需求派生需求（映射到DeriveRequirement）
     * - satisfy: 实现满足需求（映射到Satisfy）  
     * - refine: 需求细化需求（映射到Refine）
     * - trace: 通用追溯关系（映射到Trace）
     */
    private String type;
    
    /**
     * 追溯关系名称
     * 用于显示和标识追溯关系
     */
    private String name;
    
    /**
     * 追溯关系描述
     * 说明该追溯关系的业务含义和目的
     */
    private String description;
    
    /**
     * 追溯关系理由
     * 建立此追溯关系的原因或依据
     */
    private String rationale;
    
    /**
     * 【REQ-C3-4】源端元素类型
     * 用于语义约束验证
     * 例如：RequirementUsage, PartUsage, ActionUsage等
     */
    private String sourceType;
    
    /**
     * 【REQ-C3-4】目标端元素类型
     * 用于语义约束验证  
     * 例如：RequirementDefinition, RequirementUsage等
     */
    private String targetType;
    
    /**
     * 【REQ-C3-4】语义验证状态
     * true表示符合追溯语义约束，false表示违反约束
     */
    private Boolean valid;
    
    /**
     * 【REQ-C3-4】验证消息
     * 当语义验证失败时的错误信息
     */
    private String validationMessage;
    
    /**
     * 创建时间
     * ISO-8601 UTC格式：YYYY-MM-DDTHH:mm:ss.SSSZ
     * 由服务器生成和维护
     */
    private String createdAt;
    
    /**
     * 更新时间
     * ISO-8601 UTC格式：YYYY-MM-DDTHH:mm:ss.SSSZ
     * 由服务器生成和维护
     */
    private String updatedAt;
    
    /**
     * 【REQ-C3-2】便捷方法：检查是否为derive类型
     * @return true如果类型为derive
     */
    public boolean isDeriveType() {
        return "derive".equals(type);
    }
    
    /**
     * 【REQ-C3-2】便捷方法：检查是否为satisfy类型
     * @return true如果类型为satisfy
     */
    public boolean isSatisfyType() {
        return "satisfy".equals(type);
    }
    
    /**
     * 【REQ-C3-2】便捷方法：检查是否为refine类型
     * @return true如果类型为refine
     */
    public boolean isRefineType() {
        return "refine".equals(type);
    }
    
    /**
     * 【REQ-C3-2】便捷方法：检查是否为trace类型
     * @return true如果类型为trace
     */
    public boolean isTraceType() {
        return "trace".equals(type);
    }
    
    /**
     * 【REQ-C3-4】便捷方法：检查语义约束是否有效
     * @return true如果通过语义验证
     */
    public boolean isSemanticValid() {
        return Boolean.TRUE.equals(valid);
    }
    
    /**
     * 【REQ-C3-3】便捷方法：生成去重键
     * 用于检测相同(source,target,type)的重复关系
     * @return 去重键字符串
     */
    public String getDuplicateKey() {
        return source + "|" + target + "|" + type;
    }
    
    /**
     * 【REQ-C3-4】便捷方法：验证Satisfy语义约束
     * Satisfy: source∈{PartUsage,ActionUsage}, target∈{RequirementUsage,RequirementDefinition}
     * @return true如果符合Satisfy语义约束
     */
    public boolean isValidSatisfySemantics() {
        if (!"satisfy".equals(type)) {
            return false;
        }
        
        boolean validSource = "PartUsage".equals(sourceType) || 
                             "ActionUsage".equals(sourceType) ||
                             sourceType != null && sourceType.contains("Function");
        
        boolean validTarget = "RequirementUsage".equals(targetType) || 
                             "RequirementDefinition".equals(targetType);
        
        return validSource && validTarget;
    }
    
    /**
     * 【REQ-C3-4】便捷方法：验证DeriveRequirement语义约束
     * DeriveRequirement: source/target∈{RequirementDefinition,RequirementUsage}
     * @return true如果符合DeriveRequirement语义约束
     */
    public boolean isValidDeriveSemantics() {
        if (!"derive".equals(type)) {
            return false;
        }
        
        boolean validSource = "RequirementDefinition".equals(sourceType) || 
                             "RequirementUsage".equals(sourceType);
        
        boolean validTarget = "RequirementDefinition".equals(targetType) || 
                             "RequirementUsage".equals(targetType);
        
        return validSource && validTarget;
    }
    
    /**
     * 【REQ-C3-4】便捷方法：验证Refine语义约束
     * Refine: source/target∈{RequirementDefinition,RequirementUsage}
     * @return true如果符合Refine语义约束
     */
    public boolean isValidRefineSemantics() {
        if (!"refine".equals(type)) {
            return false;
        }
        
        boolean validSource = "RequirementDefinition".equals(sourceType) || 
                             "RequirementUsage".equals(sourceType);
        
        boolean validTarget = "RequirementDefinition".equals(targetType) || 
                             "RequirementUsage".equals(targetType);
        
        return validSource && validTarget;
    }
    
    /**
     * 便捷方法：获取显示名称
     * 优先返回name，否则基于type生成默认名称
     * @return 用于显示的名称
     */
    public String getDisplayName() {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        
        String typeName = type != null ? type.substring(0, 1).toUpperCase() + type.substring(1) : "Trace";
        return typeName + " Relationship";
    }
    
    /**
     * 便捷方法：检查是否为需求间的追溯关系
     * @return true如果source和target都是需求类型
     */
    public boolean isRequirementTrace() {
        return (sourceType != null && sourceType.startsWith("Requirement")) &&
               (targetType != null && targetType.startsWith("Requirement"));
    }
}