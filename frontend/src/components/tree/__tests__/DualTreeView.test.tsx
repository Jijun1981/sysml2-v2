/**
 * REQ-UI-4: 双树界面布局测试
 * 
 * 测试点：
 * - 双树布局渲染
 * - Definition数据过滤
 * - Usage数据过滤
 * - 分割线调节
 * - 独立搜索功能
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { ModelProvider } from '../../../contexts/ModelContext'
import DualTreeView from '../DualTreeView'

// Mock ModelContext
const mockModelContext = {
  getTreeViewData: vi.fn(),
  selectedIds: new Set<string>(),
  selectElement: vi.fn(),
  loading: false
}

vi.mock('../../../contexts/ModelContext', () => ({
  ModelProvider: ({ children }: any) => children,
  useModelContext: () => mockModelContext
}))

describe('DualTreeView', () => {
  const mockTreeData = [
    {
      id: 'def-1',
      elementId: 'def-1',
      eClass: 'RequirementDefinition',
      reqId: 'REQ-DEF-001',
      declaredName: 'Performance Definition',
      documentation: 'System performance requirements'
    },
    {
      id: 'def-2',
      elementId: 'def-2',
      eClass: 'RequirementDefinition',
      reqId: 'REQ-DEF-002',
      declaredName: 'Safety Definition',
      documentation: 'Safety requirements'
    },
    {
      id: 'usage-1',
      elementId: 'usage-1',
      eClass: 'RequirementUsage',
      reqId: 'REQ-USE-001',
      declaredName: 'Battery Performance',
      documentation: 'Battery system performance',
      requirementDefinition: 'def-1'
    },
    {
      id: 'usage-2',
      elementId: 'usage-2',
      eClass: 'RequirementUsage',
      reqId: 'REQ-USE-002',
      declaredName: 'Battery Safety',
      documentation: 'Battery safety requirements',
      requirementDefinition: 'def-2'
    },
    {
      id: 'usage-3',
      elementId: 'usage-3',
      eClass: 'RequirementUsage',
      reqId: 'REQ-USE-003',
      declaredName: 'Charging Safety',
      documentation: 'Charging safety requirements',
      requirementDefinition: 'def-2'
    }
  ]

  beforeEach(() => {
    vi.clearAllMocks()
    mockModelContext.getTreeViewData.mockReturnValue(mockTreeData)
    mockModelContext.selectedIds = new Set()
  })

  it('应该渲染双树布局', () => {
    render(<DualTreeView />)

    // 检查两个标题
    expect(screen.getByText('需求定义 (RequirementDefinition)')).toBeInTheDocument()
    expect(screen.getByText('需求使用 (RequirementUsage)')).toBeInTheDocument()
  })

  it('应该在上部只显示RequirementDefinition', () => {
    render(<DualTreeView />)

    // 检查Definition树中的节点
    expect(screen.getByText(/REQ-DEF-001/)).toBeInTheDocument()
    expect(screen.getByText(/REQ-DEF-002/)).toBeInTheDocument()
    
    // Definition应该有蓝色标签
    const defTags = screen.getAllByText('DEF')
    expect(defTags).toHaveLength(2)
  })

  it('应该在下部只显示RequirementUsage', () => {
    render(<DualTreeView />)

    // 检查Usage树中的节点
    expect(screen.getByText(/REQ-USE-001/)).toBeInTheDocument()
    expect(screen.getByText(/REQ-USE-002/)).toBeInTheDocument()
    expect(screen.getByText(/REQ-USE-003/)).toBeInTheDocument()
    
    // Usage应该有紫色标签
    const useTags = screen.getAllByText('USE')
    expect(useTags).toHaveLength(3)
  })

  it('应该支持独立搜索功能', async () => {
    render(<DualTreeView showSearch={true} />)

    // 获取两个搜索框
    const searchInputs = screen.getAllByPlaceholderText(/搜索/)
    expect(searchInputs).toHaveLength(2)

    // 在Definition搜索框中搜索
    const defSearch = screen.getByPlaceholderText('搜索需求定义...')
    fireEvent.change(defSearch, { target: { value: '001' } })

    await waitFor(() => {
      // 应该只显示匹配的Definition
      expect(screen.getByText(/REQ-DEF-001/)).toBeInTheDocument()
      expect(screen.queryByText(/REQ-DEF-002/)).not.toBeInTheDocument()
    })

    // 在Usage搜索框中搜索
    const usageSearch = screen.getByPlaceholderText('搜索需求使用...')
    fireEvent.change(usageSearch, { target: { value: 'Safety' } })

    await waitFor(() => {
      // 应该只显示匹配的Usage
      expect(screen.getByText(/Battery Safety/)).toBeInTheDocument()
      expect(screen.getByText(/Charging Safety/)).toBeInTheDocument()
      expect(screen.queryByText(/Battery Performance/)).not.toBeInTheDocument()
    })
  })

  it('应该处理节点选中事件', () => {
    const onSelect = vi.fn()
    render(<DualTreeView onSelect={onSelect} />)

    // 点击Definition节点
    const defNode = screen.getByText(/REQ-DEF-001/)
    fireEvent.click(defNode)

    expect(onSelect).toHaveBeenCalledWith('def-1', expect.any(Boolean))

    // 点击Usage节点
    const usageNode = screen.getByText(/REQ-USE-001/)
    fireEvent.click(usageNode)

    expect(onSelect).toHaveBeenCalledWith('usage-1', expect.any(Boolean))
  })

  it('应该显示空状态', () => {
    mockModelContext.getTreeViewData.mockReturnValue([])
    render(<DualTreeView />)

    // 应该显示空状态提示
    expect(screen.getByText('暂无需求定义')).toBeInTheDocument()
    expect(screen.getByText('暂无需求使用')).toBeInTheDocument()
  })

  it('应该处理加载状态', () => {
    mockModelContext.loading = true
    render(<DualTreeView />)

    // 应该显示加载指示器
    expect(screen.getByText('加载中...')).toBeInTheDocument()
    
    // 恢复状态
    mockModelContext.loading = false
  })

  it('应该支持分割线调节', () => {
    const { container } = render(<DualTreeView splitPosition={50} />)

    // 找到分割线
    const splitter = container.querySelector('[style*="cursor: ns-resize"]')
    expect(splitter).toBeInTheDocument()

    // 模拟拖动
    fireEvent.mouseDown(splitter!)

    // 检查分割线样式变化（拖动时变蓝）
    expect(splitter).toHaveStyle({ background: expect.stringContaining('#d9d9d9') })
  })

  it('应该正确显示搜索结果的空状态', async () => {
    render(<DualTreeView showSearch={true} />)

    // 在Definition搜索框中搜索不存在的内容
    const defSearch = screen.getByPlaceholderText('搜索需求定义...')
    fireEvent.change(defSearch, { target: { value: 'xyz123' } })

    await waitFor(() => {
      expect(screen.getByText('未找到匹配的需求定义')).toBeInTheDocument()
    })

    // 在Usage搜索框中搜索不存在的内容
    const usageSearch = screen.getByPlaceholderText('搜索需求使用...')
    fireEvent.change(usageSearch, { target: { value: 'abc456' } })

    await waitFor(() => {
      expect(screen.getByText('未找到匹配的需求使用')).toBeInTheDocument()
    })
  })
})