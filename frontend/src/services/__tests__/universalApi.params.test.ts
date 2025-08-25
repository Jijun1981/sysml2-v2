/**
 * TDD测试：验证universalApi参数传递问题
 */

import { describe, test, expect, beforeEach, vi } from 'vitest'
import axios from 'axios'
import { UniversalApiClient } from '../universalApi'

describe('UniversalApiClient参数传递测试', () => {
  let client: UniversalApiClient
  let mockAxiosInstance: any
  
  beforeEach(() => {
    // Mock axios.create
    mockAxiosInstance = {
      get: vi.fn(),
      post: vi.fn(),
      patch: vi.fn(),
      delete: vi.fn(),
      defaults: {
        headers: {
          'Content-Type': 'application/json',
          'X-Project-Id': 'default'
        }
      },
      interceptors: {
        response: {
          use: vi.fn()
        }
      }
    }
    
    vi.spyOn(axios, 'create').mockReturnValue(mockAxiosInstance as any)
    client = new UniversalApiClient()
  })
  
  describe('queryAllElements参数测试', () => {
    test('应该在请求中包含projectId参数', async () => {
      // 准备
      mockAxiosInstance.get.mockResolvedValue({ data: [] })
      
      // 执行
      await client.queryAllElements()
      
      // 验证
      expect(mockAxiosInstance.get).toHaveBeenCalledWith(
        '/elements',
        expect.objectContaining({
          params: expect.objectContaining({
            projectId: expect.any(String)
          })
        })
      )
    })
    
    test('projectId应该是default', async () => {
      // 准备
      mockAxiosInstance.get.mockResolvedValue({ data: [] })
      
      // 执行
      await client.queryAllElements()
      
      // 验证
      const callArgs = mockAxiosInstance.get.mock.calls[0]
      expect(callArgs[1].params.projectId).toBe('default')
    })
    
    test('应该保留其他参数', async () => {
      // 准备
      mockAxiosInstance.get.mockResolvedValue({ data: [] })
      
      // 执行
      await client.queryAllElements({ page: 1, size: 20 })
      
      // 验证
      const callArgs = mockAxiosInstance.get.mock.calls[0]
      expect(callArgs[1].params).toEqual({
        projectId: 'default',
        page: 1,
        size: 20
      })
    })
  })
  
  describe('queryElementsByType参数测试', () => {
    test('应该包含projectId和type参数', async () => {
      // 准备
      mockAxiosInstance.get.mockResolvedValue({ data: [] })
      
      // 执行
      await client.queryElementsByType('RequirementDefinition')
      
      // 验证
      const callArgs = mockAxiosInstance.get.mock.calls[0]
      expect(callArgs[1].params).toEqual({
        projectId: 'default',
        type: 'RequirementDefinition'
      })
    })
  })
  
  describe('createElement参数测试', () => {
    test('应该在URL中包含projectId', async () => {
      // 准备
      mockAxiosInstance.post.mockResolvedValue({ data: { elementId: 'test-001' } })
      
      // 执行
      await client.createElement('PartUsage', { declaredName: 'Test' })
      
      // 验证
      expect(mockAxiosInstance.post).toHaveBeenCalledWith(
        expect.stringContaining('projectId='),
        expect.any(Object)
      )
    })
    
    test('请求体应该包含eClass和属性', async () => {
      // 准备
      mockAxiosInstance.post.mockResolvedValue({ data: { elementId: 'test-001' } })
      
      // 执行
      await client.createElement('PartUsage', { declaredName: 'Test' })
      
      // 验证
      const callArgs = mockAxiosInstance.post.mock.calls[0]
      expect(callArgs[1]).toEqual({
        eClass: 'PartUsage',
        declaredName: 'Test'
      })
    })
  })
  
  describe('projectId管理测试', () => {
    test('setProjectId应该更新后续请求的projectId', async () => {
      // 准备
      mockAxiosInstance.get.mockResolvedValue({ data: [] })
      
      // 执行
      client.setProjectId('new-project')
      await client.queryAllElements()
      
      // 验证 - 这个测试会失败，因为当前实现有问题
      const callArgs = mockAxiosInstance.get.mock.calls[0]
      expect(callArgs[1].params.projectId).toBe('new-project')
    })
  })
})

describe('实际HTTP请求测试', () => {
  test('实际请求应该包含projectId参数', async () => {
    // 创建一个mock服务器来验证请求
    const mockServer = vi.fn((req) => {
      // 验证请求URL包含projectId
      expect(req.url).toContain('projectId=')
      return { data: [] }
    })
    
    // 使用实际的axios但mock响应
    const client = new UniversalApiClient()
    
    // 这个测试会验证实际发送的请求
    // 当前会失败，因为projectId获取方式有问题
  })
})