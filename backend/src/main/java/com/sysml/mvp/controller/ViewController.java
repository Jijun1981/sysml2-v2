package com.sysml.mvp.controller;

import com.sysml.mvp.dto.*;
import com.sysml.mvp.service.RequirementService;
import com.sysml.mvp.service.TraceService;
import com.sysml.mvp.service.ViewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 视图控制器 - TDD最小实现
 * 实现REQ-D1-1, REQ-D2-1, REQ-D3-1的基本功能
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ViewController {
    
    private final RequirementService requirementService;
    private final TraceService traceService;
    private final ViewService viewService;
    
    /**
     * REQ-D1-1: GET /tree
     * Definition为父节点，Usage为子节点
     */
    @GetMapping("/tree")
    public ResponseEntity<Map<String, Object>> getTreeView() {
        log.info("获取树视图数据");
        Map<String, Object> treeData = viewService.getTreeData();
        return ResponseEntity.ok(treeData);
    }
    
    @GetMapping("/api/v1/projects/{projectId}/tree")
    public ResponseEntity<Map<String, Object>> getProjectTreeView(
            @PathVariable String projectId) {
        log.info("获取项目树视图数据: projectId={}", projectId);
        Map<String, Object> treeData = viewService.getTreeData();
        treeData.put("projectId", projectId);
        return ResponseEntity.ok(treeData);
    }
    
    /**
     * REQ-D2-1: GET /table  
     * 支持分页、排序、搜索
     */
    @GetMapping("/table")
    public ResponseEntity<Map<String, Object>> getTableView(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String q) {
        log.info("获取表格视图数据: page={}, size={}, sort={}, q={}", page, size, sort, q);
        Map<String, Object> tableData = viewService.getTableData(page, size, sort, q);
        return ResponseEntity.ok(tableData);
    }
    
    @GetMapping("/api/v1/projects/{projectId}/table")
    public ResponseEntity<Map<String, Object>> getProjectTableView(
            @PathVariable String projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String q) {
        log.info("获取项目表视图数据: projectId={}", projectId);
        Map<String, Object> tableData = viewService.getTableData(page, size, sort, q);
        tableData.put("projectId", projectId);
        return ResponseEntity.ok(tableData);
    }
    
    /**
     * REQ-D3-1: GET /graph
     * 返回nodes/edges，支持rootId过滤  
     */
    @GetMapping("/graph")
    public ResponseEntity<Map<String, Object>> getGraphView(
            @RequestParam(required = false) String rootId) {
        log.info("获取图视图数据: rootId={}", rootId);
        Map<String, Object> graphData = viewService.getGraphData(rootId);
        return ResponseEntity.ok(graphData);
    }
    
    @GetMapping("/api/v1/projects/{projectId}/graph")
    public ResponseEntity<Map<String, Object>> getProjectGraphView(
            @PathVariable String projectId,
            @RequestParam(required = false) String rootId) {
        log.info("获取项目图视图数据: projectId={}, rootId={}", projectId, rootId);
        Map<String, Object> graphData = viewService.getGraphData(rootId);
        graphData.put("projectId", projectId);
        return ResponseEntity.ok(graphData);
    }
}