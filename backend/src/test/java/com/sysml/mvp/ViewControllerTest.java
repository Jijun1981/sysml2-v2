package com.sysml.mvp;

import com.sysml.mvp.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ViewController TDD测试 - 简化版本
 * 先定义需求验收标准，后实现功能
 * 验证REQ-D1-1, REQ-D2-1, REQ-D3-1核心功能
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ViewControllerTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * REQ-D1-1测试: GET /tree
     * 验收标准: 返回Definition为父、Usage为子的树结构
     */
    @Test
    void testREQ_D1_1_TreeViewStructure() throws Exception {
        // 创建测试数据
        createTestRequirements();
        
        // 验证REQ-D1-1: 树视图接口返回正确结构
        String url = "http://localhost:" + port + "/tree";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        // 基本验证接口可访问并返回数据
        assertTrue(response.getBody().contains("id"));
        assertTrue(response.getBody().contains("label"));
    }

    /**
     * REQ-D2-1测试: GET /api/v1/views/table  
     * 验收标准: 支持分页参数，返回表格数据
     */
    @Test
    void testREQ_D2_1_TableViewBasic() throws Exception {
        createTestRequirements();
        
        // 基本表格查询
        String url = "http://localhost:" + port + "/table";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        // 验证size参数边界 (1,200]
        String urlWithLargeSize = url + "?size=250";
        ResponseEntity<String> badResponse = restTemplate.getForEntity(urlWithLargeSize, String.class);
        assertEquals(HttpStatus.BAD_REQUEST, badResponse.getStatusCode());
    }

    /**
     * REQ-D3-1测试: GET /graph
     * 验收标准: 返回nodes/edges结构
     */
    @Test
    void testREQ_D3_1_GraphViewBasic() throws Exception {
        createTestRequirements();
        
        String url = "http://localhost:" + port + "/graph";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        // 基本验证接口可访问并返回数据
        assertTrue(response.getBody().contains("nodes"));
        assertTrue(response.getBody().contains("edges"));
    }

    // 辅助方法：创建测试数据
    private void createTestRequirements() throws Exception {
        // 创建Definition，使用时间戳避免reqId冲突
        String uniqueReqId = "TEST-" + System.currentTimeMillis();
        RequirementDefinitionDTO.CreateRequest def = RequirementDefinitionDTO.CreateRequest.builder()
                .type("definition")
                .reqId(uniqueReqId)
                .name("测试需求定义")
                .text("测试文本")
                .build();
        
        String url = "http://localhost:" + port + "/requirements";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RequirementDefinitionDTO.CreateRequest> entity = new HttpEntity<>(def, headers);
        
        ResponseEntity<RequirementDefinitionDTO> response = restTemplate.postForEntity(url, entity, RequirementDefinitionDTO.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }
}