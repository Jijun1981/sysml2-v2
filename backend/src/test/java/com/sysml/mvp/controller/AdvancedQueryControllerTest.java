package com.sysml.mvp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sysml.mvp.service.UniversalElementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 高级查询功能控制器测试 - 第四阶段数据增强层
 * 
 * 测试覆盖：
 * - 分页支持：page从0开始，size∈(1..200]，默认50
 * - 排序支持：sort参数，支持多字段排序
 * - 过滤支持：filter参数，支持字段过滤
 * - 全文搜索：search参数
 */
@WebMvcTest(AdvancedQueryController.class)
@DisplayName("高级查询功能控制器测试 - 第四阶段")
public class AdvancedQueryControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private UniversalElementService universalElementService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Test
    @DisplayName("【通用约定】分页查询 - 默认参数")
    void testPaginationQuery_DefaultParameters() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/elements/advanced"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists())
                .andExpect(jsonPath("$.content").isArray());
    }
    
    @Test
    @DisplayName("【通用约定】分页查询 - 自定义page和size")
    void testPaginationQuery_CustomParameters() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/elements/advanced")
                .param("page", "2")
                .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(20));
    }
    
    @Test
    @DisplayName("【通用约定】分页查询 - 边界验证")
    void testPaginationQuery_BoundaryValidation() throws Exception {
        // Then - size=0应该返回400
        mockMvc.perform(get("/api/v1/elements/advanced")
                .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("size must be between 1 and 200"));
        
        // Then - size=201应该返回400
        mockMvc.perform(get("/api/v1/elements/advanced")
                .param("size", "201"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("size must be between 1 and 200"));
        
        // Then - page=-1应该返回400
        mockMvc.perform(get("/api/v1/elements/advanced")
                .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("page must be >= 0"));
    }
    
    @Test
    @DisplayName("【第四阶段】排序功能 - 单字段排序")
    void testSortQuery_SingleField() throws Exception {
        // When & Then - 按declaredName升序
        mockMvc.perform(get("/api/v1/elements/advanced")
                .param("sort", "declaredName,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sort").exists())
                .andExpect(jsonPath("$.sort.declaredName").value("asc"));
        
        // When & Then - 按createdAt降序
        mockMvc.perform(get("/api/v1/elements/advanced")
                .param("sort", "createdAt,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sort.createdAt").value("desc"));
    }
    
    @Test
    @DisplayName("【第四阶段】排序功能 - 多字段排序")
    void testSortQuery_MultipleFields() throws Exception {
        // When & Then - 先按eClass升序，再按declaredName降序
        mockMvc.perform(get("/api/v1/elements/advanced")
                .param("sort", "eClass,asc")
                .param("sort", "declaredName,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sort").exists())
                .andExpect(jsonPath("$.sort.eClass").value("asc"))
                .andExpect(jsonPath("$.sort.declaredName").value("desc"));
    }
    
    @Test
    @DisplayName("【第四阶段】排序功能 - 无效字段验证")
    void testSortQuery_InvalidField() throws Exception {
        // When & Then - 无效字段应该返回400
        mockMvc.perform(get("/api/v1/elements/advanced")
                .param("sort", "invalidField,asc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid sort field: invalidField"));
    }
    
    @Test
    @DisplayName("【第四阶段】过滤功能 - 按eClass过滤")
    void testFilterQuery_ByEClass() throws Exception {
        // When & Then - 只查询RequirementDefinition
        mockMvc.perform(get("/api/v1/elements/advanced")
                .param("filter", "eClass:RequirementDefinition"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filter.eClass").value("RequirementDefinition"));
        
        // When & Then - 只查询RequirementUsage
        mockMvc.perform(get("/api/v1/elements/advanced")
                .param("filter", "eClass:RequirementUsage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filter.eClass").value("RequirementUsage"));
    }
    
    @Test
    @DisplayName("【第四阶段】过滤功能 - 按状态过滤")
    void testFilterQuery_ByStatus() throws Exception {
        // When & Then - 只查询已批准的需求
        mockMvc.perform(get("/api/v1/elements/advanced")
                .param("filter", "status:approved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filter.status").value("approved"));
    }
    
    @Test
    @DisplayName("【第四阶段】过滤功能 - 多条件过滤")
    void testFilterQuery_MultipleConditions() throws Exception {
        // When & Then - 查询已批准的RequirementUsage
        mockMvc.perform(get("/api/v1/elements/advanced")
                .param("filter", "eClass:RequirementUsage")
                .param("filter", "status:approved"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filter.eClass").value("RequirementUsage"))
                .andExpect(jsonPath("$.filter.status").value("approved"));
    }
    
    @Test
    @DisplayName("【第四阶段】全文搜索功能")
    void testSearchQuery_FullText() throws Exception {
        // When & Then - 搜索包含"电池"的需求
        mockMvc.perform(get("/api/v1/elements/advanced")
                .param("search", "电池"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.search").value("电池"));
        
        // When & Then - 搜索包含"安全"的需求
        mockMvc.perform(get("/api/v1/elements/advanced")
                .param("search", "安全"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.search").value("安全"));
    }
    
    @Test
    @DisplayName("【第四阶段】组合查询功能 - 分页+排序+过滤+搜索")
    void testCombinedQuery_AllFeatures() throws Exception {
        // When & Then - 综合查询：搜索"电池"，过滤RequirementDefinition，按名称排序，第1页每页10个
        mockMvc.perform(get("/api/v1/elements/advanced")
                .param("page", "1")
                .param("size", "10")
                .param("sort", "declaredName,asc")
                .param("filter", "eClass:RequirementDefinition")
                .param("search", "电池"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.sort.declaredName").value("asc"))
                .andExpect(jsonPath("$.filter.eClass").value("RequirementDefinition"))
                .andExpect(jsonPath("$.search").value("电池"))
                .andExpect(jsonPath("$.content").isArray());
    }
    
    @Test
    @DisplayName("【第四阶段】性能要求验证 - 查询响应时间<500ms")
    void testQueryPerformance_ResponseTime() throws Exception {
        // Given - 记录开始时间
        long startTime = System.currentTimeMillis();
        
        // When - 执行复杂查询
        mockMvc.perform(get("/api/v1/elements/advanced")
                .param("page", "0")
                .param("size", "50")
                .param("sort", "createdAt,desc")
                .param("filter", "eClass:RequirementDefinition")
                .param("search", "系统"))
                .andExpect(status().isOk());
        
        // Then - 验证响应时间<500ms
        long endTime = System.currentTimeMillis();
        long responseTime = endTime - startTime;
        
        // 注意：这个测试在mock环境下总是很快，实际性能测试需要在集成测试中进行
        // 这里主要验证API结构正确
        assert responseTime < 500;
    }
    
    @Test
    @DisplayName("【通用约定】错误处理 - 格式错误")
    void testErrorHandling_InvalidFormat() throws Exception {
        // When & Then - 无效的sort格式
        mockMvc.perform(get("/api/v1/elements/advanced")
                .param("sort", "invalid-format"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid sort format. Use: field,direction"));
        
        // When & Then - 无效的filter格式
        mockMvc.perform(get("/api/v1/elements/advanced")
                .param("filter", "invalid-format"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid filter format. Use: field:value"));
    }
    
    @Test
    @DisplayName("【通用约定】响应格式标准化")
    void testResponseFormat_Standardized() throws Exception {
        // When & Then - 验证标准分页响应格式
        mockMvc.perform(get("/api/v1/elements/advanced"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.page").isNumber())
                .andExpect(jsonPath("$.size").isNumber())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.totalPages").isNumber())
                .andExpect(jsonPath("$.first").isBoolean())
                .andExpect(jsonPath("$.last").isBoolean());
    }
}