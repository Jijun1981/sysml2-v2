package com.sysml.mvp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 图视图节点DTO
 * 用于REQ-D3-1 GET /graph接口
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphNodeDTO {
    
    private String id;
    
    private String type; // "definition" | "usage"
    
    private String label;
    
    private String reqId;
    
    private String status;
    
    private List<String> tags;
}