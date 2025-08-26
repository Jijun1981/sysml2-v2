package com.sysml.mvp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sysml.mvp.service.ProjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 项目控制器 - 负责项目导入导出功能
 * 
 * 需求实现：
 * - REQ-B3-1: 导出JSON项目文件 - GET /api/v1/projects/{pid}/export
 * - REQ-B3-2: 导入JSON项目文件 - POST /api/v1/projects/{pid}/import
 * - REQ-B3-3: 导入导出一致性保证 - ID稳定性和引用完整性
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {
    
    private final ProjectService projectService;
    private final ObjectMapper objectMapper;
    
    public ProjectController(ProjectService projectService, ObjectMapper objectMapper) {
        this.projectService = projectService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 【REQ-B3-1】导出项目
     * @param projectId 项目ID
     * @return JSON格式的项目数据，带标准文件名
     */
    @GetMapping("/{pid}/export")
    public ResponseEntity<?> exportProject(@PathVariable("pid") String projectId) {
        try {
            log.info("导出项目请求: {}", projectId);
            
            Map<String, Object> exportData = projectService.exportProject(projectId);
            
            // 设置响应头 - REQ-B3-1要求的文件名格式
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Content-Disposition", String.format("attachment; filename=\"project-%s.json\"", projectId));
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(exportData);
                    
        } catch (IllegalArgumentException e) {
            log.warn("导出项目失败: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Not Found");
            error.put("message", e.getMessage());
            return ResponseEntity.status(404).body(error);
        } catch (Exception e) {
            log.error("导出项目异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            error.put("message", "Failed to export project");
            return ResponseEntity.status(500).body(error);
        }
    }
    
    /**
     * 【REQ-B3-2】导入项目
     * @param projectId 项目ID
     * @param file 上传的JSON文件
     * @return 导入结果信息
     */
    @PostMapping("/{pid}/import")
    public ResponseEntity<?> importProject(
            @PathVariable("pid") String projectId,
            @RequestParam("file") MultipartFile file) {
        try {
            log.info("导入项目请求: {}, 文件: {}", projectId, file.getOriginalFilename());
            
            // 验证文件类型
            if (!file.getContentType().equals("application/json") && 
                !file.getOriginalFilename().endsWith(".json")) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Bad Request");
                error.put("message", "File must be JSON format");
                return ResponseEntity.status(400).body(error);
            }
            
            // 读取文件内容
            String jsonContent = new String(file.getBytes());
            
            // 委托给服务层处理
            Map<String, Object> importResult = projectService.importProject(projectId, jsonContent);
            
            return ResponseEntity.ok(importResult);
            
        } catch (IllegalArgumentException e) {
            log.warn("导入项目失败: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Bad Request");
            error.put("message", e.getMessage());
            return ResponseEntity.status(400).body(error);
        } catch (Exception e) {
            log.error("导入项目异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Internal Server Error");
            error.put("message", "Failed to import project");
            return ResponseEntity.status(500).body(error);
        }
    }
}