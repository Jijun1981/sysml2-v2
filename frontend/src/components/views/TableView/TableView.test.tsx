import React from 'react'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import TableView from './TableView'
import { useModel } from '../../../contexts/ModelContext'
import '@testing-library/jest-dom'

// Mock ModelContext
vi.mock('../../../contexts/ModelContext', () => ({
  useModel: vi.fn()
}))

describe('TableView Component', () => {
  const mockRequirements = [
    {
      id: 'R-001',
      reqId: 'REQ-001',
      name: '测试需求1',
      text: '这是测试需求1的描述',
      tags: ['tag1', 'tag2'],
      createdAt: '2025-01-15T10:00:00Z',
      updatedAt: '2025-01-15T10:00:00Z'
    },
    {
      id: 'R-002',
      reqId: 'REQ-002',
      name: '测试需求2', 
      text: '这是测试需求2的描述',
      tags: ['tag3'],
      createdAt: '2025-01-15T11:00:00Z',
      updatedAt: '2025-01-15T11:00:00Z'
    }
  ]

  const mockSelectElement = vi.fn()
  const mockDeleteRequirement = vi.fn()

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('应该正确渲染需求表格', () => {
    const mockUseModel = {
      requirements: mockRequirements,
      usages: [],
      traces: [],
      selectedId: null,
      selectElement: mockSelectElement,
      deleteRequirement: mockDeleteRequirement,
      loading: false,
      error: null
    }
    
    ;(useModel as any).mockReturnValue(mockUseModel)

    render(<TableView />)

    // 验证表头
    expect(screen.getByText('ID')).toBeInTheDocument()
    expect(screen.getByText('名称')).toBeInTheDocument()
    expect(screen.getByText('描述')).toBeInTheDocument()
    expect(screen.getByText('标签')).toBeInTheDocument()
    expect(screen.getByText('操作')).toBeInTheDocument()

    // 验证数据行
    expect(screen.getByText('REQ-001')).toBeInTheDocument()
    expect(screen.getByText('测试需求1')).toBeInTheDocument()
    expect(screen.getByText('REQ-002')).toBeInTheDocument()
    expect(screen.getByText('测试需求2')).toBeInTheDocument()
  })

  it('应该正确显示选中行的高亮', () => {
    const mockUseModel = {
      requirements: mockRequirements,
      usages: [],
      traces: [],
      selectedId: 'R-001', // 选中第一个需求
      selectElement: mockSelectElement,
      deleteRequirement: mockDeleteRequirement,
      loading: false,
      error: null
    }
    
    ;(useModel as any).mockReturnValue(mockUseModel)

    const { container } = render(<TableView />)
    
    // 查找选中的行
    const selectedRow = container.querySelector('tr[data-row-key="R-001"]')
    expect(selectedRow).toHaveStyle({ backgroundColor: '#e6f7ff' })
  })

  it('点击行应该调用selectElement', async () => {
    const mockUseModel = {
      requirements: mockRequirements,
      usages: [],
      traces: [],
      selectedId: null,
      selectElement: mockSelectElement,
      deleteRequirement: mockDeleteRequirement,
      loading: false,
      error: null
    }
    
    ;(useModel as any).mockReturnValue(mockUseModel)

    const { container } = render(<TableView />)
    
    // 点击第一行
    const firstRow = container.querySelector('tr[data-row-key="R-001"]')
    if (firstRow) {
      fireEvent.click(firstRow)
    }

    await waitFor(() => {
      expect(mockSelectElement).toHaveBeenCalledWith('R-001')
    })
  })

  it('点击编辑按钮应该选中该需求', async () => {
    const mockUseModel = {
      requirements: mockRequirements,
      usages: [],
      traces: [],
      selectedId: null,
      selectElement: mockSelectElement,
      deleteRequirement: mockDeleteRequirement,
      loading: false,
      error: null
    }
    
    ;(useModel as any).mockReturnValue(mockUseModel)

    render(<TableView />)
    
    // 找到第一个编辑按钮并点击
    const editButtons = screen.getAllByRole('button', { name: /edit/i })
    fireEvent.click(editButtons[0])

    await waitFor(() => {
      expect(mockSelectElement).toHaveBeenCalledWith('R-001')
    })
  })

  it('点击删除按钮应该调用deleteRequirement', async () => {
    const mockUseModel = {
      requirements: mockRequirements,
      usages: [],
      traces: [],
      selectedId: null,
      selectElement: mockSelectElement,
      deleteRequirement: mockDeleteRequirement,
      loading: false,
      error: null
    }
    
    ;(useModel as any).mockReturnValue(mockUseModel)

    render(<TableView />)
    
    // 找到第一个删除按钮并点击
    const deleteButtons = screen.getAllByRole('button', { name: /delete/i })
    fireEvent.click(deleteButtons[0])

    await waitFor(() => {
      expect(mockDeleteRequirement).toHaveBeenCalledWith('R-001')
    })
  })

  it('应该正确渲染标签', () => {
    const mockUseModel = {
      requirements: mockRequirements,
      usages: [],
      traces: [],
      selectedId: null,
      selectElement: mockSelectElement,
      deleteRequirement: mockDeleteRequirement,
      loading: false,
      error: null
    }
    
    ;(useModel as any).mockReturnValue(mockUseModel)

    render(<TableView />)

    // 验证标签显示
    expect(screen.getByText('tag1')).toBeInTheDocument()
    expect(screen.getByText('tag2')).toBeInTheDocument()
    expect(screen.getByText('tag3')).toBeInTheDocument()
  })

  it('空数据时应该显示空状态', () => {
    const mockUseModel = {
      requirements: [],
      usages: [],
      traces: [],
      selectedId: null,
      selectElement: mockSelectElement,
      deleteRequirement: mockDeleteRequirement,
      loading: false,
      error: null
    }
    
    ;(useModel as any).mockReturnValue(mockUseModel)

    render(<TableView />)

    // 验证空状态提示
    expect(screen.getByText(/暂无数据/i)).toBeInTheDocument()
  })
})