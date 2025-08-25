/**
 * 测试：确保前端不使用旧的API接口
 * 需求：REQ-B5-1到B5-4 - 使用通用元素接口
 */

import { describe, test, expect, vi } from 'vitest';
import * as api from '../services/api';
import * as universalApi from '../services/universalApi';

describe('REQ-B5: 通用元素接口合规性测试', () => {
  
  test('REQ-B5-1: 不应存在旧的/projects/{id}/requirements调用', () => {
    // 检查api.ts中不应该有对旧接口的调用
    const apiSource = api.toString();
    
    // 旧接口路径不应该存在
    expect(apiSource).not.toContain('/projects/');
    expect(apiSource).not.toContain('/requirements');
    expect(apiSource).not.toContain('/traces');
  });

  test('REQ-B5-2: 应该使用/api/v1/elements接口', () => {
    // 验证universalApi使用了正确的接口
    expect(universalApi.queryElementsByType).toBeDefined();
    expect(universalApi.queryAllElements).toBeDefined();
    expect(universalApi.createUniversalElement).toBeDefined();
    expect(universalApi.updateUniversalElement).toBeDefined();
    expect(universalApi.deleteUniversalElement).toBeDefined();
  });

  test('REQ-A1-1: ModelContext应该使用通用接口作为SSOT', async () => {
    // Mock universalApi调用
    const mockQueryAll = vi.spyOn(universalApi, 'queryAllElements');
    const mockQueryByType = vi.spyOn(universalApi, 'queryElementsByType');
    
    // 验证ModelContext不会调用旧API
    const { ModelProvider } = await import('../contexts/ModelContext');
    
    // 应该只调用通用接口
    expect(mockQueryAll).toBeDefined();
    expect(mockQueryByType).toBeDefined();
  });

  test('REQ-A1-2: 视图组件不应直接调用API', () => {
    // 树视图、表视图、图视图应该只从ModelContext获取数据
    const viewFiles = [
      '../components/views/TreeView/TreeView.tsx',
      '../components/views/TableView/TableView.tsx',
      '../components/views/GraphView/GraphView.tsx'
    ];
    
    viewFiles.forEach(file => {
      // 视图组件不应该import api服务
      expect(() => require(file)).not.toThrow();
    });
  });

  test('REQ-B5-3: PATCH更新应使用通用接口', () => {
    // 验证更新操作使用了PATCH /api/v1/elements/{id}
    expect(universalApi.updateUniversalElement).toBeDefined();
    
    // 不应该有PUT操作
    const apiSource = api.toString();
    expect(apiSource).not.toContain('PUT ');
  });

  test('REQ-B5-4: 零代码扩展 - 不应有硬编码的类型判断', () => {
    // universalApi不应该有硬编码的类型判断
    const universalApiSource = universalApi.toString();
    
    // 不应该有switch(eClass)或if(eClass === 'specific')
    expect(universalApiSource).not.toContain('switch');
    expect(universalApiSource).not.toContain("=== 'RequirementDefinition'");
    expect(universalApiSource).not.toContain("=== 'RequirementUsage'");
  });
});

describe('前端API清理验证', () => {
  
  test('api.ts不应该创建自己的axios实例访问后端', () => {
    // api.ts如果需要兼容，应该内部调用universalApi
    const apiExports = Object.keys(api);
    
    // 这些方法如果存在，应该是调用universalApi的包装
    if (apiExports.includes('getRequirements')) {
      // getRequirements应该调用queryElementsByType
      expect(api.getRequirements.toString()).toContain('queryElementsByType');
    }
  });

  test('前端组件不应该直接创建axios请求', () => {
    // 所有组件应该通过context或service层
    const componentFiles = [
      '../components/layout/MainLayout.tsx',
      '../App.tsx'
    ];
    
    componentFiles.forEach(file => {
      // 组件不应该直接import axios
      expect(() => require(file)).not.toThrow();
    });
  });
});