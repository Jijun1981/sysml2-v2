/**
 * REQ-UI-1: 表格分页功能测试
 * 
 * 测试点：
 * - 分页组件渲染
 * - 页码切换功能
 * - 每页条数设置
 * - 总条数显示
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { ModelProvider } from '../../../contexts/ModelContext'
import TableView from '../TableView'
import { requirementService } from '../../../services/requirementService'

// Mock requirementService
vi.mock('../../../services/requirementService', () => ({
  requirementService: {
    getRequirements: vi.fn(),
    createRequirementDefinition: vi.fn(),
    createRequirementUsage: vi.fn(),
    updateRequirement: vi.fn(),
    deleteRequirement: vi.fn()
  }
}))

describe('TablePagination', () => {
  const mockRequirements = Array.from({ length: 50 }, (_, i) => ({
    id: `req-${i}`,
    elementId: `req-${i}`,
    eClass: i % 2 === 0 ? 'RequirementDefinition' : 'RequirementUsage',
    reqId: `REQ-${i}`,
    declaredName: `Requirement ${i}`,
    documentation: `Description for requirement ${i}`,
    requirementDefinition: i % 2 === 1 ? `req-${i - 1}` : undefined
  }))

  beforeEach(() => {
    vi.clearAllMocks()
    ;(requirementService.getRequirements as any).mockResolvedValue({
      data: mockRequirements.slice(0, 20),
      totalElements: 50,
      totalPages: 3,
      page: 0,
      size: 20
    })
  })

  it('应该渲染分页组件', async () => {
    render(
      <ModelProvider>
        <TableView pageable={true} pageSize={20} />
      </ModelProvider>
    )

    await waitFor(() => {
      // 检查分页组件是否存在
      expect(screen.getByRole('navigation')).toBeInTheDocument()
      // 检查总条数显示
      expect(screen.getByText(/共 50 条/)).toBeInTheDocument()
      // 检查页码范围显示
      expect(screen.getByText(/第 1-20 条/)).toBeInTheDocument()
    })
  })

  it('应该支持页码切换', async () => {
    render(
      <ModelProvider>
        <TableView pageable={true} pageSize={20} />
      </ModelProvider>
    )

    // 等待初始加载
    await waitFor(() => {
      expect(screen.getByText(/共 50 条/)).toBeInTheDocument()
    })

    // Mock第二页数据
    ;(requirementService.getRequirements as any).mockResolvedValue({
      data: mockRequirements.slice(20, 40),
      totalElements: 50,
      totalPages: 3,
      page: 1,
      size: 20
    })

    // 点击下一页
    const nextButton = screen.getByTitle('Next Page')
    fireEvent.click(nextButton)

    await waitFor(() => {
      // 验证API被调用
      expect(requirementService.getRequirements).toHaveBeenCalledWith(
        expect.objectContaining({
          page: 1,
          size: 20
        })
      )
      // 检查页码范围更新
      expect(screen.getByText(/第 21-40 条/)).toBeInTheDocument()
    })
  })

  it('应该支持每页条数设置', async () => {
    render(
      <ModelProvider>
        <TableView pageable={true} pageSize={20} />
      </ModelProvider>
    )

    await waitFor(() => {
      expect(screen.getByText(/共 50 条/)).toBeInTheDocument()
    })

    // Mock新的页大小数据
    ;(requirementService.getRequirements as any).mockResolvedValue({
      data: mockRequirements.slice(0, 10),
      totalElements: 50,
      totalPages: 5,
      page: 0,
      size: 10
    })

    // 找到并点击页大小选择器
    const pageSizeSelector = screen.getByTitle('10 / page')
    fireEvent.click(pageSizeSelector)

    // 选择10条/页
    const option10 = screen.getByText('10 / page')
    fireEvent.click(option10)

    await waitFor(() => {
      // 验证API被调用
      expect(requirementService.getRequirements).toHaveBeenCalledWith(
        expect.objectContaining({
          page: 0,
          size: 10
        })
      )
      // 检查页码范围更新
      expect(screen.getByText(/第 1-10 条/)).toBeInTheDocument()
    })
  })

  it('应该显示正确的总条数', async () => {
    render(
      <ModelProvider>
        <TableView pageable={true} pageSize={20} />
      </ModelProvider>
    )

    await waitFor(() => {
      // 检查总条数显示
      expect(screen.getByText('共 50 条记录')).toBeInTheDocument()
      // 检查分页信息显示
      expect(screen.getByText(/第 1-20 条，共 50 条/)).toBeInTheDocument()
    })
  })

  it('应该在没有数据时正确显示', async () => {
    ;(requirementService.getRequirements as any).mockResolvedValue({
      data: [],
      totalElements: 0,
      totalPages: 0,
      page: 0,
      size: 20
    })

    render(
      <ModelProvider>
        <TableView pageable={true} pageSize={20} />
      </ModelProvider>
    )

    await waitFor(() => {
      // 检查空数据提示
      expect(screen.getByText('暂无数据')).toBeInTheDocument()
      // 检查总条数为0
      expect(screen.getByText('共 0 条记录')).toBeInTheDocument()
    })
  })
})