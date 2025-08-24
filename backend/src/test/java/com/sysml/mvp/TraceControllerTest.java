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

/**
 * Trace REST API 测试
 * 验证REQ-C3-1到REQ-C3-4的完整REST API功能
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class TraceControllerTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    private ObjectMapper objectMapper = new ObjectMapper();
    private String baseUrl;
    private HttpHeaders headers;
    
    // 测试数据
    private String fromRequirementId;
    private String toRequirementId;
    
    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
        headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // 创建测试用的需求
        createTestRequirements();
    }
    
    private void createTestRequirements() {
        // 创建源需求
        String fromRequest = String.format("""
            {
                "type": "definition",
                "reqId": "REQ-TRACE-FROM-%d", 
                "name": "源需求",
                "text": "用于测试Trace的源需求"
            }
            """, System.currentTimeMillis());
        
        ResponseEntity<String> fromResponse = restTemplate.postForEntity(
            baseUrl + "/requirements", 
            new HttpEntity<>(fromRequest, headers), 
            String.class
        );
        
        assertEquals(HttpStatus.CREATED, fromResponse.getStatusCode());
        try {
            JsonNode fromNode = objectMapper.readTree(fromResponse.getBody());
            fromRequirementId = fromNode.get("id").asText();
        } catch (Exception e) {
            fail("解析源需求响应失败: " + e.getMessage());
        }
        
        // 创建目标需求
        String toRequest = String.format("""
            {
                "type": "definition",
                "reqId": "REQ-TRACE-TO-%d", 
                "name": "目标需求",
                "text": "用于测试Trace的目标需求"
            }
            """, System.currentTimeMillis() + 1);
        
        ResponseEntity<String> toResponse = restTemplate.postForEntity(
            baseUrl + "/requirements", 
            new HttpEntity<>(toRequest, headers), 
            String.class
        );
        
        assertEquals(HttpStatus.CREATED, toResponse.getStatusCode());
        try {
            JsonNode toNode = objectMapper.readTree(toResponse.getBody());
            toRequirementId = toNode.get("id").asText();
        } catch (Exception e) {
            fail("解析目标需求响应失败: " + e.getMessage());
        }
    }
    
    @Test
    @DisplayName("REQ-C3-1: 创建Trace - 成功案例")
    void testCreateTrace_Success() throws Exception {
        // Given: 有效的Trace创建请求
        String[] validTypes = {"derive", "satisfy", "refine", "trace"};
        
        for (String type : validTypes) {
            String request = String.format("""
                {
                    "toId": "%s",
                    "type": "%s"
                }
                """, toRequirementId, type);
            
            // When: 创建Trace
            ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/requirements/" + fromRequirementId + "/traces",
                new HttpEntity<>(request, headers),
                String.class
            );
            
            // Then: 应返回201 Created
            assertEquals(HttpStatus.CREATED, response.getStatusCode(), 
                "创建" + type + "类型的Trace应返回201");
            
            assertTrue(response.getHeaders().containsKey("Location"), 
                "响应应包含Location header");
            
            // 验证响应体
            JsonNode responseNode = objectMapper.readTree(response.getBody());
            assertEquals(fromRequirementId, responseNode.get("fromId").asText());
            assertEquals(toRequirementId, responseNode.get("toId").asText());
            assertEquals(type, responseNode.get("type").asText());
            assertTrue(responseNode.get("id").asText().startsWith("T-"), 
                "Trace ID应以T-开头");
            assertNotNull(responseNode.get("createdAt"), 
                "响应应包含createdAt字段");
        }
    }
    
    @Test
    @DisplayName("REQ-C3-1: 创建Trace - fromId==toId应返回400")
    void testCreateTrace_SameFromAndTo() {
        // Given: fromId和toId相同的请求
        String request = String.format("""
            {
                "toId": "%s",
                "type": "derive"
            }
            """, fromRequirementId);  // 使用相同的ID
        
        // When: 创建Trace
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/requirements/" + fromRequirementId + "/traces",
            new HttpEntity<>(request, headers),
            String.class
        );
        
        // Then: 应返回400 Bad Request
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
    
    @Test
    @DisplayName("REQ-C3-1: 创建Trace - toId不存在应返回404")
    void testCreateTrace_ToIdNotFound() {
        // Given: 不存在的toId
        String request = """
            {
                "toId": "INVALID-ID",
                "type": "derive"
            }
            """;
        
        // When: 创建Trace
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/requirements/" + fromRequirementId + "/traces",
            new HttpEntity<>(request, headers),
            String.class
        );
        
        // Then: 应返回404 Not Found
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
    
    @Test
    @DisplayName("REQ-C3-1: 创建Trace - 无效type应返回400")
    void testCreateTrace_InvalidType() {
        // Given: 无效的type
        String request = String.format("""
            {
                "toId": "%s",
                "type": "invalid_type"
            }
            """, toRequirementId);
        
        // When: 创建Trace
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/requirements/" + fromRequirementId + "/traces",
            new HttpEntity<>(request, headers),
            String.class
        );
        
        // Then: 应返回400 Bad Request
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }
    
    @Test
    @DisplayName("REQ-C3-2: 查询Trace - 方向查询")
    void testQueryTraces_Directions() throws Exception {
        // Given: 创建一些Trace关系
        createTraceForTest("derive");
        
        // Test: 查询出边 (out)
        ResponseEntity<String> outResponse = restTemplate.getForEntity(
            baseUrl + "/requirements/" + fromRequirementId + "/traces?dir=out",
            String.class
        );
        assertEquals(HttpStatus.OK, outResponse.getStatusCode());
        JsonNode outTraces = objectMapper.readTree(outResponse.getBody());
        assertTrue(outTraces.isArray());
        assertTrue(outTraces.size() > 0, "应找到出边");
        
        // Test: 查询入边 (in)
        ResponseEntity<String> inResponse = restTemplate.getForEntity(
            baseUrl + "/requirements/" + toRequirementId + "/traces?dir=in",
            String.class
        );
        assertEquals(HttpStatus.OK, inResponse.getStatusCode());
        JsonNode inTraces = objectMapper.readTree(inResponse.getBody());
        assertTrue(inTraces.isArray());
        assertTrue(inTraces.size() > 0, "应找到入边");
        
        // Test: 查询双向 (both)
        ResponseEntity<String> bothResponse = restTemplate.getForEntity(
            baseUrl + "/requirements/" + fromRequirementId + "/traces?dir=both",
            String.class
        );
        assertEquals(HttpStatus.OK, bothResponse.getStatusCode());
        JsonNode bothTraces = objectMapper.readTree(bothResponse.getBody());
        assertTrue(bothTraces.isArray());
        assertTrue(bothTraces.size() > 0, "应找到双向关系");
    }
    
    @Test
    @DisplayName("REQ-C3-3: Trace去重 - 重复创建应返回既有对象")
    void testTraceDuplication() throws Exception {
        // Given: 创建第一个Trace
        String request = String.format("""
            {
                "toId": "%s",
                "type": "derive"
            }
            """, toRequirementId);
        
        ResponseEntity<String> firstResponse = restTemplate.postForEntity(
            baseUrl + "/requirements/" + fromRequirementId + "/traces",
            new HttpEntity<>(request, headers),
            String.class
        );
        assertEquals(HttpStatus.CREATED, firstResponse.getStatusCode());
        
        JsonNode firstTrace = objectMapper.readTree(firstResponse.getBody());
        String firstTraceId = firstTrace.get("id").asText();
        
        // When: 重复创建相同的Trace
        ResponseEntity<String> duplicateResponse = restTemplate.postForEntity(
            baseUrl + "/requirements/" + fromRequirementId + "/traces",
            new HttpEntity<>(request, headers),
            String.class
        );
        
        // Then: 应返回200 OK（不是201），并返回既有对象
        assertEquals(HttpStatus.OK, duplicateResponse.getStatusCode());
        
        JsonNode duplicateTrace = objectMapper.readTree(duplicateResponse.getBody());
        String duplicateTraceId = duplicateTrace.get("id").asText();
        
        assertEquals(firstTraceId, duplicateTraceId, 
            "重复请求应返回既有的Trace对象");
    }
    
    @Test
    @DisplayName("REQ-C3-4: 删除Trace - 成功案例")
    void testDeleteTrace_Success() throws Exception {
        // Given: 创建一个Trace
        String traceId = createTraceForTest("derive");
        
        // When: 删除Trace
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/traces/" + traceId,
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            String.class
        );
        
        // Then: 应返回204 No Content
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        
        // 验证Trace已被删除
        ResponseEntity<String> getResponse = restTemplate.getForEntity(
            baseUrl + "/traces/" + traceId,
            String.class
        );
        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
    }
    
    @Test
    @DisplayName("REQ-C3-4: 删除Trace - 不存在应返回404")
    void testDeleteTrace_NotFound() {
        // When: 删除不存在的Trace
        ResponseEntity<String> response = restTemplate.exchange(
            baseUrl + "/traces/INVALID-TRACE-ID",
            HttpMethod.DELETE,
            new HttpEntity<>(headers),
            String.class
        );
        
        // Then: 应返回404 Not Found
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }
    
    // 辅助方法：创建测试用Trace
    private String createTraceForTest(String type) throws Exception {
        String request = String.format("""
            {
                "toId": "%s",
                "type": "%s"
            }
            """, toRequirementId, type);
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/requirements/" + fromRequirementId + "/traces",
            new HttpEntity<>(request, headers),
            String.class
        );
        
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        
        JsonNode traceNode = objectMapper.readTree(response.getBody());
        return traceNode.get("id").asText();
    }
}