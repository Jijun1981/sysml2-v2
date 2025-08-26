package com.sysml.mvp.controller;

import com.sysml.mvp.dto.ElementDTO;
import com.sysml.mvp.dto.RequirementDTO;
import com.sysml.mvp.service.RequirementService;
import com.sysml.mvp.mapper.ElementMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * 需求定义控制器
 * 
 * 需求实现：
 * - REQ-A1-1: 需求定义CRUD API - 完整的REST API端点
 * - REQ-C1-1: 创建需求定义 - POST /api/v1/requirements
 * - REQ-C1-2: 查询需求定义 - GET /api/v1/requirements
 * - REQ-C1-3: 更新需求定义 - PUT /api/v1/requirements/{id}
 * - REQ-C1-4: 参数化文本渲染 - POST /api/v1/requirements/{id}/render
 * - REQ-C2-1: 创建需求使用 - POST /api/v1/requirements/usages
 * - REQ-C2-2: 查询需求使用 - GET /api/v1/requirements/usages
 * 
 * 设计说明：
 * 1. 提供标准的REST API端点
 * 2. 业务逻辑委托给RequirementService处理
 * 3. 使用ElementMapper进行DTO转换
 * 4. 标准HTTP状态码和错误处理
 */
@RestController
@RequestMapping("/api/v1/requirements")
public class RequirementController {
    
    private final RequirementService requirementService;
    private final ElementMapper elementMapper;
    
    public RequirementController(RequirementService requirementService, ElementMapper elementMapper) {
        this.requirementService = requirementService;
        this.elementMapper = elementMapper;
    }
    
    /**
     * 【REQ-C1-1】创建需求定义
     * @param requirementDto 需求定义数据
     * @return 201 Created 和创建的需求定义，或409 Conflict如果reqId重复
     */
    @PostMapping
    public ResponseEntity<?> createRequirement(@RequestBody RequirementDTO requirementDto) {
        try {
            Map<String, Object> elementData = elementMapper.toElementData(requirementDto);
            ElementDTO createdElement = requirementService.createRequirement(elementData);
            RequirementDTO responseDto = elementMapper.toRequirementDTO(createdElement);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Conflict");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
    }
    
    /**
     * 【REQ-C1-2】查询所有需求定义
     * @return 200 OK 和需求定义列表
     */
    @GetMapping
    public ResponseEntity<List<RequirementDTO>> getRequirements() {
        List<ElementDTO> requirements = requirementService.getRequirements();
        List<RequirementDTO> responseList = requirements.stream()
            .map(elementMapper::toRequirementDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }
    
    /**
     * 【REQ-C1-2】根据ID查询需求定义
     * @param id 需求定义ID
     * @return 200 OK 和需求定义，或404 Not Found如果不存在
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getRequirementById(@PathVariable String id) {
        ElementDTO requirement = requirementService.getRequirementById(id);
        if (requirement == null) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Not Found");
            error.put("message", "Requirement not found: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
        
        RequirementDTO responseDto = elementMapper.toRequirementDTO(requirement);
        return ResponseEntity.ok(responseDto);
    }
    
    /**
     * 【REQ-C1-3】更新需求定义
     * @param id 需求定义ID
     * @param requirementDto 更新数据
     * @return 200 OK 和更新后的需求定义
     */
    @PutMapping("/{id}")
    public ResponseEntity<RequirementDTO> updateRequirement(
            @PathVariable String id, 
            @RequestBody RequirementDTO requirementDto) {
        Map<String, Object> updateData = elementMapper.toElementData(requirementDto);
        ElementDTO updatedElement = requirementService.updateRequirement(id, updateData);
        RequirementDTO responseDto = elementMapper.toRequirementDTO(updatedElement);
        return ResponseEntity.ok(responseDto);
    }
    
    /**
     * 【REQ-C1-3】删除需求定义
     * @param id 需求定义ID
     * @return 204 No Content，或409 Conflict如果被引用
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRequirement(@PathVariable String id) {
        try {
            requirementService.deleteRequirement(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Conflict");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }
    }
    
    /**
     * 【REQ-C1-4】渲染参数化文本
     * @param id 需求定义ID
     * @param parameters 参数Map
     * @return 200 OK 和渲染结果
     */
    @PostMapping("/{id}/render")
    public ResponseEntity<?> renderParametricText(
            @PathVariable String id, 
            @RequestBody Map<String, Object> parameters) {
        try {
            String renderedText = requirementService.renderParametricText(id, parameters);
            Map<String, Object> response = new HashMap<>();
            response.put("renderedText", renderedText);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Not Found");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    /**
     * 【REQ-C2-1】创建需求使用
     * @param requirementDto 需求使用数据
     * @return 201 Created 和创建的需求使用，或400 Bad Request如果缺少subject
     */
    @PostMapping("/usages")
    public ResponseEntity<?> createRequirementUsage(@RequestBody RequirementDTO requirementDto) {
        try {
            Map<String, Object> elementData = elementMapper.toElementData(requirementDto);
            ElementDTO createdElement = requirementService.createRequirementUsage(elementData);
            RequirementDTO responseDto = elementMapper.toRequirementDTO(createdElement);
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (IllegalArgumentException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Bad Request");
            error.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * 【REQ-C2-2】查询所有需求使用
     * @return 200 OK 和需求使用列表
     */
    @GetMapping("/usages")
    public ResponseEntity<List<RequirementDTO>> getRequirementUsages() {
        List<ElementDTO> usages = requirementService.getRequirementUsages();
        List<RequirementDTO> responseList = usages.stream()
            .map(elementMapper::toRequirementDTO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(responseList);
    }
}