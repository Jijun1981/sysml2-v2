package com.sysml.mvp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 需求数据传输对象
 * 
 * 需求实现：
 * - REQ-B2-4: DTO选择性映射 - 需求特定字段，只包含需要的字段
 * - REQ-C1-1: 创建需求定义 - reqId唯一业务标识
 * - REQ-C1-2: 查询需求定义 - 返回RequirementDTO格式
 * - REQ-C1-4: 参数化文本渲染 - 支持模板和渲染文本
 * 
 * 设计说明：
 * 1. 专门用于需求相关API的数据传输，包含需求特定字段
 * 2. reqId作为业务层面的唯一标识（对应Pilot的declaredShortName）
 * 3. 支持参数化文本：documentation（模板）+ renderedText（渲染结果）
 * 4. 包含需求工程中的标准字段：priority、verificationMethod、status等
 * 5. 与ElementDTO配合使用：ElementDTO用于通用操作，RequirementDTO用于需求特定场景
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequirementDTO {
    
    /**
     * 元素唯一标识符
     * 对应EMF elementId属性
     * 格式：req-def-{uuid} 或 req-usage-{uuid}
     */
    private String elementId;
    
    /**
     * 【REQ-C1-1】需求业务标识符（reqId）
     * 对应Pilot declaredShortName字段
     * 层次编码格式：EBS-L1-001、EBS-L2-BMS-001、EBS-L3-SENSOR-001
     * 用于需求唯一性验证（409冲突检查）
     */
    private String reqId;
    
    /**
     * 需求名称
     * 对应Pilot declaredName字段
     * 用于显示和标识需求
     */
    private String declaredName;
    
    /**
     * 需求短名称
     * 对应Pilot declaredShortName字段
     * 通常与reqId相同或相关
     */
    private String declaredShortName;
    
    /**
     * 【REQ-C1-4】需求文本/模板文本
     * 对应Pilot documentation字段
     * 支持${placeholder}占位符语法，如：
     * "The ${subject} shall achieve ${performance} within ${window}."
     */
    private String documentation;
    
    /**
     * 【REQ-C1-4】参数化文本渲染结果
     * 当documentation包含占位符时，绑定参数后的渲染文本
     * 例如："The Engine shall achieve 100kW within 10min."
     */
    private String renderedText;
    
    /**
     * 需求状态
     * 枚举值：draft, active, deprecated, obsolete
     */
    private String status;
    
    /**
     * 需求优先级
     * 枚举值：P0（必须）、P1（重要）、P2（可选）
     */
    private String priority;
    
    /**
     * 验证方法
     * 枚举值：test（测试）、analysis（分析）、inspection（检查）、demonstration（演示）
     */
    private String verificationMethod;
    
    /**
     * 需求类别
     * 枚举值：functional（功能性）、performance（性能）、safety（安全）、usability（可用性）
     */
    private String category;
    
    /**
     * 需求来源
     * 枚举值：stakeholder（干系人）、regulation（法规）、standard（标准）、derived（派生）
     */
    private String source;
    
    /**
     * 风险等级
     * 枚举值：low（低）、medium（中）、high（高）、critical（关键）
     */
    private String riskLevel;
    
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
     * 便捷方法：检查是否为活跃状态
     * @return true如果状态为active
     */
    public boolean isActive() {
        return "active".equals(status);
    }
    
    /**
     * 便捷方法：检查是否为高优先级
     * @return true如果优先级为P0
     */
    public boolean isHighPriority() {
        return "P0".equals(priority);
    }
    
    /**
     * 便捷方法：检查是否有渲染文本
     * @return true如果renderedText不为空
     */
    public boolean hasRenderedText() {
        return renderedText != null && !renderedText.trim().isEmpty();
    }
    
    /**
     * 便捷方法：获取显示文本
     * 优先返回渲染文本，否则返回原始文档
     * @return 用于显示的文本
     */
    public String getDisplayText() {
        return hasRenderedText() ? renderedText : documentation;
    }
    
    /**
     * 便捷方法：检查是否为需求定义
     * 根据elementId前缀判断
     * @return true如果是RequirementDefinition
     */
    public boolean isRequirementDefinition() {
        return elementId != null && elementId.startsWith("req-def-");
    }
    
    /**
     * 便捷方法：检查是否为需求使用
     * 根据elementId前缀判断
     * @return true如果是RequirementUsage
     */
    public boolean isRequirementUsage() {
        return elementId != null && elementId.startsWith("req-usage-");
    }
}