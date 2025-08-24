package com.sysml.mvp;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Phase 4: 通用元素接口测试
 * 测试REQ-B5-1, REQ-B5-2, REQ-B5-3, REQ-B5-4
 * 
 * 验证动态EMF模式的核心价值：一个接口处理所有SysML类型
 */
@SpringBootTest
@AutoConfigureMockMvc
public class UniversalElementControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * REQ-B5-1: 通用创建接口 - 创建RequirementDefinition
     */
    @Test
    void shouldCreateRequirementDefinitionViaUniversalInterface() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("eClass", "RequirementDefinition");
        
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("declaredShortName", "REQ-UNI-001");
        attributes.put("declaredName", "Universal Requirement");
        attributes.put("documentation", "Created via universal interface");
        request.put("attributes", attributes);
        
        mockMvc.perform(post("/api/v1/elements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.eClass").value("RequirementDefinition"))
            .andExpect(jsonPath("$.declaredShortName").value("REQ-UNI-001"))
            .andExpect(jsonPath("$.declaredName").value("Universal Requirement"))
            .andExpect(jsonPath("$.elementId").exists());
    }
    
    /**
     * REQ-B5-1: 通用创建接口 - 创建PartUsage（无需专门代码）
     */
    @Test
    void shouldCreatePartUsageViaUniversalInterface() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("eClass", "PartUsage");
        
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("declaredName", "Engine");
        attributes.put("declaredShortName", "eng");
        attributes.put("documentation", "Main engine part");
        request.put("attributes", attributes);
        
        mockMvc.perform(post("/api/v1/elements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.eClass").value("PartUsage"))
            .andExpect(jsonPath("$.declaredName").value("Engine"))
            .andExpect(jsonPath("$.elementId").exists());
    }
    
    /**
     * REQ-B5-1: 通用创建接口 - 创建InterfaceDefinition（无需专门代码）
     */
    @Test
    void shouldCreateInterfaceDefinitionViaUniversalInterface() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("eClass", "InterfaceDefinition");
        
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("declaredName", "PowerInterface");
        attributes.put("documentation", "Interface for power transmission");
        request.put("attributes", attributes);
        
        mockMvc.perform(post("/api/v1/elements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.eClass").value("InterfaceDefinition"))
            .andExpect(jsonPath("$.declaredName").value("PowerInterface"))
            .andExpect(jsonPath("$.elementId").exists());
    }
    
    /**
     * REQ-B5-1: 未知eClass返回400
     */
    @Test
    void shouldReturn400ForUnknownEClass() throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("eClass", "NonExistentClass");
        request.put("attributes", new HashMap<>());
        
        mockMvc.perform(post("/api/v1/elements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").exists());
    }
    
    /**
     * REQ-B5-2: 按类型查询 - 查询特定类型
     */
    @Test
    void shouldQueryElementsByType() throws Exception {
        // 先创建几个不同类型的元素
        createTestElement("RequirementDefinition", "REQ-TEST-001", "Test Req");
        createTestElement("PartUsage", "PART-001", "Test Part");
        createTestElement("RequirementDefinition", "REQ-TEST-002", "Another Req");
        
        // 查询所有RequirementDefinition
        mockMvc.perform(get("/api/v1/elements")
                .param("type", "RequirementDefinition"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[*].eClass", everyItem(equalTo("RequirementDefinition"))));
        
        // 查询所有PartUsage
        mockMvc.perform(get("/api/v1/elements")
                .param("type", "PartUsage"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[*].eClass", everyItem(equalTo("PartUsage"))));
    }
    
    /**
     * REQ-B5-2: 按类型查询 - type为空返回所有元素
     */
    @Test
    void shouldReturnAllElementsWhenTypeIsEmpty() throws Exception {
        // 创建不同类型的元素
        createTestElement("RequirementDefinition", "REQ-ALL-001", "Req");
        createTestElement("PartUsage", "PART-ALL-001", "Part");
        createTestElement("InterfaceDefinition", "INTF-ALL-001", "Interface");
        
        // 不带type参数，返回所有元素
        mockMvc.perform(get("/api/v1/elements"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[?(@.eClass == 'RequirementDefinition')]").exists())
            .andExpect(jsonPath("$[?(@.eClass == 'PartUsage')]").exists())
            .andExpect(jsonPath("$[?(@.eClass == 'InterfaceDefinition')]").exists());
    }
    
    /**
     * REQ-B5-2: 支持分页
     */
    @Test
    void shouldSupportPaginationInQuery() throws Exception {
        // 创建多个元素
        for (int i = 1; i <= 10; i++) {
            createTestElement("RequirementDefinition", "REQ-PAGE-" + i, "Req " + i);
        }
        
        // 请求第一页
        mockMvc.perform(get("/api/v1/elements")
                .param("type", "RequirementDefinition")
                .param("page", "0")
                .param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()").value(5))
            .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(10)))
            .andExpect(jsonPath("$.size").value(5))
            .andExpect(jsonPath("$.number").value(0));
    }
    
    /**
     * REQ-B5-3: 通用PATCH更新
     */
    @Test
    void shouldPatchUpdateAnyElement() throws Exception {
        // 先创建一个元素
        String elementId = createTestElement("PartUsage", "PART-PATCH", "Original Part");
        
        // PATCH更新
        Map<String, Object> patchData = new HashMap<>();
        patchData.put("declaredName", "Updated Part Name");
        patchData.put("documentation", "Updated documentation");
        
        mockMvc.perform(patch("/api/v1/elements/{id}", elementId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchData)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.declaredName").value("Updated Part Name"))
            .andExpect(jsonPath("$.documentation").value("Updated documentation"))
            .andExpect(jsonPath("$.declaredShortName").value("PART-PATCH")); // 未更新的字段保持不变
    }
    
    /**
     * REQ-B5-3: 元素不存在返回404
     */
    @Test
    void shouldReturn404ForNonExistentElement() throws Exception {
        Map<String, Object> patchData = new HashMap<>();
        patchData.put("declaredName", "Any Name");
        
        mockMvc.perform(patch("/api/v1/elements/{id}", "non-existent-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchData)))
            .andExpect(status().isNotFound());
    }
    
    /**
     * REQ-B5-4: 零代码扩展验证 - 支持新类型无需修改代码
     */
    @Test
    void shouldSupportNewTypesWithoutCodeChange() throws Exception {
        // 测试一些后端从未专门处理过的SysML类型
        String[] newTypes = {
            "ActionUsage",           // 动作
            "StateUsage",            // 状态
            "ConstraintUsage",       // 约束
            "AllocationUsage",       // 分配
            "PortUsage",             // 端口
            "ConnectionUsage",       // 连接
            "ItemUsage",             // 项目
            "AttributeUsage",        // 属性
            "MetadataUsage",         // 元数据
            "ViewUsage"              // 视图
        };
        
        for (String eClass : newTypes) {
            Map<String, Object> request = new HashMap<>();
            request.put("eClass", eClass);
            
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("declaredName", eClass + " Test");
            attributes.put("declaredShortName", eClass.substring(0, 3).toLowerCase());
            request.put("attributes", attributes);
            
            // 应该能创建任何有效的SysML类型
            MvcResult result = mockMvc.perform(post("/api/v1/elements")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eClass").value(eClass))
                .andExpect(jsonPath("$.declaredName").value(eClass + " Test"))
                .andReturn();
            
            System.out.println("✅ Successfully created " + eClass + " without specific code!");
        }
    }
    
    // 辅助方法：创建测试元素
    private String createTestElement(String eClass, String shortName, String name) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("eClass", eClass);
        
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("declaredShortName", shortName);
        attributes.put("declaredName", name);
        request.put("attributes", attributes);
        
        MvcResult result = mockMvc.perform(post("/api/v1/elements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();
        
        Map<String, Object> response = objectMapper.readValue(
            result.getResponse().getContentAsString(), 
            Map.class
        );
        
        return (String) response.get("elementId");
    }
}