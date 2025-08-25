package com.sysml.mvp.controller;

import com.sysml.mvp.service.UniversalElementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 通用元素控制器 - 完全基于Map，避免DTO转换问题
 * 实现REQ-B5-1到REQ-B5-4
 * 
 * 根据mvp接口文档.md第7章实现
 */
@RestController
@RequestMapping("/api/v1/elements")
@CrossOrigin(origins = "*")
public class UniversalElementController {
    
    @Autowired
    private UniversalElementService universalElementService;
    
    /**
     * REQ-B5-1: 创建任意SysML元素
     * POST /api/v1/elements?projectId={projectId}
     * 
     * 请求体格式（平铺，不嵌套）：
     * {
     *   "eClass": "PartUsage",
     *   "declaredName": "Engine",
     *   "declaredShortName": "eng"
     * }
     */
    @PostMapping
    public ResponseEntity<?> createElement(
            @RequestParam(defaultValue = "default") String projectId,
            @RequestBody Map<String, Object> request) {
        try {
            Map<String, Object> created = universalElementService.createElement(projectId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to create element: " + e.getMessage()));
        }
    }
    
    /**
     * REQ-B5-2: 查询元素
     * GET /api/v1/elements?projectId={projectId}&type={type}
     * 
     * 直接返回数组（符合mvp接口文档.md）
     */
    @GetMapping
    public ResponseEntity<?> queryElements(
            @RequestParam(defaultValue = "default") String projectId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        try {
            List<Map<String, Object>> result = universalElementService.queryElements(projectId, type, 
                Map.of("page", page, "size", size));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to query elements: " + e.getMessage()));
        }
    }
    
    /**
     * 获取单个元素
     * GET /api/v1/elements/{id}?projectId={projectId}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getElement(
            @RequestParam(defaultValue = "default") String projectId,
            @PathVariable String id) {
        try {
            Map<String, Object> element = universalElementService.getElementById(projectId, id);
            if (element == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(element);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to get element: " + e.getMessage()));
        }
    }
    
    /**
     * REQ-B5-3: 部分更新元素（PATCH）
     * PATCH /api/v1/elements/{id}?projectId={projectId}
     * 
     * 只更新请求体中提供的字段
     */
    @PatchMapping("/{id}")
    public ResponseEntity<?> patchElement(
            @RequestParam(defaultValue = "default") String projectId,
            @PathVariable String id,
            @RequestBody Map<String, Object> updates) {
        
        try {
            Map<String, Object> updated = universalElementService.updateElement(projectId, id, updates);
            if (updated == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to update element: " + e.getMessage()));
        }
    }
    
    /**
     * 删除元素
     * DELETE /api/v1/elements/{id}?projectId={projectId}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteElement(
            @RequestParam(defaultValue = "default") String projectId,
            @PathVariable String id) {
        try {
            boolean deleted = universalElementService.deleteElement(projectId, id);
            if (!deleted) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete element: " + e.getMessage()));
        }
    }
}