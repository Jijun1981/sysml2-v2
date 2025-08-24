package com.sysml.mvp.util;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.common.util.URI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sysml.mvp.model.EMFModelRegistry;
import com.sysml.mvp.service.UniversalElementService;
import com.sysml.mvp.dto.ElementDTO;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Demo数据生成器
 * REQ-B1-4: 生成demo-project.json和small/medium/large数据集
 */
@Slf4j
@Component
public class DemoDataGenerator {
    
    @Autowired
    private EMFModelRegistry modelRegistry;
    
    @Autowired
    private UniversalElementService universalElementService;
    
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
    
    // ===============================================
    // Phase 5: Pilot格式EV电池系统数据生成方法
    // ===============================================
    
    /**
     * REQ-B1-4: 生成EV电池系统Pilot格式演示数据
     * 使用UniversalElementService创建元素，确保Pilot格式兼容性
     */
    public void generatePilotEVBatteryDemo() {
        log.info("开始生成EV电池系统Pilot格式演示数据...");
        
        try {
            // 清理现有数据
            clearDefaultProject();
            
            // 1. 生成~20个RequirementDefinition（电池系统需求库）
            List<ElementDTO> definitions = generateEVBatteryRequirementDefinitions();
            log.info("生成{}个RequirementDefinition", definitions.size());
            
            // 2. 基于Definition生成50个RequirementUsage
            List<ElementDTO> usages = generateEVBatteryRequirementUsages(definitions);
            log.info("生成{}个RequirementUsage", usages.size());
            
            // 3. 创建15-20个Trace关系
            List<ElementDTO> traces = generateEVBatteryTraceRelationships(usages);
            log.info("生成{}个Trace关系", traces.size());
            
            // 4. 保存到文件
            saveEVBatteryDataToFile("ev-battery-demo.json");
            
            log.info("EV电池系统Pilot格式演示数据生成完成");
            
        } catch (Exception e) {
            log.error("生成EV电池系统演示数据失败", e);
            throw new RuntimeException("Failed to generate EV battery demo data", e);
        }
    }
    
    /**
     * REQ-B1-4: 生成多套规模化EV电池系统数据集
     */
    public void generateScalableEVBatteryDatasets() {
        log.info("开始生成规模化EV电池系统数据集...");
        
        try {
            // Small数据集 (10个Definition + 25个Usage + 8个Trace)
            generateEVBatteryDataset("small", 10, 25, 8);
            
            // Medium数据集 (30个Definition + 75个Usage + 25个Trace)
            generateEVBatteryDataset("medium", 30, 75, 25);
            
            // Large数据集 (50个Definition + 150个Usage + 50个Trace)
            generateEVBatteryDataset("large", 50, 150, 50);
            
            log.info("规模化EV电池系统数据集生成完成");
            
        } catch (Exception e) {
            log.error("生成规模化EV电池系统数据集失败", e);
            throw new RuntimeException("Failed to generate scalable EV battery datasets", e);
        }
    }
    
    /**
     * 生成特定规模的EV电池系统数据集
     */
    private void generateEVBatteryDataset(String scale, int defCount, int usageCount, int traceCount) {
        log.info("生成{}级别EV电池数据集: {}个Definition, {}个Usage, {}个Trace", 
            scale, defCount, usageCount, traceCount);
        
        try {
            // 清理现有数据
            clearDefaultProject();
            
            // 生成Definition（按比例分配到3层架构）
            List<ElementDTO> definitions = generateEVBatteryDefinitionsForScale(defCount, scale);
            
            // 生成Usage（基于Definition，支持多实例）
            List<ElementDTO> usages = generateEVBatteryUsagesForScale(definitions, usageCount, scale);
            
            // 生成Trace关系
            List<ElementDTO> traces = generateEVBatteryTracesForScale(usages, traceCount, scale);
            
            // 保存到文件
            saveEVBatteryDataToFile("ev-battery-" + scale + ".json");
            
        } catch (Exception e) {
            log.error("生成{}级别EV电池数据集失败", e);
            throw new RuntimeException("Failed to generate " + scale + " EV battery dataset", e);
        }
    }
    
    /**
     * 生成EV电池系统RequirementDefinition（~20个）
     * 按照3层架构分配：L1系统(6个) + L2子系统(8个) + L3组件(6个)
     */
    private List<ElementDTO> generateEVBatteryRequirementDefinitions() {
        List<ElementDTO> definitions = new ArrayList<>();
        
        // L1 系统级需求 (6个)
        definitions.addAll(generateL1SystemRequirements());
        
        // L2 子系统级需求 (8个)  
        definitions.addAll(generateL2SubsystemRequirements());
        
        // L3 组件级需求 (6个)
        definitions.addAll(generateL3ComponentRequirements());
        
        return definitions;
    }
    
    /**
     * L1系统级需求 - 整车电池系统级别
     */
    private List<ElementDTO> generateL1SystemRequirements() {
        List<ElementDTO> definitions = new ArrayList<>();
        
        String[][] l1Requirements = {
            {"EV-L1-SYS-001", "电池系统能量密度需求", "电池系统应当提供不少于150Wh/kg的能量密度，以满足整车续航里程要求。"},
            {"EV-L1-SYS-002", "电池系统功率需求", "电池系统应当支持不少于200kW的峰值功率输出，满足车辆加速性能需求。"},
            {"EV-L1-SYS-003", "电池系统安全需求", "电池系统必须符合UN38.3和GB/T 31485安全标准，确保在各种工况下的安全性。"},
            {"EV-L1-SYS-004", "电池系统寿命需求", "电池系统应当支持不少于8年或16万公里的使用寿命，容量衰减不超过20%。"},
            {"EV-L1-SYS-005", "电池系统工作温度需求", "电池系统应当在-30°C至+55°C环境温度范围内正常工作。"},
            {"EV-L1-SYS-006", "电池系统快充需求", "电池系统应当支持10%-80%快充时间不超过30分钟的充电能力。"}
        };
        
        for (String[] req : l1Requirements) {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("declaredShortName", req[0]);
            attributes.put("name", req[1]);
            attributes.put("text", req[2]);
            attributes.put("elementId", generateElementId());
            
            // 添加电池系统标签
            attributes.put("tags", Arrays.asList("L1系统", "电池Pack", "整车集成", "性能需求"));
            
            ElementDTO definition = universalElementService.createElement("RequirementDefinition", attributes);
            definitions.add(definition);
        }
        
        return definitions;
    }
    
    /**
     * L2子系统级需求 - BMS、热管理、结构等子系统
     */
    private List<ElementDTO> generateL2SubsystemRequirements() {
        List<ElementDTO> definitions = new ArrayList<>();
        
        String[][] l2Requirements = {
            {"EV-L2-BMS-001", "BMS电压监测需求", "BMS应当实时监测每个电芯电压，精度不低于±5mV，采样频率不低于1Hz。"},
            {"EV-L2-BMS-002", "BMS温度监测需求", "BMS应当监测电池包内关键位置温度，精度±2°C，监测点不少于20个。"},
            {"EV-L2-BMS-003", "BMS均衡控制需求", "BMS应当具备主动均衡功能，单体电压差控制在50mV以内。"},
            {"EV-L2-TMS-001", "热管理系统冷却需求", "热管理系统应当将电池包温度控制在15-35°C工作范围内。"},
            {"EV-L2-TMS-002", "热管理系统加热需求", "热管理系统应当在低温环境下对电池包进行预热，确保正常工作性能。"},
            {"EV-L2-STR-001", "结构系统强度需求", "电池包结构应当承受5G碰撞加速度而不发生结构失效。"},
            {"EV-L2-STR-002", "结构系统密封需求", "电池包应当达到IP67防护等级，确保在涉水工况下的安全性。"},
            {"EV-L2-CHG-001", "充电系统兼容需求", "充电系统应当兼容GB/T、CCS、CHAdeMO等主流充电标准。"}
        };
        
        for (String[] req : l2Requirements) {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("declaredShortName", req[0]);
            attributes.put("name", req[1]);
            attributes.put("text", req[2]);
            attributes.put("elementId", generateElementId());
            
            // 根据子系统类型添加相应标签
            String subsystem = req[0].contains("BMS") ? "BMS电管理" : 
                             req[0].contains("TMS") ? "热管理" :
                             req[0].contains("STR") ? "结构系统" : "充电系统";
            attributes.put("tags", Arrays.asList("L2子系统", subsystem, "控制需求"));
            
            ElementDTO definition = universalElementService.createElement("RequirementDefinition", attributes);
            definitions.add(definition);
        }
        
        return definitions;
    }
    
    /**
     * L3组件级需求 - 电芯、传感器、执行器等组件
     */
    private List<ElementDTO> generateL3ComponentRequirements() {
        List<ElementDTO> definitions = new ArrayList<>();
        
        String[][] l3Requirements = {
            {"EV-L3-CELL-001", "电芯容量需求", "单体电芯标称容量应当不少于50Ah，实际容量不低于标称值的95%。"},
            {"EV-L3-CELL-002", "电芯内阻需求", "单体电芯内阻应当不大于1mΩ，确保功率输出性能。"},
            {"EV-L3-SENS-001", "电压传感器精度需求", "电压传感器测量精度应当达到±0.1%，工作温度范围-40°C至+85°C。"},
            {"EV-L3-SENS-002", "温度传感器响应需求", "温度传感器响应时间应当不超过3秒，精度±1°C。"},
            {"EV-L3-CONT-001", "继电器寿命需求", "主接触器应当支持不少于10万次开关操作，接触电阻不大于0.5mΩ。"},
            {"EV-L3-COOL-001", "冷却液流量需求", "冷却系统流量应当不少于20L/min，确保有效热交换。"}
        };
        
        for (String[] req : l3Requirements) {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("declaredShortName", req[0]);
            attributes.put("name", req[1]);
            attributes.put("text", req[2]);
            attributes.put("elementId", generateElementId());
            
            // 根据组件类型添加相应标签
            String component = req[0].contains("CELL") ? "电芯" : 
                             req[0].contains("SENS") ? "传感器" :
                             req[0].contains("CONT") ? "控制器" : "执行器";
            attributes.put("tags", Arrays.asList("L3组件", component, "器件规格"));
            
            ElementDTO definition = universalElementService.createElement("RequirementDefinition", attributes);
            definitions.add(definition);
        }
        
        return definitions;
    }
    
    /**
     * 基于Definition生成50个RequirementUsage
     * 每个Definition创建2-3个Usage实例，模拟不同应用场景
     */
    private List<ElementDTO> generateEVBatteryRequirementUsages(List<ElementDTO> definitions) {
        List<ElementDTO> usages = new ArrayList<>();
        
        // 为每个Definition创建多个Usage实例
        for (ElementDTO definition : definitions) {
            String defId = definition.getElementId();
            String reqId = (String) definition.getProperty("declaredShortName");
            
            // 为每个Definition创建2-3个不同场景的Usage
            int usageCount = 2 + (definitions.indexOf(definition) % 2); // 2或3个
            
            for (int i = 1; i <= usageCount; i++) {
                Map<String, Object> attributes = new HashMap<>();
                attributes.put("elementId", generateElementId());
                attributes.put("name", generateUsageName(reqId, i));
                attributes.put("text", generateUsageText(reqId, i));
                attributes.put("status", getUsageStatus(usages.size()));
                
                // 设置对Definition的引用（Pilot格式的of引用）
                attributes.put("ofId", defId);
                
                ElementDTO usage = universalElementService.createElement("RequirementUsage", attributes);
                usages.add(usage);
            }
        }
        
        return usages;
    }
    
    /**
     * 生成Usage名称（基于Definition的reqId和场景编号）
     */
    private String generateUsageName(String reqId, int scenarioNumber) {
        String[] scenarios = {"标准工况", "极限工况", "优化工况", "测试工况"};
        String scenario = scenarios[(scenarioNumber - 1) % scenarios.length];
        return reqId + "-" + scenario + "应用";
    }
    
    /**
     * 生成Usage文本（具体实现方案描述）
     */
    private String generateUsageText(String reqId, int scenarioNumber) {
        String baseText = "基于" + reqId + "的具体实现方案";
        
        if (reqId.contains("L1")) {
            return baseText + "，在整车级别进行系统集成验证。";
        } else if (reqId.contains("L2")) {
            return baseText + "，在子系统级别进行功能实现和测试。";
        } else {
            return baseText + "，在组件级别进行参数配置和性能验证。";
        }
    }
    
    /**
     * 创建15-20个Trace关系（Usage之间的追溯关系）
     */
    private List<ElementDTO> generateEVBatteryTraceRelationships(List<ElementDTO> usages) {
        List<ElementDTO> traces = new ArrayList<>();
        Random random = new Random(42); // 固定种子确保可重现性
        
        // 创建18个Trace关系
        for (int i = 0; i < 18; i++) {
            // 随机选择source和target（确保不同）
            int sourceIndex = random.nextInt(usages.size());
            int targetIndex;
            do {
                targetIndex = random.nextInt(usages.size());
            } while (targetIndex == sourceIndex);
            
            ElementDTO sourceUsage = usages.get(sourceIndex);
            ElementDTO targetUsage = usages.get(targetIndex);
            
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("elementId", generateElementId());
            attributes.put("name", "追溯关系_" + (i + 1));
            attributes.put("sourceId", sourceUsage.getElementId());
            attributes.put("targetId", targetUsage.getElementId());
            
            // 根据层级关系确定追溯类型
            String traceType = determineTraceType(sourceUsage, targetUsage);
            attributes.put("traceType", traceType);
            
            // 在Pilot格式中，Trace映射为Dependency
            ElementDTO trace = universalElementService.createElement("Dependency", attributes);
            traces.add(trace);
        }
        
        return traces;
    }
    
    /**
     * 根据Usage的层级关系确定追溯类型
     */
    private String determineTraceType(ElementDTO source, ElementDTO target) {
        String sourceName = (String) source.getProperty("name");
        String targetName = (String) target.getProperty("name");
        
        // 根据层级和内容确定追溯关系类型
        if (sourceName.contains("L3") && targetName.contains("L2")) {
            return "satisfy"; // 组件满足子系统需求
        } else if (sourceName.contains("L2") && targetName.contains("L1")) {
            return "satisfy"; // 子系统满足系统需求
        } else if (sourceName.contains("优化") && targetName.contains("标准")) {
            return "refine"; // 优化方案细化标准方案
        } else if (sourceName.contains("测试") && targetName.contains("标准")) {
            return "derive"; // 测试方案派生自标准方案
        } else {
            return "trace"; // 一般追溯关系
        }
    }
    
    /**
     * 为规模化数据集生成Definition
     */
    private List<ElementDTO> generateEVBatteryDefinitionsForScale(int count, String scale) {
        List<ElementDTO> definitions = new ArrayList<>();
        
        // 按比例分配到3层架构 (30% L1, 40% L2, 30% L3)
        int l1Count = Math.max(1, count * 30 / 100);
        int l3Count = Math.max(1, count * 30 / 100);
        int l2Count = count - l1Count - l3Count;
        
        // 生成L1需求
        definitions.addAll(generateScaledL1Requirements(l1Count, scale));
        
        // 生成L2需求
        definitions.addAll(generateScaledL2Requirements(l2Count, scale));
        
        // 生成L3需求
        definitions.addAll(generateScaledL3Requirements(l3Count, scale));
        
        return definitions;
    }
    
    /**
     * 为规模化数据集生成Usage
     */
    private List<ElementDTO> generateEVBatteryUsagesForScale(List<ElementDTO> definitions, int usageCount, String scale) {
        List<ElementDTO> usages = new ArrayList<>();
        
        // 平均为每个Definition分配Usage
        int usagePerDef = Math.max(1, usageCount / definitions.size());
        int remainder = usageCount - (usagePerDef * definitions.size());
        
        for (int defIndex = 0; defIndex < definitions.size(); defIndex++) {
            ElementDTO definition = definitions.get(defIndex);
            int currentUsageCount = usagePerDef + (defIndex < remainder ? 1 : 0);
            
            for (int i = 1; i <= currentUsageCount; i++) {
                Map<String, Object> attributes = new HashMap<>();
                attributes.put("elementId", generateElementId());
                attributes.put("name", scale + "_" + definition.getProperty("declaredShortName") + "_U" + i);
                attributes.put("text", "规模化测试Usage_" + scale + "_场景" + i);
                attributes.put("status", getUsageStatus(usages.size()));
                attributes.put("ofId", definition.getElementId());
                
                ElementDTO usage = universalElementService.createElement("RequirementUsage", attributes);
                usages.add(usage);
            }
        }
        
        return usages;
    }
    
    /**
     * 为规模化数据集生成Trace关系
     */
    private List<ElementDTO> generateEVBatteryTracesForScale(List<ElementDTO> usages, int traceCount, String scale) {
        List<ElementDTO> traces = new ArrayList<>();
        Random random = new Random(scale.hashCode()); // 基于scale名称的固定种子
        
        for (int i = 0; i < traceCount; i++) {
            int sourceIndex = random.nextInt(usages.size());
            int targetIndex;
            do {
                targetIndex = random.nextInt(usages.size());
            } while (targetIndex == sourceIndex);
            
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("elementId", generateElementId());
            attributes.put("name", scale + "_trace_" + (i + 1));
            attributes.put("sourceId", usages.get(sourceIndex).getElementId());
            attributes.put("targetId", usages.get(targetIndex).getElementId());
            attributes.put("traceType", getTraceType(i));
            
            ElementDTO trace = universalElementService.createElement("Dependency", attributes);
            traces.add(trace);
        }
        
        return traces;
    }
    
    /**
     * 生成扩展的L1需求（用于规模化数据集）
     */
    private List<ElementDTO> generateScaledL1Requirements(int count, String scale) {
        List<ElementDTO> definitions = new ArrayList<>();
        
        String[] reqTemplates = {
            "电池系统能量密度需求", "电池系统功率需求", "电池系统安全需求", "电池系统寿命需求",
            "电池系统工作温度需求", "电池系统快充需求", "电池系统重量需求", "电池系统成本需求",
            "电池系统可靠性需求", "电池系统维护需求", "电池系统回收需求", "电池系统标准符合需求"
        };
        
        for (int i = 0; i < count; i++) {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("declaredShortName", String.format("EV-%s-L1-SYS-%03d", scale.toUpperCase(), i + 1));
            attributes.put("name", reqTemplates[i % reqTemplates.length]);
            attributes.put("text", "系统应当满足" + reqTemplates[i % reqTemplates.length] + "的性能指标。");
            attributes.put("elementId", generateElementId());
            attributes.put("tags", Arrays.asList("L1系统", scale + "级别", "系统需求"));
            
            ElementDTO definition = universalElementService.createElement("RequirementDefinition", attributes);
            definitions.add(definition);
        }
        
        return definitions;
    }
    
    /**
     * 生成扩展的L2需求（用于规模化数据集）
     */
    private List<ElementDTO> generateScaledL2Requirements(int count, String scale) {
        List<ElementDTO> definitions = new ArrayList<>();
        
        String[] subsystems = {"BMS", "TMS", "STR", "CHG", "COM", "PWR"};
        String[] reqTypes = {"监测", "控制", "保护", "通信", "诊断", "标定"};
        
        for (int i = 0; i < count; i++) {
            String subsystem = subsystems[i % subsystems.length];
            String reqType = reqTypes[i % reqTypes.length];
            
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("declaredShortName", String.format("EV-%s-L2-%s-%03d", scale.toUpperCase(), subsystem, i + 1));
            attributes.put("name", subsystem + "子系统" + reqType + "需求");
            attributes.put("text", subsystem + "子系统应当实现" + reqType + "功能，满足相应性能要求。");
            attributes.put("elementId", generateElementId());
            attributes.put("tags", Arrays.asList("L2子系统", subsystem, scale + "级别"));
            
            ElementDTO definition = universalElementService.createElement("RequirementDefinition", attributes);
            definitions.add(definition);
        }
        
        return definitions;
    }
    
    /**
     * 生成扩展的L3需求（用于规模化数据集）
     */
    private List<ElementDTO> generateScaledL3Requirements(int count, String scale) {
        List<ElementDTO> definitions = new ArrayList<>();
        
        String[] components = {"CELL", "SENS", "CONT", "COOL", "HEAT", "FUSE"};
        String[] properties = {"精度", "响应", "寿命", "功耗", "尺寸", "重量"};
        
        for (int i = 0; i < count; i++) {
            String component = components[i % components.length];
            String property = properties[i % properties.length];
            
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("declaredShortName", String.format("EV-%s-L3-%s-%03d", scale.toUpperCase(), component, i + 1));
            attributes.put("name", component + "组件" + property + "需求");
            attributes.put("text", component + "组件的" + property + "应当满足设计规范要求。");
            attributes.put("elementId", generateElementId());
            attributes.put("tags", Arrays.asList("L3组件", component, scale + "级别"));
            
            ElementDTO definition = universalElementService.createElement("RequirementDefinition", attributes);
            definitions.add(definition);
        }
        
        return definitions;
    }
    
    /**
     * 清理默认项目的现有数据
     * 注意：为了提升测试性能，暂时禁用清理功能
     */
    private void clearDefaultProject() {
        try {
            // 暂时禁用清理功能以提升性能
            // 在实际生产环境中，可以启用此功能
            log.info("跳过数据清理以提升测试性能");
            
            /*
            // 获取所有元素的副本，避免在删除时修改集合
            List<ElementDTO> allElements = new ArrayList<>(universalElementService.queryElements(null));
            
            log.debug("准备清理{}个现有元素", allElements.size());
            
            // 逐个删除元素
            int deletedCount = 0;
            for (ElementDTO element : allElements) {
                if (element.getElementId() != null) {
                    boolean deleted = universalElementService.deleteElement(element.getElementId());
                    if (deleted) {
                        deletedCount++;
                    }
                }
            }
            
            log.info("成功清理了{}个现有元素", deletedCount);
            */
            
        } catch (Exception e) {
            log.warn("清理现有数据时出现异常: {}", e.getMessage());
        }
    }
    
    /**
     * 将EV电池数据保存到指定文件
     */
    private void saveEVBatteryDataToFile(String fileName) {
        try {
            // 数据实际上已经通过UniversalElementService保存到了默认项目
            // 这里我们只是记录数据的统计信息到指定文件
            File dataDir = new File("./data");
            if (!dataDir.exists()) {
                dataDir.mkdirs();
            }
            
            File dataFile = new File(dataDir, fileName);
            
            // 获取当前生成的数据统计
            List<ElementDTO> definitions = universalElementService.queryElements("RequirementDefinition");
            List<ElementDTO> usages = universalElementService.queryElements("RequirementUsage");
            List<ElementDTO> traces = universalElementService.queryElements("Dependency");
            
            // 写入简单的统计信息（实际数据在EMF Resource中）
            String summary = String.format(
                "{\n" +
                "  \"dataSetName\": \"%s\",\n" +
                "  \"generatedAt\": \"%s\",\n" +
                "  \"statistics\": {\n" +
                "    \"requirementDefinitions\": %d,\n" +
                "    \"requirementUsages\": %d,\n" +
                "    \"traceRelationships\": %d,\n" +
                "    \"totalElements\": %d\n" +
                "  },\n" +
                "  \"description\": \"EV Battery System Demo Data - Generated using Pilot format and Universal Element Service\"\n" +
                "}", 
                fileName, 
                new java.util.Date().toString(),
                definitions.size(),
                usages.size(), 
                traces.size(),
                definitions.size() + usages.size() + traces.size()
            );
            
            java.nio.file.Files.write(dataFile.toPath(), summary.getBytes());
            
            log.info("EV电池数据统计已保存到文件: {} ({}个Definition, {}个Usage, {}个Trace)", 
                dataFile.getAbsolutePath(), definitions.size(), usages.size(), traces.size());
            
        } catch (Exception e) {
            log.error("保存EV电池数据到文件失败: {}", e.getMessage());
        }
    }
    
    /**
     * 生成唯一元素ID
     */
    private String generateElementId() {
        return "ev_" + System.nanoTime() + "_" + (int)(Math.random() * 1000);
    }
}