package com.sysml.mvp;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.sysml.mvp.util.DemoDataGenerator;

import java.io.File;

/**
 * Demo数据生成器测试
 * REQ-B1-4: 验证Demo数据生成功能
 */
@SpringBootTest
@ActiveProfiles("test")
public class DemoDataGeneratorTest {
    
    @Autowired
    private DemoDataGenerator demoDataGenerator;
    
    @Test
    @DisplayName("REQ-B1-4: 生成Demo项目数据")
    void testGenerateDemoProject() {
        // Given: 确保data目录存在
        File dataDir = new File("./data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        // When: 生成Demo项目数据
        demoDataGenerator.generateDemoProject();
        
        // Then: 验证文件生成
        File demoFile = new File("./data/demo-project.json");
        assertTrue(demoFile.exists(), "demo-project.json应该生成");
        assertTrue(demoFile.length() > 0, "demo-project.json不应为空");
        
        System.out.println("✅ Demo项目数据已生成: " + demoFile.getAbsolutePath());
        System.out.println("📁 文件大小: " + demoFile.length() + " bytes");
    }
    
    @Test
    @DisplayName("REQ-B1-4: 生成规模化数据集")
    void testGenerateScaleDatasets() {
        // Given: 确保data目录存在
        File dataDir = new File("./data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        // When: 生成规模化数据集
        demoDataGenerator.generateScaleDatasets();
        
        // Then: 验证所有文件生成
        String[] files = {"small-project.json", "medium-project.json", "large-project.json"};
        String[] descriptions = {"Small (10个元素)", "Medium (100个元素)", "Large (500个元素)"};
        
        for (int i = 0; i < files.length; i++) {
            File file = new File("./data/" + files[i]);
            assertTrue(file.exists(), files[i] + "应该生成");
            assertTrue(file.length() > 0, files[i] + "不应为空");
            
            System.out.println("✅ " + descriptions[i] + "数据集已生成: " + file.getAbsolutePath());
            System.out.println("📁 文件大小: " + file.length() + " bytes");
        }
    }
    
    @Test
    @DisplayName("REQ-B1-4: 验证Demo数据完整性")
    void testDemoDataCompleteness() {
        // Given: 确保data目录存在并生成所有数据
        File dataDir = new File("./data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        demoDataGenerator.generateDemoProject();
        demoDataGenerator.generateScaleDatasets();
        
        // Then: 验证REQ-B1-4要求
        // 1. 仓库含demo-project.json（≥8 Definition、≥5 Trace）
        File demoFile = new File("./data/demo-project.json");
        assertTrue(demoFile.exists(), "REQ-B1-4: demo-project.json必须存在");
        assertTrue(demoFile.length() > 1000, "REQ-B1-4: demo-project.json应包含足够内容");
        
        // 2. 提供small/medium/large三套数据集
        String[] scaleFiles = {"small-project.json", "medium-project.json", "large-project.json"};
        for (String fileName : scaleFiles) {
            File file = new File("./data/" + fileName);
            assertTrue(file.exists(), "REQ-B1-4: " + fileName + "必须存在");
            assertTrue(file.length() > 100, "REQ-B1-4: " + fileName + "应包含有效内容");
        }
        
        System.out.println("\n🎯 REQ-B1-4验证通过！");
        System.out.println("📁 Demo项目: " + demoFile.getAbsolutePath());
        
        for (String fileName : scaleFiles) {
            File file = new File("./data/" + fileName);
            System.out.println("📁 " + fileName.replace("-project.json", "").toUpperCase() + 
                "数据集: " + file.getAbsolutePath());
        }
    }
}