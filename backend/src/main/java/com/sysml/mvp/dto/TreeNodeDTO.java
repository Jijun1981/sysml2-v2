package com.sysml.mvp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 树视图节点DTO
 * 用于REQ-D1-1 GET /tree接口
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreeNodeDTO {
    
    private String id;
    
    private String label;
    
    private String type; // "definition" | "usage"
    
    private String reqId;
    
    private String status;
    
    private List<String> tags;
    
    private List<TreeNodeDTO> children;
    
    private boolean leaf;
}