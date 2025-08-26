package com.sysml.mvp.service;

import java.util.Map;

/**
 * 项目服务接口 - 负责项目导入导出功能
 * 
 * 需求实现：
 * - REQ-B3-1: 导出JSON项目文件
 * - REQ-B3-2: 导入JSON项目文件  
 * - REQ-B3-3: 导入导出一致性保证
 */
public interface ProjectService {
    
    /**
     * 【REQ-B3-1】导出项目为JSON格式
     * @param projectId 项目ID
     * @return JSON格式的项目数据
     * @throws IllegalArgumentException 如果项目不存在
     */
    Map<String, Object> exportProject(String projectId);
    
    /**
     * 【REQ-B3-2】导入JSON格式的项目数据
     * @param projectId 项目ID
     * @param jsonContent JSON内容
     * @return 导入结果信息
     * @throws IllegalArgumentException 如果JSON格式错误或缺少必填字段
     */
    Map<String, Object> importProject(String projectId, String jsonContent);
}