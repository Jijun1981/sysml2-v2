package com.sysml.mvp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;

/**
 * Trace关系DTO
 * 实现REQ-C3-1到REQ-C3-4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraceDTO {
    
    private String id;
    
    @NotBlank(message = "fromId不能为空")
    private String fromId;
    
    @NotBlank(message = "toId不能为空")
    private String toId;
    
    @NotBlank(message = "type不能为空")
    @Pattern(regexp = "derive|satisfy|refine|trace", 
             message = "type必须是derive、satisfy、refine或trace中的一种")
    private String type;
    
    private Instant createdAt;
    
    // 用于创建Trace请求 - REQ-C3-1
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "toId不能为空")
        private String toId;
        
        @NotNull(message = "type不能为空")
        @Pattern(regexp = "derive|satisfy|refine|trace", 
                 message = "type必须是derive、satisfy、refine或trace中的一种")
        private String type;
    }
}