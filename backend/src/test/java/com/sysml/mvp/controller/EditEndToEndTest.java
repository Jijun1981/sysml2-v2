package com.sysml.mvp.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 端到端编辑测试 - 验证完整的数据流
 * 从HTTP请求 -> 控制器 -> 服务层 -> EMF持久化 -> 文件系统 -> 读取验证
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "logging.level.com.sysml.mvp=DEBUG"
})
public class EditEndToEndTest {

    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testEditRequirementUsage_端到端数据流验证() throws Exception {
        // 1. 首先创建一个RequirementUsage用于测试
        String createUrl = "/api/v1/requirements/usages?projectId=default";
        Map<String, Object> createData = Map.of(
            "declaredName", "测试Usage-原始",
            "declaredShortName", "TEST-ORIG",
            "documentation", "原始描述文本",
            "requirementDefinition", "DEF-FUNC"  // 关联到功能需求模板
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> createRequest = new HttpEntity<>(createData, headers);
        
        ResponseEntity<String> createResponse = restTemplate.exchange(
            createUrl, HttpMethod.POST, createRequest, String.class);
        
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        
        // 解析创建响应获取elementId
        Map<String, Object> created = objectMapper.readValue(createResponse.getBody(), Map.class);
        String elementId = (String) created.get("elementId");
        assertNotNull(elementId, "创建的元素必须有elementId");
        
        // 2. 编辑这个RequirementUsage的所有字段
        String editUrl = "/api/v1/elements/" + elementId + "?projectId=default";
        Map<String, Object> editData = Map.of(
            "declaredName", "测试Usage-已修改",
            "declaredShortName", "TEST-MODIFIED",
            "documentation", "已修改的描述文本",
            "requirementDefinition", "DEF-PERF"  // 更改关联到性能需求模板
        );
        
        HttpEntity<Map<String, Object>> editRequest = new HttpEntity<>(editData, headers);
        
        ResponseEntity<String> editResponse = restTemplate.exchange(
            editUrl, HttpMethod.PATCH, editRequest, String.class);
        
        assertEquals(HttpStatus.OK, editResponse.getStatusCode());
        
        // 解析编辑响应
        Map<String, Object> edited = objectMapper.readValue(editResponse.getBody(), Map.class);
        assertEquals(elementId, edited.get("elementId"), "elementId不应该改变");
        
        // 3. 重新查询验证所有字段都被正确更新
        String queryUrl = "/api/v1/elements/" + elementId + "?projectId=default";
        ResponseEntity<String> queryResponse = restTemplate.getForEntity(queryUrl, String.class);
        
        assertEquals(HttpStatus.OK, queryResponse.getStatusCode());
        
        Map<String, Object> queried = objectMapper.readValue(queryResponse.getBody(), Map.class);
        Map<String, Object> properties = (Map<String, Object>) queried.get("properties");
        
        // 验证所有字段都被正确持久化
        assertEquals("测试Usage-已修改", properties.get("declaredName"), "declaredName字段未正确更新");
        assertEquals("TEST-MODIFIED", properties.get("declaredShortName"), "declaredShortName字段未正确更新");
        
        // 验证文档字段（可能是List格式）
        Object documentation = properties.get("documentation");
        if (documentation instanceof String) {
            assertEquals("已修改的描述文本", documentation, "documentation字段未正确更新");
        } else {
            // 如果是List格式，检查第一个元素
            assertTrue(documentation.toString().contains("已修改的描述文本"), 
                "documentation字段未正确更新: " + documentation);
        }
        
        // 验证引用字段
        assertEquals("DEF-PERF", properties.get("requirementDefinition"), 
            "requirementDefinition字段未正确更新");
        
        System.out.println("✅ 端到端编辑测试通过！所有字段都正确更新并持久化");
        System.out.println("📝 最终数据: " + objectMapper.writeValueAsString(properties));
    }

    @Test
    public void testEditRequirementDefinition_端到端数据流验证() throws Exception {
        // 1. 首先创建一个RequirementDefinition用于测试
        String createUrl = "/api/v1/requirements?projectId=default";
        Map<String, Object> createData = Map.of(
            "reqId", "TEST-DEF-EDIT",
            "declaredName", "测试Definition-原始",
            "declaredShortName", "TEST-DEF-ORIG",
            "documentation", "原始定义描述"
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> createRequest = new HttpEntity<>(createData, headers);
        
        ResponseEntity<String> createResponse = restTemplate.exchange(
            createUrl, HttpMethod.POST, createRequest, String.class);
        
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        
        // 解析创建响应获取elementId
        Map<String, Object> created = objectMapper.readValue(createResponse.getBody(), Map.class);
        String elementId = (String) created.get("elementId");
        assertNotNull(elementId, "创建的元素必须有elementId");
        
        // 2. 编辑这个RequirementDefinition的所有字段
        String editUrl = "/api/v1/elements/" + elementId + "?projectId=default";
        Map<String, Object> editData = Map.of(
            "reqId", "TEST-DEF-EDIT", // reqId通常不应该改变
            "declaredName", "测试Definition-已修改",
            "declaredShortName", "TEST-DEF-MODIFIED",
            "documentation", "已修改的定义描述"
        );
        
        HttpEntity<Map<String, Object>> editRequest = new HttpEntity<>(editData, headers);
        
        ResponseEntity<String> editResponse = restTemplate.exchange(
            editUrl, HttpMethod.PATCH, editRequest, String.class);
        
        assertEquals(HttpStatus.OK, editResponse.getStatusCode());
        
        // 3. 重新查询验证所有字段都被正确更新
        String queryUrl = "/api/v1/elements/" + elementId + "?projectId=default";
        ResponseEntity<String> queryResponse = restTemplate.getForEntity(queryUrl, String.class);
        
        assertEquals(HttpStatus.OK, queryResponse.getStatusCode());
        
        Map<String, Object> queried = objectMapper.readValue(queryResponse.getBody(), Map.class);
        Map<String, Object> properties = (Map<String, Object>) queried.get("properties");
        
        // 验证所有字段都被正确持久化
        assertEquals("TEST-DEF-EDIT", properties.get("reqId"), "reqId字段未正确更新");
        assertEquals("测试Definition-已修改", properties.get("declaredName"), "declaredName字段未正确更新");
        assertEquals("TEST-DEF-MODIFIED", properties.get("declaredShortName"), "declaredShortName字段未正确更新");
        
        // 验证文档字段（可能是List格式）
        Object documentation = properties.get("documentation");
        if (documentation instanceof String) {
            assertEquals("已修改的定义描述", documentation, "documentation字段未正确更新");
        } else {
            assertTrue(documentation.toString().contains("已修改的定义描述"), 
                "documentation字段未正确更新: " + documentation);
        }
        
        System.out.println("✅ RequirementDefinition端到端编辑测试通过！所有字段都正确更新并持久化");
        System.out.println("📝 最终数据: " + objectMapper.writeValueAsString(properties));
    }
}