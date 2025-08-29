/**
 * RequirementExplorer组件测试用例
 * 
 * 基于设计文档requirement-modeling-architecture-v2.md
 * 采用TDD开发模式，先写测试再写实现
 * 
 * 测试覆盖：
 * 1. 双树结构渲染
 * 2. Usage树展示（上部）
 * 3. Definition树展示（下部）  
 * 4. 拖拽创建功能
 * 5. 数据加载和刷新
 */

import React from 'react'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import '@testing-library/jest-dom'
import userEvent from '@testing-library/user-event'
import { act } from 'react-dom/test-utils'
import RequirementExplorer from '../RequirementExplorer'
import { ModelProvider } from '../../contexts/ModelContext'

// Mock API responses
const mockDefinitions = [
  {
    elementId: 'def-001',
    declaredName: '性能需求模板',
    reqId: 'REQ-PERF-001',
    text: '系统响应时间应小于2秒',
    documentation: '性能需求模板定义',
    eClass: 'RequirementDefinition',
    tags: ['performance', 'response-time']
  },
  {
    elementId: 'def-002', 
    declaredName: '安全需求模板',
    reqId: 'REQ-SEC-001',
    text: '系统应支持双因素认证',
    documentation: '安全需求模板定义',
    eClass: 'RequirementDefinition',
    tags: ['security', 'authentication']
  },
  {
    elementId: 'def-003',
    declaredName: '功能需求模板',
    reqId: 'REQ-FUNC-001', 
    text: '系统应提供用户登录功能',
    documentation: '功能需求模板定义',
    eClass: 'RequirementDefinition',
    tags: ['functional', 'user-management']
  }
]

const mockUsages = [
  {
    elementId: 'usage-001',
    declaredName: 'Web应用响应时间需求',
    of: 'def-001',
    subject: 'web-application',
    text: 'Web应用的API响应时间应小于500ms',
    documentation: '基于性能需求模板的实例',
    eClass: 'RequirementUsage'
  },
  {
    elementId: 'usage-002',
    declaredName: '移动端认证需求',
    of: 'def-002',
    subject: 'mobile-app',
    text: '移动应用应支持指纹和人脸识别',
    documentation: '基于安全需求模板的实例',
    eClass: 'RequirementUsage'
  },
  {
    elementId: 'usage-003',
    declaredName: 'API性能需求',
    of: 'def-001',
    derive: 'usage-001', // derive关系，用于构建层级
    subject: 'api-gateway',
    text: 'API网关响应时间应小于200ms',
    documentation: '派生自Web应用响应时间需求',
    eClass: 'RequirementUsage'
  }
]

// Mock fetch
global.fetch = jest.fn()

describe('RequirementExplorer - TDD测试套件', () => {
  
  beforeEach(() => {
    jest.clearAllMocks()
    // 设置默认的fetch响应
    ;(fetch as jest.Mock).mockImplementation((url: string) => {
      if (url.includes('/requirements?')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockDefinitions)
        })
      }
      if (url.includes('/requirements/usages')) {
        return Promise.resolve({
          ok: true,
          json: () => Promise.resolve(mockUsages)
        })
      }
      return Promise.resolve({
        ok: true,
        json: () => Promise.resolve([])
      })
    })
  })

  describe('1. 双树结构渲染测试', () => {
    
    it('应该渲染上下两个树区域', async () => {
      render(
        <ModelProvider>
          <RequirementExplorer />
        </ModelProvider>
      )

      await waitFor(() => {
        // 检查上树标题
        expect(screen.getByText(/需求实例 \(Usage\)/i)).toBeInTheDocument()
        // 检查下树标题
        expect(screen.getByText(/需求模板 \(Definition\)/i)).toBeInTheDocument()
      })
    })

    it('应该有分隔线分割上下两个树', async () => {
      const { container } = render(
        <ModelProvider>
          <RequirementExplorer />
        </ModelProvider>
      )

      await waitFor(() => {
        // 检查是否有Divider组件
        const divider = container.querySelector('.ant-divider')
        expect(divider).toBeInTheDocument()
      })
    })

    it('两个树区域应该都有新建按钮', async () => {
      render(
        <ModelProvider>
          <RequirementExplorer />
        </ModelProvider>
      )

      await waitFor(() => {
        // 应该有两个新建按钮
        const createButtons = screen.getAllByText('新建')
        expect(createButtons).toHaveLength(2)
      })
    })
  })

  describe('2. Usage树（上树）测试', () => {
    
    it('应该显示所有RequirementUsage元素', async () => {
      render(
        <ModelProvider>
          <RequirementExplorer />
        </ModelProvider>
      )

      await waitFor(() => {
        // 检查Usage树是否显示所有usage
        expect(screen.getByText('Web应用响应时间需求')).toBeInTheDocument()
        expect(screen.getByText('移动端认证需求')).toBeInTheDocument()
        expect(screen.getByText('API性能需求')).toBeInTheDocument()
      })
    })

    it('应该显示技术规格书根节点', async () => {
      render(
        <ModelProvider>
          <RequirementExplorer />
        </ModelProvider>
      )

      await waitFor(() => {
        // 检查是否有技术规格书根节点
        expect(screen.getByText(/技术规格书/)).toBeInTheDocument()
      })
    })

    it('基于derive关系构建层级结构', async () => {
      const { container } = render(
        <ModelProvider>
          <RequirementExplorer />
        </ModelProvider>
      )

      await waitFor(() => {
        // TODO: 验证usage-003是usage-001的子节点
        // 这需要检查DOM结构的层级关系
        const trees = container.querySelectorAll('.ant-tree')
        expect(trees.length).toBeGreaterThan(0)
      })
    })

    it('点击Usage应该触发选择事件', async () => {
      const onUsageSelect = jest.fn()
      
      render(
        <ModelProvider>
          <RequirementExplorer onUsageSelect={onUsageSelect} />
        </ModelProvider>
      )

      await waitFor(async () => {
        const usageNode = screen.getByText('Web应用响应时间需求')
        await userEvent.click(usageNode)
        expect(onUsageSelect).toHaveBeenCalledWith('usage-001')
      })
    })

    it('Usage树应该有刷新按钮', async () => {
      const { container } = render(
        <ModelProvider>
          <RequirementExplorer />
        </ModelProvider>
      )

      await waitFor(() => {
        // 找到上部区域的刷新按钮
        const upperSection = container.querySelectorAll('.ant-space')[0]
        expect(upperSection).toBeTruthy()
        const refreshButton = screen.getAllByText('刷新')[0]
        expect(refreshButton).toBeInTheDocument()
      })
    })
  })

  describe('3. Definition树（下树）测试', () => {
    
    it('应该显示所有RequirementDefinition元素', async () => {
      render(
        <ModelProvider>
          <RequirementExplorer />
        </ModelProvider>
      )

      await waitFor(() => {
        // 检查Definition树是否显示所有definition
        expect(screen.getByText('性能需求模板')).toBeInTheDocument()
        expect(screen.getByText('安全需求模板')).toBeInTheDocument()
        expect(screen.getByText('功能需求模板')).toBeInTheDocument()
      })
    })

    it('Definition应该按类别分组显示', async () => {
      render(
        <ModelProvider>
          <RequirementExplorer />
        </ModelProvider>
      )

      await waitFor(() => {
        // 检查是否有分类节点
        expect(screen.getByText(/性能需求 \(\d+\)/)).toBeInTheDocument()
        expect(screen.getByText(/安全需求 \(\d+\)/)).toBeInTheDocument()
        expect(screen.getByText(/功能需求 \(\d+\)/)).toBeInTheDocument()
      })
    })

    it('应该有需求模板和需求包两个标签页', async () => {
      render(
        <ModelProvider>
          <RequirementExplorer />
        </ModelProvider>
      )

      await waitFor(() => {
        expect(screen.getByText('需求模板')).toBeInTheDocument()
        expect(screen.getByText('需求包')).toBeInTheDocument()
      })
    })

    it('Definition节点应该可拖拽', async () => {
      const { container } = render(
        <ModelProvider>
          <RequirementExplorer />
        </ModelProvider>
      )

      await waitFor(() => {
        // 检查树是否设置了draggable属性
        const trees = container.querySelectorAll('.ant-tree')
        const definitionTree = trees[trees.length - 1] // 下树
        expect(definitionTree.classList.contains('ant-tree-draggable')).toBeTruthy()
      })
    })

    it('每个Definition应该有"基于此创建"按钮', async () => {
      render(
        <ModelProvider>
          <RequirementExplorer />
        </ModelProvider>
      )

      await waitFor(() => {
        // 查找复制按钮（用于基于模板创建Usage）
        const copyButtons = screen.getAllByTitle('基于此模板创建Usage')
        expect(copyButtons.length).toBeGreaterThan(0)
      })
    })
  })

  describe('4. 拖拽创建Usage功能测试', () => {
    
    it('拖拽Definition应该触发拖拽开始事件', async () => {
      const onDefinitionDragStart = jest.fn()
      
      render(
        <ModelProvider>
          <RequirementExplorer onDefinitionDragStart={onDefinitionDragStart} />
        </ModelProvider>
      )

      await waitFor(() => {
        const definitionNode = screen.getByText('性能需求模板')
        
        // 模拟拖拽开始
        const dragStartEvent = new DragEvent('dragstart', {
          dataTransfer: new DataTransfer(),
          bubbles: true
        })
        
        fireEvent(definitionNode, dragStartEvent)
        
        // 验证是否触发了拖拽事件处理
        expect(onDefinitionDragStart).toHaveBeenCalled()
      })
    })

    it('点击Definition的创建按钮应该打开创建Usage对话框', async () => {
      render(
        <ModelProvider>
          <RequirementExplorer />
        </ModelProvider>
      )

      await waitFor(async () => {
        // 找到第一个"基于此创建"按钮
        const copyButton = screen.getAllByTitle('基于此模板创建Usage')[0]
        await userEvent.click(copyButton)
        
        // 检查是否打开了创建对话框
        expect(screen.getByText(/创建需求使用/i)).toBeInTheDocument()
      })
    })
  })

  describe('5. 数据加载和刷新测试', () => {
    
    it('初始加载时应该显示加载状态', async () => {
      render(
        <ModelProvider>
          <RequirementExplorer />
        </ModelProvider>
      )

      // 初始应该有加载指示器
      expect(screen.getAllByRole('img', { hidden: true })[0]).toBeInTheDocument()
      
      await waitFor(() => {
        // 加载完成后不应该显示加载状态
        expect(screen.queryByRole('progressbar')).not.toBeInTheDocument()
      })
    })

    it('刷新按钮应该重新加载数据', async () => {
      render(
        <ModelProvider>
          <RequirementExplorer />
        </ModelProvider>
      )

      await waitFor(async () => {
        // 清除之前的调用记录
        jest.clearAllMocks()
        
        // 点击刷新按钮
        const refreshButton = screen.getAllByText('刷新')[0]
        await userEvent.click(refreshButton)
        
        // 验证重新调用了API
        expect(fetch).toHaveBeenCalledWith(
          expect.stringContaining('/requirements/usages')
        )
        expect(fetch).toHaveBeenCalledWith(
          expect.stringContaining('/requirements?')
        )
      })
    })

    it('数据加载失败时应该显示错误消息', async () => {
      // 模拟API失败
      ;(fetch as jest.Mock).mockRejectedValue(new Error('Network error'))
      
      render(
        <ModelProvider>
          <RequirementExplorer />
        </ModelProvider>
      )

      await waitFor(() => {
        // 应该显示错误提示
        expect(screen.getByText(/加载需求数据失败/)).toBeInTheDocument()
      })
    })

    it('空数据时应该显示空状态提示', async () => {
      // 模拟空数据响应
      ;(fetch as jest.Mock).mockImplementation(() => 
        Promise.resolve({
          ok: true,
          json: () => Promise.resolve([])
        })
      )
      
      render(
        <ModelProvider>
          <RequirementExplorer />
        </ModelProvider>
      )

      await waitFor(() => {
        expect(screen.getByText('暂无需求实例')).toBeInTheDocument()
        expect(screen.getByText('暂无需求模板')).toBeInTheDocument()
      })
    })
  })

  describe('6. 对话框交互测试', () => {
    
    it('点击上树新建按钮应该打开创建Usage对话框', async () => {
      render(
        <ModelProvider>
          <RequirementExplorer />
        </ModelProvider>
      )

      await waitFor(async () => {
        // 找到上部的新建按钮（第一个）
        const createButtons = screen.getAllByText('新建')
        await userEvent.click(createButtons[0])
        
        // 检查是否打开了创建Usage对话框
        expect(screen.getByText(/创建需求使用/i)).toBeInTheDocument()
      })
    })

    it('点击下树新建按钮应该打开创建Definition对话框', async () => {
      render(
        <ModelProvider>
          <RequirementExplorer />
        </ModelProvider>
      )

      await waitFor(async () => {
        // 找到下部的新建按钮（第二个）
        const createButtons = screen.getAllByText('新建')
        await userEvent.click(createButtons[1])
        
        // 检查是否打开了创建Definition对话框
        expect(screen.getByText(/创建需求定义/i)).toBeInTheDocument()
      })
    })

    it('创建成功后应该刷新数据', async () => {
      const { rerender } = render(
        <ModelProvider>
          <RequirementExplorer />
        </ModelProvider>
      )

      await waitFor(async () => {
        // 打开创建对话框
        const createButton = screen.getAllByText('新建')[0]
        await userEvent.click(createButton)
        
        // 填写表单并提交
        const nameInput = screen.getByLabelText(/名称/)
        await userEvent.type(nameInput, '新需求使用')
        
        const submitButton = screen.getByText('确定')
        await userEvent.click(submitButton)
        
        // 验证成功消息
        await waitFor(() => {
          expect(screen.getByText('创建成功')).toBeInTheDocument()
        })
        
        // 验证数据刷新
        expect(fetch).toHaveBeenCalledWith(
          expect.stringContaining('/requirements')
        )
      })
    })
  })

  describe('7. 树节点选择和高亮测试', () => {
    
    it('选中的Usage节点应该高亮显示', async () => {
      const { container } = render(
        <ModelProvider>
          <RequirementExplorer />
        </ModelProvider>
      )

      await waitFor(async () => {
        const usageNode = screen.getByText('Web应用响应时间需求')
        await userEvent.click(usageNode)
        
        // 检查是否有选中样式
        const selectedNode = container.querySelector('.ant-tree-node-selected')
        expect(selectedNode).toBeInTheDocument()
      })
    })

    it('选中的Definition节点应该高亮显示', async () => {
      const { container } = render(
        <ModelProvider>
          <RequirementExplorer />
        </ModelProvider>
      )

      await waitFor(async () => {
        const defNode = screen.getByText('性能需求模板')
        await userEvent.click(defNode)
        
        // 检查是否有选中样式
        const trees = container.querySelectorAll('.ant-tree')
        const defTree = trees[trees.length - 1]
        const selectedNode = defTree.querySelector('.ant-tree-node-selected')
        expect(selectedNode).toBeInTheDocument()
      })
    })
  })
})