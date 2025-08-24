package com.sysml.mvp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * 表格视图行DTO
 * 用于REQ-D2-1 GET /table接口
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableRowDTO {
    
    private String id;
    
    private String reqId;
    
    private String name;
    
    private String type; // "definition" | "usage"
    
    private String status;
    
    private List<String> tags;
    
    private String text;
    
    private String of; // for usage: reference to definition id
    
    private Instant createdAt;
    
    private Instant updatedAt;
}