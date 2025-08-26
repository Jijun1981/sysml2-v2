package com.sysml.mvp.controller;

import com.sysml.mvp.dto.ElementDTO;
import com.sysml.mvp.dto.TraceDTO;
import com.sysml.mvp.service.TraceService;
import com.sysml.mvp.service.ValidationService;
import com.sysml.mvp.mapper.ElementMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 追溯关系控制器
 * 
 * 需求实现：
 * - REQ-A2-1: 追溯关系CRUD API - 完整的REST API端点
 * - REQ-C3-1: 创建追溯关系 - POST /api/v1/traces
 * - REQ-C3-2: 查询追溯关系 - GET /api/v1/traces
 * - REQ-C3-3: 追溯关系去重检测 - 409 Conflict响应
 * - REQ-C3-4: 追溯关系语义约束验证 - 400 Bad Request响应
 * - REQ-C3-5: 删除追溯关系 - DELETE /api/v1/traces/{id}
 * 
 * 设计说明：
 * 1. 提供标准的REST API端点用于追溯关系管理
 * 2. 业务逻辑委托给TraceService处理
 * 3. 使用ValidationService进行重复性和语义约束验证
 * 4. 使用ElementMapper进行DTO转换
 * 5. 标准HTTP状态码和错误处理
 */
@RestController
@RequestMapping("/api/v1/traces")
public class TraceController {
    
    private final TraceService traceService;
    private final ValidationService validationService;
    private final ElementMapper elementMapper;
    
    public TraceController(TraceService traceService, ValidationService validationService, ElementMapper elementMapper) {
        this.traceService = traceService;
        this.validationService = validationService;
        this.elementMapper = elementMapper;
    }
    
    /**
     * 【REQ-C3-1】创建追溯关系
     * @param traceDto 追溯关系数据
     * @return 201 Created 和创建的追溯关系，或409 Conflict如果重复，或400 Bad Request如果语义无效
     */
    @PostMapping
    public ResponseEntity<?> createTrace(@RequestBody TraceDTO traceDto) {
        // 【REQ-C3-3】检查重复性
        boolean isDuplicate = !validationService.validateTraceDuplication(
            traceDto.getSource(), traceDto.getTarget(), traceDto.getType());
        
        if (isDuplicate) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Conflict");
            error.put("message", String.format("Duplicate trace relationship: %s %s %s", 
                traceDto.getSource(), traceDto.getType(), traceDto.getTarget()));
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
        
        // 【REQ-C3-4】检查语义约束
        boolean isSemanticValid = validationService.validateTraceSemantics(
            traceDto.getSource(), traceDto.getTarget(), traceDto.getType());
        
        if (!isSemanticValid) {
            String validationMessage = validationService.getTraceSemanticValidationMessage(
                traceDto.getSource(), traceDto.getTarget(), traceDto.getType());
            
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Bad Request");
            error.put("message", validationMessage);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
        
        try {
            Map<String, Object> elementData = elementMapper.toElementData(traceDto);
            ElementDTO createdElement = traceService.createTrace(elementData);
            TraceDTO responseDto = elementMapper.toTraceDTO(createdElement);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Bad Request");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * 【REQ-C3-2】查询追溯关系
     * 支持按type、fromId、toId过滤
     * @param type 可选，追溯类型过滤
     * @param fromId 可选，源端元素ID过滤
     * @param toId 可选，目标端元素ID过滤
     * @return 200 OK 和追溯关系列表
     */
    @GetMapping
    public ResponseEntity<List<TraceDTO>> getTraces(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String fromId,
            @RequestParam(required = false) String toId) {
        
        List<ElementDTO> traces;
        
        if (type != null) {
            traces = traceService.getTracesByType(type);
        } else if (fromId != null) {
            traces = traceService.getTracesByFromId(fromId);
        } else if (toId != null) {
            traces = traceService.getTracesByToId(toId);
        } else {
            traces = traceService.getAllTraces();
        }
        
        List<TraceDTO> responseList = elementMapper.toTraceDTOList(traces);
        return ResponseEntity.ok(responseList);
    }
    
    /**
     * 【REQ-C3-2】根据ID查询追溯关系
     * @param id 追溯关系ID
     * @return 200 OK 和追溯关系，或404 Not Found如果不存在
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTraceById(@PathVariable String id) {
        ElementDTO trace = traceService.getTraceById(id);
        if (trace == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Not Found");
            error.put("message", "Trace not found: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        
        TraceDTO responseDto = elementMapper.toTraceDTO(trace);
        return ResponseEntity.ok(responseDto);
    }
    
    /**
     * 【REQ-C3-5】删除追溯关系
     * @param id 追溯关系ID
     * @return 204 No Content，或404 Not Found如果不存在
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTrace(@PathVariable String id) {
        try {
            traceService.deleteTrace(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Not Found");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    /**
     * 【REQ-C3-4】验证追溯关系语义约束
     * @param source 源端元素ID
     * @param target 目标端元素ID
     * @param type 追溯类型
     * @return 200 OK 和验证结果
     */
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateTraceSemantics(
            @RequestParam String source,
            @RequestParam String target,
            @RequestParam String type) {
        
        Map<String, Object> response = new HashMap<>();
        
        // 检查重复性
        boolean isDuplicate = !validationService.validateTraceDuplication(source, target, type);
        response.put("isDuplicate", isDuplicate);
        
        // 检查语义约束
        boolean isSemanticValid = validationService.validateTraceSemantics(source, target, type);
        response.put("isSemanticValid", isSemanticValid);
        
        // 获取验证消息
        String validationMessage = validationService.getTraceSemanticValidationMessage(source, target, type);
        response.put("validationMessage", validationMessage);
        
        // 综合有效性
        boolean isValid = !isDuplicate && isSemanticValid;
        response.put("isValid", isValid);
        
        return ResponseEntity.ok(response);
    }
}