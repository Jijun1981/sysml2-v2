package com.sysml.mvp;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import com.sysml.mvp.dto.HealthResponse;
import com.sysml.mvp.dto.ModelHealthResponse;

/**
 * 健康检查接口测试
 * 验证REQ-A2-1和REQ-A2-2的健康检查功能
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class HealthControllerTest {
    
    @LocalServerPort
    private int port;
    
    @Autowired
    private TestRestTemplate restTemplate;
    
    private String baseUrl;
    
    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }
    
    @Test
    @DisplayName("REQ-A2-1: 基础健康检查 - 应返回UP状态和必需字段")
    void testBasicHealthCheck() throws Exception {
        // Given: 预热请求（首次请求会初始化DispatcherServlet）
        restTemplate.getForEntity(baseUrl + "/health", HealthResponse.class);
        
        // When: 调用基础健康检查接口（真实性能测试）
        long startTime = System.currentTimeMillis();
        ResponseEntity<HealthResponse> response = restTemplate.getForEntity(
            baseUrl + "/health", 
            HealthResponse.class
        );
        long responseTime = System.currentTimeMillis() - startTime;
        
        // Then: 应返回200 OK
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // 验证响应时间 < 100ms（开发机要求，预热后）
        assertTrue(responseTime < 100, 
            "健康检查响应时间应 < 100ms，实际: " + responseTime + "ms");
        
        // 验证响应体结构
        HealthResponse healthInfo = response.getBody();
        assertNotNull(healthInfo, "响应体不能为空");
        
        // 必需字段验证
        assertNotNull(healthInfo.getStatus(), "响应应包含status字段");
        String status = healthInfo.getStatus();
        assertTrue(status.equals("UP") || status.equals("DOWN"), 
            "status应为UP或DOWN，实际: " + status);
        
        assertNotNull(healthInfo.getBuildVersion(), "响应应包含buildVersion字段");
        assertFalse(healthInfo.getBuildVersion().isEmpty(), "buildVersion不能为空");
        
        assertNotNull(healthInfo.getGitCommit(), "响应应包含gitCommit字段");
        assertFalse(healthInfo.getGitCommit().isEmpty(), "gitCommit不能为空");
        
        assertNotNull(healthInfo.getServerTimeUtc(), "响应应包含serverTimeUtc字段");
        
        // 验证serverTimeUtc格式（应为ISO-8601 UTC格式，支持纳秒精度）
        String serverTime = healthInfo.getServerTimeUtc();
        assertTrue(serverTime.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z"), 
            "serverTimeUtc应为ISO-8601 UTC格式，实际: " + serverTime);
    }
    
    @Test
    @DisplayName("REQ-A2-1: 健康检查性能要求 - 多次调用验证稳定性")
    void testHealthCheckPerformance() {
        // Given: 进行多次健康检查调用
        int testCount = 5;
        long totalTime = 0;
        
        // When: 多次调用健康检查
        for (int i = 0; i < testCount; i++) {
            long startTime = System.currentTimeMillis();
            ResponseEntity<HealthResponse> response = restTemplate.getForEntity(
                baseUrl + "/health", 
                HealthResponse.class
            );
            long responseTime = System.currentTimeMillis() - startTime;
            totalTime += responseTime;
            
            // Then: 每次调用都应成功且快速
            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertTrue(responseTime < 100, 
                "第" + (i+1) + "次调用响应时间应 < 100ms，实际: " + responseTime + "ms");
        }
        
        // 平均响应时间验证
        double avgTime = (double) totalTime / testCount;
        assertTrue(avgTime < 50, 
            "平均响应时间应 < 50ms，实际: " + avgTime + "ms");
    }
    
    @Test
    @DisplayName("REQ-A2-2: EMF模型健康检查 - 应返回已注册EPackage列表")
    void testModelHealthCheck() throws Exception {
        // When: 调用EMF模型健康检查接口
        ResponseEntity<ModelHealthResponse> response = restTemplate.getForEntity(
            baseUrl + "/health/model", 
            ModelHealthResponse.class
        );
        
        // Then: 应返回200 OK
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // 验证响应体结构
        ModelHealthResponse modelHealth = response.getBody();
        assertNotNull(modelHealth, "响应体不能为空");
        
        // 应包含packages字段
        assertNotNull(modelHealth.getPackages(), "响应应包含packages字段");
        
        // 验证必需的SysML2包存在
        boolean foundSysML2 = false;
        for (ModelHealthResponse.PackageInfo pkg : modelHealth.getPackages()) {
            assertNotNull(pkg.getNsUri(), "每个包应有nsUri字段");
            assertNotNull(pkg.getSource(), "每个包应有source字段");
            
            if ("urn:your:sysml2".equals(pkg.getNsUri())) {
                foundSysML2 = true;
                assertEquals("local", pkg.getSource(), 
                    "SysML2包的来源应标注为local");
                assertEquals("sysml", pkg.getName(), 
                    "SysML2包的名称应为sysml");
            }
        }
        
        assertTrue(foundSysML2, 
            "应包含urn:your:sysml2命名空间的包");
        
        // 验证至少包含基本的包信息
        assertTrue(modelHealth.getPackages().size() > 0, 
            "应至少注册一个EPackage");
    }
    
    @Test
    @DisplayName("REQ-A2-2: EMF模型健康检查 - 验证包内容详情")
    void testModelHealthCheckDetails() throws Exception {
        // When: 调用EMF模型健康检查接口
        ResponseEntity<ModelHealthResponse> response = restTemplate.getForEntity(
            baseUrl + "/health/model", 
            ModelHealthResponse.class
        );
        
        // Then: 验证SysML2包的详细信息
        ModelHealthResponse modelHealth = response.getBody();
        assertNotNull(modelHealth, "响应体不能为空");
        
        ModelHealthResponse.PackageInfo sysmlPackage = null;
        for (ModelHealthResponse.PackageInfo pkg : modelHealth.getPackages()) {
            if ("urn:your:sysml2".equals(pkg.getNsUri())) {
                sysmlPackage = pkg;
                break;
            }
        }
        
        assertNotNull(sysmlPackage, "应找到SysML2包");
        
        // 验证包的基本属性
        assertEquals("sysml", sysmlPackage.getName(), "包名应为sysml");
        assertEquals("local", sysmlPackage.getSource(), "来源应为local");
        
        // 验证分类器数量（至少应有RequirementDefinition和RequirementUsage）
        assertNotNull(sysmlPackage.getClassCount(), "应包含classCount字段");
        assertTrue(sysmlPackage.getClassCount() >= 2, 
            "至少应包含2个分类器（RequirementDefinition和RequirementUsage）");
        
        // 验证响应状态
        assertNotNull(modelHealth.getStatus(), "响应应包含status字段");
        assertEquals("UP", modelHealth.getStatus(), "模型健康状态应为UP");
    }
    
    @Test
    @DisplayName("健康检查接口路径验证 - 确保路径正确")
    void testHealthEndpointPaths() {
        // 测试context path情况下的完整路径
        String healthPath = baseUrl + "/health";
        String modelHealthPath = baseUrl + "/health/model";
        
        // Test basic health endpoint
        ResponseEntity<HealthResponse> basicHealth = restTemplate.getForEntity(
            healthPath, 
            HealthResponse.class
        );
        assertEquals(HttpStatus.OK, basicHealth.getStatusCode());
        
        // Test model health endpoint  
        ResponseEntity<ModelHealthResponse> modelHealth = restTemplate.getForEntity(
            modelHealthPath, 
            ModelHealthResponse.class
        );
        assertEquals(HttpStatus.OK, modelHealth.getStatusCode());
        
        // Test invalid health endpoint should return error status (404 or 500)
        ResponseEntity<String> invalidHealth = restTemplate.getForEntity(
            baseUrl + "/nonexistent", 
            String.class
        );
        assertTrue(invalidHealth.getStatusCode().is4xxClientError() || invalidHealth.getStatusCode().is5xxServerError(),
            "无效路径应返回错误状态码，实际: " + invalidHealth.getStatusCode());
    }
}