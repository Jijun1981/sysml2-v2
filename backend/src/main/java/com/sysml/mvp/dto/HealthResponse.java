package com.sysml.mvp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 健康检查响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthResponse {
    private String status;
    private String buildVersion;
    private String gitCommit;
    private String serverTimeUtc;
    private String storage;
}