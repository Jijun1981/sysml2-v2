package com.sysml.mvp.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * 通用元素DTO
 * 用于传输任意SysML元素数据
 * 
 * 使用@JsonAnyGetter和@JsonAnySetter支持动态属性
 */
public class ElementDTO {
    
    @JsonProperty("eClass")
    private String eClass;
    
    @JsonProperty("elementId")
    private String elementId;
    
    // 动态属性存储
    private Map<String, Object> properties = new HashMap<>();
    
    public ElementDTO() {
    }
    
    public ElementDTO(String eClass, String elementId) {
        this.eClass = eClass;
        this.elementId = elementId;
    }
    
    public String getEClass() {
        return eClass;
    }
    
    public void setEClass(String eClass) {
        this.eClass = eClass;
    }
    
    public String getElementId() {
        return elementId;
    }
    
    public void setElementId(String elementId) {
        this.elementId = elementId;
    }
    
    /**
     * 获取所有动态属性
     * Jackson会将这些属性展开到JSON根级别
     */
    @JsonAnyGetter
    public Map<String, Object> getProperties() {
        return properties;
    }
    
    /**
     * 设置动态属性
     * Jackson反序列化时会调用此方法
     */
    @JsonAnySetter
    public void setProperty(String key, Object value) {
        // 跳过已知的固定字段
        if (!"eClass".equals(key) && !"elementId".equals(key)) {
            properties.put(key, value);
        }
    }
    
    /**
     * 获取单个属性值
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    /**
     * 设置所有属性
     */
    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }
}