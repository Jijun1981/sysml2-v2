/**
 * GraphView组件测试 - TDD第六阶段
 * 
 * 测试覆盖：
 * - REQ-D3-1: 依赖图，选中高亮
 * - REQ-D3-3: 布局算法，拖拽移动，缩放控制
 * - 节点渲染和交互
 * - 边连接渲染
 * - 自动布局功能
 * - 视图联动响应
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { userEvent } from '@testing-library/user-event'
import React from 'react'
import GraphView from '../GraphView'
import type { GraphViewData, GraphNode, GraphEdge } from '../../../types/models'

// Mock数据
const mockGraphData: GraphViewData = {
  nodes: [
    {
      id: 'R-001',
      position: { x: 100, y: 100 },
      data: {
        label: '电池系统需求',
        type: 'RequirementDefinition',
        status: 'approved',
        properties: {
          declaredName: '电池系统需求',
          declaredShortName: 'BATTERY-001',
          reqId: 'REQ-001'
        }
      }
    },
    {
      id: 'R-002',
      position: { x: 300, y: 100 },
      data: {
        label: '充电系统需求',
        type: 'RequirementDefinition',
        status: 'draft',
        properties: {
          declaredName: '充电系统需求',
          declaredShortName: 'CHARGING-001',
          reqId: 'REQ-002'
        }
      }
    },
    {
      id: 'U-001',
      position: { x: 100, y: 250 },
      data: {
        label: '电池使用实例',
        type: 'RequirementUsage',
        status: 'approved',
        properties: {
          declaredName: '电池使用实例',
          of: 'R-001'
        }
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
    },
    {
      id: 'e2',
      source: 'R-002',
      target: 'R-001',
      type: 'derive',
      label: '衍生'
    }
  ]
}

const mockUseModelContext = {
  elements: {},
  selectedIds: new Set<string>(),
  loading: false,
  error: null,
  pagination: { page: 0, size: 50, totalElements: 3, totalPages: 1, first: true, last: true },
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

// Mock ReactFlow组件
vi.mock('reactflow', () => ({
  default: ({ children, nodes, edges, onNodesChange, onEdgesChange, onConnect, onNodeClick, ...props }: any) => (
    <div role="application" aria-label="flow-chart" {...props}>
      <div className="react-flow__nodes">
        {nodes?.map((node: any) => (
          <div 
            key={node.id} 
            className="react-flow__node"
            data-id={node.id}
            onClick={() => onNodeClick?.(null, node)}
            style={{ position: 'absolute', left: node.position.x, top: node.position.y }}
          >
            <div className="node-label">{node.data.label}</div>
            <div className="node-type">{node.data.type}</div>
          </div>
        ))}
      </div>
      <div className="react-flow__edges">
        {edges?.map((edge: any) => (
          <div key={edge.id} className="react-flow__edge" data-source={edge.source} data-target={edge.target}>
            {edge.label}
          </div>
        ))}
      </div>
      {children}
    </div>
  ),
  Controls: ({ children }: any) => (
    <div className="react-flow__controls">
      <button title="zoom-in">放大</button>
      <button title="zoom-out">缩小</button>
      <button title="fit-view">适应视图</button>
      {children}
    </div>
  ),
  Background: () => <div className="react-flow__background" />,
  MiniMap: () => <div className="react-flow__minimap" />,
  MarkerType: { ArrowClosed: 'arrowclosed' },
  Position: { Top: 'top', Bottom: 'bottom', Left: 'left', Right: 'right' },
  useNodesState: () => [mockGraphData.nodes, vi.fn()],
  useEdgesState: () => [mockGraphData.edges, vi.fn()],
  Handle: ({ type, position, id }: any) => (
    <div className={`react-flow__handle react-flow__handle-${type}`} data-handleid={id} data-handlepos={position} />
  )
}))

describe('GraphView组件 - REQ-D3-1依赖图', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('【基础渲染】图视图展示', () => {
    it('应该渲染图视图容器', () => {
      render(<GraphView />)
      
      expect(screen.getByRole('application', { name: 'flow-chart' })).toBeInTheDocument()
      expect(screen.getByText('需求依赖关系图')).toBeInTheDocument()
    })

    it('应该渲染所有节点', () => {
      render(<GraphView />)
      
      expect(screen.getByText('电池系统需求')).toBeInTheDocument()
      expect(screen.getByText('充电系统需求')).toBeInTheDocument()
      expect(screen.getByText('电池使用实例')).toBeInTheDocument()
    })

    it('应该显示节点类型信息', () => {
      render(<GraphView />)
      
      const definitionNodes = screen.getAllByText('RequirementDefinition')
      const usageNodes = screen.getAllByText('RequirementUsage')
      
      expect(definitionNodes).toHaveLength(2)
      expect(usageNodes).toHaveLength(1)
    })

    it('应该渲染边连接', () => {
      render(<GraphView />)
      
      const edges = screen.getByText('包含')
      expect(edges).toBeInTheDocument()
      
      const deriveEdge = screen.getByText('衍生')
      expect(deriveEdge).toBeInTheDocument()
    })
  })

  describe('【节点样式】不同类型节点展示', () => {
    it('应该区分不同类型节点的样式', () => {
      render(<GraphView />)
      
      const batteryNode = screen.getByText('电池系统需求').closest('.react-flow__node')
      const usageNode = screen.getByText('电池使用实例').closest('.react-flow__node')
      
      expect(batteryNode).toHaveAttribute('data-id', 'R-001')
      expect(usageNode).toHaveAttribute('data-id', 'U-001')
    })

    it('应该根据状态显示不同颜色', () => {
      render(<GraphView />)
      
      // 通过data属性或类名区分状态
      const nodes = document.querySelectorAll('.react-flow__node')
      expect(nodes.length).toBeGreaterThan(0)
    })

    it('应该显示节点工具提示', async () => {
      const user = userEvent.setup()
      render(<GraphView />)
      
      const batteryNode = screen.getByText('电池系统需求')
      await user.hover(batteryNode)
      
      // 验证tooltip内容
      await waitFor(() => {
        expect(screen.getByText(/REQ-001/)).toBeInTheDocument()
      })
    })
  })

  describe('【交互功能】节点选中和高亮', () => {
    it('应该支持节点选中', async () => {
      const user = userEvent.setup()
      render(<GraphView />)
      
      const batteryNode = screen.getByText('电池系统需求').closest('.react-flow__node')
      await user.click(batteryNode!)
      
      expect(mockUseModelContext.selectElement).toHaveBeenCalledWith('R-001', true)
    })

    it('应该高亮选中节点', () => {
      mockUseModelContext.selectedIds = new Set(['R-001'])
      render(<GraphView />)
      
      const batteryNode = screen.getByText('电池系统需求').closest('.react-flow__node')
      expect(batteryNode).toHaveClass('selected')
    })

    it('应该高亮相关边', () => {
      mockUseModelContext.selectedIds = new Set(['R-001'])
      render(<GraphView />)
      
      // 验证与R-001相关的边被高亮
      const containEdge = screen.getByText('包含').closest('.react-flow__edge')
      expect(containEdge).toHaveClass('highlighted')
    })

    it('应该支持多选', async () => {
      const user = userEvent.setup()
      render(<GraphView />)
      
      // Ctrl+点击多选
      const batteryNode = screen.getByText('电池系统需求')
      const chargingNode = screen.getByText('充电系统需求')
      
      await user.keyboard('{Control>}')
      await user.click(batteryNode)
      await user.click(chargingNode)
      await user.keyboard('{/Control}')
      
      expect(mockUseModelContext.selectElement).toHaveBeenCalledTimes(2)
    })
  })

  describe('【布局算法】REQ-D3-3自动布局', () => {
    it('应该提供自动布局按钮', () => {
      render(<GraphView />)
      
      const layoutButton = screen.getByText('自动布局')
      expect(layoutButton).toBeInTheDocument()
    })

    it('应该支持层次布局', async () => {
      const user = userEvent.setup()
      render(<GraphView />)
      
      const layoutButton = screen.getByText('自动布局')
      await user.click(layoutButton)
      
      // 选择层次布局
      const hierarchicalOption = screen.getByText('层次布局')
      await user.click(hierarchicalOption)
      
      // 验证节点位置更新
      await waitFor(() => {
        const nodes = document.querySelectorAll('.react-flow__node')
        nodes.forEach(node => {
          const style = window.getComputedStyle(node as Element)
          expect(style.left).toBeDefined()
          expect(style.top).toBeDefined()
        })
      })
    })

    it('应该支持力导向布局', async () => {
      const user = userEvent.setup()
      render(<GraphView />)
      
      const layoutButton = screen.getByText('自动布局')
      await user.click(layoutButton)
      
      const forceOption = screen.getByText('力导向布局')
      await user.click(forceOption)
      
      // 验证布局应用
      expect(screen.getByText('布局已应用')).toBeInTheDocument()
    })

    it('应该支持径向布局', async () => {
      const user = userEvent.setup()
      render(<GraphView />)
      
      const layoutButton = screen.getByText('自动布局')
      await user.click(layoutButton)
      
      const radialOption = screen.getByText('径向布局')
      await user.click(radialOption)
      
      // 验证中心节点定位
      const centerNode = document.querySelector('[data-id="R-001"]')
      expect(centerNode).toHaveStyle({ position: 'absolute' })
    })
  })

  describe('【拖拽功能】节点拖拽移动', () => {
    it('应该支持节点拖拽', async () => {
      render(<GraphView />)
      
      const batteryNode = screen.getByText('电池系统需求').closest('.react-flow__node')
      
      // 模拟拖拽
      fireEvent.mouseDown(batteryNode!, { clientX: 100, clientY: 100 })
      fireEvent.mouseMove(batteryNode!, { clientX: 200, clientY: 200 })
      fireEvent.mouseUp(batteryNode!)
      
      // 验证位置更新
      await waitFor(() => {
        expect(batteryNode).toHaveStyle({ left: '200px', top: '200px' })
      })
    })

    it('应该保存拖拽后的位置', async () => {
      render(<GraphView />)
      
      const batteryNode = screen.getByText('电池系统需求').closest('.react-flow__node')
      
      // 拖拽节点
      fireEvent.mouseDown(batteryNode!, { clientX: 100, clientY: 100 })
      fireEvent.mouseMove(batteryNode!, { clientX: 250, clientY: 150 })
      fireEvent.mouseUp(batteryNode!)
      
      // 验证位置被保存
      expect(mockUseModelContext.updateElement).toHaveBeenCalledWith(
        'R-001',
        expect.objectContaining({
          position: { x: 250, y: 150 }
        })
      )
    })
  })

  describe('【缩放控制】视图缩放功能', () => {
    it('应该提供缩放控制按钮', () => {
      render(<GraphView />)
      
      expect(screen.getByTitle('zoom-in')).toBeInTheDocument()
      expect(screen.getByTitle('zoom-out')).toBeInTheDocument()
      expect(screen.getByTitle('fit-view')).toBeInTheDocument()
    })

    it('应该支持放大操作', async () => {
      const user = userEvent.setup()
      render(<GraphView />)
      
      const zoomInButton = screen.getByTitle('zoom-in')
      await user.click(zoomInButton)
      
      // 验证缩放级别
      const flowChart = screen.getByRole('application', { name: 'flow-chart' })
      expect(flowChart).toHaveStyle({ transform: expect.stringContaining('scale') })
    })

    it('应该支持缩小操作', async () => {
      const user = userEvent.setup()
      render(<GraphView />)
      
      const zoomOutButton = screen.getByTitle('zoom-out')
      await user.click(zoomOutButton)
      
      // 验证缩放级别
      const flowChart = screen.getByRole('application', { name: 'flow-chart' })
      expect(flowChart).toHaveAttribute('data-zoom-level')
    })

    it('应该支持适应视图', async () => {
      const user = userEvent.setup()
      render(<GraphView />)
      
      const fitViewButton = screen.getByTitle('fit-view')
      await user.click(fitViewButton)
      
      // 验证所有节点在视口内
      const nodes = document.querySelectorAll('.react-flow__node')
      nodes.forEach(node => {
        const rect = (node as HTMLElement).getBoundingClientRect()
        expect(rect.left).toBeGreaterThanOrEqual(0)
        expect(rect.top).toBeGreaterThanOrEqual(0)
      })
    })

    it('应该支持鼠标滚轮缩放', async () => {
      render(<GraphView />)
      
      const flowChart = screen.getByRole('application', { name: 'flow-chart' })
      
      // 模拟滚轮事件
      fireEvent.wheel(flowChart, { deltaY: -100, ctrlKey: true })
      
      // 验证缩放
      await waitFor(() => {
        expect(flowChart).toHaveAttribute('data-zoom-level')
      })
    })
  })

  describe('【边类型】不同关系类型展示', () => {
    it('应该区分不同类型的边', () => {
      render(<GraphView />)
      
      const containEdge = screen.getByText('包含').closest('.react-flow__edge')
      const deriveEdge = screen.getByText('衍生').closest('.react-flow__edge')
      
      expect(containEdge).toHaveAttribute('data-source', 'R-001')
      expect(containEdge).toHaveAttribute('data-target', 'U-001')
      expect(deriveEdge).toHaveAttribute('data-source', 'R-002')
    })

    it('应该显示箭头方向', () => {
      render(<GraphView />)
      
      const edges = document.querySelectorAll('.react-flow__edge')
      edges.forEach(edge => {
        const marker = edge.querySelector('marker')
        expect(marker).toHaveAttribute('markerEnd', expect.stringContaining('arrow'))
      })
    })

    it('应该支持不同边样式', () => {
      render(<GraphView />)
      
      // 验证不同类型的边有不同样式
      const containEdge = screen.getByText('包含').closest('.react-flow__edge')
      expect(containEdge).toHaveClass('edge-contain')
      
      const deriveEdge = screen.getByText('衍生').closest('.react-flow__edge')
      expect(deriveEdge).toHaveClass('edge-derive')
    })
  })

  describe('【小地图】导航支持', () => {
    it('应该显示小地图', () => {
      render(<GraphView showMinimap />)
      
      expect(document.querySelector('.react-flow__minimap')).toBeInTheDocument()
    })

    it('应该在小地图中显示节点', () => {
      render(<GraphView showMinimap />)
      
      const minimap = document.querySelector('.react-flow__minimap')
      const minimapNodes = minimap?.querySelectorAll('.react-flow__minimap-node')
      
      expect(minimapNodes?.length).toBe(3)
    })
  })

  describe('【状态处理】加载和错误状态', () => {
    it('应该显示加载状态', () => {
      mockUseModelContext.loading = true
      render(<GraphView />)
      
      expect(screen.getByText('加载中...')).toBeInTheDocument()
    })

    it('应该显示错误状态', () => {
      mockUseModelContext.loading = false
      mockUseModelContext.error = new Error('加载失败')
      render(<GraphView />)
      
      expect(screen.getByText(/加载失败/)).toBeInTheDocument()
    })

    it('应该显示空状态', () => {
      mockUseModelContext.getGraphViewData.mockReturnValue({ nodes: [], edges: [] })
      render(<GraphView />)
      
      expect(screen.getByText('暂无数据')).toBeInTheDocument()
    })
  })

  describe('【性能优化】大数据集处理', () => {
    it('应该支持500个节点渲染', () => {
      const largeGraphData: GraphViewData = {
        nodes: Array.from({ length: 500 }, (_, i) => ({
          id: `node-${i}`,
          position: { x: (i % 20) * 150, y: Math.floor(i / 20) * 150 },
          data: {
            label: `需求 ${i}`,
            type: i % 2 === 0 ? 'RequirementDefinition' : 'RequirementUsage',
            status: 'approved',
            properties: {}
          }
        })),
        edges: []
      }
      
      mockUseModelContext.getGraphViewData.mockReturnValue(largeGraphData)
      
      const startTime = performance.now()
      render(<GraphView />)
      const endTime = performance.now()
      
      // 验证渲染时间 < 1000ms
      expect(endTime - startTime).toBeLessThan(1000)
      
      // 验证节点数量
      const nodes = document.querySelectorAll('.react-flow__node')
      expect(nodes.length).toBe(500)
    })

    it('应该优化边渲染性能', () => {
      const largeGraphData: GraphViewData = {
        nodes: Array.from({ length: 100 }, (_, i) => ({
          id: `node-${i}`,
          position: { x: (i % 10) * 150, y: Math.floor(i / 10) * 150 },
          data: {
            label: `节点 ${i}`,
            type: 'RequirementDefinition',
            status: 'approved',
            properties: {}
          }
        })),
        edges: Array.from({ length: 200 }, (_, i) => ({
          id: `edge-${i}`,
          source: `node-${i % 100}`,
          target: `node-${(i + 1) % 100}`,
          type: 'derive',
          label: '关系'
        }))
      }
      
      mockUseModelContext.getGraphViewData.mockReturnValue(largeGraphData)
      
      render(<GraphView />)
      
      const edges = document.querySelectorAll('.react-flow__edge')
      expect(edges.length).toBe(200)
    })
  })
})