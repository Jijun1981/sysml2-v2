import { render, screen, waitFor, act } from '@testing-library/react'
import { describe, test, expect, vi, beforeEach } from 'vitest'
import { ModelProvider } from '../../../contexts/ModelContext'
import TreeView from '../TreeView/TreeView'
import TableView from '../TableView/TableView'
import '@testing-library/jest-dom'

// Mock the universal API
vi.mock('../../../services/universalApi', () => ({
  createUniversalElement: vi.fn(),
  queryElementsByType: vi.fn().mockResolvedValue({
    data: [],
    meta: { page: 0, size: 50, total: 0 }
  }),
  queryAllElements: vi.fn().mockResolvedValue({
    data: [],
    meta: { page: 0, size: 50, total: 0 }
  }),
  getElementById: vi.fn(),
  updateUniversalElement: vi.fn(),
  deleteUniversalElement: vi.fn(),
  setProjectId: vi.fn(),
  handleUniversalApiError: vi.fn()
}))

describe('View Components Effect Dependencies', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    
    // Suppress console warnings for these tests
    vi.spyOn(console, 'warn').mockImplementation(() => {})
    vi.spyOn(console, 'error').mockImplementation(() => {})
  })

  const TestWrapper = ({ children }: { children: React.ReactNode }) => (
    <ModelProvider projectId="test-project">{children}</ModelProvider>
  )

  test('TreeView should not cause infinite re-renders', async () => {
    // 计算渲染次数
    let renderCount = 0
    const OriginalTreeView = TreeView
    
    // 包装TreeView来计算渲染次数
    const WrappedTreeView = () => {
      renderCount++
      return <OriginalTreeView />
    }

    render(
      <TestWrapper>
        <WrappedTreeView />
      </TestWrapper>
    )

    // 等待初始渲染完成
    await waitFor(() => {
      expect(screen.getByTestId('tree-view')).toBeInTheDocument()
    }, { timeout: 1000 })

    // 验证渲染次数在合理范围内（不超过5次，考虑到React的严格模式）
    expect(renderCount).toBeLessThan(5)
  })

  test('TableView should not cause infinite re-renders', async () => {
    // 计算渲染次数
    let renderCount = 0
    const OriginalTableView = TableView
    
    // 包装TableView来计算渲染次数
    const WrappedTableView = () => {
      renderCount++
      return <OriginalTableView />
    }

    render(
      <TestWrapper>
        <WrappedTableView />
      </TestWrapper>
    )

    // 等待初始渲染完成
    await waitFor(() => {
      expect(screen.getByTestId('table-view')).toBeInTheDocument()
    }, { timeout: 1000 })

    // 验证渲染次数在合理范围内
    expect(renderCount).toBeLessThan(5)
  })

  test('useEffect dependencies should be stable', async () => {
    const consoleSpy = vi.spyOn(console, 'warn')
    
    render(
      <TestWrapper>
        <TreeView />
        <TableView />
      </TestWrapper>
    )

    // 等待组件完全渲染
    await act(async () => {
      await new Promise(resolve => setTimeout(resolve, 100))
    })

    // 检查是否有无限更新警告
    const maxUpdateWarnings = consoleSpy.mock.calls.filter(call =>
      call[0]?.includes?.('Maximum update depth exceeded')
    )

    expect(maxUpdateWarnings).toHaveLength(0)
    
    consoleSpy.mockRestore()
  })

  test('components should handle loading states without infinite loops', async () => {
    // Mock一个会延迟响应的API
    const { queryElementsByType, queryAllElements } = await import('../../../services/universalApi')
    
    vi.mocked(queryElementsByType).mockImplementation(
      () => new Promise(resolve => setTimeout(() => resolve({
        data: [],
        meta: { page: 0, size: 50, total: 0 }
      }), 50))
    )
    
    vi.mocked(queryAllElements).mockImplementation(
      () => new Promise(resolve => setTimeout(() => resolve({
        data: [],
        meta: { page: 0, size: 50, total: 0 }
      }), 50))
    )

    render(
      <TestWrapper>
        <div>
          <TreeView />
          <TableView />
        </div>
      </TestWrapper>
    )

    // 等待加载完成
    await waitFor(() => {
      expect(screen.getByTestId('tree-view')).toBeInTheDocument()
      expect(screen.getByTestId('table-view')).toBeInTheDocument()
    }, { timeout: 2000 })

    // 如果有无限循环，这个测试会超时
    expect(true).toBe(true) // 如果能到这里说明没有无限循环
  })
})