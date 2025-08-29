/**
 * TEST-F1-2-1: RequirementUsage CRUD前端集成测试（字段标准化版）
 * 
 * 需求: REQ-F1-2 - RequirementUsage CRUD前端集成
 * 验收标准:
 * - 前端主界面提供'创建需求使用'按钮
 * - 必须选择关联的RequirementDefinition (requirementDefinition字段)
 * - 不再需要subject字段（已删除）
 * - 填写参数后调用requirementService.createRequirementUsage()
 * - 正确处理requirementDefinition字段验证
 * 
 * 字段变更:
 * - 删除: subject字段
 * - 使用: requirementDefinition 替代 of
 * - 使用: documentation 替代 text
 */

import React from 'react'
import { render, screen, waitFor, within, act } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import '@testing-library/jest-dom'
import { vi, describe, it, expect, beforeEach } from 'vitest'
import { ModelProvider } from '../../contexts/ModelContext'
import ModelViewerClean from '../../components/ModelViewerClean'
import { requirementService } from '../../services/requirementService'

// Mock requirementService
vi.mock('../../services/requirementService', () => ({
  requirementService: {
    getAllRequirements: vi.fn(),
    getRequirementDefinitions: vi.fn(),
    getRequirementUsages: vi.fn(),
    createRequirementUsage: vi.fn(),
    createRequirementDefinition: vi.fn(),
    updateRequirement: vi.fn(),
    deleteRequirement: vi.fn()
  }
}))

// Mock fetch for API calls
global.fetch = vi.fn() as any

describe('RequirementUsage CRUD前端集成测试', () => {
  
  beforeEach(() => {
    vi.clearAllMocks()
    
    // Mock Definition列表数据 - Usage必须基于这些Definition创建
    const mockDefinitions = [
      {
        elementId: 'def-001',
        declaredName: '性能需求模板',
        reqId: 'REQ-PERF-001',
        documentation: '系统响应时间应小于2秒'  // 使用documentation替代text
      },
      {
        elementId: 'def-002',
        declaredName: '安全需求模板',
        reqId: 'REQ-SEC-001',
        documentation: '系统应支持双因素认证'  // 使用documentation替代text
      }
    ]
    
    // Mock API responses
    ;(requirementService.getRequirementDefinitions as any).mockResolvedValue({
      content: mockDefinitions,
      totalElements: 2
    })
    
    ;(requirementService.getAllRequirements as any).mockResolvedValue({
      content: [],
      totalElements: 0
    })
    
    ;(requirementService.getRequirementUsages as any).mockResolvedValue({
      content: [],
      totalElements: 0
    })
    
    // Mock fetch for other API calls
    ;(fetch as any).mockImplementation((url: string) => {
      if (url.includes('/requirements')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve({ content: mockDefinitions })
        })
      }
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve({ content: [] })
      })
    })
  })

  describe('AC1: 前端主界面提供创建需求使用按钮', () => {
    
    it('应该在主界面显示"创建使用"按钮', async () => {
      render(
        <ModelProvider>
          <ModelViewerClean />
        </ModelProvider>
      )
      
      await waitFor(() => {
        // 查找创建使用按钮
        const createUsageButton = screen.getByText(/创建使用/i)
        expect(createUsageButton).toBeInTheDocument()
      })
    })
    
    it('点击创建使用按钮应该打开创建对话框', async () => {
      const user = userEvent.setup()
      
      render(
        <ModelProvider>
          <ModelViewerClean />
        </ModelProvider>
      )
      
      // 点击创建使用按钮
      const createUsageButton = await screen.findByText(/创建使用/i)
      await user.click(createUsageButton)
      
      // 验证对话框打开
      await waitFor(() => {
        expect(screen.getByRole('dialog')).toBeInTheDocument()
        expect(screen.getByText(/创建需求使用/i)).toBeInTheDocument()
      })
    })
  })

  describe('AC2: 必须选择关联的RequirementDefinition（requirementDefinition字段）', () => {
    
    it('创建对话框应该强制要求选择Definition', async () => {
      const user = userEvent.setup()
      
      render(
        <ModelProvider>
          <ModelViewerClean />
        </ModelProvider>
      )
      
      // 打开创建使用对话框
      const createUsageButton = await screen.findByText(/创建使用/i)
      await user.click(createUsageButton)
      
      await waitFor(() => {
        // 应该有RequirementDefinition选择器，且是必选项
        const definitionSelect = screen.getByLabelText(/需求定义|RequirementDefinition/i)
        expect(definitionSelect).toBeInTheDocument()
        expect(definitionSelect).toHaveAttribute('aria-required', 'true')
      })
    })
    
    it('Definition选择器应该包含所有可用的定义', async () => {
      const user = userEvent.setup()
      
      render(
        <ModelProvider>
          <ModelViewerClean />
        </ModelProvider>
      )
      
      // 打开创建使用对话框
      const createUsageButton = await screen.findByText(/创建使用/i)
      await user.click(createUsageButton)
      
      // 点击Definition选择器
      const definitionSelect = await screen.findByLabelText(/基于定义/i)
      await user.click(definitionSelect)
      
      // 验证选项列表包含mock的definitions
      await waitFor(() => {
        const dropdown = screen.getByRole('listbox')
        // 使用aria-label查询，因为Ant Design Select可能渲染方式不同
        const option1 = within(dropdown).getByRole('option', { name: '性能需求模板' })
        const option2 = within(dropdown).getByRole('option', { name: '安全需求模板' })
        expect(option1).toBeInTheDocument()
        expect(option2).toBeInTheDocument()
      })
    })
    
    it('选择Definition后应该自动填充相关信息', async () => {
      const user = userEvent.setup()
      
      render(
        <ModelProvider>
          <ModelViewerClean />
        </ModelProvider>
      )
      
      // 打开创建使用对话框
      const createUsageButton = await screen.findByText(/创建使用/i)
      await user.click(createUsageButton)
      
      // 选择一个Definition
      const definitionSelect = await screen.findByLabelText(/基于定义/i)
      await user.click(definitionSelect)
      
      // 使用role查询选项
      const option = await screen.findByRole('option', { name: '性能需求模板' })
      await user.click(option)
      
      // 验证相关字段被填充或显示
      await waitFor(() => {
        // 应该显示选中的模板信息
        expect(screen.getByText(/性能需求模板/)).toBeInTheDocument()
      })
    })
  })

  describe('AC3: 填写实例化参数并创建', () => {
    
    it('应该提供必要的实例化参数输入字段', async () => {
      const user = userEvent.setup()
      
      render(
        <ModelProvider>
          <ModelViewerClean />
        </ModelProvider>
      )
      
      // 打开创建使用对话框
      const createUsageButton = await screen.findByText(/创建使用/i)
      await user.click(createUsageButton)
      
      await waitFor(() => {
        // 验证必要的输入字段（不再有subject字段）
        expect(screen.getByLabelText(/名称|declaredName/i)).toBeInTheDocument()
        expect(screen.getByLabelText(/文档|documentation/i)).toBeInTheDocument()
        // 验证subject字段不存在
        expect(screen.queryByLabelText(/subject|应用主体/i)).not.toBeInTheDocument()
      })
    })
    
    it('填写完整信息后应该调用createRequirementUsage', async () => {
      const user = userEvent.setup()
      
      // 设置mock返回值
      ;(requirementService.createRequirementUsage as any).mockResolvedValue({
        elementId: 'usage-001',
        declaredName: 'Web应用性能需求',
        requirementDefinition: 'def-001',  // 使用requirementDefinition替代of
        documentation: 'Web应用响应时间应小于500ms'
      })
      
      render(
        <ModelProvider>
          <ModelViewerClean />
        </ModelProvider>
      )
      
      // 打开创建使用对话框
      const createUsageButton = await screen.findByText(/创建使用/i)
      console.log('Found create usage button')
      
      await act(async () => {
        await user.click(createUsageButton)
      })
      console.log('Clicked create usage button')
      
      // 等待对话框出现
      await waitFor(() => {
        const dialog = screen.getByRole('dialog')
        expect(dialog).toBeInTheDocument()
        console.log('Dialog found in DOM')
      }, { timeout: 3000 })
      console.log('Dialog opened')
      
      // 选择Definition
      const definitionSelect = await screen.findByLabelText(/基于定义/i)
      console.log('Found definition select')
      
      await act(async () => {
        await user.click(definitionSelect)
      })
      console.log('Clicked definition select')
      
      const option = await screen.findByRole('option', { name: '性能需求模板' })
      console.log('Found option')
      
      await act(async () => {
        await user.click(option)
      })
      console.log('Selected option')
      
      // 填写实例化参数
      console.log('Looking for name input')
      const nameInput = await screen.findByLabelText(/名称/i)
      console.log('Found name input')
      await user.click(nameInput)
      await user.clear(nameInput)
      await user.type(nameInput, 'Web应用性能需求')
      console.log('Filled name input')
      
      // subject字段已删除，不再需要填写
      
      console.log('Looking for documentation input')
      const docInput = await screen.findByLabelText(/文档|documentation/i)
      console.log('Found documentation input')
      await user.clear(docInput)
      await user.type(docInput, 'Web应用响应时间应小于500ms')
      console.log('Filled documentation input')
      
      // 提交表单 - Ant Design Modal会在中文字符间加空格
      console.log('Looking for submit button')
      await waitFor(() => {
        const submitButton = screen.getByRole('button', { name: '创 建' })
        expect(submitButton).toBeInTheDocument()
        console.log('Found submit button')
      })
      
      const submitButton = screen.getByRole('button', { name: '创 建' })
      console.log('Clicking submit button')
      await act(async () => {
        await user.click(submitButton)
      })
      console.log('Submit button clicked')
      
      // 验证调用了createRequirementUsage
      await waitFor(() => {
        expect(requirementService.createRequirementUsage).toHaveBeenCalledWith(
          expect.objectContaining({
            declaredName: 'Web应用性能需求',
            requirementDefinition: 'def-001',  // 使用requirementDefinition
            documentation: 'Web应用响应时间应小于500ms'  // 使用documentation
          })
        )
      }, { timeout: 3000 })
    })
    
    it('创建成功后应该显示成功消息并刷新列表', async () => {
      const user = userEvent.setup()
      
      ;(requirementService.createRequirementUsage as any).mockResolvedValue({
        elementId: 'usage-001',
        declaredName: 'Web应用性能需求'
      })
      
      render(
        <ModelProvider>
          <ModelViewerClean />
        </ModelProvider>
      )
      
      // 打开创建对话框并填写信息
      const createUsageButton = await screen.findByText(/创建使用/i)
      await user.click(createUsageButton)
      
      // 选择Definition
      const definitionSelect = await screen.findByLabelText(/基于定义/i)
      await user.click(definitionSelect)
      const option = await screen.findByRole('option', { name: '性能需求模板' })
      await user.click(option)
      
      // 填写名称
      const nameInput = await screen.findByLabelText(/名称/i)
      await user.clear(nameInput)
      await user.type(nameInput, 'Web应用性能需求')
      
      // 提交 - Ant Design Modal会在中文字符间加空格
      const submitButton = screen.getByRole('button', { name: '创 建' })
      await user.click(submitButton)
      
      // 验证成功消息
      await waitFor(() => {
        expect(screen.getByText(/创建成功/i)).toBeInTheDocument()
      })
      
      // 验证刷新了数据
      expect(requirementService.getAllRequirements).toHaveBeenCalledTimes(2) // 初始加载 + 刷新
    })
  })

  describe('字段标准化验证', () => {
    
    it('不应该显示subject字段', async () => {
      const user = userEvent.setup()
      
      render(
        <ModelProvider>
          <ModelViewerClean />
        </ModelProvider>
      )
      
      // 打开创建使用对话框
      const createUsageButton = await screen.findByText(/创建使用/i)
      await user.click(createUsageButton)
      
      await waitFor(() => {
        // 验证subject字段不存在
        expect(screen.queryByLabelText(/subject/i)).not.toBeInTheDocument()
        expect(screen.queryByLabelText(/约束对象/i)).not.toBeInTheDocument()
        expect(screen.queryByLabelText(/应用主体/i)).not.toBeInTheDocument()
      })
    })
    
    it('应该使用requirementDefinition字段而不是of字段', async () => {
      const user = userEvent.setup()
      
      render(
        <ModelProvider>
          <ModelViewerClean />
        </ModelProvider>
      )
      
      // 打开创建使用对话框
      const createUsageButton = await screen.findByText(/创建使用/i)
      await user.click(createUsageButton)
      
      await waitFor(() => {
        // 验证使用正确的字段名
        const definitionSelect = screen.getByLabelText(/需求定义|RequirementDefinition/i)
        expect(definitionSelect).toBeInTheDocument()
        
        // 验证不使用of字段
        expect(screen.queryByLabelText(/\bof\b/i)).not.toBeInTheDocument()
      })
    })
    
    it('应该使用documentation字段而不是text字段', async () => {
      const user = userEvent.setup()
      
      render(
        <ModelProvider>
          <ModelViewerClean />
        </ModelProvider>
      )
      
      // 打开创建使用对话框
      const createUsageButton = await screen.findByText(/创建使用/i)
      await user.click(createUsageButton)
      
      await waitFor(() => {
        // 验证使用documentation字段
        expect(screen.getByLabelText(/文档|documentation/i)).toBeInTheDocument()
        
        // 验证不使用text字段（除非在标签文本中）
        const textInputs = screen.queryAllByLabelText(/^text$/i)
        expect(textInputs).toHaveLength(0)
      })
    })
  })

  describe('错误处理', () => {
    
    it('创建失败时应该显示错误消息', async () => {
      const user = userEvent.setup()
      
      // Mock创建失败
      ;(requirementService.createRequirementUsage as any).mockRejectedValue(
        new Error('创建失败: reqId重复')
      )
      
      render(
        <ModelProvider>
          <ModelViewerClean />
        </ModelProvider>
      )
      
      // 打开创建对话框
      const createUsageButton = await screen.findByText(/创建使用/i)
      await user.click(createUsageButton)
      
      // 选择Definition并填写信息
      const definitionSelect = await screen.findByLabelText(/基于定义/i)
      await user.click(definitionSelect)
      const option = await screen.findByRole('option', { name: '性能需求模板' })
      await user.click(option)
      
      const nameInput = await screen.findByLabelText(/名称/i)
      await user.clear(nameInput)
      await user.type(nameInput, 'Test')
      
      // 提交 - Ant Design Modal会在中文字符间加空格
      const submitButton = screen.getByRole('button', { name: '创 建' })
      await user.click(submitButton)
      
      // 验证错误消息
      await waitFor(() => {
        expect(screen.getByText(/创建失败/i)).toBeInTheDocument()
      })
    })
    
    it('未选择Definition时应该提示必选', async () => {
      const user = userEvent.setup()
      
      render(
        <ModelProvider>
          <ModelViewerClean />
        </ModelProvider>
      )
      
      // 打开创建对话框
      const createUsageButton = await screen.findByText(/创建使用/i)
      await user.click(createUsageButton)
      
      // 直接填写名称不选择Definition
      const nameInput = await screen.findByLabelText(/名称/i)
      await user.type(nameInput, 'Test')
      
      // 尝试提交 - Ant Design Modal会在中文字符间加空格
      const submitButton = screen.getByRole('button', { name: '创 建' })
      await user.click(submitButton)
      
      // 验证错误提示
      await waitFor(() => {
        expect(screen.getByText(/请选择基于的定义/i)).toBeInTheDocument()
      })
    })
  })
})