package com.sysml.mvp.controller;

import com.sysml.mvp.dto.TraceDTO;
import com.sysml.mvp.service.TraceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;

/**
 * Trace关系管理控制器
 * 实现 REQ-C3-1, REQ-C3-2, REQ-C3-3, REQ-C3-4
 * 
 * @deprecated 此控制器已被UniversalElementController替代。
 * 追溯关系现在通过创建Dependency类型实现：/api/v1/elements
 * 此类仅为兼容性保留，计划在下个版本删除。
 */
@Deprecated
@Slf4j
@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "Traces (Deprecated)", description = "Trace关系管理API - 已废弃，请使用UniversalElementController")
public class TraceController {
    
    private final TraceService traceService;
    
    /**
     * REQ-C3-1: 创建Trace关系
     * POST /requirements/{id}/traces {toId,type}
     * type∈{derive,satisfy,refine,trace}；fromId==toId→400；toId不存在→404；成功对象含createdAt(UTC)
     * REQ-C3-3: 同(from,to,type)不重复创建；重复请求返回既有对象200
     */
    @PostMapping("/requirements/{fromId}/traces")
    @Operation(summary = "创建Trace关系", description = "在两个需求之间创建Trace关系，支持derive、satisfy、refine、trace四种类型")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "创建成功"),
        @ApiResponse(responseCode = "200", description = "重复请求，返回既有对象"),
        @ApiResponse(responseCode = "400", description = "参数错误或fromId等于toId"),
        @ApiResponse(responseCode = "404", description = "fromId或toId对应的需求不存在")
    })
    public ResponseEntity<TraceDTO> createTrace(
            @Parameter(description = "源需求ID") @PathVariable String fromId,
            @Valid @RequestBody TraceDTO.CreateRequest request) {
        
        log.info("收到创建Trace请求: fromId={}, toId={}, type={}", fromId, request.getToId(), request.getType());
        
        // REQ-C3-3: 检查是否已存在相同的(from,to,type)组合
        TraceDTO existing = traceService.findExistingTrace(fromId, request.getToId(), request.getType());
        if (existing != null) {
            log.info("发现重复的Trace关系，返回既有对象: id={}", existing.getId());
            return ResponseEntity.ok(existing); // 200 OK for duplicate
        }
        
        TraceDTO created = traceService.createTrace(fromId, request);
        
        // 构建Location header
        URI location = URI.create("/traces/" + created.getId());
        
        return ResponseEntity.created(location).body(created); // 201 Created for new
    }
    
    /**
     * REQ-C3-2: 获取需求的Trace关系
     * GET /requirements/{id}/traces?dir=in|out|both 返回入/出边
     */
    @GetMapping("/requirements/{id}/traces")
    @Operation(summary = "获取需求的Trace关系", description = "获取指定需求的所有Trace关系，支持方向筛选")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "需求不存在")
    })
    public ResponseEntity<List<TraceDTO>> getTracesByRequirement(
            @Parameter(description = "需求ID") @PathVariable String id,
            @Parameter(description = "方向筛选: in（入边）, out（出边）, both（双向，默认）") 
            @RequestParam(required = false, defaultValue = "both") String dir) {
        
        log.debug("获取需求的Trace关系: id={}, dir={}", id, dir);
        
        // 验证dir参数
        if (!"in".equals(dir) && !"out".equals(dir) && !"both".equals(dir)) {
            log.warn("无效的dir参数: {}, 使用默认值both", dir);
            dir = "both";
        }
        
        List<TraceDTO> traces = traceService.getTracesByRequirement(id, dir);
        return ResponseEntity.ok(traces);
    }
    
    /**
     * REQ-C3-4: 删除Trace关系
     * DELETE /traces/{traceId}→204；不存在→404
     */
    @GetMapping("/traces/{traceId}")
    @Operation(summary = "获取单个Trace关系", description = "根据ID获取指定的Trace关系")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "成功获取Trace关系"),
        @ApiResponse(responseCode = "404", description = "Trace关系不存在")
    })
    public ResponseEntity<TraceDTO> getTrace(
            @Parameter(description = "Trace关系ID") @PathVariable String traceId) {
        
        log.info("获取Trace关系: traceId={}", traceId);
        
        TraceDTO trace = traceService.getTrace(traceId);
        return ResponseEntity.ok(trace);
    }
    
    @DeleteMapping("/traces/{traceId}")
    @Operation(summary = "删除Trace关系", description = "删除指定的Trace关系")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "Trace关系不存在")
    })
    public ResponseEntity<Void> deleteTrace(
            @Parameter(description = "Trace关系ID") @PathVariable String traceId) {
        
        log.info("删除Trace关系: traceId={}", traceId);
        
        traceService.deleteTrace(traceId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 全局异常处理器会处理ResponseStatusException
     * 这里不需要额外的异常处理方法
     */
}