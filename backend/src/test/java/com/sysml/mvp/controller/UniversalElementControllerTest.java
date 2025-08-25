package com.sysml.mvp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sysml.mvp.service.UniversalElementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 通用元素接口测试 - 基于mvp接口文档.md
 * 验证REQ-B5-1到REQ-B5-4的实现
 */
@WebMvcTest(UniversalElementController.class)
public class UniversalElementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UniversalElementService universalElementService;

    @Autowired
    private ObjectMapper objectMapper;

    private Map<String, Object> testElement;

    @BeforeEach
    void setUp() {
        testElement = new HashMap<>();
        testElement.put("eClass", "RequirementDefinition");
        testElement.put("elementId", "requirementdefinition-001");
        testElement.put("declaredName", "Test Requirement");
        testElement.put("declaredShortName", "REQ-001");
        testElement.put("isLibraryElement", false);
        testElement.put("isVariation", false);
        testElement.put("isIndividual", false);
        testElement.put("isSufficient", false);
        testElement.put("isAbstract", false);
    }

    @Test
    void testQueryElements_ShouldReturnArray() throws Exception {
        // 测试：GET /api/v1/elements?projectId=test 应该直接返回数组
        List<Map<String, Object>> elements = Arrays.asList(testElement);
        when(universalElementService.queryElements(eq("test"), any(), any())).thenReturn(elements);

        mockMvc.perform(get("/api/v1/elements")
                .param("projectId", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", isA(List.class)))
                .andExpect(jsonPath("$[0].eClass").value("RequirementDefinition"))
                .andExpect(jsonPath("$[0].elementId").value("requirementdefinition-001"));
    }

    @Test
    void testQueryElementsByType() throws Exception {
        // 测试：GET /api/v1/elements?projectId=test&type=RequirementDefinition
        List<Map<String, Object>> elements = Arrays.asList(testElement);
        when(universalElementService.queryElements(eq("test"), eq("RequirementDefinition"), any()))
                .thenReturn(elements);

        mockMvc.perform(get("/api/v1/elements")
                .param("projectId", "test")
                .param("type", "RequirementDefinition"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].eClass").value("RequirementDefinition"));
    }

    @Test
    void testCreateElement() throws Exception {
        // 测试：POST /api/v1/elements?projectId=test
        // 请求体格式应该是平铺的，不是嵌套的attributes
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("eClass", "PartUsage");
        requestBody.put("declaredName", "Engine");
        requestBody.put("declaredShortName", "eng");

        Map<String, Object> createdElement = new HashMap<>(requestBody);
        createdElement.put("elementId", "partusage-001");
        createdElement.put("isLibraryElement", false);
        createdElement.put("isDerived", false);

        when(universalElementService.createElement(eq("test"), any())).thenReturn(createdElement);

        mockMvc.perform(post("/api/v1/elements")
                .param("projectId", "test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eClass").value("PartUsage"))
                .andExpect(jsonPath("$.elementId").exists())
                .andExpect(jsonPath("$.declaredName").value("Engine"));
    }

    @Test
    void testGetElementById() throws Exception {
        // 测试：GET /api/v1/elements/{id}?projectId=test
        when(universalElementService.getElementById(eq("test"), eq("requirementdefinition-001")))
                .thenReturn(testElement);

        mockMvc.perform(get("/api/v1/elements/requirementdefinition-001")
                .param("projectId", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eClass").value("RequirementDefinition"))
                .andExpect(jsonPath("$.elementId").value("requirementdefinition-001"));
    }

    @Test
    void testPatchElement() throws Exception {
        // 测试：PATCH /api/v1/elements/{id}?projectId=test
        Map<String, Object> updates = new HashMap<>();
        updates.put("declaredName", "Updated Name");
        updates.put("documentation", "Updated documentation");

        Map<String, Object> updatedElement = new HashMap<>(testElement);
        updatedElement.put("declaredName", "Updated Name");
        updatedElement.put("documentation", "Updated documentation");

        when(universalElementService.updateElement(eq("test"), eq("requirementdefinition-001"), any()))
                .thenReturn(updatedElement);

        mockMvc.perform(patch("/api/v1/elements/requirementdefinition-001")
                .param("projectId", "test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.declaredName").value("Updated Name"));
    }

    @Test
    void testDeleteElement() throws Exception {
        // 测试：DELETE /api/v1/elements/{id}?projectId=test
        mockMvc.perform(delete("/api/v1/elements/requirementdefinition-001")
                .param("projectId", "test"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testHandleEMFListFields() throws Exception {
        // 测试：确保EMF List字段被正确处理
        Map<String, Object> elementWithList = new HashMap<>(testElement);
        // documentation应该是List<Map>，不是String
        elementWithList.put("documentation", Arrays.asList(
            Map.of("body", "Test documentation")
        ));

        List<Map<String, Object>> elements = Arrays.asList(elementWithList);
        when(universalElementService.queryElements(eq("test"), any(), any())).thenReturn(elements);

        mockMvc.perform(get("/api/v1/elements")
                .param("projectId", "test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentation", isA(List.class)))
                .andExpect(jsonPath("$[0].documentation[0].body").value("Test documentation"));
    }

    @Test
    void testZeroCodeExtension() throws Exception {
        // 测试REQ-B5-4：零代码扩展 - 创建任意SysML类型
        Map<String, Object> interfaceUsage = new HashMap<>();
        interfaceUsage.put("eClass", "InterfaceUsage");
        interfaceUsage.put("declaredName", "PowerInterface");
        interfaceUsage.put("declaredShortName", "pwr-if");

        Map<String, Object> created = new HashMap<>(interfaceUsage);
        created.put("elementId", "interfaceusage-001");

        when(universalElementService.createElement(eq("test"), any())).thenReturn(created);

        mockMvc.perform(post("/api/v1/elements")
                .param("projectId", "test")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(interfaceUsage)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eClass").value("InterfaceUsage"))
                .andExpect(jsonPath("$.elementId").exists());
    }
}