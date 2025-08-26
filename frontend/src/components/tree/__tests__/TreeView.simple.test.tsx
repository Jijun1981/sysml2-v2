/**
 * TreeView组件测试 - TDD第六阶段（简化版）
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import React from 'react'
import TreeView from '../TreeView'

// Mock ModelContext
const mockTreeData = {
  definitions: [
    {
      id: 'R-001',
      label: '电池系统需求',
      type: 'definition' as const,
      children: [],
      usages: [
        {
          id: 'U-001',
          label: '电池使用实例1',
          type: 'usage' as const
        }
      ]
    }
  ]
}

const mockUseModelContext = {
  elements: {},
  selectedIds: new Set<string>(),
  loading: false,
  error: null,
  pagination: { page: 0, size: 50, totalElements: 1, totalPages: 1, first: true, last: true },
  createElement: vi.fn(),
  updateElement: vi.fn(),
  deleteElement: vi.fn(),
  loadElementsByType: vi.fn(),
  loadAllElements: vi.fn(),
  searchRequirements: vi.fn(),
  getApprovedRequirements: vi.fn(),
  selectElement: vi.fn(),
  clearSelection: vi.fn(),
  setElements: vi.fn(),
  setLoading: vi.fn(),
  setError: vi.fn(),
  getTreeViewData: vi.fn().mockReturnValue(mockTreeData),
  getTableViewData: vi.fn(),
  getGraphViewData: vi.fn(),
  setProjectId: vi.fn(),
  refreshProject: vi.fn()
}

// Mock the entire ModelContext module
vi.mock('../../../contexts/ModelContext', () => ({
  useModelContext: () => mockUseModelContext
}))

describe('TreeView组件基础功能', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('应该渲染树视图组件', () => {
    render(<TreeView />)
    
    expect(screen.getByText('需求层次结构')).toBeInTheDocument()
    expect(screen.getByRole('tree')).toBeInTheDocument()
  })

  it('应该显示树节点', () => {
    render(<TreeView />)
    
    expect(screen.getByText('电池系统需求')).toBeInTheDocument()
  })

  it('应该支持搜索功能', () => {
    render(<TreeView searchable />)
    
    expect(screen.getByPlaceholderText('搜索需求...')).toBeInTheDocument()
  })

  it('应该处理加载状态', () => {
    mockUseModelContext.loading = true
    render(<TreeView />)
    
    expect(screen.getByText('加载中...')).toBeInTheDocument()
  })

  it('应该处理错误状态', () => {
    mockUseModelContext.loading = false
    mockUseModelContext.error = new Error('加载失败')
    render(<TreeView />)
    
    expect(screen.getByText(/加载失败/)).toBeInTheDocument()
  })
})