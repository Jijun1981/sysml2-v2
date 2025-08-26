/**
 * 高级查询API客户端测试 - TDD第五阶段
 * 
 * 测试覆盖【REQ-D0-3】前端自动加载数据的高级查询功能：
 * - 分页查询
 * - 排序查询  
 * - 过滤查询
 * - 搜索查询
 * - 组合查询
 * - 错误处理
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import axios from 'axios'
import {
  queryAdvanced,
  QueryParams,
  AdvancedQueryResponse,
  SortParam,
  FilterParam
} from '../advancedQueryApi'

// Mock axios
vi.mock('axios')
const mockedAxios = vi.mocked(axios, true)

describe('高级查询API客户端 - REQ-D0-3', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('【REQ-D0-3】基础分页查询', () => {
    it('应该支持默认分页参数', async () => {
      // Given - 模拟后端响应
      const mockResponse = {
        content: [
          { elementId: 'R-001', eClass: 'RequirementDefinition', properties: { declaredName: '测试需求1' } },
          { elementId: 'R-002', eClass: 'RequirementUsage', properties: { declaredName: '测试需求2' } }
        ],
        page: 0,
        size: 50,
        totalElements: 100,
        totalPages: 2,
        first: true,
        last: false
      }
      mockedAxios.get.mockResolvedValue({ data: mockResponse })

      // When - 执行查询
      const result = await queryAdvanced()

      // Then - 验证请求参数和响应
      expect(mockedAxios.get).toHaveBeenCalledWith('http://localhost:8080/api/v1/elements/advanced', {
        params: {
          page: 0,
          size: 50
        }
      })
      expect(result.content).toHaveLength(2)
      expect(result.totalElements).toBe(100)
      expect(result.first).toBe(true)
    })

    it('应该支持自定义分页参数', async () => {
      // Given
      const mockResponse = {
        content: [],
        page: 2,
        size: 20,
        totalElements: 100,
        totalPages: 5,
        first: false,
        last: false
      }
      mockedAxios.get.mockResolvedValue({ data: mockResponse })

      // When
      const params: QueryParams = { page: 2, size: 20 }
      const result = await queryAdvanced(params)

      // Then
      expect(mockedAxios.get).toHaveBeenCalledWith('http://localhost:8080/api/v1/elements/advanced', {
        params: { page: 2, size: 20 }
      })
      expect(result.page).toBe(2)
      expect(result.size).toBe(20)
    })
  })

  describe('【REQ-D0-3】排序查询', () => {
    it('应该支持单字段排序', async () => {
      // Given
      const mockResponse = {
        content: [{ elementId: 'R-001', eClass: 'RequirementDefinition' }],
        sort: { declaredName: 'asc' },
        page: 0,
        size: 50,
        totalElements: 1,
        totalPages: 1
      }
      mockedAxios.get.mockResolvedValue({ data: mockResponse })

      // When
      const sort: SortParam[] = [{ field: 'declaredName', direction: 'asc' }]
      const result = await queryAdvanced({ sort })

      // Then
      expect(mockedAxios.get).toHaveBeenCalledWith('http://localhost:8080/api/v1/elements/advanced', {
        params: {
          page: 0,
          size: 50,
          sort: ['declaredName,asc']
        }
      })
      expect(result.sort).toEqual({ declaredName: 'asc' })
    })

    it('应该支持多字段排序', async () => {
      // Given
      const mockResponse = {
        content: [],
        sort: { eClass: 'asc', declaredName: 'desc' },
        page: 0,
        size: 50,
        totalElements: 0,
        totalPages: 0
      }
      mockedAxios.get.mockResolvedValue({ data: mockResponse })

      // When
      const sort: SortParam[] = [
        { field: 'eClass', direction: 'asc' },
        { field: 'declaredName', direction: 'desc' }
      ]
      const result = await queryAdvanced({ sort })

      // Then
      expect(mockedAxios.get).toHaveBeenCalledWith('http://localhost:8080/api/v1/elements/advanced', {
        params: {
          page: 0,
          size: 50,
          sort: ['eClass,asc', 'declaredName,desc']
        }
      })
      expect(result.sort).toEqual({ eClass: 'asc', declaredName: 'desc' })
    })
  })

  describe('【REQ-D0-3】过滤查询', () => {
    it('应该支持按eClass过滤', async () => {
      // Given
      const mockResponse = {
        content: [
          { elementId: 'R-001', eClass: 'RequirementDefinition', properties: {} }
        ],
        filter: { eClass: 'RequirementDefinition' },
        page: 0,
        size: 50,
        totalElements: 1,
        totalPages: 1
      }
      mockedAxios.get.mockResolvedValue({ data: mockResponse })

      // When
      const filter: FilterParam[] = [{ field: 'eClass', value: 'RequirementDefinition' }]
      const result = await queryAdvanced({ filter })

      // Then
      expect(mockedAxios.get).toHaveBeenCalledWith('http://localhost:8080/api/v1/elements/advanced', {
        params: {
          page: 0,
          size: 50,
          filter: ['eClass:RequirementDefinition']
        }
      })
      expect(result.filter).toEqual({ eClass: 'RequirementDefinition' })
      expect(result.content[0].eClass).toBe('RequirementDefinition')
    })

    it('应该支持多条件过滤', async () => {
      // Given
      const mockResponse = {
        content: [],
        filter: { eClass: 'RequirementUsage', status: 'approved' },
        page: 0,
        size: 50,
        totalElements: 0,
        totalPages: 0
      }
      mockedAxios.get.mockResolvedValue({ data: mockResponse })

      // When
      const filter: FilterParam[] = [
        { field: 'eClass', value: 'RequirementUsage' },
        { field: 'status', value: 'approved' }
      ]
      const result = await queryAdvanced({ filter })

      // Then
      expect(mockedAxios.get).toHaveBeenCalledWith('http://localhost:8080/api/v1/elements/advanced', {
        params: {
          page: 0,
          size: 50,
          filter: ['eClass:RequirementUsage', 'status:approved']
        }
      })
      expect(result.filter).toEqual({ eClass: 'RequirementUsage', status: 'approved' })
    })
  })

  describe('【REQ-D0-3】搜索查询', () => {
    it('应该支持全文搜索', async () => {
      // Given
      const mockResponse = {
        content: [
          { elementId: 'R-001', eClass: 'RequirementDefinition', properties: { declaredName: '电池安全需求' } }
        ],
        search: '电池',
        page: 0,
        size: 50,
        totalElements: 1,
        totalPages: 1
      }
      mockedAxios.get.mockResolvedValue({ data: mockResponse })

      // When
      const result = await queryAdvanced({ search: '电池' })

      // Then
      expect(mockedAxios.get).toHaveBeenCalledWith('http://localhost:8080/api/v1/elements/advanced', {
        params: {
          page: 0,
          size: 50,
          search: '电池'
        }
      })
      expect(result.search).toBe('电池')
      expect(result.content[0].properties.declaredName).toContain('电池')
    })

    it('应该处理空搜索字符串', async () => {
      // Given
      const mockResponse = {
        content: [],
        page: 0,
        size: 50,
        totalElements: 0,
        totalPages: 0
      }
      mockedAxios.get.mockResolvedValue({ data: mockResponse })

      // When
      const result = await queryAdvanced({ search: '' })

      // Then
      expect(mockedAxios.get).toHaveBeenCalledWith('http://localhost:8080/api/v1/elements/advanced', {
        params: {
          page: 0,
          size: 50
          // search参数应该被过滤掉
        }
      })
      expect(result.search).toBeUndefined()
    })
  })

  describe('【REQ-D0-3】组合查询', () => {
    it('应该支持分页+排序+过滤+搜索的组合查询', async () => {
      // Given
      const mockResponse = {
        content: [
          { elementId: 'R-001', eClass: 'RequirementDefinition', properties: { declaredName: '电池管理系统' } }
        ],
        page: 1,
        size: 10,
        sort: { createdAt: 'desc' },
        filter: { eClass: 'RequirementDefinition' },
        search: '电池',
        totalElements: 5,
        totalPages: 1,
        first: false,
        last: true
      }
      mockedAxios.get.mockResolvedValue({ data: mockResponse })

      // When
      const params: QueryParams = {
        page: 1,
        size: 10,
        sort: [{ field: 'createdAt', direction: 'desc' }],
        filter: [{ field: 'eClass', value: 'RequirementDefinition' }],
        search: '电池'
      }
      const result = await queryAdvanced(params)

      // Then
      expect(mockedAxios.get).toHaveBeenCalledWith('http://localhost:8080/api/v1/elements/advanced', {
        params: {
          page: 1,
          size: 10,
          sort: ['createdAt,desc'],
          filter: ['eClass:RequirementDefinition'],
          search: '电池'
        }
      })
      expect(result.page).toBe(1)
      expect(result.sort).toEqual({ createdAt: 'desc' })
      expect(result.filter).toEqual({ eClass: 'RequirementDefinition' })
      expect(result.search).toBe('电池')
    })
  })

  describe('【通用约定】错误处理', () => {
    it('应该处理400参数验证错误', async () => {
      // Given
      const errorResponse = {
        response: {
          status: 400,
          data: {
            error: 'Bad Request',
            message: 'Invalid sort field: invalidField'
          }
        }
      }
      mockedAxios.get.mockRejectedValue(errorResponse)

      // When & Then
      await expect(queryAdvanced({
        sort: [{ field: 'invalidField', direction: 'asc' }]
      })).rejects.toThrow('Invalid sort field: invalidField')
    })

    it('应该处理500服务器错误', async () => {
      // Given
      const errorResponse = {
        response: {
          status: 500,
          data: {
            error: 'Internal Server Error',
            message: 'Query execution failed'
          }
        }
      }
      mockedAxios.get.mockRejectedValue(errorResponse)

      // When & Then
      await expect(queryAdvanced()).rejects.toThrow('Query execution failed')
    })

    it('应该处理网络错误', async () => {
      // Given
      const networkError = new Error('Network Error')
      mockedAxios.get.mockRejectedValue(networkError)

      // When & Then
      await expect(queryAdvanced()).rejects.toThrow('Network Error')
    })
  })

  describe('【通用约定】类型安全', () => {
    it('应该提供完整的TypeScript类型支持', () => {
      // 这个测试主要是编译时验证，确保类型定义正确
      const params: QueryParams = {
        page: 0,
        size: 50,
        sort: [
          { field: 'declaredName', direction: 'asc' },
          { field: 'createdAt', direction: 'desc' }
        ],
        filter: [
          { field: 'eClass', value: 'RequirementDefinition' },
          { field: 'status', value: 'approved' }
        ],
        search: '测试'
      }

      // 验证类型正确性
      expect(params.page).toBeTypeOf('number')
      expect(params.sort![0].direction).toBeTypeOf('string')
      expect(['asc', 'desc']).toContain(params.sort![0].direction)
    })

    it('应该正确映射ElementDTO类型', async () => {
      // Given
      const mockElement = {
        elementId: 'R-001',
        eClass: 'RequirementDefinition',
        properties: {
          declaredName: '测试需求',
          declaredShortName: 'TEST-001',
          reqId: 'REQ-001',
          text: '这是一个测试需求',
          status: 'approved'
        }
      }
      const mockResponse = {
        content: [mockElement],
        page: 0,
        size: 50,
        totalElements: 1,
        totalPages: 1
      }
      mockedAxios.get.mockResolvedValue({ data: mockResponse })

      // When
      const result = await queryAdvanced()

      // Then - 验证返回的数据结构符合ElementDTO
      const element = result.content[0]
      expect(element.elementId).toBe('R-001')
      expect(element.eClass).toBe('RequirementDefinition')
      expect(element.properties).toBeDefined()
      expect(element.properties.declaredName).toBe('测试需求')
    })
  })
})