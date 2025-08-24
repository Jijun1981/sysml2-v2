package com.sysml.mvp.util;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.common.util.URI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sysml.mvp.model.EMFModelRegistry;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.*;

/**
 * Demo数据生成器
 * REQ-B1-4: 生成demo-project.json和small/medium/large数据集
 */
@Slf4j
@Component
public class DemoDataGenerator {
    
    @Autowired
    private EMFModelRegistry modelRegistry;
    
    /**
     * 生成Demo项目数据
     */
    public void generateDemoProject() {
        log.info("开始生成Demo项目数据...");
        
        try {
            // 创建ResourceSet
            ResourceSet resourceSet = new ResourceSetImpl();
            
            // 创建demo-project.json资源
            URI demoUri = URI.createFileURI("./data/demo-project.json");
            Resource demoResource = resourceSet.createResource(demoUri);
            
            // 生成数据（8个Definition + 5个Trace）
            generateProjectData(demoResource, "demo", 8, 5);
            
            // 保存
            demoResource.save(getJsonSaveOptions());
            log.info("Demo项目数据生成完成: {}", demoUri.toFileString());
            
        } catch (Exception e) {
            log.error("生成Demo项目数据失败", e);
            throw new RuntimeException("Failed to generate demo project data", e);
        }
    }
    
    /**
     * 生成三套规模化数据集
     */
    public void generateScaleDatasets() {
        log.info("开始生成规模化数据集...");
        
        try {
            ResourceSet resourceSet = new ResourceSetImpl();
            
            // Small数据集 (10个元素)
            generateDataset(resourceSet, "small", 8, 2);
            
            // Medium数据集 (100个元素) 
            generateDataset(resourceSet, "medium", 70, 30);
            
            // Large数据集 (500个元素)
            generateDataset(resourceSet, "large", 350, 150);
            
            log.info("规模化数据集生成完成");
            
        } catch (Exception e) {
            log.error("生成规模化数据集失败", e);
            throw new RuntimeException("Failed to generate scale datasets", e);
        }
    }
    
    private void generateDataset(ResourceSet resourceSet, String scale, int defCount, int traceCount) 
            throws IOException {
        URI uri = URI.createFileURI("./data/" + scale + "-project.json");
        Resource resource = resourceSet.createResource(uri);
        
        generateProjectData(resource, scale, defCount, traceCount);
        resource.save(getJsonSaveOptions());
        
        log.info("生成{}数据集: {} ({}个Definition, {}个Trace)", 
            scale, uri.toFileString(), defCount, traceCount);
    }
    
    /**
     * 生成项目数据内容
     */
    private void generateProjectData(Resource resource, String projectName, int defCount, int traceCount) {
        List<EObject> definitions = new ArrayList<>();
        List<EObject> usages = new ArrayList<>();
        
        // 生成RequirementDefinitions
        for (int i = 1; i <= defCount; i++) {
            EObject def = modelRegistry.createRequirementDefinition();
            setRequirementDefinitionData(def, projectName, i);
            definitions.add(def);
            resource.getContents().add(def);
            
            // 为每个Definition创建1个Usage
            EObject usage = modelRegistry.createRequirementUsage(getId(def));
            setRequirementUsageData(usage, projectName, i);
            usages.add(usage);
            resource.getContents().add(usage);
        }
        
        // 生成Traces
        for (int i = 1; i <= traceCount; i++) {
            String fromId = getId(definitions.get(i % definitions.size()));
            String toId = getId(definitions.get((i + 1) % definitions.size()));
            String traceType = getTraceType(i);
            
            EObject trace = modelRegistry.createTrace(fromId, toId, traceType);
            resource.getContents().add(trace);
        }
        
        log.debug("项目{}数据生成: {}个Definition, {}个Usage, {}个Trace", 
            projectName, definitions.size(), usages.size(), traceCount);
    }
    
    private void setRequirementDefinitionData(EObject def, String projectName, int index) {
        try {
            def.eSet(def.eClass().getEStructuralFeature("reqId"), 
                String.format("%s-REQ-%03d", projectName.toUpperCase(), index));
            def.eSet(def.eClass().getEStructuralFeature("name"), 
                String.format("%s系统需求 %d", getProjectDisplayName(projectName), index));
            def.eSet(def.eClass().getEStructuralFeature("text"), 
                generateRequirementText(projectName, index));
            def.eSet(def.eClass().getEStructuralFeature("doc"), 
                String.format("这是%s项目的第%d个需求定义，用于演示和测试。", projectName, index));
            
            // 设置tags
            List<String> tags = generateTags(index);
            def.eSet(def.eClass().getEStructuralFeature("tags"), tags);
            
        } catch (Exception e) {
            log.warn("设置RequirementDefinition数据失败: {}", e.getMessage());
        }
    }
    
    private void setRequirementUsageData(EObject usage, String projectName, int index) {
        try {
            usage.eSet(usage.eClass().getEStructuralFeature("name"), 
                String.format("%s需求实现 %d", getProjectDisplayName(projectName), index));
            usage.eSet(usage.eClass().getEStructuralFeature("text"), 
                String.format("基于%s-REQ-%03d的具体实现方案", projectName.toUpperCase(), index));
            usage.eSet(usage.eClass().getEStructuralFeature("status"), 
                getUsageStatus(index));
            
        } catch (Exception e) {
            log.warn("设置RequirementUsage数据失败: {}", e.getMessage());
        }
    }
    
    private String getId(EObject obj) {
        try {
            return (String) obj.eGet(obj.eClass().getEStructuralFeature("id"));
        } catch (Exception e) {
            return "unknown-id";
        }
    }
    
    private String getProjectDisplayName(String projectName) {
        switch (projectName.toLowerCase()) {
            case "demo": return "演示";
            case "small": return "小型";
            case "medium": return "中型";
            case "large": return "大型";
            default: return projectName;
        }
    }
    
    private String generateRequirementText(String projectName, int index) {
        String[] templates = {
            "系统应当能够%s，确保功能%d的正常运行。",
            "用户必须能够%s，满足业务需求%d。",
            "平台应当提供%s功能，支持场景%d的处理。",
            "系统需要实现%s机制，保障需求%d的执行。"
        };
        
        String[] functions = {
            "处理用户登录验证", "管理数据存储", "执行业务逻辑", "提供接口服务",
            "监控系统状态", "处理异常情况", "优化性能表现", "确保安全访问"
        };
        
        String template = templates[index % templates.length];
        String function = functions[index % functions.length];
        
        return String.format(template, function, index);
    }
    
    private List<String> generateTags(int index) {
        List<String> allTags = Arrays.asList(
            "核心功能", "用户体验", "安全", "性能", "集成", 
            "API", "数据库", "前端", "后端", "测试"
        );
        
        List<String> tags = new ArrayList<>();
        tags.add(allTags.get(index % allTags.size()));
        if (index % 3 == 0) {
            tags.add(allTags.get((index + 1) % allTags.size()));
        }
        
        return tags;
    }
    
    private String getTraceType(int index) {
        String[] types = {"derive", "satisfy", "refine", "trace"};
        return types[index % types.length];
    }
    
    private String getUsageStatus(int index) {
        String[] statuses = {"draft", "approved", "implemented", "verified"};
        return statuses[index % statuses.length];
    }
    
    private Map<String, Object> getJsonSaveOptions() {
        Map<String, Object> options = new HashMap<>();
        options.put("OPTION_FORCE_DEFAULT_REFERENCE_SERIALIZATION", true);
        return options;
    }
}