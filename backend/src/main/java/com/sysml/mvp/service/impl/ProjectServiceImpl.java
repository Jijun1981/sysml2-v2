package com.sysml.mvp.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sysml.mvp.dto.ElementDTO;
import com.sysml.mvp.service.ProjectService;
import com.sysml.mvp.service.UniversalElementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 项目服务实现 - 委托UniversalElementService进行数据操作
 * 
 * 需求实现：
 * - REQ-B3-1: 导出JSON - 规范文件名和格式
 * - REQ-B3-2: 导入JSON - 验证格式并创建元素
 * - REQ-B3-3: 一致性保证 - ID稳定性和引用完整性
 */
@Slf4j
@Service
public class ProjectServiceImpl implements ProjectService {
    
    private final UniversalElementService universalElementService;
    private final ObjectMapper objectMapper;
    
    public ProjectServiceImpl(UniversalElementService universalElementService, ObjectMapper objectMapper) {
        this.universalElementService = universalElementService;
        this.objectMapper = objectMapper;
    }
    
    /**
     * 【REQ-B3-1】导出项目为标准SysML JSON格式
     */
    @Override
    public Map<String, Object> exportProject(String projectId) {
        try {
            log.info("导出项目: {}", projectId);
            
            // 获取所有元素
            List<ElementDTO> allElements = universalElementService.getAllElements();
            if (allElements.isEmpty()) {
                throw new IllegalArgumentException("Project not found: " + projectId);
            }
            
            // 构建标准SysML JSON格式
            Map<String, Object> exportData = new HashMap<>();
            
            // JSON版本信息
            Map<String, Object> jsonInfo = new HashMap<>();
            jsonInfo.put("version", "1.0");
            jsonInfo.put("encoding", "UTF-8");
            exportData.put("json", jsonInfo);
            
            // 命名空间 - 使用标准SysML命名空间
            Map<String, Object> namespaces = new HashMap<>();
            namespaces.put("sysml", "https://www.omg.org/spec/SysML/20250201");
            exportData.put("ns", namespaces);
            
            // Schema位置
            Map<String, Object> schemaLocation = new HashMap<>();
            schemaLocation.put("https://www.omg.org/spec/SysML/20250201", 
                "file:/mnt/d/sysml2%20v2/backend/../opensource/SysML-v2-Pilot-Implementation/org.omg.sysml/model/SysML.ecore#/-1");
            exportData.put("schemaLocation", schemaLocation);
            
            // 内容转换
            List<Map<String, Object>> content = new ArrayList<>();
            for (ElementDTO element : allElements) {
                Map<String, Object> elementData = new HashMap<>();
                elementData.put("eClass", "sysml:" + element.getEClass());
                
                Map<String, Object> data = new HashMap<>();
                data.put("elementId", element.getElementId());
                
                // 添加安全的属性（过滤掉EMF内部对象）
                if (element.getProperties() != null) {
                    for (Map.Entry<String, Object> entry : element.getProperties().entrySet()) {
                        Object value = entry.getValue();
                        // 只添加基本数据类型，过滤掉EMF对象
                        if (value != null && isSerializableValue(value)) {
                            data.put(entry.getKey(), value);
                        } else if (value != null) {
                            // 对于复杂对象，只保存toString()值
                            data.put(entry.getKey(), value.toString());
                        }
                    }
                }
                
                elementData.put("data", data);
                content.add(elementData);
            }
            
            exportData.put("content", content);
            
            log.info("成功导出项目: {}, 包含{}个元素", projectId, content.size());
            return exportData;
            
        } catch (Exception e) {
            log.error("导出项目失败: {}", projectId, e);
            throw new IllegalArgumentException("Failed to export project: " + e.getMessage());
        }
    }
    
    /**
     * 【REQ-B3-2】导入JSON项目数据
     */
    @Override
    public Map<String, Object> importProject(String projectId, String jsonContent) {
        try {
            log.info("导入项目: {}", projectId);
            
            // 解析JSON
            JsonNode rootNode = objectMapper.readTree(jsonContent);
            
            // 验证必填字段
            validateJsonStructure(rootNode);
            
            // 获取内容数组
            JsonNode contentNode = rootNode.get("content");
            if (!contentNode.isArray()) {
                throw new IllegalArgumentException("content must be an array");
            }
            
            int elementsImported = 0;
            List<String> createdElementIds = new ArrayList<>();
            
            // 导入每个元素
            for (JsonNode elementNode : contentNode) {
                try {
                    String elementId = importSingleElement(elementNode);
                    if (elementId != null) {
                        createdElementIds.add(elementId);
                        elementsImported++;
                    }
                } catch (Exception e) {
                    log.warn("导入元素失败: {}, 继续导入其他元素", e.getMessage());
                }
            }
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("projectId", projectId);
            result.put("elementsImported", elementsImported);
            result.put("createdElements", createdElementIds);
            result.put("status", "success");
            
            log.info("成功导入项目: {}, 导入{}个元素", projectId, elementsImported);
            return result;
            
        } catch (JsonProcessingException e) {
            String errorMsg = String.format("Invalid JSON format at line %d, column %d: %s", 
                e.getLocation().getLineNr(), e.getLocation().getColumnNr(), e.getOriginalMessage());
            log.error("JSON解析失败: {}", errorMsg);
            throw new IllegalArgumentException(errorMsg);
        } catch (Exception e) {
            log.error("导入项目失败: {}", projectId, e);
            throw new IllegalArgumentException("Failed to import project: " + e.getMessage());
        }
    }
    
    /**
     * 验证JSON结构的必填字段
     */
    private void validateJsonStructure(JsonNode rootNode) {
        // 验证json字段
        if (!rootNode.has("json") || !rootNode.get("json").has("version")) {
            throw new IllegalArgumentException("Missing required field: json.version");
        }
        
        // 验证ns字段  
        if (!rootNode.has("ns")) {
            throw new IllegalArgumentException("Missing required field: ns");
        }
        
        // 验证content字段
        if (!rootNode.has("content")) {
            throw new IllegalArgumentException("Missing required field: content");
        }
    }
    
    /**
     * 导入单个元素
     */
    private String importSingleElement(JsonNode elementNode) {
        if (!elementNode.has("eClass") || !elementNode.has("data")) {
            log.warn("元素缺少eClass或data字段，跳过");
            return null;
        }
        
        String eClass = elementNode.get("eClass").asText();
        // 移除sysml:前缀
        if (eClass.startsWith("sysml:")) {
            eClass = eClass.substring(6);
        }
        
        // EClass名称映射：处理demo数据与系统的差异
        eClass = mapEClassName(eClass);
        
        JsonNode dataNode = elementNode.get("data");
        Map<String, Object> attributes = new HashMap<>();
        
        // 转换所有数据字段为属性Map，处理字段名映射
        dataNode.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            
            // 字段名转换：将demo数据格式转换为系统格式
            String mappedKey = mapFieldName(key);
            
            if (value.isTextual()) {
                attributes.put(mappedKey, value.asText());
            } else if (value.isNumber()) {
                attributes.put(mappedKey, value.asDouble());
            } else if (value.isBoolean()) {
                attributes.put(mappedKey, value.asBoolean());
            } else if (value.isArray()) {
                // 处理数组类型（如tags）
                List<String> arrayValues = new ArrayList<>();
                for (JsonNode arrayElement : value) {
                    arrayValues.add(arrayElement.asText());
                }
                attributes.put(mappedKey, arrayValues);
            } else {
                attributes.put(mappedKey, value.toString());
            }
        });
        
        try {
            log.info("准备创建元素: eClass={}, attributes={}", eClass, attributes);
            // 使用UniversalElementService创建元素
            ElementDTO createdElement = universalElementService.createElement(eClass, attributes);
            log.info("成功导入元素: {} ({})", createdElement.getElementId(), eClass);
            return createdElement.getElementId();
        } catch (Exception e) {
            log.warn("创建元素失败: eClass={}, elementId={}, attributes={}, error={}", 
                eClass, attributes.get("elementId"), attributes, e.getMessage());
            return null;
        }
    }
    
    /**
     * EClass名称映射：处理demo数据与系统的差异
     */
    private String mapEClassName(String originalEClass) {
        switch (originalEClass) {
            case "Trace":
                return "Dependency"; // demo数据用Trace，系统用Dependency
            default:
                return originalEClass; // 其他EClass保持不变
        }
    }
    
    /**
     * 字段名映射：将demo数据格式转换为系统格式
     */
    private String mapFieldName(String originalName) {
        // 处理demo数据中的字段名映射
        switch (originalName) {
            case "id":
                return "elementId";  // demo数据用id，系统用elementId
            case "name":
                return "declaredName"; // demo数据用name，系统用declaredName
            case "text":
                return "text"; // 文本内容保持不变
            case "doc":
                return "documentation"; // demo数据用doc，系统用documentation
            default:
                return originalName; // 其他字段名保持不变
        }
    }
    
    /**
     * 检查值是否为可安全序列化的基本数据类型
     */
    private boolean isSerializableValue(Object value) {
        return value instanceof String ||
               value instanceof Number ||
               value instanceof Boolean ||
               value instanceof java.util.Date ||
               (value instanceof java.util.Collection && 
                ((java.util.Collection<?>) value).isEmpty()) ||
               (value instanceof java.util.Map && 
                ((java.util.Map<?, ?>) value).isEmpty());
    }
}