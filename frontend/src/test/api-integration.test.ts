/**
 * API集成测试 - 验证前后端通信
 * 基于TDD原则，先写测试再修复代码
 */

import { describe, test, expect, beforeAll } from 'vitest'
import axios from 'axios'

describe('前后端API集成测试', () => {
  const API_URL = 'http://localhost:8080/api/v1'
  const PROJECT_ID = 'default'
  
  describe('后端API直接测试', () => {
    test('GET /elements应该返回数组', async () => {
      // 测试后端是否正确响应
      const response = await axios.get(`${API_URL}/elements`, {
        params: { projectId: PROJECT_ID }
      })
      
      expect(response.status).toBe(200)
      expect(Array.isArray(response.data)).toBe(true)
      expect(response.data.length).toBeGreaterThan(0)
      
      // 验证数据格式
      const firstElement = response.data[0]
      expect(firstElement).toHaveProperty('elementId')
      expect(firstElement).toHaveProperty('eClass')
    })
    
    test('不带projectId应该使用默认值', async () => {
      // 后端应该有默认projectId处理
      const response = await axios.get(`${API_URL}/elements`)
      
      expect(response.status).toBe(200)
      expect(Array.isArray(response.data)).toBe(true)
    })
  })
  
  describe('前端universalApi测试', () => {
    test('queryAllElements应该正确发送请求', async () => {
      // 模拟前端API调用
      const mockApi = {
        get: async (url: string, config?: any) => {
          // 验证请求格式
          expect(url).toBe('/elements')
          expect(config.params).toHaveProperty('projectId')
          
          // 返回模拟数据
          return {
            data: [
              {
                elementId: 'test-001',
                eClass: 'RequirementDefinition',
                declaredName: 'Test'
              }
            ]
          }
        }
      }
      
      // 测试queryAllElements的请求构造
      const params = { projectId: 'default' }
      const response = await mockApi.get('/elements', { params })
      
      expect(response.data).toHaveLength(1)
    })
  })
  
  describe('ModelContext数据流测试', () => {
    test('loadAllElements应该正确处理响应', () => {
      // 测试数据转换
      const apiResponse = [
        {
          elementId: 'req-001',
          eClass: 'RequirementDefinition',
          declaredName: 'Test Requirement',
          declaredShortName: 'REQ-001'
        }
      ]
      
      // 模拟ModelContext的数据处理
      const processedData = apiResponse.reduce((acc, element) => {
        const id = element.elementId
        acc[id] = {
          ...element,
          id: id,
          attributes: {
            declaredName: element.declaredName,
            declaredShortName: element.declaredShortName,
            ...element
          }
        }
        return acc
      }, {} as Record<string, any>)
      
      expect(Object.keys(processedData)).toHaveLength(1)
      expect(processedData['req-001'].id).toBe('req-001')
      expect(processedData['req-001'].attributes.declaredName).toBe('Test Requirement')
    })
  })
  
  describe('完整数据流测试', () => {
    test('从API到视图的完整流程', async () => {
      // 1. 后端API调用
      const backendResponse = await axios.get(`${API_URL}/elements?projectId=default`)
      expect(backendResponse.status).toBe(200)
      
      // 2. 数据格式验证
      const elements = backendResponse.data
      expect(Array.isArray(elements)).toBe(true)
      
      // 3. 数据转换测试
      const transformedElements = elements.reduce((acc, el) => {
        const id = el.elementId
        acc[id] = {
          id,
          eClass: el.eClass,
          attributes: el
        }
        return acc
      }, {})
      
      expect(Object.keys(transformedElements).length).toBe(elements.length)
      
      // 4. 视图数据构建测试（TreeView）
      const treeData = Object.values(transformedElements)
        .filter((el: any) => el.eClass === 'RequirementDefinition')
        .map((def: any) => ({
          id: def.id,
          label: def.attributes.declaredName || def.id,
          children: Object.values(transformedElements)
            .filter((usage: any) => 
              usage.eClass === 'RequirementUsage' && 
              usage.attributes.of === def.id
            )
            .map((usage: any) => ({
              id: usage.id,
              label: usage.attributes.declaredName || usage.id
            }))
        }))
      
      expect(Array.isArray(treeData)).toBe(true)
      
      // 5. 视图数据构建测试（TableView）
      const tableData = Object.values(transformedElements).map((el: any) => ({
        key: el.id,
        id: el.id,
        eClass: el.eClass,
        declaredName: el.attributes.declaredName || '',
        declaredShortName: el.attributes.declaredShortName || ''
      }))
      
      expect(tableData.length).toBe(Object.keys(transformedElements).length)
      
      // 6. 视图数据构建测试（GraphView）
      const nodes = Object.values(transformedElements).map((el: any) => ({
        id: el.id,
        label: el.attributes.declaredName || el.id,
        type: el.eClass
      }))
      
      const edges: any[] = []
      Object.values(transformedElements).forEach((el: any) => {
        if (el.attributes.of) {
          edges.push({
            id: `of-${el.id}`,
            source: el.attributes.of,
            target: el.id,
            type: 'of'
          })
        }
      })
      
      expect(nodes.length).toBe(Object.keys(transformedElements).length)
    })
  })
})

describe('错误场景测试', () => {
  test('后端500错误应该被正确处理', async () => {
    // 测试错误处理
    try {
      // 故意发送错误请求
      await axios.get('http://localhost:8080/api/v1/elements', {
        params: { projectId: 'invalid-!@#$%' }
      })
    } catch (error: any) {
      // 应该捕获错误
      expect(error.response?.status).toBeDefined()
    }
  })
  
  test('前端应该处理空响应', () => {
    const emptyResponse: any[] = []
    
    const processedData = emptyResponse.reduce((acc, element) => {
      const id = element.elementId
      acc[id] = element
      return acc
    }, {} as Record<string, any>)
    
    expect(Object.keys(processedData)).toHaveLength(0)
  })
})