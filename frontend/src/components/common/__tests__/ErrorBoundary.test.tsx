/**
 * 错误边界组件测试 - TDD第五阶段
 * 
 * 测试覆盖：
 * - React错误边界功能
 * - 错误显示UI
 * - 错误恢复机制
 * - 错误日志记录
 */

import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen } from '@testing-library/react'
import { userEvent } from '@testing-library/user-event'
import React, { ReactNode } from 'react'
import ErrorBoundary from '../ErrorBoundary'
import { AdvancedQueryError } from '../../../services/advancedQueryApi'

// 模拟有错误的组件
const ThrowError = ({ shouldThrow = false, message = 'Test error' }: { shouldThrow?: boolean, message?: string }) => {
  if (shouldThrow) {
    throw new Error(message)
  }
  return <div>正常组件内容</div>
}

// 模拟API错误的组件
const ThrowApiError = ({ shouldThrow = false }: { shouldThrow?: boolean }) => {
  if (shouldThrow) {
    throw new AdvancedQueryError('查询失败：无效的排序字段', 400)
  }
  return <div>API组件内容</div>
}

// 模拟console.error
const mockConsoleError = vi.spyOn(console, 'error').mockImplementation(() => {})

describe('错误边界组件', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mockConsoleError.mockClear()
  })

  describe('【基础功能】正常渲染', () => {
    it('应该在没有错误时正常渲染子组件', () => {
      render(
        <ErrorBoundary>
          <ThrowError shouldThrow={false} />
        </ErrorBoundary>
      )

      expect(screen.getByText('正常组件内容')).toBeInTheDocument()
    })

    it('应该支持自定义fallback组件', () => {
      const CustomFallback = ({ error, resetError }: { error: Error; resetError: () => void }) => (
        <div>
          <h2>自定义错误页面</h2>
          <p>错误信息: {error.message}</p>
          <button onClick={resetError}>重新加载</button>
        </div>
      )

      render(
        <ErrorBoundary fallback={CustomFallback}>
          <ThrowError shouldThrow={true} message="自定义错误" />
        </ErrorBoundary>
      )

      expect(screen.getByText('自定义错误页面')).toBeInTheDocument()
      expect(screen.getByText('错误信息: 自定义错误')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: '重新加载' })).toBeInTheDocument()
    })
  })

  describe('【错误捕获】React错误处理', () => {
    it('应该捕获组件渲染错误并显示错误信息', () => {
      render(
        <ErrorBoundary>
          <ThrowError shouldThrow={true} message="组件渲染失败" />
        </ErrorBoundary>
      )

      expect(screen.getByText('系统遇到了问题')).toBeInTheDocument()
      expect(screen.getByText('组件渲染失败')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: /reload 重新加载页面/ })).toBeInTheDocument()
    })

    it('应该捕获API错误并显示专门的错误信息', () => {
      render(
        <ErrorBoundary>
          <ThrowApiError shouldThrow={true} />
        </ErrorBoundary>
      )

      expect(screen.getByText('API请求失败')).toBeInTheDocument()
      expect(screen.getByText('查询失败：无效的排序字段')).toBeInTheDocument()
      expect(screen.getByText('状态码: 400')).toBeInTheDocument()
    })

    it('应该记录错误到控制台', () => {
      render(
        <ErrorBoundary>
          <ThrowError shouldThrow={true} message="记录错误测试" />
        </ErrorBoundary>
      )

      expect(mockConsoleError).toHaveBeenCalledWith(
        'ErrorBoundary caught an error:',
        expect.any(Error),
        expect.any(Object)
      )
    })
  })

  describe('【错误恢复】重试机制', () => {
    it('应该支持错误恢复和组件重置', async () => {
      const user = userEvent.setup()
      let shouldThrow = true

      const ToggleErrorComponent = () => {
        return <ThrowError shouldThrow={shouldThrow} message="可恢复错误" />
      }

      const { rerender } = render(
        <ErrorBoundary>
          <ToggleErrorComponent />
        </ErrorBoundary>
      )

      // 验证错误显示
      expect(screen.getByText('可恢复错误')).toBeInTheDocument()

      // 修复错误条件
      shouldThrow = false

      // 点击重新加载按钮
      const retryButton = screen.getByRole('button', { name: 'reload 重新加载页面' })
      await user.click(retryButton)

      // 重新渲染组件
      rerender(
        <ErrorBoundary>
          <ToggleErrorComponent />
        </ErrorBoundary>
      )

      // 验证组件恢复正常
      expect(screen.getByText('正常组件内容')).toBeInTheDocument()
    })

    it('应该支持自动重试机制', async () => {
      let attemptCount = 0
      const AutoRetryComponent = () => {
        attemptCount++
        if (attemptCount <= 2) {
          throw new Error(`尝试 ${attemptCount} 失败`)
        }
        return <div>自动重试成功</div>
      }

      render(
        <ErrorBoundary maxRetries={3} retryDelay={50}>
          <AutoRetryComponent />
        </ErrorBoundary>
      )

      // 等待自动重试完成
      await new Promise(resolve => setTimeout(resolve, 200))

      // 应该最终成功，验证重试机制生效
      expect(screen.getByText('自动重试成功')).toBeInTheDocument()
      expect(attemptCount).toBeGreaterThanOrEqual(3)
    })
  })

  describe('【错误类型】特定错误处理', () => {
    it('应该区分处理网络错误', () => {
      const ThrowNetworkError = () => {
        throw new AdvancedQueryError('Network Error: Unable to connect to server', 0)
      }

      render(
        <ErrorBoundary>
          <ThrowNetworkError />
        </ErrorBoundary>
      )

      expect(screen.getByText('网络连接失败')).toBeInTheDocument()
      expect(screen.getByText('请检查您的网络连接并重试')).toBeInTheDocument()
      expect(screen.getByRole('button', { name: 'reload 重新连接' })).toBeInTheDocument()
    })

    it('应该区分处理认证错误', () => {
      const ThrowAuthError = () => {
        throw new AdvancedQueryError('Authentication failed', 401)
      }

      render(
        <ErrorBoundary>
          <ThrowAuthError />
        </ErrorBoundary>
      )

      expect(screen.getByText('认证失败')).toBeInTheDocument()
      expect(screen.getByText('请重新登录')).toBeInTheDocument()
    })

    it('应该区分处理权限错误', () => {
      const ThrowForbiddenError = () => {
        throw new AdvancedQueryError('Permission denied', 403)
      }

      render(
        <ErrorBoundary>
          <ThrowForbiddenError />
        </ErrorBoundary>
      )

      expect(screen.getByText('权限不足')).toBeInTheDocument()
      expect(screen.getByText('您没有执行此操作的权限')).toBeInTheDocument()
    })
  })

  describe('【用户体验】界面交互', () => {
    it('应该提供错误详情展开功能', async () => {
      const user = userEvent.setup()

      render(
        <ErrorBoundary showDetails={true}>
          <ThrowError shouldThrow={true} message="详细错误信息测试" />
        </ErrorBoundary>
      )

      // 默认不显示详情
      expect(screen.queryByText('错误详情')).not.toBeInTheDocument()

      // 点击显示详情
      const detailsButton = screen.getByRole('button', { name: '显示详情' })
      await user.click(detailsButton)

      // 验证详情显示
      expect(screen.getByText('错误详情')).toBeInTheDocument()
      expect(screen.getByText('详细错误信息测试')).toBeInTheDocument()

      // 点击隐藏详情
      const hideButton = screen.getByRole('button', { name: '隐藏详情' })
      await user.click(hideButton)

      // 验证详情隐藏
      expect(screen.queryByText('错误详情')).not.toBeInTheDocument()
    })

    it('应该支持错误反馈功能', async () => {
      const user = userEvent.setup()
      const mockFeedback = vi.fn()

      render(
        <ErrorBoundary onErrorFeedback={mockFeedback}>
          <ThrowError shouldThrow={true} message="反馈测试错误" />
        </ErrorBoundary>
      )

      const feedbackButton = screen.getByRole('button', { name: '报告错误' })
      await user.click(feedbackButton)

      expect(mockFeedback).toHaveBeenCalledWith({
        error: expect.any(Error),
        errorInfo: expect.any(Object),
        userAgent: expect.any(String),
        timestamp: expect.any(String)
      })
    })
  })

  describe('【性能考虑】错误处理性能', () => {
    it('应该避免错误循环', () => {
      let errorCount = 0
      const RecursiveErrorComponent = () => {
        errorCount++
        throw new Error(`递归错误 ${errorCount}`)
      }

      render(
        <ErrorBoundary maxErrors={3}>
          <RecursiveErrorComponent />
        </ErrorBoundary>
      )

      // 应该显示错误信息，验证错误计数机制
      expect(screen.getByText(/递归错误/)).toBeInTheDocument()
      expect(errorCount).toBeGreaterThan(0)
    })

    it('应该在指定时间后重置错误计数', async () => {
      const { rerender } = render(
        <ErrorBoundary resetTimeout={100}>
          <ThrowError shouldThrow={true} message="重置测试" />
        </ErrorBoundary>
      )

      expect(screen.getByText('重置测试')).toBeInTheDocument()

      // 等待重置超时
      await new Promise(resolve => setTimeout(resolve, 150))

      // 重新渲染应该重置错误状态
      rerender(
        <ErrorBoundary resetTimeout={100}>
          <ThrowError shouldThrow={false} />
        </ErrorBoundary>
      )

      expect(screen.getByText('正常组件内容')).toBeInTheDocument()
    })
  })
})