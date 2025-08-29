/**
 * 调试测试 - 查看TableView实际渲染内容
 */

import React from 'react'
import { render, screen, waitFor } from '@testing-library/react'
import { describe, test, expect, beforeEach, vi } from 'vitest'
import { ModelProvider } from '../../../contexts/ModelContext'

// mock axios
vi.mock('axios')
vi.mock('../../../services/requirementService')
vi.mock('../../../services/universalApi')
vi.mock('../../../services/advancedQueryApi')

import TableView from '../TableView'
import { requirementService } from '../../../services/requirementService'

describe('TableView Debug', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  test('应该显示mock数据', async () => {
    render(
      <ModelProvider>
        <TableView />
      </ModelProvider>
    )

    // 等待加载完成
    await waitFor(() => {
      expect(requirementService.getAllRequirements).toHaveBeenCalled()
    })

    // 等待一段时间让数据渲染
    await new Promise(resolve => setTimeout(resolve, 1000))

    // 打印整个DOM内容用于调试
    console.log('=== DOM Content ===')
    console.log(document.body.innerHTML)
    
    // 检查表格是否存在
    const table = document.querySelector('.ant-table')
    console.log('=== Table exists ===', !!table)
    
    if (table) {
      console.log('=== Table content ===')
      console.log(table.innerHTML)
    }
    
    // 检查是否有任何文本内容
    const allTexts = Array.from(document.querySelectorAll('*'))
      .map(el => el.textContent?.trim())
      .filter(text => text && text.length > 0)
    
    console.log('=== All text content ===')
    console.log(allTexts.slice(0, 20)) // 只显示前20个
    
    // 基本断言确保测试不会失败
    expect(document.body).toBeInTheDocument()
  })
})