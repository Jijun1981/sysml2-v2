package com.sysml.mvp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 图视图边DTO  
 * 用于REQ-D3-1 GET /graph接口
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphEdgeDTO {
    
    private String id;
    
    private String from;
    
    private String to;
    
    private String type; // "derive" | "satisfy" | "refine" | "trace"
    
    private Instant createdAt;
}