import { describe, test, expect, beforeEach, vi } from 'vitest'
import { render, act, renderHook } from '@testing-library/react'
import { ModelProvider, useModelContext } from '../ModelContext'
import { createUniversalElement, queryElementsByType } from '../../services/universalApi'

// Mock the universal API
vi.mock('../../services/universalApi', () => ({
  createUniversalElement: vi.fn(),
  queryElementsByType: vi.fn(),
  queryAllElements: vi.fn(),
  getElementById: vi.fn(),
  updateUniversalElement: vi.fn(),
  deleteUniversalElement: vi.fn(),
  validateStatic: vi.fn(),
  checkUniversalHealth: vi.fn(),
  setProjectId: vi.fn(),
  handleUniversalApiError: vi.fn(),
  default: {
    createElement: vi.fn(),
    queryElementsByType: vi.fn(),
    queryAllElements: vi.fn(),
    getElementById: vi.fn(),
    updateElement: vi.fn(),
    deleteElement: vi.fn(),
    validateStatic: vi.fn(),
    checkHealth: vi.fn(),
    setProjectId: vi.fn()
  },
  // Export types for compatibility
  ElementData: {},
  QueryResponse: {},
  ApiError: {}
}))

const mockCreateElement = vi.mocked(createUniversalElement)
const mockQueryElements = vi.mocked(queryElementsByType)

describe('ModelContext SSOT', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  const TestWrapper = ({ children }: { children: React.ReactNode }) => (
    <ModelProvider projectId="test-project">{children}</ModelProvider>
  )

  test('should initialize with empty state', () => {
    // Arrange & Act
    const { result } = renderHook(() => useModelContext(), {
      wrapper: TestWrapper
    })

    // Assert
    expect(result.current.elements).toEqual({})
    expect(result.current.loading).toBe(false)
    expect(result.current.error).toBeNull()
  })

  test('should sync data across views when element created', async () => {
    // Arrange
    const mockElement = {
      id: 'def-001',
      eClass: 'RequirementDefinition',
      attributes: {
        declaredShortName: 'EBS-L1-001',
        declaredName: '电池系统性能需求'
      }
    }
    
    mockCreateElement.mockResolvedValueOnce(mockElement)

    const { result } = renderHook(() => useModelContext(), {
      wrapper: TestWrapper
    })

    // Act
    await act(async () => {
      await result.current.createElement('RequirementDefinition', {
        declaredShortName: 'EBS-L1-001',
        declaredName: '电池系统性能需求'
      })
    })

    // Assert
    expect(result.current.elements['def-001']).toEqual(mockElement)
    expect(mockCreateElement).toHaveBeenCalledWith('RequirementDefinition', {
      declaredShortName: 'EBS-L1-001',
      declaredName: '电池系统性能需求'
    })
  })

  test('should update all views when element modified', async () => {
    // Arrange
    const initialElement = {
      id: 'def-001',
      eClass: 'RequirementDefinition',
      attributes: {
        declaredShortName: 'EBS-L1-001',
        declaredName: '原始名称'
      }
    }

    const updatedElement = {
      ...initialElement,
      attributes: {
        ...initialElement.attributes,
        declaredName: '更新后的名称'
      }
    }

    const { result } = renderHook(() => useModelContext(), {
      wrapper: TestWrapper
    })

    // 设置初始状态
    await act(async () => {
      result.current.setElements({ 'def-001': initialElement })
    })

    // Act
    await act(async () => {
      result.current.updateElement('def-001', {
        declaredName: '更新后的名称'
      })
    })

    // Assert
    expect(result.current.elements['def-001'].attributes.declaredName).toBe('更新后的名称')
  })

  test('should maintain single source of truth across different view types', async () => {
    // Arrange
    const mockElements = [
      {
        id: 'def-001',
        eClass: 'RequirementDefinition',
        attributes: { declaredName: '需求定义1' }
      },
      {
        id: 'usage-001',
        eClass: 'RequirementUsage',
        attributes: { declaredName: '需求使用1', of: 'def-001' }
      }
    ]

    mockQueryElements.mockResolvedValueOnce({
      data: mockElements,
      meta: { total: 2, page: 0, size: 50 }
    })

    const { result } = renderHook(() => useModelContext(), {
      wrapper: TestWrapper
    })

    // Act - 从不同视图类型查询数据
    await act(async () => {
      await result.current.loadElementsByType('RequirementDefinition')
    })

    await act(async () => {
      await result.current.loadElementsByType('RequirementUsage')
    })

    // Assert - 验证SSOT：相同元素在不同视图中保持一致
    expect(result.current.elements['def-001']).toBeDefined()
    expect(result.current.elements['usage-001']).toBeDefined()
    expect(result.current.elements['usage-001'].attributes.of).toBe('def-001')
    
    // 验证引用关系一致性
    const definition = result.current.elements['def-001']
    const usage = result.current.elements['usage-001']
    expect(usage.attributes.of).toBe(definition.id)
  })

  test('should handle concurrent updates correctly', async () => {
    // Arrange
    const { result } = renderHook(() => useModelContext(), {
      wrapper: TestWrapper
    })

    const element = {
      id: 'def-001',
      eClass: 'RequirementDefinition',
      attributes: { declaredName: '初始名称' }
    }

    await act(async () => {
      result.current.setElements({ 'def-001': element })
    })

    // Act - 模拟并发更新
    const update1Promise = act(async () => {
      result.current.updateElement('def-001', { declaredName: '更新1' })
    })

    const update2Promise = act(async () => {
      result.current.updateElement('def-001', { declaredName: '更新2' })
    })

    await Promise.all([update1Promise, update2Promise])

    // Assert - 验证最终状态一致性
    expect(result.current.elements['def-001'].attributes.declaredName).toMatch(/更新[12]/)
  })

  test('should provide view-specific data projections while maintaining SSOT', () => {
    // Arrange
    const mockElements = {
      'def-001': {
        id: 'def-001',
        eClass: 'RequirementDefinition',
        attributes: { declaredName: '定义1' }
      },
      'usage-001': {
        id: 'usage-001',
        eClass: 'RequirementUsage', 
        attributes: { declaredName: '使用1', of: 'def-001' }
      },
      'satisfy-001': {
        id: 'satisfy-001',
        eClass: 'Satisfy',
        attributes: { source: 'part-001', target: 'usage-001' }
      }
    }

    const { result } = renderHook(() => useModelContext(), {
      wrapper: TestWrapper
    })

    act(() => {
      result.current.setElements(mockElements)
    })

    // Act & Assert - 测试不同视图的数据投影
    const treeData = result.current.getTreeViewData()
    expect(treeData.definitions).toHaveLength(1)
    expect(treeData.definitions[0].usages).toHaveLength(1)

    const tableData = result.current.getTableViewData()
    expect(tableData).toHaveLength(3) // 所有元素

    const graphData = result.current.getGraphViewData()
    expect(graphData.nodes).toHaveLength(4) // 包括part-001
    expect(graphData.edges).toHaveLength(2) // of关系 + satisfy关系
  })

  test('should handle loading states properly', async () => {
    // Arrange
    let resolveQuery: (value: any) => void
    const queryPromise = new Promise(resolve => {
      resolveQuery = resolve
    })
    mockQueryElements.mockReturnValueOnce(queryPromise)

    const { result } = renderHook(() => useModelContext(), {
      wrapper: TestWrapper
    })

    // Act
    const loadPromise = act(async () => {
      result.current.loadElementsByType('RequirementDefinition')
    })

    // Assert - 加载中状态
    expect(result.current.loading).toBe(true)

    // 完成加载
    resolveQuery({ data: [], meta: { total: 0 } })
    await loadPromise

    // Assert - 加载完成状态
    expect(result.current.loading).toBe(false)
  })

  test('should handle error states gracefully', async () => {
    // Arrange
    const error = new Error('API Error')
    mockQueryElements.mockRejectedValueOnce(error)

    const { result } = renderHook(() => useModelContext(), {
      wrapper: TestWrapper
    })

    // Act
    await act(async () => {
      try {
        await result.current.loadElementsByType('RequirementDefinition')
      } catch {
        // Expected to fail
      }
    })

    // Assert
    expect(result.current.error).toBe(error)
    expect(result.current.loading).toBe(false)
  })
})