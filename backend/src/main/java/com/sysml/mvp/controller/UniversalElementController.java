package com.sysml.mvp.controller;

import com.sysml.mvp.dto.ElementDTO;
import com.sysml.mvp.service.UniversalElementService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 通用元素控制器
 * 实现REQ-B5-1, REQ-B5-2, REQ-B5-3
 * 
 * 通过动态EMF模式，一个控制器处理所有182个SysML类型
 */
@RestController
@RequestMapping("/api/v1/elements")
@CrossOrigin(origins = "*")
public class UniversalElementController {
    
    @Autowired
    private UniversalElementService universalElementService;
    
    /**
     * REQ-B5-1: 通用创建接口
     * POST /api/v1/elements
     * 
     * 请求体格式：
     * {
     *   "eClass": "PartUsage",
     *   "attributes": {
     *     "declaredName": "Engine",
     *     "declaredShortName": "eng"
     *   }
     * }
     */
    @PostMapping
    public ResponseEntity<?> createElement(@RequestBody Map<String, Object> request) {
        try {
            String eClass = (String) request.get("eClass");
            if (eClass == null || eClass.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "eClass is required"));
            }
            
            @SuppressWarnings("unchecked")
            Map<String, Object> attributes = (Map<String, Object>) request.get("attributes");
            if (attributes == null) {
                attributes = Map.of();
            }
            
            ElementDTO created = universalElementService.createElement(eClass, attributes);
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
     * REQ-B5-2: 按类型查询
     * GET /api/v1/elements?type=PartUsage&page=0&size=10
     * 
     * @param type 元素类型，空则返回所有
     * @param page 页码，从0开始
     * @param size 每页大小
     */
    @GetMapping
    public ResponseEntity<?> queryElements(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        try {
            // 如果提供了分页参数，返回Page对象
            if (page > 0 || size != 50) {
                Pageable pageable = PageRequest.of(page, size);
                Page<ElementDTO> result = universalElementService.queryElements(type, pageable);
                return ResponseEntity.ok(result);
            } else {
                // 默认返回List（向后兼容）
                List<ElementDTO> result = universalElementService.queryElements(type);
                return ResponseEntity.ok(result);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to query elements: " + e.getMessage()));
        }
    }
    
    /**
     * REQ-B5-3: 通用PATCH更新
     * PATCH /api/v1/elements/{id}
     * 
     * 只更新请求体中提供的字段
     */
    @PatchMapping("/{id}")
    public ResponseEntity<?> patchElement(
            @PathVariable String id,
            @RequestBody Map<String, Object> updates) {
        
        try {
            ElementDTO updated = universalElementService.patchElement(id, updates);
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
     * 获取单个元素
     * GET /api/v1/elements/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getElement(@PathVariable String id) {
        try {
            ElementDTO element = universalElementService.getElementById(id);
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
     * 删除元素
     * DELETE /api/v1/elements/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteElement(@PathVariable String id) {
        try {
            boolean deleted = universalElementService.deleteElement(id);
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