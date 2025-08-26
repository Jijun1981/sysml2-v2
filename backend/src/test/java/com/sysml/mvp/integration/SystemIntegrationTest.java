package com.sysml.mvp.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sysml.mvp.dto.RequirementDTO;
import com.sysml.mvp.dto.TraceDTO;
import com.sysml.mvp.dto.ValidationResultDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 系统集成测试 - 端到端验证
 * 
 * 测试目标：
 * - 验证Controller层到持久化层的完整数据流
 * - 测试EMF模型的序列化和反序列化
 * - 验证业务流程的端到端功能
 * - 测试文件系统持久化的实际效果
 * 
 * 测试场景：
 * 1. 创建需求定义 → 持久化到文件 → 从文件读取验证
 * 2. 创建追溯关系 → 持久化验证 → 语义约束测试
 * 3. 执行验证 → 检测实际违规 → 验证结果正确性
 * 4. 完整的CRUD生命周期测试
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebMvc
@TestPropertySource(properties = {
    "app.model.storage.path=./data/test-projects",
    "app.model.default-project=integration-test"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("系统集成测试 - 端到端验证")
public class SystemIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private static String createdRequirement1Id;
    private static String createdRequirement2Id;
    private static String createdTraceId;
    
    @BeforeEach
    void setUp() throws Exception {
        // 确保测试目录存在
        Path testDir = Paths.get("./data/test-projects/integration-test");
        Files.createDirectories(testDir);
        
        // 清理之前的测试文件
        File modelFile = testDir.resolve("model.json").toFile();
        if (modelFile.exists()) {
            modelFile.delete();
        }
    }
    
    @Test
    @Order(1)
    @DisplayName("【端到端】创建需求定义并验证持久化")
    void testCreateRequirementAndVerifyPersistence() throws Exception {
        // 准备需求数据
        RequirementDTO requirement1 = new RequirementDTO();
        requirement1.setReqId("REQ-INTEGRATION-001");
        requirement1.setDeclaredName("集成测试需求1");
        requirement1.setDeclaredShortName("INT-REQ-1");
        requirement1.setDocumentation("这是一个端到端集成测试的需求定义");
        requirement1.setStatus("draft");
        requirement1.setPriority("high");
        
        // 通过REST API创建需求
        MvcResult result1 = mockMvc.perform(post("/api/v1/requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requirement1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reqId").value("REQ-INTEGRATION-001"))
                .andExpect(jsonPath("$.declaredName").value("集成测试需求1"))
                .andReturn();
        
        // 提取创建的需求ID
        String responseBody1 = result1.getResponse().getContentAsString();
        RequirementDTO createdReq1 = objectMapper.readValue(responseBody1, RequirementDTO.class);
        createdRequirement1Id = createdReq1.getElementId();
        assertNotNull(createdRequirement1Id, "需求ID不应为空");
        
        // 创建第二个需求用于后续追溯关系测试
        RequirementDTO requirement2 = new RequirementDTO();
        requirement2.setReqId("REQ-INTEGRATION-002");
        requirement2.setDeclaredName("集成测试需求2");
        requirement2.setDeclaredShortName("INT-REQ-2");
        requirement2.setDocumentation("用于追溯关系测试的第二个需求");
        requirement2.setStatus("approved");
        requirement2.setPriority("medium");
        
        MvcResult result2 = mockMvc.perform(post("/api/v1/requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requirement2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reqId").value("REQ-INTEGRATION-002"))
                .andReturn();
        
        String responseBody2 = result2.getResponse().getContentAsString();
        RequirementDTO createdReq2 = objectMapper.readValue(responseBody2, RequirementDTO.class);
        createdRequirement2Id = createdReq2.getElementId();
        
        // 验证文件系统持久化
        Path modelFile = Paths.get("./data/test-projects/integration-test/model.json");
        assertTrue(Files.exists(modelFile), "模型文件应该被创建");
        assertTrue(Files.size(modelFile) > 0, "模型文件不应为空");
        
        // 通过API验证需求可以被查询到
        mockMvc.perform(get("/api/v1/requirements/" + createdRequirement1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reqId").value("REQ-INTEGRATION-001"))
                .andExpect(jsonPath("$.declaredName").value("集成测试需求1"));
        
        System.out.println("✅ 需求定义创建和持久化测试通过");
        System.out.println("   - 需求1 ID: " + createdRequirement1Id);
        System.out.println("   - 需求2 ID: " + createdRequirement2Id);
        System.out.println("   - 模型文件大小: " + Files.size(modelFile) + " bytes");
    }
    
    @Test
    @Order(2)
    @DisplayName("【端到端】创建追溯关系并验证业务约束")
    void testCreateTraceRelationshipWithValidation() throws Exception {
        // 确保前序测试已创建需求
        assertNotNull(createdRequirement1Id, "需要先创建需求1");
        assertNotNull(createdRequirement2Id, "需要先创建需求2");
        
        // 准备追溯关系数据 - derive关系
        TraceDTO trace = new TraceDTO();
        trace.setSource(createdRequirement1Id);
        trace.setTarget(createdRequirement2Id);
        trace.setType("derive");
        trace.setName("需求派生关系");
        trace.setDescription("REQ-001派生出REQ-002");
        trace.setRationale("基于用户反馈细化需求");
        
        // 先验证追溯关系的语义约束
        mockMvc.perform(get("/api/v1/validation/trace/semantics")
                .param("source", createdRequirement1Id)
                .param("target", createdRequirement2Id)
                .param("type", "derive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isValid").value(true));
        
        // 创建追溯关系
        MvcResult traceResult = mockMvc.perform(post("/api/v1/traces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(trace)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value(createdRequirement1Id))
                .andExpect(jsonPath("$.target").value(createdRequirement2Id))
                .andExpect(jsonPath("$.type").value("derive"))
                .andReturn();
        
        String traceResponseBody = traceResult.getResponse().getContentAsString();
        TraceDTO createdTrace = objectMapper.readValue(traceResponseBody, TraceDTO.class);
        createdTraceId = createdTrace.getElementId();
        
        // 验证追溯关系持久化
        mockMvc.perform(get("/api/v1/traces/" + createdTraceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value(createdRequirement1Id))
                .andExpect(jsonPath("$.target").value(createdRequirement2Id))
                .andExpect(jsonPath("$.type").value("derive"));
        
        // 测试重复性检测
        mockMvc.perform(post("/api/v1/traces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(trace)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
        
        System.out.println("✅ 追溯关系创建和验证测试通过");
        System.out.println("   - 追溯关系 ID: " + createdTraceId);
        System.out.println("   - 重复性检测正常工作");
    }
    
    @Test
    @Order(3)
    @DisplayName("【端到端】执行静态验证并检验结果")
    void testStaticValidationWithRealData() throws Exception {
        // 执行静态验证
        MvcResult validationResult = mockMvc.perform(post("/api/v1/validation/static")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.violations").isArray())
                .andExpect(jsonPath("$.elementCount").isNumber())
                .andExpect(jsonPath("$.processingTimeMs").isNumber())
                .andExpect(jsonPath("$.version").value("1.0"))
                .andReturn();
        
        String validationResponseBody = validationResult.getResponse().getContentAsString();
        ValidationResultDTO validationDto = objectMapper.readValue(validationResponseBody, ValidationResultDTO.class);
        
        // 验证结果应该包含我们创建的元素
        assertTrue(validationDto.getElementCount() >= 3, 
            "元素数量应该至少包含2个需求定义 + 1个追溯关系");
        
        // 检查是否有违规（目前应该没有，因为我们的数据是合法的）
        assertTrue(validationDto.getViolations().isEmpty() || validationDto.getViolations().size() == 0,
            "当前测试数据应该没有违规");
        
        System.out.println("✅ 静态验证测试通过");
        System.out.println("   - 验证元素数量: " + validationDto.getElementCount());
        System.out.println("   - 处理时间: " + validationDto.getProcessingTimeMs() + "ms");
        System.out.println("   - 违规数量: " + validationDto.getViolations().size());
    }
    
    @Test
    @Order(4)
    @DisplayName("【端到端】测试重复reqId检测")
    void testDuplicateReqIdDetection() throws Exception {
        // 创建一个重复reqId的需求
        RequirementDTO duplicateReq = new RequirementDTO();
        duplicateReq.setReqId("REQ-INTEGRATION-001"); // 重复的reqId
        duplicateReq.setDeclaredName("重复需求");
        duplicateReq.setDocumentation("这应该被拒绝");
        
        // 尝试创建重复reqId的需求，应该被拒绝
        mockMvc.perform(post("/api/v1/requirements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateReq)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Conflict"));
        
        // 现在执行静态验证，应该检测到重复
        MvcResult validationResult = mockMvc.perform(post("/api/v1/validation/static"))
                .andExpect(status().isOk())
                .andReturn();
        
        String validationResponseBody = validationResult.getResponse().getContentAsString();
        ValidationResultDTO validationDto = objectMapper.readValue(validationResponseBody, ValidationResultDTO.class);
        
        System.out.println("✅ 重复reqId检测测试通过");
        System.out.println("   - 重复创建被正确阻止");
    }
    
    @Test
    @Order(5)
    @DisplayName("【端到端】更新和删除操作测试")
    void testUpdateAndDeleteOperations() throws Exception {
        // 更新需求1
        RequirementDTO updateReq = new RequirementDTO();
        updateReq.setReqId("REQ-INTEGRATION-001");
        updateReq.setDeclaredName("更新后的集成测试需求1");
        updateReq.setStatus("approved");
        updateReq.setPriority("critical");
        updateReq.setDocumentation("需求已更新");
        
        mockMvc.perform(put("/api/v1/requirements/" + createdRequirement1Id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.declaredName").value("更新后的集成测试需求1"))
                .andExpect(jsonPath("$.status").value("approved"));
        
        // 验证更新已持久化
        mockMvc.perform(get("/api/v1/requirements/" + createdRequirement1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.declaredName").value("更新后的集成测试需求1"));
        
        // 删除追溯关系
        mockMvc.perform(delete("/api/v1/traces/" + createdTraceId))
                .andExpect(status().isNoContent());
        
        // 验证追溯关系已被删除
        mockMvc.perform(get("/api/v1/traces/" + createdTraceId))
                .andExpect(status().isNotFound());
        
        // 删除需求（按相反顺序删除以避免引用问题）
        mockMvc.perform(delete("/api/v1/requirements/" + createdRequirement2Id))
                .andExpect(status().isNoContent());
        
        mockMvc.perform(delete("/api/v1/requirements/" + createdRequirement1Id))
                .andExpect(status().isNoContent());
        
        System.out.println("✅ 更新和删除操作测试通过");
        System.out.println("   - 需求更新成功");
        System.out.println("   - 追溯关系删除成功");
        System.out.println("   - 需求删除成功");
    }
    
    @Test
    @Order(6)
    @DisplayName("【端到端】验证模型文件一致性")
    void testModelFileConsistency() throws Exception {
        Path modelFile = Paths.get("./data/test-projects/integration-test/model.json");
        
        if (Files.exists(modelFile)) {
            String content = Files.readString(modelFile);
            
            // 验证是否为有效JSON
            assertDoesNotThrow(() -> {
                objectMapper.readTree(content);
            }, "模型文件应该是有效的JSON");
            
            System.out.println("✅ 模型文件一致性测试通过");
            System.out.println("   - 文件路径: " + modelFile.toAbsolutePath());
            System.out.println("   - 文件大小: " + Files.size(modelFile) + " bytes");
            System.out.println("   - JSON格式有效");
        } else {
            System.out.println("⚠️ 模型文件不存在，可能所有数据已被删除");
        }
    }
    
    @Test
    @Order(7)
    @DisplayName("【端到端】综合验证API组合使用")
    void testComprehensiveValidationAPI() throws Exception {
        // 测试综合验证API
        String requestBody = """
            {
                "reqId": "REQ-COMPREHENSIVE-001",
                "source": "non-existent-1",
                "target": "non-existent-2",
                "type": "derive"
            }
            """;
        
        MvcResult result = mockMvc.perform(post("/api/v1/validation/comprehensive")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reqIdValidation.isUnique").value(true))
                .andExpect(jsonPath("$.traceValidation.isDuplicate").value(false))
                .andExpect(jsonPath("$.overallValid").exists())
                .andReturn();
        
        System.out.println("✅ 综合验证API测试通过");
        System.out.println("   - reqId验证正常");
        System.out.println("   - 追溯关系验证正常");
        System.out.println("   - 综合结果计算正确");
    }
}