/**
 * 树表合并视图测试
 * 
 * 测试要求：
 * - 左边是双树（上Usage下Definition）
 * - 右边是表格（只显示Usage）
 * - 布局正确性验证
 * - 数据过滤验证
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, waitFor } from '@testing-library/react'
import { ModelProvider } from '../../contexts/ModelContext'
import ModelViewerClean from '../ModelViewerClean'

// Mock data
const mockElements = new Map([
  ['def-1', {
    id: 'def-1',
    elementId: 'def-1',
    eClass: 'RequirementDefinition',
    reqId: 'REQ-DEF-001',
    declaredName: 'Performance Definition',
    documentation: 'System performance requirements'
  }],
  ['def-2', {
    id: 'def-2',
    elementId: 'def-2',
    eClass: 'RequirementDefinition',
    reqId: 'REQ-DEF-002',
    declaredName: 'Safety Definition',
    documentation: 'Safety requirements'
  }],
  ['usage-1', {
    id: 'usage-1',
    elementId: 'usage-1',
    eClass: 'RequirementUsage',
    reqId: 'REQ-USE-001',
    declaredName: 'Battery Performance',
    documentation: 'Battery system performance',
    requirementDefinition: 'def-1'
  }],
  ['usage-2', {
    id: 'usage-2',
    elementId: 'usage-2',
    eClass: 'RequirementUsage',
    reqId: 'REQ-USE-002',
    declaredName: 'Battery Safety',
    documentation: 'Battery safety requirements',
    requirementDefinition: 'def-2'
  }],
  ['usage-3', {
    id: 'usage-3',
    elementId: 'usage-3',
    eClass: 'RequirementUsage',
    reqId: 'REQ-USE-003',
    declaredName: 'Charging Safety',
    documentation: 'Charging safety requirements',
    requirementDefinition: 'def-2'
  }]
])

// Mock requirementService
vi.mock('../../services/requirementService', () => ({
  requirementService: {
    getRequirements: vi.fn().mockResolvedValue({
      data: Array.from(mockElements.values()),
      totalElements: 5,
      totalPages: 1,
      page: 0,
      size: 20
    }),
    createRequirementDefinition: vi.fn(),
    createRequirementUsage: vi.fn(),
    updateRequirement: vi.fn(),
    deleteRequirement: vi.fn()
  }
}))

describe('TreeTableView - 树表合并视图', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('应该渲染左右分栏布局', async () => {
    const { container } = render(
      <ModelProvider>
        <ModelViewerClean />
      </ModelProvider>
    )

    // 等待数据加载
    await waitFor(() => {
      // 点击树视图标签
      const treeTab = screen.getByText('🌳 树视图')
      expect(treeTab).toBeInTheDocument()
      treeTab.click()
    })

    await waitFor(() => {
      // 检查是否有左右分栏容器
      const splitContainer = container.querySelector('[style*="display: flex"]')
      expect(splitContainer).toBeInTheDocument()
      
      // 应该有两个主要区域
      const mainAreas = splitContainer?.children
      expect(mainAreas?.length).toBeGreaterThanOrEqual(2)
    })
  })

  it('左侧应该有两个树区域', async () => {
    render(
      <ModelProvider>
        <ModelViewerClean />
      </ModelProvider>
    )

    // 切换到树视图
    const treeTab = screen.getByText('🌳 树视图')
    treeTab.click()

    await waitFor(() => {
      // 应该有两个树标题
      expect(screen.getByText('需求使用')).toBeInTheDocument()
      expect(screen.getByText('需求定义')).toBeInTheDocument()
    })
  })

  it('上部树应该只显示RequirementUsage', async () => {
    render(
      <ModelProvider>
        <ModelViewerClean />
      </ModelProvider>
    )

    // 切换到树视图
    const treeTab = screen.getByText('🌳 树视图')
    treeTab.click()

    await waitFor(() => {
      // 上部树（Usage）区域
      const usageSection = screen.getByText('需求使用').closest('div')
      
      // 应该显示Usage数据
      expect(usageSection).toHaveTextContent('Battery Performance')
      expect(usageSection).toHaveTextContent('Battery Safety')
      expect(usageSection).toHaveTextContent('Charging Safety')
      
      // 不应该显示Definition数据
      expect(usageSection).not.toHaveTextContent('Performance Definition')
      expect(usageSection).not.toHaveTextContent('Safety Definition')
    })
  })

  it('下部树应该只显示RequirementDefinition', async () => {
    render(
      <ModelProvider>
        <ModelViewerClean />
      </ModelProvider>
    )

    // 切换到树视图
    const treeTab = screen.getByText('🌳 树视图')
    treeTab.click()

    await waitFor(() => {
      // 下部树（Definition）区域
      const defSection = screen.getByText('需求定义').closest('div')
      
      // 应该显示Definition数据
      expect(defSection).toHaveTextContent('Performance Definition')
      expect(defSection).toHaveTextContent('Safety Definition')
      
      // 不应该显示Usage数据
      expect(defSection).not.toHaveTextContent('Battery Performance')
      expect(defSection).not.toHaveTextContent('Battery Safety')
    })
  })

  it('右侧表格应该只显示RequirementUsage', async () => {
    render(
      <ModelProvider>
        <ModelViewerClean />
      </ModelProvider>
    )

    // 切换到树视图（包含表格的合并视图）
    const treeTab = screen.getByText('🌳 树视图')
    treeTab.click()

    await waitFor(() => {
      // 表格标题应该明确说明是Usage
      expect(screen.getByText(/需求条目列表|RequirementUsage/)).toBeInTheDocument()
      
      // 表格应该显示Usage数据
      expect(screen.getByText('REQ-USE-001')).toBeInTheDocument()
      expect(screen.getByText('REQ-USE-002')).toBeInTheDocument()
      expect(screen.getByText('REQ-USE-003')).toBeInTheDocument()
      
      // 表格不应该显示Definition数据
      expect(screen.queryByText('REQ-DEF-001')).not.toBeInTheDocument()
      expect(screen.queryByText('REQ-DEF-002')).not.toBeInTheDocument()
    })
  })

  it('左侧树应该有独立的搜索功能', async () => {
    render(
      <ModelProvider>
        <ModelViewerClean />
      </ModelProvider>
    )

    // 切换到树视图
    const treeTab = screen.getByText('🌳 树视图')
    treeTab.click()

    await waitFor(() => {
      // 应该有两个搜索框
      const searchInputs = screen.getAllByPlaceholderText(/搜索/)
      expect(searchInputs.length).toBeGreaterThanOrEqual(2)
      
      // 一个是搜索Usage
      expect(screen.getByPlaceholderText('搜索需求使用...')).toBeInTheDocument()
      
      // 一个是搜索Definition
      expect(screen.getByPlaceholderText('搜索需求定义...')).toBeInTheDocument()
    })
  })

  it('右侧表格应该有分页功能', async () => {
    render(
      <ModelProvider>
        <ModelViewerClean />
      </ModelProvider>
    )

    // 切换到树视图
    const treeTab = screen.getByText('🌳 树视图')
    treeTab.click()

    await waitFor(() => {
      // 检查分页组件
      const pagination = screen.getByRole('navigation')
      expect(pagination).toBeInTheDocument()
      
      // 检查记录统计
      expect(screen.getByText(/共 \d+ 条记录/)).toBeInTheDocument()
    })
  })

  it('表格应该有工具栏', async () => {
    render(
      <ModelProvider>
        <ModelViewerClean />
      </ModelProvider>
    )

    // 切换到树视图
    const treeTab = screen.getByText('🌳 树视图')
    treeTab.click()

    await waitFor(() => {
      // 检查工具栏
      const toolbar = screen.getByRole('toolbar')
      expect(toolbar).toBeInTheDocument()
      
      // 检查编辑按钮
      expect(screen.getByRole('button', { name: /编辑/ })).toBeInTheDocument()
      
      // 检查删除按钮
      expect(screen.getByRole('button', { name: /删除/ })).toBeInTheDocument()
      
      // 检查刷新按钮
      expect(screen.getByRole('button', { name: /刷新/ })).toBeInTheDocument()
    })
  })

  it('布局比例应该合理', async () => {
    const { container } = render(
      <ModelProvider>
        <ModelViewerClean />
      </ModelProvider>
    )

    // 切换到树视图
    const treeTab = screen.getByText('🌳 树视图')
    treeTab.click()

    await waitFor(() => {
      // 左侧树区域应该有固定宽度
      const leftPanel = container.querySelector('[style*="width: 350px"]')
      expect(leftPanel).toBeInTheDocument()
      
      // 右侧表格应该占据剩余空间
      const rightPanel = container.querySelector('[style*="flex: 1"]')
      expect(rightPanel).toBeInTheDocument()
    })
  })
})