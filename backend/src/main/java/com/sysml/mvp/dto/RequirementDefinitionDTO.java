package com.sysml.mvp.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

/**
 * 需求定义DTO
 * 对应数据模型中的RequirementDefinition
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementDefinitionDTO {
    
    private String id;
    
    @JsonProperty("eClass")
    private String eClass;  // "RequirementDefinition" 或 "RequirementUsage"
    
    @NotBlank(message = "reqId不能为空")
    private String reqId;
    
    // RequirementUsage专用字段
    private String of;  // 引用的RequirementDefinition的ID
    private String status;  // usage专用状态字段
    
    @NotBlank(message = "name不能为空")
    private String name;
    
    @NotBlank(message = "text不能为空")
    private String text;
    
    private String doc;
    
    @Builder.Default
    private List<String> tags = List.of();
    
    @Builder.Default
    private String version = "1.0";
    
    private Instant createdAt;
    private Instant updatedAt;
    
    // 用于创建请求
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotNull
        private String type; // "definition" 或 "usage"
        
        // definition专用字段
        private String reqId;  // definition必需，usage不需要
        
        // usage专用字段
        private String of;  // usage必需，definition不需要
        
        // 通用字段（usage的name和text可以为空）
        private String name;
        private String text;
        private String doc;
        private List<String> tags;
    }
    
    // 用于更新请求
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        // definition字段
        private String reqId;  // 允许更新reqId（会检查唯一性）
        private String name;
        private String text;
        private String doc;
        private List<String> tags;
        
        // usage专用字段
        private String status;  // usage允许更新status
    }
}