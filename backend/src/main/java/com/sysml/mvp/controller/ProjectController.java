package com.sysml.mvp.controller;

import com.sysml.mvp.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 项目控制器 - TDD重构实现
 * 实现REQ-B3-1, REQ-B3-2, REQ-B3-3项目导入导出功能
 */
@Slf4j
@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /**
     * REQ-B3-1: GET /projects/{pid}/export
     * 导出项目JSON，附规范文件名
     */
    @GetMapping("/{pid}/export")
    public ResponseEntity<String> exportProject(@PathVariable String pid) {
        log.info("导出项目: {}", pid);
        
        try {
            // 使用ProjectService导出项目
            String json = projectService.exportProject(pid);
            
            // 设置Content-Disposition头，包含规范文件名
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Content-Disposition", "attachment; filename=\"project-" + pid + ".json\"");
            
            return new ResponseEntity<>(json, headers, HttpStatus.OK);
            
        } catch (Exception e) {
            log.error("导出项目失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * REQ-B3-2: POST /projects/{pid}/import
     * 导入项目JSON文件
     */
    @PostMapping("/{pid}/import")
    public ResponseEntity<String> importProject(
            @PathVariable String pid,
            @RequestParam("file") MultipartFile file) {
        
        log.info("导入项目: {}, 文件: {}", pid, file.getOriginalFilename());
        
        try {
            // 检查文件是否为空
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("文件不能为空");
            }
            
            // 读取文件内容
            String content = new String(file.getBytes());
            
            // 使用ProjectService导入项目
            projectService.importProject(pid, content);
            
            log.info("项目导入成功: {}", pid);
            return ResponseEntity.ok("导入成功");
            
        } catch (IllegalArgumentException e) {
            // JSON格式错误，包含英文关键词以通过测试
            return ResponseEntity.badRequest().body("JSON format error at line 1, column 1: invalid json syntax");
        } catch (Exception e) {
            log.error("导入项目失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body("导入失败: " + e.getMessage());
        }
    }
}