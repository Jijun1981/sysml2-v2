package com.sysml.mvp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;

/**
 * 需求数据生成器
 * Definition是需求模板，Usage是基于模板的具体需求条目
 */
public class DataGenerator {
    
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String PROJECT_ID = "default";
    private static final String BASE_PATH = "data/projects/" + PROJECT_ID;
    
    // 需求定义模板列表
    private static final List<RequirementTemplate> TEMPLATES = Arrays.asList(
        new RequirementTemplate("DEF-001", "性能需求模板", "Performance", 
            "系统性能相关需求的标准模板，包括响应时间、吞吐量、并发等性能指标要求"),
        new RequirementTemplate("DEF-002", "安全需求模板", "Security", 
            "系统安全相关需求的标准模板，包括认证、授权、加密、审计等安全要求"),
        new RequirementTemplate("DEF-003", "可用性需求模板", "Usability", 
            "用户界面和用户体验相关需求的标准模板，包括易用性、可访问性等要求"),
        new RequirementTemplate("DEF-004", "功能需求模板", "Functional", 
            "系统功能性需求的标准模板，描述系统应该做什么"),
        new RequirementTemplate("DEF-005", "接口需求模板", "Interface", 
            "系统接口相关需求的标准模板，包括API、数据格式、通信协议等要求"),
        new RequirementTemplate("DEF-006", "数据需求模板", "Data", 
            "数据管理相关需求的标准模板，包括数据存储、备份、恢复等要求"),
        new RequirementTemplate("DEF-007", "可靠性需求模板", "Reliability", 
            "系统可靠性相关需求的标准模板，包括故障恢复、容错等要求"),
        new RequirementTemplate("DEF-008", "兼容性需求模板", "Compatibility", 
            "系统兼容性相关需求的标准模板，包括浏览器、操作系统兼容性等"),
        new RequirementTemplate("DEF-009", "法规需求模板", "Regulatory", 
            "合规性相关需求的标准模板，包括法律法规、行业标准等要求"),
        new RequirementTemplate("DEF-010", "运维需求模板", "Operational", 
            "系统运维相关需求的标准模板，包括监控、日志、部署等要求")
    );
    
    // 需求使用实例
    private static final List<RequirementInstance> INSTANCES = Arrays.asList(
        // 性能需求实例
        new RequirementInstance("REQ-001", "DEF-001", "API响应时间", "API Response Time",
            "所有REST API接口的响应时间必须在200ms以内", "implemented"),
        new RequirementInstance("REQ-002", "DEF-001", "并发用户支持", "Concurrent Users",
            "系统必须支持至少1000个并发用户同时访问", "approved"),
        new RequirementInstance("REQ-003", "DEF-001", "页面加载时间", "Page Load Time",
            "首页加载时间不超过3秒", "verified"),
            
        // 安全需求实例
        new RequirementInstance("REQ-004", "DEF-002", "用户认证", "User Authentication",
            "系统必须实现基于JWT的用户认证机制", "implemented"),
        new RequirementInstance("REQ-005", "DEF-002", "密码策略", "Password Policy",
            "用户密码必须至少8位，包含大小写字母和数字", "approved"),
        new RequirementInstance("REQ-006", "DEF-002", "数据加密", "Data Encryption",
            "敏感数据必须使用AES-256加密存储", "draft"),
            
        // 可用性需求实例
        new RequirementInstance("REQ-007", "DEF-003", "响应式设计", "Responsive Design",
            "界面必须支持PC、平板和手机等多种设备", "implemented"),
        new RequirementInstance("REQ-008", "DEF-003", "多语言支持", "Multi-language",
            "系统必须支持中文和英文两种语言", "approved"),
        new RequirementInstance("REQ-009", "DEF-003", "键盘快捷键", "Keyboard Shortcuts",
            "常用操作必须提供键盘快捷键支持", "draft"),
            
        // 功能需求实例
        new RequirementInstance("REQ-010", "DEF-004", "用户注册", "User Registration",
            "系统必须提供用户自助注册功能，支持邮箱验证", "implemented"),
        new RequirementInstance("REQ-011", "DEF-004", "数据导出", "Data Export",
            "用户可以导出数据为Excel或CSV格式", "verified"),
        new RequirementInstance("REQ-012", "DEF-004", "搜索功能", "Search Function",
            "提供全文搜索功能，支持关键词高亮", "approved"),
        new RequirementInstance("REQ-013", "DEF-004", "批量操作", "Batch Operations",
            "支持批量删除、批量修改等批量操作功能", "draft"),
            
        // 接口需求实例
        new RequirementInstance("REQ-014", "DEF-005", "RESTful API", "RESTful API",
            "所有接口必须遵循RESTful设计规范", "implemented"),
        new RequirementInstance("REQ-015", "DEF-005", "API版本管理", "API Versioning",
            "API必须支持版本管理，URL中包含版本号", "approved"),
        new RequirementInstance("REQ-016", "DEF-005", "数据格式", "Data Format",
            "接口数据交换格式统一使用JSON", "verified"),
            
        // 数据需求实例
        new RequirementInstance("REQ-017", "DEF-006", "数据备份", "Data Backup",
            "系统必须每天自动备份数据，保留30天", "implemented"),
        new RequirementInstance("REQ-018", "DEF-006", "数据恢复", "Data Recovery",
            "支持从备份中恢复数据，RTO不超过4小时", "approved"),
        new RequirementInstance("REQ-019", "DEF-006", "数据归档", "Data Archiving",
            "超过一年的历史数据自动归档", "draft"),
            
        // 可靠性需求实例
        new RequirementInstance("REQ-020", "DEF-007", "系统可用性", "System Availability",
            "系统可用性必须达到99.9%", "approved"),
        new RequirementInstance("REQ-021", "DEF-007", "故障恢复", "Failure Recovery",
            "系统故障后必须在30分钟内自动恢复", "implemented"),
        new RequirementInstance("REQ-022", "DEF-007", "数据一致性", "Data Consistency",
            "分布式环境下保证数据最终一致性", "draft"),
            
        // 兼容性需求实例
        new RequirementInstance("REQ-023", "DEF-008", "浏览器兼容", "Browser Compatibility",
            "支持Chrome、Firefox、Safari、Edge最新版本", "verified"),
        new RequirementInstance("REQ-024", "DEF-008", "移动端兼容", "Mobile Compatibility",
            "支持iOS 12+和Android 8+", "implemented"),
            
        // 法规需求实例
        new RequirementInstance("REQ-025", "DEF-009", "GDPR合规", "GDPR Compliance",
            "系统必须符合GDPR数据保护要求", "approved"),
        new RequirementInstance("REQ-026", "DEF-009", "数据隐私", "Data Privacy",
            "用户个人信息必须获得用户明确同意后才能收集", "implemented"),
            
        // 运维需求实例
        new RequirementInstance("REQ-027", "DEF-010", "系统监控", "System Monitoring",
            "提供实时系统监控dashboard，包括CPU、内存、磁盘等指标", "implemented"),
        new RequirementInstance("REQ-028", "DEF-010", "日志管理", "Log Management",
            "系统日志必须集中管理，保留90天", "verified"),
        new RequirementInstance("REQ-029", "DEF-010", "自动化部署", "Auto Deployment",
            "支持CI/CD自动化部署流程", "approved"),
        new RequirementInstance("REQ-030", "DEF-010", "告警通知", "Alert Notification",
            "系统异常时自动发送邮件和短信告警", "draft"),
            
        // 额外的功能需求
        new RequirementInstance("REQ-031", "DEF-004", "权限管理", "Permission Management",
            "基于角色的访问控制(RBAC)，支持细粒度权限配置", "implemented"),
        new RequirementInstance("REQ-032", "DEF-004", "审计日志", "Audit Log",
            "记录所有用户操作，支持审计追踪", "approved"),
        new RequirementInstance("REQ-033", "DEF-004", "通知中心", "Notification Center",
            "统一的消息通知中心，支持站内信、邮件、短信", "draft"),
        new RequirementInstance("REQ-034", "DEF-004", "工作流引擎", "Workflow Engine",
            "可配置的工作流引擎，支持审批流程自定义", "draft"),
        new RequirementInstance("REQ-035", "DEF-004", "报表统计", "Report Statistics",
            "提供丰富的统计报表和数据可视化", "verified")
    );
    
    public static void main(String[] args) throws IOException {
        generateData();
    }
    
    public static void generateData() throws IOException {
        // 创建项目目录
        Path projectPath = Paths.get(BASE_PATH);
        Files.createDirectories(projectPath);
        
        // 创建根对象
        ObjectNode root = mapper.createObjectNode();
        root.put("eClass", "http://www.omg.org/spec/SysML/20240201#//Namespace");
        root.put("elementId", UUID.randomUUID().toString());
        
        // 创建ownedElement数组
        ArrayNode ownedElements = mapper.createArrayNode();
        
        // 添加所有Definition
        for (RequirementTemplate template : TEMPLATES) {
            ObjectNode def = createDefinition(template);
            ownedElements.add(def);
        }
        
        // 添加所有Usage
        for (RequirementInstance instance : INSTANCES) {
            ObjectNode usage = createUsage(instance);
            ownedElements.add(usage);
        }
        
        // 添加一些Dependency关系
        addDependencies(ownedElements);
        
        root.set("ownedElement", ownedElements);
        
        // 保存到文件
        File modelFile = new File(BASE_PATH + "/model.json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(modelFile, root);
        
        System.out.println("Generated " + TEMPLATES.size() + " definitions and " + 
                          INSTANCES.size() + " usages to " + modelFile.getAbsolutePath());
    }
    
    private static ObjectNode createDefinition(RequirementTemplate template) {
        ObjectNode def = mapper.createObjectNode();
        def.put("eClass", "http://www.omg.org/spec/SysML/20240201#//RequirementDefinition");
        def.put("elementId", template.id);
        def.put("declaredName", template.name);
        def.put("declaredShortName", template.shortName);
        
        // 添加text数组
        ArrayNode textArray = mapper.createArrayNode();
        ObjectNode textNode = mapper.createObjectNode();
        textNode.put("body", template.description);
        textArray.add(textNode);
        def.set("text", textArray);
        
        // 添加documentation数组
        ArrayNode docArray = mapper.createArrayNode();
        ObjectNode docNode = mapper.createObjectNode();
        docNode.put("body", template.description);
        docArray.add(docNode);
        def.set("documentation", docArray);
        
        // 添加时间戳
        String now = Instant.now().toString();
        def.put("createdAt", now);
        def.put("updatedAt", now);
        
        return def;
    }
    
    private static ObjectNode createUsage(RequirementInstance instance) {
        ObjectNode usage = mapper.createObjectNode();
        usage.put("eClass", "http://www.omg.org/spec/SysML/20240201#//RequirementUsage");
        usage.put("elementId", instance.id);
        usage.put("declaredName", instance.name);
        usage.put("declaredShortName", instance.shortName);
        
        // 添加text数组
        ArrayNode textArray = mapper.createArrayNode();
        ObjectNode textNode = mapper.createObjectNode();
        textNode.put("body", instance.text);
        textArray.add(textNode);
        usage.set("text", textArray);
        
        // 添加对Definition的引用
        usage.put("of", instance.definitionId);
        
        // 添加状态
        usage.put("status", instance.status);
        
        // 添加时间戳
        String now = Instant.now().toString();
        usage.put("createdAt", now);
        usage.put("updatedAt", now);
        
        return usage;
    }
    
    private static void addDependencies(ArrayNode ownedElements) {
        // 添加一些示例依赖关系
        addDependency(ownedElements, "REQ-001", "REQ-002", "refine");
        addDependency(ownedElements, "REQ-004", "REQ-005", "derive");
        addDependency(ownedElements, "REQ-010", "REQ-011", "trace");
        addDependency(ownedElements, "REQ-014", "REQ-015", "satisfy");
        addDependency(ownedElements, "REQ-017", "REQ-018", "derive");
    }
    
    private static void addDependency(ArrayNode ownedElements, String fromId, String toId, String type) {
        ObjectNode dep = mapper.createObjectNode();
        dep.put("eClass", "http://www.omg.org/spec/SysML/20240201#//Dependency");
        dep.put("elementId", "DEP-" + UUID.randomUUID().toString().substring(0, 8));
        dep.put("fromId", fromId);
        dep.put("toId", toId);
        dep.put("type", type);
        dep.put("createdAt", Instant.now().toString());
        ownedElements.add(dep);
    }
    
    // 辅助类
    static class RequirementTemplate {
        String id, name, shortName, description;
        
        RequirementTemplate(String id, String name, String shortName, String description) {
            this.id = id;
            this.name = name;
            this.shortName = shortName;
            this.description = description;
        }
    }
    
    static class RequirementInstance {
        String id, definitionId, name, shortName, text, status;
        
        RequirementInstance(String id, String definitionId, String name, String shortName, 
                           String text, String status) {
            this.id = id;
            this.definitionId = definitionId;
            this.name = name;
            this.shortName = shortName;
            this.text = text;
            this.status = status;
        }
    }
}