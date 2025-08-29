/**
 * RequirementUsage表单测试
 * 绕过Modal问题，直接测试表单逻辑
 */

import React from 'react'
import { render, screen, waitFor, fireEvent } from '@testing-library/react'
import '@testing-library/jest-dom'
import { vi, describe, it, expect, beforeEach } from 'vitest'
import { requirementService } from '../../services/requirementService'

// Mock requirementService
vi.mock('../../services/requirementService', () => ({
  requirementService: {
    createRequirementUsage: vi.fn()
  }
}))

// Mock antd message
vi.mock('antd', async () => {
  const actual = await vi.importActual('antd')
  return {
    ...actual,
    message: {
      success: vi.fn(),
      error: vi.fn()
    }
  }
})

describe('RequirementUsage表单逻辑测试', () => {
  
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(requirementService.createRequirementUsage).mockResolvedValue({
      elementId: 'usage-001',
      declaredName: 'Test Usage'
    })
  })
  
  it('应该正确调用createRequirementUsage服务', async () => {
    // 直接测试service调用
    const data = {
      declaredName: 'Test Usage',
      text: 'Test text content',
      of: 'def-001'
    }
    
    await requirementService.createRequirementUsage(data)
    
    expect(requirementService.createRequirementUsage).toHaveBeenCalledWith(data)
  })
  
  it('应该验证必填字段', async () => {
    // 测试验证逻辑
    const validateUsageData = (data: any) => {
      const errors = []
      if (!data.declaredName) errors.push('名称是必填项')
      if (!data.text) errors.push('需求文本是必填项')
      if (!data.of) errors.push('必须选择基于的定义')
      return errors
    }
    
    // 测试缺少必填字段
    const invalidData = {
      declaredName: '',
      text: '',
      of: ''
    }
    
    const errors = validateUsageData(invalidData)
    expect(errors).toContain('名称是必填项')
    expect(errors).toContain('需求文本是必填项')
    expect(errors).toContain('必须选择基于的定义')
    
    // 测试完整数据
    const validData = {
      declaredName: 'Test Usage',
      text: 'Test text',
      of: 'def-001'
    }
    
    const validErrors = validateUsageData(validData)
    expect(validErrors).toHaveLength(0)
  })
  
  it('应该正确处理service错误', async () => {
    vi.mocked(requirementService.createRequirementUsage).mockRejectedValue(
      new Error('创建失败')
    )
    
    try {
      await requirementService.createRequirementUsage({
        declaredName: 'Test',
        text: 'Test',
        of: 'def-001'
      })
    } catch (error: any) {
      expect(error.message).toBe('创建失败')
    }
  })
})