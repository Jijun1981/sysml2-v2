package com.sysml.mvp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 通用元素数据传输对象
 * 
 * 需求实现：
 * - REQ-D0-1: 通用元素数据API - 每个ElementDTO包含eClass字段标识类型
 * - REQ-B2-4: DTO选择性映射 - 只包含需要的字段，支持动态属性
 * - 通用约定: 未识别字段应保留（round-trip不丢失）
 * 
 * 设计说明：
 * 1. eClass字段标识SysML元素类型（RequirementDefinition, PartUsage等）
 * 2. elementId作为唯一标识符
 * 3. properties Map存储所有动态属性，支持任意SysML类型
 * 4. 不需要为每个SysML类型创建专门的DTO，一个ElementDTO处理所有182个类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ElementDTO {
    
    /**
     * 【REQ-D0-1】SysML元素类型标识
     * 例如：RequirementDefinition, RequirementUsage, PartDefinition, Dependency等
     * 对应EMF EClass.getName()
     */
    private String eClass;
    
    /**
     * 元素唯一标识符
     * 对应EMF elementId属性
     * 格式：{type-prefix}-{uuid} 例如：req-def-12345
     */
    private String elementId;
    
    /**
     * 【REQ-B2-4】动态属性Map
     * 存储所有其他属性，支持选择性映射
     * 例如：declaredName, declaredShortName, documentation, status等
     * 
     * 设计优势：
     * - 支持任意SysML类型而无需修改代码
     * - 保留未识别字段，确保round-trip不丢失数据
     * - 前端可以根据eClass类型动态处理属性
     */
    private Map<String, Object> properties = new HashMap<>();
    
    /**
     * 设置动态属性
     * @param key 属性名
     * @param value 属性值（支持任意类型）
     */
    public void setProperty(String key, Object value) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        properties.put(key, value);
    }
    
    /**
     * 获取动态属性
     * @param key 属性名
     * @return 属性值
     */
    public Object getProperty(String key) {
        if (properties == null) {
            return null;
        }
        return properties.get(key);
    }
    
    /**
     * 获取所有动态属性
     * @return 属性Map的副本
     */
    public Map<String, Object> getProperties() {
        if (properties == null) {
            properties = new HashMap<>();
        }
        return new HashMap<>(properties);
    }
    
    /**
     * 设置所有属性（用于反序列化）
     * @param properties 属性Map
     */
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties != null ? new HashMap<>(properties) : new HashMap<>();
    }
    
    /**
     * 便捷方法：获取字符串类型的属性
     * @param key 属性名
     * @return 字符串值，如果不存在或类型不匹配返回null
     */
    public String getStringProperty(String key) {
        Object value = getProperty(key);
        return value instanceof String ? (String) value : null;
    }
    
    /**
     * 便捷方法：获取整数类型的属性
     * @param key 属性名
     * @return 整数值，如果不存在或类型不匹配返回null
     */
    public Integer getIntProperty(String key) {
        Object value = getProperty(key);
        return value instanceof Integer ? (Integer) value : null;
    }
    
    /**
     * 便捷方法：获取布尔类型的属性
     * @param key 属性名
     * @return 布尔值，如果不存在或类型不匹配返回null
     */
    public Boolean getBooleanProperty(String key) {
        Object value = getProperty(key);
        return value instanceof Boolean ? (Boolean) value : null;
    }
}