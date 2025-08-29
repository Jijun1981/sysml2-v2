/**
 * 简化的RequirementUsage测试
 * 用于调试表单提交问题
 */

import React from 'react'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import '@testing-library/jest-dom'
import { vi, describe, it, expect } from 'vitest'
import CreateRequirementDialog from '../../components/dialogs/CreateRequirementDialog'
import { requirementService } from '../../services/requirementService'

// Mock requirementService
vi.mock('../../services/requirementService', () => ({
  requirementService: {
    createRequirementUsage: vi.fn()
  }
}))

// Mock message
vi.mock('antd', async () => {
  const actual = await vi.importActual('antd')
  return {
    ...actual,
    message: {
      success: vi.fn(),
      error: vi.fn()
    }
  }
})

describe('简化的RequirementUsage测试', () => {
  beforeEach(() => {
    // 在每个测试前设置mock返回值
    vi.mocked(requirementService.createRequirementUsage).mockResolvedValue({ 
      elementId: 'test-001',
      declaredName: 'Test Usage',
      text: 'Test text content',
      of: 'def-001'
    })
  })
  
  it('应该能创建RequirementUsage', async () => {
    const onClose = vi.fn()
    const onSuccess = vi.fn()
    
    const { container } = render(
      <CreateRequirementDialog
        open={true}
        onClose={onClose}
        onSuccess={onSuccess}
        type="usage"
        definitionId="def-001"  // 提供预设的definitionId
      />
    )
    
    // 找到并填写名称字段
    const nameInput = await screen.findByLabelText(/名称/i)
    fireEvent.change(nameInput, { target: { value: 'Test Usage' } })
    
    // 找到并填写文本字段
    const textInput = await screen.findByLabelText(/需求文本/i)
    fireEvent.change(textInput, { target: { value: 'Test text content' } })
    
    // 使用role查询找到确认按钮 - Ant Design Modal会在中文字符间加空格
    const submitButton = screen.getByRole('button', { name: '创 建' })
    console.log('Found submit button by role:', submitButton)
    console.log('Mock function before click:', requirementService.createRequirementUsage)
    console.log('Mock call count before:', vi.mocked(requirementService.createRequirementUsage).mock.calls.length)
    
    // 点击提交
    fireEvent.click(submitButton)
    console.log('Clicked submit button')
    console.log('Mock call count after click:', vi.mocked(requirementService.createRequirementUsage).mock.calls.length)
    
    // 等待一小段时间让异步操作完成
    await new Promise(resolve => setTimeout(resolve, 100))
    
    console.log('Mock call count after wait:', vi.mocked(requirementService.createRequirementUsage).mock.calls.length)
    console.log('Mock calls:', vi.mocked(requirementService.createRequirementUsage).mock.calls)
    
    // 等待service被调用
    await waitFor(() => {
      const mockCalls = vi.mocked(requirementService.createRequirementUsage).mock.calls
      console.log('Checking mock calls in waitFor:', mockCalls.length)
      expect(mockCalls.length).toBeGreaterThan(0)
    }, { timeout: 2000 })
    
    expect(requirementService.createRequirementUsage).toHaveBeenCalledWith(
      expect.objectContaining({
        declaredName: 'Test Usage',
        text: 'Test text content',
        of: 'def-001'
      })
    )
    
    expect(onSuccess).toHaveBeenCalled()
  })
  
  it('直接测试handleSubmit', async () => {
    const onClose = vi.fn()
    const onSuccess = vi.fn()
    
    const { container } = render(
      <CreateRequirementDialog
        open={true}
        onClose={onClose}
        onSuccess={onSuccess}
        type="usage"
        definitionId="def-001"
      />
    )
    
    // 调试：打印所有按钮
    await waitFor(() => {
      const buttons = screen.getAllByRole('button')
      console.log('All buttons:', buttons.map(b => b.textContent))
    })
    
    // 找到Modal的确定按钮（Ant Design Modal会在中文字符间加空格）
    const okButton = screen.getByRole('button', { name: '创 建' })
    console.log('Found OK button')
    
    // 填写必填字段
    const nameInput = await screen.findByLabelText(/名称/i)
    fireEvent.change(nameInput, { target: { value: 'Test' } })
    
    const textInput = await screen.findByLabelText(/需求文本/i)
    fireEvent.change(textInput, { target: { value: 'Test text' } })
    
    // 点击确定按钮
    fireEvent.click(okButton)
    console.log('Clicked OK button')
    
    // 验证handleSubmit被触发（通过查看console日志）
    // 这里主要是验证按钮点击能触发处理函数
  })
})