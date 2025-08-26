package com.sysml.mvp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.ArrayList;

/**
 * 验证结果数据传输对象
 * 
 * 需求实现：
 * - REQ-E1-3: 接口返回 - POST /api/v1/validate/static返回ValidationResultDTO格式
 * - REQ-E1-1: MVP规则集 - 支持3条核心规则的验证结果
 * - REQ-E1-2: 规则码固定枚举 - 包含violations列表，每个违规有固定规则码
 * 
 * 设计说明：
 * 1. 静态验证接口的标准响应格式
 * 2. 包含验证违规列表和验证元数据
 * 3. 支持性能要求：≤500元素<2s处理时间
 * 4. 提供便捷方法用于违规统计和检查
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResultDTO {
    
    /**
     * 【REQ-E1-3】验证违规列表
     * 包含所有检测到的违规信息
     */
    private List<ValidationViolationDTO> violations = new ArrayList<>();
    
    /**
     * 【REQ-E1-3】验证时间戳
     * ISO-8601 UTC格式：YYYY-MM-DDTHH:mm:ss.SSSZ
     */
    private String validatedAt;
    
    /**
     * 【REQ-E1-3】验证的元素数量
     * 用于性能监控，支持≤500元素要求
     */
    private Integer elementCount;
    
    /**
     * 【REQ-E1-3】处理时间（毫秒）
     * 用于性能监控，目标<2000ms
     */
    private Long processingTimeMs;
    
    /**
     * 【REQ-E1-3】验证器版本
     * 标识使用的验证规则版本
     */
    private String version;
    
    /**
     * 【REQ-E1-3】便捷方法：检查是否有违规
     * @return true如果存在违规
     */
    public boolean hasViolations() {
        return violations != null && !violations.isEmpty();
    }
    
    /**
     * 【REQ-E1-3】便捷方法：获取违规总数
     * @return 违规数量
     */
    public int getViolationCount() {
        return violations != null ? violations.size() : 0;
    }
    
    /**
     * 【REQ-E1-3】便捷方法：按规则码统计违规数量
     * @param ruleCode 规则码
     * @return 指定规则码的违规数量
     */
    public int getViolationCountByRuleCode(String ruleCode) {
        if (violations == null || ruleCode == null) {
            return 0;
        }
        
        return (int) violations.stream()
            .filter(v -> ruleCode.equals(v.getRuleCode()))
            .count();
    }
}