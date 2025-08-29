/**
 * TreeView数据加载测试
 * REQ-F2-1: 树视图数据加载修复
 * 
 * 验收标准：
 * - 树视图显示RequirementDefinition和RequirementUsage
 * - Usage节点显示关联的Definition（通过requirementDefinition字段）
 * - 按类型分组显示
 * - 显示正确的字段名称
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
import TreeView from '../TreeView'
import { requirementService } from '../../../services/requirementService'

describe('TreeView 真实数据加载测试', () => {
  beforeEach(() => {
    // 重置所有mock
    vi.clearAllMocks()
  })

  describe('数据加载功能', () => {
    test('应该从requirementService加载真实数据', async () => {
      render(
        <ModelProvider>
          <TreeView />
        </ModelProvider>
      )

      // 等待数据加载
      await waitFor(() => {
        expect(requirementService.getRequirementDefinitions).toHaveBeenCalled()
        expect(requirementService.getRequirementUsages).toHaveBeenCalled()
      })

      // 验证API调用参数
      expect(requirementService.getRequirementDefinitions).toHaveBeenCalledWith(0, 100)
      expect(requirementService.getRequirementUsages).toHaveBeenCalledWith(0, 100)
    })

    test('应该显示加载状态', () => {
      render(
        <ModelProvider>
          <TreeView />
        </ModelProvider>
      )

      // 初始应该显示加载状态 - Spin组件的tip属性
      const loadingElement = document.querySelector('.ant-spin')
      expect(loadingElement).toBeInTheDocument()
    })

    test('应该在加载失败时显示错误信息', async () => {
      // 模拟加载失败
      vi.mocked(requirementService.getRequirementDefinitions).mockRejectedValueOnce(
        new Error('网络错误')
      )

      render(
        <ModelProvider>
          <TreeView />
        </ModelProvider>
      )

      await waitFor(() => {
        expect(screen.getAllByText(/加载失败/i)[0]).toBeInTheDocument()
      })
    })
  })

  describe('树形结构显示', () => {
    test('应该按类型分组显示节点', async () => {
      render(
        <ModelProvider>
          <TreeView />
        </ModelProvider>
      )

      await waitFor(() => {
        // 应该有RequirementDefinition分组
        expect(screen.getByText('需求定义 (2)')).toBeInTheDocument()
        // 应该有RequirementUsage分组  
        expect(screen.getByText('需求使用 (3)')).toBeInTheDocument()
      })
    })

    test('应该显示所有RequirementDefinition节点', async () => {
      render(
        <ModelProvider>
          <TreeView />
        </ModelProvider>
      )

      await waitFor(() => {
        // 验证所有Definition节点显示
        expect(screen.getByText('充电时间需求')).toBeInTheDocument()
        expect(screen.getByText('电池容量需求')).toBeInTheDocument()
      })
    })

    test('应该显示所有RequirementUsage节点', async () => {
      render(
        <ModelProvider>
          <TreeView />
        </ModelProvider>
      )

      await waitFor(() => {
        // 验证所有Usage节点显示
        expect(screen.getByText('快充场景')).toBeInTheDocument()
        expect(screen.getByText('慢充场景')).toBeInTheDocument()
        expect(screen.getByText('标准电池包')).toBeInTheDocument()
      })
    })
  })

  describe('Usage-Definition关联显示', () => {
    test('Usage节点应该显示关联的Definition信息', async () => {
      render(
        <ModelProvider>
          <TreeView />
        </ModelProvider>
      )

      await waitFor(() => {
        // 找到Usage节点
        const usageNode = screen.getByText('快充场景')
        
        // 应该显示关联的Definition信息 - 实际显示在同一节点中
        const parentNode = usageNode.closest('.ant-tree-treenode')
        expect(parentNode).toBeTruthy()
        // 验证关联的Definition名称显示
        expect(screen.getByText('充电时间需求')).toBeInTheDocument()
      })
    })

    test('应该正确处理requirementDefinition字段', async () => {
      render(
        <ModelProvider>
          <TreeView />
        </ModelProvider>
      )

      await waitFor(() => {
        // 验证慢充场景和标准电池包都显示
        expect(screen.getByText('慢充场景')).toBeInTheDocument()
        expect(screen.getByText('标准电池包')).toBeInTheDocument()
        
        // 验证关联的Definition也在树中显示
        expect(screen.getByText('充电时间需求')).toBeInTheDocument()
        expect(screen.getByText('电池容量需求')).toBeInTheDocument()
      })
    })
  })

  describe('字段标准化验证', () => {
    test('不应该显示旧的of字段', async () => {
      // 创建包含of字段的错误数据
      const wrongData = [{
        elementId: 'req-usage-001',
        eClass: 'RequirementUsage',
        of: 'req-def-001', // 旧字段
        requirementDefinition: undefined,
        declaredName: '快充场景',
        documentation: '在快充模式下的充电时间需求',
        status: 'implemented',
        priority: 'P0'
      }]
      
      vi.mocked(requirementService.getRequirementUsages).mockResolvedValueOnce({
        content: wrongData,
        totalElements: 1,
        totalPages: 1,
        number: 0
      })

      render(
        <ModelProvider>
          <TreeView />
        </ModelProvider>
      )

      await waitFor(() => {
        // 应该显示快充场景但没有关联
        const usageNode = screen.getByText('快充场景')
        expect(usageNode).toBeInTheDocument()
        
        // 由于requirementDefinition未定义，不应该显示Definition名称作为子节点
        const parentNode = usageNode.closest('.ant-tree-treenode')
        // 检查是否为叶子节点（没有子节点）
        expect(parentNode?.classList.contains('ant-tree-treenode-leaf')).toBe(true)
      })
    })

    test('不应该显示已删除的subject字段', async () => {
      render(
        <ModelProvider>
          <TreeView />
        </ModelProvider>
      )

      await waitFor(() => {
        // 确保不显示subject相关内容
        expect(screen.queryByText(/subject/i)).not.toBeInTheDocument()
        expect(screen.queryByText(/约束对象/i)).not.toBeInTheDocument()
      })
    })
  })

  describe('节点操作', () => {
    test('点击节点应该触发选择事件', async () => {
      const onSelect = vi.fn()
      
      render(
        <ModelProvider>
          <TreeView onSelect={onSelect} />
        </ModelProvider>
      )

      await waitFor(() => {
        screen.getByText('充电时间需求')
      })
      
      fireEvent.click(screen.getByText('充电时间需求'))

      // Ant Design Tree的onSelect回调格式是(keys, info)
      expect(onSelect).toHaveBeenCalled()
      const callArgs = onSelect.mock.calls[0]
      expect(callArgs[0]).toContain('req-def-001') // 第一个参数是选中的keys数组
    })

    test('展开/折叠应该正常工作', async () => {
      render(
        <ModelProvider>
          <TreeView />
        </ModelProvider>
      )

      await waitFor(() => {
        screen.getByText('需求定义 (2)')
      })

      const definitionGroup = screen.getByText('需求定义 (2)').closest('.ant-tree-treenode')
      
      // Ant Design Tree使用ant-tree-treenode-switcher-open/close类
      expect(definitionGroup).toHaveClass('ant-tree-treenode-switcher-open')
      
      // 找到展开/折叠图标
      const expandIcon = definitionGroup?.querySelector('.ant-tree-switcher')
      
      // 点击折叠
      if (expandIcon) {
        fireEvent.click(expandIcon)
        // 折叠后应该有ant-tree-treenode-switcher-close类
        await waitFor(() => {
          expect(definitionGroup).toHaveClass('ant-tree-treenode-switcher-close')
        })
      }
    })
  })
})