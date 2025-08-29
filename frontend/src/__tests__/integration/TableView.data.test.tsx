/**
 * REQ-F2-2: 表视图数据集成测试
 * 测试表视图显示标准化后的真实需求字段
 * 
 * 验收标准：
 * - 显示reqId、declaredName、documentation等核心字段
 * - 显示status、priority、verificationMethod等元数据字段  
 * - Usage行显示requirementDefinition关联
 * - 支持字段排序和过滤
 */

import React from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import App from '../../App'
import { vi } from 'vitest'

// Mock requirementService  
vi.mock('../../services/requirementService', () => ({
  requirementService: {
    getRequirements: vi.fn(),
    getAllRequirements: vi.fn(),
  }
}))

import { requirementService } from '../../services/requirementService'
const mockRequirementService = requirementService as any

describe('REQ-F2-2: 表视图数据集成', () => {
  const user = userEvent.setup()
  
  beforeEach(() => {
    vi.clearAllMocks()
  })

  const mockRequirementsData = [
    {
      elementId: 'req-def-001',
      reqId: 'REQ-001', 
      declaredName: '系统性能需求',
      documentation: '系统应在正常负载下响应时间不超过100ms',
      status: 'active',
      priority: 'P1',
      verificationMethod: 'test',
      requirementUsage: false
    },
    {
      elementId: 'req-usage-001',
      reqId: 'REQ-U-001',
      declaredName: '具体性能需求',
      documentation: '登录响应时间<2s',
      status: 'draft', 
      priority: 'P2',
      verificationMethod: 'analysis',
      requirementUsage: true,
      requirementDefinition: 'req-def-001'
    }
  ]

  describe('AC1: 显示reqId、declaredName、documentation等核心字段', () => {
    test('表视图应该显示核心字段列', async () => {
      mockRequirementService.getAllRequirements.mockResolvedValue(mockRequirementsData)
      
      render(<App />)
      
      // 切换到表格视图
      await waitFor(() => {
        expect(screen.getByText('📊 表格视图') || screen.getByRole('tab', { name: /表格/i })).toBeInTheDocument()
      })
      
      const tableTab = screen.getByText('📊 表格视图')
      await user.click(tableTab)
      
      // 验证表头显示核心字段
      await waitFor(() => {
        expect(screen.getByText('需求ID')).toBeInTheDocument()
        expect(screen.getByText('名称')).toBeInTheDocument() 
        expect(screen.getByText('文档')).toBeInTheDocument()
      })
    })

    test('表视图应该显示具体的需求数据', async () => {
      mockRequirementService.getAllRequirements.mockResolvedValue(mockRequirementsData)
      
      render(<App />)
      
      // 切换到表格视图并等待数据加载
      const tableTab = await screen.findByText('📊 表格视图')
      await user.click(tableTab)
      
      // 验证具体数据显示
      await waitFor(() => {
        expect(screen.getByText('REQ-001')).toBeInTheDocument()
        expect(screen.getByText('系统性能需求')).toBeInTheDocument()
        expect(screen.getByText(/系统应在正常负载下响应时间/)).toBeInTheDocument()
      })
    })
  })

  describe('AC2: 显示status、priority、verificationMethod等元数据字段', () => {
    test('表视图应该显示元数据字段列', async () => {
      mockRequirementService.getAllRequirements.mockResolvedValue(mockRequirementsData)
      
      render(<App />)
      
      const tableTab = await screen.findByText('📊 表格视图')
      await user.click(tableTab)
      
      // 验证元数据字段列存在
      await waitFor(() => {
        expect(screen.getByText('状态')).toBeInTheDocument()
        expect(screen.getByText('优先级')).toBeInTheDocument()
        expect(screen.getByText('验证方法')).toBeInTheDocument()
      })
    })

    test('表视图应该显示具体的元数据值', async () => {
      mockRequirementService.getAllRequirements.mockResolvedValue(mockRequirementsData)
      
      render(<App />)
      
      const tableTab = await screen.findByText('📊 表格视图')
      await user.click(tableTab)
      
      // 验证元数据值显示
      await waitFor(() => {
        expect(screen.getByText('active')).toBeInTheDocument()
        expect(screen.getByText('P1')).toBeInTheDocument()
        expect(screen.getByText('test')).toBeInTheDocument()
        expect(screen.getByText('draft')).toBeInTheDocument() 
        expect(screen.getByText('P2')).toBeInTheDocument()
        expect(screen.getByText('analysis')).toBeInTheDocument()
      })
    })
  })

  describe('AC3: Usage行显示requirementDefinition关联', () => {
    test('RequirementUsage应该显示关联的Definition', async () => {
      mockRequirementService.getAllRequirements.mockResolvedValue(mockRequirementsData)
      
      render(<App />)
      
      const tableTab = await screen.findByText('📊 表格视图')
      await user.click(tableTab)
      
      // 验证Usage显示关联Definition
      await waitFor(() => {
        expect(screen.getByText('REQ-U-001')).toBeInTheDocument()
        expect(screen.getByText('具体性能需求')).toBeInTheDocument()
        // 应该显示关联的Definition ID
        expect(screen.getByText('req-def-001')).toBeInTheDocument()
      })
    })

    test('表视图应该有关联定义列', async () => {
      mockRequirementService.getAllRequirements.mockResolvedValue(mockRequirementsData)
      
      render(<App />)
      
      const tableTab = await screen.findByText('📊 表格视图')
      await user.click(tableTab)
      
      // 验证关联定义列存在
      await waitFor(() => {
        expect(screen.getByText('关联定义')).toBeInTheDocument()
      })
    })
  })

  describe('数据加载和错误处理', () => {
    test('应该调用正确的API加载数据', async () => {
      mockRequirementService.getAllRequirements.mockResolvedValue(mockRequirementsData)
      
      render(<App />)
      
      const tableTab = await screen.findByText('📊 表格视图')
      await user.click(tableTab)
      
      // 验证API被调用
      await waitFor(() => {
        expect(mockRequirementService.getAllRequirements).toHaveBeenCalled()
      })
    })

    test('数据加载失败时应该显示错误信息', async () => {
      mockRequirementService.getAllRequirements.mockRejectedValue(new Error('API Error'))
      
      render(<App />)
      
      const tableTab = await screen.findByText('📊 表格视图')
      await user.click(tableTab)
      
      // 验证错误处理
      await waitFor(() => {
        expect(screen.getByText(/加载失败|错误|无法加载/)).toBeInTheDocument()
      })
    })
  })
})