import { describe, test, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { ModelProvider } from '../../contexts/ModelContext'
import { EditDialog } from '../EditDialog'

// Mock universalApi
vi.mock('../../services/universalApi', () => ({
  updateUniversalElement: vi.fn(),
  createUniversalElement: vi.fn(),
  queryElementsByType: vi.fn()
}))

// Mock requirementService
vi.mock('../../services/requirementService', () => ({
  requirementService: {
    getDefinitions: vi.fn()
  }
}))

const mockElement = {
  elementId: 'test-element-id',
  id: 'test-element-id',
  eClass: 'RequirementDefinition',
  declaredName: '测试需求',
  declaredShortName: 'TEST-001',
  documentation: '这是一个测试需求',
  properties: {
    declaredName: '测试需求',
    declaredShortName: 'TEST-001', 
    documentation: '这是一个测试需求'
  }
}

describe('EditDialog HTTP Request Fix', () => {
  let mockOnSave: any
  let mockOnClose: any

  beforeEach(() => {
    mockOnSave = vi.fn()
    mockOnClose = vi.fn()
    vi.clearAllMocks()
  })

  test('应该在保存时发送HTTP请求', async () => {
    const { updateUniversalElement } = await import('../../services/universalApi')
    const mockUpdateElement = updateUniversalElement as any
    mockUpdateElement.mockResolvedValue({
      ...mockElement,
      declaredName: '更新的需求名称'
    })

    render(
      <ModelProvider>
        <EditDialog
          visible={true}
          element={mockElement}
          onSave={mockOnSave}
          onClose={mockOnClose}
        />
      </ModelProvider>
    )

    // 修改名称
    const nameInput = screen.getByDisplayValue('测试需求')
    fireEvent.change(nameInput, { target: { value: '更新的需求名称' } })

    // 点击保存按钮
    const saveButton = screen.getByRole('button', { name: /保存/ })
    fireEvent.click(saveButton)

    // 验证HTTP请求被发送
    await waitFor(() => {
      expect(mockUpdateElement).toHaveBeenCalledWith('test-element-id', {
        declaredName: '更新的需求名称',
        declaredShortName: 'TEST-001',
        documentation: '这是一个测试需求'
      })
    })

    // 验证回调被调用
    await waitFor(() => {
      expect(mockOnSave).toHaveBeenCalled()
    })
  })

  test('useCallback依赖项应该正确工作', async () => {
    const { updateUniversalElement } = await import('../../services/universalApi') 
    const mockUpdateElement = updateUniversalElement as any
    
    // 模拟网络延迟
    mockUpdateElement.mockImplementation(() => 
      new Promise(resolve => setTimeout(() => 
        resolve({
          ...mockElement,
          declaredName: '延迟响应的需求'
        }), 100))
    )

    render(
      <ModelProvider>
        <EditDialog
          visible={true}
          element={mockElement}
          onSave={mockOnSave}
          onClose={mockOnClose}
        />
      </ModelProvider>
    )

    const nameInput = screen.getByDisplayValue('测试需求')
    fireEvent.change(nameInput, { target: { value: '延迟响应的需求' } })

    const saveButton = screen.getByRole('button', { name: /保存/ })
    fireEvent.click(saveButton)

    // 验证loading状态
    expect(screen.getByText('保存中...')).toBeInTheDocument()

    // 等待请求完成
    await waitFor(() => {
      expect(mockUpdateElement).toHaveBeenCalled()
    }, { timeout: 2000 })

    await waitFor(() => {
      expect(mockOnSave).toHaveBeenCalled()
    })
  })
})