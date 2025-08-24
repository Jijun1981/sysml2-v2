package com.sysml.mvp.controller;

import com.sysml.mvp.dto.RequirementDefinitionDTO;
import com.sysml.mvp.dto.TraceDTO;
import com.sysml.mvp.service.RequirementService;
import com.sysml.mvp.service.TraceService;
import com.sysml.mvp.service.ViewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 视图控制器测试 - TDD实现
 * REQ-D1-1: 树视图接口
 * REQ-D2-1: 表视图接口  
 * REQ-D3-1: 图视图接口
 */
@WebMvcTest(ViewController.class)
public class ViewControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ViewService viewService;
    
    @MockBean
    private RequirementService requirementService;
    
    @MockBean
    private TraceService traceService;
    
    private RequirementDefinitionDTO sampleDefinition;
    private RequirementDefinitionDTO sampleUsage;
    private TraceDTO sampleTrace;
    
    @BeforeEach
    void setUp() {
        // 准备测试数据
        sampleDefinition = RequirementDefinitionDTO.builder()
            .id("R-001")
            .eClass("RequirementDefinition")
            .reqId("REQ-001")
            .name("功能需求")
            .text("系统应该提供用户登录功能")
            .tags(Arrays.asList("critical"))
            .createdAt(Instant.now())
            .build();
            
        sampleUsage = RequirementDefinitionDTO.builder()
            .id("U-001")
            .eClass("RequirementUsage")
            .of("R-001")
            .name("Web登录实现")
            .text("基于REST API的登录实现")
            .status("draft")
            .build();
            
        sampleTrace = TraceDTO.builder()
            .id("T-001")
            .fromId("R-001")
            .toId("R-002")
            .type("derive")
            .createdAt(Instant.now())
            .build();
    }
    
    /**
     * REQ-D1-1: 树视图接口
     * AC: GET /tree 返回Definition为父、Usage为子；支持懒加载
     */
    @Test
    void testGetTreeView() throws Exception {
        // 准备树形结构数据
        Map<String, Object> treeData = new HashMap<>();
        treeData.put("id", "root");
        treeData.put("label", "Requirements");
        
        Map<String, Object> defNode = new HashMap<>();
        defNode.put("id", "R-001");
        defNode.put("label", "REQ-001: 功能需求");
        defNode.put("type", "definition");
        
        Map<String, Object> usageNode = new HashMap<>();
        usageNode.put("id", "U-001");
        usageNode.put("label", "Web登录实现");
        usageNode.put("type", "usage");
        usageNode.put("children", Arrays.asList());
        
        defNode.put("children", Arrays.asList(usageNode));
        treeData.put("children", Arrays.asList(defNode));
        
        when(viewService.getTreeData()).thenReturn(treeData);
        
        mockMvc.perform(get("/tree"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("root"))
            .andExpect(jsonPath("$.label").value("Requirements"))
            .andExpect(jsonPath("$.children").isArray())
            .andExpect(jsonPath("$.children[0].id").value("R-001"))
            .andExpect(jsonPath("$.children[0].type").value("definition"))
            .andExpect(jsonPath("$.children[0].children[0].id").value("U-001"))
            .andExpect(jsonPath("$.children[0].children[0].type").value("usage"));
        
        verify(viewService).getTreeData();
    }
    
    /**
     * REQ-D2-1: 表视图接口
     * AC: GET /table?page&size&sort&q，列包含reqId,name,type,tags,status
     */
    @Test
    void testGetTableView() throws Exception {
        // 准备表格数据
        Map<String, Object> tableData = new HashMap<>();
        tableData.put("columns", Arrays.asList("reqId", "name", "type", "tags", "status"));
        
        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", "R-001");
        row1.put("reqId", "REQ-001");
        row1.put("name", "功能需求");
        row1.put("type", "definition");
        row1.put("tags", Arrays.asList("critical"));
        row1.put("status", "approved");
        
        Map<String, Object> row2 = new HashMap<>();
        row2.put("id", "U-001");
        row2.put("reqId", null);
        row2.put("name", "Web登录实现");
        row2.put("type", "usage");
        row2.put("tags", Arrays.asList());
        row2.put("status", "draft");
        
        tableData.put("rows", Arrays.asList(row1, row2));
        tableData.put("page", 0);
        tableData.put("totalPages", 1);
        tableData.put("totalElements", 2);
        
        when(viewService.getTableData(0, 50, null, null)).thenReturn(tableData);
        
        mockMvc.perform(get("/table")
                .param("page", "0")
                .param("size", "50"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.columns", hasSize(5)))
            .andExpect(jsonPath("$.columns[0]").value("reqId"))
            .andExpect(jsonPath("$.rows", hasSize(2)))
            .andExpect(jsonPath("$.rows[0].reqId").value("REQ-001"))
            .andExpect(jsonPath("$.rows[0].type").value("definition"))
            .andExpect(jsonPath("$.rows[1].type").value("usage"))
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.totalElements").value(2));
        
        verify(viewService).getTableData(0, 50, null, null);
    }
    
    /**
     * REQ-D2-1: 表视图支持搜索和排序
     */
    @Test
    void testGetTableViewWithSearchAndSort() throws Exception {
        Map<String, Object> tableData = new HashMap<>();
        tableData.put("columns", Arrays.asList("reqId", "name", "type", "tags", "status"));
        tableData.put("rows", Arrays.asList());
        tableData.put("page", 0);
        tableData.put("totalPages", 0);
        
        when(viewService.getTableData(0, 20, "reqId,asc", "login")).thenReturn(tableData);
        
        mockMvc.perform(get("/table")
                .param("page", "0")
                .param("size", "20")
                .param("sort", "reqId,asc")
                .param("q", "login"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.columns", hasSize(5)));
        
        verify(viewService).getTableData(0, 20, "reqId,asc", "login");
    }
    
    /**
     * REQ-D3-1: 图视图接口
     * AC: GET /graph?rootId 返回nodes/edges；节点含id,type,label，边含from,to,type
     */
    @Test
    void testGetGraphView() throws Exception {
        // 准备图数据
        Map<String, Object> graphData = new HashMap<>();
        
        Map<String, Object> node1 = new HashMap<>();
        node1.put("id", "R-001");
        node1.put("type", "requirement");
        node1.put("label", "REQ-001");
        node1.put("position", Map.of("x", 100, "y", 100));
        node1.put("data", Map.of("eClass", "RequirementDefinition", "name", "功能需求"));
        
        Map<String, Object> node2 = new HashMap<>();
        node2.put("id", "R-002");
        node2.put("type", "requirement");
        node2.put("label", "REQ-002");
        node2.put("position", Map.of("x", 300, "y", 100));
        node2.put("data", Map.of("eClass", "RequirementDefinition", "name", "性能需求"));
        
        Map<String, Object> edge1 = new HashMap<>();
        edge1.put("id", "T-001");
        edge1.put("source", "R-001");
        edge1.put("target", "R-002");
        edge1.put("type", "derive");
        edge1.put("label", "derive");
        
        graphData.put("nodes", Arrays.asList(node1, node2));
        graphData.put("edges", Arrays.asList(edge1));
        
        when(viewService.getGraphData(null)).thenReturn(graphData);
        
        mockMvc.perform(get("/graph"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nodes", hasSize(2)))
            .andExpect(jsonPath("$.nodes[0].id").value("R-001"))
            .andExpect(jsonPath("$.nodes[0].type").value("requirement"))
            .andExpect(jsonPath("$.nodes[0].label").value("REQ-001"))
            .andExpect(jsonPath("$.nodes[0].position.x").value(100))
            .andExpect(jsonPath("$.edges", hasSize(1)))
            .andExpect(jsonPath("$.edges[0].source").value("R-001"))
            .andExpect(jsonPath("$.edges[0].target").value("R-002"))
            .andExpect(jsonPath("$.edges[0].type").value("derive"));
        
        verify(viewService).getGraphData(null);
    }
    
    /**
     * REQ-D3-1: 图视图支持rootId参数
     * AC: 当rootId为空时返回分页/窗口化全局子图
     */
    @Test
    void testGetGraphViewWithRootId() throws Exception {
        Map<String, Object> graphData = new HashMap<>();
        Map<String, Object> node = new HashMap<>();
        node.put("id", "R-001");
        node.put("type", "requirement");
        node.put("label", "REQ-001");
        
        graphData.put("nodes", Arrays.asList(node));
        graphData.put("edges", Arrays.asList());
        
        when(viewService.getGraphData("R-001")).thenReturn(graphData);
        
        mockMvc.perform(get("/graph")
                .param("rootId", "R-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nodes", hasSize(1)))
            .andExpect(jsonPath("$.nodes[0].id").value("R-001"));
        
        verify(viewService).getGraphData("R-001");
    }
    
    /**
     * 测试API路径格式 - 支持项目ID路径
     */
    @Test
    void testProjectSpecificPaths() throws Exception {
        Map<String, Object> treeData = new HashMap<>();
        treeData.put("id", "root");
        treeData.put("label", "Requirements");
        treeData.put("children", Arrays.asList());
        
        when(viewService.getTreeData()).thenReturn(treeData);
        
        // 测试带项目ID的路径也能工作
        mockMvc.perform(get("/api/v1/projects/default/tree"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("root"));
        
        mockMvc.perform(get("/api/v1/projects/default/table"))
            .andExpect(status().isOk());
        
        mockMvc.perform(get("/api/v1/projects/default/graph"))
            .andExpect(status().isOk());
    }
}