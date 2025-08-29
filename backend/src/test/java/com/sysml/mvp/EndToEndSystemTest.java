package com.sysml.mvp;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class EndToEndSystemTest {

    @LocalServerPort
    private int port;

    private TestRestTemplate restTemplate = new TestRestTemplate();
    private ObjectMapper objectMapper = new ObjectMapper();

    private String getBaseUrl() {
        return "http://localhost:" + port + "/api/v1";
    }

    @BeforeEach
    void setUp() {
        // 清理可能存在的数据
        System.out.println("正在初始化端到端测试...");
    }

    @Test
    void testCompleteSystemWorkflow() {
        System.out.println("=== 开始完整系统工作流测试 ===");
        
        // Step 1: 创建RequirementDefinition
        String defId = createRequirementDefinition();
        assertNotNull(defId, "RequirementDefinition创建失败");
        System.out.println("✅ RequirementDefinition创建成功: " + defId);

        // Step 2: 创建RequirementUsage
        String usageId = createRequirementUsage(defId);
        assertNotNull(usageId, "RequirementUsage创建失败");
        System.out.println("✅ RequirementUsage创建成功: " + usageId);

        // Step 3: 测试高级查询
        testAdvancedQuery();
        System.out.println("✅ 高级查询功能正常");

        // Step 4: 测试更新功能
        testUpdateElement(defId);
        System.out.println("✅ 更新功能正常");

        // Step 5: 测试删除功能
        testDeleteElement(usageId);
        System.out.println("✅ 删除功能正常");

        System.out.println("=== 端到端测试全部通过 ===");
    }

    private String createRequirementDefinition() {
        Map<String, Object> reqDef = new HashMap<>();
        reqDef.put("eClass", "RequirementDefinition");
        reqDef.put("declaredName", "电池系统性能需求");
        reqDef.put("declaredShortName", "EBS-L1-001");
        reqDef.put("reqId", "EBS-L1-001");
        reqDef.put("text", "电池系统应在-20℃至45℃环境温度下正常工作，容量保持率不低于80%");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(reqDef, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                getBaseUrl() + "/elements", request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "创建RequirementDefinition失败: " + response.getBody());
        
        try {
            JsonNode json = objectMapper.readTree(response.getBody());
            return json.get("data").get("elementId").asText();
        } catch (Exception e) {
            System.out.println("解析响应失败: " + response.getBody());
            return null;
        }
    }

    private String createRequirementUsage(String defId) {
        Map<String, Object> reqUsage = new HashMap<>();
        reqUsage.put("eClass", "RequirementUsage");
        reqUsage.put("declaredName", "高温环境性能验证");
        reqUsage.put("declaredShortName", "EBS-L1-001-U1");
        reqUsage.put("reqId", "EBS-L1-001-U1");
        reqUsage.put("text", "在45℃环境下验证电池系统性能指标");
        reqUsage.put("requirementDefinition", defId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(reqUsage, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(
                getBaseUrl() + "/elements", request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "创建RequirementUsage失败: " + response.getBody());
        
        try {
            JsonNode json = objectMapper.readTree(response.getBody());
            return json.get("data").get("elementId").asText();
        } catch (Exception e) {
            System.out.println("解析响应失败: " + response.getBody());
            return null;
        }
    }

    private void testAdvancedQuery() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                getBaseUrl() + "/elements/advanced?page=0&size=10", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "高级查询失败: " + response.getBody());
        
        try {
            JsonNode json = objectMapper.readTree(response.getBody());
            assertTrue(json.get("content").isArray(), "响应应包含content数组");
            assertTrue(json.get("content").size() >= 2, "应该至少返回2个元素");
            System.out.println("查询到 " + json.get("content").size() + " 个元素");
        } catch (Exception e) {
            fail("解析高级查询响应失败: " + response.getBody());
        }
    }

    private void testUpdateElement(String elementId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("declaredName", "更新后的电池系统性能需求");
        updates.put("text", "更新后的需求描述");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(updates, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                getBaseUrl() + "/elements/" + elementId,
                org.springframework.http.HttpMethod.PATCH,
                request, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "更新元素失败: " + response.getBody());
    }

    private void testDeleteElement(String elementId) {
        ResponseEntity<String> response = restTemplate.exchange(
                getBaseUrl() + "/elements/" + elementId,
                org.springframework.http.HttpMethod.DELETE,
                null, String.class);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode(), "删除元素失败: " + response.getBody());
    }
}