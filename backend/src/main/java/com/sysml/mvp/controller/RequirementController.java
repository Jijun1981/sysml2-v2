package com.sysml.mvp.controller;

import com.sysml.mvp.dto.RequirementDefinitionDTO;
import com.sysml.mvp.service.RequirementService;
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
 * 需求管理控制器
 * 实现 REQ-C1-1, REQ-C1-2, REQ-C1-3, REQ-C2-1, REQ-C2-2
 */
@Slf4j
@RestController
@RequestMapping("/requirements")
@RequiredArgsConstructor
@Validated
@Tag(name = "Requirements", description = "需求管理API")
public class RequirementController {
    
    private final RequirementService requirementService;
    
    /**
     * REQ-C1-1 & REQ-C2-1: 创建需求（定义或用法）
     * AC: POST /requirements（type=definition）缺reqId|name|text→400；成功201返回对象与Location
     * AC: POST /requirements（type=usage, of=defId）缺of→400；defId不存在→404
     */
    @PostMapping
    @Operation(summary = "创建需求", description = "创建新的需求定义或用法，type为definition或usage")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "参数错误或缺少必填字段"),
        @ApiResponse(responseCode = "404", description = "引用的定义不存在"),
        @ApiResponse(responseCode = "409", description = "reqId重复")
    })
    public ResponseEntity<RequirementDefinitionDTO> createRequirement(
            @Valid @RequestBody RequirementDefinitionDTO.CreateRequest request) {
        
        log.info("收到创建需求请求: type={}, reqId={}, of={}", request.getType(), request.getReqId(), request.getOf());
        
        RequirementDefinitionDTO created = requirementService.createRequirement(request);
        
        // 构建Location header
        URI location = URI.create("/requirements/" + created.getId());
        
        return ResponseEntity.created(location).body(created);
    }
    
    /**
     * REQ-C1-2 & REQ-C2-2: 获取需求（定义或用法）
     * AC: GET /requirements/{id}可用
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取需求", description = "根据ID获取需求定义或用法详情")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "需求不存在")
    })
    public ResponseEntity<RequirementDefinitionDTO> getRequirement(
            @Parameter(description = "需求ID") @PathVariable String id) {
        
        log.debug("获取需求: id={}", id);
        
        RequirementDefinitionDTO requirement = requirementService.getRequirement(id);
        return ResponseEntity.ok(requirement);
    }
    
    /**
     * REQ-C1-2 & REQ-C2-2: 更新需求（定义或用法）
     * AC: PUT /requirements/{id}；允许更新name,text,doc,tags（definition）
     * AC: PUT /requirements/{id}；允许更新name,text,status,tags（usage）
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新需求", description = "更新需求定义或用法的允许字段")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "404", description = "需求不存在")
    })
    public ResponseEntity<RequirementDefinitionDTO> updateRequirement(
            @Parameter(description = "需求ID") @PathVariable String id,
            @Valid @RequestBody RequirementDefinitionDTO.UpdateRequest request) {
        
        log.info("更新需求: id={}", id);
        
        RequirementDefinitionDTO updated = requirementService.updateRequirement(id, request);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * REQ-C1-2 & REQ-C2-2: 删除需求（定义或用法）
     * AC: DELETE /requirements/{id}；被引用删除→409（definition）
     * AC: DELETE /requirements/{id}；存在Trace时删除→409（usage，返回阻塞traceIds）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除需求", description = "删除指定的需求定义或用法")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "需求不存在"),
        @ApiResponse(responseCode = "409", description = "需求被引用或存在Trace关系，无法删除")
    })
    public ResponseEntity<Void> deleteRequirement(
            @Parameter(description = "需求ID") @PathVariable String id) {
        
        log.info("删除需求: id={}", id);
        
        requirementService.deleteRequirement(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 获取所有需求列表（支持类型筛选）
     */
    @GetMapping
    @Operation(summary = "获取需求列表", description = "获取所有需求的列表，支持type参数筛选")
    @ApiResponse(responseCode = "200", description = "获取成功")
    public ResponseEntity<List<RequirementDefinitionDTO>> listRequirements(
            @Parameter(description = "类型筛选: definition, usage 或空（所有）") 
            @RequestParam(required = false) String type) {
        
        log.debug("获取需求列表: type={}", type);
        
        List<RequirementDefinitionDTO> requirements;
        if ("definition".equals(type)) {
            requirements = requirementService.listDefinitions();
        } else if ("usage".equals(type)) {
            requirements = requirementService.listUsages();
        } else {
            requirements = requirementService.listRequirements();
        }
        
        return ResponseEntity.ok(requirements);
    }
    
    /**
     * 全局异常处理器会处理ResponseStatusException
     * 这里不需要额外的异常处理方法
     */
}