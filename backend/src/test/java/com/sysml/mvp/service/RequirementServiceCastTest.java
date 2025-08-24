package com.sysml.mvp.service;

import com.sysml.mvp.dto.RequirementDefinitionDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD测试：修复EDataTypeUniqueEList转换问题
 */
@SpringBootTest
@ActiveProfiles("test")
public class RequirementServiceCastTest {

    @Autowired
    private RequirementService requirementService;

    @Test
    void testConvertTagsToList() {
        // Arrange: 创建一个带tags的需求定义
        RequirementDefinitionDTO.CreateRequest request = RequirementDefinitionDTO.CreateRequest.builder()
                .type("definition")
                .reqId("CAST-TEST-001")
                .name("测试EList转换")
                .text("验证tags字段能正确转换")
                .tags(Arrays.asList("tag1", "tag2", "tag3"))
                .build();

        // Act: 创建需求并获取
        RequirementDefinitionDTO created = requirementService.createRequirement(request);
        
        // Assert: 验证tags被正确处理
        assertNotNull(created, "创建的需求不应为null");
        assertNotNull(created.getTags(), "tags不应为null");
        assertTrue(created.getTags() instanceof List, "tags应该是List类型");
        assertEquals(3, created.getTags().size(), "应该有3个tag");
        assertTrue(created.getTags().contains("tag1"), "应包含tag1");
        assertTrue(created.getTags().contains("tag2"), "应包含tag2");
        assertTrue(created.getTags().contains("tag3"), "应包含tag3");
    }
    
    @Test
    void testListRequirementsWithTags() {
        // Arrange: 确保有带tags的需求
        RequirementDefinitionDTO.CreateRequest request = RequirementDefinitionDTO.CreateRequest.builder()
                .type("definition")
                .reqId("LIST-TEST-001")
                .name("列表测试需求")
                .tags(Arrays.asList("test", "list"))
                .build();
        requirementService.createRequirement(request);

        // Act: 列出所有需求
        List<RequirementDefinitionDTO> requirements = requirementService.listRequirements();

        // Assert: 验证列表操作不会抛出ClassCastException
        assertNotNull(requirements, "需求列表不应为null");
        assertFalse(requirements.isEmpty(), "需求列表不应为空");
        
        // 找到刚创建的需求
        RequirementDefinitionDTO found = requirements.stream()
                .filter(r -> "LIST-TEST-001".equals(r.getReqId()))
                .findFirst()
                .orElse(null);
        
        assertNotNull(found, "应该找到刚创建的需求");
        assertNotNull(found.getTags(), "tags不应为null");
        assertTrue(found.getTags() instanceof List, "tags应该是List类型");
    }
    
    @Test
    void testEmptyTagsList() {
        // Arrange: 创建一个没有tags的需求定义
        RequirementDefinitionDTO.CreateRequest request = RequirementDefinitionDTO.CreateRequest.builder()
                .type("definition")
                .reqId("EMPTY-TAGS-001")
                .name("无标签需求")
                .text("这个需求没有标签")
                .build();

        // Act: 创建需求
        RequirementDefinitionDTO created = requirementService.createRequirement(request);
        
        // Assert: 验证空tags被正确处理
        assertNotNull(created, "创建的需求不应为null");
        assertNotNull(created.getTags(), "tags不应为null，应该是空列表");
        assertTrue(created.getTags().isEmpty(), "tags应该是空列表");
    }
}