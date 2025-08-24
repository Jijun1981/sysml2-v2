package com.sysml.mvp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.jayway.jsonpath.JsonPath;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD测试用例 - 基于REQ-A2-2 EMF模型健康检查
 * 
 * 需求验收标准：
 * - GET /health/model 返回已注册EPackage摘要信息：总数、每个包的完整nsURI（如https://www.omg.org/spec/SysML/20250201）
 * - 支持 ?detailed=true 返回完整EClass列表
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class HealthControllerPilotTest {
    
    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    public void shouldReturnRegisteredEPackageSummary() throws Exception {
        // REQ-A2-2: GET /health/model 返回已注册EPackage摘要信息：总数、每个包的完整nsURI
        String url = "http://localhost:" + port + "/health/model";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        String responseBody = response.getBody();
        assertNotNull(responseBody);
        
        // 验证返回摘要信息：总数
        assertEquals("UP", JsonPath.read(responseBody, "$.status"));
        assertTrue(JsonPath.read(responseBody, "$.totalCount") instanceof Number);
        Integer totalCount = JsonPath.read(responseBody, "$.totalCount");
        assertTrue(totalCount > 0, "总数必须大于0");
        
        // 验证包含packages数组
        List<Object> packages = JsonPath.read(responseBody, "$.packages");
        assertTrue(packages.size() > 0, "必须返回至少一个已注册的EPackage");
        
        // 验证每个包的完整nsURI
        for (int i = 0; i < packages.size(); i++) {
            String nsURI = JsonPath.read(responseBody, "$.packages[" + i + "].nsUri");
            assertNotNull(nsURI, "每个包必须有nsURI字段");
            assertFalse(nsURI.isEmpty(), "nsURI不能为空");
        }
    }
    
    @Test
    public void shouldReturnPilotStandardNamespaceURI() throws Exception {
        // REQ-A2-2: 返回每个包的完整nsURI（如https://www.omg.org/spec/SysML/20250201）
        String url = "http://localhost:" + port + "/health/model";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        String responseBody = response.getBody();
        assertNotNull(responseBody);
        
        // 验证SysML包的完整nsURI使用Pilot标准格式
        List<String> nsURIs = JsonPath.read(responseBody, "$.packages[*].nsUri");
        boolean foundPilotURI = false;
        
        for (String nsURI : nsURIs) {
            if (nsURI.startsWith("https://www.omg.org/spec/SysML/")) {
                foundPilotURI = true;
                // 验证是完整的Pilot标准命名空间格式
                assertTrue(nsURI.matches("https://www\\.omg\\.org/spec/SysML/\\d{8}"), 
                           "nsURI必须符合Pilot标准格式 https://www.omg.org/spec/SysML/YYYYMMDD，实际: " + nsURI);
                break;
            }
        }
        
        assertTrue(foundPilotURI, "必须包含Pilot标准命名空间URI");
    }
    
    @Test 
    public void shouldSupportDetailedModeForCompleteEClassList() throws Exception {
        // REQ-A2-2: 支持?detailed=true返回完整EClass列表
        String url = "http://localhost:" + port + "/health/model?detailed=true";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        String responseBody = response.getBody();
        assertNotNull(responseBody);
        
        // 验证detailed模式返回完整EClass列表
        List<Object> eClasses = JsonPath.read(responseBody, "$.packages[0].eClasses");
        assertNotNull(eClasses, "详细模式必须返回eClasses字段");
        assertTrue(eClasses.size() > 0, "详细模式必须返回EClass列表");
        
        // 验证EClass数量与eClassCount一致
        Integer eClassCount = JsonPath.read(responseBody, "$.packages[0].eClassCount");
        assertEquals(eClasses.size(), eClassCount.intValue(), "eClasses数组大小必须与eClassCount一致");
        
        // 验证包含SysML核心类
        List<String> eClassNames = JsonPath.read(responseBody, "$.packages[0].eClasses[*]");
        assertTrue(eClassNames.contains("Element"), "必须包含Element类");
        assertTrue(eClassNames.contains("RequirementDefinition"), "必须包含RequirementDefinition类");
        assertTrue(eClassNames.contains("RequirementUsage"), "必须包含RequirementUsage类");
    }
    
    @Test
    public void shouldReturnEClassCountInSummary() throws Exception {
        // REQ-A2-2: 返回已注册EPackage摘要信息：总数
        String url = "http://localhost:" + port + "/health/model";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        String responseBody = response.getBody();
        assertNotNull(responseBody);
        
        // 验证totalCount字段
        Integer totalCount = JsonPath.read(responseBody, "$.totalCount");
        assertTrue(totalCount > 100, "完整Pilot元模型应该有100+个EClass，实际: " + totalCount);
        
        // 验证每个包的eClassCount字段
        Integer packageClassCount = JsonPath.read(responseBody, "$.packages[0].eClassCount");
        assertTrue(packageClassCount > 100, "SysML包应包含100+个EClass，实际: " + packageClassCount);
        
        // 验证totalCount与各包eClassCount的一致性
        List<Integer> classCounts = JsonPath.read(responseBody, "$.packages[*].eClassCount");
        int sumClassCount = classCounts.stream().mapToInt(Integer::intValue).sum();
        assertEquals(totalCount.intValue(), sumClassCount, "totalCount必须等于所有包的eClassCount之和");
    }
    
    @Test
    public void shouldNotReturnEClassesInNonDetailedMode() throws Exception {
        // REQ-A2-2: 默认模式不应返回完整EClass列表（仅摘要信息）
        String url = "http://localhost:" + port + "/health/model";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        String responseBody = response.getBody();
        assertNotNull(responseBody);
        
        // 验证默认模式不返回eClasses字段（或为null）
        try {
            List<String> eClasses = JsonPath.read(responseBody, "$.packages[0].eClasses");
            assertNull(eClasses, "默认模式不应返回eClasses字段");
        } catch (Exception e) {
            // JsonPath找不到eClasses字段是正确的
        }
        
        // 但应该返回eClassCount摘要信息
        Integer eClassCount = JsonPath.read(responseBody, "$.packages[0].eClassCount");
        assertNotNull(eClassCount, "默认模式必须返回eClassCount摘要信息");
    }
    
    @Test
    public void shouldHaveConsistentCountBetweenSummaryAndDetailed() throws Exception {
        // 验证摘要模式和详细模式的数量一致性
        String summaryUrl = "http://localhost:" + port + "/health/model";
        ResponseEntity<String> summaryResponse = restTemplate.getForEntity(summaryUrl, String.class);
        assertEquals(HttpStatus.OK, summaryResponse.getStatusCode());
        
        String detailedUrl = "http://localhost:" + port + "/health/model?detailed=true";
        ResponseEntity<String> detailedResponse = restTemplate.getForEntity(detailedUrl, String.class);
        assertEquals(HttpStatus.OK, detailedResponse.getStatusCode());
        
        Integer summaryCount = JsonPath.read(summaryResponse.getBody(), "$.packages[0].eClassCount");
        Integer detailedCount = JsonPath.read(detailedResponse.getBody(), "$.packages[0].eClassCount");
        List<String> detailedEClasses = JsonPath.read(detailedResponse.getBody(), "$.packages[0].eClasses");
        
        assertEquals(summaryCount, detailedCount, "摘要和详细模式的eClassCount必须一致");
        assertEquals(detailedEClasses.size(), detailedCount.intValue(), "详细模式EClass数组大小必须与数量一致");
    }
}