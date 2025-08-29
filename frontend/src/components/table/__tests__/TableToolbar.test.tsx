/**
 * REQ-UI-3: 表格顶部编辑按钮测试
 * 
 * 测试点：
 * - 工具栏渲染
 * - 按钮状态管理
 * - 编辑对话框触发
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

// Mock EditRequirementDialog
vi.mock('../../dialogs/EditRequirementDialog', () => ({
  default: vi.fn(({ visible, onClose, requirement, onSuccess }) => {
    if (!visible) return null
    return (
      <div data-testid="edit-dialog">
        <h2>编辑需求</h2>
        <button onClick={() => {
          onSuccess()
          onClose()
        }}>保存</button>
        <button onClick={onClose}>取消</button>
      </div>
    )
  })
}))

describe('TableToolbar', () => {
  const mockRequirements = [
    {
      id: 'usage-1',
      elementId: 'usage-1',
      eClass: 'RequirementUsage',
      reqId: 'REQ-USE-001',
      declaredName: 'Battery Performance',
      documentation: 'Battery system performance',
      requirementDefinition: 'def-1'
    },
    {
      id: 'usage-2',
      elementId: 'usage-2',
      eClass: 'RequirementUsage',
      reqId: 'REQ-USE-002',
      declaredName: 'Battery Safety',
      documentation: 'Battery safety requirements',
      requirementDefinition: 'def-2'
    }
  ]

  beforeEach(() => {
    vi.clearAllMocks()
    ;(requirementService.getRequirements as any).mockResolvedValue({
      data: mockRequirements,
      totalElements: 2,
      totalPages: 1,
      page: 0,
      size: 20
    })
  })

  it('应该渲染工具栏和编辑按钮', async () => {
    render(
      <ModelProvider>
        <TableView 
          editable={true} 
          selectable={true}
          showToolbar={true}
        />
      </ModelProvider>
    )

    await waitFor(() => {
      // 检查工具栏是否存在
      expect(screen.getByRole('toolbar')).toBeInTheDocument()
      // 检查编辑按钮是否存在
      expect(screen.getByRole('button', { name: /编辑/i })).toBeInTheDocument()
      // 初始状态应该是禁用的
      expect(screen.getByRole('button', { name: /编辑/i })).toBeDisabled()
    })
  })

  it('应该在选中单个需求时启用编辑按钮', async () => {
    render(
      <ModelProvider>
        <TableView 
          editable={true} 
          selectable={true}
          showToolbar={true}
        />
      </ModelProvider>
    )

    await waitFor(() => {
      expect(screen.getByText('Battery Performance')).toBeInTheDocument()
    })

    // 选中一行
    const checkbox = screen.getAllByRole('checkbox')[1] // 第一个是全选框
    fireEvent.click(checkbox)

    await waitFor(() => {
      // 编辑按钮应该启用
      expect(screen.getByRole('button', { name: /编辑/i })).not.toBeDisabled()
    })
  })

  it('应该在选中多个需求时禁用编辑按钮', async () => {
    render(
      <ModelProvider>
        <TableView 
          editable={true} 
          selectable={true}
          showToolbar={true}
        />
      </ModelProvider>
    )

    await waitFor(() => {
      expect(screen.getByText('Battery Performance')).toBeInTheDocument()
    })

    // 选中两行
    const checkboxes = screen.getAllByRole('checkbox')
    fireEvent.click(checkboxes[1]) // 选中第一行
    fireEvent.click(checkboxes[2]) // 选中第二行

    await waitFor(() => {
      // 编辑按钮应该禁用（不支持批量编辑）
      expect(screen.getByRole('button', { name: /编辑/i })).toBeDisabled()
    })
  })

  it('应该在点击编辑按钮时打开编辑对话框', async () => {
    render(
      <ModelProvider>
        <TableView 
          editable={true} 
          selectable={true}
          showToolbar={true}
        />
      </ModelProvider>
    )

    await waitFor(() => {
      expect(screen.getByText('Battery Performance')).toBeInTheDocument()
    })

    // 选中一行
    const checkbox = screen.getAllByRole('checkbox')[1]
    fireEvent.click(checkbox)

    // 点击编辑按钮
    const editButton = screen.getByRole('button', { name: /编辑/i })
    fireEvent.click(editButton)

    await waitFor(() => {
      // 检查编辑对话框是否打开
      expect(screen.getByTestId('edit-dialog')).toBeInTheDocument()
      expect(screen.getByText('编辑需求')).toBeInTheDocument()
    })
  })

  it('应该显示批量删除按钮', async () => {
    render(
      <ModelProvider>
        <TableView 
          editable={true} 
          selectable={true}
          showToolbar={true}
        />
      </ModelProvider>
    )

    await waitFor(() => {
      expect(screen.getByText('Battery Performance')).toBeInTheDocument()
    })

    // 选中一行
    const checkbox = screen.getAllByRole('checkbox')[1]
    fireEvent.click(checkbox)

    await waitFor(() => {
      // 删除按钮应该存在并启用
      expect(screen.getByRole('button', { name: /删除/i })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /删除/i })).not.toBeDisabled()
    })
  })

  it('应该显示创建按钮', async () => {
    render(
      <ModelProvider>
        <TableView 
          editable={true} 
          selectable={true}
          showToolbar={true}
        />
      </ModelProvider>
    )

    await waitFor(() => {
      // 创建按钮应该始终可用
      expect(screen.getByRole('button', { name: /创建|新建/i })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /创建|新建/i })).not.toBeDisabled()
    })
  })

  it('应该显示选中项数量', async () => {
    render(
      <ModelProvider>
        <TableView 
          editable={true} 
          selectable={true}
          showToolbar={true}
        />
      </ModelProvider>
    )

    await waitFor(() => {
      expect(screen.getByText('Battery Performance')).toBeInTheDocument()
    })

    // 选中一行
    const checkbox = screen.getAllByRole('checkbox')[1]
    fireEvent.click(checkbox)

    await waitFor(() => {
      // 应该显示选中数量
      expect(screen.getByText(/已选择 1 项/)).toBeInTheDocument()
    })

    // 再选中一行
    const checkbox2 = screen.getAllByRole('checkbox')[2]
    fireEvent.click(checkbox2)

    await waitFor(() => {
      // 应该更新选中数量
      expect(screen.getByText(/已选择 2 项/)).toBeInTheDocument()
    })
  })
})