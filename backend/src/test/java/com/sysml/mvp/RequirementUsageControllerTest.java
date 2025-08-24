package com.sysml.mvp;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * RequirementUsage REST API 测试
 * 验证REQ-C2-1到REQ-C2-2的完整REST API功能
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class RequirementUsageControllerTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    private ObjectMapper objectMapper = new ObjectMapper();
    private String baseUrl;
    private HttpHeaders headers;
    
    // 测试数据
    private String definitionId;
    
    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // 创建测试用的RequirementDefinition
        createTestDefinition();
    }
    
    private void createTestDefinition() {
        String definitionRequest = String.format("""
            {
                "type": "definition",
                "reqId": "REQ-USAGE-TEST-%d", 
                "name": "测试定义需求",
                "text": "用于测试RequirementUsage的定义需求"
            }
            """, System.currentTimeMillis());
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/requirements", 
            new HttpEntity<>(definitionRequest, headers), 
            String.class
        );
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        try {
            JsonNode node = objectMapper.readTree(response.getBody());
            definitionId = node.get("id").asText();
        } catch (Exception e) {
            fail("解析定义需求响应失败: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("REQ-C2-1: 创建RequirementUsage - 成功案例")
    void testCreateUsage_Success() throws Exception {
        // Given: 有效的Usage创建请求
        String request = String.format("""
            {
                "type": "usage",
                "of": "%s",
                "name": "登录功能实现",
                "text": "Web端登录功能的具体实现"
            }
            """, definitionId);
        
        // When: 创建Usage
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/requirements",
            new HttpEntity<>(request, headers),
            String.class
        );
        
        // Then: 应返回201 Created
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        
        assertTrue(response.getHeaders().containsKey("Location"), 
            "响应应包含Location header");
        
        // 验证响应体
        JsonNode responseNode = objectMapper.readTree(response.getBody());
        assertEquals(definitionId, responseNode.get("of").asText());
        assertEquals("登录功能实现", responseNode.get("name").asText());
        assertEquals("Web端登录功能的具体实现", responseNode.get("text").asText());
        assertTrue(responseNode.get("id").asText().startsWith("U-"), 
            "Usage ID应以U-开头");
        assertNotNull(responseNode.get("createdAt"), 
            "响应应包含createdAt字段");
    }
    
    @Test
    @DisplayName("REQ-C2-1: 创建RequirementUsage - 缺少of参数应返回400")
    void testCreateUsage_MissingOf() {
        // Given: 缺少of字段的请求
        String request = """
            {
                "type": "usage",
                "name": "缺少of的用法",
                "text": "这应该失败"
            }
            """;
        
        // When: 创建Usage
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/requirements",
            new HttpEntity<>(request, headers),
            String.class
        );
        
        // Then: 应返回400 Bad Request
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
    
    @Test
    @DisplayName("REQ-C2-1: 创建RequirementUsage - of引用不存在应返回404")
    void testCreateUsage_OfNotFound() {
        // Given: of字段引用不存在的ID
        String request = """
            {
                "type": "usage",
                "of": "INVALID-DEF-ID",
                "name": "引用不存在的定义",
                "text": "这应该失败"
            }
            """;
        
        // When: 创建Usage
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/requirements",
            new HttpEntity<>(request, headers),
            String.class
        );
        
        // Then: 应返回404 Not Found
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
    
    @Test
    @DisplayName("REQ-C2-2: RequirementUsage CRUD - 读取、更新、删除")
    void testUsageCRUD() throws Exception {
        // Given: 先创建一个Usage
        String usageId = createUsageForTest();
        
        // Test: 读取Usage
        ResponseEntity<String> getResponse = restTemplate.getForEntity(
            baseUrl + "/requirements/" + usageId,
            String.class
        );
        assertEquals(HttpStatus.OK, getResponse.getStatusCode());
        JsonNode usage = objectMapper.readTree(getResponse.getBody());
        assertEquals(definitionId, usage.get("of").asText());
        
        // Test: 更新Usage（允许name,text,status,tags）
        String updateRequest = """
            {
                "name": "更新后的用法名称",
                "text": "更新后的用法文本",
                "status": "approved",
                "tags": ["updated", "test"]
            }
            """;
        
        ResponseEntity<String> updateResponse = restTemplate.exchange(
            baseUrl + "/requirements/" + usageId,
            HttpMethod.PUT,
            new HttpEntity<>(updateRequest, headers),
            String.class
        );
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        
        JsonNode updated = objectMapper.readTree(updateResponse.getBody());
        assertEquals("更新后的用法名称", updated.get("name").asText());
        assertEquals("approved", updated.get("status").asText());
        assertTrue(updated.get("tags").isArray());
        assertEquals(2, updated.get("tags").size());
        
        // Test: 删除Usage（无Trace引用时应该成功）
        ResponseEntity<String> deleteResponse = restTemplate.exchange(
            baseUrl + "/requirements/" + usageId,
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            String.class
        );
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());
        
        // 验证已删除
        ResponseEntity<String> getAfterDelete = restTemplate.getForEntity(
            baseUrl + "/requirements/" + usageId,
            String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, getAfterDelete.getStatusCode());
    }
    
    @Test
    @DisplayName("REQ-C2-2: 删除被Trace引用的Usage应返回409")
    void testDeleteUsage_WithTraceReferences() throws Exception {
        // Given: 创建Usage和另一个需求
        String usageId = createUsageForTest();
        String anotherDefId = createAnotherDefinition();
        
        // 创建Trace引用这个Usage
        String traceRequest = String.format("""
            {
                "toId": "%s",
                "type": "satisfy"
            }
            """, usageId);
        
        ResponseEntity<String> traceResponse = restTemplate.postForEntity(
            baseUrl + "/requirements/" + anotherDefId + "/traces",
            new HttpEntity<>(traceRequest, headers),
            String.class
        );
        assertEquals(HttpStatus.CREATED, traceResponse.getStatusCode());
        
        // When: 删除被引用的Usage
        ResponseEntity<String> deleteResponse = restTemplate.exchange(
            baseUrl + "/requirements/" + usageId,
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            String.class
        );
        
        // Then: 应返回409 Conflict
        assertEquals(HttpStatus.CONFLICT, deleteResponse.getStatusCode());
        
        JsonNode errorResponse = objectMapper.readTree(deleteResponse.getBody());
        assertTrue(errorResponse.has("blockingTraceIds"));
        assertTrue(errorResponse.get("blockingTraceIds").isArray());
        assertTrue(errorResponse.get("blockingTraceIds").size() > 0);
    }
    
    // 辅助方法
    private String createUsageForTest() throws Exception {
        String request = String.format("""
            {
                "type": "usage",
                "of": "%s",
                "name": "测试用法",
                "text": "测试专用的用法实例"
            }
            """, definitionId);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/requirements",
            new HttpEntity<>(request, headers),
            String.class
        );
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        
        JsonNode node = objectMapper.readTree(response.getBody());
        return node.get("id").asText();
    }
    
    private String createAnotherDefinition() throws Exception {
        String request = String.format("""
            {
                "type": "definition",
                "reqId": "REQ-DEF-%d",
                "name": "另一个定义需求",
                "text": "用于测试引用关系"
            }
            """, System.currentTimeMillis());
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/requirements",
            new HttpEntity<>(request, headers),
            String.class
        );
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        
        JsonNode node = objectMapper.readTree(response.getBody());
        return node.get("id").asText();
    }
}