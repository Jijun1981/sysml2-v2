/**
 * 端到端验证测试 - TDD深度修复验证
 * 验证前后端通信、三视图同步、CRUD操作
 */

import axios from 'axios';

describe('端到端前后端通信验证', () => {
  const baseURL = 'http://localhost:8080/api/v1';
  
  beforeAll(async () => {
    // 等待服务器启动
    await new Promise(resolve => setTimeout(resolve, 2000));
  });

  test('后端API健康检查', async () => {
    const response = await axios.get(`${baseURL}/health`);
    expect(response.status).toBe(200);
    expect(response.data.status).toBe('UP');
  });

  test('CORS配置验证 - 预检请求', async () => {
    const response = await axios.options(`${baseURL}/elements`, {
      headers: {
        'Origin': 'http://localhost:3002',
        'Access-Control-Request-Method': 'GET'
      }
    });
    expect(response.headers['access-control-allow-origin']).toBe('*');
  });

  test('获取所有元素 - API通信正常', async () => {
    const response = await axios.get(`${baseURL}/elements`);
    expect(response.status).toBe(200);
    expect(Array.isArray(response.data)).toBe(true);
    expect(response.data.length).toBeGreaterThan(0);
  });

  test('创建新元素 - CRUD操作验证', async () => {
    const newElement = {
      eClass: 'RequirementDefinition',
      attributes: {
        declaredShortName: 'E2E-TEST-REQ',
        declaredName: '端到端测试需求'
      }
    };

    const response = await axios.post(`${baseURL}/elements`, newElement);
    expect(response.status).toBe(201);
    expect(response.data.eClass).toBe('RequirementDefinition');
    expect(response.data.elementId).toBeDefined();
    
    // 清理测试数据
    await axios.delete(`${baseURL}/elements/${response.data.elementId}`);
  });

  test('按类型查询元素', async () => {
    const response = await axios.get(`${baseURL}/elements?type=RequirementDefinition`);
    expect(response.status).toBe(200);
    expect(Array.isArray(response.data)).toBe(true);
    
    // 验证返回的都是RequirementDefinition类型
    response.data.forEach(element => {
      expect(element.eClass).toBe('RequirementDefinition');
    });
  });

  test('数据结构符合前端期望', async () => {
    const response = await axios.get(`${baseURL}/elements`);
    const elements = response.data;
    
    elements.forEach(element => {
      // 验证必要字段存在
      expect(element.elementId).toBeDefined();
      expect(element.eClass).toBeDefined();
      expect(typeof element.elementId).toBe('string');
      expect(typeof element.eClass).toBe('string');
      
      // 验证前端需要的字段
      if (element.declaredShortName) {
        expect(typeof element.declaredShortName).toBe('string');
      }
      if (element.declaredName) {
        expect(typeof element.declaredName).toBe('string');
      }
    });
  });
});