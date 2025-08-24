package com.sysml.mvp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 图视图数据DTO
 * 用于REQ-D3-1 GET /graph接口返回nodes和edges
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphDataDTO {
    
    private List<GraphNodeDTO> nodes;
    
    private List<GraphEdgeDTO> edges;
    
    private int totalNodes;
    
    private int totalEdges;
}