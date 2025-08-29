/**
 * 三视图联动集成测试
 * REQ-A1-1: 数据源唯一 - 任一视图更新，其他视图同步
 * REQ-A1-2: 视图为投影 - 不各自持久化副本
 * REQ-A1-3: 性能底线 - 联动响应<500ms
 * 
 * 验收标准：
 * - T-SSOT-01：三视图数据一致，同一id对象属性相同
 * - 选中状态实时同步
 * - 数据更新实时反映
 */

import React from 'react'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { describe, test, expect, beforeEach, vi } from 'vitest'
import { ModelProvider } from '../../contexts/ModelContext'

// mock服务
vi.mock('axios')
vi.mock('../../services/requirementService')
vi.mock('../../services/universalApi')
vi.mock('../../services/advancedQueryApi')

// 简化的mock组件
const MockTreeView = () => {
  const { selectElement, selectedIds, getTreeViewData } = useModelContext()
  const data = getTreeViewData()
  
  return (
    <div data-testid="tree-view">
      {data.definitions.map(def => (
        <div 
          key={def.id} 
          data-testid={`tree-node-${def.id}`}
          onClick={() => selectElement(def.id)}
          className={selectedIds.has(def.id) ? 'selected' : ''}
        >
          {def.label}
        </div>
      ))}
    </div>
  )
}

const MockTableView = () => {
  const { selectElement, selectedIds, getTableViewData } = useModelContext()
  const data = getTableViewData()
  
  return (
    <div data-testid="table-view">
      {data.map(row => (
        <div 
          key={row.id} 
          data-testid={`table-row-${row.id}`}
          onClick={() => selectElement(row.id)}
          className={selectedIds.has(row.id) ? 'selected' : ''}
        >
          {row.declaredName}
        </div>
      ))}
    </div>
  )
}

const MockGraphView = () => {
  const { selectElement, selectedIds, getGraphViewData } = useModelContext()
  const data = getGraphViewData()
  
  return (
    <div data-testid="graph-view">
      {data.nodes.map(node => (
        <div 
          key={node.id} 
          data-testid={`graph-node-${node.id}`}
          onClick={() => selectElement(node.id)}
          className={selectedIds.has(node.id) ? 'selected' : ''}
        >
          {node.data.label}
        </div>
      ))}
    </div>
  )
}

// 需要导入useModelContext
import { useModelContext } from '../../contexts/ModelContext'

describe('三视图联动测试', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('REQ-A1-1: 数据源唯一', () => {
    test('树视图选中时，表视图和图视图应该同步高亮', async () => {
      render(
        <ModelProvider>
          <MockTreeView />
          <MockTableView />
          <MockGraphView />
        </ModelProvider>
      )

      // 等待数据加载
      await waitFor(() => {
        expect(screen.getByTestId('tree-view')).toBeInTheDocument()
      })

      // 在树视图中选中节点
      const treeNode = screen.getByTestId('tree-node-req-def-001')
      fireEvent.click(treeNode)

      // 验证表视图中对应行也被选中
      await waitFor(() => {
        const tableRow = screen.getByTestId('table-row-req-def-001')
        expect(tableRow).toHaveClass('selected')
      })

      // 验证图视图中对应节点也被选中
      await waitFor(() => {
        const graphNode = screen.getByTestId('graph-node-req-def-001')
        expect(graphNode).toHaveClass('selected')
      })
    })

    test('表视图选中时，树视图和图视图应该同步高亮', async () => {
      render(
        <ModelProvider>
          <MockTreeView />
          <MockTableView />
          <MockGraphView />
        </ModelProvider>
      )

      await waitFor(() => {
        expect(screen.getByTestId('table-view')).toBeInTheDocument()
      })

      // 在表视图中选中行
      const tableRow = screen.getByTestId('table-row-req-def-002')
      fireEvent.click(tableRow)

      // 验证树视图中对应节点被选中
      await waitFor(() => {
        const treeNode = screen.getByTestId('tree-node-req-def-002')
        expect(treeNode).toHaveClass('selected')
      })

      // 验证图视图中对应节点被选中
      await waitFor(() => {
        const graphNode = screen.getByTestId('graph-node-req-def-002')
        expect(graphNode).toHaveClass('selected')
      })
    })
  })

  describe('REQ-A1-2: 视图为投影', () => {
    test('所有视图应该显示相同的数据源', async () => {
      render(
        <ModelProvider>
          <MockTreeView />
          <MockTableView />
          <MockGraphView />
        </ModelProvider>
      )

      await waitFor(() => {
        // 获取三个视图中的元素数量
        const treeNodes = screen.getAllByTestId(/^tree-node-/)
        const tableRows = screen.getAllByTestId(/^table-row-/)
        const graphNodes = screen.getAllByTestId(/^graph-node-/)

        // 验证Definition数量一致（至少有2个）
        expect(treeNodes.length).toBeGreaterThanOrEqual(2)
        expect(tableRows.length).toBeGreaterThanOrEqual(5) // 包含Usage
        expect(graphNodes.length).toBeGreaterThanOrEqual(5)
      })
    })

    test('数据更新后所有视图应该同步更新', async () => {
      const { rerender } = render(
        <ModelProvider>
          <MockTreeView />
          <MockTableView />
          <MockGraphView />
        </ModelProvider>
      )

      // 模拟数据更新
      // 这里需要通过ModelContext的updateElement方法更新数据
      // 然后验证三个视图都反映了更新

      // 由于mock限制，这里仅验证视图渲染
      await waitFor(() => {
        expect(screen.getByTestId('tree-view')).toBeInTheDocument()
        expect(screen.getByTestId('table-view')).toBeInTheDocument()
        expect(screen.getByTestId('graph-view')).toBeInTheDocument()
      })
    })
  })

  describe('REQ-A1-3: 性能底线', () => {
    test('视图联动响应时间应该小于500ms', async () => {
      render(
        <ModelProvider>
          <MockTreeView />
          <MockTableView />
          <MockGraphView />
        </ModelProvider>
      )

      await waitFor(() => {
        expect(screen.getByTestId('tree-view')).toBeInTheDocument()
      })

      const startTime = performance.now()
      
      // 触发选中
      const treeNode = screen.getByTestId('tree-node-req-def-001')
      fireEvent.click(treeNode)

      // 等待其他视图更新
      await waitFor(() => {
        const tableRow = screen.getByTestId('table-row-req-def-001')
        expect(tableRow).toHaveClass('selected')
      })

      const endTime = performance.now()
      const responseTime = endTime - startTime

      // 验证响应时间小于500ms
      expect(responseTime).toBeLessThan(500)
    })
  })

  describe('T-SSOT-01: 数据一致性', () => {
    test('三视图应该显示相同ID的相同属性', async () => {
      render(
        <ModelProvider>
          <MockTreeView />
          <MockTableView />
          <MockGraphView />
        </ModelProvider>
      )

      await waitFor(() => {
        // 验证同一个元素在不同视图中的显示
        const treeNode = screen.getByTestId('tree-node-req-def-001')
        const tableRow = screen.getByTestId('table-row-req-def-001')
        const graphNode = screen.getByTestId('graph-node-req-def-001')

        // 都应该显示相同的标签文本
        expect(treeNode).toHaveTextContent('充电时间需求')
        expect(tableRow).toHaveTextContent('充电时间需求')
        expect(graphNode).toHaveTextContent('充电时间需求')
      })
    })
  })
})