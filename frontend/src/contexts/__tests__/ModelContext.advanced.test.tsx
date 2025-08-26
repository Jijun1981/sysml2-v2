/**
 * ModelContext高级查询集成测试 - TDD第五阶段
 * 
 * 测试覆盖：
 * - 高级查询API集成
 * - 分页、排序、过滤功能
 * - 错误处理和重试
 * - SSOT原则保持
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { renderHook, act } from '@testing-library/react'
import React from 'react'
import { ModelProvider, useModelContext } from '../ModelContext'
import type { AdvancedQueryResponse } from '../../services/advancedQueryApi'

// Mock高级查询API
vi.mock('../../services/advancedQueryApi', () => ({
  queryAdvanced: vi.fn(),
  queryByType: vi.fn(),
  searchRequirements: vi.fn(),
  getApprovedRequirements: vi.fn(),
  AdvancedQueryError: class extends Error {
    constructor(message: string, public statusCode?: number) {
      super(message)
      this.name = 'AdvancedQueryError'
    }
  }
}))

const { queryAdvanced, queryByType, searchRequirements, getApprovedRequirements, AdvancedQueryError } = 
  await import('../../services/advancedQueryApi')

// Mock数据
const mockElements = [
  {
    elementId: 'R-001',
    eClass: 'RequirementDefinition',
    properties: {
      declaredName: '电池系统需求',
      declaredShortName: 'BATTERY-001',
      reqId: 'REQ-001',
      text: '电池系统应当提供可靠的电力供应'
    }
  },
  {
    elementId: 'U-001',
    eClass: 'RequirementUsage',
    properties: {
      of: 'R-001',
      declaredName: '电池使用实例',
      status: 'approved'
    }
  }
]

const mockAdvancedQueryResponse: AdvancedQueryResponse = {
  content: mockElements,
  page: 0,
  size: 50,
  totalElements: 2,
  totalPages: 1,
  first: true,
  last: true
}

const wrapper = ({ children }: { children: React.ReactNode }) => (
  <ModelProvider>{children}</ModelProvider>
)

describe('ModelContext高级查询集成', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(queryAdvanced).mockResolvedValue(mockAdvancedQueryResponse)
  })

  describe('【基础查询】高级查询API替换', () => {
    it('应该使用高级查询API加载所有元素', async () => {
      const { result } = renderHook(() => useModelContext(), { wrapper })

      expect(vi.mocked(queryAdvanced)).toHaveBeenCalledWith({
        page: 0,
        size: 50
      })

      // 等待初始加载完成
      await act(async () => {
        await new Promise(resolve => setTimeout(resolve, 0))
      })

      expect(result.current.elements).toHaveProperty('R-001')
      expect(result.current.elements).toHaveProperty('U-001')
      expect(Object.keys(result.current.elements)).toHaveLength(2)
    })

    it('应该使用类型过滤加载特定类型元素', async () => {
      vi.mocked(queryByType).mockResolvedValue({
        ...mockAdvancedQueryResponse,
        content: [mockElements[0]] // 只返回RequirementDefinition
      })

      const { result } = renderHook(() => useModelContext(), { wrapper })

      await act(async () => {
        await result.current.loadElementsByType('RequirementDefinition')
      })

      expect(vi.mocked(queryByType)).toHaveBeenCalledWith('RequirementDefinition', {})
      expect(result.current.elements['R-001']).toBeDefined()
      expect(result.current.elements['R-001'].eClass).toBe('RequirementDefinition')
    })
  })

  describe('【分页查询】分页和排序功能', () => {
    it('应该支持分页查询', async () => {
      const { result } = renderHook(() => useModelContext(), { wrapper })

      await act(async () => {
        await result.current.loadAllElements({
          page: 1,
          size: 20
        })
      })

      expect(vi.mocked(queryAdvanced)).toHaveBeenCalledWith({
        page: 1,
        size: 20
      })
    })

    it('应该支持排序查询', async () => {
      const { result } = renderHook(() => useModelContext(), { wrapper })

      await act(async () => {
        await result.current.loadAllElements({
          sort: [
            { field: 'declaredName', direction: 'asc' },
            { field: 'createdAt', direction: 'desc' }
          ]
        })
      })

      expect(vi.mocked(queryAdvanced)).toHaveBeenCalledWith({
        page: 0,
        size: 50,
        sort: [
          { field: 'declaredName', direction: 'asc' },
          { field: 'createdAt', direction: 'desc' }
        ]
      })
    })

    it('应该支持过滤查询', async () => {
      const { result } = renderHook(() => useModelContext(), { wrapper })

      await act(async () => {
        await result.current.loadAllElements({
          filter: [
            { field: 'status', value: 'approved' },
            { field: 'eClass', value: 'RequirementDefinition' }
          ]
        })
      })

      expect(vi.mocked(queryAdvanced)).toHaveBeenCalledWith({
        page: 0,
        size: 50,
        filter: [
          { field: 'status', value: 'approved' },
          { field: 'eClass', value: 'RequirementDefinition' }
        ]
      })
    })

    it('应该支持搜索查询', async () => {
      const { result } = renderHook(() => useModelContext(), { wrapper })

      await act(async () => {
        await result.current.loadAllElements({
          search: '电池'
        })
      })

      expect(vi.mocked(queryAdvanced)).toHaveBeenCalledWith({
        page: 0,
        size: 50,
        search: '电池'
      })
    })
  })

  describe('【便利方法】特定查询快捷方法', () => {
    it('应该提供搜索需求的便利方法', async () => {
      vi.mocked(searchRequirements).mockResolvedValue(mockAdvancedQueryResponse)

      const { result } = renderHook(() => useModelContext(), { wrapper })

      await act(async () => {
        await result.current.searchRequirements('电池系统')
      })

      expect(vi.mocked(searchRequirements)).toHaveBeenCalledWith('电池系统', {})
    })

    it('应该提供获取已批准需求的便利方法', async () => {
      vi.mocked(getApprovedRequirements).mockResolvedValue(mockAdvancedQueryResponse)

      const { result } = renderHook(() => useModelContext(), { wrapper })

      await act(async () => {
        await result.current.getApprovedRequirements()
      })

      expect(vi.mocked(getApprovedRequirements)).toHaveBeenCalledWith({})
    })
  })

  describe('【错误处理】高级查询错误处理', () => {
    it('应该正确处理网络错误', async () => {
      // 创建网络错误实例但不直接抛出
      const errorMessage = 'Network Error: Unable to connect to server'
      const errorInstance = { message: errorMessage, statusCode: 0, name: 'AdvancedQueryError' }
      vi.mocked(queryAdvanced).mockRejectedValue(errorInstance)

      const { result } = renderHook(() => useModelContext(), { wrapper })

      await act(async () => {
        try {
          await result.current.loadAllElements()
        } catch (error) {
          // 错误由上级处理
        }
      })

      expect(result.current.error?.message).toContain('Network Error')
    })

    it('应该正确处理API错误', async () => {
      // 创建API错误实例但不直接抛出
      const errorMessage = '查询失败：无效的排序字段'
      const errorInstance = { message: errorMessage, statusCode: 400, name: 'AdvancedQueryError' }
      vi.mocked(queryAdvanced).mockRejectedValue(errorInstance)

      const { result } = renderHook(() => useModelContext(), { wrapper })

      await act(async () => {
        try {
          await result.current.loadAllElements()
        } catch (error) {
          // 错误由上级处理
        }
      })

      expect(result.current.error?.message).toBe('查询失败：无效的排序字段')
    })
  })

  describe('【SSOT原则】数据一致性保持', () => {
    it('应该将ElementDTO转换为SSOT格式', async () => {
      const { result } = renderHook(() => useModelContext(), { wrapper })

      await act(async () => {
        await new Promise(resolve => setTimeout(resolve, 0))
      })

      const element = result.current.elements['R-001']
      expect(element).toBeDefined()
      expect(element.id).toBe('R-001')
      expect(element.eClass).toBe('RequirementDefinition')
      expect(element.attributes.declaredName).toBe('电池系统需求')
      expect(element.attributes.declaredShortName).toBe('BATTERY-001')
    })

    it('应该保持视图投影方法正常工作', async () => {
      const { result } = renderHook(() => useModelContext(), { wrapper })

      await act(async () => {
        await new Promise(resolve => setTimeout(resolve, 0))
      })

      const treeData = result.current.getTreeViewData()
      expect(treeData.definitions).toHaveLength(1)
      expect(treeData.definitions[0].label).toBe('电池系统需求')
      expect(treeData.definitions[0].usages).toHaveLength(1)

      const tableData = result.current.getTableViewData()
      expect(tableData).toHaveLength(2)
      expect(tableData[0].declaredName).toBe('电池系统需求')

      const graphData = result.current.getGraphViewData()
      expect(graphData.nodes).toHaveLength(2)
      expect(graphData.edges).toHaveLength(1)
      expect(graphData.edges[0].type).toBe('of')
    })
  })

  describe('【分页状态】分页信息管理', () => {
    it('应该提供分页状态信息', async () => {
      const paginatedResponse: AdvancedQueryResponse = {
        ...mockAdvancedQueryResponse,
        page: 1,
        size: 10,
        totalElements: 25,
        totalPages: 3,
        first: false,
        last: false
      }
      vi.mocked(queryAdvanced).mockResolvedValue(paginatedResponse)

      const { result } = renderHook(() => useModelContext(), { wrapper })

      await act(async () => {
        await result.current.loadAllElements({ page: 1, size: 10 })
      })

      expect(result.current.pagination).toBeDefined()
      expect(result.current.pagination.page).toBe(1)
      expect(result.current.pagination.size).toBe(10)
      expect(result.current.pagination.totalElements).toBe(25)
      expect(result.current.pagination.totalPages).toBe(3)
      expect(result.current.pagination.first).toBe(false)
      expect(result.current.pagination.last).toBe(false)
    })
  })
})