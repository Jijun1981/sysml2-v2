/**
 * REQ-F1-1: RequirementDefinition CRUD前端集成测试
 * 测试需求定义的创建、查看、编辑、删除功能
 * 
 * 字段标准化更新：
 * - 使用 declaredName 替代 name
 * - 使用 documentation 替代 text
 * - 确保没有 subject 字段
 * - 确保没有 of 字段（RequirementUsage 使用 requirementDefinition）
 */

import React from 'react'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { message } from 'antd'
import App from '../../App'
import { requirementService } from '../../services/requirementService'

import { vi } from 'vitest'

// Mock requirementService
vi.mock('../../services/requirementService', () => ({
  requirementService: {
    createRequirementDefinition: vi.fn(),
    getRequirements: vi.fn(),
    updateRequirement: vi.fn(),
    deleteRequirement: vi.fn(),
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

describe('RequirementDefinition CRUD Integration', () => {
  const user = userEvent.setup()

  beforeEach(() => {
    vi.clearAllMocks()
    
    // Mock 默认返回空列表
    mockRequirementService.getRequirements.mockResolvedValue({
      content: [],
      totalElements: 0,
      page: 0,
      size: 50
    })
  })

  describe('REQ-F1-1: RequirementDefinition CRUD前端集成', () => {
    test('AC1: 前端主界面提供"创建需求定义"按钮', async () => {
      render(<App />)
      
      // 等待界面加载
      await waitFor(() => {
        expect(screen.getByText('SysML v2 建模平台')).toBeInTheDocument()
      })
      
      // 验证创建需求定义按钮存在
      const createButton = screen.getByRole('button', { name: /创建需求定义/i })
      expect(createButton).toBeInTheDocument()
      expect(createButton).toBeEnabled()
    })

    test('AC2: 点击后弹出CreateRequirementDialog', async () => {
      render(<App />)
      
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /创建需求定义/i })).toBeInTheDocument()
      })
      
      const createButton = screen.getByRole('button', { name: /创建需求定义/i })
      await user.click(createButton)
      
      // 验证对话框出现 - 通过查找modal title
      await waitFor(() => {
        expect(screen.getByRole('dialog')).toBeInTheDocument()
        const modalTitle = screen.getByText((content, element) => {
          return element?.className === 'ant-modal-title' && content === '创建需求定义'
        })
        expect(modalTitle).toBeInTheDocument()
      })
      
      // 验证表单字段存在 - 使用placeholder更可靠
      expect(screen.getByPlaceholderText('例如：REQ-001')).toBeInTheDocument()
      expect(screen.getByPlaceholderText('需求的完整名称')).toBeInTheDocument()
      expect(screen.getByPlaceholderText(/需求的详细描述/)).toBeInTheDocument()
    })

    test('AC3: 填写标准化字段后调用requirementService.createRequirementDefinition()', async () => {
      const mockRequirement = {
        elementId: 'req-def-001',
        eClass: 'RequirementDefinition',
        reqId: 'REQ-001',
        declaredName: '系统性能需求',
        documentation: '系统应在正常负载下响应时间不超过100ms',
        status: 'draft',
        priority: 'P0',
        verificationMethod: 'test'
      }
      
      mockRequirementService.createRequirementDefinition.mockResolvedValue(mockRequirement)
      
      render(<App />)
      
      // 点击创建按钮
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /创建需求定义/i })).toBeInTheDocument()
      })
      
      const createButton = screen.getByRole('button', { name: /创建需求定义/i })
      await user.click(createButton)
      
      // 填写表单 - 使用placeholder查找输入框更可靠
      await waitFor(() => {
        expect(screen.getByPlaceholderText('例如：REQ-001')).toBeInTheDocument()
      })
      
      await user.type(screen.getByPlaceholderText('例如：REQ-001'), 'REQ-001')
      await user.type(screen.getByPlaceholderText('需求的完整名称'), '系统性能需求')
      await user.type(screen.getByPlaceholderText(/需求的详细描述/), '系统应在正常负载下响应时间不超过100ms')
      
      // 提交表单
      const submitButton = screen.getByRole('button', { name: '创建' })
      await user.click(submitButton)
      
      // 验证API调用 - 使用标准化字段
      await waitFor(() => {
        expect(mockRequirementService.createRequirementDefinition).toHaveBeenCalledWith(
          expect.objectContaining({
            reqId: 'REQ-001',
            declaredName: '系统性能需求',
            documentation: '系统应在正常负载下响应时间不超过100ms',
            status: 'draft',
            priority: 'P2',  // 默认优先级是P2
            verificationMethod: 'test'
          })
        )
      })
      
      // 验证成功消息
      expect(message.success).toHaveBeenCalledWith('需求定义创建成功')
    })

    test('AC4: 创建成功后自动刷新需求列表', async () => {
      const newRequirement = {
        elementId: 'req-def-001',
        reqId: 'REQ-001',
        declaredName: '系统性能需求',
        documentation: '系统应在正常负载下响应时间不超过100ms'
      }
      
      mockRequirementService.createRequirementDefinition.mockResolvedValue(newRequirement)
      
      // 第一次调用返回空，第二次调用返回新创建的需求
      mockRequirementService.getRequirements
        .mockResolvedValueOnce({
          content: [],
          totalElements: 0,
          page: 0,
          size: 50
        })
        .mockResolvedValueOnce({
          content: [newRequirement],
          totalElements: 1,
          page: 0,
          size: 50
        })
      
      render(<App />)
      
      // 创建需求
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /创建需求定义/i })).toBeInTheDocument()
      })
      
      const createButton = screen.getByRole('button', { name: /创建需求定义/i })
      await user.click(createButton)
      
      await waitFor(() => {
        expect(screen.getByLabelText('需求ID *')).toBeInTheDocument()
      })
      
      await user.type(screen.getByLabelText('需求ID *'), 'REQ-001')
      await user.type(screen.getByLabelText('需求名称 *'), '系统性能需求')
      await user.type(screen.getByLabelText('需求文档 *'), '系统应在正常负载下响应时间不超过100ms')
      
      const submitButton = screen.getByRole('button', { name: /确定|创建/i })
      await user.click(submitButton)
      
      // 验证API被调用两次：初始加载 + 创建后刷新
      await waitFor(() => {
        expect(mockRequirementService.getRequirements).toHaveBeenCalledTimes(2)
      })
      
      // 验证新需求出现在列表中
      await waitFor(() => {
        expect(screen.getByText('REQ-001')).toBeInTheDocument()
        expect(screen.getByText('系统性能需求')).toBeInTheDocument()
      })
    })

    test('错误处理: API调用失败时显示错误信息', async () => {
      const errorMessage = 'reqId already exists'
      mockRequirementService.createRequirementDefinition.mockRejectedValue(
        new Error(errorMessage)
      )
      
      render(<App />)
      
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /创建需求定义/i })).toBeInTheDocument()
      })
      
      const createButton = screen.getByRole('button', { name: /创建需求定义/i })
      await user.click(createButton)
      
      await waitFor(() => {
        expect(screen.getByPlaceholderText('例如：REQ-001')).toBeInTheDocument()
      })
      
      await user.type(screen.getByPlaceholderText('例如：REQ-001'), 'REQ-DUPLICATE')
      await user.type(screen.getByPlaceholderText('需求的完整名称'), '重复需求')
      await user.type(screen.getByPlaceholderText(/需求的详细描述/), '这是一个重复的需求')
      
      const submitButton = screen.getByRole('button', { name: /确定|创建/i })
      await user.click(submitButton)
      
      // 验证错误消息显示
      await waitFor(() => {
        expect(message.error).toHaveBeenCalledWith(`创建失败: ${errorMessage}`)
      })
    })

    test('表单验证: 必填字段验证', async () => {
      render(<App />)
      
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /创建需求定义/i })).toBeInTheDocument()
      })
      
      const createButton = screen.getByRole('button', { name: /创建需求定义/i })
      await user.click(createButton)
      
      await waitFor(() => {
        expect(screen.getByRole('button', { name: '创建' })).toBeInTheDocument()
      })
      
      // 直接点击提交按钮（不填写任何字段）
      const submitButton = screen.getByRole('button', { name: '创建' })
      await user.click(submitButton)
      
      // 验证必填字段提示出现
      await waitFor(() => {
        expect(screen.getByText('请输入需求ID')).toBeInTheDocument()
        expect(screen.getByText('请输入需求名称')).toBeInTheDocument()
        expect(screen.getByText('请输入需求文档')).toBeInTheDocument()
      })
      
      // 验证API没有被调用
      expect(mockRequirementService.createRequirementDefinition).not.toHaveBeenCalled()
    })
  })

  describe('字段标准化验证', () => {
    test('不应该显示或使用subject字段', async () => {
      render(<App />)
      
      // 打开创建对话框
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /创建需求定义/i })).toBeInTheDocument()
      })
      
      const createButton = screen.getByRole('button', { name: /创建需求定义/i })
      await user.click(createButton)
      
      // 等待对话框打开
      await waitFor(() => {
        expect(screen.getByRole('dialog')).toBeInTheDocument()
      })
      
      // 验证subject字段不存在
      expect(screen.queryByLabelText(/subject/i)).not.toBeInTheDocument()
      expect(screen.queryByText(/约束对象/i)).not.toBeInTheDocument()
    })
    
    test('应该使用documentation而不是text字段', async () => {
      render(<App />)
      
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /创建需求定义/i })).toBeInTheDocument()
      })
      
      const createButton = screen.getByRole('button', { name: /创建需求定义/i })
      await user.click(createButton)
      
      await waitFor(() => {
        expect(screen.getByRole('dialog')).toBeInTheDocument()
      })
      
      // 验证使用documentation字段 - 通过placeholder查找
      expect(screen.getByPlaceholderText(/需求的详细描述/)).toBeInTheDocument()
    })
    
    test('应该显示元数据字段（status、priority、verificationMethod）', async () => {
      render(<App />)
      
      await waitFor(() => {
        expect(screen.getByRole('button', { name: /创建需求定义/i })).toBeInTheDocument()
      })
      
      const createButton = screen.getByRole('button', { name: /创建需求定义/i })
      await user.click(createButton)
      
      await waitFor(() => {
        expect(screen.getByRole('dialog')).toBeInTheDocument()
      })
      
      // 验证元数据字段存在 - 通过查找select框的文本
      expect(screen.getByText('状态')).toBeInTheDocument()
      expect(screen.getByText('优先级')).toBeInTheDocument()
      expect(screen.getByText('验证方法')).toBeInTheDocument()
    })
  })

  describe('需求列表显示测试', () => {
    test('应该显示需求列表在三个视图中', async () => {
      const mockRequirements = [
        {
          elementId: 'req-def-001',
          reqId: 'REQ-001',
          declaredName: '性能需求',
          documentation: '系统响应时间要求',
          eClass: 'RequirementDefinition',
          status: 'active',
          priority: 'P0'
        },
        {
          elementId: 'req-usage-001', 
          reqId: 'REQ-U-001',
          declaredName: '具体性能需求',
          documentation: '登录响应时间<2s',
          eClass: 'RequirementUsage',
          requirementDefinition: 'req-def-001'  // 使用标准化字段
        }
      ]
      
      mockRequirementService.getRequirements.mockResolvedValue({
        content: mockRequirements,
        totalElements: 2,
        page: 0,
        size: 50
      })
      
      render(<App />)
      
      // 等待数据加载
      await waitFor(() => {
        expect(mockRequirementService.getRequirements).toHaveBeenCalled()
      })
      
      // 验证需求在不同视图中显示
      await waitFor(() => {
        // 在表视图中显示
        expect(screen.getByText('REQ-001')).toBeInTheDocument()
        expect(screen.getByText('性能需求')).toBeInTheDocument()
        
        // 检查是否有视图切换按钮
        expect(screen.getByText('表格视图') || screen.getByRole('tab', { name: /表格/i })).toBeInTheDocument()
      })
    })
  })
})