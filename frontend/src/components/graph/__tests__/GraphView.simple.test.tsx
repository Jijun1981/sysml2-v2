/**
 * GraphView组件测试 - TDD第六阶段（简化版）
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import React from 'react'
import GraphView from '../GraphView'

// Mock数据
const mockGraphData = {
  nodes: [
    {
      id: 'R-001',
      position: { x: 100, y: 100 },
      data: {
        label: '电池系统需求',
        type: 'RequirementDefinition',
        status: 'approved'
      }
    }
  ],
  edges: [
    {
      id: 'e1',
      source: 'R-001',
      target: 'U-001',
      type: 'contain',
      label: '包含'
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
  getTreeViewData: vi.fn(),
  getTableViewData: vi.fn(),
  getGraphViewData: vi.fn().mockReturnValue(mockGraphData),
  setProjectId: vi.fn(),
  refreshProject: vi.fn()
}

// Mock ModelContext
vi.mock('../../../contexts/ModelContext', () => ({
  useModelContext: () => mockUseModelContext
}))

// 简化的ReactFlow Mock
vi.mock('reactflow', () => ({
  default: ({ children, nodes }: any) => (
    <div role="application" aria-label="flow-chart">
      <div>需求依赖关系图</div>
      {nodes?.map((node: any) => (
        <div key={node.id} className="node">
          {node.data.label}
        </div>
      ))}
      {children}
    </div>
  ),
  Controls: () => <div>Controls</div>,
  Background: () => <div>Background</div>,
  MiniMap: () => <div>MiniMap</div>,
  MarkerType: { ArrowClosed: 'arrowclosed' },
  Position: { Top: 'top', Bottom: 'bottom' },
  useNodesState: () => [mockGraphData.nodes, vi.fn(), vi.fn()],
  useEdgesState: () => [mockGraphData.edges, vi.fn(), vi.fn()],
  Handle: () => null
}))

describe('GraphView组件基础功能', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('应该渲染图视图组件', () => {
    render(<GraphView />)
    
    expect(screen.getAllByText('需求依赖关系图')[0]).toBeInTheDocument()
    expect(screen.getByRole('application', { name: 'flow-chart' })).toBeInTheDocument()
  })

  it('应该显示节点', () => {
    render(<GraphView />)
    
    expect(screen.getByText('电池系统需求')).toBeInTheDocument()
  })

  it('应该提供布局按钮', () => {
    render(<GraphView />)
    
    expect(screen.getByText('自动布局')).toBeInTheDocument()
  })

  it('应该处理加载状态', () => {
    mockUseModelContext.loading = true
    render(<GraphView />)
    
    expect(screen.getByText('加载中...')).toBeInTheDocument()
  })

  it('应该处理错误状态', () => {
    mockUseModelContext.loading = false
    mockUseModelContext.error = new Error('加载失败')
    render(<GraphView />)
    
    expect(screen.getByText(/加载失败/)).toBeInTheDocument()
  })

  it('应该处理空数据状态', () => {
    mockUseModelContext.loading = false
    mockUseModelContext.error = null
    mockUseModelContext.getGraphViewData.mockReturnValue({ nodes: [], edges: [] })
    render(<GraphView />)
    
    expect(screen.getByText('暂无数据')).toBeInTheDocument()
  })

  it('应该提供缩放控制', () => {
    mockUseModelContext.loading = false
    mockUseModelContext.error = null
    mockUseModelContext.getGraphViewData.mockReturnValue(mockGraphData)
    render(<GraphView />)
    
    expect(screen.getByText('放大')).toBeInTheDocument()
    expect(screen.getByText('缩小')).toBeInTheDocument()
    expect(screen.getByText('适应视图')).toBeInTheDocument()
  })
})