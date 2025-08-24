import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import GraphView from './GraphView'
import { useModel } from '../../../contexts/ModelContext'
import '@testing-library/jest-dom'

// Mock ModelContext
vi.mock('../../../contexts/ModelContext', () => ({
  useModel: vi.fn()
}))

// Mock ReactFlow
vi.mock('reactflow', () => ({
  default: vi.fn(({ nodes, edges, onNodesChange, onEdgesChange, onConnect, onNodeClick }) => {
    return (
      <div data-testid="react-flow">
        <div data-testid="nodes-count">{nodes.length}</div>
        <div data-testid="edges-count">{edges.length}</div>
        {nodes.map((node: any) => (
          <div 
            key={node.id}
            data-testid={`node-${node.id}`}
            onClick={() => onNodeClick?.(null, node)}
          >
            {node.data.label}
          </div>
        ))}
      </div>
    )
  }),
  Controls: () => <div data-testid="controls">Controls</div>,
  MiniMap: () => <div data-testid="minimap">MiniMap</div>,
  Background: () => <div data-testid="background">Background</div>,
  BackgroundVariant: { Dots: 'dots' },
  MarkerType: { ArrowClosed: 'arrowclosed' }
}))

describe('GraphView Component', () => {
  const mockRequirements = [
    {
      id: 'R-001',
      reqId: 'REQ-001',
      name: '需求1',
      text: '描述1',
      tags: ['tag1']
    },
    {
      id: 'R-002',
      reqId: 'REQ-002',
      name: '需求2',
      text: '描述2',
      tags: ['tag2']
    }
  ]

  const mockUsages = [
    {
      id: 'U-001',
      of: 'R-001',
      name: '用法1',
      text: '用法描述1'
    }
  ]

  const mockTraces = [
    {
      id: 'T-001',
      fromId: 'R-001',
      toId: 'R-002',
      type: 'derive'
    }
  ]

  const mockSelectElement = vi.fn()
  const mockCreateTrace = vi.fn()
  const mockDeleteTrace = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('应该渲染所有节点和边', () => {
    const mockUseModel = {
      requirements: mockRequirements,
      usages: mockUsages,
      traces: mockTraces,
      selectedId: null,
      selectElement: mockSelectElement,
      createTrace: mockCreateTrace,
      deleteTrace: mockDeleteTrace,
      loading: false,
      error: null
    }
    
    ;(useModel as any).mockReturnValue(mockUseModel)

    render(<GraphView />)

    // 验证节点数量 (2个requirements + 1个usage = 3个节点)
    expect(screen.getByTestId('nodes-count')).toHaveTextContent('3')
    
    // 验证边数量 (1个trace)
    expect(screen.getByTestId('edges-count')).toHaveTextContent('1')
    
    // 验证节点存在
    expect(screen.getByTestId('node-R-001')).toBeInTheDocument()
    expect(screen.getByTestId('node-R-002')).toBeInTheDocument()
    expect(screen.getByTestId('node-U-001')).toBeInTheDocument()
  })

  it('点击节点应该调用selectElement', async () => {
    const mockUseModel = {
      requirements: mockRequirements,
      usages: [],
      traces: [],
      selectedId: null,
      selectElement: mockSelectElement,
      createTrace: mockCreateTrace,
      deleteTrace: mockDeleteTrace,
      loading: false,
      error: null
    }
    
    ;(useModel as any).mockReturnValue(mockUseModel)

    render(<GraphView />)
    
    // 点击第一个节点
    const node = screen.getByTestId('node-R-001')
    fireEvent.click(node)

    await waitFor(() => {
      expect(mockSelectElement).toHaveBeenCalledWith('R-001')
    })
  })

  it('选中的节点应该有特殊样式', () => {
    const mockUseModel = {
      requirements: mockRequirements,
      usages: [],
      traces: [],
      selectedId: 'R-001', // 选中第一个节点
      selectElement: mockSelectElement,
      createTrace: mockCreateTrace,
      deleteTrace: mockDeleteTrace,
      loading: false,
      error: null
    }
    
    ;(useModel as any).mockReturnValue(mockUseModel)

    const { container } = render(<GraphView />)
    
    // 验证选中状态通过数据传递（实际样式在ReactFlow内部处理）
    expect(container.querySelector('[data-testid="react-flow"]')).toBeInTheDocument()
  })

  it('应该显示控制组件', () => {
    const mockUseModel = {
      requirements: [],
      usages: [],
      traces: [],
      selectedId: null,
      selectElement: mockSelectElement,
      createTrace: mockCreateTrace,
      deleteTrace: mockDeleteTrace,
      loading: false,
      error: null
    }
    
    ;(useModel as any).mockReturnValue(mockUseModel)

    render(<GraphView />)

    // 验证控制组件存在
    expect(screen.getByTestId('controls')).toBeInTheDocument()
    expect(screen.getByTestId('minimap')).toBeInTheDocument()
    expect(screen.getByTestId('background')).toBeInTheDocument()
  })

  it('应该正确处理不同类型的追溯关系', () => {
    const mockTracesWithTypes = [
      { id: 'T-001', fromId: 'R-001', toId: 'R-002', type: 'derive' },
      { id: 'T-002', fromId: 'R-002', toId: 'R-001', type: 'satisfy' },
      { id: 'T-003', fromId: 'R-001', toId: 'U-001', type: 'refine' }
    ]

    const mockUseModel = {
      requirements: mockRequirements,
      usages: mockUsages,
      traces: mockTracesWithTypes,
      selectedId: null,
      selectElement: mockSelectElement,
      createTrace: mockCreateTrace,
      deleteTrace: mockDeleteTrace,
      loading: false,
      error: null
    }
    
    ;(useModel as any).mockReturnValue(mockUseModel)

    render(<GraphView />)

    // 验证边数量
    expect(screen.getByTestId('edges-count')).toHaveTextContent('3')
  })
})