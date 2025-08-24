package com.sysml.mvp.controller;

import com.sysml.mvp.dto.RequirementDefinitionDTO;
import com.sysml.mvp.dto.TraceDTO;
import com.sysml.mvp.service.RequirementService;
import com.sysml.mvp.service.TraceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API控制器 - 为前端提供统一的API接口
 * 映射路径: /api/v1/projects/{projectId}/*
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ApiController {
    
    private final RequirementService requirementService;
    private final TraceService traceService;
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(health);
    }
    
    /**
     * 获取项目的所有需求（包括定义和用法）
     */
    @GetMapping("/projects/{projectId}/requirements")
    public ResponseEntity<Map<String, Object>> getProjectRequirements(
            @PathVariable String projectId) {
        log.info("获取项目需求: projectId={}", projectId);
        
        List<RequirementDefinitionDTO> requirements = requirementService.listRequirements();
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", requirements);
        response.put("projectId", projectId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 创建需求
     */
    @PostMapping("/projects/{projectId}/requirements")
    public ResponseEntity<RequirementDefinitionDTO> createRequirement(
            @PathVariable String projectId,
            @RequestBody RequirementDefinitionDTO.CreateRequest request) {
        log.info("创建需求: projectId={}, type={}", projectId, request.getType());
        
        RequirementDefinitionDTO created = requirementService.createRequirement(request);
        return ResponseEntity.ok(created);
    }
    
    /**
     * 更新需求
     */
    @PutMapping("/projects/{projectId}/requirements/{id}")
    public ResponseEntity<RequirementDefinitionDTO> updateRequirement(
            @PathVariable String projectId,
            @PathVariable String id,
            @RequestBody RequirementDefinitionDTO.UpdateRequest request) {
        log.info("更新需求: projectId={}, id={}", projectId, id);
        
        RequirementDefinitionDTO updated = requirementService.updateRequirement(id, request);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * 删除需求
     */
    @DeleteMapping("/projects/{projectId}/requirements/{id}")
    public ResponseEntity<Void> deleteRequirement(
            @PathVariable String projectId,
            @PathVariable String id) {
        log.info("删除需求: projectId={}, id={}", projectId, id);
        
        requirementService.deleteRequirement(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 获取项目的所有追溯关系
     */
    @GetMapping("/projects/{projectId}/traces")
    public ResponseEntity<Map<String, Object>> getProjectTraces(
            @PathVariable String projectId) {
        log.info("获取项目追溯: projectId={}", projectId);
        
        List<TraceDTO> traces = traceService.findAllTraces();
        
        Map<String, Object> response = new HashMap<>();
        response.put("traces", traces);
        response.put("projectId", projectId);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 创建追溯关系
     */
    @PostMapping("/projects/{projectId}/requirements/{fromId}/traces")
    public ResponseEntity<TraceDTO> createTrace(
            @PathVariable String projectId,
            @PathVariable String fromId,
            @RequestBody TraceDTO.CreateRequest request) {
        log.info("创建追溯: projectId={}, fromId={}, toId={}", projectId, fromId, request.getToId());
        
        TraceDTO created = traceService.createTrace(fromId, request);
        return ResponseEntity.ok(created);
    }
    
    /**
     * 删除追溯关系
     */
    @DeleteMapping("/projects/{projectId}/traces/{traceId}")
    public ResponseEntity<Void> deleteTrace(
            @PathVariable String projectId,
            @PathVariable String traceId) {
        log.info("删除追溯: projectId={}, traceId={}", projectId, traceId);
        
        traceService.deleteTrace(traceId);
        return ResponseEntity.noContent().build();
    }
}