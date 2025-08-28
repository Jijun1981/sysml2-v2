package com.sysml.mvp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sysml.mvp.service.ProjectService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Arrays;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ProjectController测试 - REQ-B3-1,B3-2
 * 
 * 测试覆盖：
 * - REQ-B3-1: 导出JSON项目文件
 * - REQ-B3-2: 导入JSON项目文件
 * - REQ-B3-3: 导入导出一致性验证
 */
@WebMvcTest(ProjectController.class)
@DisplayName("ProjectController测试 - REQ-B3-1,B3-2")
public class ProjectControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ProjectService projectService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @DisplayName("【REQ-B3-1】导出项目 - 成功场景")
    void testExportProject_Success() throws Exception {
        // Given
        String projectId = "default";
        Map<String, Object> mockExportData = createMockExportData();
        
        when(projectService.exportProject(projectId)).thenReturn(mockExportData);
        
        // When & Then
        mockMvc.perform(get("/api/v1/projects/{pid}/export", projectId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"project-default.json\""))
                .andExpect(jsonPath("$.json.version").value("1.0"))
                .andExpect(jsonPath("$.ns.sysml").value("https://www.omg.org/spec/SysML/20250201"))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(3));
        
        verify(projectService).exportProject(projectId);
    }
    
    @Test
    @DisplayName("【REQ-B3-1】导出项目 - 项目不存在")
    void testExportProject_ProjectNotFound() throws Exception {
        // Given
        String projectId = "nonexistent";
        
        when(projectService.exportProject(projectId))
                .thenThrow(new IllegalArgumentException("Project not found: " + projectId));
        
        // When & Then
        mockMvc.perform(get("/api/v1/projects/{pid}/export", projectId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Project not found: " + projectId));
        
        verify(projectService).exportProject(projectId);
    }
    
    @Test
    @DisplayName("【REQ-B3-2】导入项目 - 成功场景")
    void testImportProject_Success() throws Exception {
        // Given
        String projectId = "default";
        String jsonContent = createMockImportJson();
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "project-test.json", 
            "application/json", 
            jsonContent.getBytes()
        );
        
        Map<String, Object> mockImportResult = new HashMap<>();
        mockImportResult.put("projectId", projectId);
        mockImportResult.put("elementsImported", 23);
        mockImportResult.put("status", "success");
        
        when(projectService.importProject(eq(projectId), anyString()))
                .thenReturn(mockImportResult);
        
        // When & Then
        mockMvc.perform(multipart("/api/v1/projects/{pid}/import", projectId)
                .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(projectId))
                .andExpect(jsonPath("$.elementsImported").value(23))
                .andExpect(jsonPath("$.status").value("success"));
        
        verify(projectService).importProject(eq(projectId), eq(jsonContent));
    }
    
    @Test
    @DisplayName("【REQ-B3-2】导入项目 - JSON格式错误")
    void testImportProject_InvalidJsonFormat() throws Exception {
        // Given
        String projectId = "default";
        String invalidJson = "{ invalid json content";
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "invalid.json", 
            "application/json", 
            invalidJson.getBytes()
        );
        
        when(projectService.importProject(eq(projectId), eq(invalidJson)))
                .thenThrow(new IllegalArgumentException("Invalid JSON format at line 1, column 3: Expected valid JSON"));
        
        // When & Then
        mockMvc.perform(multipart("/api/v1/projects/{pid}/import", projectId)
                .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid JSON format at line 1, column 3: Expected valid JSON"));
        
        verify(projectService).importProject(eq(projectId), eq(invalidJson));
    }
    
    @Test
    @DisplayName("【REQ-B3-2】导入项目 - 缺少必填字段")
    void testImportProject_MissingRequiredFields() throws Exception {
        // Given
        String projectId = "default";
        String jsonWithMissingFields = "{\"json\":{\"version\":\"1.0\"}}";
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "incomplete.json", 
            "application/json", 
            jsonWithMissingFields.getBytes()
        );
        
        when(projectService.importProject(eq(projectId), eq(jsonWithMissingFields)))
                .thenThrow(new IllegalArgumentException("Missing required field: ns"));
        
        // When & Then
        mockMvc.perform(multipart("/api/v1/projects/{pid}/import", projectId)
                .file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Missing required field: ns"));
        
        verify(projectService).importProject(eq(projectId), eq(jsonWithMissingFields));
    }
    
    @Test
    @DisplayName("【REQ-B3-3】导入导出一致性 - 往返测试")
    void testImportExportConsistency_RoundTrip() throws Exception {
        // Given
        String projectId = "test-consistency";
        Map<String, Object> originalData = createMockExportData();
        
        when(projectService.exportProject(projectId)).thenReturn(originalData);
        
        Map<String, Object> importResult = new HashMap<>();
        importResult.put("projectId", projectId);
        importResult.put("elementsImported", 3);
        importResult.put("status", "success");
        
        when(projectService.importProject(eq(projectId), anyString()))
                .thenReturn(importResult);
        
        // When - 先导出
        String exportedJson = mockMvc.perform(get("/api/v1/projects/{pid}/export", projectId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        
        // Then - 再导入
        MockMultipartFile file = new MockMultipartFile(
            "file", 
            "exported.json", 
            "application/json", 
            exportedJson.getBytes()
        );
        
        mockMvc.perform(multipart("/api/v1/projects/{pid}/import", projectId)
                .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.elementsImported").value(3));
        
        verify(projectService).exportProject(projectId);
        verify(projectService).importProject(eq(projectId), eq(exportedJson));
    }
    
    @Test
    @DisplayName("【REQ-B3-1】导出项目 - 文件名格式验证")
    void testExportProject_FileNameFormat() throws Exception {
        // Given
        String projectId = "my-special-project";
        Map<String, Object> mockExportData = createMockExportData();
        
        when(projectService.exportProject(projectId)).thenReturn(mockExportData);
        
        // When & Then
        mockMvc.perform(get("/api/v1/projects/{pid}/export", projectId))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"project-my-special-project.json\""));
        
        verify(projectService).exportProject(projectId);
    }
    
    // 辅助方法
    private Map<String, Object> createMockExportData() {
        Map<String, Object> exportData = new HashMap<>();
        
        // JSON版本信息
        Map<String, Object> jsonInfo = new HashMap<>();
        jsonInfo.put("version", "1.0");
        jsonInfo.put("encoding", "UTF-8");
        exportData.put("json", jsonInfo);
        
        // 命名空间
        Map<String, Object> namespaces = new HashMap<>();
        namespaces.put("sysml", "https://www.omg.org/spec/SysML/20250201");
        exportData.put("ns", namespaces);
        
        // 内容 
        List<Map<String, Object>> content = Arrays.asList(
            createMockRequirementDefinition("REQ-001", "测试需求1"),
            createMockRequirementUsage("REQ-001-U1", "REQ-001"),
            createMockDependency("REQ-001", "REQ-001-U1", "derive")
        );
        exportData.put("content", content);
        
        return exportData;
    }
    
    private Map<String, Object> createMockRequirementDefinition(String reqId, String name) {
        Map<String, Object> element = new HashMap<>();
        element.put("eClass", "sysml:RequirementDefinition");
        
        Map<String, Object> data = new HashMap<>();
        data.put("elementId", "R-" + reqId);
        data.put("reqId", reqId);
        data.put("declaredName", name);
        data.put("documentation", "这是一个测试需求定义");
        element.put("data", data);
        
        return element;
    }
    
    private Map<String, Object> createMockRequirementUsage(String usageId, String ofId) {
        Map<String, Object> element = new HashMap<>();
        element.put("eClass", "sysml:RequirementUsage");
        
        Map<String, Object> data = new HashMap<>();
        data.put("elementId", "U-" + usageId);
        data.put("declaredName", "Usage of " + ofId);
        data.put("requirementDefinition", ofId);
        element.put("data", data);
        
        return element;
    }
    
    private Map<String, Object> createMockDependency(String fromId, String toId, String type) {
        Map<String, Object> element = new HashMap<>();
        element.put("eClass", "sysml:Dependency");
        
        Map<String, Object> data = new HashMap<>();
        data.put("elementId", "T-" + fromId + "-" + toId);
        data.put("fromId", fromId);
        data.put("toId", toId);
        data.put("type", type);
        element.put("data", data);
        
        return element;
    }
    
    private String createMockImportJson() {
        Map<String, Object> importData = createMockExportData();
        try {
            return objectMapper.writeValueAsString(importData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create mock JSON", e);
        }
    }
}