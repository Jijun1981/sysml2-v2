/**
 * TableView数据集成测试
 * REQ-F2-2: 表视图数据集成
 * 
 * 验收标准：
 * - 显示reqId、declaredName、documentation等核心字段
 * - 显示status、priority、verificationMethod等元数据字段
 * - Usage行显示requirementDefinition关联
 * - 支持字段排序和过滤
 */

import React from 'react'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import { describe, test, expect, beforeEach, vi } from 'vitest'
import { ModelProvider } from '../../../contexts/ModelContext'

// 先mock axios避免序列化问题
vi.mock('axios')

// mock所有使用axios的服务
vi.mock('../../../services/requirementService')
vi.mock('../../../services/universalApi')
vi.mock('../../../services/advancedQueryApi')

// 最后导入组件
import TableView from '../TableView'
import { requirementService } from '../../../services/requirementService'

describe('TableView 数据集成测试', () => {
  // 模拟的测试数据 - 使用标准化字段
  const mockRequirementDefinitions = [
    {
      elementId: 'req-def-001',
      eClass: 'RequirementDefinition',
      reqId: 'REQ-001',
      declaredName: '充电时间需求',
      declaredShortName: 'REQ-001',
      documentation: '充电时间不超过30分钟',
      status: 'approved',
      priority: 'P0',
      verificationMethod: 'test'
    },
    {
      elementId: 'req-def-002',
      eClass: 'RequirementDefinition',
      reqId: 'REQ-002',
      declaredName: '电池容量需求',
      declaredShortName: 'REQ-002',
      documentation: '电池容量不低于75kWh',
      status: 'draft',
      priority: 'P1',
      verificationMethod: 'analysis'
    }
  ]

  const mockRequirementUsages = [
    {
      elementId: 'req-usage-001',
      eClass: 'RequirementUsage',
      requirementDefinition: 'req-def-001', // 使用标准化的requirementDefinition字段
      declaredName: '快充场景',
      documentation: '在快充模式下的充电时间需求',
      status: 'implemented',
      priority: 'P0'
    },
    {
      elementId: 'req-usage-002',
      eClass: 'RequirementUsage',
      requirementDefinition: 'req-def-002',
      declaredName: '标准电池包',
      documentation: '标准配置的电池容量',
      status: 'approved',
      priority: 'P1'
    }
  ]

  const mockAllElements = [
    ...mockRequirementDefinitions,
    ...mockRequirementUsages
  ]

  beforeEach(() => {
    // 重置所有mock
    vi.clearAllMocks()
    
    // 设置默认的mock返回值
    ;(requirementService.getAllRequirements as any).mockResolvedValue({
      content: mockAllElements,
      totalElements: 4,
      totalPages: 1,
      number: 0
    })
  })

  describe('标准字段显示', () => {
    test('应该显示核心字段列', async () => {
      render(
        <ModelProvider>
          <TableView />
        </ModelProvider>
      )

      // 等待数据加载完成
      await waitFor(() => {
        expect(requirementService.getAllRequirements).toHaveBeenCalled()
      })

      await waitFor(() => {
        // 验证表头显示核心字段
        expect(screen.getByText('需求ID')).toBeInTheDocument()
        expect(screen.getByText('名称')).toBeInTheDocument()
        expect(screen.getByText('文档')).toBeInTheDocument()
        expect(screen.getByText('类型')).toBeInTheDocument()
      })
    })

    test('应该显示元数据字段列', async () => {
      render(
        <ModelProvider>
          <TableView />
        </ModelProvider>
      )

      await waitFor(() => {
        // 验证表头显示元数据字段
        expect(screen.getByText('状态')).toBeInTheDocument()
        expect(screen.getByText('优先级')).toBeInTheDocument()
        expect(screen.getByText('验证方法')).toBeInTheDocument()
      })
    })

    test('应该正确显示RequirementDefinition数据', async () => {
      render(
        <ModelProvider>
          <TableView />
        </ModelProvider>
      )

      // 等待数据加载完成
      await waitFor(() => {
        expect(requirementService.getAllRequirements).toHaveBeenCalled()
      })

      await waitFor(() => {
        // 验证Definition数据显示
        expect(screen.getByText('REQ-001')).toBeInTheDocument()
        expect(screen.getByText('充电时间需求')).toBeInTheDocument()
        expect(screen.getByText('充电时间不超过30分钟')).toBeInTheDocument()
        expect(screen.getByText('approved')).toBeInTheDocument()
        expect(screen.getByText('P0')).toBeInTheDocument()
        expect(screen.getByText('test')).toBeInTheDocument()
      })
    })

    test('应该正确显示RequirementUsage数据', async () => {
      render(
        <ModelProvider>
          <TableView />
        </ModelProvider>
      )

      // 等待数据加载完成
      await waitFor(() => {
        expect(requirementService.getAllRequirements).toHaveBeenCalled()
      })

      await waitFor(() => {
        // 验证Usage数据显示
        expect(screen.getByText('快充场景')).toBeInTheDocument()
        expect(screen.getByText('在快充模式下的充电时间需求')).toBeInTheDocument()
        expect(screen.getByText('implemented')).toBeInTheDocument()
      })
    })
  })

  describe('Usage-Definition关联显示', () => {
    test('Usage行应该显示关联的Definition', async () => {
      render(
        <ModelProvider>
          <TableView />
        </ModelProvider>
      )

      // 等待数据加载完成
      await waitFor(() => {
        expect(requirementService.getAllRequirements).toHaveBeenCalled()
      })

      await waitFor(() => {
        // 找到Usage行
        const usageRow = screen.getByText('快充场景').closest('tr')
        
        // 应该显示关联的Definition信息
        expect(usageRow).toHaveTextContent('→ REQ-001')
      }, { timeout: 3000 })
    })

    test('应该在专门的列显示requirementDefinition字段', async () => {
      render(
        <ModelProvider>
          <TableView showRelation />
        </ModelProvider>
      )

      // 等待数据加载完成
      await waitFor(() => {
        expect(requirementService.getAllRequirements).toHaveBeenCalled()
      })

      await waitFor(() => {
        // 应该有"关联定义"列
        expect(screen.getByText('关联定义')).toBeInTheDocument()
      })

      await waitFor(() => {
        // Usage行应该显示Definition ID
        const usageRow = screen.getByText('标准电池包').closest('tr')
        expect(usageRow).toHaveTextContent('req-def-002')
      }, { timeout: 3000 })
    })
  })

  describe('字段标准化验证', () => {
    test('不应该显示旧的of字段', async () => {
      render(
        <ModelProvider>
          <TableView />
        </ModelProvider>
      )

      await waitFor(() => {
        // 确保表头不包含of字段
        expect(screen.queryByText('of')).not.toBeInTheDocument()
        expect(screen.queryByText('OF')).not.toBeInTheDocument()
      })
    })

    test('不应该显示已删除的subject字段', async () => {
      render(
        <ModelProvider>
          <TableView />
        </ModelProvider>
      )

      await waitFor(() => {
        // 确保不显示subject相关内容
        expect(screen.queryByText(/subject/i)).not.toBeInTheDocument()
        expect(screen.queryByText('约束对象')).not.toBeInTheDocument()
      })
    })

    test('应该使用documentation替代text字段', async () => {
      render(
        <ModelProvider>
          <TableView />
        </ModelProvider>
      )

      await waitFor(() => {
        // 应该显示"文档"而不是"文本"
        expect(screen.getByText('文档')).toBeInTheDocument()
        expect(screen.queryByText('文本')).not.toBeInTheDocument()
        expect(screen.queryByText('text')).not.toBeInTheDocument()
      })
    })
  })

  describe('排序功能', () => {
    test('应该支持按需求ID排序', async () => {
      render(
        <ModelProvider>
          <TableView sortable />
        </ModelProvider>
      )

      await waitFor(() => {
        // 点击需求ID列头排序
        const reqIdHeader = screen.getByText('需求ID')
        fireEvent.click(reqIdHeader)
      })

      // 验证排序图标出现
      expect(screen.getByLabelText('caret-up')).toBeInTheDocument()
    })

    test('应该支持按优先级排序', async () => {
      render(
        <ModelProvider>
          <TableView sortable />
        </ModelProvider>
      )

      await waitFor(() => {
        // 点击优先级列头排序
        const priorityHeader = screen.getByText('优先级')
        fireEvent.click(priorityHeader)
      })

      // 验证数据按优先级排序（P0应该在前）
      const rows = screen.getAllByRole('row')
      expect(rows[1]).toHaveTextContent('P0') // 第一行数据
    })
  })

  describe('过滤功能', () => {
    test('应该支持按状态过滤', async () => {
      render(
        <ModelProvider>
          <TableView filterable />
        </ModelProvider>
      )

      await waitFor(() => {
        // 打开状态过滤下拉
        const statusFilter = screen.getByLabelText('filter')
        fireEvent.click(statusFilter)
      })

      // 选择approved状态
      const approvedOption = screen.getByText('approved')
      fireEvent.click(approvedOption)

      await waitFor(() => {
        // 只显示approved状态的数据
        expect(screen.getByText('REQ-001')).toBeInTheDocument()
        expect(screen.queryByText('快充场景')).not.toBeInTheDocument() // implemented状态
      })
    })

    test('应该支持按类型过滤', async () => {
      render(
        <ModelProvider>
          <TableView filterable />
        </ModelProvider>
      )

      await waitFor(() => {
        // 选择只显示RequirementUsage
        const typeFilter = screen.getByLabelText('类型过滤')
        fireEvent.change(typeFilter, { target: { value: 'RequirementUsage' } })
      })

      // 只显示Usage数据
      expect(screen.getByText('快充场景')).toBeInTheDocument()
      expect(screen.getByText('标准电池包')).toBeInTheDocument()
      expect(screen.queryByText('REQ-001')).not.toBeInTheDocument()
    })
  })

  describe('分页功能', () => {
    test('应该显示分页控件', async () => {
      render(
        <ModelProvider>
          <TableView pageable pageSize={2} />
        </ModelProvider>
      )

      await waitFor(() => {
        // 验证分页控件存在
        expect(screen.getByLabelText('pagination')).toBeInTheDocument()
        expect(screen.getByText('共 4 条')).toBeInTheDocument()
      })
    })

    test('应该支持切换页面', async () => {
      // Mock分页数据
      ;(requirementService.getAllRequirements as jest.Mock)
        .mockResolvedValueOnce({
          content: mockRequirementDefinitions,
          totalElements: 4,
          totalPages: 2,
          number: 0
        })
        .mockResolvedValueOnce({
          content: mockRequirementUsages,
          totalElements: 4,
          totalPages: 2,
          number: 1
        })

      render(
        <ModelProvider>
          <TableView pageable pageSize={2} />
        </ModelProvider>
      )

      await waitFor(() => {
        // 切换到第2页
        const page2Button = screen.getByTitle('2')
        fireEvent.click(page2Button)
      })

      // 验证API被调用
      expect(requirementService.getAllRequirements).toHaveBeenCalledTimes(2)
      expect(requirementService.getAllRequirements).toHaveBeenLastCalledWith(1, 2)
    })
  })
})