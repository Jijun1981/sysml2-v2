/**
 * TableView组件测试 - TDD第六阶段（简化版）
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import React from 'react'
import TableView from '../TableView'

// Mock数据
const mockTableData = [
  {
    id: 'R-001',
    eClass: 'RequirementDefinition',
    declaredShortName: 'REQ-001',
    declaredName: '电池系统需求',
    status: 'approved'
  }
]

const mockUseModelContext = {
  elements: {},
  selectedIds: new Set<string>(),
  loading: false,
  error: null,
  pagination: { page: 0, size: 10, totalElements: 1, totalPages: 1, first: true, last: true },
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
  getTreeViewData: vi.fn(),
  getTableViewData: vi.fn().mockReturnValue(mockTableData),
  getGraphViewData: vi.fn(),
  setProjectId: vi.fn(),
  refreshProject: vi.fn()
}

// Mock ModelContext
vi.mock('../../../contexts/ModelContext', () => ({
  useModelContext: () => mockUseModelContext
}))

// Mock antd Table to avoid jsdom issues
vi.mock('antd', async () => {
  const actual = await vi.importActual('antd')
  return {
    ...actual,
    Table: ({ dataSource, columns, ...props }: any) => (
      <div role="table" {...props}>
        <div>需求数据表格</div>
        {dataSource?.map((item: any) => (
          <div key={item.id}>{item.declaredName}</div>
        ))}
      </div>
    )
  }
})

describe('TableView组件基础功能', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('应该渲染表格组件', () => {
    render(<TableView />)
    
    expect(screen.getByText('需求数据表格')).toBeInTheDocument()
    expect(screen.getByRole('table')).toBeInTheDocument()
  })

  it('应该显示表格数据', () => {
    render(<TableView />)
    
    expect(screen.getByText('电池系统需求')).toBeInTheDocument()
  })

  it('应该显示统计信息', () => {
    render(<TableView />)
    
    expect(screen.getByText(/共 1 条记录/)).toBeInTheDocument()
  })

  it('应该提供搜索框', () => {
    render(<TableView searchable />)
    
    expect(screen.getByPlaceholderText('搜索需求...')).toBeInTheDocument()
  })

  it('应该处理加载状态', () => {
    mockUseModelContext.loading = true
    render(<TableView />)
    
    // Table组件会处理loading状态
    expect(screen.getByRole('table')).toHaveAttribute('loading', 'true')
  })

  it('应该处理错误状态', () => {
    mockUseModelContext.loading = false
    mockUseModelContext.error = new Error('加载失败')
    render(<TableView />)
    
    expect(screen.getByText(/加载失败/)).toBeInTheDocument()
  })

  it('应该处理空数据状态', () => {
    mockUseModelContext.getTableViewData.mockReturnValue([])
    render(<TableView />)
    
    // 表格组件应该存在但没有数据行
    expect(screen.getByRole('table')).toBeInTheDocument()
  })
})