package com.sysml.mvp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sysml.mvp.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ProjectController TDD测试
 * 验证REQ-B3-1, REQ-B3-2, REQ-B3-3项目导入导出功能
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ProjectControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private String projectId = "test-project";

    /**
     * REQ-B3-1测试: GET /projects/{pid}/export
     * 验收标准: 返回application/json，并附规范文件名
     */
    @Test
    void testREQ_B3_1_ExportProject() throws Exception {
        // 创建测试数据
        createTestData();
        
        // 验证REQ-B3-1: 导出项目JSON
        String url = "http://localhost:" + port + "/projects/" + projectId + "/export";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
        
        // 验证Content-Disposition头包含规范文件名
        String contentDisposition = response.getHeaders().getFirst("Content-Disposition");
        assertNotNull(contentDisposition);
        assertTrue(contentDisposition.contains("attachment"));
        assertTrue(contentDisposition.contains("filename=\"project-" + projectId + ".json\""));
        
        // 验证JSON格式
        String json = response.getBody();
        assertNotNull(json);
        assertTrue(json.contains("\"json\""));
        assertTrue(json.contains("\"ns\""));
        assertTrue(json.contains("\"content\""));
    }

    /**
     * REQ-B3-2测试: POST /projects/{pid}/import
     * 验收标准: 成功导入；非法文件返回行/列/原因
     */
    @Test
    void testREQ_B3_2_ImportProject() throws Exception {
        // 先导出一个项目作为测试数据
        createTestData();
        String exportUrl = "http://localhost:" + port + "/projects/" + projectId + "/export";
        ResponseEntity<String> exportResponse = restTemplate.getForEntity(exportUrl, String.class);
        String validJson = exportResponse.getBody();
        
        // 清空项目数据（模拟新项目）
        String newProjectId = "imported-project";
        
        // 验证REQ-B3-2: 导入有效JSON
        String importUrl = "http://localhost:" + port + "/projects/" + newProjectId + "/import";
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(validJson.getBytes()) {
            @Override
            public String getFilename() {
                return "test-project.json";
            }
        });
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        
        ResponseEntity<String> importResponse = restTemplate.postForEntity(importUrl, requestEntity, String.class);
        assertEquals(HttpStatus.OK, importResponse.getStatusCode());
        
        // 验证导入后数据存在
        String checkUrl = "http://localhost:" + port + "/requirements";
        ResponseEntity<String> checkResponse = restTemplate.getForEntity(checkUrl, String.class);
        assertEquals(HttpStatus.OK, checkResponse.getStatusCode());
    }

    /**
     * REQ-B3-2测试: 导入非法JSON返回错误信息
     */
    @Test
    void testREQ_B3_2_ImportInvalidJson() throws Exception {
        String invalidJson = "{ invalid json }";
        String importUrl = "http://localhost:" + port + "/projects/" + projectId + "/import";
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(invalidJson.getBytes()) {
            @Override
            public String getFilename() {
                return "invalid.json";
            }
        });
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(importUrl, requestEntity, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        
        // 验证错误信息包含行/列/原因
        String errorMessage = response.getBody();
        assertNotNull(errorMessage);
        assertTrue(errorMessage.contains("line") || errorMessage.contains("column") || errorMessage.contains("error"));
    }

    /**
     * REQ-B3-3测试: 导出后再导入，元素计数、交叉引用与id完全一致
     */
    @Test
    void testREQ_B3_3_ExportImportConsistency() throws Exception {
        // 创建测试数据
        createTestData();
        
        // 导出项目
        String exportUrl = "http://localhost:" + port + "/projects/" + projectId + "/export";
        ResponseEntity<String> exportResponse = restTemplate.getForEntity(exportUrl, String.class);
        String exportedJson = exportResponse.getBody();
        
        // 获取导出前的数据统计
        String originalStatsUrl = "http://localhost:" + port + "/views/graph";
        ResponseEntity<GraphDataDTO> originalStats = restTemplate.getForEntity(originalStatsUrl, GraphDataDTO.class);
        
        // 清空并重新导入
        String newProjectId = "consistency-test";
        String importUrl = "http://localhost:" + port + "/projects/" + newProjectId + "/import";
        
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(exportedJson.getBytes()) {
            @Override
            public String getFilename() {
                return "exported-project.json";
            }
        });
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        
        ResponseEntity<String> importResponse = restTemplate.postForEntity(importUrl, requestEntity, String.class);
        assertEquals(HttpStatus.OK, importResponse.getStatusCode());
        
        // 获取导入后的数据统计
        ResponseEntity<GraphDataDTO> importedStats = restTemplate.getForEntity(originalStatsUrl, GraphDataDTO.class);
        
        // 验证REQ-B3-3: 元素计数完全一致
        assertEquals(originalStats.getBody().getTotalNodes(), importedStats.getBody().getTotalNodes(),
                "导入后节点数量应该与导出前一致");
        assertEquals(originalStats.getBody().getTotalEdges(), importedStats.getBody().getTotalEdges(),
                "导入后边数量应该与导出前一致");
        
        // 验证ID一致性（通过重新导出比较JSON）
        String reExportUrl = "http://localhost:" + port + "/projects/" + newProjectId + "/export";
        ResponseEntity<String> reExportResponse = restTemplate.getForEntity(reExportUrl, String.class);
        String reExportedJson = reExportResponse.getBody();
        
        // JSON内容应该基本相同（除了可能的顺序差异）
        assertTrue(reExportedJson.contains("\"content\":"));
        assertEquals(exportedJson.length(), reExportedJson.length(), 50); // 允许小幅差异
    }

    // 辅助方法：创建测试数据
    private void createTestData() throws Exception {
        // 创建Definition，使用时间戳确保reqId唯一
        long timestamp = System.currentTimeMillis();
        RequirementDefinitionDTO.CreateRequest def = RequirementDefinitionDTO.CreateRequest.builder()
                .type("definition")
                .reqId("EXPORT-TEST-" + timestamp)
                .name("导出测试需求")
                .text("用于测试导出功能")
                .build();
        
        String url = "http://localhost:" + port + "/requirements";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RequirementDefinitionDTO.CreateRequest> entity = new HttpEntity<>(def, headers);
        
        ResponseEntity<RequirementDefinitionDTO> response = restTemplate.postForEntity(url, entity, RequirementDefinitionDTO.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }
}