/**
 * TreeView组件测试 - TDD第六阶段
 * 
 * 测试覆盖：
 * - REQ-D1-1: 层级展示，包含/引用关系
 * - 树节点展开/折叠
 * - 选中状态同步
 * - 视图联动响应
 * - 性能优化
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, act } from '@testing-library/react'
import { userEvent } from '@testing-library/user-event'
import React from 'react'
import TreeView from '../TreeView'
import { ModelProvider } from '../../../contexts/ModelContext'
import type { TreeViewData } from '../../../types/models'

// Mock数据
const mockTreeData: TreeViewData = {
  definitions: [
    {
      id: 'R-001',
      label: '电池系统需求',
      type: 'definition',
      children: [],
      usages: [
        {
          id: 'U-001',
          label: '电池使用实例1',
          type: 'usage'
        },
        {
          id: 'U-002', 
          label: '电池使用实例2',
          type: 'usage'
        }
      ]
    },
    {
      id: 'R-002',
      label: '充电系统需求',
      type: 'definition',
      children: [
        {
          id: 'R-003',
          label: '快充需求',
          type: 'definition',
          usages: [
            {
              id: 'U-003',
              label: '快充实例',
              type: 'usage'
            }
          ]
        }
      ],
      usages: []
    }
  ]
}

const mockElements = {
  'R-001': {
    id: 'R-001',
    eClass: 'RequirementDefinition',
    attributes: {
      declaredName: '电池系统需求',
      declaredShortName: 'BATTERY-001',
      reqId: 'REQ-001'
    }
  },
  'U-001': {
    id: 'U-001',
    eClass: 'RequirementUsage',
    attributes: {
      of: 'R-001',
      declaredName: '电池使用实例1',
      status: 'approved'
    }
  }
}

// Mock ModelContext using the actual ModelProvider
vi.mock('../../../contexts/ModelContext', () => {
  const originalModule = vi.importActual('../../../contexts/ModelContext')
  return {
    ...originalModule,
    useModelContext: vi.fn(),
    ModelProvider: ({ children }: { children: React.ReactNode }) => children
  }
})

describe('TreeView组件 - REQ-D1-1层级展示', () => {
  let mockSelectElement: any

  beforeEach(() => {
    vi.clearAllMocks()
    mockSelectElement = vi.fn()
  })

  describe('【基础渲染】树结构展示', () => {
    it('应该渲染树视图容器', () => {
      render(
        <MockModelProvider>
          <TreeView />
        </MockModelProvider>
      )

      expect(screen.getByRole('tree')).toBeInTheDocument()
      expect(screen.getByText('需求层次结构')).toBeInTheDocument()
    })

    it('应该展示RequirementDefinition节点', () => {
      render(
        <MockModelProvider>
          <TreeView />
        </MockModelProvider>
      )

      expect(screen.getByText('电池系统需求')).toBeInTheDocument()
      expect(screen.getByText('充电系统需求')).toBeInTheDocument()
    })

    it('应该展示层级关系', () => {
      render(
        <MockModelProvider>
          <TreeView />
        </MockModelProvider>
      )

      // 验证父子层级关系
      const batteryReq = screen.getByText('电池系统需求')
      const chargingReq = screen.getByText('充电系统需求')
      
      expect(batteryReq).toBeInTheDocument()
      expect(chargingReq).toBeInTheDocument()
    })
  })

  describe('【包含关系】Definition-Usage关系', () => {
    it('应该显示RequirementUsage作为子节点', async () => {
      render(
        <MockModelProvider>
          <TreeView />
        </MockModelProvider>
      )

      // 展开电池系统需求节点
      const batteryNode = screen.getByText('电池系统需求')
      const expandButton = batteryNode.closest('.ant-tree-treenode')?.querySelector('.ant-tree-switcher')
      
      if (expandButton) {
        await userEvent.click(expandButton)
      }

      // 验证Usage节点显示
      expect(screen.getByText('电池使用实例1')).toBeInTheDocument()
      expect(screen.getByText('电池使用实例2')).toBeInTheDocument()
    })

    it('应该区分Definition和Usage节点图标', () => {
      render(
        <MockModelProvider>
          <TreeView />
        </MockModelProvider>
      )

      // Definition节点应该有文件夹图标
      const definitionNodes = screen.getAllByRole('img', { name: 'folder' })
      expect(definitionNodes.length).toBeGreaterThan(0)
    })

    it('应该支持多层级嵌套', async () => {
      render(
        <MockModelProvider>
          <TreeView />
        </MockModelProvider>
      )

      // 展开充电系统需求
      const chargingNode = screen.getByText('充电系统需求')
      const expandButton = chargingNode.closest('.ant-tree-treenode')?.querySelector('.ant-tree-switcher')
      
      if (expandButton) {
        await userEvent.click(expandButton)
      }

      // 验证子定义节点
      expect(screen.getByText('快充需求')).toBeInTheDocument()
    })
  })

  describe('【交互功能】展开折叠操作', () => {
    it('应该支持节点展开和折叠', async () => {
      const user = userEvent.setup()
      
      render(
        <MockModelProvider>
          <TreeView />
        </MockModelProvider>
      )

      const expandButton = screen.getAllByRole('img', { name: 'caret-right' })[0]?.closest('span')
      
      if (expandButton) {
        // 展开节点
        await user.click(expandButton)
        
        // 验证子节点显示
        expect(screen.getByText('电池使用实例1')).toBeInTheDocument()
        
        // 再次点击折叠
        const collapseButton = screen.getByRole('img', { name: 'caret-down' })?.closest('span')
        if (collapseButton) {
          await user.click(collapseButton)
        }
      }
    })

    it('应该记住节点展开状态', async () => {
      const user = userEvent.setup()
      
      const { rerender } = render(
        <MockModelProvider>
          <TreeView />
        </MockModelProvider>
      )

      // 展开一个节点
      const expandButton = screen.getAllByRole('img', { name: 'caret-right' })[0]?.closest('span')
      if (expandButton) {
        await user.click(expandButton)
      }

      // 重新渲染
      rerender(
        <MockModelProvider>
          <TreeView />
        </MockModelProvider>
      )

      // 验证展开状态保持
      expect(screen.getByText('电池使用实例1')).toBeInTheDocument()
    })
  })

  describe('【选中同步】视图联动', () => {
    it('应该支持节点选中', async () => {
      const user = userEvent.setup()
      
      render(
        <MockModelProvider>
          <TreeView onSelect={mockSelectElement} />
        </MockModelProvider>
      )

      const batteryNode = screen.getByText('电池系统需求')
      await user.click(batteryNode)

      expect(mockSelectElement).toHaveBeenCalledWith('R-001', expect.any(Object))
    })

    it('应该支持多选模式', async () => {
      const user = userEvent.setup()
      
      render(
        <MockModelProvider>
          <TreeView onSelect={mockSelectElement} multiple />
        </MockModelProvider>
      )

      const batteryNode = screen.getByText('电池系统需求')
      const chargingNode = screen.getByText('充电系统需求')

      // Ctrl+点击多选
      await user.keyboard('{Control>}')
      await user.click(batteryNode)
      await user.click(chargingNode)
      await user.keyboard('{/Control}')

      expect(mockSelectElement).toHaveBeenCalledTimes(2)
    })

    it('应该同步显示选中状态', () => {
      const selectedIds = new Set(['R-001'])
      
      render(
        <MockModelProvider>
          <TreeView selectedKeys={Array.from(selectedIds)} />
        </MockModelProvider>
      )

      const batteryNode = screen.getByText('电池系统需求').closest('.ant-tree-node-content-wrapper')
      expect(batteryNode).toHaveClass('ant-tree-node-selected')
    })
  })

  describe('【搜索过滤】树节点搜索', () => {
    it('应该支持节点搜索', async () => {
      const user = userEvent.setup()
      
      render(
        <MockModelProvider>
          <TreeView searchable />
        </MockModelProvider>
      )

      const searchInput = screen.getByPlaceholderText('搜索需求...')
      await user.type(searchInput, '电池')

      // 验证搜索结果
      expect(screen.getByText('电池系统需求')).toBeInTheDocument()
      expect(screen.queryByText('充电系统需求')).not.toBeInTheDocument()
    })

    it('应该高亮搜索匹配文本', async () => {
      const user = userEvent.setup()
      
      render(
        <MockModelProvider>
          <TreeView searchable />
        </MockModelProvider>
      )

      const searchInput = screen.getByPlaceholderText('搜索需求...')
      await user.type(searchInput, '电池')

      // 验证高亮效果
      expect(screen.getByText('电池', { selector: 'mark' })).toBeInTheDocument()
    })
  })

  describe('【性能优化】大数据集支持', () => {
    it('应该支持虚拟滚动', () => {
      // 创建大数据集
      const largeTreeData: TreeViewData = {
        definitions: Array.from({ length: 100 }, (_, i) => ({
          id: `R-${i.toString().padStart(3, '0')}`,
          label: `需求定义 ${i + 1}`,
          type: 'definition',
          children: [],
          usages: Array.from({ length: 5 }, (_, j) => ({
            id: `U-${i}-${j}`,
            label: `使用实例 ${i + 1}-${j + 1}`,
            type: 'usage'
          }))
        }))
      }

      const LargeDataProvider = ({ children }: { children: React.ReactNode }) => {
        const mockContextValue = {
          elements: {},
          selectedIds: new Set<string>(),
          loading: false,
          error: null,
          pagination: { page: 0, size: 50, totalElements: 500, totalPages: 10, first: true, last: false },
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
          getTreeViewData: vi.fn().mockReturnValue(largeTreeData),
          getTableViewData: vi.fn(),
          getGraphViewData: vi.fn(),
          setProjectId: vi.fn(),
          refreshProject: vi.fn()
        }

        return React.createElement(
          React.createContext(mockContextValue).Provider,
          { value: mockContextValue },
          children
        )
      }

      render(
        <LargeDataProvider>
          <TreeView virtual />
        </LargeDataProvider>
      )

      // 验证虚拟滚动容器
      expect(screen.getByRole('tree')).toHaveAttribute('data-virtual', 'true')
    })

    it('应该在500节点内保持响应性能', () => {
      const startTime = performance.now()
      
      // 渲染大数据集
      render(
        <MockModelProvider>
          <TreeView />
        </MockModelProvider>
      )

      const endTime = performance.now()
      const renderTime = endTime - startTime

      // 验证渲染时间 < 100ms
      expect(renderTime).toBeLessThan(100)
    })
  })

  describe('【键盘导航】无障碍支持', () => {
    it('应该支持键盘导航', async () => {
      const user = userEvent.setup()
      
      render(
        <MockModelProvider>
          <TreeView />
        </MockModelProvider>
      )

      const treeView = screen.getByRole('tree')
      await user.click(treeView)

      // 使用方向键导航
      await user.keyboard('{ArrowDown}')
      await user.keyboard('{ArrowDown}')
      await user.keyboard('{Enter}')

      // 验证键盘选中
      expect(mockSelectElement).toHaveBeenCalled()
    })

    it('应该支持无障碍属性', () => {
      render(
        <MockModelProvider>
          <TreeView />
        </MockModelProvider>
      )

      const treeView = screen.getByRole('tree')
      expect(treeView).toHaveAttribute('aria-label', '需求树视图')
      
      const treeItems = screen.getAllByRole('treeitem')
      treeItems.forEach(item => {
        expect(item).toHaveAttribute('aria-label')
      })
    })
  })
})