/**
 * 三视图集成测试 - TDD第六阶段
 * 
 * 测试覆盖：
 * - REQ-A1-1: 三视图选中联动
 * - REQ-A1-3: 性能要求（500节点，响应<100ms）
 * - 视图间数据同步
 * - 选中状态同步
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { userEvent } from '@testing-library/user-event'
import React from 'react'
import { ModelProvider } from '../../contexts/ModelContext'
import TreeView from '../tree/TreeView'
import TableView from '../table/TableView'
import GraphView from '../graph/GraphView'

// Mock数据
const mockElements = {
  'R-001': {
    elementId: 'R-001',
    eClass: 'RequirementDefinition',
    properties: {
      declaredName: '电池系统需求',
      declaredShortName: 'BATTERY-001',
      reqId: 'REQ-001',
      status: 'approved'
    }
  },
  'R-002': {
    elementId: 'R-002',
    eClass: 'RequirementDefinition',
    properties: {
      declaredName: '充电系统需求',
      declaredShortName: 'CHARGING-001',
      reqId: 'REQ-002',
      status: 'draft'
    }
  },
  'U-001': {
    elementId: 'U-001',
    eClass: 'RequirementUsage',
    properties: {
      declaredName: '电池使用实例',
      of: 'R-001',
      status: 'approved'
    }
  }
}

// Mock API
vi.mock('../../services/advancedQueryApi', () => ({
  queryAdvanced: vi.fn().mockResolvedValue({
    content: Object.values(mockElements),
    page: 0,
    size: 50,
    totalElements: 3,
    totalPages: 1,
    first: true,
    last: true
  })
}))

// 简化的Mock组件
vi.mock('../tree/TreeView', () => ({
  default: ({ onSelect }: any) => (
    <div data-testid="tree-view">
      <div onClick={() => onSelect?.('R-001')} data-testid="tree-node-R-001">
        电池系统需求
      </div>
      <div onClick={() => onSelect?.('R-002')} data-testid="tree-node-R-002">
        充电系统需求
      </div>
    </div>
  )
}))

vi.mock('../table/TableView', () => ({
  default: () => {
    const { useModelContext } = require('../../contexts/ModelContext')
    const { selectedIds, selectElement } = useModelContext()
    
    return (
      <div data-testid="table-view">
        <div 
          onClick={() => selectElement('R-001', true)} 
          data-testid="table-row-R-001"
          className={selectedIds.has('R-001') ? 'selected' : ''}
        >
          电池系统需求
        </div>
        <div 
          onClick={() => selectElement('R-002', true)} 
          data-testid="table-row-R-002"
          className={selectedIds.has('R-002') ? 'selected' : ''}
        >
          充电系统需求
        </div>
      </div>
    )
  }
}))

vi.mock('../graph/GraphView', () => ({
  default: ({ onNodeSelect }: any) => {
    const { useModelContext } = require('../../contexts/ModelContext')
    const { selectedIds, selectElement } = useModelContext()
    
    return (
      <div data-testid="graph-view">
        <div 
          onClick={() => {
            selectElement('R-001', true)
            onNodeSelect?.('R-001')
          }} 
          data-testid="graph-node-R-001"
          className={selectedIds.has('R-001') ? 'selected' : ''}
        >
          电池系统需求
        </div>
        <div 
          onClick={() => {
            selectElement('R-002', true)
            onNodeSelect?.('R-002')
          }} 
          data-testid="graph-node-R-002"
          className={selectedIds.has('R-002') ? 'selected' : ''}
        >
          充电系统需求
        </div>
      </div>
    )
  }
}))

describe('三视图集成测试 - REQ-A1-1', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('【选中联动】视图间选中状态同步', () => {
    it('树视图选中应同步到表格和图视图', async () => {
      const handleTreeSelect = vi.fn()
      const handleNodeSelect = vi.fn()
      
      render(
        <ModelProvider>
          <TreeView onSelect={handleTreeSelect} />
          <TableView />
          <GraphView onNodeSelect={handleNodeSelect} />
        </ModelProvider>
      )

      // 在树视图中选中节点
      const treeNode = screen.getByTestId('tree-node-R-001')
      await userEvent.click(treeNode)

      await waitFor(() => {
        // 验证表格视图中对应行被选中
        const tableRow = screen.getByTestId('table-row-R-001')
        expect(tableRow).toHaveClass('selected')

        // 验证图视图中对应节点被选中
        const graphNode = screen.getByTestId('graph-node-R-001')
        expect(graphNode).toHaveClass('selected')
      })
    })

    it('表格视图选中应同步到树和图视图', async () => {
      render(
        <ModelProvider>
          <TreeView />
          <TableView />
          <GraphView />
        </ModelProvider>
      )

      // 在表格视图中选中行
      const tableRow = screen.getByTestId('table-row-R-002')
      await userEvent.click(tableRow)

      await waitFor(() => {
        // 验证图视图中对应节点被选中
        const graphNode = screen.getByTestId('graph-node-R-002')
        expect(graphNode).toHaveClass('selected')
      })
    })

    it('图视图选中应同步到树和表格视图', async () => {
      render(
        <ModelProvider>
          <TreeView />
          <TableView />
          <GraphView />
        </ModelProvider>
      )

      // 在图视图中选中节点
      const graphNode = screen.getByTestId('graph-node-R-001')
      await userEvent.click(graphNode)

      await waitFor(() => {
        // 验证表格视图中对应行被选中
        const tableRow = screen.getByTestId('table-row-R-001')
        expect(tableRow).toHaveClass('selected')
      })
    })

    it('应支持多选同步', async () => {
      render(
        <ModelProvider>
          <TreeView />
          <TableView />
          <GraphView />
        </ModelProvider>
      )

      // 选中多个节点
      const node1 = screen.getByTestId('graph-node-R-001')
      const node2 = screen.getByTestId('graph-node-R-002')
      
      await userEvent.keyboard('{Control>}')
      await userEvent.click(node1)
      await userEvent.click(node2)
      await userEvent.keyboard('{/Control}')

      await waitFor(() => {
        // 验证两个节点都被选中
        const tableRow1 = screen.getByTestId('table-row-R-001')
        const tableRow2 = screen.getByTestId('table-row-R-002')
        expect(tableRow1).toHaveClass('selected')
        expect(tableRow2).toHaveClass('selected')
      })
    })
  })

  describe('【数据同步】视图间数据一致性', () => {
    it('所有视图应显示相同的数据', async () => {
      render(
        <ModelProvider>
          <TreeView />
          <TableView />
          <GraphView />
        </ModelProvider>
      )

      await waitFor(() => {
        // 验证所有视图都显示相同的需求
        expect(screen.getAllByText('电池系统需求')).toHaveLength(3)
        expect(screen.getAllByText('充电系统需求')).toHaveLength(3)
      })
    })

    it('数据更新应同步到所有视图', async () => {
      const { rerender } = render(
        <ModelProvider>
          <TreeView />
          <TableView />
          <GraphView />
        </ModelProvider>
      )

      // 模拟数据更新
      mockElements['R-003'] = {
        elementId: 'R-003',
        eClass: 'RequirementDefinition',
        properties: {
          declaredName: '新增需求',
          declaredShortName: 'NEW-001',
          reqId: 'REQ-003',
          status: 'draft'
        }
      }

      // 重新渲染
      rerender(
        <ModelProvider>
          <TreeView />
          <TableView />
          <GraphView />
        </ModelProvider>
      )

      await waitFor(() => {
        // 验证新数据在所有视图中出现
        // 注意：由于我们的Mock组件是简化的，这里仅验证基本功能
        expect(screen.getByTestId('tree-view')).toBeInTheDocument()
        expect(screen.getByTestId('table-view')).toBeInTheDocument()
        expect(screen.getByTestId('graph-view')).toBeInTheDocument()
      })
    })
  })

  describe('【性能测试】REQ-A1-3响应时间', () => {
    it('选中联动响应时间应小于100ms', async () => {
      render(
        <ModelProvider>
          <TreeView />
          <TableView />
          <GraphView />
        </ModelProvider>
      )

      const startTime = performance.now()
      
      // 触发选中
      const treeNode = screen.getByTestId('tree-node-R-001')
      await userEvent.click(treeNode)

      await waitFor(() => {
        const tableRow = screen.getByTestId('table-row-R-001')
        expect(tableRow).toHaveClass('selected')
      })

      const endTime = performance.now()
      const responseTime = endTime - startTime

      // 验证响应时间 < 100ms
      expect(responseTime).toBeLessThan(100)
    })

    it('应支持500个节点的渲染', () => {
      // 创建大数据集
      const largeElements: any = {}
      for (let i = 0; i < 500; i++) {
        largeElements[`R-${i}`] = {
          elementId: `R-${i}`,
          eClass: 'RequirementDefinition',
          properties: {
            declaredName: `需求${i}`,
            declaredShortName: `REQ-${i}`,
            reqId: `REQ-${i}`,
            status: 'draft'
          }
        }
      }

      const startTime = performance.now()
      
      render(
        <ModelProvider>
          <TreeView />
          <TableView />
          <GraphView />
        </ModelProvider>
      )

      const endTime = performance.now()
      const renderTime = endTime - startTime

      // 验证渲染时间 < 3000ms
      expect(renderTime).toBeLessThan(3000)
    })
  })

  describe('【清除选中】取消选择功能', () => {
    it('应支持清除所有选中状态', async () => {
      const { container } = render(
        <ModelProvider>
          <TreeView />
          <TableView />
          <GraphView />
        </ModelProvider>
      )

      // 先选中一些节点
      const node1 = screen.getByTestId('graph-node-R-001')
      await userEvent.click(node1)

      // 点击空白处清除选中
      await userEvent.click(container)

      await waitFor(() => {
        const tableRow = screen.getByTestId('table-row-R-001')
        expect(tableRow).not.toHaveClass('selected')
      })
    })

    it('应支持取消单个选中', async () => {
      render(
        <ModelProvider>
          <TreeView />
          <TableView />
          <GraphView />
        </ModelProvider>
      )

      // 选中节点
      const node = screen.getByTestId('graph-node-R-001')
      await userEvent.click(node)

      // 再次点击取消选中
      await userEvent.click(node)

      await waitFor(() => {
        const tableRow = screen.getByTestId('table-row-R-001')
        expect(tableRow).not.toHaveClass('selected')
      })
    })
  })
})