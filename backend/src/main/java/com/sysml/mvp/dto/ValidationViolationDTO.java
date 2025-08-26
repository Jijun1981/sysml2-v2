package com.sysml.mvp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 验证违规数据传输对象
 * 
 * 需求实现：
 * - REQ-E1-2: 规则码固定枚举 - 支持DUP_REQID, CYCLE_DERIVE_REFINE, BROKEN_REF
 * - REQ-E1-1: MVP规则集 - 仅检测3条核心规则的违规信息
 * - REQ-E1-3: 接口返回 - 作为ValidationResultDTO的violations元素
 * 
 * 设计说明：
 * 1. 表示单个验证规则的违规信息
 * 2. ruleCode固定为3种枚举值，对应MVP阶段的核心规则
 * 3. 包含违规目标、错误信息和详细描述
 * 4. 提供便捷方法用于规则类型判断
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationViolationDTO {
    
    /**
     * 【REQ-E1-2】规则码
     * 固定枚举值：
     * - DUP_REQID: 需求ID重复
     * - CYCLE_DERIVE_REFINE: 派生/细化循环依赖
     * - BROKEN_REF: 悬挂引用
     */
    private String ruleCode;
    
    /**
     * 违规目标元素ID
     * 指向发生违规的具体元素
     */
    private String targetId;
    
    /**
     * 违规消息
     * 简要描述违规内容
     */
    private String message;
    
    /**
     * 违规详细信息
     * 提供更详细的违规上下文和建议
     */
    private String details;
    
    /**
     * 【REQ-E1-2】便捷方法：检查是否为重复reqId违规
     * @return true如果规则码为DUP_REQID
     */
    public boolean isDuplicateReqId() {
        return "DUP_REQID".equals(ruleCode);
    }
    
    /**
     * 【REQ-E1-2】便捷方法：检查是否为循环依赖违规
     * @return true如果规则码为CYCLE_DERIVE_REFINE
     */
    public boolean isCyclicDependency() {
        return "CYCLE_DERIVE_REFINE".equals(ruleCode);
    }
    
    /**
     * 【REQ-E1-2】便捷方法：检查是否为悬挂引用违规
     * @return true如果规则码为BROKEN_REF
     */
    public boolean isBrokenReference() {
        return "BROKEN_REF".equals(ruleCode);
    }
}