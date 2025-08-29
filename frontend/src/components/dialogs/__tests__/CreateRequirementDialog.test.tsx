/**
 * CreateRequirementDialog 组件单元测试
 * 测试需求创建对话框的所有功能
 */

import React from 'react'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { message } from 'antd'
import CreateRequirementDialog from '../CreateRequirementDialog'
import { requirementService } from '../../../services/requirementService'

import { vi } from 'vitest'

// Mock requirementService
vi.mock('../../../services/requirementService', () => ({
  requirementService: {
    createRequirementDefinition: vi.fn(),
    createRequirementUsage: vi.fn(),
  }
}))

// Mock antd message
vi.mock('antd', async () => {
  const actual = await vi.importActual('antd')
  return {
    ...actual,
    message: {
      success: vi.fn(),
      error: vi.fn(),
      loading: vi.fn(),
    }
  }
})

const mockRequirementService = requirementService as any

describe('CreateRequirementDialog', () => {
  const user = userEvent.setup()
  
  const defaultProps = {
    open: true,
    onClose: vi.fn(),
    onSuccess: vi.fn(),
    type: 'definition' as const
  }

  beforeEach(() => {
    vi.clearAllMocks()
  })

  describe('基础渲染测试', () => {
    test('应该正确渲染对话框标题和表单字段', () => {
      render(<CreateRequirementDialog {...defaultProps} />)
      
      expect(screen.getByText('创建需求定义')).toBeInTheDocument()
      expect(screen.getByLabelText('需求ID *')).toBeInTheDocument()
      expect(screen.getByLabelText('需求名称 *')).toBeInTheDocument()
      expect(screen.getByLabelText('需求文本 *')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: '取消' })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: '创建' })).toBeInTheDocument()
    })

    test('type为usage时应该显示不同的标题', () => {
      render(<CreateRequirementDialog {...defaultProps} type="usage" />)
      
      expect(screen.getByText('创建需求使用')).toBeInTheDocument()
    })

    test('对话框关闭时不应该显示', () => {
      render(<CreateRequirementDialog {...defaultProps} open={false} />)
      
      expect(screen.queryByText('创建需求定义')).not.toBeInTheDocument()
    })
  })

  describe('表单验证测试', () => {
    test('必填字段为空时应该显示验证错误', async () => {
      render(<CreateRequirementDialog {...defaultProps} />)
      
      const createButton = screen.getByRole('button', { name: '创建' })
      await user.click(createButton)
      
      await waitFor(() => {
        expect(screen.getByText('请输入需求ID')).toBeInTheDocument()
        expect(screen.getByText('请输入需求名称')).toBeInTheDocument()
        expect(screen.getByText('请输入需求文本')).toBeInTheDocument()
      })
      
      expect(mockRequirementService.createRequirementDefinition).not.toHaveBeenCalled()
    })

    test('需求ID格式验证', async () => {
      render(<CreateRequirementDialog {...defaultProps} />)
      
      const reqIdInput = screen.getByLabelText('需求ID *')
      await user.type(reqIdInput, 'invalid-id')
      
      const createButton = screen.getByRole('button', { name: '创建' })
      await user.click(createButton)
      
      await waitFor(() => {
        expect(screen.getByText('需求ID格式不正确，应以REQ-开头')).toBeInTheDocument()
      })
    })

    test('需求文本长度验证', async () => {
      render(<CreateRequirementDialog {...defaultProps} />)
      
      const textInput = screen.getByLabelText('需求文本 *')
      await user.type(textInput, '短文本')  // 少于10个字符
      
      const createButton = screen.getByRole('button', { name: '创建' })
      await user.click(createButton)
      
      await waitFor(() => {
        expect(screen.getByText('需求文本至少需要10个字符')).toBeInTheDocument()
      })
    })
  })

  describe('需求定义创建测试', () => {
    test('成功创建需求定义', async () => {
      const mockRequirement = {
        id: 'req-def-001',
        reqId: 'REQ-001',
        declaredName: '系统性能需求',
        text: '系统应在正常负载下响应时间不超过100ms',
        status: 'draft'
      }
      
      mockRequirementService.createRequirementDefinition.mockResolvedValue(mockRequirement)
      
      render(<CreateRequirementDialog {...defaultProps} />)
      
      // 填写表单
      await user.type(screen.getByLabelText('需求ID *'), 'REQ-001')
      await user.type(screen.getByLabelText('需求名称 *'), '系统性能需求')
      await user.type(screen.getByLabelText('需求文本 *'), '系统应在正常负载下响应时间不超过100ms')
      
      // 选择状态
      const statusSelect = screen.getByLabelText('状态')
      await user.click(statusSelect)
      await user.click(screen.getByText('草稿'))
      
      // 提交表单
      const createButton = screen.getByRole('button', { name: '创建' })
      await user.click(createButton)
      
      // 验证API调用
      await waitFor(() => {
        expect(mockRequirementService.createRequirementDefinition).toHaveBeenCalledWith({
          reqId: 'REQ-001',
          declaredName: '系统性能需求',
          text: '系统应在正常负载下响应时间不超过100ms',
          status: 'draft',
          isAbstract: false,
          tags: []
        })
      })
      
      // 验证成功回调
      expect(defaultProps.onSuccess).toHaveBeenCalled()
      expect(message.success).toHaveBeenCalledWith('需求定义创建成功')
    })

    test('API错误处理', async () => {
      const errorMessage = 'reqId already exists'
      mockRequirementService.createRequirementDefinition.mockRejectedValue({
        response: {
          status: 409,
          data: { message: errorMessage }
        }
      })
      
      render(<CreateRequirementDialog {...defaultProps} />)
      
      // 填写表单
      await user.type(screen.getByLabelText('需求ID *'), 'REQ-DUPLICATE')
      await user.type(screen.getByLabelText('需求名称 *'), '重复需求')
      await user.type(screen.getByLabelText('需求文本 *'), '这是一个重复的需求ID测试')
      
      // 提交表单
      const createButton = screen.getByRole('button', { name: '创建' })
      await user.click(createButton)
      
      // 验证错误处理
      await waitFor(() => {
        expect(message.error).toHaveBeenCalledWith('创建失败: reqId already exists')
      })
      
      // 验证对话框仍然打开（没有调用onSuccess）
      expect(defaultProps.onSuccess).not.toHaveBeenCalled()
      expect(screen.getByText('创建需求定义')).toBeInTheDocument()
    })
  })

  describe('需求使用创建测试', () => {
    test('创建需求使用时显示模板选择', () => {
      render(<CreateRequirementDialog {...defaultProps} type="usage" />)
      
      expect(screen.getByText('创建需求使用')).toBeInTheDocument()
      expect(screen.getByLabelText('基于模板')).toBeInTheDocument()
    })

    test('成功创建需求使用', async () => {
      const mockUsage = {
        id: 'req-usage-001',
        definition: 'req-def-001',
        declaredName: '登录性能需求',
        text: '用户登录响应时间应小于2秒',
        parameterValues: { timeout: '2s' }
      }
      
      mockRequirementService.createRequirementUsage.mockResolvedValue(mockUsage)
      
      render(<CreateRequirementDialog {...defaultProps} type="usage" templateId="req-def-001" />)
      
      // 填写表单
      await user.type(screen.getByLabelText('需求名称 *'), '登录性能需求')
      await user.type(screen.getByLabelText('需求文本 *'), '用户登录响应时间应小于2秒')
      
      // 提交表单
      const createButton = screen.getByRole('button', { name: '创建' })
      await user.click(createButton)
      
      // 验证API调用
      await waitFor(() => {
        expect(mockRequirementService.createRequirementUsage).toHaveBeenCalledWith({
          definition: 'req-def-001',
          declaredName: '登录性能需求',
          text: '用户登录响应时间应小于2秒',
          parameterValues: {}
        })
      })
      
      expect(defaultProps.onSuccess).toHaveBeenCalled()
      expect(message.success).toHaveBeenCalledWith('需求使用创建成功')
    })
  })

  describe('用户交互测试', () => {
    test('取消按钮应该关闭对话框', async () => {
      render(<CreateRequirementDialog {...defaultProps} />)
      
      const cancelButton = screen.getByRole('button', { name: '取消' })
      await user.click(cancelButton)
      
      expect(defaultProps.onClose).toHaveBeenCalled()
    })

    test('ESC键应该关闭对话框', async () => {
      render(<CreateRequirementDialog {...defaultProps} />)
      
      await user.keyboard('{Escape}')
      
      expect(defaultProps.onClose).toHaveBeenCalled()
    })

    test('标签输入功能', async () => {
      render(<CreateRequirementDialog {...defaultProps} />)
      
      // 测试标签输入
      const tagInput = screen.getByLabelText('标签')
      await user.type(tagInput, 'performance')
      await user.keyboard('{Enter}')
      
      expect(screen.getByText('performance')).toBeInTheDocument()
      
      // 测试添加多个标签
      await user.type(tagInput, 'critical')
      await user.keyboard('{Enter}')
      
      expect(screen.getByText('critical')).toBeInTheDocument()
    })

    test('加载状态显示', async () => {
      // 让API调用挂起
      mockRequirementService.createRequirementDefinition.mockImplementation(
        () => new Promise(resolve => setTimeout(() => resolve({
          id: 'req-001',
          reqId: 'REQ-001',
          declaredName: '测试需求',
          text: '测试需求文本内容足够长'
        }), 1000))
      )
      
      render(<CreateRequirementDialog {...defaultProps} />)
      
      // 填写并提交表单
      await user.type(screen.getByLabelText('需求ID *'), 'REQ-001')
      await user.type(screen.getByLabelText('需求名称 *'), '测试需求')
      await user.type(screen.getByLabelText('需求文本 *'), '测试需求文本内容足够长')
      
      const createButton = screen.getByRole('button', { name: '创建' })
      await user.click(createButton)
      
      // 验证加载状态
      expect(screen.getByRole('button', { name: '创建中...' })).toBeInTheDocument()
      expect(screen.getByRole('button', { name: '创建中...' })).toBeDisabled()
    })
  })
})