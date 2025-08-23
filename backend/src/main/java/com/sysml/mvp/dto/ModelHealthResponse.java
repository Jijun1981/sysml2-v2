package com.sysml.mvp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 模型健康检查响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelHealthResponse {
    private String status;
    private List<PackageInfo> packages;
    private String dataDirectory;
    private Integer projectCount;
    private Integer totalElements;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PackageInfo {
        private String nsUri;
        private String name;
        private String source;
        private Integer classCount;
    }
}