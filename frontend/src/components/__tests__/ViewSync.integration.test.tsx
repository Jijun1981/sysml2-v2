import { describe, test, expect, beforeEach, vi } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ModelProvider } from '../../contexts/ModelContext'
import TreeView from '../views/TreeView/TreeView'
import TableView from '../views/TableView/TableView'
import GraphView from '../views/GraphView/GraphView'

// Mock the universal API
vi.mock('../../services/universalApi', () => ({
  createUniversalElement: vi.fn(),
  queryElementsByType: vi.fn(),
  queryAllElements: vi.fn(),
  getElementById: vi.fn(),
  updateUniversalElement: vi.fn(),
  deleteUniversalElement: vi.fn(),
  validateStatic: vi.fn(),
  checkUniversalHealth: vi.fn(),
  setProjectId: vi.fn(),
  handleUniversalApiError: vi.fn(),
  default: {
    createElement: vi.fn(),
    queryElementsByType: vi.fn(),
    queryAllElements: vi.fn(),
    getElementById: vi.fn(),
    updateElement: vi.fn(),
    deleteElement: vi.fn(),
    validateStatic: vi.fn(),
    checkHealth: vi.fn(),
    setProjectId: vi.fn()
  }
}))

describe('Three View Sync Integration', () => {
  const mockElements = {
    'def-001': {
      id: 'def-001',
      eClass: 'RequirementDefinition',
      attributes: {
        declaredShortName: 'EBS-L1-001',
        declaredName: '电池系统性能需求'
      }
    },
    'usage-001': {
      id: 'usage-001', 
      eClass: 'RequirementUsage',
      attributes: {
        declaredShortName: 'EBS-L1-001-U1',
        declaredName: 'BMS性能要求实例',
        of: 'def-001',
        subject: 'bms-part-001'
      }
    },
    'satisfy-001': {
      id: 'satisfy-001',
      eClass: 'Satisfy',
      attributes: {
        source: 'bms-part-001',
        target: 'usage-001'
      }
    }
  }

  const TestApp = () => {
    return (
      <ModelProvider projectId="test-project" initialElements={mockElements}>
        <div data-testid="app-container">
          <div data-testid="tree-view">
            <TreeView />
          </div>
          <div data-testid="table-view">
            <TableView />
          </div>
          <div data-testid="graph-view">
            <GraphView />
          </div>
        </div>
      </ModelProvider>
    )
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  test('tree selection should highlight in table and graph', async () => {
    // Arrange
    const user = userEvent.setup()
    render(<TestApp />)

    // Act - 在树视图中选择一个需求定义
    const treeNode = await screen.findByTestId('tree-node-def-001')
    await user.click(treeNode)

    // Assert - 验证表视图和图视图中对应元素被高亮
    await waitFor(() => {
      const tableRow = screen.getByTestId('table-row-def-001')
      expect(tableRow).toHaveClass('selected') // 表格行高亮
    })

    await waitFor(() => {
      const graphNode = screen.getByTestId('graph-node-def-001')  
      expect(graphNode).toHaveClass('selected') // 图节点高亮
    })
  })

  test('table row click should update tree and graph', async () => {
    // Arrange
    const user = userEvent.setup()
    render(<TestApp />)

    // Act - 在表视图中点击一行
    const tableRow = await screen.findByTestId('table-row-usage-001')
    await user.click(tableRow)

    // Assert - 验证树视图展开并选中对应节点
    await waitFor(() => {
      const treeNode = screen.getByTestId('tree-node-usage-001')
      expect(treeNode).toHaveClass('selected')
      
      // 验证父节点展开
      const parentNode = screen.getByTestId('tree-node-def-001')
      expect(parentNode).toHaveClass('expanded')
    })

    // Assert - 验证图视图选中对应节点
    await waitFor(() => {
      const graphNode = screen.getByTestId('graph-node-usage-001')
      expect(graphNode).toHaveClass('selected')
    })
  })

  test('graph node selection should sync with tree and table', async () => {
    // Arrange
    const user = userEvent.setup()
    render(<TestApp />)

    // Act - 在图视图中选择一个节点
    const graphNode = await screen.findByTestId('graph-node-satisfy-001')
    await user.click(graphNode)

    // Assert - 验证表视图滚动到对应行并高亮
    await waitFor(() => {
      const tableRow = screen.getByTestId('table-row-satisfy-001')
      expect(tableRow).toHaveClass('selected')
      expect(tableRow.scrollIntoView).toHaveBeenCalled()
    })

    // Assert - 验证树视图选中状态（依赖关系可能不在树中显示）
    // 这里验证至少没有其他元素被错误选中
    const treeNodes = screen.getAllByTestId(/tree-node-/)
    treeNodes.forEach(node => {
      if (node.getAttribute('data-testid') !== 'tree-node-satisfy-001') {
        expect(node).not.toHaveClass('selected')
      }
    })
  })

  test('selection state should persist during view updates', async () => {
    // Arrange
    const user = userEvent.setup()
    render(<TestApp />)

    // 先选中一个元素
    const treeNode = await screen.findByTestId('tree-node-def-001')
    await user.click(treeNode)

    // Act - 触发视图更新（比如过滤、排序等）
    const filterInput = screen.getByTestId('table-filter-input')
    await user.type(filterInput, 'BMS')

    // Assert - 验证选中状态在视图更新后保持
    await waitFor(() => {
      const selectedTreeNode = screen.getByTestId('tree-node-def-001')
      expect(selectedTreeNode).toHaveClass('selected')
      
      // 注意：过滤后表格可能不显示该项，这是正常的
      if (screen.queryByTestId('table-row-def-001')) {
        expect(screen.getByTestId('table-row-def-001')).toHaveClass('selected')
      }
    })
  })

  test('multi-selection should work across views', async () => {
    // Arrange
    const user = userEvent.setup()
    render(<TestApp />)

    // Act - 使用Ctrl+点击进行多选
    const treeNode1 = await screen.findByTestId('tree-node-def-001')
    await user.click(treeNode1)
    
    const treeNode2 = screen.getByTestId('tree-node-usage-001')
    await user.keyboard('[ControlLeft>]')
    await user.click(treeNode2)
    await user.keyboard('[/ControlLeft]')

    // Assert - 验证多选状态在所有视图中同步
    await waitFor(() => {
      expect(screen.getByTestId('tree-node-def-001')).toHaveClass('selected')
      expect(screen.getByTestId('tree-node-usage-001')).toHaveClass('selected')
      
      expect(screen.getByTestId('table-row-def-001')).toHaveClass('selected')
      expect(screen.getByTestId('table-row-usage-001')).toHaveClass('selected')
      
      expect(screen.getByTestId('graph-node-def-001')).toHaveClass('selected')
      expect(screen.getByTestId('graph-node-usage-001')).toHaveClass('selected')
    })
  })

  test('real-time updates should sync across all views', async () => {
    // Arrange
    const user = userEvent.setup()
    render(<TestApp />)

    // Act - 在表视图中编辑一个元素的名称
    const editButton = await screen.findByTestId('edit-button-def-001')
    await user.click(editButton)

    const nameInput = screen.getByTestId('name-input-def-001')
    await user.clear(nameInput)
    await user.type(nameInput, '更新后的需求名称')
    
    const saveButton = screen.getByTestId('save-button-def-001')
    await user.click(saveButton)

    // Assert - 验证更新后的名称在所有视图中同步显示
    await waitFor(() => {
      // 树视图
      const treeNodeText = screen.getByTestId('tree-node-text-def-001')
      expect(treeNodeText).toHaveTextContent('更新后的需求名称')
      
      // 表视图
      const tableRowName = screen.getByTestId('table-row-name-def-001')
      expect(tableRowName).toHaveTextContent('更新后的需求名称')
      
      // 图视图
      const graphNodeLabel = screen.getByTestId('graph-node-label-def-001')
      expect(graphNodeLabel).toHaveTextContent('更新后的需求名称')
    })
  })

  test('view-specific operations should not break sync', async () => {
    // Arrange
    const user = userEvent.setup()
    render(<TestApp />)

    // Act - 在图视图中执行特定操作（如拖拽节点位置）
    const graphNode = await screen.findByTestId('graph-node-def-001')
    
    // 模拟拖拽操作
    fireEvent.mouseDown(graphNode, { clientX: 100, clientY: 100 })
    fireEvent.mouseMove(graphNode, { clientX: 200, clientY: 150 })
    fireEvent.mouseUp(graphNode, { clientX: 200, clientY: 150 })

    // Assert - 验证视图特定操作不影响选中状态同步
    await user.click(graphNode)
    
    await waitFor(() => {
      expect(screen.getByTestId('tree-node-def-001')).toHaveClass('selected')
      expect(screen.getByTestId('table-row-def-001')).toHaveClass('selected')
    })
  })

  test('should handle selection of related elements correctly', async () => {
    // Arrange
    const user = userEvent.setup()
    render(<TestApp />)

    // Act - 选择一个RequirementUsage，应该自动显示相关的Definition和Satisfy关系
    const usageNode = await screen.findByTestId('tree-node-usage-001')
    await user.click(usageNode)

    // Assert - 验证相关元素在不同视图中被适当高亮或显示
    await waitFor(() => {
      // 选中的Usage元素
      expect(screen.getByTestId('table-row-usage-001')).toHaveClass('selected')
      expect(screen.getByTestId('graph-node-usage-001')).toHaveClass('selected')
      
      // 相关的Definition元素应该被标记为相关
      expect(screen.getByTestId('graph-node-def-001')).toHaveClass('related')
      
      // 相关的Satisfy关系应该被高亮
      expect(screen.getByTestId('graph-edge-satisfy-001')).toHaveClass('highlighted')
    })
  })
})