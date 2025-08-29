/**
 * REQ-UI-2: 分离Definition和Usage显示测试
 * 
 * 测试点：
 * - 过滤Definition数据
 * - 只显示Usage数据
 * - 显示关联Definition
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
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

describe('TableUsageFilter', () => {
  const mockData = [
    {
      id: 'def-1',
      elementId: 'def-1',
      eClass: 'RequirementDefinition',
      reqId: 'REQ-DEF-001',
      declaredName: 'Performance Definition',
      documentation: 'System performance requirements'
    },
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
      id: 'def-2',
      elementId: 'def-2',
      eClass: 'RequirementDefinition',
      reqId: 'REQ-DEF-002',
      declaredName: 'Safety Definition',
      documentation: 'Safety requirements'
    },
    {
      id: 'usage-2',
      elementId: 'usage-2',
      eClass: 'RequirementUsage',
      reqId: 'REQ-USE-002',
      declaredName: 'Battery Safety',
      documentation: 'Battery safety requirements',
      requirementDefinition: 'def-2'
    },
    {
      id: 'usage-3',
      elementId: 'usage-3',
      eClass: 'RequirementUsage',
      reqId: 'REQ-USE-003',
      declaredName: 'Charging Safety',
      documentation: 'Charging safety requirements',
      requirementDefinition: 'def-2'
    }
  ]

  beforeEach(() => {
    vi.clearAllMocks()
    ;(requirementService.getRequirements as any).mockResolvedValue({
      data: mockData,
      totalElements: 5,
      totalPages: 1,
      page: 0,
      size: 20
    })
  })

  it('应该只显示RequirementUsage数据', async () => {
    render(
      <ModelProvider>
        <TableView usageOnly={true} />
      </ModelProvider>
    )

    await waitFor(() => {
      // 应该显示所有Usage
      expect(screen.getByText('Battery Performance')).toBeInTheDocument()
      expect(screen.getByText('Battery Safety')).toBeInTheDocument()
      expect(screen.getByText('Charging Safety')).toBeInTheDocument()
      
      // 不应该显示Definition
      expect(screen.queryByText('Performance Definition')).not.toBeInTheDocument()
      expect(screen.queryByText('Safety Definition')).not.toBeInTheDocument()
    })
  })

  it('应该显示Usage关联的Definition名称', async () => {
    render(
      <ModelProvider>
        <TableView usageOnly={true} showRelation={true} />
      </ModelProvider>
    )

    await waitFor(() => {
      // 检查是否有关联定义列
      expect(screen.getByText('关联定义')).toBeInTheDocument()
      
      // 检查Usage的关联Definition显示
      // 注意：这里需要根据实际实现调整，可能需要显示Definition的名称而不是ID
      const rows = screen.getAllByRole('row')
      expect(rows.length).toBeGreaterThan(3) // 至少有3个Usage行
    })
  })

  it('应该正确统计Usage数量', async () => {
    render(
      <ModelProvider>
        <TableView usageOnly={true} />
      </ModelProvider>
    )

    await waitFor(() => {
      // 应该只统计Usage的数量（3个）
      expect(screen.getByText(/共 3 条记录/)).toBeInTheDocument()
    })
  })

  it('应该在表格标题中明确显示是需求使用列表', async () => {
    render(
      <ModelProvider>
        <TableView usageOnly={true} />
      </ModelProvider>
    )

    await waitFor(() => {
      // 标题应该明确表示这是需求使用（Usage）列表
      expect(screen.getByText(/需求使用列表|需求条目|RequirementUsage/i)).toBeInTheDocument()
    })
  })

  it('应该正确处理没有关联Definition的Usage', async () => {
    const dataWithOrphanUsage = [
      ...mockData,
      {
        id: 'usage-orphan',
        elementId: 'usage-orphan',
        eClass: 'RequirementUsage',
        reqId: 'REQ-USE-ORPHAN',
        declaredName: 'Orphan Usage',
        documentation: 'Usage without definition',
        requirementDefinition: null
      }
    ]

    ;(requirementService.getRequirements as any).mockResolvedValue({
      data: dataWithOrphanUsage,
      totalElements: 6,
      totalPages: 1,
      page: 0,
      size: 20
    })

    render(
      <ModelProvider>
        <TableView usageOnly={true} showRelation={true} />
      </ModelProvider>
    )

    await waitFor(() => {
      // 应该显示孤立的Usage
      expect(screen.getByText('Orphan Usage')).toBeInTheDocument()
      // 关联定义应该显示为空或"-"
      const orphanRow = screen.getByText('Orphan Usage').closest('tr')
      expect(orphanRow).toHaveTextContent('-')
    })
  })
})