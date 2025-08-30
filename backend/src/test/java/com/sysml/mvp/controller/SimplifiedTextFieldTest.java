package com.sysml.mvp.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
 * 测试简化文本字段方案
 * REQ-TEXT-SIMPLE-001: 使用declaredName存储描述文本
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "logging.level.com.sysml.mvp=DEBUG"
})
public class SimplifiedTextFieldTest {

    @Autowired
    private TestRestTemplate restTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCreateRequirementUsage_使用declaredName作为描述() throws Exception {
        // REQ-TEXT-SIMPLE-001-1: 测试字段映射
        String createUrl = "/api/v1/requirements/usages?projectId=default";
        Map<String, Object> createData = Map.of(
            "declaredShortName", "TEST-REQ-001",
            "declaredName", "这是一个测试需求的详细描述文本，用于验证declaredName字段的使用",
            "requirementDefinition", "DEF-FUNC"
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> createRequest = new HttpEntity<>(createData, headers);
        
        ResponseEntity<String> createResponse = restTemplate.exchange(
            createUrl, HttpMethod.POST, createRequest, String.class);
        
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        
        // 验证返回的数据
        Map<String, Object> created = objectMapper.readValue(createResponse.getBody(), Map.class);
        String elementId = (String) created.get("elementId");
        assertNotNull(elementId, "必须返回elementId");
        
        // REQ-TEXT-SIMPLE-001-4: 验证数据持久化
        Map<String, Object> properties = (Map<String, Object>) created.get("properties");
        assertEquals("TEST-REQ-001", properties.get("declaredShortName"), "短名称应该被保存");
        assertEquals("这是一个测试需求的详细描述文本，用于验证declaredName字段的使用", 
            properties.get("declaredName"), "长名称（描述）应该被保存");
        
        // 验证没有documentation字段
        assertNull(properties.get("documentation"), "不应该有documentation字段");
        assertNull(properties.get("text"), "不应该有text字段");
        
        System.out.println("✅ 创建测试通过：declaredName作为描述字段成功保存");
    }

    @Test
    public void testEditRequirementUsage_修改declaredName() throws Exception {
        // REQ-TEXT-SIMPLE-001-5: 测试编辑功能
        
        // 1. 先创建一个需求
        String createUrl = "/api/v1/requirements/usages?projectId=default";
        Map<String, Object> createData = Map.of(
            "declaredShortName", "EDIT-TEST",
            "declaredName", "原始描述文本",
            "requirementDefinition", "DEF-FUNC"
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> createRequest = new HttpEntity<>(createData, headers);
        
        ResponseEntity<String> createResponse = restTemplate.exchange(
            createUrl, HttpMethod.POST, createRequest, String.class);
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        
        Map<String, Object> created = objectMapper.readValue(createResponse.getBody(), Map.class);
        String elementId = (String) created.get("elementId");
        
        // 2. 编辑declaredName字段
        String editUrl = "/api/v1/elements/" + elementId + "?projectId=default";
        Map<String, Object> editData = Map.of(
            "declaredName", "这是修改后的详细描述文本，验证编辑功能是否正常工作",
            "declaredShortName", "EDIT-TEST-MODIFIED"
        );
        
        HttpEntity<Map<String, Object>> editRequest = new HttpEntity<>(editData, headers);
        ResponseEntity<String> editResponse = restTemplate.exchange(
            editUrl, HttpMethod.PATCH, editRequest, String.class);
        
        assertEquals(HttpStatus.OK, editResponse.getStatusCode());
        
        // 3. 重新查询验证修改结果
        String queryUrl = "/api/v1/elements/" + elementId + "?projectId=default";
        ResponseEntity<String> queryResponse = restTemplate.getForEntity(queryUrl, String.class);
        
        assertEquals(HttpStatus.OK, queryResponse.getStatusCode());
        
        Map<String, Object> queried = objectMapper.readValue(queryResponse.getBody(), Map.class);
        Map<String, Object> properties = (Map<String, Object>) queried.get("properties");
        
        // 验证修改成功
        assertEquals("EDIT-TEST-MODIFIED", properties.get("declaredShortName"), 
            "declaredShortName应该被更新");
        assertEquals("这是修改后的详细描述文本，验证编辑功能是否正常工作", 
            properties.get("declaredName"), "declaredName（描述）应该被更新");
        
        System.out.println("✅ 编辑测试通过：declaredName字段成功更新");
    }

    @Test
    public void testCreateRequirementDefinition_使用declaredName() throws Exception {
        // 测试RequirementDefinition也能使用declaredName
        String createUrl = "/api/v1/requirements?projectId=default";
        Map<String, Object> createData = Map.of(
            "reqId", "DEF-TEST-TEXT",
            "declaredShortName", "TestDef",
            "declaredName", "这是一个测试定义的详细描述，验证Definition也能使用declaredName"
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> createRequest = new HttpEntity<>(createData, headers);
        
        ResponseEntity<String> createResponse = restTemplate.exchange(
            createUrl, HttpMethod.POST, createRequest, String.class);
        
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        
        Map<String, Object> created = objectMapper.readValue(createResponse.getBody(), Map.class);
        Map<String, Object> properties = (Map<String, Object>) created.get("properties");
        
        // 验证字段
        assertEquals("DEF-TEST-TEXT", properties.get("reqId"));
        assertEquals("TestDef", properties.get("declaredShortName"));
        assertEquals("这是一个测试定义的详细描述，验证Definition也能使用declaredName", 
            properties.get("declaredName"));
        
        // 验证没有documentation相关字段
        assertNull(properties.get("documentation"));
        assertNull(properties.get("text"));
        
        System.out.println("✅ RequirementDefinition测试通过：declaredName正常工作");
    }

    @Test
    public void testQueryRequirement_显示declaredName() throws Exception {
        // REQ-TEXT-SIMPLE-001-2: 测试前端显示
        
        // 1. 创建带描述的需求
        String createUrl = "/api/v1/requirements/usages?projectId=default";
        Map<String, Object> createData = Map.of(
            "declaredShortName", "DISPLAY-TEST",
            "declaredName", "用于测试显示功能的描述文本内容"
        );
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> createRequest = new HttpEntity<>(createData, headers);
        
        ResponseEntity<String> createResponse = restTemplate.exchange(
            createUrl, HttpMethod.POST, createRequest, String.class);
        assertEquals(HttpStatus.CREATED, createResponse.getStatusCode());
        
        // 2. 查询所有需求
        String queryUrl = "/api/v1/requirements/usages?projectId=default";
        ResponseEntity<String> queryResponse = restTemplate.getForEntity(queryUrl, String.class);
        assertEquals(HttpStatus.OK, queryResponse.getStatusCode());
        
        // 3. 验证返回的数据包含declaredName
        Map<String, Object>[] results = objectMapper.readValue(queryResponse.getBody(), Map[].class);
        boolean found = false;
        for (Map<String, Object> item : results) {
            Map<String, Object> props = (Map<String, Object>) item.get("properties");
            if ("DISPLAY-TEST".equals(props.get("declaredShortName"))) {
                assertEquals("用于测试显示功能的描述文本内容", props.get("declaredName"),
                    "查询结果应包含declaredName字段");
                found = true;
                break;
            }
        }
        assertTrue(found, "应该能查询到创建的需求");
        
        System.out.println("✅ 查询显示测试通过：declaredName字段正确返回");
    }
}