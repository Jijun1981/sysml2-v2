/**
 * TableView组件测试 - TDD第六阶段
 * 
 * 测试覆盖：
 * - REQ-D2-1: 分页表格，内联编辑
 * - 分页功能
 * - 排序功能
 * - 过滤功能
 * - 内联编辑
 * - 视图联动
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { userEvent } from '@testing-library/user-event'
import React from 'react'
import TableView from '../TableView'

// Mock数据
const mockTableData = [
  {
    id: 'R-001',
    eClass: 'RequirementDefinition',
    declaredShortName: 'REQ-001',
    declaredName: '电池系统需求',
    status: 'approved',
    reqId: 'REQ-001',
    text: '电池系统应当提供可靠的电力供应',
    createdAt: '2025-08-25T10:00:00Z'
  },
  {
    id: 'U-001',
    eClass: 'RequirementUsage',
    declaredShortName: 'USE-001',
    declaredName: '电池使用实例',
    status: 'draft',
    of: 'R-001',
    createdAt: '2025-08-25T11:00:00Z'
  },
  {
    id: 'R-002',
    eClass: 'RequirementDefinition',
    declaredShortName: 'REQ-002',
    declaredName: '充电系统需求',
    status: 'rejected',
    reqId: 'REQ-002',
    createdAt: '2025-08-25T12:00:00Z'
  }
]

const mockUseModelContext = {
  elements: {},
  selectedIds: new Set<string>(),
  loading: false,
  error: null,
  pagination: { page: 0, size: 10, totalElements: 3, totalPages: 1, first: true, last: true },
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

describe('TableView组件 - REQ-D2-1分页表格', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('【基础渲染】表格结构展示', () => {
    it('应该渲染表格组件', () => {
      render(<TableView />)
      
      expect(screen.getByRole('table')).toBeInTheDocument()
      expect(screen.getByText('需求数据表格')).toBeInTheDocument()
    })

    it('应该显示表头列', () => {
      render(<TableView />)
      
      expect(screen.getByText('类型')).toBeInTheDocument()
      expect(screen.getByText('短名称')).toBeInTheDocument()
      expect(screen.getByText('名称')).toBeInTheDocument()
      expect(screen.getByText('状态')).toBeInTheDocument()
      expect(screen.getByText('操作')).toBeInTheDocument()
    })

    it('应该显示表格数据', () => {
      render(<TableView />)
      
      expect(screen.getByText('电池系统需求')).toBeInTheDocument()
      expect(screen.getByText('电池使用实例')).toBeInTheDocument()
      expect(screen.getByText('充电系统需求')).toBeInTheDocument()
    })

    it('应该区分不同的eClass类型', () => {
      render(<TableView />)
      
      expect(screen.getByText('RequirementDefinition')).toBeInTheDocument()
      expect(screen.getByText('RequirementUsage')).toBeInTheDocument()
    })
  })

  describe('【分页功能】表格分页', () => {
    it('应该显示分页器', () => {
      render(<TableView />)
      
      // 检查分页组件存在
      expect(screen.getByRole('list', { name: /pagination/i })).toBeInTheDocument()
    })

    it('应该显示总数信息', () => {
      render(<TableView />)
      
      expect(screen.getByText(/共 3 条/)).toBeInTheDocument()
    })

    it('应该支持页面大小调整', async () => {
      const user = userEvent.setup()
      render(<TableView />)
      
      // 查找页面大小选择器
      const pageSizeSelector = screen.getByDisplayValue('10')
      await user.selectOptions(pageSizeSelector, '20')
      
      expect(mockUseModelContext.loadAllElements).toHaveBeenCalledWith({
        page: 0,
        size: 20
      })
    })

    it('应该支持页码跳转', async () => {
      // 模拟多页数据
      mockUseModelContext.pagination.totalPages = 3
      mockUseModelContext.pagination.last = false
      
      const user = userEvent.setup()
      render(<TableView />)
      
      // 点击下一页
      const nextButton = screen.getByTitle('下一页')
      await user.click(nextButton)
      
      expect(mockUseModelContext.loadAllElements).toHaveBeenCalledWith({
        page: 1,
        size: 10
      })
    })
  })

  describe('【排序功能】列排序', () => {
    it('应该支持列标题排序', async () => {
      const user = userEvent.setup()
      render(<TableView />)
      
      // 点击名称列排序
      const nameColumn = screen.getByText('名称')
      await user.click(nameColumn)
      
      expect(mockUseModelContext.loadAllElements).toHaveBeenCalledWith({
        page: 0,
        size: 10,
        sort: [{ field: 'declaredName', direction: 'ascend' }]
      })
    })

    it('应该支持多重排序', async () => {
      const user = userEvent.setup()
      render(<TableView />)
      
      // 按住Shift点击多个列头
      const typeColumn = screen.getByText('类型')
      const statusColumn = screen.getByText('状态')
      
      await user.keyboard('{Shift>}')
      await user.click(typeColumn)
      await user.click(statusColumn)
      await user.keyboard('{/Shift}')
      
      expect(mockUseModelContext.loadAllElements).toHaveBeenCalledWith(
        expect.objectContaining({
          sort: expect.arrayContaining([
            { field: 'eClass', direction: 'ascend' },
            { field: 'status', direction: 'ascend' }
          ])
        })
      )
    })

    it('应该显示排序指示器', async () => {
      const user = userEvent.setup()
      render(<TableView />)
      
      // 点击排序后应显示排序图标
      const nameColumn = screen.getByText('名称')
      await user.click(nameColumn)
      
      expect(nameColumn.closest('th')).toHaveClass('ant-table-column-sort')
    })
  })

  describe('【过滤功能】列过滤', () => {
    it('应该支持状态过滤', async () => {
      const user = userEvent.setup()
      render(<TableView />)
      
      // 找到状态列的过滤按钮
      const statusFilterButton = screen.getByRole('button', { name: /filter/i })
      await user.click(statusFilterButton)
      
      // 选择approved过滤项
      const approvedOption = screen.getByText('已批准')
      await user.click(approvedOption)
      
      // 点击确认
      const confirmButton = screen.getByText('确定')
      await user.click(confirmButton)
      
      expect(mockUseModelContext.loadAllElements).toHaveBeenCalledWith({
        page: 0,
        size: 10,
        filter: [{ field: 'status', value: 'approved' }]
      })
    })

    it('应该支持类型过滤', async () => {
      const user = userEvent.setup()
      render(<TableView />)
      
      // 找到类型列的过滤按钮并测试过滤
      const typeFilterButton = screen.getAllByRole('button', { name: /filter/i })[0]
      await user.click(typeFilterButton)
      
      const definitionOption = screen.getByText('RequirementDefinition')
      await user.click(definitionOption)
      
      expect(mockUseModelContext.loadAllElements).toHaveBeenCalledWith(
        expect.objectContaining({
          filter: [{ field: 'eClass', value: 'RequirementDefinition' }]
        })
      )
    })
  })

  describe('【内联编辑】行编辑功能', () => {
    it('应该支持行内编辑', async () => {
      const user = userEvent.setup()
      render(<TableView editable />)
      
      // 找到编辑按钮
      const editButton = screen.getAllByText('编辑')[0]
      await user.click(editButton)
      
      // 应该出现编辑状态
      expect(screen.getByDisplayValue('电池系统需求')).toBeInTheDocument()
    })

    it('应该支持保存编辑', async () => {
      const user = userEvent.setup()
      render(<TableView editable />)
      
      // 进入编辑模式
      const editButton = screen.getAllByText('编辑')[0]
      await user.click(editButton)
      
      // 修改内容
      const nameInput = screen.getByDisplayValue('电池系统需求')
      await user.clear(nameInput)
      await user.type(nameInput, '修改后的需求名称')
      
      // 保存
      const saveButton = screen.getByText('保存')
      await user.click(saveButton)
      
      expect(mockUseModelContext.updateElement).toHaveBeenCalledWith(
        'R-001',
        expect.objectContaining({
          declaredName: '修改后的需求名称'
        })
      )
    })

    it('应该支持取消编辑', async () => {
      const user = userEvent.setup()
      render(<TableView editable />)
      
      // 进入编辑模式
      const editButton = screen.getAllByText('编辑')[0]
      await user.click(editButton)
      
      // 取消编辑
      const cancelButton = screen.getByText('取消')
      await user.click(cancelButton)
      
      // 应该恢复原始内容
      expect(screen.getByText('电池系统需求')).toBeInTheDocument()
    })
  })

  describe('【行选中】选择功能', () => {
    it('应该支持单行选中', async () => {
      const user = userEvent.setup()
      render(<TableView />)
      
      // 点击行选择框
      const checkboxes = screen.getAllByRole('checkbox')
      await user.click(checkboxes[1]) // 第一行数据的checkbox
      
      expect(mockUseModelContext.selectElement).toHaveBeenCalledWith('R-001', false)
    })

    it('应该支持多行选中', async () => {
      const user = userEvent.setup()
      render(<TableView />)
      
      // 选中多行
      const checkboxes = screen.getAllByRole('checkbox')
      await user.click(checkboxes[1])
      await user.click(checkboxes[2])
      
      expect(mockUseModelContext.selectElement).toHaveBeenCalledTimes(2)
    })

    it('应该支持全选', async () => {
      const user = userEvent.setup()
      render(<TableView />)
      
      // 点击全选checkbox
      const selectAllCheckbox = screen.getAllByRole('checkbox')[0]
      await user.click(selectAllCheckbox)
      
      expect(mockUseModelContext.selectElement).toHaveBeenCalledTimes(3)
    })

    it('应该同步显示选中状态', () => {
      mockUseModelContext.selectedIds = new Set(['R-001', 'U-001'])
      render(<TableView />)
      
      const checkboxes = screen.getAllByRole('checkbox')
      expect(checkboxes[1]).toBeChecked() // R-001对应的checkbox
      expect(checkboxes[2]).toBeChecked() // U-001对应的checkbox
    })
  })

  describe('【搜索功能】全局搜索', () => {
    it('应该提供搜索框', () => {
      render(<TableView searchable />)
      
      expect(screen.getByPlaceholderText('搜索需求...')).toBeInTheDocument()
    })

    it('应该支持搜索查询', async () => {
      const user = userEvent.setup()
      render(<TableView searchable />)
      
      const searchInput = screen.getByPlaceholderText('搜索需求...')
      await user.type(searchInput, '电池')
      
      // 等待搜索防抖
      await new Promise(resolve => setTimeout(resolve, 300))
      
      expect(mockUseModelContext.loadAllElements).toHaveBeenCalledWith({
        page: 0,
        size: 10,
        search: '电池'
      })
    })
  })

  describe('【状态处理】加载和错误状态', () => {
    it('应该显示加载状态', () => {
      mockUseModelContext.loading = true
      render(<TableView />)
      
      expect(screen.getByText(/loading/i)).toBeInTheDocument()
    })

    it('应该显示错误状态', () => {
      mockUseModelContext.loading = false
      mockUseModelContext.error = new Error('加载失败')
      render(<TableView />)
      
      expect(screen.getByText(/加载失败/)).toBeInTheDocument()
    })

    it('应该显示空状态', () => {
      mockUseModelContext.getTableViewData.mockReturnValue([])
      render(<TableView />)
      
      expect(screen.getByText(/暂无数据/)).toBeInTheDocument()
    })
  })
})