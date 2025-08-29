/**
 * RequirementExplorer基础测试 - TDD第一步
 * 
 * 从最基础的测试开始，逐步构建
 * Red-Green-Refactor循环
 */

import React from 'react'
import { render, screen } from '@testing-library/react'
import '@testing-library/jest-dom'

// 测试1: 组件应该能够渲染
describe('RequirementExplorer基础测试', () => {
  
  it('组件应该存在并能够导入', () => {
    // 这个测试只是确保组件文件存在
    expect(() => {
      require('../RequirementExplorer')
    }).not.toThrow()
  })

  it('组件应该能够渲染不报错', () => {
    // 最基础的渲染测试
    const RequirementExplorer = require('../RequirementExplorer').default
    
    expect(() => {
      render(<RequirementExplorer />)
    }).not.toThrow()
  })

  it('应该显示基本的布局结构', () => {
    const RequirementExplorer = require('../RequirementExplorer').default
    
    const { container } = render(<RequirementExplorer />)
    
    // 应该有一个根容器
    expect(container.firstChild).toBeInTheDocument()
  })
})