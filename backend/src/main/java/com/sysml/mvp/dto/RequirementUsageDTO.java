package com.sysml.mvp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 需求使用DTO
 * 用于RequirementUsage相关操作
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementUsageDTO {
    
    private String id;
    
    private String eClass; // EMF eClass name for type identification
    
    private String type; // API type (usage)
    
    private String of; // reference to RequirementDefinition id
    
    private String name;
    
    private String text;
    
    private String status;
    
    private List<String> tags;
    
    private String version;
    
    private Instant createdAt;
    
    private Instant updatedAt;
}