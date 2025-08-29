package com.sysml.mvp.repository;

import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.emf.ecore.resource.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootTest
public class FileModelRepositoryTest {

    @Autowired
    private FileModelRepository repository;

    @Test
    void testLoadEmptyProject() {
        System.out.println("=== 测试加载空项目Resource初始化问题 ===");
        
        try {
            // 测试加载不存在的项目
            Resource resource = repository.loadProject("test-empty-resource");
            
            // 核心断言：Resource不能为null
            assertNotNull(resource, "❌ Resource不能为null");
            System.out.println("✅ Resource不为null: " + resource);
            
            // 核心断言：Resource.getURI()不能为null
            assertNotNull(resource.getURI(), "❌ Resource.getURI()不能为null");
            System.out.println("✅ Resource.getURI()不为null: " + resource.getURI());
            
            // 核心断言：Resource.getResourceSet()不能为null
            assertNotNull(resource.getResourceSet(), "❌ Resource.getResourceSet()不能为null");
            System.out.println("✅ Resource.getResourceSet()不为null");
            
            // 空项目应该包含0个元素
            assertEquals(0, resource.getContents().size(), "❌ 空项目应该包含0个元素");
            System.out.println("✅ 空项目元素数量正确: " + resource.getContents().size());
            
            System.out.println("🎉 空项目Resource初始化测试全部通过!");
            
        } catch (Exception e) {
            System.err.println("❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
            fail("loadProject失败: " + e.getMessage());
        }
    }

    @Test 
    void testLoadProjectWithExistingFile() {
        System.out.println("=== 测试加载现有项目文件 ===");
        
        try {
            // 测试加载default项目（应该存在JSON文件）
            Resource resource = repository.loadProject("default");
            
            // 同样的核心断言
            assertNotNull(resource, "❌ Resource不能为null");
            assertNotNull(resource.getURI(), "❌ Resource.getURI()不能为null");
            assertNotNull(resource.getResourceSet(), "❌ Resource.getResourceSet()不能为null");
            
            System.out.println("✅ 现有项目Resource初始化成功");
            System.out.println("   URI: " + resource.getURI());
            System.out.println("   元素数量: " + resource.getContents().size());
            
        } catch (Exception e) {
            System.err.println("❌ 加载现有项目失败: " + e.getMessage());
            e.printStackTrace();
            fail("loadProject(default)失败: " + e.getMessage());
        }
    }

    @Test
    void testResourceUriConsistency() {
        System.out.println("=== 测试Resource URI一致性 ===");
        
        try {
            // 连续加载相同项目，URI应该一致
            Resource resource1 = repository.loadProject("test-consistency");
            Resource resource2 = repository.loadProject("test-consistency");
            
            assertNotNull(resource1, "Resource1不能为null");
            assertNotNull(resource2, "Resource2不能为null");
            assertNotNull(resource1.getURI(), "Resource1.getURI()不能为null");
            assertNotNull(resource2.getURI(), "Resource2.getURI()不能为null");
            
            // URI应该一致
            assertEquals(resource1.getURI().toString(), resource2.getURI().toString(), 
                "相同项目的URI应该一致");
            
            System.out.println("✅ URI一致性测试通过");
            System.out.println("   URI: " + resource1.getURI());
            
        } catch (Exception e) {
            System.err.println("❌ URI一致性测试失败: " + e.getMessage());
            e.printStackTrace();
            fail("URI一致性测试失败: " + e.getMessage());
        }
    }
}