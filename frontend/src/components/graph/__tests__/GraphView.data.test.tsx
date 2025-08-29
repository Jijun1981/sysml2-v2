/**
 * GraphView数据加载测试
 * REQ-F2-3: 图视图关系展示
 * 
 * 验收标准：
 * - 使用React Flow显示需求节点和关系
 * - RequirementDefinition显示为蓝色方形节点
 * - RequirementUsage显示为绿色圆形节点
 * - Usage和Definition之间显示连接线
 * - 支持节点拖拽和画布缩放
 */

import React from 'react'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { describe, test, expect, beforeEach, vi } from 'vitest'
import { ModelProvider } from '../../../contexts/ModelContext'

// 先mock axios避免序列化问题
vi.mock('axios')

// mock所有使用axios的服务
vi.mock('../../../services/requirementService')
vi.mock('../../../services/universalApi')
vi.mock('../../../services/advancedQueryApi')

// mock React Flow
vi.mock('reactflow', () => ({
  __esModule: true,
  default: ({ children, nodes, edges, onNodesChange, onEdgesChange }: any) => (
    <div data-testid="react-flow-mock">
      <div data-testid="nodes-count">{nodes?.length || 0}</div>
      <div data-testid="edges-count">{edges?.length || 0}</div>
      {nodes?.map((node: any) => (
        <div key={node.id} data-testid={`node-${node.id}`} data-node-type={node.type}>
          {node.data.label}
        </div>
      ))}
      {edges?.map((edge: any) => (
        <div key={edge.id} data-testid={`edge-${edge.id}`}>
          {edge.source} → {edge.target}
        </div>
      ))}
    </div>
  ),
  ReactFlowProvider: ({ children }: any) => children,
  MiniMap: () => <div data-testid="minimap" />,
  Controls: () => <div data-testid="controls" />,
  Background: () => <div data-testid="background" />,
  addEdge: vi.fn(),
  useNodesState: vi.fn(() => [[], vi.fn()]),
  useEdgesState: vi.fn(() => [[], vi.fn()]),
  MarkerType: { ArrowClosed: 'arrowclosed' }
}))

// 最后导入组件
import GraphView from '../GraphView'
import { requirementService } from '../../../services/requirementService'

describe('GraphView 关系展示测试', () => {
  beforeEach(() => {
    // 重置所有mock
    vi.clearAllMocks()
  })

  describe('数据加载功能', () => {
    test('应该从requirementService加载数据', async () => {
      render(
        <ModelProvider>
          <GraphView />
        </ModelProvider>
      )

      // 等待数据加载
      await waitFor(() => {
        expect(requirementService.getRequirementDefinitions).toHaveBeenCalled()
        expect(requirementService.getRequirementUsages).toHaveBeenCalled()
      })
    })

    test('应该显示加载状态', () => {
      render(
        <ModelProvider>
          <GraphView />
        </ModelProvider>
      )

      // 初始应该显示加载状态
      const loadingElement = document.querySelector('.ant-spin')
      expect(loadingElement).toBeInTheDocument()
    })

    test('应该在加载失败时显示错误信息', async () => {
      // 模拟加载失败
      vi.mocked(requirementService.getRequirementDefinitions).mockRejectedValueOnce(
        new Error('网络错误')
      )

      render(
        <ModelProvider>
          <GraphView />
        </ModelProvider>
      )

      await waitFor(() => {
        expect(screen.getByText(/加载失败/i)).toBeInTheDocument()
      })
    })
  })

  describe('节点显示', () => {
    test('应该显示正确数量的节点', async () => {
      render(
        <ModelProvider>
          <GraphView />
        </ModelProvider>
      )

      await waitFor(() => {
        // 等待数据加载完成
        expect(requirementService.getRequirementDefinitions).toHaveBeenCalled()
      })

      await waitFor(() => {
        // 应该有5个节点（2个Definition + 3个Usage）
        expect(screen.getByTestId('nodes-count')).toHaveTextContent('5')
      })
    })

    test('应该显示RequirementDefinition节点', async () => {
      render(
        <ModelProvider>
          <GraphView />
        </ModelProvider>
      )

      await waitFor(() => {
        expect(requirementService.getRequirementDefinitions).toHaveBeenCalled()
      })

      await waitFor(() => {
        // 验证Definition节点显示
        expect(screen.getByTestId('node-req-def-001')).toBeInTheDocument()
        expect(screen.getByTestId('node-req-def-002')).toBeInTheDocument()
        expect(screen.getByText('充电时间需求')).toBeInTheDocument()
        expect(screen.getByText('电池容量需求')).toBeInTheDocument()
      })
    })

    test('应该显示RequirementUsage节点', async () => {
      render(
        <ModelProvider>
          <GraphView />
        </ModelProvider>
      )

      await waitFor(() => {
        expect(requirementService.getRequirementUsages).toHaveBeenCalled()
      })

      await waitFor(() => {
        // 验证Usage节点显示
        expect(screen.getByTestId('node-req-usage-001')).toBeInTheDocument()
        expect(screen.getByTestId('node-req-usage-002')).toBeInTheDocument()
        expect(screen.getByTestId('node-req-usage-003')).toBeInTheDocument()
        expect(screen.getByText('快充场景')).toBeInTheDocument()
        expect(screen.getByText('慢充场景')).toBeInTheDocument()
        expect(screen.getByText('标准电池包')).toBeInTheDocument()
      })
    })

    test('Definition节点应该使用正确的样式', async () => {
      render(
        <ModelProvider>
          <GraphView />
        </ModelProvider>
      )

      await waitFor(() => {
        expect(requirementService.getRequirementDefinitions).toHaveBeenCalled()
      })

      await waitFor(() => {
        const defNode = screen.getByTestId('node-req-def-001')
        expect(defNode).toHaveAttribute('data-node-type', 'definition')
      })
    })

    test('Usage节点应该使用正确的样式', async () => {
      render(
        <ModelProvider>
          <GraphView />
        </ModelProvider>
      )

      await waitFor(() => {
        expect(requirementService.getRequirementUsages).toHaveBeenCalled()
      })

      await waitFor(() => {
        const usageNode = screen.getByTestId('node-req-usage-001')
        expect(usageNode).toHaveAttribute('data-node-type', 'usage')
      })
    })
  })

  describe('关系连线', () => {
    test('应该显示正确数量的连接线', async () => {
      render(
        <ModelProvider>
          <GraphView />
        </ModelProvider>
      )

      await waitFor(() => {
        expect(requirementService.getRequirementUsages).toHaveBeenCalled()
      })

      await waitFor(() => {
        // 应该有3条连接线（每个Usage连接到其Definition）
        expect(screen.getByTestId('edges-count')).toHaveTextContent('3')
      })
    })

    test('应该显示Usage到Definition的连接线', async () => {
      render(
        <ModelProvider>
          <GraphView />
        </ModelProvider>
      )

      await waitFor(() => {
        expect(requirementService.getRequirementUsages).toHaveBeenCalled()
      })

      await waitFor(() => {
        // 验证连接线显示
        expect(screen.getByTestId('edge-req-usage-001_req-def-001')).toBeInTheDocument()
        expect(screen.getByTestId('edge-req-usage-002_req-def-001')).toBeInTheDocument()
        expect(screen.getByTestId('edge-req-usage-003_req-def-002')).toBeInTheDocument()
      })
    })
  })

  describe('交互功能', () => {
    test('应该显示缩放和平移控件', async () => {
      render(
        <ModelProvider>
          <GraphView />
        </ModelProvider>
      )

      await waitFor(() => {
        expect(screen.getByTestId('controls')).toBeInTheDocument()
      })
    })

    test('应该显示小地图', async () => {
      render(
        <ModelProvider>
          <GraphView showMiniMap />
        </ModelProvider>
      )

      await waitFor(() => {
        expect(screen.getByTestId('minimap')).toBeInTheDocument()
      })
    })

    test('应该显示背景网格', async () => {
      render(
        <ModelProvider>
          <GraphView showBackground />
        </ModelProvider>
      )

      await waitFor(() => {
        expect(screen.getByTestId('background')).toBeInTheDocument()
      })
    })

    test('点击节点应该触发选择事件', async () => {
      const onNodeClick = vi.fn()
      
      render(
        <ModelProvider>
          <GraphView onNodeClick={onNodeClick} />
        </ModelProvider>
      )

      await waitFor(() => {
        const node = screen.getByTestId('node-req-def-001')
        fireEvent.click(node)
      })

      expect(onNodeClick).toHaveBeenCalledWith('req-def-001', expect.anything())
    })
  })

  describe('布局功能', () => {
    test('应该支持自动布局', async () => {
      render(
        <ModelProvider>
          <GraphView autoLayout />
        </ModelProvider>
      )

      await waitFor(() => {
        expect(requirementService.getRequirementDefinitions).toHaveBeenCalled()
      })

      // 验证节点有位置信息
      await waitFor(() => {
        const nodes = screen.getAllByTestId(/^node-/)
        expect(nodes.length).toBeGreaterThan(0)
      })
    })

    test('应该支持层次布局', async () => {
      render(
        <ModelProvider>
          <GraphView layoutType="hierarchical" />
        </ModelProvider>
      )

      await waitFor(() => {
        expect(screen.getByTestId('nodes-count')).toHaveTextContent('5')
      })
    })
  })

  describe('过滤和搜索', () => {
    test('应该支持按节点类型过滤', async () => {
      render(
        <ModelProvider>
          <GraphView showFilter />
        </ModelProvider>
      )

      await waitFor(() => {
        // 应该有过滤控件
        const filterButton = screen.getByText('过滤')
        expect(filterButton).toBeInTheDocument()
      })
    })

    test('应该支持节点搜索', async () => {
      render(
        <ModelProvider>
          <GraphView showSearch />
        </ModelProvider>
      )

      await waitFor(() => {
        // 应该有搜索框
        const searchInput = screen.getByPlaceholderText(/搜索节点/i)
        expect(searchInput).toBeInTheDocument()
      })
    })
  })
})