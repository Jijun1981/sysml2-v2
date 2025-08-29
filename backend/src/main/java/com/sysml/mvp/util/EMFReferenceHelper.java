package com.sysml.mvp.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.emf.ecore.*;
import org.eclipse.emf.ecore.resource.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * 辅助类，用于处理EMF引用的持久化
 * 由于requirementDefinition是derived/transient/volatile，需要特殊处理
 */
@Slf4j
@Component
public class EMFReferenceHelper {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 在JSON文件中添加引用信息
     * 由于Sirius EMF JSON不会序列化derived属性，我们手动添加
     */
    public void addReferenceToJson(Path jsonPath, String elementId, String referenceField, String targetId) {
        try {
            // 读取JSON文件
            String content = Files.readString(jsonPath);
            JsonNode root = objectMapper.readTree(content);
            
            // 查找对应的元素
            JsonNode contentArray = root.get("content");
            if (contentArray != null && contentArray.isArray()) {
                for (int i = 0; i < contentArray.size(); i++) {
                    JsonNode item = contentArray.get(i);
                    JsonNode data = item.get("data");
                    if (data != null && targetId.equals(data.get("elementId").asText())) {
                        // 找到目标元素，添加引用
                        ((ObjectNode) data).put(referenceField, targetId);
                        log.info("添加引用到JSON: {}.{} = {}", elementId, referenceField, targetId);
                        break;
                    }
                }
            }
            
            // 写回文件
            String updatedContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            Files.writeString(jsonPath, updatedContent);
            
        } catch (IOException e) {
            log.error("处理JSON引用失败: {}", jsonPath, e);
        }
    }
    
    /**
     * 创建引用映射表，用于后处理
     * 键: elementId, 值: Map<引用字段名, 目标ID>
     */
    private static final Map<String, Map<String, String>> pendingReferences = new HashMap<>();
    
    /**
     * 注册待处理的引用
     */
    public static void registerPendingReference(String elementId, String refField, String targetId) {
        pendingReferences.computeIfAbsent(elementId, k -> new HashMap<>())
                        .put(refField, targetId);
        log.debug("注册待处理引用: {} -> {}.{}", elementId, refField, targetId);
    }
    
    /**
     * 处理所有待处理的引用
     */
    public void processPendingReferences(Resource resource) {
        if (pendingReferences.isEmpty()) {
            return;
        }
        
        log.info("开始处理{}个待处理的引用", pendingReferences.size());
        
        for (Map.Entry<String, Map<String, String>> entry : pendingReferences.entrySet()) {
            String elementId = entry.getKey();
            Map<String, String> refs = entry.getValue();
            
            // 查找元素
            EObject element = findByElementId(resource, elementId);
            if (element != null) {
                for (Map.Entry<String, String> refEntry : refs.entrySet()) {
                    String refField = refEntry.getKey();
                    String targetId = refEntry.getValue();
                    
                    // 设置引用
                    setReference(element, refField, targetId, resource);
                }
            }
        }
        
        // 清空待处理列表
        pendingReferences.clear();
    }
    
    /**
     * 根据elementId查找对象
     */
    private EObject findByElementId(Resource resource, String elementId) {
        for (EObject obj : resource.getContents()) {
            EStructuralFeature idFeature = obj.eClass().getEStructuralFeature("elementId");
            if (idFeature != null) {
                Object value = obj.eGet(idFeature);
                if (elementId.equals(value)) {
                    return obj;
                }
            }
        }
        return null;
    }
    
    /**
     * 设置引用
     */
    private void setReference(EObject source, String refField, String targetId, Resource resource) {
        EStructuralFeature feature = source.eClass().getEStructuralFeature(refField);
        if (feature instanceof EReference) {
            EObject target = findByElementId(resource, targetId);
            if (target != null) {
                source.eSet(feature, target);
                log.info("设置引用: {}.{} -> {}", 
                    source.eGet(source.eClass().getEStructuralFeature("elementId")),
                    refField, targetId);
            }
        }
    }
    
    /**
     * 在保存前，将引用信息添加到JSON结构中
     * 这是一个后处理步骤，确保derived属性也被保存
     */
    public void postProcessJsonFile(Path jsonPath, Resource resource) {
        try {
            // 读取JSON
            String content = Files.readString(jsonPath);
            JsonNode root = objectMapper.readTree(content);
            ObjectNode rootObj = (ObjectNode) root;
            JsonNode contentArray = rootObj.get("content");
            
            if (contentArray != null && contentArray.isArray()) {
                // 遍历所有对象
                for (int i = 0; i < contentArray.size(); i++) {
                    JsonNode item = contentArray.get(i);
                    String eClass = item.get("eClass").asText();
                    
                    if ("sysml:RequirementUsage".equals(eClass)) {
                        JsonNode data = item.get("data");
                        String elementId = data.get("elementId").asText();
                        
                        // 查找对应的EMF对象
                        EObject obj = findByElementId(resource, elementId);
                        if (obj != null) {
                            // 获取requirementDefinition引用
                            EStructuralFeature refFeature = obj.eClass().getEStructuralFeature("requirementDefinition");
                            if (refFeature != null) {
                                Object refValue = obj.eGet(refFeature);
                                if (refValue instanceof EObject) {
                                    EObject refObj = (EObject) refValue;
                                    EStructuralFeature idFeature = refObj.eClass().getEStructuralFeature("elementId");
                                    if (idFeature != null) {
                                        String refId = (String) refObj.eGet(idFeature);
                                        // 添加到JSON
                                        ((ObjectNode) data).put("requirementDefinition", refId);
                                        log.info("后处理：添加requirementDefinition引用 {} -> {}", elementId, refId);
                                    }
                                }
                            }
                        }
                    }
                }
                
                // 写回文件
                String updatedContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
                Files.writeString(jsonPath, updatedContent);
            }
            
        } catch (IOException e) {
            log.error("后处理JSON文件失败: {}", jsonPath, e);
        }
    }
}