package com.sysml.mvp.service;

import org.eclipse.emf.ecore.EObject;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.List;

/**
 * SysML 2.0 Metadata管理服务
 * 
 * 基于SysML 2.0原生的MetadataDefinition和MetadataUsage机制
 * 实现治理字段的结构化存储和管理
 * 
 * 需求对齐：
 * - REQ-FS-4: Metadata机制实现
 * - 与动态EMF架构兼容
 * - 支持182个SysML类型的通用Metadata管理
 */
@Service
public interface MetadataService {
    
    /**
     * 创建MetadataDefinition
     * 定义Metadata的结构和验证规则
     * 
     * @param name Metadata类型名称（如"Priority", "Status"）
     * @param definition Metadata定义数据，包含schema等
     * @return MetadataDefinition的ID
     * 
     * 示例：
     * Map<String, Object> priorityDef = Map.of(
     *     "name", "Priority",
     *     "schema", Map.of(
     *         "level", Map.of("type", "enum", "values", List.of("Low", "Medium", "High", "Critical")),
     *         "rationale", Map.of("type", "string", "required", false)
     *     )
     * );
     * String defId = createMetadataDefinition("Priority", priorityDef);
     */
    String createMetadataDefinition(String name, Map<String, Object> definition);
    
    /**
     * 查询MetadataDefinition
     * 
     * @param name Metadata类型名称
     * @return MetadataDefinition数据，如果不存在返回null
     */
    Map<String, Object> getMetadataDefinition(String name);
    
    /**
     * 应用Metadata到元素
     * 创建MetadataUsage并关联到指定元素
     * 
     * @param element 目标元素
     * @param metadataType Metadata类型名称
     * @param values Metadata值
     * @return MetadataUsage的ID
     * 
     * 示例：
     * Map<String, Object> values = Map.of("level", "High", "rationale", "Safety critical");
     * String usageId = applyMetadata(requirement, "Priority", values);
     */
    String applyMetadata(EObject element, String metadataType, Map<String, Object> values);
    
    /**
     * 设置元素的单个Metadata字段
     * 简化的Metadata设置接口
     * 
     * @param element 目标元素
     * @param fieldName 字段名（如"status", "priority"）
     * @param value 字段值
     * @return 是否设置成功
     * 
     * 示例：
     * setMetadata(requirement, "status", "approved");
     * setMetadata(requirement, "priority", "High");
     */
    boolean setMetadata(EObject element, String fieldName, Object value);
    
    /**
     * 获取元素的Metadata值
     * 
     * @param element 目标元素
     * @param metadataType Metadata类型名称
     * @return Metadata值，如果不存在返回null
     */
    Map<String, Object> getMetadata(EObject element, String metadataType);
    
    /**
     * 获取元素的单个Metadata字段值
     * 
     * @param element 目标元素  
     * @param fieldName 字段名
     * @return 字段值，如果不存在返回null
     */
    Object getMetadataValue(EObject element, String fieldName);
    
    /**
     * 删除元素的Metadata
     * 
     * @param element 目标元素
     * @param metadataType Metadata类型名称
     * @return 是否删除成功
     */
    boolean removeMetadata(EObject element, String metadataType);
    
    /**
     * 批量设置Metadata
     * 
     * @param elements 目标元素列表
     * @param metadataType Metadata类型名称
     * @param values Metadata值
     * @return 成功设置的元素数量
     */
    int batchSetMetadata(List<EObject> elements, String metadataType, Map<String, Object> values);
    
    /**
     * 查询具有特定Metadata值的元素
     * 
     * @param projectId 项目ID
     * @param metadataType Metadata类型名称
     * @param fieldName 字段名
     * @param value 字段值
     * @return 匹配的元素列表
     */
    List<EObject> findElementsByMetadata(String projectId, String metadataType, String fieldName, Object value);
    
    /**
     * 初始化核心治理Metadata定义
     * 创建项目常用的Metadata定义：Priority, Status, Owner, Source, VerificationMethod
     */
    void initializeCoreGovernanceMetadata();
    
    /**
     * 验证Metadata值是否符合定义的schema
     * 
     * @param metadataType Metadata类型名称
     * @param values 要验证的值
     * @return 验证结果，包含是否通过和错误信息
     */
    MetadataValidationResult validateMetadata(String metadataType, Map<String, Object> values);
    
    /**
     * Metadata验证结果
     */
    class MetadataValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        public MetadataValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }
        
        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
    }
}