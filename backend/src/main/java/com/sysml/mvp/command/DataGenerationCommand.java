package com.sysml.mvp.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.sysml.mvp.util.DemoDataGenerator;

import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * 数据生成命令
 * REQ-B1-4: 支持生成Demo数据和测试数据集
 */
@Slf4j
@ShellComponent
public class DataGenerationCommand {
    
    @Autowired
    private DemoDataGenerator demoDataGenerator;
    
    @ShellMethod(value = "生成Demo项目数据", key = "generate-demo")
    public String generateDemo() {
        try {
            // 确保data目录存在
            File dataDir = new File("./data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
                log.info("创建数据目录: {}", dataDir.getAbsolutePath());
            }
            
            // 生成Demo数据
            demoDataGenerator.generateDemoProject();
            
            return "✅ Demo项目数据生成完成！位置: ./data/demo-project.json";
            
        } catch (Exception e) {
            log.error("生成Demo数据失败", e);
            return "❌ Demo数据生成失败: " + e.getMessage();
        }
    }
    
    @ShellMethod(value = "生成规模化测试数据集", key = "generate-scale")
    public String generateScaleDatasets() {
        try {
            // 确保data目录存在
            File dataDir = new File("./data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
                log.info("创建数据目录: {}", dataDir.getAbsolutePath());
            }
            
            // 生成规模化数据集
            demoDataGenerator.generateScaleDatasets();
            
            StringBuilder result = new StringBuilder();
            result.append("✅ 规模化数据集生成完成！\n");
            result.append("📁 Small数据集: ./data/small-project.json (10个元素)\n");
            result.append("📁 Medium数据集: ./data/medium-project.json (100个元素)\n");
            result.append("📁 Large数据集: ./data/large-project.json (500个元素)");
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("生成规模化数据集失败", e);
            return "❌ 规模化数据集生成失败: " + e.getMessage();
        }
    }
    
    @ShellMethod(value = "生成所有Demo数据", key = "generate-all")
    public String generateAllData() {
        try {
            // 确保data目录存在
            File dataDir = new File("./data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
                log.info("创建数据目录: {}", dataDir.getAbsolutePath());
            }
            
            // 生成所有数据
            demoDataGenerator.generateDemoProject();
            demoDataGenerator.generateScaleDatasets();
            
            StringBuilder result = new StringBuilder();
            result.append("✅ 所有Demo数据生成完成！\n");
            result.append("📁 Demo项目: ./data/demo-project.json\n");
            result.append("📁 Small数据集: ./data/small-project.json (10个元素)\n");
            result.append("📁 Medium数据集: ./data/medium-project.json (100个元素)\n");
            result.append("📁 Large数据集: ./data/large-project.json (500个元素)\n");
            result.append("\n🎯 REQ-B1-4 Demo数据需求已完成！");
            
            return result.toString();
            
        } catch (Exception e) {
            log.error("生成所有Demo数据失败", e);
            return "❌ 生成所有Demo数据失败: " + e.getMessage();
        }
    }
    
    @ShellMethod(value = "验证Demo数据文件", key = "verify-demo")
    public String verifyDemoData() {
        StringBuilder result = new StringBuilder();
        result.append("📋 Demo数据验证结果:\n");
        
        // 检查各个数据文件
        String[] files = {
            "./data/demo-project.json",
            "./data/small-project.json", 
            "./data/medium-project.json",
            "./data/large-project.json"
        };
        
        String[] descriptions = {
            "Demo项目 (≥8 Definition, ≥5 Trace)",
            "Small数据集 (10个元素)",
            "Medium数据集 (100个元素)", 
            "Large数据集 (500个元素)"
        };
        
        boolean allExist = true;
        
        for (int i = 0; i < files.length; i++) {
            File file = new File(files[i]);
            if (file.exists() && file.length() > 0) {
                result.append("✅ ").append(descriptions[i]).append(": ").append(files[i]).append("\n");
            } else {
                result.append("❌ ").append(descriptions[i]).append(": 文件不存在或为空\n");
                allExist = false;
            }
        }
        
        if (allExist) {
            result.append("\n🎯 REQ-B1-4验证通过！所有Demo数据文件已就绪。");
        } else {
            result.append("\n⚠️  部分Demo数据文件缺失，请运行 'generate-all' 命令生成。");
        }
        
        return result.toString();
    }
}